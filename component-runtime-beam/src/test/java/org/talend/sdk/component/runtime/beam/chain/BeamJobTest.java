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
package org.talend.sdk.component.runtime.beam.chain;

import static java.lang.Thread.sleep;
import static java.net.URLEncoder.encode;
import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.io.TempDir;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.runtime.beam.PluginGenerator;
import org.talend.sdk.component.runtime.manager.ComponentManager;
import org.talend.sdk.component.runtime.manager.chain.GroupKeyProvider;
import org.talend.sdk.component.runtime.manager.chain.Job;

import lombok.extern.slf4j.Slf4j;

@Slf4j
class BeamJobTest implements Serializable {

    private transient final PluginGenerator pluginGenerator = new PluginGenerator();

    @Test
    void complex(final TestInfo info, @TempDir final Path temporaryFolder) throws IOException {
        final String testName = info.getTestMethod().get().getName();
        final String plugin = testName + ".jar";
        final File jar = pluginGenerator.createChainPlugin(temporaryFolder.toFile(), plugin);
        final File out = new File(temporaryFolder.toFile(), testName + "-out.txt");

        try (final ComponentManager manager =
                new ComponentManager(new File("target/fake-m2"), "TALEND-INF/dependencies.txt", null) {

                    private final ComponentManager contextualInstance;

                    {
                        contextualInstance = ComponentManager.contextualInstance().get();
                        ComponentManager.contextualInstance().set(this);
                        addPlugin(jar.getAbsolutePath());
                    }

                    @Override
                    public void close() {
                        ComponentManager.contextualInstance().set(contextualInstance);
                        super.close();
                        removePlugin(jar.getAbsolutePath());
                    }
                }) {

            Job
                    .components()
                    .component("formatter", "chain://formatter?__version=1")
                    .component("concat", "chain://concat?__version=1")
                    .component("concat_2", "chain://concat?__version=1")
                    .component("firstNames-dataset",
                            "chain://list?__version=1&values[0]=noha&values[1]=Emma&values[2]=liam&values[3]=Olivia")
                    .component("lastNames-dataset",
                            "chain://list?__version=1&values[0]=manson&values[1]=Sophia&values[2]=jacob")
                    .component("address",
                            "chain://list?__version=1&values[0]=Paris&values[1]=Strasbourg&values[2]=Bordeaux&values[3]=Nantes")
                    .component("outFile", "chain://file?__version=1&file=" + encode(out.getAbsolutePath(), "utf-8"))
                    .connections()
                    .from("firstNames-dataset")
                    .to("formatter", "firstName")
                    .from("lastNames-dataset")
                    .to("formatter", "lastName")
                    .from("formatter", "formatted-lastName")
                    .to("concat", "str1")
                    .from("formatter", "formatted-firstName")
                    .to("concat", "str2")
                    .from("concat")
                    .to("concat_2", "str1")
                    .from("address")
                    .to("concat_2", "str2")
                    .from("concat_2")
                    .to("outFile")
                    .build()
                    .property(GroupKeyProvider.class.getName(), (GroupKeyProvider) this::getKey)
                    .run();

            final int maxRetries = 120;
            for (int i = 0; i < maxRetries; i++) {
                try {
                    assertTrue(out.isFile());
                    assertEquals(Stream
                            .of("MANSON noha Paris", "SOPHIA emma Strasbourg", "JACOB liam Bordeaux",
                                    "null olivia Nantes")
                            .collect(toSet()), new HashSet<>(Files.readAllLines(out.toPath())));
                } catch (final AssertionError ae) {
                    if (i == maxRetries - 1) {
                        throw ae;
                    }
                    try {
                        sleep(500);
                        log.info("Output file is not yet matching the expected output, will retry: " + ae.getMessage());
                    } catch (final InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }

    }

    private String getKey(final GroupKeyProvider.GroupContext context) {
        final Record record = context.getData();
        final Record internals = record.getRecord("__talend_internal");
        return internals.getString("key");
    }
}
