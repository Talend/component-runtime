/**
 *  Copyright (C) 2006-2018 Talend Inc. - www.talend.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.talend.sdk.component.bridge;

import static org.apache.ziplock.JarLocation.jarLocation;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

import org.junit.Test;

public class BridgeValidatorIT {

    @Test
    public void checkModulesExist() throws IOException {
        final File root = jarLocation(BridgeValidatorIT.class).getParentFile()
                .listFiles((FilenameFilter) (d, n) -> "oldtcomponentbridgemojo_workdir".equals(n))[0]
                        .listFiles((FilenameFilter) (d, n) -> "test".equals(n))[0];

        {
            final File definition = root.listFiles((dir, name) -> name.equals("test-definition"))[0];
            assertTrue(definition.getAbsolutePath(), definition.isDirectory());
            // ensure it was compiled
            final File[] outputs = new File(definition, "target")
                    .listFiles((dir, name) -> name.startsWith("test-definition-") && name.endsWith(".jar"));
            assertNotNull(outputs);
            assertEquals(2 /* normal and source */, outputs.length);
            // TODO validate
        }

        {
            final File runtime = root.listFiles((dir, name) -> name.equals("test-runtime"))[0];
            assertTrue(runtime.getAbsolutePath(), runtime.isDirectory());
            // ensure it was compiled
            final File[] outputs = new File(runtime, "target")
                    .listFiles((dir, name) -> name.startsWith("test-runtime-") && name.endsWith(".jar"));
            assertNotNull(outputs);
            assertEquals(2 /* normal and source */, outputs.length);
        }
    }
}
