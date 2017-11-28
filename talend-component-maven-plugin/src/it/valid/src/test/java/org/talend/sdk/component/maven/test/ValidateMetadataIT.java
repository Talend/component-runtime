/**
 * Copyright (C) 2006-2017 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.maven.test;

import static org.apache.ziplock.IO.slurp;
import static org.apache.ziplock.JarLocation.jarLocation;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import org.junit.Test;

public class ValidateMetadataIT {

    @Test
    public void checkExists() throws IOException {
        final File jar = jarLocation(ValidateMetadataIT.class).getParentFile().listFiles(
                (FilenameFilter) (dir, name) -> name.startsWith("sample-") && name.endsWith(".jar"))[0];
        assertTrue(jar.getAbsolutePath(), jar.getName().endsWith(".jar")); // ensure we don't run from target/classes

        try (final JarFile jf = new JarFile(jar)) {
            final ZipEntry entry = jf.getEntry("TALEND-INF/components.json");
            assertNotNull(entry);

            try (final InputStream content = jf.getInputStream(entry)) {
                final String val = slurp(content);
                System.out.println();
                System.out.println();
                System.out.println(val); // intended since a change in meta goal will affect this test so easier to
                                         // iterate
                System.out.println();
                System.out.println();
                assertEquals("{\"components\":[{\"categories\":[\"Misc\"],\"displayName\":\"Component Test my\","
                        + "\"family\":\"test\",\"icon\":\"file-job-o\",\"inputs\":[\"__default__\"],\"name\":\"my\","
                        + "\"outputs\":[\"__default__\"]}]}", val);
            }
        }
    }
}
