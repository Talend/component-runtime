/**
 * Copyright (C) 2006-2019 Talend Inc. - www.talend.com
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
import static java.util.Comparator.comparing;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static lombok.AccessLevel.PRIVATE;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Stream;

import org.apache.xbean.finder.AnnotationFinder;
import org.apache.xbean.propertyeditor.PropertyEditorRegistry;
import org.talend.sdk.component.api.meta.Documentation;
import org.talend.sdk.component.runtime.internationalization.ParameterBundle;
import org.talend.sdk.component.runtime.manager.ParameterMeta;
import org.talend.sdk.component.runtime.manager.reflect.Constructors;
import org.talend.sdk.component.runtime.manager.reflect.ParameterModelService;
import org.talend.sdk.component.runtime.manager.reflect.parameterenricher.BaseParameterEnricher;
import org.talend.sdk.component.runtime.manager.reflect.parameterenricher.ConditionParameterEnricher;
import org.talend.sdk.component.runtime.manager.reflect.parameterenricher.ConfigurationTypeParameterEnricher;
import org.talend.sdk.component.runtime.manager.reflect.parameterenricher.DocumentationParameterEnricher;
import org.talend.sdk.component.runtime.manager.service.LocalConfigurationService;
import org.talend.sdk.component.runtime.manager.util.DefaultValueInspector;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

abstract class DocBaseGenerator extends BaseTask {

    private final DefaultValueInspector defaultValueInspector = new DefaultValueInspector();

    @Getter
    private final Locale locale;

    private final AbsolutePathResolver resolver = new AbsolutePathResolver();

    protected final Log log;

    protected final File output;

    private final ParameterModelService parameterModelService =
            new ParameterModelService(asList(new DocumentationParameterEnricher(), new ConditionParameterEnricher(),
                    new ConfigurationTypeParameterEnricher()), new PropertyEditorRegistry()) {

            };

    DocBaseGenerator(final File[] classes, final Locale locale, final Object log, final File output) {
        super(classes);
        this.locale = locale;
        this.output = output;
        try {
            this.log = Log.class.isInstance(log) ? Log.class.cast(log) : new ReflectiveLog(log);
        } catch (final NoSuchMethodException e) {
            throw new IllegalArgumentException(e);
        }
    }

    protected Stream<ComponentDescription> components() {
        final AnnotationFinder finder = newFinder();
        return findComponents(finder).map(component -> {
            final Collection<ParameterMeta> parameterMetas = parameterModelService
                    .buildParameterMetas(Constructors.findConstructor(component),
                            ofNullable(component.getPackage()).map(Package::getName).orElse(""),
                            new BaseParameterEnricher.Context(new LocalConfigurationService(emptyList(), "tools")));
            final Component componentMeta = componentMarkers()
                    .filter(component::isAnnotationPresent)
                    .map(component::getAnnotation)
                    .map(this::asComponent)
                    .findFirst()
                    .orElseThrow(NoSuchElementException::new);
            String family = "";
            try {
                family = findFamily(componentMeta, component);
            } catch (final IllegalArgumentException iae) {
                // skip for doc
            }
            return new ComponentDescription(component, family, componentMeta.name(), getDoc(component),
                    sort(parameterMetas), resolver, defaultValueInspector, locale);
        });
    }

    protected Stream<Class<?>> findComponents(final AnnotationFinder finder) {
        return componentMarkers().flatMap(a -> finder.findAnnotatedClasses(a).stream());
    }

    private String getDoc(final Class<?> component) {
        final Collection<String> docKeys = Stream
                .of(getComponentPrefix(component), component.getSimpleName())
                .map(it -> it + "._documentation")
                .collect(toList());
        return ofNullable(findResourceBundle(component))
                .map(bundle -> docKeys
                        .stream()
                        .filter(bundle::containsKey)
                        .map(bundle::getString)
                        .findFirst()
                        .map(v -> v + "\n\n")
                        .orElse(null))
                .orElseGet(() -> ofNullable(component.getAnnotation(Documentation.class))
                        .map(Documentation::value)
                        .map(v -> v + "\n\n")
                        .orElse(""));
    }

    private String getComponentPrefix(final Class<?> component) {
        try {
            return components(component)
                    .map(c -> findFamily(c, component) + "." + c.name())
                    .orElseGet(component::getSimpleName);
        } catch (final RuntimeException e) {
            return component.getSimpleName();
        }
    }

    private Collection<ParameterMeta> sort(final Collection<ParameterMeta> parameterMetas) {
        return parameterMetas.stream().sorted(comparing(ParameterMeta::getPath)).collect(toList());
    }

    protected void write(final File output, final String content) {
        ensureParentExists(output);
        try (final Writer writer = new BufferedWriter(new FileWriter(output))) {
            writer.write(content);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    protected void ensureParentExists(final File output) {
        if (!output.getParentFile().isDirectory() && !output.getParentFile().mkdirs()) {
            throw new IllegalStateException("Can't create " + output.getParentFile());
        }
    }

    private static class AbsolutePathResolver {

        // ensure it is aligned with org.talend.sdk.component.studio.model.parameter.SettingsCreator.computeTargetPath()
        // and org.talend.sdk.component.form.internal.converter.impl.widget.path.AbsolutePathResolver
        public String resolveProperty(final String propPath, final String paramRef) {
            return doResolveProperty(propPath, normalizeParamRef(paramRef));
        }

        private String normalizeParamRef(final String paramRef) {
            return (!paramRef.contains(".") ? "../" : "") + paramRef;
        }

        private String doResolveProperty(final String propPath, final String paramRef) {
            if (".".equals(paramRef)) {
                return propPath;
            }
            if (paramRef.startsWith("..")) {
                String current = propPath;
                String ref = paramRef;
                while (ref.startsWith("..")) {
                    int lastDot = current.lastIndexOf('.');
                    if (lastDot < 0) {
                        lastDot = 0;
                    }
                    current = current.substring(0, lastDot);
                    ref = ref.substring("..".length(), ref.length());
                    if (ref.startsWith("/")) {
                        ref = ref.substring(1);
                    }
                    if (current.isEmpty()) {
                        break;
                    }
                }
                return Stream.of(current, ref.replace('/', '.')).filter(it -> !it.isEmpty()).collect(joining("."));
            }
            if (paramRef.startsWith(".") || paramRef.startsWith("./")) {
                return propPath + '.' + paramRef.replaceFirst("\\./?", "").replace('/', '.');
            }
            return paramRef;
        }
    }

    @Data
    @AllArgsConstructor(access = PRIVATE)
    protected static class Condition {

        private final String target;

        private final String path;

        private final String value;

        private final boolean negate;

        private final String strategy;
    }

    @Data
    @AllArgsConstructor(access = PRIVATE)
    protected static class Param {

        private final String displayName;

        private final String documentation;

        private final String defaultValue;

        private final String type;

        private final String fullPath;

        private final Conditions conditions;
    }

    @Data
    @AllArgsConstructor(access = PRIVATE)
    public static class Conditions {

        private final String path;

        private final String operator;

        private final Collection<Condition> conditions;
    }

    @Data
    @AllArgsConstructor(access = PRIVATE)
    protected static class ComponentDescription {

        private final Class<?> type;

        private final String family;

        private final String name;

        private final String documentation;

        private final Collection<ParameterMeta> parameters;

        private final AbsolutePathResolver resolver;

        private final DefaultValueInspector defaultValueInspector;

        private final Locale locale;

        Stream<Param> parameters() {
            return mapParameters(parameters, null, null, new HashMap<>());
        }

        private Stream<Param> mapParameters(final Collection<ParameterMeta> parameterMetas,
                final DefaultValueInspector.Instance parentInstance, final ParameterBundle parentBundle,
                final Map<String, String> types) {
            return parameterMetas.stream().flatMap(p -> {
                final DefaultValueInspector.Instance instance = defaultValueInspector
                        .createDemoInstance(
                                ofNullable(parentInstance).map(DefaultValueInspector.Instance::getValue).orElse(null),
                                p);
                return Stream
                        .concat(Stream.of(toParam(p, instance, parentBundle, types)),
                                mapParameters(p.getNestedParameters(), instance, findBundle(p), types));
            });
        }

        private Param toParam(final ParameterMeta p, final DefaultValueInspector.Instance instance,
                final ParameterBundle parent, final Map<String, String> types) {
            final ParameterBundle bundle = findBundle(p);
            final String type = findEnclosingConfigurationType(p, types);
            return new Param(bundle.displayName(parent).orElse(p.getName()),
                    bundle.documentation(parent).orElseGet(() -> findDocumentation(p)),
                    ofNullable(findDefault(p, instance)).orElse("-"), ofNullable(type).orElse("-"), p.getPath(),
                    createConditions(p.getPath(), p.getMetadata()));
        }

        private ParameterBundle findBundle(final ParameterMeta p) {
            return p.findBundle(Thread.currentThread().getContextClassLoader(), locale);
        }

        private String findEnclosingConfigurationType(final ParameterMeta p, final Map<String, String> types) {
            String type = p.getMetadata().get("tcomp::configurationtype::type");
            if (type != null) {
                types.put(p.getPath(), type);
            } else { // try to find the closest parent
                String currentPath = p.getPath();
                while (type == null) {
                    final int sep = currentPath.lastIndexOf('.');
                    if (sep < 0) {
                        break;
                    }
                    currentPath = currentPath.substring(0, sep);
                    type = types.get(currentPath);
                }
            }
            return type;
        }

        private String findDefault(final ParameterMeta p, final DefaultValueInspector.Instance instance) {
            if (instance == null || instance.getValue() == null || instance.isCreated()) {
                return null;
            }

            switch (p.getType()) {
            case NUMBER:
            case BOOLEAN:
            case STRING:
            case ENUM:
                return ofNullable(instance.getValue())
                        .map(String::valueOf)
                        .map(it -> it.isEmpty() ? "<empty>" : it)
                        .orElse(null);
            case ARRAY:
                return String
                        .valueOf(Collection.class.isInstance(instance.getValue())
                                ? Collection.class.cast(instance.getValue()).size()
                                : Array.getLength(instance.getValue()));
            case OBJECT:
            default:
                return null;
            }
        }

        private Conditions createConditions(final String path, final Map<String, String> metadata) {
            final String globalOperator = metadata.getOrDefault("tcomp::condition::ifs::operator", "AND");
            final Collection<Condition> conditionEntries =
                    metadata.keySet().stream().filter(it -> it.startsWith("tcomp::condition::if::target")).map(it -> {
                        final String target = metadata.get(it);
                        return new Condition(target, resolver.doResolveProperty(path, target),
                                metadata.get(it.replace("::target", "::value")),
                                Boolean.parseBoolean(metadata.get(it.replace("::target", "::negate"))),
                                metadata.get(it.replace("::target", "::evaluationStrategy")));
                    }).collect(toList());
            return new Conditions(path, globalOperator, conditionEntries);
        }

        private String findDocumentation(final ParameterMeta p) {
            final String inline = p.getMetadata().get("tcomp::documentation::value");
            if (inline != null) {
                if (inline.startsWith("resource:")) {
                    final InputStream stream = Thread
                            .currentThread()
                            .getContextClassLoader()
                            .getResourceAsStream(inline.substring("resource:".length()));
                    if (stream != null) {
                        try (final BufferedReader reader =
                                new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {

                            return reader.lines().collect(joining("\n"));
                        } catch (final IOException e) {
                            throw new IllegalArgumentException("Bad resource: '" + inline + "'", e);
                        }
                    } else {
                        throw new IllegalArgumentException("No resource: '" + inline + "'");
                    }

                }
                return inline;
            }
            return p.getName() + " configuration";
        }
    }
}
