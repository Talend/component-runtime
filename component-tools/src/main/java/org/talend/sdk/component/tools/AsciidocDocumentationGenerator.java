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

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static java.util.Comparator.comparing;
import static java.util.Locale.ENGLISH;
import static java.util.Locale.ROOT;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

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
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.IntStream;
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
import org.talend.sdk.component.runtime.manager.service.LocalConfigurationService;
import org.talend.sdk.component.runtime.manager.util.DefaultValueInspector;
import org.talend.sdk.component.spi.parameter.ParameterExtensionEnricher;

import lombok.AllArgsConstructor;

public class AsciidocDocumentationGenerator extends BaseTask {

    private final File output;

    private final String levelPrefix;

    private final DefaultValueInspector defaultValueInspector = new DefaultValueInspector();

    private final Map<String, String> formats;

    private final Map<String, String> attributes;

    private final File templateDir;

    private final File workDir;

    private final String templateEngine;

    private final String title;

    private final String version;

    private final Log log;

    private final Locale locale;

    private final AbsolutePathResolver resolver = new AbsolutePathResolver();

    private final ParameterModelService parameterModelService =
            new ParameterModelService(asList((ParameterExtensionEnricher) (parameterName, parameterType,
                    annotation) -> annotation.annotationType() == Documentation.class
                            ? singletonMap("documentation", Documentation.class.cast(annotation).value())
                            : emptyMap(),
                    new ConditionParameterEnricher()), new PropertyEditorRegistry()) {

            };

    // CHECKSTYLE:OFF - used by reflection so better to not create a wrapper
    public AsciidocDocumentationGenerator(final File[] classes, final File output, final String title, final int level,
            final Map<String, String> formats, final Map<String, String> attributes, final File templateDir,
            final String templateEngine, final Object log, final File workDir, final String version,
            final Locale locale) {
        // CHECKSTYLE:ON
        super(classes);
        this.locale = locale;
        this.title = title;
        this.output = output;
        this.formats = formats;
        this.attributes = attributes;
        this.templateDir = templateDir;
        this.templateEngine = templateEngine;
        this.workDir = workDir;
        this.version = version;
        this.levelPrefix = IntStream.range(0, level).mapToObj(i -> "=").collect(joining(""));
        try {
            this.log = Log.class.isInstance(log) ? Log.class.cast(log) : new ReflectiveLog(log);
        } catch (final NoSuchMethodException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public void run() {
        final AnnotationFinder finder = newFinder();
        final String doc = componentMarkers()
                .flatMap(a -> finder.findAnnotatedClasses(a).stream())
                .map(this::toAsciidoc)
                .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
                .toString();
        output.getParentFile().mkdirs();
        try (final Writer writer = new BufferedWriter(new FileWriter(output))) {
            writer.write(doc);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        log.info("Generated " + output.getAbsolutePath());
        try (final AsciidoctorExecutor asciidoctorExecutor = new AsciidoctorExecutor()) {
            ofNullable(formats).ifPresent(f -> f.forEach((format, output) -> {
                switch (format.toLowerCase(ENGLISH)) {
                case "html":
                    asciidoctorExecutor
                            .render(workDir, version, log, "html5", this.output, new File(output), title, attributes,
                                    templateDir, templateEngine);
                    break;
                case "pdf":
                    asciidoctorExecutor
                            .render(workDir, version, log, "pdf", this.output, new File(output), title, attributes,
                                    templateDir, templateEngine);
                    break;
                default:
                    throw new IllegalArgumentException("unknown format: '" + format + "', supported: [html, pdf]");
                }
            }));
        }
    }

    private String toAsciidoc(final Class<?> aClass) {
        final Collection<ParameterMeta> parameterMetas = parameterModelService
                .buildParameterMetas(Constructors.findConstructor(aClass),
                        ofNullable(aClass.getPackage()).map(Package::getName).orElse(""),
                        new BaseParameterEnricher.Context(new LocalConfigurationService(emptyList(), "tools")));
        return levelPrefix + " "
                + componentMarkers()
                        .filter(aClass::isAnnotationPresent)
                        .map(aClass::getAnnotation)
                        .map(this::asComponent)
                        .findFirst()
                        .orElseThrow(NoSuchElementException::new)
                        .name()
                + "\n\n"
                + ofNullable(aClass.getAnnotation(Documentation.class))
                        .map(Documentation::value)
                        .map(v -> v + "\n\n")
                        .orElse("")
                + (parameterMetas.isEmpty() ? ""
                        : (levelPrefix + "= Configuration\n\n" + toAsciidocRows(sort(parameterMetas), null, null)
                                .collect(joining("\n",
                                        "[cols=\"d,d,m,a,e\",options=\"header\"]\n|===\n|Display Name|Description|Default Value|Enabled If|Configuration Path\n",
                                        "\n|===\n\n"))));
    }

    private Collection<ParameterMeta> sort(final Collection<ParameterMeta> parameterMetas) {
        return parameterMetas.stream().sorted(comparing(ParameterMeta::getPath)).collect(toList());
    }

    private Stream<String> toAsciidocRows(final Collection<ParameterMeta> parameterMetas,
            final DefaultValueInspector.Instance parentInstance, final ParameterBundle parentBundle) {
        return parameterMetas.stream().flatMap(p -> {
            final DefaultValueInspector.Instance instance = defaultValueInspector
                    .createDemoInstance(
                            ofNullable(parentInstance).map(DefaultValueInspector.Instance::getValue).orElse(null), p);
            return Stream
                    .concat(Stream.of(toAsciidoctor(p, instance, parentBundle)),
                            toAsciidocRows(p.getNestedParameters(), instance, findBundle(p)));
        });
    }

    private String toAsciidoctor(final ParameterMeta p, final DefaultValueInspector.Instance instance,
            final ParameterBundle parent) {
        final ParameterBundle bundle = findBundle(p);
        return "|" + bundle.displayName(parent).orElse(p.getName()) + '|'
                + bundle.documentation(parent).orElseGet(() -> findDocumentation(p)) + '|'
                + ofNullable(findDefault(p, instance)).orElse("-") + '|'
                + renderConditions(p.getPath(), p.getMetadata()) + '|' + p.getPath();
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

    private String renderConditions(final String path, final Map<String, String> metadata) {
        final String globalOperator = metadata.getOrDefault("tcomp::condition::ifs::operator", "AND");
        final Collection<Condition> conditionEntries = metadata
                .keySet()
                .stream()
                .filter(it -> it.startsWith("tcomp::condition::if::target"))
                .map(it -> new Condition(metadata.get(it), metadata.get(it.replace("::target", "::value")),
                        Boolean.parseBoolean(metadata.get(it.replace("::target", "::negate"))),
                        metadata.get(it.replace("::target", "::evaluationStrategy"))))
                .collect(toList());
        switch (conditionEntries.size()) {
        case 0:
            return "Always enabled";
        case 1:
            return renderCondition(path, conditionEntries.iterator().next());
        default:
            final String conditions = conditionEntries
                    .stream()
                    .map(c -> renderCondition(path, c))
                    .map(c -> "- " + c)
                    .collect(joining("\n", "\n", "\n"));
            switch (globalOperator.toUpperCase(ROOT)) {
            case "OR":
                return "One of these conditions is meet:\n" + conditions;
            case "AND":
            default:
                return "All of the following conditions are met:\n" + conditions;
            }
        }
    }

    private String renderCondition(final String paramPath, final Condition condition) {
        final String path = resolver.doResolveProperty(paramPath, condition.target);
        final String values = Stream.of(condition.value.split(",")).map(v -> '`' + v + '`').collect(joining(" or "));
        switch (ofNullable(condition.strategy).orElse("default").toLowerCase(ROOT)) {
        case "length":
            if (condition.negate) {
                if (values.equals("`0`")) {
                    return '`' + path + "` is not empty";
                }
                return "the length of `" + path + "` is not " + values;
            }
            if (values.equals("`0`")) {
                return '`' + path + "` is empty";
            }
            return "the length of `" + path + "` is " + values;
        case "contains":
            if (condition.negate) {
                return '`' + path + "` does not contain " + values;
            }
            return '`' + path + "` contains " + values;
        case "contains(lowercase=true)":
            if (condition.negate) {
                return "the lowercase value of `" + path + "` does not contain " + values;
            }
            return "the lowercase value of `" + path + "` contains " + values;
        case "default":
        default:
            if (condition.negate) {
                return '`' + path + "` is not equal to " + values;
            }
            return '`' + path + "` is equal to " + values;
        }
    }

    private String findDocumentation(final ParameterMeta p) {
        final String inline = p.getMetadata().get("documentation");
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

    private ParameterBundle findBundle(final ParameterMeta p) {
        return p.findBundle(Thread.currentThread().getContextClassLoader(), locale);
    }

    @AllArgsConstructor
    private static class Condition {

        private final String target;

        private final String value;

        private final boolean negate;

        private final String strategy;
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
}
