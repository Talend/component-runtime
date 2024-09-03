/**
 * Copyright (C) 2006-2024 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.runtime.manager.service;

import static java.util.Collections.emptyMap;
import static java.util.Optional.ofNullable;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.util.MissingResourceException;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.talend.sdk.component.runtime.input.Mapper;
import org.talend.sdk.component.runtime.manager.ComponentManager;
import org.talend.sdk.component.runtime.manager.asm.PluginGenerator;

public class InternalizationServiceTest {

    private final PluginGenerator pluginGenerator = new PluginGenerator();

    @Test
    void wrongCL(@TempDir final File temporaryFolder) {
        final File pluginFolder = new File(temporaryFolder, "test-plugins_" + UUID.randomUUID().toString());
        pluginFolder.mkdirs();
        final File plugin = pluginGenerator.createChainPlugin(pluginFolder, "plugin.jar");
        final File deps = new File("target/test-dependencies");
        try (final ComponentManager manager = new ComponentManager(deps, "META-INF/test/dependencies", null)) {
            manager.addPlugin(plugin.getAbsolutePath());
            try {
                final Mapper mapper =
                        manager.findMapper("db", "input", 1, emptyMap()).orElseThrow(IllegalStateException::new);
            } catch (MissingResourceException e) {
                fail("Bundle should have been loaded.");
            }
        } finally { // clean temp files
            Stream.of(pluginFolder.listFiles()).forEach(File::delete);
            if (ofNullable(pluginFolder.listFiles()).map(f -> f.length == 0).orElse(true)) {
                pluginFolder.delete();
            }
        }

    }
}
