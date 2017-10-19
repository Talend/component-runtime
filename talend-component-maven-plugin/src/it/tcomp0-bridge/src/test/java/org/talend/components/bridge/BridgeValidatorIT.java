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
package org.talend.components.bridge;

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
