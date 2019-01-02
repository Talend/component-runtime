/**
 * Copyright (C) 2006-2019 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.runtime.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.junit.base.junit5.TemporaryFolder;
import org.talend.sdk.component.junit.base.junit5.WithTemporaryFolder;
import org.talend.sdk.component.runtime.manager.asm.PluginGenerator;

@WithTemporaryFolder
class ModuleResolverTest {

    private final PluginGenerator pluginGenerator = new PluginGenerator();

    @Test
    void missingModule() {
        final File plugin = new File("missing/" + UUID.randomUUID().toString() + "-missing.jar");
        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            try (final ComponentManager manager =
                    new ComponentManager(new File("target/test-dependencies"), "META-INF/test/dependencies", null)) {
                manager.addPlugin(plugin.getAbsolutePath());
            }
        });

        assertEquals(
                "Module error: check that the module exist and is a jar or a directory. " + plugin.getAbsolutePath(),
                ex.getSuppressed()[0].getMessage());
    }

    @Test
    void unsupportedType(final TemporaryFolder temporaryFolder) throws IOException {
        final File pluginFolder = new File(temporaryFolder.getRoot(), "test-plugins_" + UUID.randomUUID().toString());
        pluginFolder.mkdirs();
        final File plugin = pluginGenerator.createPlugin(pluginFolder, "plugin1.xyz", "");
        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            try (final ComponentManager manager =
                    new ComponentManager(new File("target/test-dependencies"), "META-INF/test/dependencies", null)) {
                manager.addPlugin(plugin.getAbsolutePath());
            }
        });

        assertEquals("Unsupported module " + plugin.getAbsolutePath(), ex.getMessage());
    }

    @Test
    void moduleFileExist(final TemporaryFolder temporaryFolder) throws IOException {
        final File pluginFolder = new File(temporaryFolder.getRoot(), "test-plugins_" + UUID.randomUUID().toString());
        pluginFolder.mkdirs();
        final File plugin = pluginGenerator.createPlugin(pluginFolder, "plugin1.jar", "");

        try (final ComponentManager manager =
                new ComponentManager(new File("target/test-dependencies"), "META-INF/test/dependencies", null)) {
            manager.addPlugin(plugin.getAbsolutePath());

            assertTrue(manager.findPlugin(plugin.getName()).isPresent());
        }
    }

}
