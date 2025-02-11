/*
 * Copyright (C) 2006-2025 Talend Inc. - www.talend.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 */
package org.talend.sdk.component.runtime.manager.checkpoint;

import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.nio.file.Path;
import java.util.Map;

import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.spi.JsonProvider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;
import org.talend.sdk.component.runtime.input.InputImpl;
import org.talend.sdk.component.runtime.input.LocalPartitionMapper;
import org.talend.sdk.component.runtime.manager.ComponentManager;
import org.talend.sdk.component.runtime.manager.asm.PluginGenerator;
import org.talend.sdk.component.runtime.manager.chain.Job;
import org.talend.sdk.component.runtime.manager.serialization.DynamicContainerFinder;
import org.talend.sdk.component.runtime.record.RecordBuilderFactoryImpl;

public class CheckpointInputTest {

    private final JsonProvider jsonp = JsonProvider.provider();

    private final JsonBuilderFactory jsonFactory = jsonp.createBuilderFactory(emptyMap());

    private final PluginGenerator pluginGenerator = new PluginGenerator();

    @Test
    void checkpointNoCheckpoint(@TempDir final Path temporaryFolder) {
        try (final ComponentManager manager = newTestManager(temporaryFolder)) {
            final InputImpl input = getInput(manager, 1, emptyMap());
            input.start();
            input.next();
            input.next();
            assertEquals(1, ((JsonObject) input.checkpoint()).getInt("checkpoint"));
            input.stop();
        }
    }

    @Test
    void checkpointResumeCheckpoint(@TempDir final Path temporaryFolder) {
        try (final ComponentManager manager = newTestManager(temporaryFolder)) {
            final InputImpl input = getInput(manager, 1, emptyMap());
            input.start();
            input.resume(jsonFactory.createObjectBuilder().add("checkpoint", 5).build());
            input.next();
            input.next();
            input.next();
            JsonObject chck = (JsonObject) input.checkpoint();
            assertEquals(7, chck.getInt("checkpoint"));
            input.stop();
        }
    }

    @Test
    void validateCheckpointJobLifeCycle(@TempDir final Path temporaryFolder) {
        try (final ComponentManager manager = newTestManager(temporaryFolder)) {
            Job
                    .components()
                    .component("countdown", "checkpoint://list-input")
                    .component("square", "lifecycle://square?__version=1")
                    .connections()
                    .from("countdown")
                    .to("square")
                    .build()
                    .run();
        }
    }

    private InputImpl getInput(final ComponentManager manager, final int version, final Map<String, String> configuration) {
        final LocalPartitionMapper mapper =
                LocalPartitionMapper.class.cast(manager.findMapper("checkpoint", "list-input", version, configuration).get());
        return InputImpl.class.cast(mapper.create());
    }

    private ComponentManager newTestManager(final Path temporaryFolder) {
        return new ComponentManager(new File("target/fake-m2"), "TALEND-INF/dependencies.txt", null) {
            final File jar = pluginGenerator.createChainPlugin(temporaryFolder.toFile(), "checkpoint-test.jar");
            final ComponentManager originalMgr = ComponentManager.contextualInstance().get();

            {
                ComponentManager.contextualInstance().set(this);
                final String containerId = addPlugin(jar.getAbsolutePath());
                DynamicContainerFinder.SERVICES
                        .put(RecordBuilderFactory.class, new RecordBuilderFactoryImpl(containerId));
            }

            @Override
            public void close() {
                DynamicContainerFinder.SERVICES.clear();
                super.close();
                ComponentManager.contextualInstance().set(originalMgr);
            }
        };
    }

}