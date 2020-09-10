/**
 * Copyright (C) 2006-2020 Talend Inc. - www.talend.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.talend.sdk.component.tools;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
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
import static org.talend.sdk.component.runtime.manager.ParameterMeta.Type.STRING;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.xbean.finder.AnnotationFinder;
import org.talend.sdk.component.api.component.Components;
import org.talend.sdk.component.api.component.Icon;
import org.talend.sdk.component.api.component.MigrationHandler;
import org.talend.sdk.component.api.component.Version;
import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.configuration.action.Checkable;
import org.talend.sdk.component.api.configuration.action.Proposable;
import org.talend.sdk.component.api.configuration.action.Updatable;
import org.talend.sdk.component.api.configuration.type.DataSet;
import org.talend.sdk.component.api.configuration.type.DataStore;
import org.talend.sdk.component.api.configuration.ui.DefaultValue;
import org.talend.sdk.component.api.configuration.ui.widget.Structure;
import org.talend.sdk.component.api.exception.ComponentException;
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
import org.talend.sdk.component.runtime.internationalization.ParameterBundle;
import org.talend.sdk.component.runtime.manager.ParameterMeta;
import org.talend.sdk.component.runtime.manager.component.AbstractMigrationHandler;
import org.talend.sdk.component.runtime.manager.reflect.IconFinder;
import org.talend.sdk.component.runtime.manager.reflect.ParameterModelService;
import org.talend.sdk.component.runtime.manager.reflect.parameterenricher.BaseParameterEnricher;
import org.talend.sdk.component.runtime.manager.service.LocalConfigurationService;
import org.talend.sdk.component.runtime.manager.service.http.HttpClientFactoryImpl;
import org.talend.sdk.component.runtime.manager.xbean.registry.EnrichedPropertyEditorRegistry;
import org.talend.sdk.component.runtime.visitor.ModelListener;
import org.talend.sdk.component.runtime.visitor.ModelVisitor;
import org.talend.sdk.component.tools.spi.ValidationExtension;

import lombok.Data;

// IMPORTANT: this class is used by reflection in gradle integration, don't break signatures without checking it
public class ComponentValidator extends BaseTask {

    private final Configuration configuration;

    private final Log log;

    private final ParameterModelService parameterModelService =
            new ParameterModelService(new EnrichedPropertyEditorRegistry());

    private final SvgValidator validator = new SvgValidator();

    private final Map<Class<?>, List<ParameterMeta>> parametersCache = new HashMap<>();

    private final List<ValidationExtension> extensions;

    public ComponentValidator(final Configuration configuration, final File[] classes, final Object log) {
        super(classes);
        this.configuration = configuration;

        try {
            this.log = Log.class.isInstance(log) ? Log.class.cast(log) : new ReflectiveLog(log);
        } catch (final NoSuchMethodException e) {
            throw new IllegalArgumentException(e);
        }
        this.extensions = StreamSupport
                .stream(ServiceLoader.load(ValidationExtension.class).spliterator(), false)
                .collect(toList());
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
                    final Icon icon =
                            findPackageOrFail(c, apiTester(Icon.class), Icon.class.getName()).getAnnotation(Icon.class);
                    ofNullable(validateIcon(icon, errors)).ifPresent(errors::add);
                } catch (final IllegalArgumentException iae) {
                    final IconFinder iconFinder = new IconFinder();
                    try {
                        findPackageOrFail(c, it -> iconFinder.findIndirectIcon(it).isPresent(), Icon.class.getName());
                    } catch (final IllegalArgumentException iae2) {
                        errors.add(iae.getMessage());
                    }
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
            validateLocalConfiguration(finder,
                    Optional.of(configuration).map(Configuration::getPluginId).orElseGet(this::guessPluginId), errors);
        }

        if (configuration.isValidateOutputConnection()) {
            validateOutputConnection(components, errors);
        }

        if (configuration.isValidatePlaceholder()) {
            validatePlaceholders(components, errors);
        }

        if (configuration.isValidateNoFinalOption()) {
            validateNoFinalOption(finder, errors);
        }

        if (configuration.isValidateWording()) {
            if (configuration.isValidateDocumentation()) {
                validateDocumentationWording(finder, components, errors);
            }
            if (configuration.isValidateInternationalization()) {
                validateInternationalizationWording(finder, components, errors);
            }
        }

        if (configuration.isValidateExceptions()) {
            validateExceptions(errors);
        }

        if (configuration.isValidateMigrations()) {
            // Migration handlers should extends AbstractMigrationHandler !!!
            errors.addAll(finder.findAnnotatedClasses(Version.class).stream().map(c -> {
                Class<?> migration = c.getAnnotation(Version.class).migrationHandler();
                if (migration == MigrationHandler.class || AbstractMigrationHandler.class.isAssignableFrom(migration)) {
                    return null;
                }
                return String
                        .format("Migration %s should inherit from %s.", c.getName(),
                                AbstractMigrationHandler.class.getCanonicalName());
            }).filter(Objects::nonNull).sorted().collect(toSet()));
        }

        if (!extensions.isEmpty()) {
            final ValidationExtension.ValidationContext context = new ValidationExtension.ValidationContext() {

                @Override
                public AnnotationFinder finder() {
                    return finder;
                }

                @Override
                public List<Class<?>> components() {
                    return components;
                }

                @Override
                public List<ParameterMeta> parameters(final Class<?> component) {
                    return buildOrGetParameters(component);
                }
            };
            errors
                    .addAll(extensions
                            .stream()
                            .map(extension -> extension.validate(context))
                            .filter(result -> result.getErrors() != null)
                            .flatMap(result -> result.getErrors().stream())
                            .filter(Objects::nonNull)
                            .collect(toList()));
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

    private String guessPluginId() { // assume folder name == module id
        return ofNullable(classes).flatMap(c -> Stream.of(c).map(f -> {
            if (!f.isDirectory()) {
                return null;
            }
            File current = f;
            int iteration = 5;
            while (iteration-- > 0 && current != null) {
                final File currentRef = current;
                if (Stream
                        .of("classes", "target", "main", "java", "build")
                        .anyMatch(it -> it.equals(currentRef.getName()))) {
                    current = current.getParentFile();
                } else {
                    return current.getName();
                }
            }
            return null;
        }).filter(Objects::nonNull).findFirst()).orElseThrow(() -> new IllegalArgumentException("No pluginId set"));
    }

    private void validatePlaceholders(final List<Class<?>> components, final Set<String> errors) {
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        errors
                .addAll(components
                        .stream()
                        .map(this::buildOrGetParameters)
                        .flatMap(this::flatten)
                        .filter(this::isStringifiable)
                        .filter(p -> !hasPlaceholder(loader, p, null))
                        .map(p -> "No _placeholder set for " + p.getPath() + " in Messages.properties of packages: "
                                + asList(p.getI18nPackages()))
                        .collect(toSet()));
    }

    private String validateIcon(final Icon annotation, final Collection<String> errors) {
        if (classes.length == 0) {
            return null;
        }

        if (annotation.value() == Icon.IconType.CUSTOM) {
            final String icon = annotation.custom();
            final Set<File> svgs =
                    of(classes).map(it -> new File(it, "icons/" + icon + ".svg")).filter(File::exists).collect(toSet());
            if (svgs.isEmpty()) {
                log.error("No 'icons/" + icon + ".svg' found, this will run in degraded mode in Talend Cloud");
            } else {
                if (configuration.isValidateSvg()) {
                    errors.addAll(svgs.stream().flatMap(this::validateSvg).collect(toSet()));
                }
            }
            if (Stream.of(classes).map(it -> new File(it, "icons/" + icon + "_icon32.png")).noneMatch(File::exists)) {
                return "No icon: '" + icon + "' found, did you create - or generated with svg2png - 'icons/" + icon
                        + "_icon32.png' in resources?";
            }
        }
        return null;
    }

    private Stream<String> validateSvg(final File file) {
        return validator.validate(file.toPath());
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

    private void validateLocalConfiguration(final AnnotationFinder finder, final String pluginId,
            final Set<String> errors) {

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
                                    .filter(it -> !it.toLowerCase(Locale.ROOT).startsWith(pluginId + "."))
                                    .map(it -> "'" + it + "' does not start with '" + pluginId + "', "
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
                                    .startsWith("local_configuration:" + pluginId + ".")) {
                                return d + " does not start with family name (followed by a dot): '" + pluginId + "'";
                            }
                            return null;
                        })
                        .filter(Objects::nonNull)
                        .collect(toSet()));
    }

    private void validateLayout(final List<Class<?>> components, final Set<String> errors) {
        components
                .stream()
                .map(this::buildOrGetParameters)
                .flatMap(this::toFlatNonPrimitiveConfig)
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

    private boolean hasPlaceholder(final ClassLoader loader, final ParameterMeta parameterMeta,
            final ParameterBundle parent) {
        return parameterMeta.findBundle(loader, Locale.ROOT).placeholder(parent).isPresent();
    }

    private List<ParameterMeta> buildOrGetParameters(final Class<?> c) {
        return parametersCache
                .computeIfAbsent(c,
                        k -> parameterModelService
                                .buildParameterMetas(findConstructor(c),
                                        ofNullable(c.getPackage()).map(Package::getName).orElse(""),
                                        new BaseParameterEnricher.Context(
                                                new LocalConfigurationService(emptyList(), "tools"))));
    }

    private boolean isStringifiable(final ParameterMeta meta) {
        return STRING.equals(meta.getType()) || ENUM.equals(meta.getType());
    }

    private Stream<AbstractMap.SimpleEntry<String, ParameterMeta>>
            toFlatNonPrimitiveConfig(final List<ParameterMeta> config) {
        if (config == null || config.isEmpty()) {
            return empty();
        }
        return config
                .stream()
                .filter(Objects::nonNull)
                .filter(p -> OBJECT.equals(p.getType()) || isArrayOfObject(p))
                .filter(p -> p.getNestedParameters() != null)
                .flatMap(p -> concat(of(new AbstractMap.SimpleEntry<>(toJavaType(p).getName(), p)),
                        toFlatNonPrimitiveConfig(p.getNestedParameters())));
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
        errors.addAll(findOptions(finder).filter(field -> {
            final String name = field.getAnnotation(Option.class).value();
            return name.contains(".") || name.startsWith("$");
        }).distinct().map(field -> {
            final String name = field.getAnnotation(Option.class).value();
            return "Option name `" + name
                    + "` is invalid, you can't start an option name with a '$' and it can't contain a '.'. "
                    + "Please fix it on field `" + field.getDeclaringClass().getName() + "#" + field.getName() + "`";
        }).sorted().collect(toSet()));
    }

    private void validateNoFinalOption(final AnnotationFinder finder, final Set<String> errors) {
        errors
                .addAll(findOptions(finder)
                        .filter(field -> Modifier.isFinal(field.getModifiers()))
                        .distinct()
                        .map(field -> "@Option fields must not be final, found one field violating this rule: " + field)
                        .sorted()
                        .collect(toSet()));
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
                .addAll(findOptions(finder)
                        .filter(field -> !field.isAnnotationPresent(Documentation.class)
                                && !field.getType().isAnnotationPresent(Documentation.class))
                        .map(field -> "No @Documentation on '" + field + "'")
                        .sorted()
                        .collect(toSet()));
    }

    private void validateDocumentationWording(final AnnotationFinder finder, final List<Class<?>> components,
            final Set<String> errors) {
        final Predicate<String> isIncorrectWording = s -> !s.matches("^[A-Z0-9]+.*\\.$");
        final String error = "@Documentation on '%s' is empty or is not capitalized or ends not by a dot.";
        errors
                .addAll(components
                        .stream()
                        .filter(c -> c.isAnnotationPresent(Documentation.class))
                        .filter(c -> isIncorrectWording.test(c.getAnnotation(Documentation.class).value()))
                        .map(c -> String.format(error, c.getName()))
                        .sorted()
                        .collect(toSet()));
        errors
                .addAll(findOptions(finder)
                        .filter(field -> field.isAnnotationPresent(Documentation.class))
                        .filter(c -> isIncorrectWording.test(c.getAnnotation(Documentation.class).value()))
                        .map(c -> String.format(error, c.getName()))
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
                .addAll(findOptions(finder)
                        .map(Field::getType)
                        .filter(Class::isEnum)
                        .distinct()
                        .flatMap(enumType -> Stream
                                .of(enumType.getFields())
                                .filter(f -> Modifier.isStatic(f.getModifiers()) && Modifier.isFinal(f.getModifiers()))
                                .filter(f -> hasBundleEntry(enumType, f, "_displayName"))
                                .map(f -> "Missing key " + enumType.getSimpleName() + "." + f.getName()
                                        + "._displayName in " + enumType + " resource bundle"))
                        .sorted()
                        .collect(toSet()));

        // others - just logged for now, we can add it to errors if we encounter it too often
        final List<String> missingOptionTranslations = findOptions(finder).distinct().filter(field -> {
            final ResourceBundle bundle = ofNullable(findResourceBundle(field.getDeclaringClass()))
                    .orElseGet(() -> findResourceBundle(field.getType()));
            final String key = field.getDeclaringClass().getSimpleName() + "." + field.getName() + "._displayName";
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
                final Collection<Collection<String>> keys = of(i.getMethods())
                        .filter(m -> m.getDeclaringClass() != Object.class)
                        .map(m -> asList(i.getName() + "." + m.getName(), i.getSimpleName() + "." + m.getName()))
                        .collect(Collectors.toSet());
                errors
                        .addAll(keys
                                .stream()
                                .filter(ks -> ks.stream().noneMatch(resourceBundle::containsKey))
                                .map(k -> "Missing key " + k.iterator().next() + " in " + i + " resource bundle")
                                .sorted()
                                .collect(toSet()));

                errors
                        .addAll(resourceBundle
                                .keySet()
                                .stream()
                                .filter(k -> (k.startsWith(i.getName() + ".") || k.startsWith(i.getSimpleName() + "."))
                                        && keys.stream().noneMatch(ks -> ks.contains(k)))
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

    private void validateInternationalizationWording(final AnnotationFinder finder, final List<Class<?>> components,
            final Set<String> errors) {
        // TODO: define rules to apply to messages to users.
        // as a starter can apply it to all non enum, *_displayname and *_placeholder
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

    private Stream<Field> findOptions(final AnnotationFinder finder) {
        return finder.findAnnotatedFields(Option.class).stream();
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
        errors.addAll(components.stream().flatMap(component -> {
            Collection<String> messages = null;
            final IconFinder iconFinder = new IconFinder();
            if (iconFinder.findDirectIcon(component).isPresent()) {
                final Icon icon = component.getAnnotation(Icon.class);
                messages = new ArrayList<>();
                messages.add(validateIcon(icon, errors));
            } else if (!iconFinder.findIndirectIcon(component).isPresent()) {
                messages = new ArrayList<>(singleton("No @Icon on " + component));
            }

            if (!component.isAnnotationPresent(Version.class)) {
                if (messages == null) {
                    messages = new ArrayList<>();
                }
                messages.add("Component " + component + " should use @Icon and @Version");
            }
            return messages == null ? empty() : messages.stream();
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
                        .filter(dataset -> inputs.isEmpty() || inputs.entrySet().stream().allMatch(input -> {
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

    private boolean hasBundleEntry(final Class<?> enumType, final Field f, final String keyName) {
        final ResourceBundle bundle = findBundleFor(enumType, f);
        final String key = enumType.getSimpleName() + "." + f.getName() + "." + keyName;
        return bundle == null || !bundle.containsKey(key);
    }

    // todo: drop it to use parameter meta now we cache it?
    private ResourceBundle findBundleFor(final Class<?> enumType, final Field f) {
        return ofNullable(findResourceBundle(enumType)).orElseGet(() -> findResourceBundle(f.getDeclaringClass()));
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
        return options
                .stream()
                .flatMap(it -> Stream
                        .concat(Stream.of(it),
                                it.getNestedParameters().isEmpty() ? empty() : flatten(it.getNestedParameters())));
    }

    private String validateFamilyI18nKey(final Class<?> clazz, final String... keys) {
        final Class<?> pck = findPackageOrFail(clazz, apiTester(Components.class), Components.class.getName());
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

    private void validateExceptions(final Set<String> errors) {
        boolean exceptionFound = Arrays
                .stream(classes)
                .flatMap(f -> streamClassesInDirectory(null, f))
                .filter(ComponentException.class::isAssignableFrom)
                .findFirst()
                .isPresent();

        if (!exceptionFound) {
            if (configuration.isFailOnValidateExceptions()) {
                errors.add("Component should declare a custom Exception that inherits from ComponentException.");
            } else {
                log.info("Component should declare a custom Exception that inherits from ComponentException.");
            }
        }
    }

    private Stream<Class> streamClassesInDirectory(final String pckg, final File classFile) {
        if (classFile.isDirectory()) {
            return Arrays
                    .stream(classFile.listFiles())
                    .flatMap(f -> streamClassesInDirectory(pckg == null ? "" : (pckg + classFile.getName() + "."), f));
        }

        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (classFile.getName().endsWith(".class")) {
            String className = classFile.getName().substring(0, classFile.getName().lastIndexOf("."));
            try {
                return Stream.of(loader.loadClass(pckg + className));
            } catch (Exception e) {
                log.error("Could not load class : " + pckg + className + "=>" + e.getMessage());
            }
        }

        return null;
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

        private boolean validateWording;

        private boolean validateLayout;

        private boolean validateOptionNames;

        private boolean validateLocalConfiguration;

        private boolean validateOutputConnection;

        private boolean validatePlaceholder;

        private boolean validateSvg;

        private boolean validateNoFinalOption;

        private String pluginId;

        private boolean validateExceptions;

        private boolean failOnValidateExceptions;

        private boolean validateMigrations;
    }
}
