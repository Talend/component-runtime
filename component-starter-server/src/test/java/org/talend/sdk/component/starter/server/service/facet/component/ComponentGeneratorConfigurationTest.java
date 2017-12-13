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
package org.talend.sdk.component.starter.server.service.facet.component;

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
import org.talend.sdk.component.starter.server.service.domain.ProjectRequest;
import org.talend.sdk.component.starter.server.service.template.TemplateRenderer;

import lombok.Data;

@RunWith(Parameterized.class)
public class ComponentGeneratorConfigurationTest {

    @Rule
    public final TestRule container = new MonoMeecrowave.Rule().inject(this);

    @Parameterized.Parameter
    public Scenario scenario;

    @Inject
    private TemplateRenderer renderer;

    @Parameterized.Parameters(name = "{0}")
    public static Iterable<Scenario> scenarii() {
        return asList(
                // no field
                new Scenario(new ProjectRequest.DataStructure(emptyList()),
                        "package demo.source;\n" + "\n" + "import java.io.Serializable;\n" + "\n"
                                + "import org.talend.sdk.component.api.configuration.Option;\n"
                                + "import org.talend.sdk.component.api.configuration.ui.layout.GridLayout;\n"
                                + "import org.talend.sdk.component.api.meta.Documentation;\n\n" + "@GridLayout({\n"
                                + "    // the generated layout put one configuration entry per line,\n"
                                + "    // customize it as much as needed\n" + "})\n"
                                + "@Documentation(\"TODO fill the documentation for this configuration\")" + "\n"
                                + "public class Demo implements Serializable {\n}"),
                // string field
                new Scenario(
                        new ProjectRequest.DataStructure(singleton(new ProjectRequest.Entry("name", "String", null))),
                        "package demo.source;\n" + "\n" + "import java.io.Serializable;\n" + "\n"
                                + "import org.talend.sdk.component.api.configuration.Option;\n"
                                + "import org.talend.sdk.component.api.configuration.ui.layout.GridLayout;\n"
                                + "import org.talend.sdk.component.api.meta.Documentation;\n\n" + "@GridLayout({\n"
                                + "    // the generated layout put one configuration entry per line,\n"
                                + "    // customize it as much as needed\n" + "    @GridLayout.Row({ \"name\" })\n"
                                + "})\n" + "@Documentation(\"TODO fill the documentation for this configuration\")"
                                + "\n" + "public class Demo implements Serializable {\n" + "    @Option\n"
                                + "    @Documentation(\"TODO fill the documentation for this parameter\")" + "\n"
                                + "    private String name;\n" + "\n"

                                + "    public String getName() {\n" + "        return name;\n" + "    }\n" + "\n"
                                + "    public Demo setName(String name) {\n" + "        this.name = name;\n"
                                + "        return this;\n" + "    }\n" + "}"),
                // string field + int field
                new Scenario(
                        new ProjectRequest.DataStructure(asList(new ProjectRequest.Entry("name", "String", null),
                                new ProjectRequest.Entry("age", "int", null))),
                        "package demo.source;\n" + "\n" + "import java.io.Serializable;\n" + "\n"
                                + "import org.talend.sdk.component.api.configuration.Option;\n"
                                + "import org.talend.sdk.component.api.configuration.ui.layout.GridLayout;\n"
                                + "import org.talend.sdk.component.api.meta.Documentation;\n\n" + "@GridLayout({\n"
                                + "    // the generated layout put one configuration entry per line,\n"
                                + "    // customize it as much as needed\n" + "    @GridLayout.Row({ \"name\" }),\n"
                                + "    @GridLayout.Row({ \"age\" })\n" + "})\n"
                                + "@Documentation(\"TODO fill the documentation for this configuration\")" + "\n"
                                + "public class Demo implements Serializable {\n" + "    @Option\n"
                                + "    @Documentation(\"TODO fill the documentation for this parameter\")" + "\n"
                                + "    private String name;\n\n" + "    @Option\n"
                                + "    @Documentation(\"TODO fill the documentation for this parameter\")" + "\n"
                                + "    private int age;\n" + "\n" + "    public String getName() {\n"
                                + "        return name;\n" + "    }\n" + "\n"
                                + "    public Demo setName(String name) {\n" + "        this.name = name;\n"
                                + "        return this;\n" + "    }\n" + "\n" + "    public int getAge() {\n"
                                + "        return age;\n" + "    }\n" + "\n" + "    public Demo setAge(int age) {\n"
                                + "        this.age = age;\n" + "        return this;\n" + "    }\n" + "}"));
    }

    @Test
    public void run() {
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
