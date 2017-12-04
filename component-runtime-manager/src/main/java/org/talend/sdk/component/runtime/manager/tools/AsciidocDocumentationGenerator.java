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
package org.talend.sdk.component.runtime.manager.tools;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static java.util.Comparator.comparing;
import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.joining;
import static org.talend.sdk.component.runtime.manager.reflect.Constructors.findConstructor;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.xbean.finder.AnnotationFinder;
import org.talend.sdk.component.api.meta.Documentation;
import org.talend.sdk.component.runtime.manager.ParameterMeta;
import org.talend.sdk.component.runtime.manager.reflect.ParameterModelService;
import org.talend.sdk.component.spi.parameter.ParameterExtensionEnricher;

public class AsciidocDocumentationGenerator extends BaseTask {

    private final File output;

    private final String levelPrefix;

    private final DefaultValueInspector defaultValueInspector = new DefaultValueInspector();

    private final ParameterModelService parameterModelService =
            new ParameterModelService(
                    singletonList(
                            (ParameterExtensionEnricher) (parameterName, parameterType,
                                    annotation) -> annotation.annotationType() == Documentation.class ? singletonMap(
                                            "documentation", Documentation.class.cast(annotation).value())
                                            : emptyMap())) {
            };

    public AsciidocDocumentationGenerator(final File[] classes, final File output, final int level) {
        super(classes);
        this.output = output;
        this.levelPrefix = IntStream.range(0, level).mapToObj(i -> "=").collect(joining(""));
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
    }

    private String toAsciidoc(final Class<?> aClass) {
        final Collection<ParameterMeta> parameterMetas = parameterModelService.buildParameterMetas(
                findConstructor(aClass), ofNullable(aClass.getPackage()).map(Package::getName).orElse(""));
        return levelPrefix + " " + aClass.getSimpleName() + "\n\n"
                + ofNullable(aClass.getAnnotation(Documentation.class))
                        .map(Documentation::value)
                        .map(v -> v + "\n\n")
                        .orElse("")
                + (parameterMetas.isEmpty() ? ""
                        : (levelPrefix + "= Configuration\n\n"
                                + toAsciidocRows(parameterMetas, null).sorted(comparing(identity())).collect(
                                        joining("\n", "|===\n|Path|Description|Default Value\n", "\n|===\n\n"))));
    }

    private Stream<String> toAsciidocRows(final Collection<ParameterMeta> parameterMetas, final Object parentInstance) {
        return parameterMetas.stream().flatMap(p -> {
            final Object instance = defaultValueInspector.createDemoInstance(parentInstance, p);
            return Stream.concat(Stream.of(toAsciidoctor(p, instance)),
                    toAsciidocRows(p.getNestedParameters(), instance));
        });
    }

    private String toAsciidoctor(final ParameterMeta p, final Object instance) {
        return "|" + p.getPath() + '|' + p.getMetadata().getOrDefault("documentation", p.getName() + " configuration")
                + '|' + ofNullable(defaultValueInspector.findDefault(instance, p)).orElse("-");
    }
}
