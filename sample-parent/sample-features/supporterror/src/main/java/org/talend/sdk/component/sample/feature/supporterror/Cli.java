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
package org.talend.sdk.component.sample.feature.supporterror;


import static java.util.Optional.ofNullable;
import static lombok.AccessLevel.PRIVATE;
import static org.talend.sdk.component.runtime.manager.ComponentManager.findM2;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.api.record.SchemaProperty;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;
import org.talend.sdk.component.dependencies.maven.Artifact;
import org.talend.sdk.component.dependencies.maven.MvnCoordinateToFileConverter;
import org.talend.sdk.component.runtime.input.PartitionMapperImpl;
import org.talend.sdk.component.runtime.manager.ComponentManager;
import org.talend.sdk.component.runtime.record.RecordBuilderFactoryImpl;
import org.talend.sdk.component.runtime.serialization.ContainerFinder;
import org.talend.sdk.component.runtime.serialization.LightContainer;


import lombok.NoArgsConstructor;

@NoArgsConstructor(access = PRIVATE)
@Command(name="supportError")
public final class Cli implements Callable<Integer> {

    //support errors or not. default=false
    @Option(names = "-s", defaultValue = "false")
    boolean support;

    @Option(names = { "-f", "--file" }, paramLabel = "ARCHIVE", description = "the jar file")
    File jar;

    @Option(names = { "-m", "--mapper" },defaultValue = "SupportErrorMapper")
    String mapper;

    @Option(names = { "-fl", "--family" },defaultValue = "supporterror")
    String family;

    static final String GAV = "org.talend.sdk.component.sample.feature:supporterror:jar:"
            + Versions.KIT_VERSION;

    @Override
    public Integer call() throws Exception {

        try (final ComponentManager manager = manager(jar, GAV)) {
        //    final JsonObject jsonConfig = readJsonFromFile(configurationFile);
            final Map<String, String> configuration = new HashMap<>();

            info("support" + String.valueOf(support));
            if (support) {
                setSupportError(support);
            }
            // create the mapper
            final PartitionMapperImpl mpr = (PartitionMapperImpl)manager.findMapper(family, mapper, 1, configuration)
                    .orElseThrow(() -> new IllegalStateException(
                            String.format("No mapper found for: %s/%s.", family, manager)));

//            final ProcessorImpl processor = (ProcessorImpl)manager.findMapper(family, "SupportErrorInput", 1, configuration)
//                    .orElseThrow(() -> new IllegalStateException(
//                            String.format("No Processor found for: %s/%s.", family, manager)));

            info("create input now.");

            SupportErrorInput seInput = new SupportErrorInput(null);
            seInput.init();

            info("getting the record.");
            Record data = seInput.data();

            info("Record isValid = " + data.isValid());
            entryout(data, "date");
            entryout(data, "age");
           //
            info("finished.");
        } catch (Exception e) {
            error(e);
        }

        return 0;
    }

    private static void entryout(final Record data, final String column) {
        Schema.Entry ageEntry = data.getSchema().getEntries().stream().filter(e -> column.equals(e.getName())).findAny().get();
        if(ageEntry.isValid()) {
            Integer age = data.get(Integer.class, column);
            //  process the age...
            info("Record '"+ column +"': " + age);
        } else{
            String errorMessage = ageEntry.getProp(SchemaProperty.ENTRY_ERROR_MESSAGE);
            info("ERROR: " + errorMessage);
        }
    }

    //set support or not.
    public void setSupportError(final boolean supportError) {
        final String val = System.getProperty(Record.RECORD_ERROR_SUPPORT);
        System.setProperty(Record.RECORD_ERROR_SUPPORT, String.valueOf(supportError));
        final String val2 = System.getProperty(Record.RECORD_ERROR_SUPPORT);
    }

    public static void main(final String... args) {
        int exitCode = new CommandLine(new Cli()).execute(args);
        System.exit(exitCode);
    }

    static final String ERROR = "[ERROR] ";

    static final String WARN = "[WARN]  ";

    static final String INFO = "[INFO]  ";

    static MvnCoordinateToFileConverter mvnCoordinateToFileConverter = new MvnCoordinateToFileConverter();

    public static ComponentManager manager(final File jar, final String artifact) {
        return new ComponentManager(findM2()) {

            final ContainerFinder containerFinder = ContainerFinder.Instance.get();

            final ComponentManager originalMgr = contextualInstance().get();

            {
                contextualInstance().set(this);
                String containerId;
                if (jar != null) {
                    containerId = addPlugin(jar.getAbsolutePath());
                    Cli.info(String.format("Manager is using plugin %s from %s.", containerId, jar));
                } else {
                    final String pluginPath = ofNullable(artifact)
                            .map(gav -> mvnCoordinateToFileConverter.toArtifact(gav))
                            .map(Artifact::toPath)
                            .orElseThrow(() -> new IllegalArgumentException("Plugin GAV can't be empty"));
                    String p = findM2().resolve(pluginPath).toAbsolutePath().toString();
                    containerId = addPlugin(p);
                    Cli.info(String.format("Manager is using plugin: %s from GAV %s.", containerId, artifact));
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

    public static void info(final String message) {
        System.out.println(INFO + message);
    }

    public static void warn(final String message) {
        System.err.println(WARN + message);
    }

    public static void error(final Throwable e) {
        System.err.println(ERROR + e.getMessage());
        System.exit(501);
    }

}
