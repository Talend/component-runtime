/**
 * Copyright (C) 2006-2025 Talend Inc. - www.talend.com
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

import static java.util.Optional.ofNullable;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.talend.sdk.component.runtime.manager.asm.PluginGenerator;
import org.talend.sdk.component.runtime.manager.serialization.DynamicContainerFinder;

class MaxBatchSizeParamTest {

    private final PluginGenerator pluginGenerator = new PluginGenerator();

    @Test
    void builtinParams(@TempDir final File temporaryFolder) throws Exception {
        final File pluginFolder = new File(temporaryFolder, "test-plugins_" + UUID.randomUUID().toString());
        pluginFolder.mkdirs();
        final File plugin = pluginGenerator.createChainPlugin(pluginFolder, "plugin.jar", 20);
        try (final ComponentManager manager =
                new ComponentManager(new File("target/test-dependencies"), "META-INF/test/dependencies", null)) {
            manager.addPlugin(plugin.getAbsolutePath());
            manager
                    .findPlugin("plugin")
                    .orElseGet(() -> (fail("can't find test plugin")))
                    .get(ContainerComponentRegistry.class)
                    .getComponents()
                    .entrySet()
                    .stream()
                    .filter(e -> "bulkProcessor".equals(e.getKey()))
                    .map(Map.Entry::getValue)
                    .flatMap(c -> c.getProcessors().values().stream())
                    .forEach(c -> {
                        final ParameterMeta parent = c
                                .getParameterMetas()
                                .get()
                                .stream()
                                .filter(m -> m.getName().equals(m.getPath()))
                                .findFirst()
                                .orElseGet(() -> fail("No parent config found"));

                        if ("BulkProcessorWithoutAfterGroup".equals(c.getName())) {
                            assertTrue(parent
                                    .getNestedParameters()
                                    .stream()
                                    .noneMatch(meta -> "$maxBatchSize".equals(meta.getName())));

                        } else {
                            final ParameterMeta maxBatchSize = parent
                                    .getNestedParameters()
                                    .stream()
                                    .filter(meta -> "$maxBatchSize".equals(meta.getName()))
                                    .findFirst()
                                    .orElseGet(() -> fail("Missing max batch size param"));

                            assertEquals("20", maxBatchSize.getMetadata().get("tcomp::ui::defaultvalue::value"));
                            assertEquals("1", maxBatchSize.getMetadata().get("tcomp::validation::min"));
                            assertEquals(ParameterMeta.Type.NUMBER, maxBatchSize.getType());
                            assertEquals(Integer.class, maxBatchSize.getJavaType());
                            switch (c.getName()) {
                                case "BulkProcessorWithNoConfig":
                                    assertEquals("$configuration", parent.getPath());
                                    assertEquals("$configuration.$maxBatchSize", maxBatchSize.getPath());
                                    assertEquals("$maxBatchSize",
                                            parent.getMetadata().get("tcomp::ui::gridlayout::Advanced::value"));
                                    break;
                                case "BulkProcessorWithAutoLayoutConfig":
                                    assertEquals(parent.getPath() + "." + maxBatchSize.getName(),
                                            maxBatchSize.getPath());
                                    assertEquals("true", parent.getMetadata().get("tcomp::ui::autolayout"));
                                    break;
                                case "BulkProcessorWithHorizontalLayoutConfig":
                                    assertEquals(parent.getPath() + "." + maxBatchSize.getName(),
                                            maxBatchSize.getPath());
                                    assertEquals("true", parent.getMetadata().get("tcomp::ui::horizontallayout"));
                                    break;
                                case "BulkProcessorWithVerticalLayoutConfig":
                                    assertEquals(parent.getPath() + "." + maxBatchSize.getName(),
                                            maxBatchSize.getPath());
                                    assertEquals("true", parent.getMetadata().get("tcomp::ui::verticallayout"));
                                    break;
                                case "BulkProcessorWithOptionOrderConfig":
                                    assertEquals(parent.getPath() + "." + maxBatchSize.getName(),
                                            maxBatchSize.getPath());
                                    assertEquals("true", parent.getMetadata().get("tcomp::ui::optionsorder"));
                                    break;
                                case "BulkProcessorWithGridLayoutConfig":
                                    assertEquals(parent.getPath() + "." + maxBatchSize.getName(),
                                            maxBatchSize.getPath());
                                    assertEquals("$maxBatchSize",
                                            parent.getMetadata().get("tcomp::ui::gridlayout::Advanced::value"));
                                    break;
                                case "BulkProcessorWithAdvancedGridLayoutConfig":
                                    assertEquals(parent.getPath() + "." + maxBatchSize.getName(),
                                            maxBatchSize.getPath());
                                    assertEquals("$maxBatchSize|config",
                                            parent.getMetadata().get("tcomp::ui::gridlayout::Advanced::value"));
                                    break;
                                case "BulkProcessorWithoutLayout":
                                    assertEquals(parent.getPath() + "." + maxBatchSize.getName(),
                                            maxBatchSize.getPath());
                                    assertEquals("config",
                                            parent.getMetadata().get("tcomp::ui::gridlayout::Main::value"));
                                    assertEquals("$maxBatchSize",
                                            parent.getMetadata().get("tcomp::ui::gridlayout::Advanced::value"));
                                    break;
                            }
                        }
                    });

        } finally { // clean temp files
            DynamicContainerFinder.LOADERS.clear();
            Stream.of(pluginFolder.listFiles()).forEach(File::delete);
            if (ofNullable(pluginFolder.listFiles()).map(f -> f.length == 0).orElse(true)) {
                pluginFolder.delete();
            }
        }
    }
}
