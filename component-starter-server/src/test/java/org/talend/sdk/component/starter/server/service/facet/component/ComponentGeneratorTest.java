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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;

import org.apache.meecrowave.junit.MonoMeecrowave;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.talend.sdk.component.starter.server.service.domain.Build;
import org.talend.sdk.component.starter.server.service.domain.ProjectRequest;
import org.talend.sdk.component.starter.server.service.facet.FacetGenerator;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(MonoMeecrowave.Runner.class)
public class ComponentGeneratorTest {

    @Inject
    private ComponentGenerator generator;

    private Build build = new Build("test", "src/main/java", "src/test/java", "src/main/resources", "src/test/resources",
            "src/main/webapp", "pom.xml", "some pom", "target");

    @Test
    public void source() {
        final Set<ProjectRequest.SourceConfiguration> sources = singleton(new ProjectRequest.SourceConfiguration("mycomp", "",
                false, new ProjectRequest.DataStructure(singleton(new ProjectRequest.Entry("name", "string", null))),
                new ProjectRequest.StructureConfiguration(
                        new ProjectRequest.DataStructure(singleton(new ProjectRequest.Entry("name", "string", null))), false)));
        final Map<String, String> files = generator.create("com.foo", build, "superfamily", "supercategory", sources, emptyList())
                                                   .collect(toMap(FacetGenerator.InMemoryFile::getPath,
                                                           i -> new String(i.getContent(), StandardCharsets.UTF_8)));
        assertEquals(7, files.size());

        assertTrue(files.keySet().stream().filter(k -> k.contains(".png")).findFirst().isPresent());

        assertEquals(resourceFileToString("generated/ComponentGeneratorTest/source/package-info.java"),
                files.get("src/main/java/com/foo/package-info.java"));

        assertEquals(resourceFileToString("generated/ComponentGeneratorTest/source/TestService.java"),
                files.get("src/main/java/com/foo/service/TestService.java"));

        assertEquals(resourceFileToString("generated/ComponentGeneratorTest/source/MycompSource.java"),
                files.get("src/main/java/com/foo/source/MycompSource.java"));

        assertEquals(resourceFileToString("generated/ComponentGeneratorTest/source/MycompMapper.java"),
                files.get("src/main/java/com/foo/source/MycompMapper.java"));

        assertEquals(resourceFileToString("generated/ComponentGeneratorTest/source/MycompSourceConfiguration.java"),
                files.get("src/main/java/com/foo/source/MycompSourceConfiguration.java"));

        assertEquals(resourceFileToString("generated/ComponentGeneratorTest/source/MycompRecord.java"),
                files.get("src/main/java/com/foo/source/MycompRecord.java"));
    }

    private String resourceFileToString(String filePath) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[1024];
        try {
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream(filePath);
            while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            buffer.flush();
            byte[] byteArray = buffer.toByteArray();
            return new String(byteArray, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Test
    public void sourceComplexConfiguration() {
        final Map<String, String> files = generator
                .create("com.foo", build, "superfamily", "supercategory",
                        singleton(new ProjectRequest.SourceConfiguration("mycomp", "", false,
                                new ProjectRequest.DataStructure(singleton(new ProjectRequest.Entry("person", "",
                                        new ProjectRequest.DataStructure(asList(new ProjectRequest.Entry("name", "string", null),
                                                new ProjectRequest.Entry("age", "int", null)))))),
                                new ProjectRequest.StructureConfiguration(new ProjectRequest.DataStructure(emptyList()), false))),
                        emptyList())
                .collect(toMap(FacetGenerator.InMemoryFile::getPath, i -> new String(i.getContent(), StandardCharsets.UTF_8)));

        assertEquals(resourceFileToString("generated/ComponentGeneratorTest/sourceComplexConfiguration/MycompSourceConfiguration.java"),
                files.get("src/main/java/com/foo/source/MycompSourceConfiguration.java"));

        assertEquals(resourceFileToString("generated/ComponentGeneratorTest/sourceComplexConfiguration/PersonConfiguration.java"),
                files.get("src/main/java/com/foo/source/PersonConfiguration.java"));
    }

    @Test
    public void genericSource() {
        final Map<String, String> files = generator
                .create("com.foo", build, "superfamily", "supercategory",
                        singleton(new ProjectRequest.SourceConfiguration("mycomp", "", false, null, null)), emptyList())
                .collect(toMap(FacetGenerator.InMemoryFile::getPath, i -> new String(i.getContent(), StandardCharsets.UTF_8)));

        assertEquals(resourceFileToString("generated/ComponentGeneratorTest/genericSource/MycompSource.java"),
                files.get("src/main/java/com/foo/source/MycompSource.java"));
    }

    @Test
    public void genericStreamMapper() {
        final Map<String, String> files = generator
                .create("com.foo", build, "superfamily", "supercategory",
                        singleton(new ProjectRequest.SourceConfiguration("mycomp", "", true, null, null)), emptyList())
                .collect(toMap(FacetGenerator.InMemoryFile::getPath, i -> new String(i.getContent(), StandardCharsets.UTF_8)));

        assertEquals(resourceFileToString("generated/ComponentGeneratorTest/genericStreamMapper/MycompMapper.java"),
                files.get("src/main/java/com/foo/source/MycompMapper.java"));
    }

    @Test
    public void isolatedProcessor() {
        final Map<String, String> files = generator
                .create("com.foo", build, "superfamily", "supercategory", emptyList(),
                        singletonList(new ProjectRequest.ProcessorConfiguration("tProc", "", null, null, null)))
                .collect(toMap(FacetGenerator.InMemoryFile::getPath, i -> new String(i.getContent(), StandardCharsets.UTF_8)));

        assertEquals(resourceFileToString("generated/ComponentGeneratorTest/isolatedProcessor/TProcProcessor.java"),
                files.get("src/main/java/com/foo/output/TProcProcessor.java"));
    }

    @Test
    public void processorOutput() {
        final Map<String, String> files = generator.create("com.foo", build, "superfamily", "supercategory", emptyList(),
                singletonList(new ProjectRequest.ProcessorConfiguration("tProc", "", null, null,
                        new HashMap<String, ProjectRequest.StructureConfiguration>() {

                            {
                                put("__default__", new ProjectRequest.StructureConfiguration(new ProjectRequest.DataStructure(
                                        singletonList(new ProjectRequest.Entry("name", "string", null))), false));
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
    public void processorGenericOutput() {
        final Map<String, String> files = generator
                .create("com.foo", build, "superfamily", "supercategory", emptyList(),
                        singletonList(new ProjectRequest.ProcessorConfiguration("tProc", "", null, null,
                                singletonMap("__default__", new ProjectRequest.StructureConfiguration(null, true)))))
                .collect(toMap(FacetGenerator.InMemoryFile::getPath, i -> new String(i.getContent(), StandardCharsets.UTF_8)));

        assertNull(files.get("src/main/java/com/foo/processor/TProcDefaultOutput.java"));
        assertEquals(resourceFileToString("generated/ComponentGeneratorTest/processorGenericOutput/TProcProcessor.java"),
                files.get("src/main/java/com/foo/processor/TProcProcessor.java"));
    }

    @Test
    public void processorInput() {
        final Map<String, String> files = generator
                .create("com.foo", build, "superfamily", "supercategory", emptyList(),
                        singletonList(new ProjectRequest.ProcessorConfiguration("tProc", "", null,
                                singletonMap("__default__",
                                        new ProjectRequest.StructureConfiguration(new ProjectRequest.DataStructure(
                                                singleton(new ProjectRequest.Entry("name", "string", null))), false)),
                                null)))
                .collect(toMap(FacetGenerator.InMemoryFile::getPath, i -> new String(i.getContent(), StandardCharsets.UTF_8)));

        assertEquals(resourceFileToString("generated/ComponentGeneratorTest/processorInput/TProcProcessor.java"),
                files.get("src/main/java/com/foo/output/TProcProcessor.java"));

        assertEquals(resourceFileToString("generated/ComponentGeneratorTest/processorInput/TProcDefaultInput.java"),
                files.get("src/main/java/com/foo/output/TProcDefaultInput.java"));
    }

    @Test
    public void processorGenericInput() {
        final Map<String, String> files = generator
                .create("com.foo", build, "superfamily", "supercategory", emptyList(),
                        singletonList(new ProjectRequest.ProcessorConfiguration("tProc", "", null,
                                singletonMap("__default__", new ProjectRequest.StructureConfiguration(null, true)), null)))
                .collect(toMap(FacetGenerator.InMemoryFile::getPath, i -> new String(i.getContent(), StandardCharsets.UTF_8)));

        assertEquals(resourceFileToString("generated/ComponentGeneratorTest/processorGenericInput/TProcProcessor.java"),
                files.get("src/main/java/com/foo/output/TProcProcessor.java"));
    }

    @Test
    public void standardProcessor() {
        final Map<String, String> files = generator.create("com.foo", build, "superfamily", "supercategory", emptyList(),
                singletonList(new ProjectRequest.ProcessorConfiguration("tProc", "", null,
                        singletonMap("__default__", new ProjectRequest.StructureConfiguration(null, true)),
                        new HashMap<String, ProjectRequest.StructureConfiguration>() {

                            {
                                put("__default__", new ProjectRequest.StructureConfiguration(
                                        new ProjectRequest.DataStructure(singleton(new ProjectRequest.Entry("age", "int", null))),
                                        false));
                                put("reject", new ProjectRequest.StructureConfiguration(null, true));
                            }
                        })))
                                                   .collect(toMap(FacetGenerator.InMemoryFile::getPath,
                                                           i -> new String(i.getContent(), StandardCharsets.UTF_8)));

        assertEquals(resourceFileToString("generated/ComponentGeneratorTest/standardProcessor/TProcProcessor.java"),
                files.get("src/main/java/com/foo/processor/TProcProcessor.java"));

        assertEquals(resourceFileToString("generated/ComponentGeneratorTest/standardProcessor/TProcDefaultOutput.java"),
                files.get("src/main/java/com/foo/processor/TProcDefaultOutput.java"));
    }
}
