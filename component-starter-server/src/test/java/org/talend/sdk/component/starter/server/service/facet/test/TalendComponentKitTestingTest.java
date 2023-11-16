/**
 * Copyright (C) 2006-2023 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.starter.server.service.facet.test;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.talend.sdk.component.starter.server.service.Resources.resourceFileToString;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import javax.inject.Inject;

import org.apache.meecrowave.junit5.MonoMeecrowaveConfig;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.starter.server.service.domain.Build;
import org.talend.sdk.component.starter.server.service.domain.ProjectRequest;
import org.talend.sdk.component.starter.server.service.facet.testing.TalendComponentKitTesting;
import org.talend.sdk.component.starter.server.service.info.ServerInfo;

@MonoMeecrowaveConfig
class TalendComponentKitTestingTest {

    @Inject
    private TalendComponentKitTesting generator;

    @Inject
    private ServerInfo info;

    private Build build = new Build("test", "test", null, "src/main/java", "src/test/java", "src/main/resources",
            "src/test/resources", "src/main/webapp", "pom.xml", "some pom", "target", emptyList());

    @Test
    void testSourceWithoutConf() {
        final Set<ProjectRequest.SourceConfiguration> sources =
                singleton(new ProjectRequest.SourceConfiguration("mycomp", "", false,
                        new ProjectRequest.DataStructure(new ArrayList<>()),
                        new ProjectRequest.StructureConfiguration(null, true)));

        String testFile = generator
                .create("foo.bar", build, emptyList(), sources, emptyList(), info.getSnapshot())
                .map(i -> new String(i.getContent(), StandardCharsets.UTF_8))
                .findFirst()
                .orElse(null);

        assertEquals(
                resourceFileToString("generated/TalendComponentKitTesting/testSourceWithoutConf/MycompSourceTest.java"),
                testFile);

    }

    @Test
    void testSourceWithConf() {
        final Set<ProjectRequest.SourceConfiguration> sources =
                singleton(new ProjectRequest.SourceConfiguration("mycomp", "", false, complexConfig(),
                        new ProjectRequest.StructureConfiguration(null, true)));

        String testFile = generator
                .create("foo.bar", build, emptyList(), sources, emptyList(), info.getSnapshot())
                .map(i -> new String(i.getContent(), StandardCharsets.UTF_8))
                .findFirst()
                .orElse(null);

        assertEquals(
                resourceFileToString("generated/TalendComponentKitTesting/testSourceWithConf/MycompSourceTest.java"),
                testFile);
    }

    @Test
    void testSourceWithNonGenericOutput() {
        final Set<ProjectRequest.SourceConfiguration> sources =
                singleton(new ProjectRequest.SourceConfiguration("mycomp", "", false, complexConfig(),
                        new ProjectRequest.StructureConfiguration(complexConfig(), false)));

        String testFile = generator
                .create("foo.bar", build, emptyList(), sources, emptyList(), info.getSnapshot())
                .map(i -> new String(i.getContent(), StandardCharsets.UTF_8))
                .findFirst()
                .orElse(null);

        assertEquals(
                resourceFileToString(
                        "generated/TalendComponentKitTesting/testSourceWithNonGenericOutput/MycompSourceTest.java"),
                testFile);
    }

    @Test
    void testProcessorWithoutConf() {
        final Set<ProjectRequest.ProcessorConfiguration> processors =
                singleton(new ProjectRequest.ProcessorConfiguration("mycomp", "",
                        new ProjectRequest.DataStructure(new ArrayList<>()),
                        singletonMap("__default__", new ProjectRequest.StructureConfiguration(null, true)),
                        singletonMap("__default__", new ProjectRequest.StructureConfiguration(null, true))));

        String testFile = generator
                .create("foo.bar", build, emptyList(), emptyList(), processors, info.getSnapshot())
                .map(i -> new String(i.getContent(), StandardCharsets.UTF_8))
                .findFirst()
                .orElse(null);

        assertEquals(
                resourceFileToString(
                        "generated/TalendComponentKitTesting/testProcessorWithoutConf/MycompProcessorTest.java"),
                testFile);

    }

    @Test
    void testProcessorWithConf() {
        final Set<ProjectRequest.ProcessorConfiguration> processors =
                singleton(new ProjectRequest.ProcessorConfiguration("mycomp", "", complexConfig(),
                        singletonMap("__default__", new ProjectRequest.StructureConfiguration(null, true)),
                        singletonMap("__default__", new ProjectRequest.StructureConfiguration(null, true))));

        String testFile = generator
                .create("foo.bar", build, emptyList(), emptyList(), processors, info.getSnapshot())
                .map(i -> new String(i.getContent(), StandardCharsets.UTF_8))
                .findFirst()
                .orElse(null);

        assertEquals(
                resourceFileToString(
                        "generated/TalendComponentKitTesting/testProcessorWithConf/MycompProcessorTest.java"),
                testFile);
    }

    @Test
    void testProcessorWithNonGenericOutput() {
        final Set<ProjectRequest.ProcessorConfiguration> processors =
                singleton(new ProjectRequest.ProcessorConfiguration("mycomp", "", complexConfig(),
                        new HashMap<String, ProjectRequest.StructureConfiguration>() {

                            {
                                put("__default__", new ProjectRequest.StructureConfiguration(complexConfig(), false));
                                put("input2", new ProjectRequest.StructureConfiguration(complexConfig(), false));
                                put("input3", new ProjectRequest.StructureConfiguration(complexConfig(), false));
                            }
                        }, new HashMap<String, ProjectRequest.StructureConfiguration>() {

                            {
                                put("__default__", new ProjectRequest.StructureConfiguration(complexConfig(), false));
                                put("reject", new ProjectRequest.StructureConfiguration(complexConfig(), false));
                            }
                        }));

        String testFile = generator
                .create("foo.bar", build, emptyList(), emptyList(), processors, info.getSnapshot())
                .map(i -> new String(i.getContent(), StandardCharsets.UTF_8))
                .findFirst()
                .orElse(null);

        assertEquals(resourceFileToString(
                "generated/TalendComponentKitTesting/testProcessorWithNonGenericOutput/MycompProcessorTest.java"),
                testFile);
    }

    private ProjectRequest.DataStructure complexConfig() {
        return new ProjectRequest.DataStructure(
                new ArrayList<>(
                        asList(new ProjectRequest.Entry("host", "string", null, null),
                                new ProjectRequest.Entry("port", "string", null, null),
                                new ProjectRequest.Entry("credential", "object", null,
                                        new ProjectRequest.DataStructure(new ArrayList<>(asList(
                                                new ProjectRequest.Entry("username", "string", null, null),
                                                new ProjectRequest.Entry("password", "string", null, null))))))));
    }

}
