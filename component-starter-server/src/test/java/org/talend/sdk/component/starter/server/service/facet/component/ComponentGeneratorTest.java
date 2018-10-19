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
                new ProjectRequest.DataStructure(singleton(new ProjectRequest.Entry("name", "string", null))),
                new ProjectRequest.StructureConfiguration(
                        new ProjectRequest.DataStructure(singleton(new ProjectRequest.Entry("name", "string", null))),
                        false)));
        final Map<String, String> files = generator
                .create("com.foo", build, "superfamily", "supercategory", sources, emptyList())
                .collect(toMap(FacetGenerator.InMemoryFile::getPath,
                        i -> new String(i.getContent(), StandardCharsets.UTF_8)));
        assertEquals(9, files.size());

        assertTrue(files.keySet().stream().anyMatch(k -> k.contains(".png")));

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
        final Map<String, String> files = generator
                .create("com.foo", build, "superfamily", "supercategory",
                        singleton(new ProjectRequest.SourceConfiguration("mycomp", "", false,
                                new ProjectRequest.DataStructure(singleton(new ProjectRequest.Entry("person", "",
                                        new ProjectRequest.DataStructure(
                                                asList(new ProjectRequest.Entry("name", "string", null),
                                                        new ProjectRequest.Entry("age", "int", null)))))),
                                new ProjectRequest.StructureConfiguration(new ProjectRequest.DataStructure(emptyList()),
                                        false))),
                        emptyList())
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
    void genericSource() {
        final Map<String, String> files = generator
                .create("com.foo", build, "superfamily", "supercategory",
                        singleton(new ProjectRequest.SourceConfiguration("mycomp", "", false, null, null)), emptyList())
                .collect(toMap(FacetGenerator.InMemoryFile::getPath,
                        i -> new String(i.getContent(), StandardCharsets.UTF_8)));

        assertEquals(resourceFileToString("generated/ComponentGeneratorTest/genericSource/MycompSource.java"),
                files.get("src/main/java/com/foo/source/MycompSource.java"));
    }

    @Test
    void genericStreamMapper() {
        final Map<String, String> files = generator
                .create("com.foo", build, "superfamily", "supercategory",
                        singleton(new ProjectRequest.SourceConfiguration("mycomp", "", true, null, null)), emptyList())
                .collect(toMap(FacetGenerator.InMemoryFile::getPath,
                        i -> new String(i.getContent(), StandardCharsets.UTF_8)));

        assertEquals(resourceFileToString("generated/ComponentGeneratorTest/genericStreamMapper/MycompMapper.java"),
                files.get("src/main/java/com/foo/source/MycompMapper.java"));
    }

    @Test
    void isolatedProcessor() {
        final Map<String, String> files = generator
                .create("com.foo", build, "superfamily", "supercategory", emptyList(),
                        singletonList(new ProjectRequest.ProcessorConfiguration("tProc", "", null, null, null)))
                .collect(toMap(FacetGenerator.InMemoryFile::getPath,
                        i -> new String(i.getContent(), StandardCharsets.UTF_8)));

        assertEquals(resourceFileToString("generated/ComponentGeneratorTest/isolatedProcessor/TProcProcessor.java"),
                files.get("src/main/java/com/foo/output/TProcOutput.java"));
    }

    @Test
    void processorOutput() {
        final Map<String, String> files = generator
                .create("com.foo", build, "superfamily", "supercategory", emptyList(),
                        singletonList(new ProjectRequest.ProcessorConfiguration("tProc", "", null, null,
                                new HashMap<String, ProjectRequest.StructureConfiguration>() {

                                    {
                                        put("__default__",
                                                new ProjectRequest.StructureConfiguration(
                                                        new ProjectRequest.DataStructure(singletonList(
                                                                new ProjectRequest.Entry("name", "string", null))),
                                                        false));
                                    }
                                })))
                .collect(toMap(FacetGenerator.InMemoryFile::getPath,
                        i -> new String(i.getContent(), StandardCharsets.UTF_8)));

        assertEquals(resourceFileToString("generated/ComponentGeneratorTest/processorOutput/TProcProcessor.java"),
                files.get("src/main/java/com/foo/processor/TProcProcessor.java"));

        assertEquals(resourceFileToString("generated/ComponentGeneratorTest/processorOutput/TProcDefaultOutput.java"),
                files.get("src/main/java/com/foo/processor/TProcDefaultOutput.java"));

    }

    @Test
    void processorGenericOutput() {
        final Map<String, String> files =
                generator
                        .create("com.foo", build, "superfamily", "supercategory", emptyList(),
                                singletonList(new ProjectRequest.ProcessorConfiguration("tProc", "", null, null,
                                        singletonMap("__default__",
                                                new ProjectRequest.StructureConfiguration(null, true)))))
                        .collect(toMap(FacetGenerator.InMemoryFile::getPath,
                                i -> new String(i.getContent(), StandardCharsets.UTF_8)));

        assertNull(files.get("src/main/java/com/foo/processor/TProcDefaultOutput.java"));
        assertEquals(
                resourceFileToString("generated/ComponentGeneratorTest/processorGenericOutput/TProcProcessor.java"),
                files.get("src/main/java/com/foo/processor/TProcProcessor.java"));
    }

    @Test
    void processorInput() {
        final Map<String, String> files = generator
                .create("com.foo", build, "superfamily", "supercategory", emptyList(),
                        singletonList(new ProjectRequest.ProcessorConfiguration("tProc", "", null, singletonMap(
                                "__default__",
                                new ProjectRequest.StructureConfiguration(new ProjectRequest.DataStructure(
                                        singleton(new ProjectRequest.Entry("name", "string", null))), false)),
                                null)))
                .collect(toMap(FacetGenerator.InMemoryFile::getPath,
                        i -> new String(i.getContent(), StandardCharsets.UTF_8)));

        assertEquals(resourceFileToString("generated/ComponentGeneratorTest/processorInput/TProcProcessor.java"),
                files.get("src/main/java/com/foo/output/TProcOutput.java"));

        assertEquals(resourceFileToString("generated/ComponentGeneratorTest/processorInput/TProcDefaultInput.java"),
                files.get("src/main/java/com/foo/output/TProcDefaultInput.java"));
    }

    @Test
    void processorGenericInput() {
        final Map<String, String> files =
                generator
                        .create("com.foo", build, "superfamily", "supercategory", emptyList(),
                                singletonList(new ProjectRequest.ProcessorConfiguration("tProc", "", null,
                                        singletonMap("__default__",
                                                new ProjectRequest.StructureConfiguration(null, true)),
                                        null)))
                        .collect(toMap(FacetGenerator.InMemoryFile::getPath,
                                i -> new String(i.getContent(), StandardCharsets.UTF_8)));

        assertEquals(resourceFileToString("generated/ComponentGeneratorTest/processorGenericInput/TProcProcessor.java"),
                files.get("src/main/java/com/foo/output/TProcOutput.java"));
    }

    @Test
    void standardProcessor() {
        final Map<String, String> files = generator
                .create("com.foo", build, "superfamily", "supercategory", emptyList(),
                        singletonList(new ProjectRequest.ProcessorConfiguration("tProc", "", null,
                                new HashMap<String, ProjectRequest.StructureConfiguration>() {

                                    {

                                        put("__default__", new ProjectRequest.StructureConfiguration(null, true));
                                        put("Input_1", new ProjectRequest.StructureConfiguration(null, true));
                                    }
                                }, new HashMap<String, ProjectRequest.StructureConfiguration>() {

                                    {
                                        put("__default__", new ProjectRequest.StructureConfiguration(
                                                new ProjectRequest.DataStructure(
                                                        singleton(new ProjectRequest.Entry("age", "int", null))),
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
                                })))
                .collect(toMap(FacetGenerator.InMemoryFile::getPath,
                        i -> new String(i.getContent(), StandardCharsets.UTF_8)));

        assertEquals(resourceFileToString("generated/ComponentGeneratorTest/standardProcessor/TProcProcessor.java"),
                files.get("src/main/java/com/foo/processor/TProcProcessor.java"));

        assertEquals(resourceFileToString("generated/ComponentGeneratorTest/standardProcessor/TProcDefaultOutput.java"),
                files.get("src/main/java/com/foo/processor/TProcDefaultOutput.java"));
    }

    @Test
    void configurationWithCredential() {

        ProjectRequest.DataStructure config = new ProjectRequest.DataStructure(asList(
                new ProjectRequest.Entry("host", "string", null), new ProjectRequest.Entry("port", "string", null),
                new ProjectRequest.Entry("credential", null, // type
                        // with
                        // nested
                        new ProjectRequest.DataStructure(asList(new ProjectRequest.Entry("username", "string", null),
                                new ProjectRequest.Entry("password", "string", null))))));

        final Map<String, String> files =
                generator
                        .create("com.foo", build, "superfamily", "supercategory", emptyList(),
                                singletonList(new ProjectRequest.ProcessorConfiguration("tProc", "", config,
                                        singletonMap("__default__",
                                                new ProjectRequest.StructureConfiguration(null, true)),
                                        emptyMap())))
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
