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
package org.talend.sdk.component.runtime.manager.validator;

import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import java.io.File;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.xbean.finder.AnnotationFinder;
import org.apache.xbean.finder.archive.FileArchive;
import org.talend.sdk.component.api.component.Components;
import org.talend.sdk.component.api.component.Icon;
import org.talend.sdk.component.api.component.Version;
import org.talend.sdk.component.api.configuration.type.DataSet;
import org.talend.sdk.component.api.configuration.type.DataStore;
import org.talend.sdk.component.api.input.Emitter;
import org.talend.sdk.component.api.input.PartitionMapper;
import org.talend.sdk.component.api.internationalization.Internationalized;
import org.talend.sdk.component.api.processor.Processor;
import org.talend.sdk.component.api.service.ActionType;
import org.talend.sdk.component.api.service.Service;
import org.talend.sdk.component.api.service.asyncvalidation.AsyncValidation;
import org.talend.sdk.component.api.service.completion.DynamicValues;
import org.talend.sdk.component.api.service.healthcheck.HealthCheck;
import org.talend.sdk.component.api.service.schema.DiscoverSchema;
import org.talend.sdk.component.runtime.visitor.ModelListener;
import org.talend.sdk.component.runtime.visitor.ModelVisitor;

import lombok.Data;

// todo: for now the test coverage is in the maven module, we should move it here
// IMPORTANT: this class is used by reflection in gradle integration, don't break signatures without checking it
public class ComponentValidator implements Runnable {

    private final Configuration configuration;

    private final File classes;

    private final Log log;

    public ComponentValidator(final Configuration configuration, final File classes, final Object log) {
        this.configuration = configuration;
        this.classes = classes;
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

        if (configuration.validateFamily) {
            // todo: better fix is to get the pcakage with @Components then check it has
            // icon
            // but it should be enough for now
            components.forEach(c -> findPackageOrFail(c, Icon.class));
        }

        if (configuration.validateSerializable) {
            final Collection<Class<?>> copy = new ArrayList<>(components);
            copy.removeIf(this::isSerializable);
            if (!copy.isEmpty()) {
                copy.forEach(c -> log.error(c + " is not Serializable"));
                throw new IllegalStateException("All components must be serializable for BEAM execution support");

            }
        }

        if (configuration.validateInternationalization) {
            final List<String> errors =
                    components.stream().map(this::validateComponentResourceBundle).filter(Objects::nonNull).collect(
                            toList());
            if (!errors.isEmpty()) {
                errors.forEach(log::error);
                throw new IllegalStateException("Some component internationalization is not complete");
            }

            for (final Class<?> i : finder.findAnnotatedClasses(Internationalized.class)) {
                final ResourceBundle resourceBundle = findResourceBundle(i);
                if (resourceBundle != null) {
                    final Collection<String> keys = Stream
                            .of(i.getMethods())
                            .filter(m -> m.getDeclaringClass() != Object.class)
                            .map(m -> i.getName() + "." + m.getName())
                            .collect(toSet());
                    final Collection<String> missingKeys =
                            keys.stream().filter(k -> !resourceBundle.containsKey(k)).collect(toList());
                    if (!missingKeys.isEmpty()) {
                        missingKeys.forEach(k -> log.error("Missing key " + k + " in " + i + " resource bundle"));
                        throw new IllegalStateException(i + " is missing some internalization messages");
                    }

                    final List<String> outdatedKeys = resourceBundle
                            .keySet()
                            .stream()
                            .filter(k -> k.startsWith(i.getName() + ".") && !keys.contains(k))
                            .collect(toList());
                    if (!outdatedKeys.isEmpty()) {
                        outdatedKeys.forEach(k -> log.error("Key " + k + " from " + i + " is no more used"));
                        throw new IllegalStateException(i + " has some deleted keys in its resource bundle");
                    }
                } else {
                    log.error("No resource bundle for " + i);
                    throw new IllegalStateException("Missing resource bundle for " + i);
                }
            }
        }

        if (configuration.validateModel) {
            final List<Class<?>> ambiguousComponents = components
                    .stream()
                    .filter(c -> componentMarkers().filter(c::isAnnotationPresent).count() > 1)
                    .collect(toList());
            if (!ambiguousComponents.isEmpty()) {
                ambiguousComponents.forEach(
                        i -> log.error(i + " has conflicting component annotations, ensure it has a single one"));
                throw new IllegalStateException("Ambiguous components are present: " + ambiguousComponents);
            }

            final ModelVisitor modelVisitor = new ModelVisitor();
            final ModelListener noop = new ModelListener() {

            };
            final List<String> errors = components.stream().map(c -> {
                try {
                    modelVisitor.visit(c, noop, configuration.validateComponent);
                    return null;
                } catch (final RuntimeException re) {
                    return re.getMessage();
                }
            }).filter(Objects::nonNull).collect(toList());
            if (!errors.isEmpty()) {
                errors.forEach(log::error);
                throw new IllegalStateException("Some model error were detected");
            }
        }

        if (configuration.validateMetadata) {
            components.forEach(component -> {
                if (!component.isAnnotationPresent(Version.class) || !component.isAnnotationPresent(Icon.class)) {
                    throw new IllegalArgumentException("Component " + component + " should use @Icon and @Version");
                }
            });
        }

        if (configuration.validateDataStore) {
            final List<String> datastores = finder
                    .findAnnotatedClasses(DataStore.class)
                    .stream()
                    .map(d -> d.getAnnotation(DataStore.class).value())
                    .collect(toList());

            Set<String> uniqueDatastores = new HashSet<>(datastores);
            if (datastores.size() != uniqueDatastores.size()) {
                throw new IllegalStateException("Duplicated DataStore found : " + datastores
                        .stream()
                        .collect(groupingBy(identity()))
                        .entrySet()
                        .stream()
                        .filter(e -> e.getValue().size() > 1)
                        .map(Map.Entry::getKey)
                        .collect(joining(", ")));
            }

            final Set<String> healthchecks = finder
                    .findAnnotatedMethods(HealthCheck.class)
                    .stream()
                    .map(m -> m.getAnnotation(HealthCheck.class).value())
                    .collect(toSet());
            if (!healthchecks.containsAll(datastores)) {
                final Set<String> missing = new HashSet<>(datastores);
                datastores.removeAll(healthchecks);
                throw new IllegalStateException("No @HealthCheck for " + missing + " datastores");
            }

        }

        if (configuration.validateDataSet) {
            final List<String> datasets = finder
                    .findAnnotatedClasses(DataSet.class)
                    .stream()
                    .map(d -> d.getAnnotation(DataSet.class).value())
                    .collect(toList());
            Set<String> uniqueDatasets = new HashSet<>(datasets);
            if (datasets.size() != uniqueDatasets.size()) {
                throw new IllegalStateException("Duplicated DataSet found : " + datasets
                        .stream()
                        .collect(groupingBy(identity()))
                        .entrySet()
                        .stream()
                        .filter(e -> e.getValue().size() > 1)
                        .map(Map.Entry::getKey)
                        .collect(joining(", ")));
            }
        }

        if (configuration.validateActions) {
            final Set<String> errors = new HashSet<>();

            // returned types
            errors.addAll(Stream
                    .of(AsyncValidation.class, DynamicValues.class, HealthCheck.class, DiscoverSchema.class)
                    .flatMap(action -> {
                        final Class<?> returnedType = action.getAnnotation(ActionType.class).expectedReturnedType();
                        return finder
                                .findAnnotatedMethods(action)
                                .stream()
                                .filter(m -> !returnedType.isAssignableFrom(m.getReturnType()))
                                .map(m -> m + " doesn't return a " + returnedType + ", please fix it")
                                .collect(toSet())
                                .stream();
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
                    .filter(m -> countParameters(m) != 1
                            || !m.getParameterTypes()[0].isAnnotationPresent(DataStore.class))
                    .map(m -> m + " should have its first parameter being a datastore (marked with @DataStore)")
                    .collect(toSet()));

            // parameters for @DiscoverSchema
            errors.addAll(finder
                    .findAnnotatedMethods(DiscoverSchema.class)
                    .stream()
                    .filter(m -> countParameters(m) != 1
                            || !m.getParameterTypes()[0].isAnnotationPresent(DataSet.class))
                    .map(m -> m + " should have its first parameter being a dataset (marked with @Config)")
                    .collect(toSet()));

            if (!errors.isEmpty()) {
                throw new IllegalStateException(errors.stream().collect(joining("\n- ", "\n- ", "")));
            }
        }
    }

    private int countParameters(final Method m) {
        return (int) Stream
                .of(m.getParameterTypes())
                .filter(p -> !p.getName().startsWith("org.talend.sdk.component.api.service")
                        && !p.isAnnotationPresent(Service.class))
                .count();
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
            return baseName + " is missing the keys:" + missingKeys.stream().collect(joining("\n"));
        }
        return null;
    }

    private Stream<Class<? extends Annotation>> componentMarkers() {
        return Stream.of(PartitionMapper.class, Processor.class, Emitter.class);
    }

    private AnnotationFinder newFinder() {
        return new AnnotationFinder(new FileArchive(Thread.currentThread().getContextClassLoader(), classes));
    }

    private Optional<Component> components(final Class<?> component) {
        return componentMarkers().map(component::getAnnotation).filter(Objects::nonNull).findFirst().map(
                this::asComponent);
    }

    private String findFamily(final Component c, final Class<?> component) {
        return of(c.family()).filter(name -> !name.isEmpty()).orElseGet(
                () -> findPackageOrFail(component, Components.class).family());
    }

    private <A extends Annotation> A findPackageOrFail(final Class<?> component, final Class<A> api) {
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        final String pck = component.getPackage() == null ? null : component.getPackage().getName();
        if (pck != null) {
            String currentPackage = pck;
            do {
                try {
                    final Class<?> pckInfo = loader.loadClass(currentPackage + ".package-info");
                    if (pckInfo.isAnnotationPresent(api)) {
                        return pckInfo.getAnnotation(api);
                    }
                } catch (final ClassNotFoundException e) {
                    // no-op
                }

                final int endPreviousPackage = currentPackage.lastIndexOf('.');
                if (endPreviousPackage < 0) { // we don't accept default package since it is not specific enough
                    break;
                }

                currentPackage = currentPackage.substring(0, endPreviousPackage);
            } while (true);
        }
        throw new IllegalArgumentException("No @" + api.getName() + " on " + component
                + ", add it or disable this validation" + " (which can have side effects in integrations/designers)");
    }

    private ResourceBundle findResourceBundle(final Class<?> component) {
        final String baseName = ofNullable(component.getPackage()).map(p -> p.getName() + ".").orElse("") + "Messages";
        try {
            return ResourceBundle.getBundle(baseName, Locale.ENGLISH, Thread.currentThread().getContextClassLoader());
        } catch (final MissingResourceException mre) {
            return null;
        }
    }

    private boolean isSerializable(final Class<?> aClass) {
        return Serializable.class.isAssignableFrom(aClass);
    }

    private Component asComponent(final Annotation a) {
        return Component.class.cast(Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                new Class<?>[] { Component.class },
                (proxy, method, args) -> a.annotationType().getMethod(method.getName()).invoke(a)));
    }

    public interface Component {

        String family();

        String name();
    }

    public interface Log {

        void debug(String s);

        void error(String s);
    }

    private static class ReflectiveLog implements Log {

        private final Object delegate;

        private final Method error;

        private final Method debug;

        private ReflectiveLog(final Object delegate) throws NoSuchMethodException {
            this.delegate = delegate;
            final Class<?> delegateClass = delegate.getClass();
            this.error = delegateClass.getMethod("error", String.class);
            this.debug = delegateClass.getMethod("debug", String.class);
        }

        @Override
        public void debug(final String s) {
            try {
                debug.invoke(debug, s);
            } catch (final IllegalAccessException e) {
                throw new IllegalStateException(e);
            } catch (final InvocationTargetException e) {
                throw new IllegalStateException(e.getTargetException());
            }
        }

        @Override
        public void error(final String s) {
            try {
                error.invoke(debug, s);
            } catch (final IllegalAccessException e) {
                throw new IllegalStateException(e);
            } catch (final InvocationTargetException e) {
                throw new IllegalStateException(e.getTargetException());
            }
        }
    }

    @Data
    public static class Configuration {

        private boolean validateFamily;

        private boolean validateSerializable;

        private boolean validateInternationalization;

        private boolean validateModel;

        private boolean validateMetadata;

        private boolean validateComponent;

        private boolean validateDataStore;

        private boolean validateDataSet;

        private boolean validateActions;

        public static Configuration from(final Object template) {
            final Configuration configuration = new Configuration();
            Stream.of(template.getClass().getDeclaredFields()).filter(f -> {
                // validate the field is the same then we'll
                // be able to set it, also set them as
                // accessible at the same time
                try {
                    Configuration.class.getDeclaredField(f.getName()).setAccessible(true);
                    f.setAccessible(true);
                    return true;
                } catch (final NoSuchFieldException e) {
                    return false;
                }
            }).forEach(f -> {
                try {
                    Configuration.class.getDeclaredField(f.getName()).set(configuration, f.get(template));
                } catch (final IllegalAccessException | NoSuchFieldException e) {
                    throw new IllegalArgumentException(e);
                }
            });
            return configuration;
        }
    }
}
