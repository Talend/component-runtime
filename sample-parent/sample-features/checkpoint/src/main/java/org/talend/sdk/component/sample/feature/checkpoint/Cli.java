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
package org.talend.sdk.component.sample.feature.checkpoint;

import static java.util.Optional.ofNullable;
import static lombok.AccessLevel.PRIVATE;
import static org.talend.sdk.component.runtime.manager.ComponentManager.findM2;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonWriter;

import org.talend.sdk.component.api.service.record.RecordBuilderFactory;
import org.talend.sdk.component.dependencies.maven.Artifact;
import org.talend.sdk.component.dependencies.maven.MvnCoordinateToFileConverter;
import org.talend.sdk.component.runtime.input.CheckpointState;
import org.talend.sdk.component.runtime.input.InputImpl;
import org.talend.sdk.component.runtime.input.Mapper;
import org.talend.sdk.component.runtime.manager.ComponentManager;
import org.talend.sdk.component.runtime.record.RecordBuilderFactoryImpl;
import org.talend.sdk.component.runtime.serialization.ContainerFinder;
import org.talend.sdk.component.runtime.serialization.LightContainer;
import org.tomitribe.crest.Main;
import org.tomitribe.crest.api.Command;
import org.tomitribe.crest.api.Default;
import org.tomitribe.crest.api.Option;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = PRIVATE)
public final class Cli {

    static final String ERROR = "[ERROR] ";

    static final String WARN = "[WARN]  ";

    static final String INFO = "[INFO]  ";

    static final String DATA = "[DATA]  ";

    static final String GAV = "org.talend.sdk.component.sample.feature:checkpoint:jar:1.79.0-SNAPSHOT";

    static MvnCoordinateToFileConverter mvnCoordinateToFileConverter = new MvnCoordinateToFileConverter();

    static int counter = 0;

    static int checkpointId = 0;

    // CHECKSTYLE:OFF
    @Command("checkpoint")
    public static void runInput(
            @Option("gav") @Default(GAV) final String gav,
            @Option("jar") final File jar,
            @Option("family") @Default("checkpoint") final String family,
            @Option("mapper") @Default("input") final String mapper,
            @Option("configuration") @Default("./configuration.json") final File configurationFile,
            @Option("checkpoint") @Default("./checkpoint.json") final File checkpointFile,
            @Option("re-use") @Default("true") final boolean reuse,
            @Option("fail-after") @Default("-1") final int failAfter,
            @Option("log") @Default("false") final boolean log,
            @Option("disable") @Default("false") final boolean disable,
            @Option("work-dir") @Default("./") final File work) {
        if (!disable) {
            System.setProperty("talend.checkpoint.enabled", "true");
        }
        //
        // check consistency and build parameters
        //
        if (jar != null) {
            if (!gav.equals(GAV)) {
                error("Cannot mix arguments --gav and --jar.");
            }
            if (!jar.isFile()) {
                error("Not a file: " + jar.getAbsolutePath());
            }
        }
        if (!configurationFile.exists() && !configurationFile.getPath().equals("./configuration.json")) {
            error("Not a file: " + configurationFile.getAbsolutePath());
        }
        if (!checkpointFile.exists() && !checkpointFile.getPath().equals("./checkpoint.json")) {
            error("Not a file: " + checkpointFile.getAbsolutePath());
        }
        if (!work.exists()) {
            work.mkdirs();
        }
        //
        // define the checkpoint callback
        //
        final Consumer<CheckpointState> callback = (state) -> {
            checkpointId++;
            if (log) {
                info("Checkpoint " + checkpointId + " reached.");
            }
            File file;
            if (reuse) {
                file = new File(checkpointFile.getAbsolutePath());
            } else {
                file = new File(work, "checkpoint_" + checkpointId + ".json");
            }
            try (FileOutputStream fos = new FileOutputStream(file);
                    JsonWriter writer = Json.createWriter(fos)) {
                writer.writeObject(state.toJson());
            } catch (IOException e) {
                error("Failed to write checkpoint to file: " + e.getMessage());
            }
        };

        try (final ComponentManager manager = manager(jar.getAbsolutePath(), gav)) {
            //
            // build configuration for mapper
            //
            final JsonObject jsonConfig = readJsonFromFile(configurationFile);
            final Map<String, String> configuration =
                    jsonConfig == null ? new HashMap<>() : manager.jsonToMap(jsonConfig);
            final JsonObject jsonCheckpoint = readJsonFromFile(checkpointFile);
            if (jsonCheckpoint != null) {
                info("checkpoint: " + jsonCheckpoint);
                Map<String, String> chk = manager.jsonToMap(jsonCheckpoint);
                configuration.putAll(chk);
            }
            info("configuration: " + configuration);
            // create the mapper
            final Mapper mpr = manager.findMapper(family, mapper, 1, configuration).get();
            final InputImpl input = InputImpl.class.cast(mpr.create());
            Object record;
            // run lifecycle
            input.start(callback);
            while ((record = input.next()) != null) {
                data(record);
                counter++;
                if (failAfter > 0 && counter++ >= failAfter) {
                    error("Fail after " + failAfter + " records reached.");
                }
            }
            input.stop();
            //
            info("finished.");
        } catch (Exception e) {
            error(Arrays.stream(e.getCause().getStackTrace())
                    .map(StackTraceElement::toString)
                    .collect(Collectors.joining("\n")));
        }
    }

    public static ComponentManager manager(final String jar, final String artifact) {
        return new ComponentManager(findM2()) {

            final ContainerFinder containerFinder = ContainerFinder.Instance.get();

            final ComponentManager originalMgr = contextualInstance().get();

            {
                contextualInstance().set(this);
                String containerId;
                if (jar != null) {
                    containerId = addPlugin(jar);
                    Cli.info(String.format("plugin: %s from %s.", containerId, jar));
                } else {
                    final String pluginPath = ofNullable(GAV)
                            .map(gav -> mvnCoordinateToFileConverter.toArtifact(gav))
                            .map(Artifact::toPath)
                            .orElseThrow(() -> new IllegalArgumentException("Plugin GAV can't be empty"));
                    String p = findM2().resolve(pluginPath).toAbsolutePath().toString();
                    containerId = addPlugin(p);
                    Cli.info(String.format("plugin: %s from GAV %s.", containerId, artifact));
                }
                DynamicContainerFinder.SERVICES.put(RecordBuilderFactory.class,
                        new RecordBuilderFactoryImpl(containerId));
            }

            @Override
            public void close() {
                DynamicContainerFinder.SERVICES.clear();
                super.close();
                contextualInstance().set(originalMgr);
            }
        };
    }

    public static JsonObject readJsonFromFile(final File filePath) {
        try (FileInputStream fis = new FileInputStream(filePath); JsonReader reader = Json.createReader(fis)) {
            return reader.readObject();
        } catch (Exception e) {
            warn(e.getMessage());
            return null;
        }
    }

    public static class DynamicContainerFinder implements ContainerFinder {

        public static final Map<String, ClassLoader> LOADERS = new ConcurrentHashMap<>();

        public static final Map<Class<?>, Object> SERVICES = new ConcurrentHashMap<>();

        @Override
        public LightContainer find(final String plugin) {
            return new LightContainer() {

                @Override
                public ClassLoader classloader() {
                    return LOADERS.get(plugin);
                }

                @Override
                public <T> T findService(final Class<T> key) {
                    return key.cast(SERVICES.get(key));
                }
            };
        }
    }

    public static void main(final String[] args) throws Exception {
        ofNullable(run(args)).ifPresent(System.out::println);
    }

    public static Object run(final String[] args) throws Exception {
        return new Main(Cli.class).exec(args);
    }

    public static void info(String message) {
        System.out.println(INFO + message);
    }

    public static void warn(String message) {
        System.err.println(WARN + message);
    }

    public static void error(String message) {
        System.err.println(ERROR + message);
        System.exit(501);
    }

    public static void data(Object message) {
        System.out.println(DATA + message);
    }

}
