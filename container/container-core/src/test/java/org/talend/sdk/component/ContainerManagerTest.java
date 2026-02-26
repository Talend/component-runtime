/**
 * Copyright (C) 2006-2026 Talend Inc. - www.talend.com
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
package org.talend.sdk.component;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.talend.sdk.component.container.Container;
import org.talend.sdk.component.container.ContainerListener;
import org.talend.sdk.component.container.ContainerManager;
import org.talend.sdk.component.dependencies.maven.MvnDependencyListLocalRepositoryResolver;
import org.talend.sdk.component.path.PathFactory;
import org.talend.sdk.component.test.Constants;
import org.talend.sdk.component.test.rule.TempJars;

@ExtendWith(TempJars.class)
class ContainerManagerTest {

    // note: in this method we could keep a ref of list or finds but
    // we abuse intentionally of the manager methods
    // to ensure they are reentrant
    @Test
    void manage(final TempJars jars) {
        final ContainerManager ref;
        final Collection<Container> containers;
        try (final ContainerManager manager = createDefaultManager()) {
            ref = manager;
            final Container container = manager.builder("foo", createZiplockJar(jars).getAbsolutePath()).create();
            assertNotNull(container);
            // container is not tested here but in ContainerTest. Here we just take care
            // of the manager.

            assertEquals(1, manager.findAll().size());
            assertEquals(singletonList(container), new ArrayList<>(manager.findAll()));

            assertTrue(manager.find("foo").isPresent());
            assertEquals(container, manager.find("foo").get());

            final Container xbean = manager.builder("bar", createZiplockJar(jars).getAbsolutePath()).create();
            assertEquals(2, manager.findAll().size());
            Stream.of(container, xbean).forEach(c -> assertTrue(manager.findAll().contains(c)));

            containers = manager.findAll();
        }

        // now manager is closed so all containers are cleaned up
        assertTrue(ref.isClosed());
        containers.forEach(c -> assertTrue(c.isClosed()));
    }

    @Test
    void autoContainerId(final TempJars jars) {
        Stream
                .of("ziplock-7.00.3.jar", "ziplock-7.3.jar", "ziplock-7.3-SNAPSHOT.jar", "ziplock-7.3.0-SNAPSHOT.jar")
                .forEach(jarName -> {
                    try (final ContainerManager manager = createDefaultManager()) {
                        final File module = createZiplockJar(jars);
                        final File jar = new File(module.getParentFile(), jarName);
                        assertTrue(module.renameTo(jar));
                        final Container container = manager.builder(jar.getAbsolutePath()).create();
                        assertEquals("ziplock"/* no version, no .jar */, container.getId());
                    }
                });
    }

    @Test
    void autoContainerIdWithJiraIssue(final TempJars jars) {
        final List<String> plugins = Arrays.asList("data-processing-runtime-streamsjob-2.21.0-PR-999-SNAPSHOT.jar",
                "ziplock-7.3-TCOMP-2285-SNAPSHOT.jar", "rest-1.38.0-TDI-46666-SNAPSHOT.jar",
                "rest-1.38.0-TD-46666.jar", "ziplock-7.00.3.jar", "ziplock-7.3.jar");
        final List<String> pluginIds =
                Arrays.asList("data-processing-runtime-streamsjob", "ziplock", "rest", "rest", "ziplock", "ziplock");
        final List<String> results = plugins.stream().map(jar -> {
            try (final ContainerManager manager = createDefaultManager()) {
                final File module = createZiplockJar(jars);
                final File j = new File(module.getParentFile(), jar);
                assertTrue(module.renameTo(j));
                final Container container = manager.builder(j.getAbsolutePath()).create();
                return container.getId();
            }
        }).collect(Collectors.toList());
        assertEquals(pluginIds, results);
    }

    @Test
    void listeners(final TempJars jars) {
        final Collection<String> states = new ArrayList<>();
        final ContainerListener listener = new ContainerListener() {

            @Override
            public void onCreate(final Container container) {
                states.add("deploy #" + container.getId());
            }

            @Override
            public void onClose(final Container container) {
                states.add("undeploy #" + container.getId());
            }
        };
        try (final ContainerManager manager = createDefaultManager().registerListener(listener)) {
            assertEquals(emptyList(), states);

            try (final Container container = manager.builder(createZiplockJar(jars).getAbsolutePath()).create()) {
                assertEquals(1, states.size());
            }
            assertEquals(2, states.size());

            manager.unregisterListener(listener);
            try (final Container container = manager.builder(createZiplockJar(jars).getAbsolutePath()).create()) {
                assertEquals(2, states.size());
            }
            assertEquals(2, states.size());
        }
        assertEquals(2, states.size());
    }

    @Test
    void close(final TempJars jars) {
        assertThrows(IllegalStateException.class, () -> {
            final ContainerManager manager = createDefaultManager();
            manager.close();
            manager.builder("foo", createZiplockJar(jars).getAbsolutePath()).create();
        });
    }

    @Test
    void getClasspathFromJarWithValidManifestClassPath(@TempDir final Path tempDir) throws Exception {
        final File jar1 = createJarFile(tempDir, "lib1.jar");
        final File jar2 = createJarFile(tempDir, "lib2.jar");
        final File mainJar = createJarWithManifest(tempDir, "main.jar",
                jar1.getAbsolutePath() + " " + jar2.getAbsolutePath());

        try (final ContainerManager containerManager = createDefaultManager()) {
            final List<String> result = containerManager.getClasspathFromJar(mainJar.toPath());

            assertEquals(2, result.size());
            assertTrue(result.contains(jar1.getAbsolutePath()));
            assertTrue(result.contains(jar2.getAbsolutePath()));
        }
    }

    @Test
    void getClasspathFromJarWithNoManifest(@TempDir final Path tempDir) throws Exception {
        final File jar = createJarFile(tempDir, "noManifest.jar");

        try (final ContainerManager containerManager = createDefaultManager()) {
            final List<String> result = containerManager.getClasspathFromJar(jar.toPath());

            assertTrue(result.isEmpty());
        }
    }

    @Test
    void getClasspathFromJarWithEmptyClassPath(@TempDir final Path tempDir) throws Exception {
        final File jar = createJarWithManifest(tempDir, "emptyCP.jar", "");

        try (final ContainerManager containerManager = createDefaultManager()) {
            final List<String> result = containerManager.getClasspathFromJar(jar.toPath());

            assertTrue(result.isEmpty());
        }
    }

    private static File createJarFile(final Path tempDir, final String fileName) throws IOException {
        final File jar = new File(tempDir.toFile(), fileName);
        try (final JarOutputStream jos = new JarOutputStream(new FileOutputStream(jar))) {
            jos.putNextEntry(new ZipEntry("dummy.txt"));
            jos.write("dummy content".getBytes(StandardCharsets.UTF_8));
            jos.closeEntry();
        }
        return jar;
    }

    private static File createJarWithManifest(final Path tempDir, final String fileName, final String classPath)
            throws IOException {
        final File jar = new File(tempDir.toFile(), fileName);
        final Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        if (classPath != null && !classPath.isEmpty()) {
            manifest.getMainAttributes().put(Attributes.Name.CLASS_PATH, classPath);
        }
        try (final JarOutputStream jos = new JarOutputStream(new FileOutputStream(jar), manifest)) {
            jos.putNextEntry(new ZipEntry("dummy.txt"));
            jos.write("dummy content".getBytes(StandardCharsets.UTF_8));
            jos.closeEntry();
        }
        return jar;
    }

    private File createZiplockJar(final TempJars jars) {
        return jars.create("org.apache.tomee:ziplock:jar:" +
                "8.0.14");
    }

    private ContainerManager createDefaultManager() {
        return new ContainerManager(ContainerManager.DependenciesResolutionConfiguration
                .builder()
                .resolver(new MvnDependencyListLocalRepositoryResolver(Constants.DEPENDENCIES_LIST_RESOURCE_PATH,
                        d -> null))
                .rootRepositoryLocation(PathFactory.get(Constants.DEPENDENCIES_LOCATION))
                .create(), ContainerManager.ClassLoaderConfiguration.builder().create(), null, Level.INFO);
    }
}
