// ============================================================================
//
// Copyright (C) 2006-2017 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.components.starter.server.front;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
import org.talend.components.starter.server.model.FactoryConfiguration;
import org.talend.components.starter.server.model.ProjectModel;
import org.talend.components.starter.server.test.ClientRule;

@RunWith(MonoMeecrowave.Runner.class)
public class ProjectResourceTest {

    @Rule
    public final ClientRule client = new ClientRule();

    @Test
    public void configuration() {
        final FactoryConfiguration config = client.target().path("project/configuration").request(MediaType.APPLICATION_JSON_TYPE)
                .get(FactoryConfiguration.class);
        final String debug = config.toString();
        assertEquals(debug, new HashSet<>(asList("Gradle", "Maven")), new HashSet<>(config.getBuildTypes()));
        assertEquals(debug, new HashMap<String, List<FactoryConfiguration.Facet>>() {

            {
                put("Test", singletonList(
                        new FactoryConfiguration.Facet("Talend Component Kit Testing", "Generates test(s) for each component.")));
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
        Stream.of("component-api", "<source>1.8</source>", "<trimStackTrace>false</trimStackTrace>")
                .forEach(token -> assertTrue(token, files.get("application/pom.xml").contains(token)));
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
        Stream.of("component-api", "<source>1.8</source>", "<trimStackTrace>false</trimStackTrace>")
                .forEach(token -> assertTrue(token, files.get("application/pom.xml").contains(token)));
        assertEquals("= A Talend generated Component Starter Project\n" + "\n" + "== Test\n" + "\n"
                + "=== Talend Component Kit Testing\n" + "\n"
                + "Talend Component Kit Testing skeleton generator. For each component selected it generates an associated test suffixed with `Test`.\n"
                + "\n" + "\n", files.get("application/README.adoc"));
    }

    private Map<String, String> createZip(final ProjectModel projectModel) throws IOException {
        final Map<String, String> files = new HashMap<>();
        try (final ZipInputStream stream = new ZipInputStream(
                client.target().path("project/zip").request(MediaType.APPLICATION_JSON_TYPE).accept("application/zip")
                        .post(Entity.entity(projectModel, MediaType.APPLICATION_JSON_TYPE), InputStream.class))) {
            ZipEntry entry;
            while ((entry = stream.getNextEntry()) != null) {
                files.put(entry.getName(), new BufferedReader(new InputStreamReader(stream)).lines().collect(joining("\n")));
            }
        }
        return files;
    }
}
