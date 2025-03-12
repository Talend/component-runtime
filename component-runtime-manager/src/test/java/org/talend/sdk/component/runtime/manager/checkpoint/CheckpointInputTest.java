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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.json.JsonArray;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.bind.Jsonb;
import javax.json.bind.spi.JsonbProvider;
import javax.json.spi.JsonProvider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;
import org.talend.sdk.component.runtime.input.CheckpointState;
import org.talend.sdk.component.runtime.input.Input;
import org.talend.sdk.component.runtime.input.InputImpl;
import org.talend.sdk.component.runtime.input.Mapper;
import org.talend.sdk.component.runtime.manager.ComponentManager;
import org.talend.sdk.component.runtime.manager.asm.PluginGenerator;
import org.talend.sdk.component.runtime.manager.chain.ChainedMapper;
import org.talend.sdk.component.runtime.manager.chain.Job;
import org.talend.sdk.component.runtime.manager.serialization.DynamicContainerFinder;
import org.talend.sdk.component.runtime.record.RecordBuilderFactoryImpl;

import lombok.extern.slf4j.Slf4j;

@Slf4j
class CheckpointInputTest {

    private final JsonProvider jsonp = JsonProvider.provider();

    private final JsonBuilderFactory jsonFactory = jsonp.createBuilderFactory(emptyMap());

    private final Jsonb jsonb = JsonbProvider.provider().create().build();

    private final PluginGenerator pluginGenerator = new PluginGenerator();

    private final Consumer<CheckpointState> checkpointCallback = o -> {
        assertNotNull(o);
        final JsonObject checkpoint = o.toJson();
        log.warn("[consumer] {}", checkpoint);
        if (checkpoint.getJsonObject(CheckpointState.CHECKPOINT_KEY).getInt("since_id") == 9) {
            assertEquals("finished", checkpoint.getJsonObject(CheckpointState.CHECKPOINT_KEY).getString("status"));
        } else {
            assertEquals("running", checkpoint.getJsonObject(CheckpointState.CHECKPOINT_KEY).getString("status"));
        }
    };

    @Test
    void standardLifecycle(@TempDir final Path temporaryFolder) {
        try (final ComponentManager manager = newTestManager(temporaryFolder)) {
            final InputImpl input = getInput(manager, "list-input", 1, emptyMap());
            input.start(checkpointCallback);
            do {
            } while (input.next() != null);
            assertNull(input.next());
            JsonObject json = input.getCheckpoint().toJson();
            assertEquals(9, json.getJsonObject(CheckpointState.CHECKPOINT_KEY).getInt("since_id"));
            assertEquals("finished", json.getJsonObject(CheckpointState.CHECKPOINT_KEY).getString("status"));
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
    void jobLifeCycleWithCheckpoint(@TempDir final Path temporaryFolder) {
        try (final ComponentManager manager = newTestManager(temporaryFolder)) {
            Job
                    .components()
                    .component("countdown", "checkpoint://list-input")
                    .checkpoint(checkpointCallback)
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
            final Map<String, String> configuration = new HashMap<>();
            configuration.put("configuration.user", "localhost");
            configuration.put("configuration.pass", "localpass");
            configuration.put("configuration.checkpoint.sinceId", "5");
            configuration.put("configuration.checkpoint.status", "none");

            final InputImpl input = getInput(manager, "list-input", 1, configuration);
            input.start(checkpointCallback);
            input.next();
            input.next();
            input.next();
            JsonObject chck = input.getCheckpoint().toJson();
            assertEquals(7, chck.getJsonObject(CheckpointState.CHECKPOINT_KEY).getInt("since_id"));
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
            configuration.put("configuration.user", "localhost");
            configuration.put("configuration.pass", "localpass");
            configuration.put("configuration.checkpoint.sinceId", "5");
            configuration.put("configuration.checkpoint.status", "finished");
            //
            final Mapper mapper = mgr.findMapper("checkpoint", "list-input", 1, configuration).get();
            // org.talend.sdk.component.runtime.di.JobStateAware.init(mapper, globalMap);
            mapper.start(); // LocalPartitionMapper
            final ChainedMapper chainedMapper;
            // get ChainedMapper
            final List<Mapper> split = mapper.split(mapper.assess());
            chainedMapper = new ChainedMapper(mapper, split.iterator());
            chainedMapper.start();
            mapper.stop();
            //
            final Input input = chainedMapper.create(); // ChainedInput
            // input.start(null, (s) -> System.err.println("state: " + s)); // ChainedInput delegate is InputImpl
            input.start();
            //
            Object rawData;
            int counted = 0;
            while ((rawData = input.next()) != null) {
                // data conversion of rawData to rowStruct and operate on rowStruct...
                if (input.isCheckpointReady()) {
                    System.err.println(input.getCheckpoint());
                }
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
            final Map<String, String> configuration = new HashMap<>();
            configuration.put("configuration.user", "localhost");
            configuration.put("configuration.pass", "localpass");
            configuration.put("configuration.checkpoint.sinceId", "5");
            configuration.put("configuration.checkpoint.status", "none");
            //
            final Mapper mapper = mgr.findMapper("checkpoint", "list-input", 1, configuration).get();
            mapper.start();
            final List<Mapper> split = mapper.split(mapper.assess());
            final ChainedMapper chainedMapper = new ChainedMapper(mapper, split.iterator());
            chainedMapper.start();
            mapper.stop();
            //
            final Input input = chainedMapper.create(); // ChainedInput
            input.start((s) -> log.warn("[studioLifecycleWithResume] state: {}.", s));
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

    @Test
    void manualUsage(@TempDir final Path temporaryFolder) {
        try (final ComponentManager mgr = newTestManager(temporaryFolder)) {
            //
            final Map<String, String> configuration = new HashMap<>();
            configuration.put("configuration.user", "localhost");
            configuration.put("configuration.pass", "localpass");
            configuration.put("configuration.checkpoint.sinceId", "5");
            configuration.put("configuration.checkpoint.status", "finished");
            //
            final Input input = getInput(mgr, "list-input", 1, configuration);
            input.start();
            Object previousCheckpoint = 1;
            Object currentCheckpoint = 1;
            while ((input.next()) != null) {
                if (input.isCheckpointReady()) {
                    currentCheckpoint = input.getCheckpoint();
                    assertNotNull(currentCheckpoint);
                    assertNotEquals(currentCheckpoint, previousCheckpoint);
                    previousCheckpoint = currentCheckpoint;
                }
            }
            input.stop();
        }
    }

    @Test
    void resumeableInputManualUsage(@TempDir final Path temporaryFolder) throws Exception {
        try (final ComponentManager mgr = newTestManager(temporaryFolder)) {
            // restore configuration from json
            final Map<String, String> configuration = jsonToMap("", resourceAsJson("data/resumeable-input-conf.json"));
            final Map<String, String> checkpointConf = jsonToMap("", resourceAsJson("data/checkpoint_id.json"));
            final String resourcePath = getClass().getClassLoader().getResource("data/names.csv").getPath();
            configuration.put("configuration.resourcePath", resourcePath);
            configuration.putAll(checkpointConf);
            //
            final Input input = getInput(mgr, "resumeable-input", 1, configuration);
            input.start();
            Record record;
            int counted = 0;
            while ((record = (Record) input.next()) != null) {
                counted++;
                if (input.isCheckpointReady()) {
                    assertNotNull(input.getCheckpoint());
                }
            }
            final JsonObject checkpoint = input.getCheckpoint().toJson().getJsonObject(CheckpointState.CHECKPOINT_KEY);
            log.warn("[resumeableInputManualUsage] checkpoint: {}.", checkpoint);
            assertEquals(100, checkpoint.getInt("lastId"));
            assertEquals("2023-04-09", checkpoint.getString("lastUpdate"));
            assertEquals("finished", checkpoint.getString("status"));
            input.stop();
            assertEquals(50, counted);
        }
    }

    private JsonObject resourceAsJson(final String resource) {
        try {
            final Path cp = Paths.get(getClass().getClassLoader().getResource(resource).toURI());
            final String dd = new String(Files.readAllBytes(cp));
            return jsonb.fromJson(dd, JsonObject.class);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static Map<String, String> jsonToMap(String path, JsonValue jsonValue) {
        final Map<String, String> result = new HashMap<>();
        if (jsonValue instanceof JsonObject) {
            JsonObject jsonObj = (JsonObject) jsonValue;
            for (String key : jsonObj.keySet()) {
                String newPath = path.isEmpty() ? key : path + "." + key;
                result.putAll(jsonToMap(newPath, jsonObj.get(key)));
            }
        } else if (jsonValue instanceof JsonArray) {
            JsonArray jsonArray = (JsonArray) jsonValue;
            for (int i = 0; i < jsonArray.size(); i++) {
                String newPath = path + "[" + i + "]";
                result.putAll(jsonToMap(newPath, jsonArray.get(i)));
            }
        } else {
            String str;
            if (jsonValue.getValueType() == JsonValue.ValueType.STRING) {
                str = ((JsonString) (jsonValue)).getString();
            } else {
                str = jsonValue.toString();
            }
            result.put(path, str);
        }

        return result;
    }

    private InputImpl getInput(final ComponentManager manager, final String emitter, final int version,
            final Map<String, String> configuration) {
        final Mapper mapper = manager.findMapper("checkpoint", emitter, version, configuration).get();
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