/**
 * Copyright (C) 2006-2017 Talend Inc. - www.talend.com
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
package org.talend.component.starter.server.service.facet.component;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.junit.Assert.assertEquals;

import java.util.HashMap;

import javax.inject.Inject;

import org.apache.meecrowave.junit.MonoMeecrowave;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.talend.component.starter.server.service.domain.ProjectRequest;
import org.talend.component.starter.server.service.template.TemplateRenderer;

import lombok.Data;

@RunWith(Parameterized.class)
public class ComponentGeneratorConfigurationTest {

    @Rule
    public final TestRule container = new MonoMeecrowave.Rule().inject(this);

    @Parameterized.Parameters(name = "{0}")
    public static Iterable<Scenario> scenarii() {
        return asList(
                // no field
                new Scenario(new ProjectRequest.DataStructure(emptyList()),
                        "package demo.source;\n" + "\n" + "import java.util.List;\n" + "\n"
                                + "import org.talend.component.api.configuration.Option;\n"
                                + "import org.talend.component.api.configuration.ui.layout.GridLayout;\n" + "\n"
                                + "// generated configuration with query and addresses options, customize it to your need\n"
                                + "@GridLayout({\n" + "    // the generated layout put one configuration entry per line,\n"
                                + "    // customize it as much as needed\n" + "})\n" + "public class Demo {\n" + "\n" + "}"),
                // string field
                new Scenario(new ProjectRequest.DataStructure(singleton(new ProjectRequest.Entry("name", "String", null))),
                        "package demo.source;\n" + "\n" + "import java.util.List;\n" + "\n"
                                + "import org.talend.component.api.configuration.Option;\n"
                                + "import org.talend.component.api.configuration.ui.layout.GridLayout;\n" + "\n"
                                + "// generated configuration with query and addresses options, customize it to your need\n"
                                + "@GridLayout({\n" + "    // the generated layout put one configuration entry per line,\n"
                                + "    // customize it as much as needed\n" + "    @GridLayout.Row({ \"name\" })\n" + "})\n"
                                + "public class Demo {\n" + "    @Option\n" + "    private String name;\n" + "\n"
                                + "    public String getName() {\n" + "        return name;\n" + "    }\n" + "}"),
                // string field + int field
                new Scenario(
                        new ProjectRequest.DataStructure(asList(new ProjectRequest.Entry("name", "String", null),
                                new ProjectRequest.Entry("age", "int", null))),
                        "package demo.source;\n" + "\n" + "import java.util.List;\n" + "\n"
                                + "import org.talend.component.api.configuration.Option;\n"
                                + "import org.talend.component.api.configuration.ui.layout.GridLayout;\n" + "\n"
                                + "// generated configuration with query and addresses options, customize it to your need\n"
                                + "@GridLayout({\n" + "    // the generated layout put one configuration entry per line,\n"
                                + "    // customize it as much as needed\n" + "    @GridLayout.Row({ \"name\" }),\n"
                                + "    @GridLayout.Row({ \"age\" })\n" + "})\n" + "public class Demo {\n" + "    @Option\n"
                                + "    private String name;\n" + "\n" + "    @Option\n" + "    private int age;\n" + "\n"
                                + "    public String getName() {\n" + "        return name;\n" + "    }\n" + "\n"
                                + "    public int getAge() {\n" + "        return age;\n" + "    }\n" + "}"));
    }

    @Parameterized.Parameter
    public Scenario scenario;

    @Inject
    private TemplateRenderer renderer;

    @Test
    public void run() {
        final String result = renderer.render("generator/component/Configuration.java", new HashMap<String, Object>() {

            {
                put("className", "Demo");
                put("package", "demo.source");
                put("structure",
                        scenario.structure.getEntries().stream().map(
                                e -> new ComponentGenerator.Property(e.getName(), "get" + capitalize(e.getName()), e.getType()))
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
