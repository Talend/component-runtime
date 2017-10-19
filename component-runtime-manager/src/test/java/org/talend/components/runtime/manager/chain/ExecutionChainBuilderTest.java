// ============================================================================
//
// Copyright (C) 2006-2017 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.components.runtime.manager.chain;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;
import org.talend.components.runtime.manager.ComponentManager;
import org.talend.components.runtime.manager.asm.PluginGenerator;

public class ExecutionChainBuilderTest {

    @ClassRule
    public static final TemporaryFolder TEMPORARY_FOLDER = new TemporaryFolder();

    @Rule
    public final TestName testName = new TestName();

    private final PluginGenerator pluginGenerator = new PluginGenerator();

    @Test
    public void chain() throws IOException {
        final String plugin = testName.getMethodName() + ".jar";
        final File jar = pluginGenerator.createChainPlugin(TEMPORARY_FOLDER.getRoot(), plugin);

        final Collection<String> tracker = new ArrayList<>();
        final File output = new File(TEMPORARY_FOLDER.getRoot(), testName.getMethodName() + "-out.txt");

        // simulate a fatjar classloader for test context
        try (final ComponentManager manager = new ComponentManager(new File("target/fake-m2"), "TALEND-INF/dependencies.txt",
                null)) {
            manager.addPlugin(jar.getAbsolutePath()); // avoid to have to build a plugin index file and nested repo

            ExecutionChainBuilder.start().withConfiguration(testName.getMethodName(), false)
                    .fromInput("chain", "list", 1, new HashMap<String, String>() {

                        {
                            put("values[0]", "a");
                            put("values[1]", "bb");
                            put("values[2]", "ccc");
                        }
                    }).toProcessor("chain", "count", 1, new HashMap<>() /* processor config */)
                    .toProcessor(null, "chain", "file", 1, new HashMap<String, String>() {

                        {
                            put("file", output.getAbsolutePath());
                        }
                    }).create(manager, pluginName -> {
                        throw new IllegalArgumentException(
                                "Can't load plugin '" + pluginName + "', ensure you bundled correctly your application.");
                    }, data -> tracker.add("data >> " + data), (data, exception) -> {
                        exception.printStackTrace();
                        tracker.add("error >> " + data + " >> " + exception.getMessage());
                        return ExecutionChain.Skip.INSTANCE;
                    }).get().execute();
        }
        assertTrue(output.isFile());
        assertEquals(asList("1", "3", "6"), Files.readAllLines(output.toPath()));
        assertEquals(asList("data >> 1", "data >> 3", "data >> 6"), tracker);
    }

    @Test
    public void multipleOutputs() throws IOException {
        final String plugin = testName.getMethodName() + ".jar";
        final File jar = pluginGenerator.createChainPlugin(TEMPORARY_FOLDER.getRoot(), plugin);

        final Collection<String> tracker = new ArrayList<>();
        final File output = new File(TEMPORARY_FOLDER.getRoot(), testName.getMethodName() + "-out.txt");
        final File rejectOutput = new File(TEMPORARY_FOLDER.getRoot(), testName.getMethodName() + "-out_reject.txt");

        // simulate a fatjar classloader for test context
        try (final ComponentManager manager = new ComponentManager(new File("target/fake-m2"), "TALEND-INF/dependencies.txt",
                null)) {
            manager.addPlugin(jar.getAbsolutePath()); // avoid to have to build a plugin index file and nested repo

            ExecutionChainBuilder.start().withConfiguration("chain", false)
                    .fromInput("chain", "list", 1, new HashMap<String, String>() {

                        {
                            put("values[0]", "a");
                            put("values[1]", "bb");
                            put("values[2]", "reject_1");
                            put("values[3]", "ccc");
                            put("values[4]", "reject_2");
                        }
                    }).toProcessor("chain", "count", 1, new HashMap<>() /* processor config */)
                    .toProcessor("rejected", "chain", "reject", 1, new HashMap<String, String>() {

                        {
                            put("file", rejectOutput.getAbsolutePath());
                        }
                    }).getParent().toProcessor(null, "chain", "file", 1, new HashMap<String, String>() {

                        {
                            put("file", output.getAbsolutePath());
                        }
                    }).create(manager, pluginName -> {
                        assertEquals("multipleOutputs.jar", plugin);
                        return null;
                    }, data -> tracker.add("data >> " + data), (data, exception) -> {
                        exception.printStackTrace();
                        tracker.add("error >> " + data + " >> " + exception.getMessage());
                        return ExecutionChain.Skip.INSTANCE;
                    }).get().execute();
        }
        assertTrue(output.isFile());
        assertEquals(asList("1", "3", "6"), Files.readAllLines(output.toPath()));
        assertEquals(asList("reject_1", "reject_2"), Files.readAllLines(rejectOutput.toPath()));
        assertEquals(asList("data >> 1", "data >> 3", "data >> reject_1", "data >> 6", "data >> reject_2"), tracker);
    }
}
