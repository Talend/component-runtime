/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
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

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Stream.concat;
import static java.util.stream.Stream.empty;
import static java.util.stream.Stream.of;
import static org.talend.sdk.component.runtime.manager.ParameterMeta.Type.ARRAY;
import static org.talend.sdk.component.runtime.manager.ParameterMeta.Type.ENUM;
import static org.talend.sdk.component.runtime.manager.ParameterMeta.Type.OBJECT;
import static org.talend.sdk.component.runtime.manager.reflect.Constructors.findConstructor;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collector;
import java.util.stream.Stream;

import org.apache.xbean.finder.AnnotationFinder;
import org.apache.xbean.propertyeditor.PropertyEditorRegistry;
import org.talend.sdk.component.api.component.Components;
import org.talend.sdk.component.api.component.Icon;
import org.talend.sdk.component.api.component.Version;
import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.configuration.action.Checkable;
import org.talend.sdk.component.api.configuration.action.Proposable;
import org.talend.sdk.component.api.configuration.action.Updatable;
import org.talend.sdk.component.api.configuration.type.DataSet;
import org.talend.sdk.component.api.configuration.type.DataStore;
import org.talend.sdk.component.api.configuration.ui.DefaultValue;
import org.talend.sdk.component.api.configuration.ui.widget.Structure;
import org.talend.sdk.component.api.input.Emitter;
import org.talend.sdk.component.api.input.PartitionMapper;
import org.talend.sdk.component.api.internationalization.Internationalized;
import org.talend.sdk.component.api.meta.Documentation;
import org.talend.sdk.component.api.processor.AfterGroup;
import org.talend.sdk.component.api.processor.ElementListener;
import org.talend.sdk.component.api.processor.Output;
import org.talend.sdk.component.api.processor.Processor;
import org.talend.sdk.component.api.service.ActionType;
import org.talend.sdk.component.api.service.Service;
import org.talend.sdk.component.api.service.asyncvalidation.AsyncValidation;
import org.talend.sdk.component.api.service.completion.DynamicValues;
import org.talend.sdk.component.api.service.completion.Suggestions;
import org.talend.sdk.component.api.service.healthcheck.HealthCheck;
import org.talend.sdk.component.api.service.http.Request;
import org.talend.sdk.component.api.service.schema.DiscoverSchema;
import org.talend.sdk.component.api.service.update.Update;
import org.talend.sdk.component.runtime.manager.ParameterMeta;
import org.talend.sdk.component.runtime.manager.reflect.ParameterModelService;
import org.talend.sdk.component.runtime.manager.reflect.parameterenricher.BaseParameterEnricher;
import org.talend.sdk.component.runtime.manager.service.LocalConfigurationService;
import org.talend.sdk.component.runtime.manager.service.http.HttpClientFactoryImpl;
import org.talend.sdk.component.runtime.visitor.ModelListener;
import org.talend.sdk.component.runtime.visitor.ModelVisitor;

import lombok.Data;

// IMPORTANT: this class is used by reflection in gradle integration, don't break signatures without checking it
public class ComponentValidator extends BaseTask {

    private final Configuration configuration;

    private final Log log;

    private final ParameterModelService parameterModelService = new ParameterModelService(new PropertyEditorRegistry());

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

        final Set<String> errors = new LinkedHashSet<>();

        if (configuration.isValidateFamily()) {
            // todo: better fix is to get the package with @Components then check it has an icon
            // but it should be enough for now
            components.forEach(c -> {
                try {
                    final Class<?> pck = findPackageOrFail(c, Icon.class);
                    final Icon annotation = pck.getAnnotation(Icon.class);
                    ofNullable(validateIcon(annotation)).ifPresent(errors::add);
                } catch (final IllegalArgumentException iae) {
                    errors.add(iae.getMessage());
                }
            });
        }

        if (configuration.isValidateSerializable()) {
            final Collection<Class<?>> copy = new ArrayList<>(components);
            copy.removeIf(this::isSerializable);
            errors.addAll(copy.stream().map(c -> c + " is not Serializable").sorted().collect(toSet()));
        }

        if (configuration.isValidateInternationalization()) {
            validateInternationalization(finder, components, errors);
        }

        if (configuration.isValidateHttpClient()) {
            validateHttpClient(finder, errors);
        }

        if (configuration.isValidateModel()) {
            validateModel(finder, components, errors);
        }

        if (configuration.isValidateMetadata()) {
            validateMetadata(components, errors);
        }

        if (configuration.isValidateDataStore()) {
            validateDataStore(finder, errors);
        }

        if (configuration.isValidateDataSet()) {
            validateDataSet(finder, components, errors);
        }

        if (configuration.isValidateActions()) {
            validateActions(finder, errors);
        }

        if (configuration.isValidateDocumentation()) {
            validateDocumentation(finder, components, errors);
        }

        if (configuration.isValidateLayout()) {
            validateLayout(components, errors);
        }

        if (configuration.isValidateOptionNames()) {
            validateOptionNames(finder, errors);
        }

        if (configuration.isValidateLocalConfiguration()) {
            validateLocalConfiguration(components, finder, errors);
        }

        if (configuration.isValidateOutputConnection()) {
            validateOutputConnection(components, errors);
        }

        if (!errors.isEmpty()) {
            final List<String> preparedErrors =
                    errors.stream().map(it -> it.replace("java.lang.", "").replace("java.util.", "")).collect(toList());
            preparedErrors.forEach(log::error);
            throw new IllegalStateException(
                    "Some error were detected:" + preparedErrors.stream().collect(joining("\n- ", "\n- ", "")));
        }

        log.info("Validated components: " + components.stream().map(Class::getSimpleName).collect(joining(", ")));
    }

    private String validateIcon(final Icon annotation) {
        if (annotation.value() == Icon.IconType.CUSTOM) {
            final String icon = annotation.custom();
            if (classes.length > 0 && !new File(classes[0], "icons/" + icon + "_icon32.png").exists()) {
                return "No icon: '" + icon + "' found, did you create 'icons/" + icon + "_icon32.png' in resources?";
            }
        }
        return null;
    }

    private void validateOutputConnection(final List<Class<?>> components, final Set<String> errors) {
        // outputs must have only one input param
        errors
                .addAll(components
                        .stream()
                        .flatMap(c -> of(c.getMethods()).filter(m -> m.isAnnotationPresent(ElementListener.class)))
                        .filter(m -> of(m.getParameters()).noneMatch(p -> p.isAnnotationPresent(Output.class)))
                        .filter(m -> of(m.getParameters())
                                .filter(p -> !p.isAnnotationPresent(Output.class))
                                .count() > 1)
                        .map(Method::getDeclaringClass)
                        .distinct()
                        .map(clazz -> "The Output component '" + clazz
                                + "' must have only one single input branch parameter in its ElementListener method.")
                        .collect(toList()));
    }

    private void validateLocalConfiguration(final Collection<Class<?>> components, final AnnotationFinder finder,
            final Set<String> errors) {
        final String family = components
                .stream()
                .map(c -> findFamily(components(c).orElse(null), c))
                .findFirst()
                .map(s -> s.toLowerCase(Locale.ROOT))
                .orElse("");

        // first check TALEND-INF/local-configuration.properties
        errors
                .addAll(Stream
                        .of(classes)
                        .map(root -> new File(root, "TALEND-INF/local-configuration.properties"))
                        .filter(File::exists)
                        .flatMap(props -> {
                            final Properties properties = new Properties();
                            try (final InputStream stream = new BufferedInputStream(new FileInputStream(props))) {
                                properties.load(stream);
                            } catch (final IOException e) {
                                throw new IllegalStateException(e);
                            }
                            return properties
                                    .stringPropertyNames()
                                    .stream()
                                    .filter(it -> !it.toLowerCase(Locale.ROOT).startsWith(family + "."))
                                    .map(it -> "'" + it + "' does not start with '" + family + "', "
                                            + "it is recommended to prefix all keys by the family");
                        })
                        .collect(toSet()));

        // then check the @DefaultValue annotation
        errors
                .addAll(Stream
                        .concat(finder.findAnnotatedFields(DefaultValue.class).stream(),
                                finder.findAnnotatedConstructorParameters(DefaultValue.class).stream())
                        .map(d -> {
                            final DefaultValue annotation = d.getAnnotation(DefaultValue.class);
                            if (annotation.value().startsWith("local_configuration:") && !annotation
                                    .value()
                                    .toLowerCase(Locale.ROOT)
                                    .startsWith("local_configuration:" + family + ".")) {
                                return d + " does not start with family name (followed by a dot): '" + family + "'";
                            }
                            return null;
                        })
                        .filter(Objects::nonNull)
                        .collect(toSet()));
    }

    private void validateLayout(final List<Class<?>> components, final Set<String> errors) {

        components
                .stream()
                .map(c -> parameterModelService
                        .buildParameterMetas(findConstructor(c),
                                ofNullable(c.getPackage()).map(Package::getName).orElse(""),
                                new BaseParameterEnricher.Context(new LocalConfigurationService(emptyList(), "tools"))))
                .flatMap(this::toFlatNonPrimitivConfig)
                .collect(toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue, (p1, p2) -> p1))
                .entrySet()
                .forEach(c -> visitLayout(c, errors));
    }

    private void visitLayout(final Map.Entry<String, ParameterMeta> config, final Set<String> errors) {

        final Set<String> fieldsInGridLayout = config
                .getValue()
                .getMetadata()
                .entrySet()
                .stream()
                .filter(meta -> meta.getKey().startsWith("tcomp::ui::gridlayout"))
                .flatMap(meta -> of(meta.getValue().split("\\|")))
                .flatMap(s -> of(s.split(",")))
                .filter(s -> !s.isEmpty())
                .sorted()
                .collect(toSet());

        final Set<String> fieldsInOptionOrder = config
                .getValue()
                .getMetadata()
                .entrySet()
                .stream()
                .filter(meta -> meta.getKey().startsWith("tcomp::ui::optionsorder"))
                .flatMap(meta -> of(meta.getValue().split(",")))
                .sorted()
                .collect(toSet());

        if (fieldsInGridLayout.isEmpty() && fieldsInOptionOrder.isEmpty()) {
            return;
        }

        if (!fieldsInGridLayout.isEmpty() && !fieldsInOptionOrder.isEmpty()) {
            this.log.error("Concurrent layout found for '" + config.getKey() + "', the @OptionsOrder will be ignored.");
        }

        if (!fieldsInGridLayout.isEmpty()) {
            errors
                    .addAll(fieldsInGridLayout
                            .stream()
                            .filter(fieldInLayout -> config
                                    .getValue()
                                    .getNestedParameters()
                                    .stream()
                                    .map(ParameterMeta::getName)
                                    .noneMatch(field -> field.equals(fieldInLayout)))
                            .map(fieldInLayout -> "Option '" + fieldInLayout
                                    + "' in @GridLayout doesn't exist in declaring class '" + config.getKey() + "'")
                            .sorted()
                            .collect(toSet()));

            config
                    .getValue()
                    .getNestedParameters()
                    .stream()
                    .filter(field -> !fieldsInGridLayout.contains(field.getName()))
                    .map(field -> "Field '" + field.getName() + "' in " + config.getKey()
                            + " is not declared in any layout.")
                    .forEach(this.log::error);
        } else {
            errors
                    .addAll(fieldsInOptionOrder
                            .stream()
                            .filter(fieldInLayout -> config
                                    .getValue()
                                    .getNestedParameters()
                                    .stream()
                                    .map(ParameterMeta::getName)
                                    .noneMatch(field -> field.equals(fieldInLayout)))
                            .map(fieldInLayout -> "Option '" + fieldInLayout
                                    + "' in @OptionOrder doesn't exist in declaring class '" + config.getKey() + "'")
                            .sorted()
                            .collect(toSet()));

            config
                    .getValue()
                    .getNestedParameters()
                    .stream()
                    .filter(field -> !fieldsInOptionOrder.contains(field.getName()))
                    .map(field -> "Field '" + field.getName() + "' in " + config.getKey()
                            + " is not declared in any layout.")
                    .forEach(this.log::error);
        }

    }

    private Stream<AbstractMap.SimpleEntry<String, ParameterMeta>>
            toFlatNonPrimitivConfig(final List<ParameterMeta> config) {
        if (config == null || config.isEmpty()) {
            return empty();
        }

        return config
                .stream()
                .filter(Objects::nonNull)
                .filter(p -> OBJECT.equals(p.getType()) || isArrayOfObject(p))
                .filter(p -> p.getNestedParameters() != null)
                .flatMap(p -> concat(of(new AbstractMap.SimpleEntry<>(toJavaType(p).getName(), p)),
                        toFlatNonPrimitivConfig(p.getNestedParameters())));
    }

    private Class<?> toJavaType(final ParameterMeta p) {
        if (p.getType().equals(OBJECT) || p.getType().equals(ENUM)) {
            if (Class.class.isInstance(p.getJavaType())) {
                return Class.class.cast(p.getJavaType());
            }
            throw new IllegalArgumentException("Unsupported type for parameter " + p.getPath() + " (from "
                    + p.getSource().declaringClass() + "), ensure it is a Class<?>");
        }

        if (p.getType().equals(ARRAY) && ParameterizedType.class.isInstance(p.getJavaType())) {
            final ParameterizedType parameterizedType = ParameterizedType.class.cast(p.getJavaType());
            final Type[] arguments = parameterizedType.getActualTypeArguments();
            if (arguments.length == 1 && Class.class.isInstance(arguments[0])) {
                return Class.class.cast(arguments[0]);
            }
            throw new IllegalArgumentException("Unsupported type for parameter " + p.getPath() + " (from "
                    + p.getSource().declaringClass() + "), " + "ensure it is a ParameterizedType with one argument");
        }

        throw new IllegalStateException("Parameter '" + p.getName() + "' is not an object.");
    }

    private boolean isArrayOfObject(final ParameterMeta param) {

        return ARRAY.equals(param.getType()) && param.getNestedParameters() != null
                && param
                        .getNestedParameters()
                        .stream()
                        .anyMatch(p -> OBJECT.equals(p.getType()) || ENUM.equals(p.getType()) || isArrayOfObject(p));

    }

    private void validateOptionNames(final AnnotationFinder finder, final Set<String> errors) {
        errors.addAll(finder.findAnnotatedFields(Option.class).stream().filter(field -> {
            final String name = field.getAnnotation(Option.class).value();
            return name.contains(".") || name.startsWith("$");
        }).distinct().map(field -> {
            final String name = field.getAnnotation(Option.class).value();
            return "Option name `" + name
                    + "` is invalid, you can't start an option name with a '$' and it can't contain a '.'. "
                    + "Please fix it on field `" + field.getDeclaringClass().getName() + "#" + field.getName() + "`";
        }).sorted().collect(toSet()));
    }

    private void validateDocumentation(final AnnotationFinder finder, final List<Class<?>> components,
            final Set<String> errors) {
        errors
                .addAll(components
                        .stream()
                        .filter(c -> !c.isAnnotationPresent(Documentation.class))
                        .map(c -> "No @Documentation on '" + c.getName() + "'")
                        .sorted()
                        .collect(toSet()));
        errors
                .addAll(finder
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
        errors
                .addAll(components
                        .stream()
                        .map(this::validateComponentResourceBundle)
                        .filter(Objects::nonNull)
                        .sorted()
                        .collect(toSet()));

        // enum
        errors
                .addAll(finder
                        .findAnnotatedFields(Option.class)
                        .stream()
                        .map(Field::getType)
                        .filter(Class::isEnum)
                        .distinct()
                        .flatMap(enumType -> Stream
                                .of(enumType.getFields())
                                .filter(f -> Modifier.isStatic(f.getModifiers()) && Modifier.isFinal(f.getModifiers()))
                                .filter(f -> {
                                    final ResourceBundle bundle = ofNullable(findResourceBundle(enumType))
                                            .orElseGet(() -> findResourceBundle(f.getDeclaringClass()));
                                    final String key = enumType.getSimpleName() + "." + f.getName() + "._displayName";
                                    return bundle == null || !bundle.containsKey(key);
                                })
                                .map(f -> "Missing key " + enumType.getSimpleName() + "." + f.getName()
                                        + "._displayName in " + enumType + " resource bundle"))
                        .sorted()
                        .collect(toSet()));

        // others - just logged for now, we can add it to errors if we encounter it too often
        final List<String> missingOptionTranslations =
                finder.findAnnotatedFields(Option.class).stream().distinct().filter(field -> {
                    final ResourceBundle bundle = ofNullable(findResourceBundle(field.getDeclaringClass()))
                            .orElseGet(() -> findResourceBundle(field.getType()));
                    final String key =
                            field.getDeclaringClass().getSimpleName() + "." + field.getName() + "._displayName";
                    return bundle == null || !bundle.containsKey(key);
                })
                        .map(f -> " " + f.getDeclaringClass().getSimpleName() + "." + f.getName() + "._displayName = <"
                                + f.getName() + ">")
                        .sorted()
                        .collect(toList());
        if (!missingOptionTranslations.isEmpty()) {
            // represent it as a single entry to enable "copy/paste fixed" pattern
            errors
                    .add(missingOptionTranslations
                            .stream()
                            .distinct()
                            .collect(joining("\n", "Missing resource bundle entries:\n", "")));
        }

        for (final Class<?> i : finder.findAnnotatedClasses(Internationalized.class)) {
            final ResourceBundle resourceBundle = findResourceBundle(i);
            if (resourceBundle != null) {
                final Collection<String> keys = of(i.getMethods())
                        .filter(m -> m.getDeclaringClass() != Object.class)
                        .map(m -> i.getName() + "." + m.getName())
                        .sorted()
                        .collect(toSet());
                errors
                        .addAll(keys
                                .stream()
                                .filter(k -> !resourceBundle.containsKey(k))
                                .map(k -> "Missing key " + k + " in " + i + " resource bundle")
                                .sorted()
                                .collect(toSet()));

                errors
                        .addAll(resourceBundle
                                .keySet()
                                .stream()
                                .filter(k -> k.startsWith(i.getName() + ".") && !keys.contains(k))
                                .map(k -> "Key " + k + " from " + i + " is no more used")
                                .sorted()
                                .collect(toSet()));
            } else {
                errors.add("No resource bundle for " + i);
            }
        }

        errors.addAll(getActionsStream().flatMap(action -> finder.findAnnotatedMethods(action).stream()).map(action -> {
            final Annotation actionAnnotation = Stream
                    .of(action.getAnnotations())
                    .filter(a -> a.annotationType().isAnnotationPresent(ActionType.class))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("No action annotation on " + action));
            final String key;
            try {
                final Class<? extends Annotation> annotationType = actionAnnotation.annotationType();
                key = "${family}.actions." + annotationType.getAnnotation(ActionType.class).value() + "."
                        + annotationType.getMethod("value").invoke(actionAnnotation).toString() + "._displayName";
            } catch (final IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                return null;
            }
            return validateFamilyI18nKey(action.getDeclaringClass(), key);
        }).filter(Objects::nonNull).collect(toSet()));
    }

    private void validateHttpClient(final AnnotationFinder finder, final Set<String> errors) {
        errors
                .addAll(finder
                        .findAnnotatedClasses(Request.class)
                        .stream()
                        .map(Class::getDeclaringClass)
                        .distinct()
                        .flatMap(c -> HttpClientFactoryImpl.createErrors(c).stream())
                        .sorted()
                        .collect(toSet()));
    }

    private void validateModel(final AnnotationFinder finder, final List<Class<?>> components,
            final Set<String> errors) {
        errors
                .addAll(components
                        .stream()
                        .filter(c -> componentMarkers().filter(c::isAnnotationPresent).count() > 1)
                        .map(i -> i + " has conflicting component annotations, ensure it has a single one")
                        .sorted()
                        .collect(toSet()));

        errors
                .addAll(components
                        .stream()
                        .filter(c -> countParameters(findConstructor(c).getParameters()) > 1)
                        .map(c -> "Component must use a single root option. '" + c.getName() + "'")
                        .sorted()
                        .collect(toSet()));

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
        }).filter(Objects::nonNull).sorted().collect(toSet()));

        // limited config types
        errors
                .addAll(finder
                        .findAnnotatedFields(Structure.class)
                        .stream()
                        .filter(f -> !ParameterizedType.class.isInstance(f.getGenericType())
                                || (!isListString(f) && !isMapString(f)))
                        .map(f -> f.getDeclaringClass() + "#" + f.getName()
                                + " uses @Structure but is not a List<String> nor a Map<String, String>")
                        .sorted()
                        .collect(toSet()));
    }

    private boolean isMapString(final Field f) {
        final ParameterizedType pt = ParameterizedType.class.cast(f.getGenericType());
        return (Map.class == pt.getRawType()) && pt.getActualTypeArguments().length == 2
                && pt.getActualTypeArguments()[0] == String.class && pt.getActualTypeArguments()[1] == String.class;
    }

    private boolean isListString(final Field f) {
        final ParameterizedType pt = ParameterizedType.class.cast(f.getGenericType());
        return ((List.class == pt.getRawType()) || (Collection.class == pt.getRawType()))
                && pt.getActualTypeArguments().length == 1 && pt.getActualTypeArguments()[0] == String.class;
    }

    private void validateMetadata(final List<Class<?>> components, final Set<String> errors) {
        errors.addAll(components.stream().map(component -> {
            final Icon icon = component.getAnnotation(Icon.class);
            if (!component.isAnnotationPresent(Version.class) || icon == null) {
                return "Component " + component + " should use @Icon and @Version";
            }
            return validateIcon(icon);
        }).filter(Objects::nonNull).sorted().collect(toSet()));
    }

    private void validateDataStore(final AnnotationFinder finder, final Set<String> errors) {
        final List<Class<?>> datastoreClasses = finder.findAnnotatedClasses(DataStore.class);

        final List<String> datastores =
                datastoreClasses.stream().map(d -> d.getAnnotation(DataStore.class).value()).collect(toList());

        Set<String> uniqueDatastores = new HashSet<>(datastores);
        if (datastores.size() != uniqueDatastores.size()) {
            errors
                    .add("Duplicated DataStore found : " + datastores
                            .stream()
                            .collect(groupingBy(identity()))
                            .entrySet()
                            .stream()
                            .filter(e -> e.getValue().size() > 1)
                            .map(Map.Entry::getKey)
                            .collect(joining(", ")));
        }

        final List<Class<?>> checkableClasses = finder.findAnnotatedClasses(Checkable.class);
        errors
                .addAll(checkableClasses
                        .stream()
                        .filter(d -> !d.isAnnotationPresent(DataStore.class))
                        .map(c -> c.getName() + " has @Checkable but is not a @DataStore")
                        .sorted()
                        .collect(toSet()));

        final Map<String, String> checkableDataStoresMap = checkableClasses
                .stream()
                .filter(d -> d.isAnnotationPresent(DataStore.class))
                .collect(toMap(d -> d.getAnnotation(DataStore.class).value(),
                        d -> d.getAnnotation(Checkable.class).value()));

        final Set<String> healthchecks = finder
                .findAnnotatedMethods(HealthCheck.class)
                .stream()
                .filter(h -> h.getDeclaringClass().isAnnotationPresent(Service.class))
                .map(m -> m.getAnnotation(HealthCheck.class).value())
                .sorted()
                .collect(toSet());
        errors
                .addAll(checkableDataStoresMap
                        .entrySet()
                        .stream()
                        .filter(e -> !healthchecks.contains(e.getValue()))
                        .map(e -> "No @HealthCheck for dataStore: '" + e.getKey() + "' with checkable: '" + e.getValue()
                                + "'")
                        .sorted()
                        .collect(toSet()));

        errors
                .addAll(datastoreClasses
                        .stream()
                        .map(clazz -> validateFamilyI18nKey(clazz,
                                "${family}.datastore." + clazz.getAnnotation(DataStore.class).value()
                                        + "._displayName"))
                        .filter(Objects::nonNull)
                        .collect(toList()));
    }

    private void validateDataSet(final AnnotationFinder finder, final List<Class<?>> components,
            final Set<String> errors) {
        final List<Class<?>> datasetClasses = finder.findAnnotatedClasses(DataSet.class);
        final Map<Class<?>, String> datasets =
                datasetClasses.stream().collect(toMap(identity(), d -> d.getAnnotation(DataSet.class).value()));
        final Set<String> uniqueDatasets = new HashSet<>(datasets.values());
        if (datasets.size() != uniqueDatasets.size()) {
            errors
                    .add("Duplicated DataSet found : " + datasets
                            .values()
                            .stream()
                            .collect(groupingBy(identity()))
                            .entrySet()
                            .stream()
                            .filter(e -> e.getValue().size() > 1)
                            .map(Map.Entry::getKey)
                            .collect(joining(", ")));
        }
        errors
                .addAll(datasets
                        .entrySet()
                        .stream()
                        .map(entry -> validateFamilyI18nKey(entry.getKey(),
                                "${family}.dataset." + entry.getValue() + "._displayName"))
                        .filter(Objects::nonNull)
                        .collect(toList()));

        // ensure there is always a source with a config matching without user entries each dataset
        final BaseParameterEnricher.Context context =
                new BaseParameterEnricher.Context(new LocalConfigurationService(emptyList(), "tools"));
        final Map<Class<?>, Collection<ParameterMeta>> componentNeedingADataSet = components
                .stream()
                .filter(c -> isSource(c) || isOutput(c))
                .collect(toMap(identity(),
                        c -> parameterModelService
                                .buildParameterMetas(findConstructor(c),
                                        ofNullable(c.getPackage()).map(Package::getName).orElse(""), context)));

        final Map<? extends Class<?>, Collection<ParameterMeta>> inputs = componentNeedingADataSet
                .entrySet()
                .stream()
                .filter(it -> isSource(it.getKey()))
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));

        errors
                .addAll(datasets
                        .entrySet()
                        .stream()
                        .filter(dataset -> inputs.isEmpty() || inputs.entrySet().stream().anyMatch(input -> {
                            final Collection<ParameterMeta> allProps = flatten(input.getValue()).collect(toList());
                            final Collection<ParameterMeta> datasetProperties =
                                    findNestedDataSets(allProps, dataset.getValue()).collect(toList());
                            return !datasetProperties.isEmpty() && allProps
                                    .stream()
                                    // .filter(it -> it.getType() != OBJECT && it.getType() != ARRAY) // should it be
                                    // done?
                                    .filter(it -> datasetProperties
                                            .stream()
                                            .noneMatch(dit -> it.getPath().equals(dit.getPath())
                                                    || it.getPath().startsWith(dit.getPath() + '.')))
                                    .anyMatch(this::isRequired);
                        }))
                        .map(dataset -> "No source instantiable without adding parameters for @DataSet(\""
                                + dataset.getValue() + "\") (" + dataset.getKey().getName()
                                + "), please ensure at least a source using this "
                                + "dataset can be used just filling the dataset information.")
                        .sorted()
                        .collect(toSet()));

        // "cloud" rule - ensure all input/output have a dataset at least
        errors
                .addAll(componentNeedingADataSet
                        .entrySet()
                        .stream()
                        .filter(it -> flatten(it.getValue())
                                .noneMatch(prop -> "dataset"
                                        .equals(prop.getMetadata().get("tcomp::configurationtype::type"))))
                        .map(it -> "The component " + it.getKey().getName()
                                + " is missing a dataset in its configuration (see @DataSet)")
                        .sorted()
                        .collect(toSet()));

        // "cloud" rule - ensure all datasets have a datastore
        errors.addAll(datasetClasses.stream().map(ds -> {
            final List<ParameterMeta> dataset = parameterModelService
                    .buildParameterMetas(Stream.of(new ParameterModelService.Param(ds, ds.getAnnotations(), "dataset")),
                            ds, ofNullable(ds.getPackage()).map(Package::getName).orElse(""), true, context);
            if (flatten(dataset)
                    .noneMatch(prop -> "datastore".equals(prop.getMetadata().get("tcomp::configurationtype::type")))) {
                return "The dataset " + ds.getName()
                        + " is missing a datastore reference in its configuration (see @DataStore)";
            }
            return null;
        }).filter(Objects::nonNull).sorted().collect(toSet()));
    }

    private boolean isRequired(final ParameterMeta parameterMeta) {
        return Boolean.parseBoolean(parameterMeta.getMetadata().getOrDefault("tcomp::validation::required", "false"));
    }

    private boolean isSource(final Class<?> component) {
        return component.isAnnotationPresent(PartitionMapper.class) || component.isAnnotationPresent(Emitter.class);
    }

    private boolean isOutput(final Class<?> component) {
        return component.isAnnotationPresent(Processor.class) && Stream
                .of(component.getMethods())
                .filter(it -> it.isAnnotationPresent(ElementListener.class) || it.isAnnotationPresent(AfterGroup.class))
                .allMatch(it -> void.class == it.getReturnType()
                        && Stream.of(it.getParameters()).noneMatch(param -> param.isAnnotationPresent(Output.class)));
    }

    private Stream<ParameterMeta> findNestedDataSets(final Collection<ParameterMeta> options, final String name) {
        return options
                .stream()
                .filter(it -> "dataset".equals(it.getMetadata().get("tcomp::configurationtype::type"))
                        && name.equals(it.getMetadata().get("tcomp::configurationtype::name")));
    }

    private Stream<ParameterMeta> flatten(final Collection<ParameterMeta> options) {
        return options.stream().flatMap(it -> Stream.concat(Stream.of(it), it.getNestedParameters().stream()));
    }

    private String validateFamilyI18nKey(final Class<?> clazz, final String... keys) {
        final Class<?> pck = findPackageOrFail(clazz, Components.class);
        final String family = pck.getAnnotation(Components.class).family();
        final String baseName = ofNullable(pck.getPackage()).map(p -> p.getName() + ".").orElse("") + "Messages";
        final ResourceBundle bundle = findResourceBundle(pck);
        if (bundle == null) {
            return "No resource bundle for " + clazz.getName() + " translations, you should create a "
                    + baseName.replace('.', '/') + ".properties at least.";
        }

        final Collection<String> missingKeys = of(keys)
                .map(key -> key.replace("${family}", family))
                .filter(k -> !bundle.containsKey(k))
                .collect(toList());
        if (!missingKeys.isEmpty()) {
            return baseName + " is missing the key(s): " + String.join("\n", missingKeys);
        }
        return null;
    }

    private void validateActions(final AnnotationFinder finder, final Set<String> errors) {
        // returned types
        errors.addAll(getActionsStream().flatMap(action -> {
            final Class<?> returnedType = action.getAnnotation(ActionType.class).expectedReturnedType();
            final List<Method> annotatedMethods = finder.findAnnotatedMethods(action);
            return Stream
                    .concat(annotatedMethods
                            .stream()
                            .filter(m -> !returnedType.isAssignableFrom(m.getReturnType()))
                            .map(m -> m + " doesn't return a " + returnedType + ", please fix it"),
                            annotatedMethods
                                    .stream()
                                    .filter(m -> !m.getDeclaringClass().isAnnotationPresent(Service.class)
                                            && !Modifier.isAbstract(m.getDeclaringClass().getModifiers()))
                                    .map(m -> m + " is not declared into a service class"));
        }).sorted().collect(toSet()));

        // parameters for @DynamicValues
        errors
                .addAll(finder
                        .findAnnotatedMethods(DynamicValues.class)
                        .stream()
                        .filter(m -> countParameters(m) != 0)
                        .map(m -> m + " should have no parameter")
                        .sorted()
                        .collect(toSet()));

        // parameters for @HealthCheck
        errors
                .addAll(finder
                        .findAnnotatedMethods(HealthCheck.class)
                        .stream()
                        .filter(m -> countParameters(m) != 1
                                || !m.getParameterTypes()[0].isAnnotationPresent(DataStore.class))
                        .map(m -> m + " should have its first parameter being a datastore (marked with @DataStore)")
                        .sorted()
                        .collect(toSet()));

        // parameters for @DiscoverSchema
        errors
                .addAll(finder
                        .findAnnotatedMethods(DiscoverSchema.class)
                        .stream()
                        .filter(m -> countParameters(m) != 1
                                || !m.getParameterTypes()[0].isAnnotationPresent(DataSet.class))
                        .map(m -> m + " should have its first parameter being a dataset (marked with @DataSet)")
                        .sorted()
                        .collect(toSet()));

        // returned type for @Update, for now limit it on objects and not primitives
        final Map<String, Method> updates = finder
                .findAnnotatedMethods(Update.class)
                .stream()
                .collect(toMap(m -> m.getAnnotation(Update.class).value(), identity()));
        errors
                .addAll(updates
                        .values()
                        .stream()
                        .filter(m -> isPrimitiveLike(m.getReturnType()))
                        .map(m -> m + " should return an object")
                        .sorted()
                        .collect(toSet()));
        final List<Field> updatableFields = finder.findAnnotatedFields(Updatable.class);
        errors
                .addAll(updatableFields
                        .stream()
                        .filter(f -> f.getAnnotation(Updatable.class).after().contains(".") /* no '..' or '.' */)
                        .map(f -> "@Updatable.after should only reference direct child primitive fields")
                        .sorted()
                        .collect(toSet()));
        errors
                .addAll(updatableFields
                        .stream()
                        .filter(f -> isPrimitiveLike(f.getType()))
                        .map(f -> "@Updatable should not be used on primitives: " + f)
                        .sorted()
                        .collect(toSet()));
        errors.addAll(updatableFields.stream().map(f -> {
            final Method service = updates.get(f.getAnnotation(Updatable.class).value());
            if (service == null) {
                return null; // another error will mention it
            }
            if (f.getType().isAssignableFrom(service.getReturnType())) {
                return null; // no error
            }
            return "@Updatable field '" + f + "' does not match returned type of '" + service + "'";
        }).filter(Objects::nonNull).sorted().collect(toSet()));
        errors
                .addAll(updatableFields
                        .stream()
                        .filter(f -> updates.get(f.getAnnotation(Updatable.class).value()) == null)
                        .map(f -> "No @Update service found for field " + f + ", did you intend to use @Updatable?")
                        .sorted()
                        .collect(toSet()));

        errors
                .addAll(finder
                        .findAnnotatedFields(Proposable.class)
                        .stream()
                        .filter(f -> f.getType().isEnum())
                        .map(f -> f.toString() + " must not define @Proposable since it is an enum")
                        .sorted()
                        .collect(toSet()));

        final Set<String> proposables = finder
                .findAnnotatedFields(Proposable.class)
                .stream()
                .map(f -> f.getAnnotation(Proposable.class).value())
                .sorted()
                .collect(toSet());
        final Set<String> dynamicValues = finder
                .findAnnotatedMethods(DynamicValues.class)
                .stream()
                .map(f -> f.getAnnotation(DynamicValues.class).value())
                .sorted()
                .collect(toSet());
        proposables.removeAll(dynamicValues);
        errors
                .addAll(proposables
                        .stream()
                        .map(p -> "No @DynamicValues(\"" + p + "\"), add a service with this method: "
                                + "@DynamicValues(\"" + p + "\") Values proposals();")
                        .sorted()
                        .collect(toSet()));
    }

    private Stream<Class<? extends Annotation>> getActionsStream() {
        return of(AsyncValidation.class, DynamicValues.class, HealthCheck.class, DiscoverSchema.class,
                Suggestions.class, Update.class);
    }

    private boolean isPrimitiveLike(final Class<?> type) {
        return type.isPrimitive() || type == String.class;
    }

    private int countParameters(final Method m) {
        return countParameters(m.getParameters());
    }

    private int countParameters(final Parameter[] params) {
        return (int) Stream
                .of(params)
                .filter(p -> !parameterModelService.isService(new ParameterModelService.Param(p)))
                .count();
    }

    private String validateComponentResourceBundle(final Class<?> component) {
        final String baseName = ofNullable(component.getPackage()).map(p -> p.getName() + ".").orElse("") + "Messages";
        final ResourceBundle bundle = findResourceBundle(component);
        if (bundle == null) {
            return "No resource bundle for " + component.getName() + ", you should create a "
                    + baseName.replace('.', '/') + ".properties at least.";
        }

        final String prefix = components(component)
                .map(c -> findFamily(c, component) + "." + c.name())
                .orElseThrow(() -> new IllegalStateException(component.getName()));
        final Collection<String> missingKeys =
                of("_displayName").map(n -> prefix + "." + n).filter(k -> !bundle.containsKey(k)).collect(toList());
        if (!missingKeys.isEmpty()) {
            return baseName + " is missing the key(s): " + String.join("\n", missingKeys);
        }
        return null;
    }

    private boolean isSerializable(final Class<?> aClass) {
        return Serializable.class.isAssignableFrom(aClass);
    }

    private static <T> Collector<T, ?, Set<T>> toSet() {
        return toCollection(TreeSet::new);
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

        private boolean validateLayout;

        private boolean validateOptionNames;

        private boolean validateLocalConfiguration;

        private boolean validateOutputConnection;
    }
}
