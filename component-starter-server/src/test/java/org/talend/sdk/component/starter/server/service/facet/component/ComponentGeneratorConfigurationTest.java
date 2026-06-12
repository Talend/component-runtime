/**
 * Copyright (C) 2006-2026 Talend Inc. - www.talend.com
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
                                """
                                package demo.source;

                                import java.io.Serializable;

                                import org.talend.sdk.component.api.configuration.Option;
                                import org.talend.sdk.component.api.configuration.ui.layout.GridLayout;
                                import org.talend.sdk.component.api.meta.Documentation;

                                @GridLayout({
                                    // the generated layout put one configuration entry per line,
                                    // customize it as much as needed
                                })
                                @Documentation("TODO fill the documentation for this configuration")
                                public class Demo implements Serializable {
                                }"""),
                        // string field
                        new Scenario(
                                new ProjectRequest.DataStructure(new ArrayList<>(
                                        singleton(new ProjectRequest.Entry("name", "String", null, null)))),
                                """
                                package demo.source;

                                import java.io.Serializable;

                                import org.talend.sdk.component.api.configuration.Option;
                                import org.talend.sdk.component.api.configuration.ui.layout.GridLayout;
                                import org.talend.sdk.component.api.meta.Documentation;

                                @GridLayout({
                                    // the generated layout put one configuration entry per line,
                                    // customize it as much as needed
                                    @GridLayout.Row({ "name" })
                                })
                                @Documentation("TODO fill the documentation for this configuration")
                                public class Demo implements Serializable {
                                    @Option
                                    @Documentation("TODO fill the documentation for this parameter")
                                    private String name;

                                    public String getName() {
                                        return name;
                                    }

                                    public Demo setName(String name) {
                                        this.name = name;
                                        return this;
                                    }
                                }"""),
                        // string field + int field
                        new Scenario(
                                new ProjectRequest.DataStructure(
                                        new ArrayList<>(asList(new ProjectRequest.Entry("name", "String", null, null),
                                                new ProjectRequest.Entry("age", "int", null, null)))),
                                """
                                package demo.source;

                                import java.io.Serializable;

                                import org.talend.sdk.component.api.configuration.Option;
                                import org.talend.sdk.component.api.configuration.ui.layout.GridLayout;
                                import org.talend.sdk.component.api.meta.Documentation;

                                @GridLayout({
                                    // the generated layout put one configuration entry per line,
                                    // customize it as much as needed
                                    @GridLayout.Row({ "name" }),
                                    @GridLayout.Row({ "age" })
                                })
                                @Documentation("TODO fill the documentation for this configuration")
                                public class Demo implements Serializable {
                                    @Option
                                    @Documentation("TODO fill the documentation for this parameter")
                                    private String name;

                                    @Option
                                    @Documentation("TODO fill the documentation for this parameter")
                                    private int age;

                                    public String getName() {
                                        return name;
                                    }

                                    public Demo setName(String name) {
                                        this.name = name;
                                        return this;
                                    }

                                    public int getAge() {
                                        return age;
                                    }

                                    public Demo setAge(int age) {
                                        this.age = age;
                                        return this;
                                    }
                                }"""));
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
