/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.talend.sdk.component.junit.base.junit5.TemporaryFolder;
import org.talend.sdk.component.junit.base.junit5.WithTemporaryFolder;
import org.talend.sdk.component.runtime.input.LocalPartitionMapper;
import org.talend.sdk.component.runtime.manager.ComponentManager;
import org.talend.sdk.component.runtime.manager.asm.PluginGenerator;
import org.talend.sdk.component.runtime.output.ProcessorImpl;

@WithTemporaryFolder
public class JobTest {

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
    void validateJobLifeCycle(final TestInfo info, final TemporaryFolder temporaryFolder) {
        final String testName = info.getTestMethod().get().getName();
        final String plugin = testName + ".jar";
        final File jar = pluginGenerator.createChainPlugin(temporaryFolder.getRoot(), plugin);
        try (final ComponentManager manager =
                new ComponentManager(new File("target/fake-m2"), "TALEND-INF/dependencies.txt", null) {

                    {
                        CONTEXTUAL_INSTANCE.set(this);
                        addPlugin(jar.getAbsolutePath());
                    }

                    @Override
                    public void close() {
                        super.close();
                        CONTEXTUAL_INSTANCE.set(null);
                    }
                }) {

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
    void multiOutput(final TestInfo info, final TemporaryFolder temporaryFolder) throws IOException {
        final String testName = info.getTestMethod().get().getName();
        final String plugin = testName + ".jar";
        final File jar = pluginGenerator.createChainPlugin(temporaryFolder.getRoot(), plugin);
        final File output = new File(temporaryFolder.getRoot(), testName + "-out.txt");
        final File reject = new File(temporaryFolder.getRoot(), testName + "-reject.txt");

        try (final ComponentManager manager =
                new ComponentManager(new File("target/fake-m2"), "TALEND-INF/dependencies.txt", null) {

                    {
                        CONTEXTUAL_INSTANCE.set(this);
                        addPlugin(jar.getAbsolutePath());
                    }

                    @Override
                    public void close() {
                        super.close();
                        CONTEXTUAL_INSTANCE.set(null);
                    }
                }) {

            Job
                    .components()
                    .component("list",
                            "chain://list?__version=1&values[0]=a&values[1]=bb-reject&values[2]=ccc&values[3]=rejectccc")
                    .component("file", "chain://file?__version=1&file=" + encode(output.getAbsolutePath(), "utf-8"))
                    .component("count", "chain://count?__version=1")
                    .component("reject", "chain://reject?__version=1&file=" + encode(reject.getAbsolutePath(), "utf-8"))
                    .connections()
                    .from("list")
                    .to("count")
                    .from("count")
                    .to("file")
                    .from("count", "rejected")
                    .to("reject")
                    .build()
                    .run();

            assertTrue(output.isFile());
            assertTrue(reject.isFile());
            assertEquals(asList("1", "4"), Files.readAllLines(output.toPath()));
            assertEquals(asList("bb-reject", "rejectccc"), Files.readAllLines(reject.toPath()));
        }

    }

    @Test
    void multiInOut(final TestInfo info, final TemporaryFolder temporaryFolder) throws IOException {
        final String testName = info.getTestMethod().get().getName();
        final String plugin = testName + ".jar";
        final File jar = pluginGenerator.createChainPlugin(temporaryFolder.getRoot(), plugin);
        final File file = new File(temporaryFolder.getRoot(), testName + "-out.txt");

        try (final ComponentManager manager =
                new ComponentManager(new File("target/fake-m2"), "TALEND-INF/dependencies.txt", null) {

                    {
                        CONTEXTUAL_INSTANCE.set(this);
                        addPlugin(jar.getAbsolutePath());
                    }

                    @Override
                    public void close() {
                        super.close();
                        CONTEXTUAL_INSTANCE.set(null);
                    }
                }) {

            Job
                    .components()
                    .component("firstNames",
                            "chain://list?__version=1&values[0]=noha&values[1]=Emma&values[2]=liam&values[3]=Olivia")
                    .component("lastNames",
                            "chain://list?__version=1&values[0]=manson&values[1]=Sophia&values[2]=jacob")
                    .component("concat", "chain://concat?__version=1")
                    .component("outFile", "chain://file?__version=1&file=" + encode(file.getAbsolutePath(), "utf-8"))
                    .connections()
                    .from("firstNames")
                    .to("concat", "str1")
                    .from("lastNames")
                    .to("concat", "str2")
                    .from("concat")
                    .to("outFile")
                    .build()
                    .run();

            assertTrue(file.isFile());
            assertEquals(asList("noha manson", "Emma Sophia", "liam jacob", "Olivia null"),
                    Files.readAllLines(file.toPath()));
        }
    }

    @Test
    void complex(final TestInfo info, final TemporaryFolder temporaryFolder) throws IOException {
        final String testName = info.getTestMethod().get().getName();
        final String plugin = testName + ".jar";
        final File jar = pluginGenerator.createChainPlugin(temporaryFolder.getRoot(), plugin);
        final File out = new File(temporaryFolder.getRoot(), testName + "-out.txt");

        try (final ComponentManager manager =
                new ComponentManager(new File("target/fake-m2"), "TALEND-INF/dependencies.txt", null) {

                    {
                        CONTEXTUAL_INSTANCE.set(this);
                        addPlugin(jar.getAbsolutePath());
                    }

                    @Override
                    public void close() {
                        super.close();
                        CONTEXTUAL_INSTANCE.set(null);
                    }
                }) {

            Job
                    .components()
                    .component("formatter", "chain://formatter?__version=1")
                    .component("concat", "chain://concat?__version=1")
                    .component("concat_2", "chain://concat?__version=1")
                    .component("firstNames",
                            "chain://list?__version=1" + "&values[0]=noha" + "&values[1]=Emma" + "&values[2]=liam"
                                    + "&values[3]=Olivia")
                    .component("lastNames",
                            "chain://list?__version=1" + "&values[0]=manson" + "&values[1]=Sophia" + "&values[2]=jacob")
                    .component("address",
                            "chain://list?__version=1" + "&values[0]=Paris" + "&values[1]=Strasbourg"
                                    + "&values[2]=Bordeaux" + "&values[3]=Nantes")
                    .component("outFile", "chain://file?__version=1&file=" + encode(out.getAbsolutePath(), "utf-8"))
                    .connections()
                    .from("firstNames")
                    .to("formatter", "firstName")
                    .from("lastNames")
                    .to("formatter", "lastName")
                    .from("formatter", "lastName")
                    .to("concat", "str1")
                    .from("formatter", "firstName")
                    .to("concat", "str2")
                    .from("concat")
                    .to("concat_2", "str1")
                    .from("address")
                    .to("concat_2", "str2")
                    .from("concat_2")
                    .to("outFile")
                    .build()
                    .run();

            assertTrue(out.isFile());
            assertEquals(
                    asList("MANSON noha Paris", "SOPHIA emma Strasbourg", "JACOB liam Bordeaux", "null olivia Nantes"),
                    Files.readAllLines(out.toPath()));
        }
    }
}
