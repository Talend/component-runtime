/**
 * Copyright (C) 2006-2024 Talend Inc. - www.talend.com
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

import static java.util.Locale.ENGLISH;
import static java.util.Locale.ROOT;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;

import java.io.File;
import java.util.Locale;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class AsciidocDocumentationGenerator extends DocBaseGenerator {

    private final String levelPrefix;

    private final Map<String, String> formats;

    private final Map<String, String> attributes;

    private final File templateDir;

    private final File workDir;

    private final String templateEngine;

    private final String title;

    private final String version;

    // CHECKSTYLE:OFF - used by reflection so better to not create a wrapper
    public AsciidocDocumentationGenerator(final File[] classes, final File output, final String title, final int level,
            final Map<String, String> formats, final Map<String, String> attributes, final File templateDir,
            final String templateEngine, final Object log, final File workDir, final String version,
            final Locale locale) {
        // CHECKSTYLE:ON
        super(classes, locale, log, output);
        this.title = title;
        this.formats = formats;
        this.attributes = attributes;
        this.templateDir = templateDir;
        this.templateEngine = templateEngine;
        this.workDir = workDir;
        this.version = version;
        this.levelPrefix = IntStream.range(0, level).mapToObj(i -> "=").collect(joining(""));
    }

    @Override
    public void doRun() {
        final String doc = components()
                .map(this::toAsciidoc)
                .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
                .toString();
        write(output, doc);
        log.info("Generated " + output.getAbsolutePath());
        renderAdoc();
    }

    private String toAsciidoc(final ComponentDescription desc) {
        final String partMarker = desc.getName();
        return "//component_start:" + partMarker + "\n\n" + levelPrefix + " " + desc.getName() + "\n\n"
                + desc.getDocumentation()
                + (desc.getParameters().isEmpty() ? ""
                        : ("//configuration_start\n\n" + levelPrefix + "= Configuration\n\n" + desc
                                .parameters()
                                .map(this::toAsciidoctor)
                                .collect(joining("\n", "[cols=\"d,d,m,a,e,d\",options=\"header\"]\n"
                                        + "|===\n|Display Name|Description|Default Value|Enabled If|Configuration Path|Configuration Type\n",
                                        "\n|===\n\n//configuration_end\n\n"))))
                + "//component_end:" + partMarker + "\n\n";
    }

    private String toAsciidoctor(final Param p) {
        return "|" + p.getDisplayName() + '|' + p.getDocumentation() + '|' + p.getDefaultValue() + '|'
                + doRenderConditions(p.getConditions()) + '|' + p.getFullPath() + '|' + p.getType();
    }

    private String doRenderConditions(final Conditions conditions) {
        switch (conditions.getConditions().size()) {
            case 0:
                return "Always enabled";
            case 1:
                return renderCondition(conditions.getConditions().iterator().next());
            default:
                final String conds = conditions
                        .getConditions()
                        .stream()
                        .map(this::renderCondition)
                        .map(c -> "- " + c)
                        .collect(joining("\n", "\n", "\n"));
                switch (conditions.getOperator().toUpperCase(ROOT)) {
                    case "OR":
                        return "One of these conditions is meet:\n" + conds;
                    case "AND":
                    default:
                        return "All of the following conditions are met:\n" + conds;
                }
        }
    }

    private String renderCondition(final DocBaseGenerator.Condition condition) {
        final String values =
                Stream.of(condition.getValue().split(",")).map(v -> '`' + v + '`').collect(joining(" or "));
        switch (ofNullable(condition.getStrategy()).orElse("default").toLowerCase(ROOT)) {
            case "length":
                if (condition.isNegate()) {
                    if (values.equals("`0`")) {
                        return '`' + condition.getPath() + "` is not empty";
                    }
                    return "the length of `" + condition.getPath() + "` is not " + values;
                }
                if (values.equals("`0`")) {
                    return '`' + condition.getPath() + "` is empty";
                }
                return "the length of `" + condition.getPath() + "` is " + values;
            case "contains":
                if (condition.isNegate()) {
                    return '`' + condition.getPath() + "` does not contain " + values;
                }
                return '`' + condition.getPath() + "` contains " + values;
            case "contains(lowercase=true)":
                if (condition.isNegate()) {
                    return "the lowercase value of `" + condition.getPath() + "` does not contain " + values;
                }
                return "the lowercase value of `" + condition.getPath() + "` contains " + values;
            case "default":
            default:
                if (condition.isNegate()) {
                    return '`' + condition.getPath() + "` is not equal to " + values;
                }
                return '`' + condition.getPath() + "` is equal to " + values;
        }
    }

    private void renderAdoc() {
        try (final AsciidoctorExecutor asciidoctorExecutor = new AsciidoctorExecutor()) {
            ofNullable(formats).ifPresent(f -> f.forEach((format, output) -> {
                switch (format.toLowerCase(ENGLISH)) {
                    case "html":
                        asciidoctorExecutor
                                .render(workDir, version, log, "html5", this.output, new File(output), title,
                                        attributes, templateDir, templateEngine);
                        break;
                    case "pdf":
                        asciidoctorExecutor
                                .render(workDir, version, log, "pdf", this.output, new File(output), title,
                                        attributes, templateDir, templateEngine);
                        break;
                    default:
                        throw new IllegalArgumentException("unknown format: '" + format + "', supported: [html, pdf]");
                }
            }));
        }
    }
}
