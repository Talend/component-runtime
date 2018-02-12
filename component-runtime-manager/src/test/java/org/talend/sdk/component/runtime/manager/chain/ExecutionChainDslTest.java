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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.talend.sdk.component.junit.base.junit5.TemporaryFolder;
import org.talend.sdk.component.junit.base.junit5.WithTemporaryFolder;
import org.talend.sdk.component.runtime.manager.ComponentManager;
import org.talend.sdk.component.runtime.manager.asm.PluginGenerator;

@WithTemporaryFolder
class ExecutionChainDslTest implements ExecutionChainDsl {

    private final PluginGenerator pluginGenerator = new PluginGenerator();

    @Test
    void chain(final TestInfo info, final TemporaryFolder temporaryFolder) throws IOException {
        final String testName = info.getTestMethod().get().getName();
        final String plugin = testName + ".jar";
        final File jar = pluginGenerator.createChainPlugin(temporaryFolder.getRoot(), plugin);

        final Collection<String> tracker = new ArrayList<>();
        final File output = new File(temporaryFolder.getRoot(), testName + "-out.txt");

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
            doJob(testName, tracker, output);
        }
        assertTrue(output.isFile());
        assertEquals(asList("1", "3", "6"), Files.readAllLines(output.toPath()));
        assertEquals(asList("data >> 1", "data >> 3", "data >> 6"), tracker);
    }

    private void doJob(final String testName, final Collection<String> tracker, final File output) {
        from("chain://list?__version=1&values[0]=a&values[1]=bb&values[2]=ccc")
                .configure(new ChainConfiguration(testName, true, data -> tracker.add("data >> " + data),
                        (data, exception) -> {
                            exception.printStackTrace();
                            tracker.add("error >> " + data + " >> " + exception.getMessage());
                            return ExecutionChain.Skip.INSTANCE;
                        }))
                .to("chain://count?__version=1")
                .to("chain://file?__version=1&file=" + output.getAbsolutePath())
                .create()
                .execute();
    }
}
