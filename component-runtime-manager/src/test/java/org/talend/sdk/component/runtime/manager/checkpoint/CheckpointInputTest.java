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
package org.talend.sdk.component.runtime.manager.checkpoint;

import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.spi.JsonProvider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;
import org.talend.sdk.component.runtime.input.Input;
import org.talend.sdk.component.runtime.input.InputImpl;
import org.talend.sdk.component.runtime.input.LocalPartitionMapper;
import org.talend.sdk.component.runtime.input.Mapper;
import org.talend.sdk.component.runtime.manager.ComponentManager;
import org.talend.sdk.component.runtime.manager.asm.PluginGenerator;
import org.talend.sdk.component.runtime.manager.chain.ChainedMapper;
import org.talend.sdk.component.runtime.manager.chain.Job;
import org.talend.sdk.component.runtime.manager.serialization.DynamicContainerFinder;
import org.talend.sdk.component.runtime.record.RecordBuilderFactoryImpl;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CheckpointInputTest {

    private final JsonProvider jsonp = JsonProvider.provider();

    private final JsonBuilderFactory jsonFactory = jsonp.createBuilderFactory(emptyMap());

    private final PluginGenerator pluginGenerator = new PluginGenerator();

    private final Function<Object, Void> defaultCheckpointFunc = o -> null;

    private final Function<Object, Void> loggingCheckpointFunc = o -> {
        log.error("[loggingCheckpointFunc] {}", o);
        final JsonObject checkpoint = (JsonObject) o;
        if (checkpoint.getInt("checkpoint") == -1) {
            assertEquals(checkpoint.getString("status"), "finished");
        } else {
            assertEquals(checkpoint.getString("status"), "running");
        }
        return null;
    };

    @Test
    void standardLifecycle(@TempDir final Path temporaryFolder) {
        try (final ComponentManager manager = newTestManager(temporaryFolder)) {
            final InputImpl input = getInput(manager, 1, emptyMap());
            input.start();
            do {
            } while (input.next() != null);
            assertNull(input.next());
            assertEquals(-1, ((JsonObject) input.checkpoint()).getInt("checkpoint"));
            assertEquals("finished", ((JsonObject) input.checkpoint()).getString("status"));
            input.stop();
        }
    }

    @Test
    void jobLifeCycle(@TempDir final Path temporaryFolder) {
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

    @Test
    void resumeCheckpoint(@TempDir final Path temporaryFolder) {
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

    /**
     * This test is a lightweight simulation of the studio lifecycle.
     *
     * @param temporaryFolder
     */
    @Test
    void studioLifecycle(@TempDir final Path temporaryFolder) {
        try (final ComponentManager mgr = newTestManager(temporaryFolder)) {
            //
            final Map<String, String> configuration = new HashMap<>();
            configuration.put("configuration.connection.hostname", "localhost");
            configuration.put("configuration.check.hostname", "localhost");
            //
            final Mapper mapper = mgr.findMapper("checkpoint", "list-input", 1, configuration).get();
            //
            // org.talend.sdk.component.runtime.di.JobStateAware.init(mapper, globalMap);
            //
            mapper.start(); // LocalPartitionMapper
            final ChainedMapper chainedMapper;
            // get ChainedMapper
            final List<Mapper> split = mapper.split(mapper.assess());
            chainedMapper = new ChainedMapper(mapper, split.iterator());
            chainedMapper.start();
            mapper.stop();
            //
            final Input input = chainedMapper.create(); // ChainedInput
            input.start(null); // ChainedInput delegate is InputImpl
            //
            Object rawData;
            int counted = 0;
            // RowStruct rowStruct = new RowStruct(); // @Data static class RowStruct {Integer data;}
            while ((rawData = input.next()) != null) {
                // data conversion of rawData to rowStruct ...
                // operate on rowStruct...
                counted++;
            }
            assertEquals(10, counted);
            // shutdown
            input.stop();
            chainedMapper.stop();
        }
    }

    @Test
    void studioLifecycleWithResume(@TempDir final Path temporaryFolder) {
        try (final ComponentManager mgr = newTestManager(temporaryFolder)) {
            final JsonObject state = jsonFactory.createObjectBuilder()
                    .add("checkpoint", 5)
                    .add("status", "running")
                    .build();
            //
            final Map<String, String> configuration = new HashMap<>();
            configuration.put("configuration.connection.hostname", "localhost");
            //
            final Mapper mapper = mgr.findMapper("checkpoint", "list-input", 1, configuration).get();
            //
            // org.talend.sdk.component.runtime.di.JobStateAware.init(mapper, globalMap);
            //
            mapper.start(); // LocalPartitionMapper
            final ChainedMapper chainedMapper;
            // get ChainedMapper
            final List<Mapper> split = mapper.split(mapper.assess());
            chainedMapper = new ChainedMapper(mapper, split.iterator());
            chainedMapper.start();
            mapper.stop();
            //
            final Input input = chainedMapper.create(); // ChainedInput
            input.start(state); // ChainedInput delegate is InputImpl
            //
            Object rawData;
            int counted = 0;
            // RowStruct rowStruct = new RowStruct(); // @Data static class RowStruct {Integer data;}
            while ((rawData = input.next()) != null) {
                // data conversion of rawData to rowStruct ...
                // operate on rowStruct...
                counted++;
            }
            assertEquals(5, counted);
            // shutdown
            input.stop();
            chainedMapper.stop();
        }
    }

    private InputImpl getInput(final ComponentManager manager, final int version,
            final Map<String, String> configuration) {
        final LocalPartitionMapper mapper =
                LocalPartitionMapper.class
                        .cast(manager.findMapper("checkpoint", "list-input", version, configuration).get());
        return InputImpl.class.cast(mapper.create());
    }

    private ComponentManager newTestManager(final Path temporaryFolder) {
        return new ComponentManager(new File("target/fake-m2"), "TALEND-INF/dependencies.txt", null) {

            final File jar = pluginGenerator.createChainPlugin(temporaryFolder.toFile(), "checkpoint-test.jar");

            final ComponentManager originalMgr = contextualInstance().get();

            {
                contextualInstance().set(this);
                final String containerId = addPlugin(jar.getAbsolutePath());
                DynamicContainerFinder.SERVICES
                        .put(RecordBuilderFactory.class, new RecordBuilderFactoryImpl(containerId));
            }

            @Override
            public void close() {
                DynamicContainerFinder.SERVICES.clear();
                super.close();
                contextualInstance().set(originalMgr);
            }
        };
    }

}