/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.talend.sdk.component.tools;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.talend.sdk.component.junit.base.junit5.TemporaryFolder;
import org.talend.sdk.component.junit.base.junit5.WithTemporaryFolder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@WithTemporaryFolder
public class StudioInstallerTest {

    @Test
    void run(final TemporaryFolder temporaryFolder, final TestInfo info) throws IOException {
        final String testName = info.getTestMethod().get().getName();
        final File studioHome = new File(temporaryFolder.getRoot(), testName);
        final File configuration = org.apache.ziplock.Files.mkdir(new File(studioHome, "configuration"));
        try (final Writer configIni = new FileWriter(new File(configuration, "config.ini"))) {
            // no-op
        }

        final File artifact = new File(temporaryFolder.getRoot(), testName + ".jar");
        try (final JarOutputStream out = new JarOutputStream(new FileOutputStream(artifact))) {
            out.putNextEntry(new JarEntry("META-INF/MANIFEST.MF"));
            out.closeEntry();
        }
        final StudioInstaller installer = new StudioInstaller("gtest:atest:1.0-SNAPSHOT", studioHome,
                singletonMap("gtest:atest:1.0-SNAPSHOT", artifact), new Log() {

                    @Override
                    public void debug(final String s) {
                        log.info(s);
                    }

                    @Override
                    public void error(final String s) {
                        log.error(s);
                    }

                    @Override
                    public void info(final String s) {
                        log.info(s);
                    }
                });
        installer.run();

        final File backup = new File(studioHome, "configuration/backup");

        {
            assertTrue(backup.exists());
            assertEquals(1, backup.listFiles((dir, name) -> name.startsWith("config.ini")).length);

            assertSetup(studioHome);
        }

        installer.run();
        {
            assertSetup(studioHome);
            // 1 again cause already here so no other backup
            assertEquals(1, backup.listFiles((dir, name) -> name.startsWith("config.ini")).length);
        }
    }

    private void assertSetup(File studioHome) throws IOException {
        final File registration = new File(studioHome, "configuration/components-registration.properties");
        assertTrue(registration.exists());
        assertEquals(singleton("atest=gtest\\:atest\\:1.0-SNAPSHOT"),
                Files.readAllLines(registration.toPath()).stream().filter(l -> !l.startsWith("#")).collect(toSet()));

        final File configIni = new File(studioHome, "configuration/config.ini");
        assertEquals(
                singleton("component.java.registry="
                        + registration.getAbsolutePath().replace("\\", "\\\\").replace(":", "\\:")),
                Files.readAllLines(configIni.toPath()).stream().filter(l -> !l.startsWith("#")).collect(toSet()));

        assertTrue(new File(studioHome, "configuration/.m2/repository/gtest/atest/1.0-SNAPSHOT/atest-1.0-SNAPSHOT.jar")
                .exists());
    }
}
