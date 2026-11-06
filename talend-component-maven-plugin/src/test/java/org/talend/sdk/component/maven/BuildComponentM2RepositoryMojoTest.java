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
package org.talend.sdk.component.maven;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singleton;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BuildComponentM2RepositoryMojoTest {

    @Test
    void legacyPluginIdIsRegistered(@TempDir final Path dir) throws Exception {
        final Path legacy = dir.resolve("legacy.properties");
        Files.write(legacy, "cosmos-db=cosmosDB\nmissing=other\n".getBytes(UTF_8));
        final BuildComponentM2RepositoryMojo mojo = mojo(legacy);

        final Properties registry = mojo
                .getNewComponentRegistry(singleton(
                        new DefaultArtifact("org.talend.components", "cosmos-db", "component", "car", "1.0")));

        assertEquals(registry.getProperty("cosmos-db"), registry.getProperty("cosmosDB"));
        assertEquals(2, registry.size());
    }

    @Test
    void collidingLegacyIdIsNotOverwritten(@TempDir final Path dir) throws Exception {
        final Path legacy = dir.resolve("legacy.properties");
        Files.write(legacy, "cosmos-db=other\n".getBytes(UTF_8));
        final Set<Artifact> cars = new HashSet<>();
        cars.add(new DefaultArtifact("org.talend.components", "cosmos-db", "component", "car", "1.0"));
        cars.add(new DefaultArtifact("org.talend.components", "other", "component", "car", "1.0"));

        final Properties registry = mojo(legacy).getNewComponentRegistry(cars);

        assertNotEquals(registry.getProperty("cosmos-db"), registry.getProperty("other"));
        assertTrue(registry.getProperty("other").contains(":other:"));
        assertEquals(2, registry.size());
    }

    @Test
    void legacyFileIsCopiedNextToRegistry(@TempDir final Path dir) throws Exception {
        final Path legacy = dir.resolve("legacy.properties");
        Files.write(legacy, "cosmos-db=cosmosDB\n".getBytes(UTF_8));
        final BuildComponentM2RepositoryMojo mojo = mojo(legacy);
        set(mojo, "m2RootPath", dir);

        final Method copy = BuildComponentM2RepositoryMojo.class.getDeclaredMethod("copyLegacyPluginIds");
        copy.setAccessible(true);
        copy.invoke(mojo);

        assertEquals("cosmos-db=cosmosDB\n",
                new String(Files.readAllBytes(dir.resolve("legacy-plugin-ids.properties")), UTF_8));
    }

    private BuildComponentM2RepositoryMojo mojo(final Path legacy) throws Exception {
        final BuildComponentM2RepositoryMojo mojo = new BuildComponentM2RepositoryMojo();
        set(mojo, "legacyPluginIds", legacy.toFile());
        return mojo;
    }

    private void set(final Object target, final String name, final Object value) throws Exception {
        final Field field = BuildComponentM2RepositoryMojo.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
