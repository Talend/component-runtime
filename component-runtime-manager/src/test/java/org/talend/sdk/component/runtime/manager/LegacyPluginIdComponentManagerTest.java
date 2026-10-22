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
package org.talend.sdk.component.runtime.manager;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.talend.sdk.component.dependencies.maven.Artifact;
import org.talend.sdk.component.runtime.manager.ComponentManager.ContainerClasspathContributor;
import org.talend.sdk.component.runtime.manager.asm.PluginGenerator;

class LegacyPluginIdComponentManagerTest {

    @Test
    void legacyIdIsUsedForClasspathContributionsAndPluginLookup(@TempDir final Path temporaryFolder)
            throws Exception {
        final Path services = temporaryFolder.resolve("services/META-INF/services");
        Files.createDirectories(services);
        Files.write(services.resolve(ContainerClasspathContributor.class.getName()),
                RecordingContributor.class.getName().getBytes(UTF_8));
        final File pluginFolder = Files.createDirectory(temporaryFolder.resolve("plugins")).toFile();
        final File plugin = new PluginGenerator().createChainPlugin(pluginFolder, "legacy-test-plugin-1.0.0.jar");

        final Thread thread = Thread.currentThread();
        final ClassLoader old = thread.getContextClassLoader();
        RecordingContributor.REQUESTED.clear();
        try (final URLClassLoader tccl =
                new URLClassLoader(new URL[] { temporaryFolder.resolve("services").toUri().toURL() }, old)) {
            thread.setContextClassLoader(tccl);
            try (final ComponentManager manager =
                    new ComponentManager(new File("target/test-dependencies"), "META-INF/test/dependencies", null)) {
                final String id = manager.addPlugin(plugin.getAbsolutePath());

                assertEquals("legacyTestId", id);
                assertTrue(RecordingContributor.REQUESTED.contains("legacyTestId"),
                        RecordingContributor.REQUESTED::toString);
                assertTrue(manager.hasPlugin("legacyTestId"));
                assertTrue(manager.hasPlugin("legacy-test-plugin"));
            }
        } finally {
            thread.setContextClassLoader(old);
        }
    }

    public static class RecordingContributor implements ContainerClasspathContributor {

        private static final List<String> REQUESTED = new ArrayList<>();

        @Override
        public Collection<Artifact> findContributions(final String pluginId) {
            REQUESTED.add(pluginId);
            return emptyList();
        }

        @Override
        public boolean canResolve(final String path) {
            return false;
        }

        @Override
        public Path resolve(final String path) {
            throw new UnsupportedOperationException();
        }
    }
}
