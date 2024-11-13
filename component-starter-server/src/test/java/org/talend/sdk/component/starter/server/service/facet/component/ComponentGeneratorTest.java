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
package org.talend.sdk.component.starter.server.service.facet.component;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.apache.meecrowave.junit5.MonoMeecrowaveConfig;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.starter.server.service.domain.Build;
import org.talend.sdk.component.starter.server.service.domain.ProjectRequest;
import org.talend.sdk.component.starter.server.service.facet.FacetGenerator;

@MonoMeecrowaveConfig
class ComponentGeneratorTest {

    @Inject
    private ComponentGenerator generator;

    private Build build = new Build("test", "test", null, "src/main/java", "src/test/java", "src/main/resources",
            "src/test/resources", "src/main/webapp", "pom.xml", "some pom", "target", emptyList());

    @Test
    void source() {
        final Set<ProjectRequest.SourceConfiguration> sources = singleton(new ProjectRequest.SourceConfiguration(
                "mycomp", "", false,
                new ProjectRequest.DataStructure(
                        new ArrayList<>(singleton(new ProjectRequest.Entry("name", "string", null, null)))),
                new ProjectRequest.StructureConfiguration(
                        new ProjectRequest.DataStructure(
                                new ArrayList<>(singleton(new ProjectRequest.Entry("name", "string", null, null)))),
                        false)));
        final Map<String, String> files = generator
                .create("com.foo", build, "superfamily", "supercategory", sources, emptyList(), emptyList())
                .collect(toMap(FacetGenerator.InMemoryFile::getPath,
                        i -> new String(i.getContent(), StandardCharsets.UTF_8)));
        assertEquals(14, files.size());

        assertTrue(files.keySet().stream().anyMatch(k -> k.contains(".svg")));

        assertEquals(resourceFileToString("generated/ComponentGeneratorTest/source/package-info.java"),
                files.get("src/main/java/com/foo/package-info.java").trim());

        assertEquals(resourceFileToString("generated/ComponentGeneratorTest/source/package-info.java"),
                files.get("src/main/java/com/foo/package-info.java").trim());

        assertEquals(resourceFileToString("generated/ComponentGeneratorTest/source/TestService.java"),
                files.get("src/main/java/com/foo/service/TestService.java"));

        assertEquals(resourceFileToString("generated/ComponentGeneratorTest/source/MycompSource.java"),
                files.get("src/main/java/com/foo/source/MycompSource.java"));

        assertEquals(resourceFileToString("generated/ComponentGeneratorTest/source/MycompMapper.java"),
                files.get("src/main/java/com/foo/source/MycompMapper.java"));

        assertEquals(resourceFileToString("generated/ComponentGeneratorTest/source/MycompMapperConfiguration.java"),
                files.get("src/main/java/com/foo/source/MycompMapperConfiguration.java"));

        assertEquals(resourceFileToString("generated/ComponentGeneratorTest/source/MycompRecord.java"),
                files.get("src/main/java/com/foo/source/MycompRecord.java"));

        assertEquals("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 16 16\"><path d=\"M6 14L0 8l1.9-1.9L6 "
                + "10.2 14.1 2 16 3.9z\"/></svg>", files.get("src/main/resources/icons/mycomp.svg"));
        assertEquals("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 16 16\"><path d=\"M6 14L0 8l1.9-1.9L6 "
                + "10.2 14.1 2 16 3.9z\"/></svg>", files.get("src/main/resources/icons/dark/mycomp.svg"));
        assertEquals("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 16 16\"><path d=\"M6 14L0 8l1.9-1.9L6 "
                + "10.2 14.1 2 16 3.9z\"/></svg>", files.get("src/main/resources/icons/light/mycomp.svg"));
        assertEquals("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 16 16\"><path d=\"M6 14L0 8l1.9-1.9L6 "
                + "10.2 14.1 2 16 3.9z\"/></svg>", files.get("src/main/resources/icons/superfamily.svg"));
        assertEquals("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 16 16\"><path d=\"M6 14L0 8l1.9-1.9L6 "
                + "10.2 14.1 2 16 3.9z\"/></svg>", files.get("src/main/resources/icons/dark/superfamily.svg"));
        assertEquals("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 16 16\"><path d=\"M6 14L0 8l1.9-1.9L6 "
                + "10.2 14.1 2 16 3.9z\"/></svg>", files.get("src/main/resources/icons/light/superfamily.svg"));

        assertTrue(files.get("src/main/java/com/foo/source/MycompMapper.java").contains("custom = \"mycomp\""));
    }

    private String resourceFileToString(final String filePath) {
        try (final BufferedReader reader =
                new BufferedReader(new InputStreamReader(getClass().getClassLoader().getResourceAsStream(filePath)))) {
            return reader.lines().collect(joining("\n"));
        } catch (final IOException e) {
            fail(e.getMessage());
            throw new IllegalStateException(e);
        }
    }

    @Test
    void sourceComplexConfiguration() {
        final Map<String, String> files =
                generator
                        .create("com.foo", build, "superfamily", "supercategory",
                                singleton(new ProjectRequest.SourceConfiguration("mycomp", "", false,
                                        new ProjectRequest.DataStructure(
                                                new ArrayList<>(singleton(new ProjectRequest.Entry("person", "", null,
                                                        new ProjectRequest.DataStructure(new ArrayList<>(asList(
                                                                new ProjectRequest.Entry("name", "string", null, null),
                                                                new ProjectRequest.Entry("age", "int", null,
                                                                        null)))))))),
                                        new ProjectRequest.StructureConfiguration(
                                                new ProjectRequest.DataStructure(new ArrayList<>()), false))),
                                emptyList(), emptyList())
                        .collect(toMap(FacetGenerator.InMemoryFile::getPath,
                                i -> new String(i.getContent(), StandardCharsets.UTF_8)));

        assertEquals(
                resourceFileToString(
                        "generated/ComponentGeneratorTest/sourceComplexConfiguration/MycompMapperConfiguration.java"),
                files.get("src/main/java/com/foo/source/MycompMapperConfiguration.java"));

        assertEquals(
                resourceFileToString(
                        "generated/ComponentGeneratorTest/sourceComplexConfiguration/PersonConfiguration.java"),
                files.get("src/main/java/com/foo/source/PersonConfiguration.java"));

        assertEquals(
                resourceFileToString(
                        "generated/ComponentGeneratorTest/sourceComplexConfiguration/RootMessages.properties"),
                files.get("src/main/resources/com/foo/Messages.properties"));

        assertEquals(
                resourceFileToString(
                        "generated/ComponentGeneratorTest/sourceComplexConfiguration/SourceMessages.properties"),
                files.get("src/main/resources/com/foo/source/Messages.properties"));
    }

    @Test
    void withDataSetDataStore() {
        final Map<String, String> files = generator
                .create("com.foo", build, "superfamily", "supercategory",
                        singleton(new ProjectRequest.SourceConfiguration("mycomp", "", false,
                                new ProjectRequest.DataStructure(new ArrayList<>(asList(
                                        new ProjectRequest.Entry("identifier", "string", null, null),
                                        new ProjectRequest.Entry("theDataSet", "com.foo.dataset.MyDataSet",
                                                "1234-4567-7890", null)))),
                                new ProjectRequest.StructureConfiguration(
                                        new ProjectRequest.DataStructure(new ArrayList<>()), false))),
                        emptyList(), asList(
                                new ProjectRequest.ReusableConfiguration("1234-4567-7890", "com.foo.dataset.MyDataSet",
                                        new ProjectRequest.DataStructure(
                                                new ArrayList<>(singleton(new ProjectRequest.Entry("theDatastore",
                                                        "com.foo.datastore.MyDataStore", "2234-4567-7890", null)))),
                                        "dataset"),
                                new ProjectRequest.ReusableConfiguration("2234-4567-7890",
                                        "com.foo.datastore.MyDataStore",
                                        new ProjectRequest.DataStructure(
                                                new ArrayList<>(singleton(new ProjectRequest.Entry("person", "", null,
                                                        new ProjectRequest.DataStructure(new ArrayList<>(asList(
                                                                new ProjectRequest.Entry("name", "string", null, null),
                                                                new ProjectRequest.Entry("age", "int", null,
                                                                        null)))))))),
                                        "datastore")))
                .collect(toMap(FacetGenerator.InMemoryFile::getPath,
                        i -> new String(i.getContent(), StandardCharsets.UTF_8)));

        assertEquals(resourceFileToString("generated/ComponentGeneratorTest/withDataSetDataStore/Messages.properties")
                .trim(), files.get("src/main/resources/com/foo/Messages.properties").trim());

        assertEquals(
                resourceFileToString("generated/ComponentGeneratorTest/withDataSetDataStore/MyDataStore.java").trim(),
                files.get("src/main/java/com/foo/datastore/MyDataStore.java").trim());

        assertEquals(
                resourceFileToString(
                        "generated/ComponentGeneratorTest/withDataSetDataStore/MyDataStorePersonConfiguration.java")
                        .trim(),
                files.get("src/main/java/com/foo/datastore/MyDataStorePersonConfiguration.java").trim());

        assertEquals(
                resourceFileToString("generated/ComponentGeneratorTest/withDataSetDataStore/MyDataSet.java").trim(),
                files.get("src/main/java/com/foo/dataset/MyDataSet.java").trim());

        assertEquals(
                resourceFileToString(
                        "generated/ComponentGeneratorTest/withDataSetDataStore/MycompMapperConfiguration.java").trim(),
                files.get("src/main/java/com/foo/source/MycompMapperConfiguration.java").trim());
    }

    @Test
    void genericSource() {
        final Map<String, String> files = generator
                .create("com.foo", build, "superfamily", "supercategory",
                        singleton(new ProjectRequest.SourceConfiguration("mycomp", "", false,
                                new ProjectRequest.DataStructure(new ArrayList<>()), null)),
                        emptyList(), emptyList())
                .collect(toMap(FacetGenerator.InMemoryFile::getPath,
                        i -> new String(i.getContent(), StandardCharsets.UTF_8)));

        assertEquals(resourceFileToString("generated/ComponentGeneratorTest/genericSource/MycompSource.java"),
                files.get("src/main/java/com/foo/source/MycompSource.java"));
    }

    @Test
    void genericStreamMapper() {
        final Map<String, String> files = generator
                .create("com.foo", build, "superfamily", "supercategory",
                        singleton(new ProjectRequest.SourceConfiguration("mycomp", "", true,
                                new ProjectRequest.DataStructure(new ArrayList<>()), null)),
                        emptyList(), emptyList())
                .collect(toMap(FacetGenerator.InMemoryFile::getPath,
                        i -> new String(i.getContent(), StandardCharsets.UTF_8)));

        assertEquals(resourceFileToString("generated/ComponentGeneratorTest/genericStreamMapper/MycompMapper.java"),
                files.get("src/main/java/com/foo/source/MycompMapper.java"));
    }

    @Test
    void isolatedProcessor() {
        final Map<String, String> files = generator
                .create("com.foo", build, "superfamily", "supercategory", emptyList(),
                        singletonList(new ProjectRequest.ProcessorConfiguration("tProc", "",
                                new ProjectRequest.DataStructure(new ArrayList<>()), null, null)),
                        emptyList())
                .collect(toMap(FacetGenerator.InMemoryFile::getPath,
                        i -> new String(i.getContent(), StandardCharsets.UTF_8)));

        assertEquals(resourceFileToString("generated/ComponentGeneratorTest/isolatedProcessor/TProcProcessor.java"),
                files.get("src/main/java/com/foo/output/TProcOutput.java"));
    }

    @Test
    void processorOutput() {
        final Map<String, String> files = generator
                .create("com.foo", build, "superfamily", "supercategory", emptyList(),
                        singletonList(new ProjectRequest.ProcessorConfiguration("tProc", "",
                                new ProjectRequest.DataStructure(new ArrayList<>()), null,
                                new HashMap<String, ProjectRequest.StructureConfiguration>() {

                                    {
                                        put("__default__", new ProjectRequest.StructureConfiguration(
                                                new ProjectRequest.DataStructure(new ArrayList<>(singletonList(
                                                        new ProjectRequest.Entry("name", "string", null, null)))),
                                                false));
                                    }
                                })),
                        emptyList())
                .collect(toMap(FacetGenerator.InMemoryFile::getPath,
                        i -> new String(i.getContent(), StandardCharsets.UTF_8)));

        assertEquals(resourceFileToString("generated/ComponentGeneratorTest/processorOutput/TProcProcessor.java"),
                files.get("src/main/java/com/foo/processor/TProcProcessor.java"));

        assertEquals(resourceFileToString("generated/ComponentGeneratorTest/processorOutput/TProcDefaultOutput.java"),
                files.get("src/main/java/com/foo/processor/TProcDefaultOutput.java"));

        assertEquals(
                resourceFileToString(
                        "generated/ComponentGeneratorTest/processorOutput/TProcProcessorConfiguration.java"),
                files.get("src/main/java/com/foo/processor/TProcProcessorConfiguration.java"));

    }

    @Test
    void processorGenericOutput() {
        final Map<String, String> files =
                generator
                        .create("com.foo", build, "superfamily", "supercategory", emptyList(),
                                singletonList(new ProjectRequest.ProcessorConfiguration("tProc", "",
                                        new ProjectRequest.DataStructure(new ArrayList<>()), null,
                                        singletonMap("__default__",
                                                new ProjectRequest.StructureConfiguration(null, true)))),
                                emptyList())
                        .collect(toMap(FacetGenerator.InMemoryFile::getPath,
                                i -> new String(i.getContent(), StandardCharsets.UTF_8)));

        assertNull(files.get("src/main/java/com/foo/processor/TProcDefaultOutput.java"));
        assertEquals(
                resourceFileToString("generated/ComponentGeneratorTest/processorGenericOutput/TProcProcessor.java"),
                files.get("src/main/java/com/foo/processor/TProcProcessor.java"));

        assertEquals(
                resourceFileToString(
                        "generated/ComponentGeneratorTest/processorGenericOutput/TProcProcessorConfiguration.java"),
                files.get("src/main/java/com/foo/processor/TProcProcessorConfiguration.java"));
    }

    @Test
    void processorInput() {
        final Map<String, String> files =
                generator
                        .create("com.foo", build, "superfamily", "supercategory", emptyList(),
                                singletonList(new ProjectRequest.ProcessorConfiguration("tProc", "",
                                        new ProjectRequest.DataStructure(new ArrayList<>()),
                                        singletonMap("__default__", new ProjectRequest.StructureConfiguration(
                                                new ProjectRequest.DataStructure(new ArrayList<>(singleton(
                                                        new ProjectRequest.Entry("name", "string", null, null)))),
                                                false)),
                                        null)),
                                emptyList())
                        .collect(toMap(FacetGenerator.InMemoryFile::getPath,
                                i -> new String(i.getContent(), StandardCharsets.UTF_8)));

        assertEquals(resourceFileToString("generated/ComponentGeneratorTest/processorInput/TProcProcessor.java"),
                files.get("src/main/java/com/foo/output/TProcOutput.java"));

        assertEquals(resourceFileToString("generated/ComponentGeneratorTest/processorInput/TProcDefaultInput.java"),
                files.get("src/main/java/com/foo/output/TProcDefaultInput.java"));

        assertEquals(
                resourceFileToString("generated/ComponentGeneratorTest/processorInput/TProcOutputConfiguration.java"),
                files.get("src/main/java/com/foo/output/TProcOutputConfiguration.java"));
    }

    @Test
    void processorGenericInput() {
        final Map<String, String> files =
                generator
                        .create("com.foo", build, "superfamily", "supercategory", emptyList(),
                                singletonList(new ProjectRequest.ProcessorConfiguration("tProc", "",
                                        new ProjectRequest.DataStructure(new ArrayList<>()),
                                        singletonMap("__default__",
                                                new ProjectRequest.StructureConfiguration(null, true)),
                                        null)),
                                emptyList())
                        .collect(toMap(FacetGenerator.InMemoryFile::getPath,
                                i -> new String(i.getContent(), StandardCharsets.UTF_8)));

        assertEquals(resourceFileToString("generated/ComponentGeneratorTest/processorGenericInput/TProcProcessor.java"),
                files.get("src/main/java/com/foo/output/TProcOutput.java"));

        assertEquals(
                resourceFileToString(
                        "generated/ComponentGeneratorTest/processorGenericInput/TProcOutputConfiguration.java"),
                files.get("src/main/java/com/foo/output/TProcOutputConfiguration.java"));
    }

    @Test
    void standardProcessor() {
        final Map<String, String> files = generator
                .create("com.foo", build, "superfamily", "supercategory", emptyList(),
                        singletonList(new ProjectRequest.ProcessorConfiguration("tProc", "",
                                new ProjectRequest.DataStructure(new ArrayList<>()),
                                new HashMap<String, ProjectRequest.StructureConfiguration>() {

                                    {

                                        put("__default__", new ProjectRequest.StructureConfiguration(null, true));
                                        put("Input_1", new ProjectRequest.StructureConfiguration(null, true));
                                    }
                                }, new HashMap<String, ProjectRequest.StructureConfiguration>() {

                                    {
                                        put("__default__", new ProjectRequest.StructureConfiguration(
                                                new ProjectRequest.DataStructure(new ArrayList<>(
                                                        singleton(new ProjectRequest.Entry("age", "int", null, null)))),
                                                false));
                                        put("reject", new ProjectRequest.StructureConfiguration(null, true));
                                        put("reject2", new ProjectRequest.StructureConfiguration(null, true));
                                        put("reject3", new ProjectRequest.StructureConfiguration(null, true)); // to
                                        // test
                                        // the
                                        // sort
                                        // of
                                        // branches
                                    }
                                })),
                        emptyList())
                .collect(toMap(FacetGenerator.InMemoryFile::getPath,
                        i -> new String(i.getContent(), StandardCharsets.UTF_8)));

        assertEquals(resourceFileToString("generated/ComponentGeneratorTest/standardProcessor/TProcProcessor.java"),
                files.get("src/main/java/com/foo/processor/TProcProcessor.java"));

        assertEquals(resourceFileToString("generated/ComponentGeneratorTest/standardProcessor/TProcDefaultOutput.java"),
                files.get("src/main/java/com/foo/processor/TProcDefaultOutput.java"));
    }

    @Test
    void configurationWithCredential() {

        ProjectRequest.DataStructure config =
                new ProjectRequest.DataStructure(
                        new ArrayList<>(asList(new ProjectRequest.Entry("host", "string", null, null),
                                new ProjectRequest.Entry("port", "string", null, null),
                                new ProjectRequest.Entry("credential", null, null,
                                        new ProjectRequest.DataStructure(new ArrayList<>(asList(
                                                new ProjectRequest.Entry("username", "string", null, null),
                                                new ProjectRequest.Entry("password", "string", null, null))))))));

        final Map<String, String> files =
                generator
                        .create("com.foo", build, "superfamily", "supercategory", emptyList(),
                                singletonList(new ProjectRequest.ProcessorConfiguration("tProc", "", config,
                                        singletonMap("__default__",
                                                new ProjectRequest.StructureConfiguration(null, true)),
                                        emptyMap())),
                                emptyList())
                        .collect(toMap(FacetGenerator.InMemoryFile::getPath,
                                i -> new String(i.getContent(), StandardCharsets.UTF_8)));

        assertEquals(
                resourceFileToString(
                        "generated/ComponentGeneratorTest/configurationWithCredential/CredentialConfiguration.java"),
                files.get("src/main/java/com/foo/output/CredentialConfiguration.java"));

        assertEquals(
                resourceFileToString(
                        "generated/ComponentGeneratorTest/configurationWithCredential/TProcOutputConfiguration.java"),
                files.get("src/main/java/com/foo/output/TProcOutputConfiguration.java"));

    }
}
