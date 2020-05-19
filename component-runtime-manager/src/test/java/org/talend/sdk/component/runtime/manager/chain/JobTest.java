/**
 * Copyright (C) 2006-2019 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.runtime.manager.chain;

import static java.net.URLEncoder.encode;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.io.TempDir;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;
import org.talend.sdk.component.runtime.input.LocalPartitionMapper;
import org.talend.sdk.component.runtime.manager.ComponentManager;
import org.talend.sdk.component.runtime.manager.asm.PluginGenerator;
import org.talend.sdk.component.runtime.manager.serialization.DynamicContainerFinder;
import org.talend.sdk.component.runtime.output.ProcessorImpl;
import org.talend.sdk.component.runtime.record.RecordBuilderFactoryImpl;
import org.talend.test.InMemCollector;

class JobTest {

    private final PluginGenerator pluginGenerator = new PluginGenerator();

    @Test
    void validateConnections() {
        assertThrows(IllegalStateException.class,
                () -> Job
                        .components()
                        .component("list", "chain://list")
                        .component("file", "chain://file")
                        .connections()
                        .from("list")
                        .to("file")
                        .from("list")
                        .to("file")
                        .build());
    }

    @Test
    void validateCyclic() {
        assertThrows(IllegalStateException.class,
                () -> Job
                        .components()
                        .component("start", "chain://start")
                        .component("list", "chain://list")
                        .component("file", "chain://file")
                        .connections()
                        .from("start")
                        .to("list", "aa")
                        .from("list")
                        .to("file")
                        .from("file")
                        .to("list")
                        .build());

        // a valid flow
        Job
                .components()
                .component("start", "chain://start")
                .component("list", "chain://list")
                .component("list_2", "chain://list")
                .component("file", "chain://file")
                .component("file_2", "chain://file")
                .connections()
                .from("start", "main")
                .to("list")
                .from("start", "reject")
                .to("list_2")
                .from("list")
                .to("file")
                .from("list_2")
                .to("file_2")
                .build();
    }

    @Test
    void validateComponentID() {
        assertThrows(IllegalStateException.class,
                () -> Job.components().component("file", "chain://file").connections().from("list").to("file").build());
    }

    @Test
    void validateJobLifeCycle(final TestInfo info, @TempDir final Path temporaryFolder) {
        final String testName = info.getTestMethod().get().getName();
        final String plugin = testName + ".jar";
        final File jar = pluginGenerator.createChainPlugin(temporaryFolder.toFile(), plugin);
        try (final ComponentManager manager = newTestManager(jar)) {

            Job
                    .components()
                    .component("countdown", "lifecycle://countdown?__version=1&start=2")
                    .component("square", "lifecycle://square?__version=1")
                    .connections()
                    .from("countdown")
                    .to("square")
                    .build()
                    .run();

            final LocalPartitionMapper mapper =
                    LocalPartitionMapper.class.cast(manager.findMapper("lifecycle", "countdown", 1, emptyMap()).get());

            assertEquals(asList("start", "produce(1)", "produce(0)", "produce(null)", "stop"),
                    ((Supplier<List<String>>) mapper.getDelegate()).get());

            final ProcessorImpl processor =
                    (ProcessorImpl) manager.findProcessor("lifecycle", "square", 1, emptyMap()).get();

            assertEquals(asList("start", "beforeGroup", "onNext(1)", "afterGroup", "beforeGroup", "onNext(0)",
                    "afterGroup", "stop"), ((Supplier<List<String>>) processor.getDelegate()).get());

        }
    }

    @Test
    void multipleEmitSupport(final TestInfo info, @TempDir final Path temporaryFolder) {
        final String testName = info.getTestMethod().get().getName();
        final String plugin = testName + ".jar";
        final File jar = pluginGenerator.createChainPlugin(temporaryFolder.toFile(), plugin);
        try (final ComponentManager manager = newTestManager(jar)) {
            final Collection<JsonObject> outputs =
                    InMemCollector.getShadedOutputs(manager.findPlugin(plugin).get().getLoader());
            outputs.clear();
            Job
                    .components()
                    .component("from", "single://input")
                    .component("to", "chain://count?multiple=true")
                    .component("end", "store://collect")
                    .connections()
                    .from("from")
                    .to("to")
                    .from("to")
                    .to("end")
                    .build()
                    .run();
            // {"cumulatedSize":15}.length x2
            assertEquals(asList(15, 30), outputs.stream().map(json -> json.getInt("cumulatedSize")).collect(toList()));
        }
    }

    @Test
    void defaultKeyProvider(final TestInfo info, @TempDir final Path temporaryFolder) throws IOException {
        final String testName = info.getTestMethod().get().getName();
        final String plugin = testName + ".jar";
        final File jar = pluginGenerator.createChainPlugin(temporaryFolder.toFile(), plugin);
        final File out = new File(temporaryFolder.toFile(), testName + "-out.txt");

        try (final ComponentManager manager = newTestManager(jar)) {

            Job
                    .components()
                    .component("users", "db://input?__version=1&tableName=users")
                    .component("address", "db://input?__version=1&tableName=address")
                    .component("salary", "db://input?__version=1&tableName=salary")
                    .component("concat", "processor://concat?__version=1")
                    .component("concat_2", "processor://concat?__version=1")
                    .component("outFile",
                            "file://out?__version=1&configuration.file=" + encode(out.getAbsolutePath(), "utf-8"))
                    .connections()
                    .from("users")
                    .to("concat", "str1")
                    .from("address")
                    .to("concat", "str2")
                    .from("concat")
                    .to("concat_2", "str1")
                    .from("salary")
                    .to("concat_2", "str2")
                    .from("concat_2")
                    .to("outFile")
                    .build()
                    .run();

            assertTrue(out.isFile());
            assertEquals(asList("sophia paris 1900", "emma nantes 3055", "liam strasbourg 2600.30", "ava lyon 2000.5"),
                    Files.readAllLines(out.toPath()));
        }
    }

    @Test
    void jobKeyProvider(final TestInfo info, @TempDir final Path temporaryFolder) throws IOException {
        final String testName = info.getTestMethod().get().getName();
        final String plugin = testName + ".jar";
        final File jar = pluginGenerator.createChainPlugin(temporaryFolder.toFile(), plugin);
        final File out = new File(temporaryFolder.toFile(), testName + "-out.txt");

        try (final ComponentManager manager = newTestManager(jar)) {

            Job
                    .components()
                    .component("users", "db://input?__version=1&tableName=users")
                    .component("address", "db://input?__version=1&tableName=address")
                    .component("salary", "db://input?__version=1&tableName=salary")
                    .component("concat", "processor://concat?__version=1")
                    .component("concat_2", "processor://concat?__version=1")
                    .component("outFile",
                            "file://out?__version=1&configuration.file=" + encode(out.getAbsolutePath(), "utf-8"))
                    .connections()
                    .from("users")
                    .to("concat", "str1")
                    .from("address")
                    .to("concat", "str2")
                    .from("concat")
                    .to("concat_2", "str1")
                    .from("salary")
                    .to("concat_2", "str2")
                    .from("concat_2")
                    .to("outFile")
                    .build()
                    .property(GroupKeyProvider.class.getName(), (GroupKeyProvider) context -> {
                        if (context.getComponentId().equals("users")) {
                            return context.getData().get(String.class, "id");
                        }

                        return context.getData().get(String.class, "userId");
                    })
                    .run();

            assertTrue(out.isFile());
            assertEquals(asList("emma strasbourg 1900", "sophia nantes 2000.5", "liam lyon 3055", "ava paris 2600.30"),
                    Files.readAllLines(out.toPath()));
        }
    }

    @Test
    void maxBatchSize(final TestInfo info, @TempDir final Path temporaryFolder) throws IOException {
        final String testName = info.getTestMethod().get().getName();
        final String plugin = testName + ".jar";
        final File jar = pluginGenerator.createChainPlugin(temporaryFolder.toFile(), plugin);
        final File out = new File(temporaryFolder.toFile(), testName + "-out.txt");
        try (final ComponentManager ignored = newTestManager(jar)) {
            Job
                    .components()
                    .component("users", "db://input?__version=1&tableName=users")
                    .component("outFile",
                            "file://out?configuration.$maxBatchSize=2&__version=1&configuration.file="
                                    + encode(out.getAbsolutePath(), "utf-8"))
                    .connections()
                    .from("users")
                    .to("outFile")
                    .build()
                    .run();

            assertTrue(out.isFile());
            assertEquals(asList("sophia", "emma", "liam", "ava"), Files.readAllLines(out.toPath()));
        }
    }

    @Test
    void contextualKeyProvider(final TestInfo info, @TempDir final Path temporaryFolder) throws IOException {
        final String testName = info.getTestMethod().get().getName();
        final String plugin = testName + ".jar";
        final File jar = pluginGenerator.createChainPlugin(temporaryFolder.toFile(), plugin);
        final File out = new File(temporaryFolder.toFile(), testName + "-out.txt");

        try (final ComponentManager manager = newTestManager(jar)) {

            final GroupKeyProvider foreignKeyProvider =
                    (GroupKeyProvider) context -> context.getData().get(String.class, "userId");

            Job
                    .components()
                    .component("users", "db://input?__version=1&tableName=users")
                    .property(GroupKeyProvider.class.getName(),
                            (GroupKeyProvider) context -> context.getData().get(String.class, "id"))
                    .component("address", "db://input?__version=1&tableName=address")
                    .property(GroupKeyProvider.class.getName(), foreignKeyProvider)
                    .component("salary", "db://input?__version=1&tableName=salary")
                    .property(GroupKeyProvider.class.getName(), foreignKeyProvider)
                    .component("concat", "processor://concat?__version=1")
                    .property(GroupKeyProvider.class.getName(), foreignKeyProvider)
                    .component("concat_2", "processor://concat?__version=1")
                    .component("outFile",
                            "file://out?__version=1&configuration.file=" + encode(out.getAbsolutePath(), "utf-8"))
                    .connections()
                    .from("users")
                    .to("concat", "str1")
                    .from("address")
                    .to("concat", "str2")
                    .from("concat")
                    .to("concat_2", "str1")
                    .from("salary")
                    .to("concat_2", "str2")
                    .from("concat_2")
                    .to("outFile")
                    .build()
                    .run();

            assertTrue(out.isFile());
            assertEquals(asList("emma strasbourg 1900", "sophia nantes 2000.5", "liam lyon 3055", "ava paris 2600.30"),
                    Files.readAllLines(out.toPath()));
        }
    }

    private ComponentManager newTestManager(final File jar) {
        return new ComponentManager(new File("target/fake-m2"), "TALEND-INF/dependencies.txt", null) {

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
