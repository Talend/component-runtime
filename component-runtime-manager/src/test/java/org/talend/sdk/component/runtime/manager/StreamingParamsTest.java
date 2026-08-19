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

import static java.util.Optional.ofNullable;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.talend.sdk.component.container.Container;
import org.talend.sdk.component.runtime.manager.ComponentFamilyMeta.PartitionMapperMeta;
import org.talend.sdk.component.runtime.manager.ParameterMeta.Type;
import org.talend.sdk.component.runtime.manager.asm.PluginGenerator;
import org.talend.sdk.component.runtime.manager.serialization.DynamicContainerFinder;

class StreamingParamsTest {

    private final PluginGenerator pluginGenerator = new PluginGenerator();

    @Test
    void builtinParams(@TempDir final File temporaryFolder) throws Exception {
        final File pluginFolder = new File(temporaryFolder, "test-plugins_" + UUID.randomUUID().toString());
        pluginFolder.mkdirs();
        final File plugin = pluginGenerator.createChainPlugin(pluginFolder, "plugin.jar", 20);
        try (final ComponentManager manager =
                new ComponentManager(new File("target/test-dependencies"), "META-INF/test/dependencies", null)) {
            manager.addPlugin(plugin.getAbsolutePath());
            Container container = manager.findPlugin("plugin").orElseGet(() -> (fail("can't find test plugin")));
            PartitionMapperMeta infinite = container
                    .get(ContainerComponentRegistry.class)
                    .getComponents()
                    .entrySet()
                    .stream()
                    .filter(e -> "streaming".equals(e.getKey()))
                    .map(java.util.Map.Entry::getValue)
                    .flatMap(c -> c.getPartitionMappers().values().stream())
                    .findFirst()
                    .orElse(null);
            assertNotNull(infinite);
            assertThrows(IllegalArgumentException.class, () -> infinite
                    .getParameterMetas()
                    .get()
                    .stream()
                    .filter(m -> m.getName().equals(m.getPath()))
                    .findFirst()
                    .orElseThrow(IllegalArgumentException::new));
            //
            PartitionMapperMeta infiniteStoppable = container
                    .get(ContainerComponentRegistry.class)
                    .getComponents()
                    .entrySet()
                    .stream()
                    .filter(e -> "streamingCustomized".equals(e.getKey()))
                    .map(java.util.Map.Entry::getValue)
                    .flatMap(c -> c.getPartitionMappers().values().stream())
                    .findFirst()
                    .orElse(null);
            assertNotNull(infinite);
            final ParameterMeta parent = infiniteStoppable
                    .getParameterMetas()
                    .get()
                    .stream()
                    .filter(m -> m.getName().equals(m.getPath()))
                    .findFirst()
                    .orElseGet(() -> fail("No parent config found"));

            // maxRecords
            final ParameterMeta maxRecords = parent
                    .getNestedParameters()
                    .stream()
                    .peek(m -> System.out.println(m.getName()))
                    .filter(meta -> "$maxRecords".equals(meta.getName()))
                    .findFirst()
                    .orElseGet(() -> fail("Missing streaming max records param"));
            System.out.println(maxRecords);
            assertEquals("-1", maxRecords.getMetadata().get("tcomp::ui::defaultvalue::value"));
            assertEquals("-1", maxRecords.getMetadata().get("tcomp::validation::min"));
            assertEquals(Type.NUMBER, maxRecords.getType());
            assertEquals(Long.class, maxRecords.getJavaType());
            // maxDurationMs
            final ParameterMeta maxDurationMs = parent
                    .getNestedParameters()
                    .stream()
                    .peek(m -> System.out.println(m.getName()))
                    .filter(meta -> "$maxDurationMs".equals(meta.getName()))
                    .findFirst()
                    .orElseGet(() -> fail("Missing streaming max duration param"));
            System.out.println(maxDurationMs);
            assertEquals("-1", maxDurationMs.getMetadata().get("tcomp::ui::defaultvalue::value"));
            assertEquals("-1", maxDurationMs.getMetadata().get("tcomp::validation::min"));
            assertEquals(Type.NUMBER, maxRecords.getType());
            assertEquals(Long.class, maxRecords.getJavaType());
        } finally { // clean temp files
            DynamicContainerFinder.LOADERS.clear();
            Stream.of(pluginFolder.listFiles()).forEach(File::delete);
            if (ofNullable(pluginFolder.listFiles()).map(f -> f.length == 0).orElse(true)) {
                pluginFolder.delete();
            }
        }
    }
}
