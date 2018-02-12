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

import static java.util.Arrays.asList;
import static java.util.Collections.list;
import static java.util.Collections.singletonMap;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.OS;
import org.talend.sdk.component.junit.base.junit5.TemporaryFolder;
import org.talend.sdk.component.junit.base.junit5.WithTemporaryFolder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@WithTemporaryFolder
class CarBundlerTest {

    @Test
    void bundle(final TemporaryFolder temporaryFolder) throws NoSuchMethodException, IOException, InterruptedException {
        final File m2 = temporaryFolder.newFolder();
        final File dep = new File(m2, "foo/bar/dummy/1.2/dummy-1.2.jar");
        dep.getParentFile().mkdirs();
        try (final JarOutputStream jos = new JarOutputStream(new FileOutputStream(dep))) {
            jos.putNextEntry(new JarEntry("test.txt"));
            jos.write("test".getBytes(StandardCharsets.UTF_8));
            jos.closeEntry();
        }

        final CarBundler.Configuration configuration = new CarBundler.Configuration();
        configuration.setVersion("1.2.3");
        configuration.setArtifacts(singletonMap("foo.bar:dummy:1.2", dep));
        configuration.setOutput(new File(temporaryFolder.getRoot(), "output.jar"));
        configuration.setMainGav("foo.bar:dummy:1.2");
        new CarBundler(configuration, new ReflectiveLog(log)).run();
        assertTrue(configuration.getOutput().exists());
        try (final JarFile jar = new JarFile(configuration.getOutput())) {
            final List<JarEntry> entries =
                    list(jar.entries()).stream().sorted(comparing(ZipEntry::getName)).collect(toList());
            final List<String> paths = entries.stream().map(ZipEntry::getName).collect(toList());
            assertEquals(asList("MAVEN-INF/", "MAVEN-INF/repository/", "MAVEN-INF/repository/foo/",
                    "MAVEN-INF/repository/foo/bar/", "MAVEN-INF/repository/foo/bar/dummy/",
                    "MAVEN-INF/repository/foo/bar/dummy/1.2/", "MAVEN-INF/repository/foo/bar/dummy/1.2/dummy-1.2.jar",
                    "META-INF/", "META-INF/MANIFEST.MF", "TALEND-INF/", "TALEND-INF/metadata.properties", "org/",
                    "org/talend/", "org/talend/sdk/", "org/talend/sdk/component/", "org/talend/sdk/component/tools/",
                    "org/talend/sdk/component/tools/exec/", "org/talend/sdk/component/tools/exec/CarMain.class"),
                    paths);
            entries.stream().filter(e -> e.getName().endsWith("/")).forEach(e -> assertTrue(e.isDirectory()));
            try (final BufferedReader reader = new BufferedReader(
                    new InputStreamReader(jar.getInputStream(jar.getEntry("TALEND-INF/metadata.properties"))))) {

                final String meta = reader.lines().collect(joining("\n"));
                assertTrue(meta.contains("version=1.2.3"));
                assertTrue(meta.contains("date="));
                assertTrue(meta.contains("component_coordinates=foo.bar\\:dummy\\:1.2"));
            }

            try (final JarInputStream depJar = new JarInputStream(
                    jar.getInputStream(jar.getEntry("MAVEN-INF/repository/foo/bar/dummy/1.2/dummy-1.2.jar")))) {

                final JarEntry nextJarEntry = depJar.getNextJarEntry();
                assertNotNull(nextJarEntry);
                assertEquals("test.txt", nextJarEntry.getName());
                assertEquals("test", new BufferedReader(new InputStreamReader(depJar)).lines().collect(joining("\n")));
                assertNull(depJar.getNextJarEntry());
            }
        }

        // try to execute the main now in a fake studio
        final File fakeStudio = temporaryFolder.newFolder();
        final File fakeConfig = new File(fakeStudio, "configuration/config.ini");
        fakeConfig.getParentFile().mkdirs();
        try (final Writer writer = new FileWriter(fakeConfig)) {
            // no-op, just create the file
        }
        final File fakeM2 = new File(fakeStudio, "configuration/.m2/repository/");
        fakeM2.mkdirs();
        assertEquals(0,
                new ProcessBuilder(
                        new File(System.getProperty("java.home"),
                                "/bin/java" + (OS.WINDOWS.isCurrentOs() ? ".exe" : "")).getAbsolutePath(),
                        "-jar", configuration.getOutput().getAbsolutePath(), "studio-deploy",
                        fakeStudio.getAbsolutePath()).inheritIO().start().waitFor());

        // asserts the jar was installed and the component registered
        assertTrue(new File(fakeM2, "foo/bar/dummy/1.2/dummy-1.2.jar").exists());
        assertEquals("component.java.coordinates = foo.bar:dummy:1.2",
                Files.readAllLines(fakeConfig.toPath()).stream().collect(joining("\n")).trim());
    }
}
