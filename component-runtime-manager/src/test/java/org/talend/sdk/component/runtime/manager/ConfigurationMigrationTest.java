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
package org.talend.sdk.component.runtime.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.util.HashMap;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.junit.base.junit5.TemporaryFolder;
import org.talend.sdk.component.junit.base.junit5.WithTemporaryFolder;
import org.talend.sdk.component.runtime.manager.asm.PluginGenerator;
import org.talend.sdk.component.runtime.output.ProcessorImpl;

@WithTemporaryFolder
class ConfigurationMigrationTest {

    private final PluginGenerator pluginGenerator = new PluginGenerator();

    @Test
    void run(final TemporaryFolder temporaryFolder) throws Exception {
        final File pluginFolder = new File(temporaryFolder.getRoot(), "test-plugins_" + UUID.randomUUID().toString());
        pluginFolder.mkdirs();
        final File jar = pluginGenerator.createChainPlugin(temporaryFolder.getRoot(), "comps.jar");

        try (final ComponentManager manager = new ComponentManager(new File("target/test-dependencies"),
                "META-INF/test/dependencies", "org.talend.test:type=plugin,value=%s")) {
            manager.addPlugin(jar.getAbsolutePath());

            {
                final Object nested = ProcessorImpl.class
                        .cast(manager.findProcessor("chain", "configured1", 0, new HashMap<String, String>() {

                            {
                                put("config.__version", "0");
                            }
                        }).orElseThrow(IllegalStateException::new))
                        .getDelegate();

                final Object config = get(nested, "getConfig");
                assertNotNull(config);
                assertEquals("ok", get(config, "getName"));
            }
            {
                final Object nested = ProcessorImpl.class
                        .cast(manager.findProcessor("chain", "configured2", 0, new HashMap<String, String>() {

                            {
                                put("config.__version", "0");
                                put("value.__version", "0");
                            }
                        }).orElseThrow(IllegalStateException::new))
                        .getDelegate();
                assertEquals("set", get(nested, "getValue"));

                final Object config = get(nested, "getConfig");
                assertNotNull(config);
                assertEquals("ok", get(config, "getName"));
            }
        }
    }

    private Object get(final Object root, final String getter) throws Exception {
        return root.getClass().getMethod(getter).invoke(root);
    }
}
