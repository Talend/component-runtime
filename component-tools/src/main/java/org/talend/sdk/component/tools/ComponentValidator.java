/**
 * Copyright (C) 2006-2017 Talend Inc. - www.talend.com
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.talend.sdk.component.tools;

import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import java.io.File;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.xbean.finder.AnnotationFinder;
import org.talend.sdk.component.api.component.Icon;
import org.talend.sdk.component.api.component.Version;
import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.configuration.action.Checkable;
import org.talend.sdk.component.api.configuration.action.Proposable;
import org.talend.sdk.component.api.configuration.type.DataSet;
import org.talend.sdk.component.api.configuration.type.DataStore;
import org.talend.sdk.component.api.internationalization.Internationalized;
import org.talend.sdk.component.api.meta.Documentation;
import org.talend.sdk.component.api.service.ActionType;
import org.talend.sdk.component.api.service.Service;
import org.talend.sdk.component.api.service.asyncvalidation.AsyncValidation;
import org.talend.sdk.component.api.service.completion.DynamicValues;
import org.talend.sdk.component.api.service.healthcheck.HealthCheck;
import org.talend.sdk.component.api.service.http.Request;
import org.talend.sdk.component.api.service.schema.DiscoverSchema;
import org.talend.sdk.component.runtime.manager.service.HttpClientFactoryImpl;
import org.talend.sdk.component.runtime.visitor.ModelListener;
import org.talend.sdk.component.runtime.visitor.ModelVisitor;

import lombok.Data;

// IMPORTANT: this class is used by reflection in gradle integration, don't break signatures without checking it
public class ComponentValidator extends BaseTask {

    private final Configuration configuration;

    private final Log log;

    public ComponentValidator(final Configuration configuration, final File[] classes, final Object log) {
        super(classes);
        this.configuration = configuration;
        try {
            this.log = Log.class.isInstance(log) ? Log.class.cast(log) : new ReflectiveLog(log);
        } catch (final NoSuchMethodException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public void run() {
        final AnnotationFinder finder = newFinder();
        final List<Class<?>> components =
                componentMarkers().flatMap(a -> finder.findAnnotatedClasses(a).stream()).collect(toList());
        components.forEach(c -> log.debug("Found component: " + c));

        final Set<String> errors = new HashSet<>();

        if (configuration.isValidateFamily()) {
            // todo: better fix is to get the package with @Components then check it has an icon
            // but it should be enough for now
            components.forEach(c -> {
                try {
                    findPackageOrFail(c, Icon.class);
                } catch (final IllegalArgumentException iae) {
                    errors.add(iae.getMessage());
                }
            });
        }

        if (configuration.isValidateSerializable()) {
            final Collection<Class<?>> copy = new ArrayList<>(components);
            copy.removeIf(this::isSerializable);
            errors.addAll(copy.stream().map(c -> c + " is not Serializable").collect(toList()));
        }

        if (configuration.isValidateInternationalization()) {
            validateInternationalization(finder, components, errors);
        }

        if (configuration.isValidateHttpClient()) {
            validateHttpClient(finder, errors);
        }

        if (configuration.isValidateModel()) {
            validateModel(components, errors);
        }

        if (configuration.isValidateMetadata()) {
            validateMetadata(components, errors);
        }

        if (configuration.isValidateDataStore()) {
            validateDataStore(finder, errors);
        }

        if (configuration.isValidateDataSet()) {
            validateDataSet(finder, errors);
        }

        if (configuration.isValidateActions()) {
            validateActions(finder, errors);

        }

        if (configuration.isValidateDocumentation()) {
            validateDocumentation(finder, components, errors);
        }

        if (!errors.isEmpty()) {
            errors.forEach(log::error);
            throw new IllegalStateException(
                    "Some error were detected:" + errors.stream().collect(joining("\n- ", "\n- ", "")));
        }
    }

    private void validateDocumentation(final AnnotationFinder finder, final List<Class<?>> components,
            final Set<String> errors) {
        errors.addAll(components
                .stream()
                .filter(c -> !c.isAnnotationPresent(Documentation.class))
                .map(c -> "No @Documentation on '" + c.getName() + "'")
                .sorted()
                .collect(toList()));
        errors.addAll(finder
                .findAnnotatedFields(Option.class)
                .stream()
                .filter(field -> !field.isAnnotationPresent(Documentation.class)
                        && !field.getType().isAnnotationPresent(Documentation.class))
                .map(field -> "No @Documentation on '" + field + "'")
                .sorted()
                .collect(toSet()));
    }

    private void validateInternationalization(final AnnotationFinder finder, final List<Class<?>> components,
            final Set<String> errors) {
        errors.addAll(components.stream().map(this::validateComponentResourceBundle).filter(Objects::nonNull).collect(
                toList()));

        for (final Class<?> i : finder.findAnnotatedClasses(Internationalized.class)) {
            final ResourceBundle resourceBundle = findResourceBundle(i);
            if (resourceBundle != null) {
                final Collection<String> keys = Stream
                        .of(i.getMethods())
                        .filter(m -> m.getDeclaringClass() != Object.class)
                        .map(m -> i.getName() + "." + m.getName())
                        .collect(toSet());
                errors.addAll(keys
                        .stream()
                        .filter(k -> !resourceBundle.containsKey(k))
                        .map(k -> "Missing key " + k + " in " + i + " resource bundle")
                        .collect(toList()));

                errors.addAll(resourceBundle
                        .keySet()
                        .stream()
                        .filter(k -> k.startsWith(i.getName() + ".") && !keys.contains(k))
                        .map(k -> "Key " + k + " from " + i + " is no more used")
                        .collect(toList()));
            } else {
                errors.add("No resource bundle for " + i);
            }
        }
    }

    private void validateHttpClient(final AnnotationFinder finder, final Set<String> errors) {
        errors.addAll(finder
                .findAnnotatedClasses(Request.class)
                .stream()
                .map(Class::getDeclaringClass)
                .distinct()
                .flatMap(c -> HttpClientFactoryImpl.createErrors(c).stream())
                .collect(toList()));
    }

    private void validateModel(final List<Class<?>> components, final Set<String> errors) {
        errors.addAll(components
                .stream()
                .filter(c -> componentMarkers().filter(c::isAnnotationPresent).count() > 1)
                .map(i -> i + " has conflicting component annotations, ensure it has a single one")
                .collect(toList()));

        final ModelVisitor modelVisitor = new ModelVisitor();
        final ModelListener noop = new ModelListener() {

        };
        errors.addAll(components.stream().map(c -> {
            try {
                modelVisitor.visit(c, noop, configuration.isValidateComponent());
                return null;
            } catch (final RuntimeException re) {
                return re.getMessage();
            }
        }).filter(Objects::nonNull).collect(toList()));
    }

    private void validateMetadata(final List<Class<?>> components, final Set<String> errors) {
        errors.addAll(components.stream().map(component -> {
            if (!component.isAnnotationPresent(Version.class) || !component.isAnnotationPresent(Icon.class)) {
                return "Component " + component + " should use @Icon and @Version";
            }
            return null;
        }).filter(Objects::nonNull).collect(toList()));
    }

    private void validateDataStore(final AnnotationFinder finder, final Set<String> errors) {
        final List<Class<?>> datastoreClasses = finder.findAnnotatedClasses(DataStore.class);

        final List<String> datastores =
                datastoreClasses.stream().map(d -> d.getAnnotation(DataStore.class).value()).collect(toList());

        Set<String> uniqueDatastores = new HashSet<>(datastores);
        if (datastores.size() != uniqueDatastores.size()) {
            errors.add("Duplicated DataStore found : " + datastores
                    .stream()
                    .collect(groupingBy(identity()))
                    .entrySet()
                    .stream()
                    .filter(e -> e.getValue().size() > 1)
                    .map(Map.Entry::getKey)
                    .collect(joining(", ")));
        }

        final List<Class<?>> checkableClasses = finder.findAnnotatedClasses(Checkable.class);
        errors.addAll(checkableClasses
                .stream()
                .filter(d -> !d.isAnnotationPresent(DataStore.class))
                .map(c -> c.getName() + " has @Checkable but is not a @DataStore")
                .collect(toList()));

        final Map<String, String> checkableDataStoresMap =
                checkableClasses.stream().filter(d -> d.isAnnotationPresent(DataStore.class)).collect(toMap(
                        d -> d.getAnnotation(DataStore.class).value(), d -> d.getAnnotation(Checkable.class).value()));

        final Set<String> healthchecks = finder
                .findAnnotatedMethods(HealthCheck.class)
                .stream()
                .filter(h -> h.getDeclaringClass().isAnnotationPresent(Service.class))
                .map(m -> m.getAnnotation(HealthCheck.class).value())
                .collect(toSet());
        errors.addAll(checkableDataStoresMap
                .entrySet()
                .stream()
                .filter(e -> !healthchecks.contains(e.getValue()))
                .map(e -> "No @HealthCheck for dataStore: '" + e.getKey() + "' with checkable: '" + e.getValue() + "'")
                .collect(toList()));
    }

    private void validateDataSet(final AnnotationFinder finder, final Set<String> errors) {
        final List<String> datasets = finder
                .findAnnotatedClasses(DataSet.class)
                .stream()
                .map(d -> d.getAnnotation(DataSet.class).value())
                .collect(toList());
        final Set<String> uniqueDatasets = new HashSet<>(datasets);
        if (datasets.size() != uniqueDatasets.size()) {
            errors.add("Duplicated DataSet found : " + datasets
                    .stream()
                    .collect(groupingBy(identity()))
                    .entrySet()
                    .stream()
                    .filter(e -> e.getValue().size() > 1)
                    .map(Map.Entry::getKey)
                    .collect(joining(", ")));
        }
    }

    private void validateActions(final AnnotationFinder finder, final Set<String> errors) {
        // returned types
        errors.addAll(Stream
                .of(AsyncValidation.class, DynamicValues.class, HealthCheck.class, DiscoverSchema.class)
                .flatMap(action -> {
                    final Class<?> returnedType = action.getAnnotation(ActionType.class).expectedReturnedType();
                    final List<Method> annotatedMethods = finder.findAnnotatedMethods(action);
                    return Stream.concat(
                            annotatedMethods
                                    .stream()
                                    .filter(m -> !returnedType.isAssignableFrom(m.getReturnType()))
                                    .map(m -> m + " doesn't return a " + returnedType + ", please fix it"),
                            annotatedMethods
                                    .stream()
                                    .filter(m -> !m.getDeclaringClass().isAnnotationPresent(Service.class)
                                            && !Modifier.isAbstract(m.getDeclaringClass().getModifiers()))
                                    .map(m -> m + " is not declared into a service class"));
                })
                .collect(toSet()));

        // parameters for @DynamicValues
        errors.addAll(finder
                .findAnnotatedMethods(DynamicValues.class)
                .stream()
                .filter(m -> countParameters(m) != 0)
                .map(m -> m + " should have no parameter")
                .collect(toSet()));

        // parameters for @HealthCheck
        errors.addAll(finder
                .findAnnotatedMethods(HealthCheck.class)
                .stream()
                .filter(m -> countParameters(m) != 1 || !m.getParameterTypes()[0].isAnnotationPresent(DataStore.class))
                .map(m -> m + " should have its first parameter being a datastore (marked with @DataStore)")
                .collect(toSet()));

        // parameters for @DiscoverSchema
        errors.addAll(finder
                .findAnnotatedMethods(DiscoverSchema.class)
                .stream()
                .filter(m -> countParameters(m) != 1 || !m.getParameterTypes()[0].isAnnotationPresent(DataSet.class))
                .map(m -> m + " should have its first parameter being a dataset (marked with @Config)")
                .collect(toSet()));

        errors.addAll(finder
                .findAnnotatedFields(Proposable.class)
                .stream()
                .filter(f -> f.getType().isEnum())
                .map(f -> f.toString() + " must not define @Proposable since it is an enum")
                .collect(toList()));

        final Set<String> proposables = finder
                .findAnnotatedFields(Proposable.class)
                .stream()
                .map(f -> f.getAnnotation(Proposable.class).value())
                .collect(toSet());
        final Set<String> dynamicValues = finder
                .findAnnotatedMethods(DynamicValues.class)
                .stream()
                .map(f -> f.getAnnotation(DynamicValues.class).value())
                .collect(toSet());
        proposables.removeAll(dynamicValues);
        errors.addAll(proposables
                .stream()
                .map(p -> "No @DynamicValues(\"" + p + "\"), add a service with this method: " + "@DynamicValues(\"" + p
                        + "\") Values proposals();")
                .collect(toList()));
    }

    private int countParameters(final Method m) {
        return (int) Stream.of(m.getParameterTypes()).filter(p -> !isService(p)).count();
    }

    private boolean isService(final Class<?> p) {
        return p.isAnnotationPresent(Service.class) || p.isAnnotationPresent(Internationalized.class)
                || Stream.of(p.getMethods()).anyMatch(m -> m.isAnnotationPresent(Request.class))
                || p.getName().startsWith("org.talend.sdk.component.api.service");
    }

    private String validateComponentResourceBundle(final Class<?> component) {
        final String baseName = ofNullable(component.getPackage()).map(p -> p.getName() + ".").orElse("") + "Messages";
        final ResourceBundle bundle = findResourceBundle(component);
        if (bundle == null) {
            return "No resource bundle for " + component.getName() + ", you should create a "
                    + baseName.replace('.', '/') + ".properties at least.";
        }

        final String prefix = components(component).map(c -> findFamily(c, component) + "." + c.name()).orElseThrow(
                () -> new IllegalStateException(component.getName()));
        final Collection<String> missingKeys =
                Stream.of("_displayName").map(n -> prefix + "." + n).filter(k -> !bundle.containsKey(k)).collect(
                        toList());
        if (!missingKeys.isEmpty()) {
            return baseName + " is missing the key(s): " + missingKeys.stream().collect(joining("\n"));
        }
        return null;
    }

    private boolean isSerializable(final Class<?> aClass) {
        return Serializable.class.isAssignableFrom(aClass);
    }

    @Data
    public static class Configuration {

        private boolean validateFamily;

        private boolean validateSerializable;

        private boolean validateInternationalization;

        private boolean validateHttpClient;

        private boolean validateModel;

        private boolean validateMetadata;

        private boolean validateComponent;

        private boolean validateDataStore;

        private boolean validateDataSet;

        private boolean validateActions;

        private boolean validateDocumentation;
    }
}
