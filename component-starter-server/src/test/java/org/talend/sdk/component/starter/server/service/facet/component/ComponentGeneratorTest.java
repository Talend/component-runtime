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
package org.talend.sdk.component.starter.server.service.facet.component;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

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
                .collect(toMap(FacetGenerator.InMemoryFile::getPath, i -> new String(i.getContent(), StandardCharsets.UTF_8)));
        assertEquals(7, files.size());
        assertEquals(
                "// this tells the framework in which family (group of components) and categories (UI grouping)\n"
                        + "// the components in the nested packages belong to\n"
                        + "@Components(family = \"superfamily\", categories = \"supercategory\")\n"
                        + "@Icon(value = Icon.IconType.CUSTOM, custom = \"superfamily\")\n" + "package com.foo.service;\n" + "\n"
                        + "import org.talend.sdk.component.api.component.Components;\n"
                        + "import org.talend.sdk.component.api.component.Icon;\n",
                files.get("src/main/java/com/foo/package-info.java"));
        assertEquals(
                "package com.foo.service;\n" + "\n" + "import org.talend.sdk.component.api.service.Service;\n" + "\n"
                        + "@Service\n" + "public class SalesforceService {\n" + "\n"
                        + "    // you can put logic here you can reuse in components\n" + "\n" + "}",
                files.get("src/main/java/com/foo/service/TestService.java"));
        assertEquals("package com.foo.source;\n" + "\n" + "import javax.annotation.PostConstruct;\n"
                + "import javax.annotation.PreDestroy;\n" + "\n" + "import org.talend.sdk.component.api.input.Producer;\n" + "\n"
                + "import com.foo.service.TestService;\n" + "\n" + "public class MycompSource implements Serializable {\n"
                + "    private final MycompSourceConfiguration configuration;\n" + "    private final TestService service;\n"
                + "\n" + "    public MycompSource(@Option(\"configuration\") final MycompSourceConfiguration configuration,\n"
                + "                         final TestService service) {\n" + "        this.configuration = configuration;\n"
                + "        this.service = service;\n" + "    }\n" + "\n" + "    @PostConstruct\n" + "    public void init() {\n"
                + "        // this method will be executed once for the whole component execution,\n"
                + "        // this is where you can establish a connection for instance\n" + "    }\n" + "\n" + "    @Producer\n"
                + "    public MycompRecord next() {\n"
                + "        // this is the method allowing you to go through the dataset associated\n"
                + "        // to the component configuration\n" + "        //\n"
                + "        // return null means the dataset has no more data to go through\n" + "        return null;\n"
                + "    }\n" + "\n" + "    @PreDestroy\n" + "    public void release() {\n"
                + "        // this is the symmetric method of the init() one,\n"
                + "        // release potential connections you created or data you cached\n" + "    }\n" + "}",
                files.get("src/main/java/com/foo/source/MycompSource.java"));
        assertEquals("package com.foo.source;\n" + "\n" + "import static java.util.Collections.singletonList;\n" + "\n"
                + "import org.talend.sdk.component.api.component.Icon;\n"
                + "import org.talend.sdk.component.api.component.Version;\n"
                + "import org.talend.sdk.component.api.configuration.Option;\n"
                + "import org.talend.sdk.component.api.input.Assessor;\n" + "import org.talend.sdk.component.api.input.Emitter;\n"
                + "import org.talend.sdk.component.api.input.PartitionSize;\n"
                + "import org.talend.sdk.component.api.input.PartitionMapper;\n"
                + "import org.talend.sdk.component.api.input.Split;\n" + "\n" + "import com.foo.service.TestService;\n" + "\n"
                + "//\n" + "// this class role is to enable the work to be distributed in environments supporting it.\n" + "//\n"
                + "@Version(1) // default version is 1, if some configuration changes happen between 2 versions you can add a migrationHandler\n"
                + "@Icon(Icon.IconType.STAR) // you can use a custom one using @Icon(value=CUSTOM, custom=\"filename\") and adding icons/filename_icon32.png in resources\n"
                + "@PartitionMapper(name = \"mycomp\")\n" + "public class MycompMapper implements Serializable {\n"
                + "    private final MycompSourceConfiguration configuration;\n" + "    private final TestService service;\n"
                + "\n" + "    public MycompMapper(@Option(\"configuration\") final MycompSourceConfiguration configuration,\n"
                + "                         final TestService service) {\n" + "        this.configuration = configuration;\n"
                + "        this.service = service;\n" + "    }\n" + "\n" + "    @Assessor\n"
                + "    public long estimateSize() throws SQLException {\n"
                + "        // this method should return the estimation of the dataset size\n"
                + "        // it is recommanded to return a byte value\n"
                + "        // if you don't have the exact size you can use a rough estimation\n" + "        return 1L;\n"
                + "    }\n" + "\n" + "    @Split\n"
                + "    public List<MycompMapper> split(@PartitionSize final long bundles) throws SQLException {\n"
                + "        // overall idea here is to split the work related to configuration in bundles of size \"bundles\"\n"
                + "        //\n" + "        // for instance if your estimateSize() returned 1000 and you can run on 10 nodes\n"
                + "        // then the environment can decide to run it concurrently (10 * 100).\n"
                + "        // In this case bundles = 100 and we must try to return 10 MycompMapper with 1/10 of the overall work each.\n"
                + "        //\n"
                + "        // default implementation returns this which means it doesn't support the work to be split\n"
                + "        return singletonList(this);\n" + "    }\n" + "\n" + "    @Emitter\n"
                + "    public MycompSource createWorker() {\n" + "        // here we create an actual worker,\n"
                + "        // you are free to rework the configuration etc but our default generated implementation\n"
                + "        // propagates the partition mapper entries.\n"
                + "        return new MycompSource(configuration, service);\n" + "    }\n" + "}",
                files.get("src/main/java/com/foo/source/MycompMapper.java"));
        assertEquals(
                "package com.foo.source;\n" + "\n" + "import java.util.List;\n" + "\n"
                        + "import org.talend.sdk.component.api.configuration.Option;\n"
                        + "import org.talend.sdk.component.api.configuration.ui.layout.GridLayout;\n" + "\n"
                        + "// generated configuration with query and addresses options, customize it to your need\n"
                        + "@GridLayout({\n" + "    // the generated layout put one configuration entry per line,\n"
                        + "    // customize it as much as needed\n" + "    @GridLayout.Row({ \"name\" })\n" + "})\n"
                        + "public class MycompSourceConfiguration {\n" + "    @Option\n" + "    private String name;\n" + "\n"
                        + "    public String getName() {\n" + "        return name;\n" + "    }\n" + "}",
                files.get("src/main/java/com/foo/source/MycompSourceConfiguration.java"));
        assertEquals("package com.foo.source;\n" + "\n" + "// this is the pojo which will be used to represent your data\n"
                + "public class MycompRecord {\n" + "\n" + "    private String name;\n" + "\n" + "    public String getName() {\n"
                + "        return name;\n" + "    }\n" + "\n" + "    public void setName(final String name) {\n"
                + "        this.name = name;\n" + "    }\n" + "    \n" + "}",
                files.get("src/main/java/com/foo/source/MycompRecord.java"));
        assertNotNull(files.get("src/main/resources/icons/superfamily_icon32.png"));
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

        assertEquals("package com.foo.source;\n" + "\n" + "import java.util.List;\n" + "\n"
                + "import org.talend.sdk.component.api.configuration.Option;\n"
                + "import org.talend.sdk.component.api.configuration.ui.layout.GridLayout;\n" + "\n"
                + "// generated configuration with query and addresses options, customize it to your need\n" + "@GridLayout({\n"
                + "    // the generated layout put one configuration entry per line,\n"
                + "    // customize it as much as needed\n" + "    @GridLayout.Row({ \"person\" })\n" + "})\n"
                + "public class MycompSourceConfiguration {\n" + "    @Option\n" + "    private PersonConfiguration person;\n"
                + "\n" + "    public PersonConfiguration getPerson() {\n" + "        return person;\n" + "    }\n" + "}",
                files.get("src/main/java/com/foo/source/MycompSourceConfiguration.java"));
        assertEquals(
                "package com.foo.source;\n" + "\n" + "import java.util.List;\n" + "\n"
                        + "import org.talend.sdk.component.api.configuration.Option;\n"
                        + "import org.talend.sdk.component.api.configuration.ui.layout.GridLayout;\n" + "\n"
                        + "// generated configuration with query and addresses options, customize it to your need\n"
                        + "@GridLayout({\n" + "    // the generated layout put one configuration entry per line,\n"
                        + "    // customize it as much as needed\n" + "    @GridLayout.Row({ \"name\" }),\n"
                        + "    @GridLayout.Row({ \"age\" })\n" + "})\n" + "public class PersonConfiguration {\n" + "    @Option\n"
                        + "    private String name;\n" + "\n" + "    @Option\n" + "    private int age;\n" + "\n"
                        + "    public String getName() {\n" + "        return name;\n" + "    }\n" + "\n"
                        + "    public int getAge() {\n" + "        return age;\n" + "    }\n" + "}",
                files.get("src/main/java/com/foo/source/PersonConfiguration.java"));
    }

    @Test
    public void genericSource() {
        final Map<String, String> files = generator
                .create("com.foo", build, "superfamily", "supercategory",
                        singleton(new ProjectRequest.SourceConfiguration("mycomp", "", false, null, null)), emptyList())
                .collect(toMap(FacetGenerator.InMemoryFile::getPath, i -> new String(i.getContent(), StandardCharsets.UTF_8)));

        assertEquals("package com.foo.source;\n" + "\n" + "import javax.annotation.PostConstruct;\n"
                + "import javax.annotation.PreDestroy;\n" + "\n" + "import org.talend.sdk.component.api.input.Producer;\n"
                + "import org.talend.sdk.component.api.processor.data.ObjectMap;\n" + "\n"
                + "import com.foo.service.TestService;\n" + "\n" + "public class MycompSource implements Serializable {\n"
                + "    private final MycompSourceConfiguration configuration;\n" + "    private final TestService service;\n"
                + "\n" + "    public MycompSource(@Option(\"configuration\") final MycompSourceConfiguration configuration,\n"
                + "                         final TestService service) {\n" + "        this.configuration = configuration;\n"
                + "        this.service = service;\n" + "    }\n" + "\n" + "    @PostConstruct\n" + "    public void init() {\n"
                + "        // this method will be executed once for the whole component execution,\n"
                + "        // this is where you can establish a connection for instance\n" + "    }\n" + "\n" + "    @Producer\n"
                + "    public ObjectMap next() {\n"
                + "        // this is the method allowing you to go through the dataset associated\n"
                + "        // to the component configuration\n" + "        //\n"
                + "        // return null means the dataset has no more data to go through\n" + "        return null;\n"
                + "    }\n" + "\n" + "    @PreDestroy\n" + "    public void release() {\n"
                + "        // this is the symmetric method of the init() one,\n"
                + "        // release potential connections you created or data you cached\n" + "    }\n" + "}",
                files.get("src/main/java/com/foo/source/MycompSource.java"));
    }

    @Test
    public void genericStreamMapper() {
        final Map<String, String> files = generator
                .create("com.foo", build, "superfamily", "supercategory",
                        singleton(new ProjectRequest.SourceConfiguration("mycomp", "", true, null, null)), emptyList())
                .collect(toMap(FacetGenerator.InMemoryFile::getPath, i -> new String(i.getContent(), StandardCharsets.UTF_8)));

        assertEquals("package com.foo.source;\n" + "\n" + "import static java.util.Collections.singletonList;\n" + "\n"
                + "import org.talend.sdk.component.api.component.Icon;\n"
                + "import org.talend.sdk.component.api.component.Version;\n"
                + "import org.talend.sdk.component.api.configuration.Option;\n"
                + "import org.talend.sdk.component.api.input.Assessor;\n" + "import org.talend.sdk.component.api.input.Emitter;\n"
                + "import org.talend.sdk.component.api.input.PartitionSize;\n"
                + "import org.talend.sdk.component.api.input.PartitionMapper;\n"
                + "import org.talend.sdk.component.api.input.Split;\n" + "\n" + "import com.foo.service.TestService;\n" + "\n"
                + "//\n" + "// this class role is to enable the work to be distributed in environments supporting it.\n" + "//\n"
                + "@Version(1) // default version is 1, if some configuration changes happen between 2 versions you can add a migrationHandler\n"
                + "@Icon(Icon.IconType.STAR) // you can use a custom one using @Icon(value=CUSTOM, custom=\"filename\") and adding icons/filename_icon32.png in resources\n"
                + "@PartitionMapper(name = \"mycomp\", infinite = true)\n"
                + "public class MycompMapper implements Serializable {\n"
                + "    private final MycompSourceConfiguration configuration;\n" + "    private final TestService service;\n"
                + "\n" + "    public MycompMapper(@Option(\"configuration\") final MycompSourceConfiguration configuration,\n"
                + "                         final TestService service) {\n" + "        this.configuration = configuration;\n"
                + "        this.service = service;\n" + "    }\n" + "\n" + "    @Assessor\n"
                + "    public long estimateSize() throws SQLException {\n"
                + "        // this method should return the estimation of the dataset size\n"
                + "        // it is recommanded to return a byte value\n"
                + "        // if you don't have the exact size you can use a rough estimation\n" + "        return 1L;\n"
                + "    }\n" + "\n" + "    @Split\n"
                + "    public List<MycompMapper> split(@PartitionSize final long bundles) throws SQLException {\n"
                + "        // overall idea here is to split the work related to configuration in bundles of size \"bundles\"\n"
                + "        //\n" + "        // for instance if your estimateSize() returned 1000 and you can run on 10 nodes\n"
                + "        // then the environment can decide to run it concurrently (10 * 100).\n"
                + "        // In this case bundles = 100 and we must try to return 10 MycompMapper with 1/10 of the overall work each.\n"
                + "        //\n"
                + "        // default implementation returns this which means it doesn't support the work to be split\n"
                + "        return singletonList(this);\n" + "    }\n" + "\n" + "    @Emitter\n"
                + "    public MycompSource createWorker() {\n" + "        // here we create an actual worker,\n"
                + "        // you are free to rework the configuration etc but our default generated implementation\n"
                + "        // propagates the partition mapper entries.\n"
                + "        return new MycompSource(configuration, service);\n" + "    }\n" + "}",
                files.get("src/main/java/com/foo/source/MycompMapper.java"));
    }

    @Test
    public void isolatedProcessor() {
        final Map<String, String> files = generator
                .create("com.foo", build, "superfamily", "supercategory", emptyList(),
                        singletonList(new ProjectRequest.ProcessorConfiguration("tProc", "", null, null, null)))
                .collect(toMap(FacetGenerator.InMemoryFile::getPath, i -> new String(i.getContent(), StandardCharsets.UTF_8)));

        assertEquals("package com.foo.output;\n" + "\n" + "import javax.annotation.PostConstruct;\n"
                + "import javax.annotation.PreDestroy;\n" + "\n" + "import org.talend.sdk.component.api.component.Icon;\n"
                + "import org.talend.sdk.component.api.component.Version;\n"
                + "import org.talend.sdk.component.api.configuration.Option;\n"
                + "import org.talend.sdk.component.api.processor.AfterGroup;\n"
                + "import org.talend.sdk.component.api.processor.BeforeGroup;\n"
                + "import org.talend.sdk.component.api.processor.Processor;\n" + "\n" + "import com.foo.service.TestService;\n"
                + "\n"
                + "@Version(1) // default version is 1, if some configuration changes happen between 2 versions you can add a migrationHandler\n"
                + "@Icon(Icon.IconType.STAR) // you can use a custom one using @Icon(value=CUSTOM, custom=\"filename\") and adding icons/filename_icon32.png in resources\n"
                + "@Processor(name = \"tProc\")\n" + "public class TProcProcessor implements Serializable {\n"
                + "    private final TProcProcessorConfiguration configuration;\n" + "    private final TestService service;\n"
                + "\n" + "    public TProcProcessor(@Option(\"configuration\") final TProcProcessorConfiguration configuration,\n"
                + "                         final TestService service) {\n" + "        this.configuration = configuration;\n"
                + "        this.service = service;\n" + "    }\n" + "\n" + "    @PostConstruct\n" + "    public void init() {\n"
                + "        // this method will be executed once for the whole component execution,\n"
                + "        // this is where you can establish a connection for instance\n"
                + "        // Note: if you don't need it you can delete it\n" + "    }\n" + "\n" + "    @BeforeGroup\n"
                + "    public void beforeGroup() {\n"
                + "        // if the environment supports chunking this method is called at the beginning if a chunk\n"
                + "        // it can be used to start a local transaction specific to the backend you use\n"
                + "        // Note: if you don't need it you can delete it\n" + "    }\n" + "\n" + "    @Producer\n"
                + "    public void onNext() {\n"
                + "        // this is the method allowing you to handle the input(s) and emit the output(s)\n"
                + "        // after some custom logic you put here, to send a value to next element you can use an\n"
                + "        // output parameter and call emit(value).\n" + "    }\n" + "\n" + "    @AfterGroup\n"
                + "    public void afterGroup() {\n"
                + "        // symmetric method of the beforeGroup() executed after the chunk processing\n"
                + "        // Note: if you don't need it you can delete it\n" + "    }\n" + "\n" + "    @PreDestroy\n"
                + "    public void release() {\n" + "        // this is the symmetric method of the init() one,\n"
                + "        // release potential connections you created or data you cached\n"
                + "        // Note: if you don't need it you can delete it\n" + "    }\n" + "}",
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
                .collect(toMap(FacetGenerator.InMemoryFile::getPath, i -> new String(i.getContent(), StandardCharsets.UTF_8)));

        assertEquals("package com.foo.processor;\n" + "\n" + "import javax.annotation.PostConstruct;\n"
                + "import javax.annotation.PreDestroy;\n" + "\n" + "import org.talend.sdk.component.api.component.Icon;\n"
                + "import org.talend.sdk.component.api.component.Version;\n"
                + "import org.talend.sdk.component.api.configuration.Option;\n"
                + "import org.talend.sdk.component.api.processor.AfterGroup;\n"
                + "import org.talend.sdk.component.api.processor.BeforeGroup;\n"
                + "import org.talend.sdk.component.api.processor.Processor;\n" + "\n" + "import com.foo.service.TestService;\n"
                + "\n"
                + "@Version(1) // default version is 1, if some configuration changes happen between 2 versions you can add a migrationHandler\n"
                + "@Icon(Icon.IconType.STAR) // you can use a custom one using @Icon(value=CUSTOM, custom=\"filename\") and adding icons/filename_icon32.png in resources\n"
                + "@Processor(name = \"tProc\")\n" + "public class TProcProcessor implements Serializable {\n"
                + "    private final TProcProcessorConfiguration configuration;\n" + "    private final TestService service;\n"
                + "\n" + "    public TProcProcessor(@Option(\"configuration\") final TProcProcessorConfiguration configuration,\n"
                + "                         final TestService service) {\n" + "        this.configuration = configuration;\n"
                + "        this.service = service;\n" + "    }\n" + "\n" + "    @PostConstruct\n" + "    public void init() {\n"
                + "        // this method will be executed once for the whole component execution,\n"
                + "        // this is where you can establish a connection for instance\n"
                + "        // Note: if you don't need it you can delete it\n" + "    }\n" + "\n" + "    @BeforeGroup\n"
                + "    public void beforeGroup() {\n"
                + "        // if the environment supports chunking this method is called at the beginning if a chunk\n"
                + "        // it can be used to start a local transaction specific to the backend you use\n"
                + "        // Note: if you don't need it you can delete it\n" + "    }\n" + "\n" + "    @Producer\n"
                + "    public void onNext(\n"
                + "        @Output(\"__default__\") final OutputEmitter<TProcDefaultOutput> defaultOutput) {\n"
                + "        // this is the method allowing you to handle the input(s) and emit the output(s)\n"
                + "        // after some custom logic you put here, to send a value to next element you can use an\n"
                + "        // output parameter and call emit(value).\n" + "    }\n" + "\n" + "    @AfterGroup\n"
                + "    public void afterGroup() {\n"
                + "        // symmetric method of the beforeGroup() executed after the chunk processing\n"
                + "        // Note: if you don't need it you can delete it\n" + "    }\n" + "\n" + "    @PreDestroy\n"
                + "    public void release() {\n" + "        // this is the symmetric method of the init() one,\n"
                + "        // release potential connections you created or data you cached\n"
                + "        // Note: if you don't need it you can delete it\n" + "    }\n" + "}",
                files.get("src/main/java/com/foo/processor/TProcProcessor.java"));
        assertEquals("package com.foo.processor;\n" + "\n" + "// this is the pojo which will be used to represent your data\n"
                + "public class TProcDefaultOutput {\n" + "\n" + "    private String name;\n" + "\n"
                + "    public String getName() {\n" + "        return name;\n" + "    }\n" + "\n"
                + "    public void setName(final String name) {\n" + "        this.name = name;\n" + "    }\n" + "    \n" + "}",
                files.get("src/main/java/com/foo/processor/TProcDefaultOutput.java"));
    }

    @Test
    public void processorGenericOutput() {
        final Map<String, String> files = generator
                .create("com.foo", build, "superfamily", "supercategory", emptyList(),
                        singletonList(new ProjectRequest.ProcessorConfiguration("tProc", "", null, null,
                                singletonMap("__default__", new ProjectRequest.StructureConfiguration(null, true)))))
                .collect(toMap(FacetGenerator.InMemoryFile::getPath, i -> new String(i.getContent(), StandardCharsets.UTF_8)));

        assertEquals("package com.foo.processor;\n" + "\n" + "import javax.annotation.PostConstruct;\n"
                + "import javax.annotation.PreDestroy;\n" + "\n" + "import org.talend.sdk.component.api.component.Icon;\n"
                + "import org.talend.sdk.component.api.component.Version;\n"
                + "import org.talend.sdk.component.api.configuration.Option;\n"
                + "import org.talend.sdk.component.api.processor.AfterGroup;\n"
                + "import org.talend.sdk.component.api.processor.BeforeGroup;\n"
                + "import org.talend.sdk.component.api.processor.Processor;\n"
                + "import org.talend.sdk.component.api.processor.data.ObjectMap;\n" + "\n"
                + "import com.foo.service.TestService;\n" + "\n"
                + "@Version(1) // default version is 1, if some configuration changes happen between 2 versions you can add a migrationHandler\n"
                + "@Icon(Icon.IconType.STAR) // you can use a custom one using @Icon(value=CUSTOM, custom=\"filename\") and adding icons/filename_icon32.png in resources\n"
                + "@Processor(name = \"tProc\")\n" + "public class TProcProcessor implements Serializable {\n"
                + "    private final TProcProcessorConfiguration configuration;\n" + "    private final TestService service;\n"
                + "\n" + "    public TProcProcessor(@Option(\"configuration\") final TProcProcessorConfiguration configuration,\n"
                + "                         final TestService service) {\n" + "        this.configuration = configuration;\n"
                + "        this.service = service;\n" + "    }\n" + "\n" + "    @PostConstruct\n" + "    public void init() {\n"
                + "        // this method will be executed once for the whole component execution,\n"
                + "        // this is where you can establish a connection for instance\n"
                + "        // Note: if you don't need it you can delete it\n" + "    }\n" + "\n" + "    @BeforeGroup\n"
                + "    public void beforeGroup() {\n"
                + "        // if the environment supports chunking this method is called at the beginning if a chunk\n"
                + "        // it can be used to start a local transaction specific to the backend you use\n"
                + "        // Note: if you don't need it you can delete it\n" + "    }\n" + "\n" + "    @Producer\n"
                + "    public void onNext(\n"
                + "        @Output(\"__default__\") final OutputEmitter<ObjectMap> defaultOutput) {\n"
                + "        // this is the method allowing you to handle the input(s) and emit the output(s)\n"
                + "        // after some custom logic you put here, to send a value to next element you can use an\n"
                + "        // output parameter and call emit(value).\n" + "    }\n" + "\n" + "    @AfterGroup\n"
                + "    public void afterGroup() {\n"
                + "        // symmetric method of the beforeGroup() executed after the chunk processing\n"
                + "        // Note: if you don't need it you can delete it\n" + "    }\n" + "\n" + "    @PreDestroy\n"
                + "    public void release() {\n" + "        // this is the symmetric method of the init() one,\n"
                + "        // release potential connections you created or data you cached\n"
                + "        // Note: if you don't need it you can delete it\n" + "    }\n" + "}",
                files.get("src/main/java/com/foo/processor/TProcProcessor.java"));
        assertNull(files.get("src/main/java/com/foo/processor/TProcDefaultOutput.java"));
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

        assertEquals("package com.foo.output;\n" + "\n" + "import javax.annotation.PostConstruct;\n"
                + "import javax.annotation.PreDestroy;\n" + "\n" + "import org.talend.sdk.component.api.component.Icon;\n"
                + "import org.talend.sdk.component.api.component.Version;\n"
                + "import org.talend.sdk.component.api.configuration.Option;\n"
                + "import org.talend.sdk.component.api.processor.AfterGroup;\n"
                + "import org.talend.sdk.component.api.processor.BeforeGroup;\n"
                + "import org.talend.sdk.component.api.processor.Processor;\n" + "\n" + "import com.foo.service.TestService;\n"
                + "\n"
                + "@Version(1) // default version is 1, if some configuration changes happen between 2 versions you can add a migrationHandler\n"
                + "@Icon(Icon.IconType.STAR) // you can use a custom one using @Icon(value=CUSTOM, custom=\"filename\") and adding icons/filename_icon32.png in resources\n"
                + "@Processor(name = \"tProc\")\n" + "public class TProcProcessor implements Serializable {\n"
                + "    private final TProcProcessorConfiguration configuration;\n" + "    private final TestService service;\n"
                + "\n" + "    public TProcProcessor(@Option(\"configuration\") final TProcProcessorConfiguration configuration,\n"
                + "                         final TestService service) {\n" + "        this.configuration = configuration;\n"
                + "        this.service = service;\n" + "    }\n" + "\n" + "    @PostConstruct\n" + "    public void init() {\n"
                + "        // this method will be executed once for the whole component execution,\n"
                + "        // this is where you can establish a connection for instance\n"
                + "        // Note: if you don't need it you can delete it\n" + "    }\n" + "\n" + "    @BeforeGroup\n"
                + "    public void beforeGroup() {\n"
                + "        // if the environment supports chunking this method is called at the beginning if a chunk\n"
                + "        // it can be used to start a local transaction specific to the backend you use\n"
                + "        // Note: if you don't need it you can delete it\n" + "    }\n" + "\n" + "    @Producer\n"
                + "    public void onNext(\n" + "        @Input(\"__default__\") final TProcDefaultInput defaultInput) {\n"
                + "        // this is the method allowing you to handle the input(s) and emit the output(s)\n"
                + "        // after some custom logic you put here, to send a value to next element you can use an\n"
                + "        // output parameter and call emit(value).\n" + "    }\n" + "\n" + "    @AfterGroup\n"
                + "    public void afterGroup() {\n"
                + "        // symmetric method of the beforeGroup() executed after the chunk processing\n"
                + "        // Note: if you don't need it you can delete it\n" + "    }\n" + "\n" + "    @PreDestroy\n"
                + "    public void release() {\n" + "        // this is the symmetric method of the init() one,\n"
                + "        // release potential connections you created or data you cached\n"
                + "        // Note: if you don't need it you can delete it\n" + "    }\n" + "}",
                files.get("src/main/java/com/foo/output/TProcProcessor.java"));
        assertEquals("package com.foo.output;\n" + "\n" + "// this is the pojo which will be used to represent your data\n"
                + "public class TProcDefaultInput {\n" + "\n" + "    private String name;\n" + "\n"
                + "    public String getName() {\n" + "        return name;\n" + "    }\n" + "\n"
                + "    public void setName(final String name) {\n" + "        this.name = name;\n" + "    }\n" + "    \n" + "}",
                files.get("src/main/java/com/foo/output/TProcDefaultInput.java"));
    }

    @Test
    public void processorGenericInput() {
        final Map<String, String> files = generator
                .create("com.foo", build, "superfamily", "supercategory", emptyList(),
                        singletonList(new ProjectRequest.ProcessorConfiguration("tProc", "", null,
                                singletonMap("__default__", new ProjectRequest.StructureConfiguration(null, true)), null)))
                .collect(toMap(FacetGenerator.InMemoryFile::getPath, i -> new String(i.getContent(), StandardCharsets.UTF_8)));

        assertEquals("package com.foo.output;\n" + "\n" + "import javax.annotation.PostConstruct;\n"
                + "import javax.annotation.PreDestroy;\n" + "\n" + "import org.talend.sdk.component.api.component.Icon;\n"
                + "import org.talend.sdk.component.api.component.Version;\n"
                + "import org.talend.sdk.component.api.configuration.Option;\n"
                + "import org.talend.sdk.component.api.processor.AfterGroup;\n"
                + "import org.talend.sdk.component.api.processor.BeforeGroup;\n"
                + "import org.talend.sdk.component.api.processor.Processor;\n"
                + "import org.talend.sdk.component.api.processor.data.ObjectMap;\n" + "\n"
                + "import com.foo.service.TestService;\n" + "\n"
                + "@Version(1) // default version is 1, if some configuration changes happen between 2 versions you can add a migrationHandler\n"
                + "@Icon(Icon.IconType.STAR) // you can use a custom one using @Icon(value=CUSTOM, custom=\"filename\") and adding icons/filename_icon32.png in resources\n"
                + "@Processor(name = \"tProc\")\n" + "public class TProcProcessor implements Serializable {\n"
                + "    private final TProcProcessorConfiguration configuration;\n" + "    private final TestService service;\n"
                + "\n" + "    public TProcProcessor(@Option(\"configuration\") final TProcProcessorConfiguration configuration,\n"
                + "                         final TestService service) {\n" + "        this.configuration = configuration;\n"
                + "        this.service = service;\n" + "    }\n" + "\n" + "    @PostConstruct\n" + "    public void init() {\n"
                + "        // this method will be executed once for the whole component execution,\n"
                + "        // this is where you can establish a connection for instance\n"
                + "        // Note: if you don't need it you can delete it\n" + "    }\n" + "\n" + "    @BeforeGroup\n"
                + "    public void beforeGroup() {\n"
                + "        // if the environment supports chunking this method is called at the beginning if a chunk\n"
                + "        // it can be used to start a local transaction specific to the backend you use\n"
                + "        // Note: if you don't need it you can delete it\n" + "    }\n" + "\n" + "    @Producer\n"
                + "    public void onNext(\n" + "        @Input(\"__default__\") final ObjectMap defaultInput) {\n"
                + "        // this is the method allowing you to handle the input(s) and emit the output(s)\n"
                + "        // after some custom logic you put here, to send a value to next element you can use an\n"
                + "        // output parameter and call emit(value).\n" + "    }\n" + "\n" + "    @AfterGroup\n"
                + "    public void afterGroup() {\n"
                + "        // symmetric method of the beforeGroup() executed after the chunk processing\n"
                + "        // Note: if you don't need it you can delete it\n" + "    }\n" + "\n" + "    @PreDestroy\n"
                + "    public void release() {\n" + "        // this is the symmetric method of the init() one,\n"
                + "        // release potential connections you created or data you cached\n"
                + "        // Note: if you don't need it you can delete it\n" + "    }\n" + "}",
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
                .collect(toMap(FacetGenerator.InMemoryFile::getPath, i -> new String(i.getContent(), StandardCharsets.UTF_8)));

        assertEquals("package com.foo.processor;\n" + "\n" + "import javax.annotation.PostConstruct;\n"
                + "import javax.annotation.PreDestroy;\n" + "\n" + "import org.talend.sdk.component.api.component.Icon;\n"
                + "import org.talend.sdk.component.api.component.Version;\n"
                + "import org.talend.sdk.component.api.configuration.Option;\n"
                + "import org.talend.sdk.component.api.processor.AfterGroup;\n"
                + "import org.talend.sdk.component.api.processor.BeforeGroup;\n"
                + "import org.talend.sdk.component.api.processor.Processor;\n"
                + "import org.talend.sdk.component.api.processor.data.ObjectMap;\n" + "\n"
                + "import com.foo.service.TestService;\n" + "\n"
                + "@Version(1) // default version is 1, if some configuration changes happen between 2 versions you can add a migrationHandler\n"
                + "@Icon(Icon.IconType.STAR) // you can use a custom one using @Icon(value=CUSTOM, custom=\"filename\") and adding icons/filename_icon32.png in resources\n"
                + "@Processor(name = \"tProc\")\n" + "public class TProcProcessor implements Serializable {\n"
                + "    private final TProcProcessorConfiguration configuration;\n" + "    private final TestService service;\n"
                + "\n" + "    public TProcProcessor(@Option(\"configuration\") final TProcProcessorConfiguration configuration,\n"
                + "                         final TestService service) {\n" + "        this.configuration = configuration;\n"
                + "        this.service = service;\n" + "    }\n" + "\n" + "    @PostConstruct\n" + "    public void init() {\n"
                + "        // this method will be executed once for the whole component execution,\n"
                + "        // this is where you can establish a connection for instance\n"
                + "        // Note: if you don't need it you can delete it\n" + "    }\n" + "\n" + "    @BeforeGroup\n"
                + "    public void beforeGroup() {\n"
                + "        // if the environment supports chunking this method is called at the beginning if a chunk\n"
                + "        // it can be used to start a local transaction specific to the backend you use\n"
                + "        // Note: if you don't need it you can delete it\n" + "    }\n" + "\n" + "    @Producer\n"
                + "    public void onNext(\n" + "        @Input(\"__default__\") final ObjectMap defaultInput\n"
                + "        @Output(\"reject\") final OutputEmitter<ObjectMap> rejectOutput,\n"
                + "        @Output(\"__default__\") final OutputEmitter<TProcDefaultOutput> defaultOutput) {\n"
                + "        // this is the method allowing you to handle the input(s) and emit the output(s)\n"
                + "        // after some custom logic you put here, to send a value to next element you can use an\n"
                + "        // output parameter and call emit(value).\n" + "    }\n" + "\n" + "    @AfterGroup\n"
                + "    public void afterGroup() {\n"
                + "        // symmetric method of the beforeGroup() executed after the chunk processing\n"
                + "        // Note: if you don't need it you can delete it\n" + "    }\n" + "\n" + "    @PreDestroy\n"
                + "    public void release() {\n" + "        // this is the symmetric method of the init() one,\n"
                + "        // release potential connections you created or data you cached\n"
                + "        // Note: if you don't need it you can delete it\n" + "    }\n" + "}",
                files.get("src/main/java/com/foo/processor/TProcProcessor.java"));
        assertEquals(
                "package com.foo.processor;\n" + "\n" + "// this is the pojo which will be used to represent your data\n"
                        + "public class TProcDefaultOutput {\n" + "\n" + "    private int age;\n" + "\n"
                        + "    public int getAge() {\n" + "        return age;\n" + "    }\n" + "\n"
                        + "    public void setAge(final int age) {\n" + "        this.age = age;\n" + "    }\n" + "    \n" + "}",
                files.get("src/main/java/com/foo/processor/TProcDefaultOutput.java"));
    }
}
