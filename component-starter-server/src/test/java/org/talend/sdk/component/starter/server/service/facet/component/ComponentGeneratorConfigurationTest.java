/**
 * Copyright (C) 2006-2022 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.starter.server.service.facet.component;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.talend.sdk.component.starter.server.service.Strings.capitalize;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.apache.meecrowave.junit5.MonoMeecrowaveConfig;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.talend.sdk.component.starter.server.service.domain.ProjectRequest;
import org.talend.sdk.component.starter.server.service.template.TemplateRenderer;

import lombok.Data;

@MonoMeecrowaveConfig
class ComponentGeneratorConfigurationTest {

    @Inject
    private TemplateRenderer renderer;

    static Stream<Scenario> scenarii() {
        return Stream
                .of(
                        // no field
                        new Scenario(new ProjectRequest.DataStructure(new ArrayList<>()),
                                "package demo.source;\n" + "\n" + "import java.io.Serializable;\n" + "\n"
                                        + "import org.talend.sdk.component.api.configuration.Option;\n"
                                        + "import org.talend.sdk.component.api.configuration.ui.layout.GridLayout;\n"
                                        + "import org.talend.sdk.component.api.meta.Documentation;\n\n"
                                        + "@GridLayout({\n"
                                        + "    // the generated layout put one configuration entry per line,\n"
                                        + "    // customize it as much as needed\n" + "})\n"
                                        + "@Documentation(\"TODO fill the documentation for this configuration\")"
                                        + "\n" + "public class Demo implements Serializable {\n}"),
                        // string field
                        new Scenario(
                                new ProjectRequest.DataStructure(new ArrayList<>(
                                        singleton(new ProjectRequest.Entry("name", "String", null, null)))),
                                "package demo.source;\n" + "\n" + "import java.io.Serializable;\n" + "\n"
                                        + "import org.talend.sdk.component.api.configuration.Option;\n"
                                        + "import org.talend.sdk.component.api.configuration.ui.layout.GridLayout;\n"
                                        + "import org.talend.sdk.component.api.meta.Documentation;\n\n"
                                        + "@GridLayout({\n"
                                        + "    // the generated layout put one configuration entry per line,\n"
                                        + "    // customize it as much as needed\n"
                                        + "    @GridLayout.Row({ \"name\" })\n" + "})\n"
                                        + "@Documentation(\"TODO fill the documentation for this configuration\")"
                                        + "\n" + "public class Demo implements Serializable {\n" + "    @Option\n"
                                        + "    @Documentation(\"TODO fill the documentation for this parameter\")"
                                        + "\n" + "    private String name;\n" + "\n"

                                        + "    public String getName() {\n" + "        return name;\n" + "    }\n"
                                        + "\n" + "    public Demo setName(String name) {\n"
                                        + "        this.name = name;\n" + "        return this;\n" + "    }\n" + "}"),
                        // string field + int field
                        new Scenario(
                                new ProjectRequest.DataStructure(
                                        new ArrayList<>(asList(new ProjectRequest.Entry("name", "String", null, null),
                                                new ProjectRequest.Entry("age", "int", null, null)))),
                                "package demo.source;\n" + "\n" + "import java.io.Serializable;\n" + "\n"
                                        + "import org.talend.sdk.component.api.configuration.Option;\n"
                                        + "import org.talend.sdk.component.api.configuration.ui.layout.GridLayout;\n"
                                        + "import org.talend.sdk.component.api.meta.Documentation;\n\n"
                                        + "@GridLayout({\n"
                                        + "    // the generated layout put one configuration entry per line,\n"
                                        + "    // customize it as much as needed\n"
                                        + "    @GridLayout.Row({ \"name\" }),\n" + "    @GridLayout.Row({ \"age\" })\n"
                                        + "})\n"
                                        + "@Documentation(\"TODO fill the documentation for this configuration\")"
                                        + "\n" + "public class Demo implements Serializable {\n" + "    @Option\n"
                                        + "    @Documentation(\"TODO fill the documentation for this parameter\")"
                                        + "\n" + "    private String name;\n\n" + "    @Option\n"
                                        + "    @Documentation(\"TODO fill the documentation for this parameter\")"
                                        + "\n" + "    private int age;\n" + "\n" + "    public String getName() {\n"
                                        + "        return name;\n" + "    }\n" + "\n"
                                        + "    public Demo setName(String name) {\n" + "        this.name = name;\n"
                                        + "        return this;\n" + "    }\n" + "\n" + "    public int getAge() {\n"
                                        + "        return age;\n" + "    }\n" + "\n"
                                        + "    public Demo setAge(int age) {\n" + "        this.age = age;\n"
                                        + "        return this;\n" + "    }\n" + "}"));
    }

    @ParameterizedTest
    @MethodSource("scenarii")
    void run(final Scenario scenario) {
        final String result =
                renderer.render("generator/component/Configuration.mustache", new HashMap<String, Object>() {

                    {
                        put("className", "Demo");
                        put("package", "demo.source");
                        put("structure",
                                scenario.structure
                                        .getEntries()
                                        .stream()
                                        .map(e -> new ComponentGenerator.Property(e.getName(), capitalize(e.getName()),
                                                e.getType(), false))
                                        .collect(toList()));
                    }
                });
        assertEquals(scenario.expectedOutput, result);
    }

    @Data
    private static class Scenario {

        private final ProjectRequest.DataStructure structure;

        private final String expectedOutput;
    }
}
