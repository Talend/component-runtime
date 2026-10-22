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
package org.talend.sdk.component.container;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LegacyPluginIdsTest {

    @Test
    void mergesAllResourcesAndKeepsFirstOnConflict(@TempDir final Path dir) throws Exception {
        final Path first = write(dir, "first", "a-b=aB\nconflict=first\n");
        final Path second = write(dir, "second", "c-d=cD\nconflict=second\n");

        try (final URLClassLoader loader =
                new URLClassLoader(new URL[] { first.toUri().toURL(), second.toUri().toURL() }, null)) {
            final Map<String, String> values = ContainerManager.LegacyPluginIds.loadLegacyPluginIds(loader);
            assertEquals(3, values.size());
            assertEquals("aB", values.get("a-b"));
            assertEquals("cD", values.get("c-d"));
            assertEquals("first", values.get("conflict"));
        }
    }

    @Test
    void noResourceGivesEmptyMapping(@TempDir final Path dir) throws Exception {
        try (final URLClassLoader loader = new URLClassLoader(new URL[] { dir.toUri().toURL() }, null)) {
            assertTrue(ContainerManager.LegacyPluginIds.loadLegacyPluginIds(loader).isEmpty());
        }
    }

    @Test
    void readsFileFromM2Root(@TempDir final Path dir) throws Exception {
        Files.write(dir.resolve("legacy-plugin-ids.properties"), "cosmos-db=cosmosDB\n".getBytes(UTF_8));
        System.setProperty("talend.component.manager.m2.repository", dir.toString());
        try (final URLClassLoader loader = new URLClassLoader(new URL[0], null)) {
            assertEquals("cosmosDB", ContainerManager.LegacyPluginIds.loadLegacyPluginIds(loader).get("cosmos-db"));
        } finally {
            System.clearProperty("talend.component.manager.m2.repository");
        }
    }

    private Path write(final Path dir, final String name, final String content) throws Exception {
        final Path root = Files.createDirectories(dir.resolve(name).resolve("TALEND-INF"));
        Files.write(root.resolve("legacy-plugin-ids.properties"), content.getBytes(UTF_8));
        return dir.resolve(name);
    }
}
