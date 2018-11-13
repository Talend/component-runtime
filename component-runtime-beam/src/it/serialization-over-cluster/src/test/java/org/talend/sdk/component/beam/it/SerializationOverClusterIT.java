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
package org.talend.sdk.component.beam.it;

import static java.util.stream.Collectors.joining;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.rules.RuleChain.outerRule;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.stream.Stream;

import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestRule;
import org.talend.sdk.component.beam.it.clusterserialization.Main;
import org.talend.sdk.component.runtime.testing.spark.SparkClusterRule;

public class SerializationOverClusterIT {

    private static final TemporaryFolder TEMPORARY_FOLDER = new TemporaryFolder();

    private static final SparkClusterRule SPARK = new SparkClusterRule("2.11", "2.4.0", 1);

    @ClassRule
    public static final TestRule RULE = outerRule(TEMPORARY_FOLDER).around(SPARK);

    @Test
    public void run() {
        final File input = new File(TEMPORARY_FOLDER.getRoot(), Main.class.getName() + ".input");
        final File output = new File(TEMPORARY_FOLDER.getRoot(), Main.class.getName() + ".output");
        SPARK.submit(Main.class, "--runner=SparkRunner", "--inputFile=" + input.getAbsolutePath(),
                "--outputFile=" + output.getAbsolutePath());
        waitAndAssert(() -> {
            // not sexy but beam TextIO adds a prefix/suffix to handle bundles
            final File[] outputs = output.getParentFile().listFiles((dir, name) -> name.startsWith(output.getName()));
            assertTrue("output doesn't exist", outputs != null && outputs.length >= 1/* for out test */);
            assertEquals("{\"id\":\"a6normal\",\"name\":\"normal\"}\n{\"id\":\"a36marilyn\",\"name\":\"marilyn\"}", Stream.of(outputs).map(f -> {
                try {
                    return Files.lines(f.toPath()).collect(joining("\n"));
                } catch (final IOException e) {
                    throw new IllegalStateException(e);
                }
            }).collect(joining("\n")).trim());
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
