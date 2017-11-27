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
package org.talend.sdk.component.starter.server.front;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.talend.sdk.component.starter.server.Versions.CXF;
import static org.talend.sdk.component.starter.server.test.Resources.resourceFileToString;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;

import org.apache.meecrowave.junit.MonoMeecrowave;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.talend.sdk.component.starter.server.model.FactoryConfiguration;
import org.talend.sdk.component.starter.server.model.ProjectModel;
import org.talend.sdk.component.starter.server.test.ClientRule;

@RunWith(MonoMeecrowave.Runner.class)
public class ProjectResourceTest {

    @Rule
    public final ClientRule client = new ClientRule();

    @Test
    public void configuration() {
        final FactoryConfiguration config =
                client.target().path("project/configuration").request(MediaType.APPLICATION_JSON_TYPE).get(
                        FactoryConfiguration.class);
        final String debug = config.toString();
        assertEquals(debug, new HashSet<>(asList("Gradle", "Maven")), new HashSet<>(config.getBuildTypes()));
        assertEquals(debug, new HashMap<String, List<FactoryConfiguration.Facet>>() {

            {
                put("Test", singletonList(new FactoryConfiguration.Facet("Talend Component Kit Testing",
                        "Generates test(s) for each component.")));
                put("Runtime", singletonList(new FactoryConfiguration.Facet("Apache Beam",
                        "Generates some tests using beam runtime instead of Talend Component Kit Testing framework.")));
                put("Libraries", singletonList(new FactoryConfiguration.Facet("WADL Client Generation",
                        "Generates a HTTP client from a WADL.")));
                put("Tool", singletonList(new FactoryConfiguration.Facet("Codenvy",
                        "Pre-configures the project to be usable with Codenvy.")));
            }
        }, new HashMap<>(config.getFacets()));
    }

    @Test
    public void emptyProject() throws IOException {
        final ProjectModel projectModel = new ProjectModel();
        final Map<String, String> files = createZip(projectModel);

        assertEquals(3, files.size());
        assertEquals(Stream.of("application/", "application/pom.xml", "application/README.adoc").collect(toSet()),
                files.keySet());
        Stream.of("component-api", "<source>1.8</source>", "<trimStackTrace>false</trimStackTrace>").forEach(
                token -> assertTrue(token, files.get("application/pom.xml").contains(token)));
        assertEquals("= A Talend generated Component Starter Project\n", files.get("application/README.adoc"));
    }

    @Test
    public void testingProject() throws IOException {
        final ProjectModel projectModel = new ProjectModel();
        projectModel.setFacets(singletonList("Talend Component Kit Testing"));
        // todo: add source/proc
        final Map<String, String> files = createZip(projectModel);

        assertEquals(3, files.size()); // TODO: should be more since we generate tests
        assertEquals(Stream.of("application/", "application/pom.xml", "application/README.adoc").collect(toSet()),
                files.keySet());
        Stream.of("component-api", "<source>1.8</source>", "<trimStackTrace>false</trimStackTrace>").forEach(
                token -> assertTrue(token, files.get("application/pom.xml").contains(token)));
        assertEquals("= A Talend generated Component Starter Project\n" + "\n" + "== Test\n" + "\n"
                + "=== Talend Component Kit Testing\n" + "\n"
                + "Talend Component Kit Testing skeleton generator. For each component selected it generates an associated test suffixed with `Test`.\n"
                + "\n" + "\n", files.get("application/README.adoc"));
    }

    @Test
    public void wadlFacetMaven() throws IOException {
        final ProjectModel projectModel = new ProjectModel();
        projectModel.setFacets(singletonList("WADL Client Generation"));
        final Map<String, String> files = createZip(projectModel);

        assertEquals(8, files.size());
        assertWadl(files);
        Stream
                .of("    <dependency>\n" + "      <groupId>org.apache.cxf</groupId>\n"
                        + "      <artifactId>cxf-rt-rs-client</artifactId>\n" + "      <version>3.2.1</version>\n"
                        + "      <scope>compile</scope>\n" + "    </dependency>",
                        "      <plugin>\n" + "        <groupId>org.apache.cxf</groupId>\n"
                                + "        <artifactId>cxf-wadl2java-plugin</artifactId>\n"
                                + "        <version>3.2.1</version>\n" + "        <executions>\n"
                                + "          <execution>\n" + "            <id>generate-http-client-from-wadl</id>\n"
                                + "            <phase>generate-sources</phase>\n" + "            <goals>\n"
                                + "              <goal>wadl2java</goal>\n" + "            </goals>\n"
                                + "          </execution>\n" + "        </executions>\n" + "        <configuration>\n"
                                + "          <wadlOptions>\n" + "            <wadlOption>\n"
                                + "              <wadl>${project.basedir}/src/main/resources/wadl/client.xml</wadl>\n"
                                + "              <packagename>com.application.client.wadl</packagename>\n"
                                + "            </wadlOption>\n" + "          </wadlOptions>\n"
                                + "        </configuration>\n" + "      </plugin>")
                .forEach(string -> assertThat(files.get("application/pom.xml"), containsString(string)));
    }

    @Test
    public void wadlFacetGradle() throws IOException {
        final ProjectModel projectModel = new ProjectModel();
        projectModel.setBuildType("Gradle");
        projectModel.setPackageBase("com.foo");
        projectModel.setFacets(singletonList("WADL Client Generation"));
        final Map<String, String> files = createZip(projectModel);

        assertEquals(8, files.size());
        assertWadl(files);
        Stream
                .of("    classpath \"org.apache.cxf:cxf-tools-wadlto-jaxrs:" + CXF,
                        "import org.apache.cxf.tools.common.ToolContext\n"
                                + "import org.apache.cxf.tools.wadlto.WADLToJava\n",
                        "def wadlGeneratedFolder = \"$buildDir/generated-sources/cxf\"\n",
                        "task generateWadlClient {\n"
                                + "  def wadl = \"$projectDir/src/main/resources/wadl/client.xml\"\n" + "\n"
                                + "  inputs.file(wadl)\n" + "  outputs.dir(wadlGeneratedFolder)\n" + "\n"
                                + "  doLast {\n" + "    new File(wadlGeneratedFolder).mkdirs()\n" + "\n"
                                + "    new WADLToJava([\n" + "      \"-d\", wadlGeneratedFolder,\n"
                                + "      \"-p\", \"com.application.client.wadl\",\n" + "      wadl\n"
                                + "    ] as String[]).run(new ToolContext())\n" + "  }\n" + "}",
                        "sourceSets {\n" + "  main {\n" + "    java {\n"
                                + "      project.tasks.compileJava.dependsOn project.tasks.generateWadlClient\n"
                                + "      srcDir wadlGeneratedFolder\n" + "    }\n" + "  }\n" + "}")
                .forEach(string -> assertThat(files.get("application/build.gradle"), containsString(string)));
    }

    @Test
    public void beamFacet() throws IOException {
        final ProjectModel projectModel = new ProjectModel();
        projectModel.setPackageBase("com.foo");
        projectModel.setFacets(singletonList("Apache Beam"));
        {
            final ProjectModel.Source source = new ProjectModel.Source();
            source.setName("tIn");
            source.setGenericOutput(true);
            projectModel.setSources(singletonList(source));
        }
        {
            final ProjectModel.Processor proc = new ProjectModel.Processor();
            proc.setName("tIn");
            {
                final ProjectModel.NamedModel in = new ProjectModel.NamedModel();
                in.setName("__default__");
                in.setGeneric(true);
                proc.setInputStructures(singletonList(in));
            }
            proc.setOutputStructures(emptyList());
            projectModel.setProcessors(singletonList(proc));
        }

        final Map<String, String> files = createZip(projectModel);

        assertEquals(41, files.size());
        assertTrue(files.toString(), files.get("application/README.adoc").contains("=== Apache Beam"));
        assertEquals(resourceFileToString("generated/ProjectResourceTest/beamFacet/TInMapperBeamTest.java"),
                files.get("application/src/test/java/com/foo/source/TInMapperBeamTest.java"));
        assertEquals(resourceFileToString("generated/ProjectResourceTest/beamFacet/TInOutputBeamTest.java"),
                files.get("application/src/test/java/com/foo/output/TInOutputBeamTest.java"));
    }

    @Test
    public void beamFacetProcessorOutput() throws IOException {
        final ProjectModel projectModel = new ProjectModel();
        projectModel.setPackageBase("com.foo");
        projectModel.setFacets(singletonList("Apache Beam"));
        {
            final ProjectModel.Processor proc = new ProjectModel.Processor();
            proc.setName("tIn");
            {
                final ProjectModel.NamedModel in = new ProjectModel.NamedModel();
                in.setName("__default__");
                in.setGeneric(true);
                proc.setInputStructures(singletonList(in));
            }
            {
                final ProjectModel.NamedModel out = new ProjectModel.NamedModel();
                out.setName("__default__");
                out.setGeneric(true);
                proc.setOutputStructures(singletonList(out));
            }
            projectModel.setProcessors(singletonList(proc));
        }

        final Map<String, String> files = createZip(projectModel);

        assertEquals(32, files.size());
        assertEquals(
                resourceFileToString(
                        "generated/ProjectResourceTest/beamFacetProcessorOutput/TInProcessorBeamTest.java"),
                files.get("application/src/test/java/com/foo/processor/TInProcessorBeamTest.java"));
    }

    @Test
    public void codenvyFacet() throws IOException {
        final ProjectModel projectModel = new ProjectModel();
        projectModel.setPackageBase("com.foo");
        projectModel.setFacets(singletonList("Codenvy"));

        final Map<String, String> files = createZip(projectModel);

        assertEquals(4, files.size());
        assertEquals(resourceFileToString("generated/ProjectResourceTest/codenvy/README.adoc").trim(),
                files.get("application/README.adoc").trim());
        assertEquals(resourceFileToString("generated/ProjectResourceTest/codenvy/factory.json"),
                files.get("application/.factory.json"));
    }

    private void assertWadl(final Map<String, String> files) {
        assertThat(files.get("application/src/main/resources/wadl/client.xml"),
                containsString("<application xmlns=\"http://wadl.dev.java.net/2009/02\""));
        assertThat(files.get("application/README.adoc"), containsString(
                "Generates the needed classes to call HTTP endpoints defined by a WADL located at `src/main/resources/wadl/client.xml`.\n"));
    }

    private Map<String, String> createZip(final ProjectModel projectModel) throws IOException {
        final Map<String, String> files = new HashMap<>();
        try (final ZipInputStream stream = new ZipInputStream(client
                .target()
                .path("project/zip")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .accept("application/zip")
                .post(Entity.entity(projectModel, MediaType.APPLICATION_JSON_TYPE), InputStream.class))) {
            ZipEntry entry;
            while ((entry = stream.getNextEntry()) != null) {
                files.put(entry.getName(),
                        new BufferedReader(new InputStreamReader(stream)).lines().collect(joining("\n")));
            }
        }
        return files;
    }
}
