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
package org.talend.components.beam.it;

import static java.util.stream.Collectors.joining;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.rules.RuleChain.outerRule;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestRule;
import org.talend.components.beam.it.clusterserialization.Main;
import org.talend.components.runtime.testing.spark.SparkClusterRule;

public class SerializationOverClusterIT {

    private static final TemporaryFolder TEMPORARY_FOLDER = new TemporaryFolder();

    private static final SparkClusterRule SPARK = new SparkClusterRule("2.10", "1.6.3", 1);

    @ClassRule
    public static final TestRule RULE = outerRule(TEMPORARY_FOLDER).around(SPARK);

    @Test
    public void run() throws IOException {
        final File input = new File(TEMPORARY_FOLDER.getRoot(), Main.class.getName() + ".input");
        final File output = new File(TEMPORARY_FOLDER.getRoot(), Main.class.getName() + ".output");
        SPARK.submit(Main.class, "--runner=SparkRunner", "--inputFile=" + input.getAbsolutePath(),
                "--outputFile=" + output.getAbsolutePath());
        waitAndAssert(() -> {
            // not sexy but beam TextIO adds a prefix/suffix to handle bundles
            final File[] outputs = output.getParentFile().listFiles((dir, name) -> name.startsWith(output.getName()));
            assertTrue("output doesn't exist", outputs != null && outputs.length == 1/* for out test */);
            try {
                assertEquals("User{id='a6normal', name='normal'}\nUser{id='a36marilyn', name='marilyn'}",
                        Files.lines(outputs[0].toPath()).collect(joining("\n")));
            } catch (final IOException e) {
                fail(e.getMessage());
            }
        });
    }

    private static void waitAndAssert(final Runnable task) { // awaitability is nice but has a ***** of stack
        final int maxRetries = 100;
        for (int i = 0; i < maxRetries; i++) {
            try {
                task.run();
                return;
            } catch (final AssertionError ae) {
                if (maxRetries - 1 == i) {
                    throw ae;
                }
                try {
                    Thread.sleep(500);
                } catch (final InterruptedException e) {
                    fail(e.getMessage());
                }
            }
        }
        fail();
    }
}
