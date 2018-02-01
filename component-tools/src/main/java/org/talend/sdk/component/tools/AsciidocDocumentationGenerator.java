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

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static java.util.Comparator.comparing;
import static java.util.Locale.ENGLISH;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.xbean.finder.AnnotationFinder;
import org.talend.sdk.component.api.meta.Documentation;
import org.talend.sdk.component.runtime.manager.ParameterMeta;
import org.talend.sdk.component.runtime.manager.reflect.Constructors;
import org.talend.sdk.component.runtime.manager.reflect.ParameterModelService;
import org.talend.sdk.component.runtime.manager.util.DefaultValueInspector;
import org.talend.sdk.component.spi.parameter.ParameterExtensionEnricher;

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

    private final ParameterModelService parameterModelService =
            new ParameterModelService(
                    singletonList(
                            (ParameterExtensionEnricher) (parameterName, parameterType,
                                    annotation) -> annotation.annotationType() == Documentation.class ? singletonMap(
                                            "documentation", Documentation.class.cast(annotation).value())
                                            : emptyMap())) {
            };

    // CHECKSTYLE:OFF - used by reflection so better to not create a wrapper
    public AsciidocDocumentationGenerator(final File[] classes, final File output, final String title, final int level,
            final Map<String, String> formats, final Map<String, String> attributes, final File templateDir,
            final String templateEngine, final Object log, final File workDir, final String version) {
        // CHECKSTYLE:ON
        super(classes);
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

        ofNullable(formats).ifPresent(f -> {
            try (final AsciidoctorExecutor asciidoctorExecutor = new AsciidoctorExecutor()) {
                f.forEach((format, output) -> {
                    switch (format.toLowerCase(ENGLISH)) {
                    case "html":
                        asciidoctorExecutor.render(workDir, version, log, "html5", this.output, new File(output), title,
                                attributes, templateDir, templateEngine);
                        break;
                    case "pdf":
                        asciidoctorExecutor.render(workDir, version, log, "pdf", this.output, new File(output), title,
                                attributes, templateDir, templateEngine);
                        break;
                    default:
                        throw new IllegalArgumentException("unknown format: '" + format + "', supported: [html, pdf]");
                    }
                });
            }
        });
    }

    private String toAsciidoc(final Class<?> aClass) {
        final Collection<ParameterMeta> parameterMetas =
                parameterModelService.buildParameterMetas(Constructors.findConstructor(aClass),
                        ofNullable(aClass.getPackage()).map(Package::getName).orElse(""), emptySet());
        return levelPrefix + " "
                + componentMarkers()
                        .filter(aClass::isAnnotationPresent)
                        .map(aClass::getAnnotation)
                        .map(this::asComponent)
                        .findFirst()
                        .get()
                        .name()
                + "\n\n"
                + ofNullable(aClass.getAnnotation(Documentation.class))
                        .map(Documentation::value)
                        .map(v -> v + "\n\n")
                        .orElse("")
                + (parameterMetas.isEmpty() ? ""
                        : (levelPrefix + "= Configuration\n\n" + toAsciidocRows(parameterMetas, null)
                                .sorted(comparing(line -> line.substring(1, line.indexOf('|', 1))))
                                .collect(joining("\n", "|===\n|Path|Description|Default Value\n", "\n|===\n\n"))));
    }

    private Stream<String> toAsciidocRows(final Collection<ParameterMeta> parameterMetas, final Object parentInstance) {
        return parameterMetas.stream().flatMap(p -> {
            final Object instance = defaultValueInspector.createDemoInstance(parentInstance, p);
            return Stream.concat(Stream.of(toAsciidoctor(p, instance)),
                    toAsciidocRows(p.getNestedParameters(), instance));
        });
    }

    private String toAsciidoctor(final ParameterMeta p, final Object instance) {
        return "|" + p.getPath() + '|' + findDocumentation(p) + '|'
                + ofNullable(defaultValueInspector.findDefault(instance, p)).orElse("-");
    }

    private String findDocumentation(final ParameterMeta p) {
        final String inline = p.getMetadata().get("documentation");
        if (inline != null) {
            if (inline.startsWith("resource:")) {
                final InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(
                        inline.substring("resource:".length()));
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
