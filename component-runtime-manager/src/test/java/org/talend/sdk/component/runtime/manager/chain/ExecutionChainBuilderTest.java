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

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.talend.sdk.component.junit.base.junit5.TemporaryFolder;
import org.talend.sdk.component.junit.base.junit5.WithTemporaryFolder;
import org.talend.sdk.component.runtime.manager.ComponentManager;
import org.talend.sdk.component.runtime.manager.asm.PluginGenerator;

@WithTemporaryFolder
class ExecutionChainBuilderTest {

    private final PluginGenerator pluginGenerator = new PluginGenerator();

    @Test
    void chain(final TestInfo info, final TemporaryFolder temporaryFolder) throws IOException {
        final String testName = info.getTestMethod().get().getName();
        final String plugin = testName + ".jar";
        final File jar = pluginGenerator.createChainPlugin(temporaryFolder.getRoot(), plugin);

        final Collection<String> tracker = new ArrayList<>();
        final File output = new File(temporaryFolder.getRoot(), testName + "-out.txt");

        // simulate a fatjar classloader for test context
        try (final ComponentManager manager =
                new ComponentManager(new File("target/fake-m2"), "TALEND-INF/dependencies.txt", null)) {
            manager.addPlugin(jar.getAbsolutePath()); // avoid to have to build a plugin index file and nested repo

            ExecutionChainBuilder
                    .start()
                    .withConfiguration(testName, false)
                    .fromInput("chain", "list", 1, new HashMap<String, String>() {

                        {
                            put("values[0]", "a");
                            put("values[1]", "bb");
                            put("values[2]", "ccc");
                        }
                    })
                    .toProcessor("chain", "count", 1, new HashMap<>() /* processor config */)
                    .toProcessor(null, "chain", "file", 1, new HashMap<String, String>() {

                        {
                            put("file", output.getAbsolutePath());
                        }
                    })
                    .create(manager, pluginName -> {
                        throw new IllegalArgumentException("Can't load plugin '" + pluginName
                                + "', ensure you bundled correctly your application.");
                    }, data -> tracker.add("data >> " + data), (data, exception) -> {
                        exception.printStackTrace();
                        tracker.add("error >> " + data + " >> " + exception.getMessage());
                        return ExecutionChain.Skip.INSTANCE;
                    })
                    .get()
                    .execute();
        }
        assertTrue(output.isFile());
        assertEquals(asList("1", "3", "6"), Files.readAllLines(output.toPath()));
        assertEquals(asList("data >> 1", "data >> 3", "data >> 6"), tracker);
    }

    @Test
    void multipleOutputs(final TestInfo info, final TemporaryFolder temporaryFolder) throws IOException {
        final String testName = info.getTestMethod().get().getName();
        final String plugin = info.getTestMethod().get().getName() + ".jar";
        final File jar = pluginGenerator.createChainPlugin(temporaryFolder.getRoot(), plugin);

        final Collection<String> tracker = new ArrayList<>();
        final File output = new File(temporaryFolder.getRoot(), testName + "-out.txt");
        final File rejectOutput = new File(temporaryFolder.getRoot(), testName + "-out_reject.txt");

        // simulate a fatjar classloader for test context
        try (final ComponentManager manager =
                new ComponentManager(new File("target/fake-m2"), "TALEND-INF/dependencies.txt", null)) {
            manager.addPlugin(jar.getAbsolutePath()); // avoid to have to build a plugin index file and nested repo

            ExecutionChainBuilder
                    .start()
                    .withConfiguration("chain", false)
                    .fromInput("chain", "list", 1, new HashMap<String, String>() {

                        {
                            put("values[0]", "a");
                            put("values[1]", "bb");
                            put("values[2]", "reject_1");
                            put("values[3]", "ccc");
                            put("values[4]", "reject_2");
                        }
                    })
                    .toProcessor("chain", "count", 1, new HashMap<>() /* processor config */)
                    .toProcessor("rejected", "chain", "reject", 1, new HashMap<String, String>() {

                        {
                            put("file", rejectOutput.getAbsolutePath());
                        }
                    })
                    .getParent()
                    .toProcessor(null, "chain", "file", 1, new HashMap<String, String>() {

                        {
                            put("file", output.getAbsolutePath());
                        }
                    })
                    .create(manager, pluginName -> {
                        assertEquals("multipleOutputs.jar", plugin);
                        return null;
                    }, data -> tracker.add("data >> " + data), (data, exception) -> {
                        exception.printStackTrace();
                        tracker.add("error >> " + data + " >> " + exception.getMessage());
                        return ExecutionChain.Skip.INSTANCE;
                    })
                    .get()
                    .execute();
        }
        assertTrue(output.isFile());
        assertEquals(asList("1", "3", "6"), Files.readAllLines(output.toPath()));
        assertEquals(asList("reject_1", "reject_2"), Files.readAllLines(rejectOutput.toPath()));
        assertEquals(asList("data >> 1", "data >> 3", "data >> reject_1", "data >> 6", "data >> reject_2"), tracker);
    }
}
