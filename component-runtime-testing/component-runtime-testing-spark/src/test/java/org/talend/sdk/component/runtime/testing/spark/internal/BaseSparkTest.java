/**
 * Copyright (C) 2006-2026 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.runtime.testing.spark.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class BaseSparkTest {

    @Rule
    public final TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void removesOnlyStaleLog4j2Jars() throws IOException {
        final File lib = folder.newFolder("lib");
        for (final String name : Arrays
                .asList("log4j-api-2.20.0.jar", "log4j-core-2.20.0.jar", "log4j-slf4j-impl-2.20.0.jar",
                        "log4j-api-2.25.5.jar", "log4j-core-2.25.5.jar", "log4j-slf4j-impl-2.25.5.jar",
                        "spark-core_2.12-3.5.9.jar", "log4j-web-2.25.5.jar")) {
            assertTrue(new File(lib, name).createNewFile());
        }

        BaseSpark.removeStaleLog4j2Jars(lib, "2.25.5");

        final Set<String> remaining = new HashSet<>(Arrays.asList(lib.list()));
        assertEquals(new HashSet<>(Arrays
                .asList("log4j-api-2.25.5.jar", "log4j-core-2.25.5.jar", "log4j-slf4j-impl-2.25.5.jar",
                        "spark-core_2.12-3.5.9.jar", "log4j-web-2.25.5.jar")),
                remaining);
    }

    @Test
    public void ignoresMissingFolder() throws IOException {
        final File missing = new File(folder.getRoot(), "missing");
        BaseSpark.removeStaleLog4j2Jars(missing, "2.25.5");
        assertFalse(missing.exists());
    }
}
