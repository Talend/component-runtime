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
package org.talend.sdk.component.starter.server.front;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;
import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.talend.sdk.component.starter.server.service.Resources.resourceFileToString;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;

import org.apache.meecrowave.junit5.MonoMeecrowaveConfig;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.starter.server.model.FactoryConfiguration;
import org.talend.sdk.component.starter.server.model.ProjectModel;
import org.talend.sdk.component.starter.server.service.info.ServerInfo;
import org.talend.sdk.component.starter.server.test.Client;

@MonoMeecrowaveConfig
@Client.Active
class ProjectResourceTest {

    @Inject
    private Jsonb jsonb;

    @Inject
    private ServerInfo versions;

    @Test
    void configuration(final WebTarget target) {
        final FactoryConfiguration config = target
                .path("project/configuration")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get(FactoryConfiguration.class);
        final String debug = config.toString();
        assertEquals(new HashSet<>(asList("Gradle", "Maven")), new HashSet<>(config.getBuildTypes()), debug);
        assertEquals(new HashMap<String, List<FactoryConfiguration.Facet>>() {

            {
                put("Test", singletonList(new FactoryConfiguration.Facet("Talend Component Kit Testing",
                        "Generates test(s) for each component.")));
                put("Runtime", singletonList(new FactoryConfiguration.Facet("Apache Beam",
                        "Generates some tests using beam runtime instead of Talend Component Kit Testing framework.")));
                put("Libraries", singletonList(new FactoryConfiguration.Facet("WADL Client Generation",
                        "Generates a HTTP client from a WADL.")));
                put("Tool",
                        asList(new FactoryConfiguration.Facet("Codenvy",
                                "Pre-configures the project to be usable with Codenvy."),
                                new FactoryConfiguration.Facet("Travis CI",
                                        "Creates a .travis.yml pre-configured for a component build.")));
            }
        }, new HashMap<>(config.getFacets()), debug);
    }

    @Test
    void emptyMavenProject(final WebTarget target) throws IOException {
        final ProjectModel projectModel = new ProjectModel();
        final Map<String, String> files = createZip(projectModel, target);

        assertEquals(Stream
                .of("application/.mvn/wrapper/", "application/mvnw.cmd", "application/", "application/mvnw",
                        "application/.mvn/", "application/.mvn/wrapper/maven-wrapper.jar",
                        "application/.mvn/wrapper/maven-wrapper.properties", "application/pom.xml",
                        "application/README.adoc")
                .collect(toSet()), files.keySet());
        Stream
                .of("component-api", "<source>1.8</source>", "<trimStackTrace>false</trimStackTrace>")
                .forEach(token -> assertTrue(files.get("application/pom.xml").contains(token), token));
        assertEquals("= A Talend generated Component Starter Project\n", files.get("application/README.adoc"));
        final ServerInfo.Snapshot snapshot = versions.getSnapshot();
        assertEquals(resourceFileToString("generated/ProjectResourceTest/emptyProject/pom.xml")
                .replace("@runtime.version@", snapshot.getKit())
                .replace("@surefire.version@", snapshot.getSurefire())
                .replace("@api.version@", snapshot.getApiKit()), files.get("application/pom.xml"));
    }

    @Test
    void formProject(final WebTarget target) throws IOException {
        final ProjectModel projectModel = new ProjectModel();
        projectModel.setBuildType("Gradle");
        final Map<String, String> files = createZip(projectModel, model -> target
                .path("project/zip/form")
                .queryParam("compressionType", "gzip")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .accept("application/zip")
                .post(Entity
                        .entity(new Form()
                                .param("project", Base64
                                        .getEncoder()
                                        .encodeToString(jsonb.toJson(projectModel).getBytes(StandardCharsets.UTF_8))),
                                APPLICATION_FORM_URLENCODED_TYPE),
                        InputStream.class));
        assertTrue(files.containsKey("application/build.gradle"));
    }

    @Test
    void emptyGradleProject(final WebTarget target) throws IOException {
        final ProjectModel projectModel = new ProjectModel();
        projectModel.setBuildType("Gradle");
        final Map<String, String> files = createZip(projectModel, target);

        assertEquals(Stream
                .of("application/gradle/", "application/", "application/gradle/wrapper/gradle-wrapper.jar",
                        "application/gradlew.bat", "application/gradlew",
                        "application/gradle/wrapper/gradle-wrapper.properties", "application/gradle/wrapper/",
                        "application/README.adoc", "application/build.gradle")
                .collect(toSet()), files.keySet());
    }

    @Test
    void testingProject(final WebTarget target) throws IOException {
        final ProjectModel projectModel = new ProjectModel();
        projectModel.setFacets(singletonList("Talend Component Kit Testing"));
        // todo: add source/proc
        final Map<String, String> files = createZip(projectModel, target);
        assertEquals(Stream
                .of("application/", "application/src/test/resources/", "application/src/", "application/mvnw.cmd",
                        "application/mvnw", "application/.mvn/", "application/.mvn/wrapper/maven-wrapper.properties",
                        "application/.mvn/wrapper/", "application/.mvn/wrapper/maven-wrapper.jar",
                        "application/src/test/", "application/src/test/resources/log4j2.xml", "application/pom.xml",
                        "application/README.adoc")
                .collect(toSet()), files.keySet());
        Stream
                .of("component-api", "<source>1.8</source>", "<trimStackTrace>false</trimStackTrace>")
                .forEach(token -> assertTrue(files.get("application/pom.xml").contains(token), token));
        assertEquals("= A Talend generated Component Starter Project\n" + "\n" + "== Test\n" + "\n"
                + "=== Talend Component Kit Testing\n" + "\n"
                + "Talend Component Kit Testing skeleton generator. For each component selected it generates an associated test suffixed with `Test`.\n"
                + "\n" + "\n", files.get("application/README.adoc"));
    }

    @Test
    void gradleTestingProject(final WebTarget target) throws IOException {
        final ProjectModel projectModel = new ProjectModel();
        projectModel.setBuildType("Gradle");
        projectModel.setFacets(singletonList("Talend Component Kit Testing"));
        final String buildFile = createZip(projectModel, target).get("application/build.gradle");
        assertTrue(buildFile
                .contains("group: 'org.talend.sdk.component', name: 'component-runtime-junit', version: '"
                        + versions.getSnapshot().getKit() + "'"),
                buildFile);
    }

    @Test
    void wadlFacetMaven(final WebTarget target) throws IOException {
        final ProjectModel projectModel = new ProjectModel();
        projectModel.setFacets(singletonList("WADL Client Generation"));
        final Map<String, String> files = createZip(projectModel, target);

        assertWadl(files);
        Stream
                .of("    <dependency>\n" + "      <groupId>org.apache.cxf</groupId>\n"
                        + "      <artifactId>cxf-rt-rs-client</artifactId>\n" + "      <version>"
                        + versions.getSnapshot().getCxf() + "</version>\n" + "      <scope>compile</scope>\n"
                        + "    </dependency>",
                        "      <plugin>\n" + "        <groupId>org.apache.cxf</groupId>\n"
                                + "        <artifactId>cxf-wadl2java-plugin</artifactId>\n" + "        <version>"
                                + versions.getSnapshot().getCxf() + "</version>\n" + "        <executions>\n"
                                + "          <execution>\n" + "            <id>generate-http-client-from-wadl</id>\n"
                                + "            <phase>generate-sources</phase>\n" + "            <goals>\n"
                                + "              <goal>wadl2java</goal>\n" + "            </goals>\n"
                                + "          </execution>\n" + "        </executions>\n" + "        <configuration>\n"
                                + "          <wadlOptions>\n" + "            <wadlOption>\n"
                                + "              <wadl>${project.basedir}/src/main/resources/wadl/client.xml</wadl>\n"
                                + "              <packagename>com.application.client.wadl</packagename>\n"
                                + "            </wadlOption>\n" + "          </wadlOptions>\n"
                                + "        </configuration>\n" + "      </plugin>")
                .forEach(string -> {
                    final String content = files.get("application/pom.xml");
                    assertTrue(content.contains(string), content);
                });
    }

    @Test
    void travisFacet(final WebTarget target) throws IOException {
        final ProjectModel projectModel = new ProjectModel();
        projectModel.setFacets(singletonList("Travis CI"));
        final Map<String, String> files = createZip(projectModel, target);

        assertTrue(files.get("application/README.adoc").contains("=== Travis CI\n"));
        assertTrue(files.get("application/.travis.yml").contains("language: java"));
    }

    @Test
    void wadlFacetGradle(final WebTarget target) throws IOException {
        final ProjectModel projectModel = new ProjectModel();
        projectModel.setBuildType("Gradle");
        projectModel.setPackageBase("com.foo");
        projectModel.setFacets(singletonList("WADL Client Generation"));
        final Map<String, String> files = createZip(projectModel, target);

        assertWadl(files);
        assertEquals("apply plugin: 'nebula.provided-base'\n" + "apply plugin: 'org.talend.sdk.component'\n"
                + "apply plugin: 'java'\napply plugin: 'maven-publish'\n" + "\n" + "tasks.withType(JavaCompile) {\n"
                + "    sourceCompatibility = 1.8\n" + "    targetCompatibility = 1.8\n"
                + "    options.compilerArgs << '-parameters'\n" + "    options.fork = true\n" + "}\n" + "\n"
                + "import org.apache.cxf.tools.common.ToolContext\n" + "import org.apache.cxf.tools.wadlto.WADLToJava\n"
                + "\n" + "buildscript {\n" + "  repositories {\n" + "    mavenLocal()\n" + "    mavenCentral()\n"
                + "    maven {\n" + "      url \"https://plugins.gradle.org/m2/\"\n" + "    }\n" + "  }\n"
                + "  dependencies {\n"
                + "    classpath \"com.netflix.nebula:gradle-extra-configurations-plugin:3.0.3\"\n"
                + "    classpath \"org.talend.sdk.component:gradle-talend-component:" + versions.getSnapshot().getKit()
                + "\"\n" + "    classpath \"org.apache.cxf:cxf-tools-wadlto-jaxrs:" + versions.getSnapshot().getCxf()
                + "\"\n" + "  }\n" + "}\n" + "\n" + "repositories {\n" + "  mavenLocal()\n" + "  mavenCentral()\n"
                + "}\n" + "\n" + "group = 'com.component'\n"
                + "description = 'An application generated by the Talend Component Kit Starter'\n"
                + "version = '0.0.1-SNAPSHOT'\n\n\n" + "jar {\n" + "  baseName = 'application'\n"
                + "  version = '0.0.1-SNAPSHOT'\n" + "}\n" + "\n" + "test {\n"
                + "  testLogging.showStandardStreams = true\n" + "}\n"
                + "def wadlGeneratedFolder = \"$buildDir/generated-sources/cxf\"\n" + "task generateWadlClient {\n"
                + "  def wadl = \"$projectDir/src/main/resources/wadl/client.xml\"\n" + "\n" + "  inputs.file(wadl)\n"
                + "  outputs.dir(wadlGeneratedFolder)\n" + "\n" + "  doLast {\n"
                + "    new File(wadlGeneratedFolder).mkdirs()\n" + "\n" + "    new WADLToJava([\n"
                + "      \"-d\", wadlGeneratedFolder,\n" + "      \"-p\", \"com.application.client.wadl\",\n"
                + "      wadl\n" + "    ] as String[]).run(new ToolContext())\n" + "  }\n" + "}\n" + "\n"
                + "sourceSets {\n" + "  main {\n" + "    java {\n"
                + "      project.tasks.compileJava.dependsOn project.tasks.generateWadlClient\n"
                + "      srcDir wadlGeneratedFolder\n" + "    }\n" + "  }\n" + "}\n" + "\n" + "repositories {\n"
                + "  mavenCentral()\n" + "}\n" + "\n" + "dependencies {\n"
                + "  provided group: 'org.talend.sdk.component', name: 'component-api', version: '"
                + versions.getSnapshot().getApiKit() + "'\n"
                + "  compile group: 'org.apache.cxf', name: 'cxf-rt-rs-client', version: '"
                + versions.getSnapshot().getCxf() + "'\n" + "}\n" + "\n" + "publishing {\n" + "  publications {\n"
                + "    mavenJava(MavenPublication) {\n" + "      from components.java\n" + "    }\n" + "  }\n"
                + "  repositories {\n" + "    mavenLocal()\n" + "  }\n" + "}\n", files.get("application/build.gradle"));
    }

    @Test
    void beamFacet(final WebTarget target) throws IOException {
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

        final Map<String, String> files = createZip(projectModel, target);
        assertTrue(files.get("application/README.adoc").contains("=== Apache Beam"), files.toString());
        assertEquals(resourceFileToString("generated/ProjectResourceTest/beamFacet/TInMapperBeamTest.java"),
                files.get("application/src/test/java/com/foo/source/TInMapperBeamTest.java"));
        assertEquals(resourceFileToString("generated/ProjectResourceTest/beamFacet/TInOutputBeamTest.java"),
                files.get("application/src/test/java/com/foo/output/TInOutputBeamTest.java"));
    }

    @Test
    void beamFacetProcessorOutput(final WebTarget target) throws IOException {
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

        final Map<String, String> files = createZip(projectModel, target);
        assertEquals(
                resourceFileToString(
                        "generated/ProjectResourceTest/beamFacetProcessorOutput/TInProcessorBeamTest.java"),
                files.get("application/src/test/java/com/foo/processor/TInProcessorBeamTest.java"));
    }

    @Test
    void codenvyFacet(final WebTarget target) throws IOException {
        final ProjectModel projectModel = new ProjectModel();
        projectModel.setPackageBase("com.foo");
        projectModel.setFacets(singletonList("Codenvy"));

        final Map<String, String> files = createZip(projectModel, target);

        assertEquals(resourceFileToString("generated/ProjectResourceTest/codenvy/README.adoc").trim(),
                files.get("application/README.adoc").trim());
        assertEquals(resourceFileToString("generated/ProjectResourceTest/codenvy/codenvy.json"),
                files.get("application/.codenvy.json"));
    }

    private void assertWadl(final Map<String, String> files) {
        assertTrue(files
                .get("application/src/main/resources/wadl/client.xml")
                .contains("<application xmlns=\"http://wadl.dev.java.net/2009/02\""));
        assertTrue(files
                .get("application/README.adoc")
                .contains(
                        "Generates the needed classes to call HTTP endpoints defined by a WADL located at `src/main/resources/wadl/client.xml`.\n"));
    }

    private Map<String, String> createZip(final ProjectModel projectModel, final WebTarget target) throws IOException {
        return createZip(projectModel,
                model -> target
                        .path("project/zip")
                        .request(MediaType.APPLICATION_JSON_TYPE)
                        .accept("application/zip")
                        .post(Entity.entity(model, MediaType.APPLICATION_JSON_TYPE), InputStream.class));
    }

    private Map<String, String> createZip(final ProjectModel projectModel,
            final Function<ProjectModel, InputStream> request) throws IOException {
        final Map<String, String> files = new HashMap<>();
        try (final ZipInputStream stream = new ZipInputStream(request.apply(projectModel))) {
            ZipEntry entry;
            while ((entry = stream.getNextEntry()) != null) {
                files
                        .put(entry.getName(),
                                new BufferedReader(new InputStreamReader(stream)).lines().collect(joining("\n")));
            }
        }
        return files;
    }
}
