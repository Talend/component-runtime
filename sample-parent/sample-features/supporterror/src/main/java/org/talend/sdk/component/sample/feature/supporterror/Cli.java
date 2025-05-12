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

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.record.Schema.Entry;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;
import org.talend.sdk.component.dependencies.maven.Artifact;
import org.talend.sdk.component.dependencies.maven.MvnCoordinateToFileConverter;
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

    static final String GAV = "org.talend.sdk.component.sample.feature:supporterror:jar:"
            + Versions.KIT_VERSION;

    @Command("supporterror")
    public static void runInput(
            @Option("gav") @Default(GAV) final String gav,
            @Option("support") @Default("false") final boolean support,
            @Option("gen-some-errors") @Default("true") final boolean genErrors,
            @Option("gen-nbrecords") @Default("10") final int nbRecords,
            @Option("jar") final File jar,
            @Option("family") @Default("supporterror") final String family,
            @Option("mapper") @Default("SupportErrorMapper") final String mapper) {

        info("support " + support);
        if (support) {
            setSupportError(support);
        }

        Map<String, String> config = new HashMap<>();
        config.put("configuration.generateErrors", String.valueOf(genErrors));
        config.put("configuration.nbRecords", String.valueOf(nbRecords));
        run(jar, gav, config, "sampleRecordWithEntriesInError", "RecordWithEntriesInErrorEmitter");
    }

    private static void run(final File jar, final String gav, final Map<String, String> configuration,
            final String family, final String mapper) {
        try (final ComponentManager manager = manager(jar, gav)) {
            info("configuration: " + configuration);

            // create the mapper
            final Mapper mpr = manager.findMapper(family, mapper, 1, configuration)
                    .orElseThrow(() -> new IllegalStateException(
                            String.format("No mapper found for: %s/%s.", family, manager)));

            List<Mapper> mappers = mpr.split(1);
            Record data;

            int count = 0;
            for (Mapper currentMapper : mappers) {
                final InputImpl input = InputImpl.class.cast(currentMapper.create());
                input.start();
                while ((data = (Record) input.next()) != null) {
                    count++;
                    recordOut(count, data);
                }
                input.stop();
            }
            info("finished.");
        } catch (Exception e) {
            error(e);
        }
    }

    private static void recordOut(final int count, final Record record) {
        System.out.printf("Record no %s is valid ? %s\n", count, record.isValid() ? "yes" : "no");
        System.out.printf("\tName: %s\n", record.getString("name"));
        Entry date = record.getSchema().getEntry("date");
        if (date.isValid()) {
            System.out.printf("\tDate: %s\n", record.getDateTime("date"));
        } else {
            System.out.printf("\tDate is on error: \n\t\tMessage:%s\n\t\tFallback value: %s\n",
                    date.getErrorMessage(), date.getErrorFallbackValue());
        }

        Entry age = record.getSchema().getEntry("age");
        if (age.isValid()) {
            System.out.printf("\tAge: %s\n", record.getInt("age"));
        } else {
            System.out.printf("\tAge is on error: \n\t\tMessage:%s\n\t\tFallback value: %s\n",
                    age.getErrorMessage(), age.getErrorFallbackValue());
        }
    }

    // set support or not.
    public static void setSupportError(final boolean supportError) {
        System.setProperty(Record.RECORD_ERROR_SUPPORT, String.valueOf(supportError));
    }

    public static void main(final String[] args) throws Exception {
        ofNullable(run(args)).ifPresent(System.out::println);
    }

    public static Object run(final String[] args) throws Exception {
        return new Main(Cli.class).exec(args);
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

        static final Map<String, ClassLoader> LOADERS = new ConcurrentHashMap<>();

        static final Map<Class<?>, Object> SERVICES = new ConcurrentHashMap<>();

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
