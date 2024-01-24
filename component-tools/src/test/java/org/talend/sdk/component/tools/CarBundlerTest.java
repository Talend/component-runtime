/**
 * Copyright (C) 2006-2024 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.tools;

import static java.util.Arrays.asList;
import static java.util.Collections.list;
import static java.util.Collections.singletonMap;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import org.apache.ziplock.IO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import com.sun.net.httpserver.HttpServer;

import lombok.extern.slf4j.Slf4j;

@Slf4j
class CarBundlerTest {

    @Test
    void bundleWithExistingSameComponent(@TempDir final File temporaryFolder) throws Exception {
        final CarBundler.Configuration configuration = prepareBundle(temporaryFolder);

        // try to execute the main now in a fake studio
        final File fakeStudio = temporaryFolder;
        final File fakeConfig = new File(fakeStudio, "configuration/config.ini");
        fakeConfig.getParentFile().mkdirs();
        try (final Writer writer = new FileWriter(fakeConfig)) {
            writer.write("component.java.coordinates = foo.bar:dummy:1.2");
        }
        final File fakeM2 = new File(fakeStudio, "configuration/.m2/repository/");
        fakeM2.mkdirs();
        assertEquals(0,
                new ProcessBuilder(
                        new File(System.getProperty("java.home"),
                                "/bin/java" + (OS.WINDOWS.isCurrentOs() ? ".exe" : "")).getAbsolutePath(),
                        "-jar", configuration.getOutput().getAbsolutePath(), "studio-deploy",
                        fakeStudio.getAbsolutePath()).inheritIO().start().waitFor());

        assertEquals("component.java.coordinates = foo.bar:dummy:1.2",
                Files.readAllLines(fakeConfig.toPath()).stream().collect(joining("\n")).trim());
    }

    @Test
    void bundleWithExistingOtherComponent(@TempDir final File temporaryFolder) throws Exception {
        final CarBundler.Configuration configuration = prepareBundle(temporaryFolder);

        // try to execute the main now in a fake studio
        final File fakeStudio = temporaryFolder;
        final File fakeConfig = new File(fakeStudio, "configuration/config.ini");
        fakeConfig.getParentFile().mkdirs();
        try (final Writer writer = new FileWriter(fakeConfig)) {
            writer.write("component.java.coordinates = a.bar:dummy:1.3,a.bar:h:2.2");
        }
        final File fakeM2 = new File(fakeStudio, "configuration/.m2/repository/");
        fakeM2.mkdirs();
        assertEquals(0,
                new ProcessBuilder(
                        new File(System.getProperty("java.home"),
                                "/bin/java" + (OS.WINDOWS.isCurrentOs() ? ".exe" : "")).getAbsolutePath(),
                        "-jar", configuration.getOutput().getAbsolutePath(), "studio-deploy",
                        fakeStudio.getAbsolutePath()).inheritIO().start().waitFor());

        assertEquals("component.java.coordinates = foo.bar:dummy:1.2,a.bar:dummy:1.3,a.bar:h:2.2",
                String.join("\n", Files.readAllLines(fakeConfig.toPath())).trim());
    }

    @Test
    void bundleWithExistingSameComponentOtherVersion(@TempDir final File temporaryFolder) throws Exception {
        final CarBundler.Configuration configuration = prepareBundle(temporaryFolder);

        // try to execute the main now in a fake studio
        final File fakeStudio = temporaryFolder;
        final File fakeConfig = new File(fakeStudio, "configuration/config.ini");
        fakeConfig.getParentFile().mkdirs();
        try (final Writer writer = new FileWriter(fakeConfig)) {
            writer.write("component.java.coordinates = foo.bar:dummy:1.1");
        }
        final File fakeM2 = new File(fakeStudio, "configuration/.m2/repository/");
        fakeM2.mkdirs();
        assertEquals(0,
                new ProcessBuilder(
                        new File(System.getProperty("java.home"),
                                "/bin/java" + (OS.WINDOWS.isCurrentOs() ? ".exe" : "")).getAbsolutePath(),
                        "-jar", configuration.getOutput().getAbsolutePath(), "studio-deploy",
                        fakeStudio.getAbsolutePath()).inheritIO().start().waitFor());

        assertEquals("component.java.coordinates = foo.bar:dummy:1.2",
                String.join("\n", Files.readAllLines(fakeConfig.toPath())).trim());
    }

    @Test
    void bundleWithExistingSameComponentOtherVersionAndOtherComponents(@TempDir final File temporaryFolder)
            throws Exception {
        final CarBundler.Configuration configuration = prepareBundle(temporaryFolder);

        // try to execute the main now in a fake studio
        final File fakeStudio = temporaryFolder;
        final File fakeConfig = new File(fakeStudio, "configuration/config.ini");
        fakeConfig.getParentFile().mkdirs();
        try (final Writer writer = new FileWriter(fakeConfig)) {
            writer.write("component.java.coordinates = a:b:1,foo.bar:dummy:1.1,d:e:3");
        }
        final File fakeM2 = new File(fakeStudio, "configuration/.m2/repository/");
        fakeM2.mkdirs();
        assertEquals(0,
                new ProcessBuilder(
                        new File(System.getProperty("java.home"),
                                "/bin/java" + (OS.WINDOWS.isCurrentOs() ? ".exe" : "")).getAbsolutePath(),
                        "-jar", configuration.getOutput().getAbsolutePath(), "studio-deploy",
                        fakeStudio.getAbsolutePath()).inheritIO().start().waitFor());

        assertEquals("component.java.coordinates = foo.bar:dummy:1.2,a:b:1,d:e:3",
                String.join("\n", Files.readAllLines(fakeConfig.toPath())).trim());
    }

    @Test
    void bundle(@TempDir final File temporaryFolder) throws Exception {
        final CarBundler.Configuration configuration = prepareBundle(temporaryFolder);

        // try to execute the main now in a fake studio
        final File fakeStudio = temporaryFolder;
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

    @Test
    void uploadToNexusV2(@TempDir final File temporaryFolder)
            throws IOException, NoSuchMethodException, InterruptedException {
        final String repoName = "releases";
        final String pathToJar = "foo/bar/dummy/1.2/dummy-1.2.jar";
        final File m2 = temporaryFolder;
        final File dep = createTempJar(m2, pathToJar);
        final byte[] expected;
        try (final InputStream in = new FileInputStream(dep)) {
            expected = IO.readBytes(in);
        }

        final CarBundler.Configuration configuration = createConfiguration(temporaryFolder, dep);

        final AtomicInteger serverCalls = new AtomicInteger(0);
        HttpServer server = createTestServerV2(serverCalls, expected, repoName, pathToJar);
        try {
            server.start();
            assertEquals(0, new ProcessBuilder(
                    new File(System.getProperty("java.home"), "/bin/java" + (OS.WINDOWS.isCurrentOs() ? ".exe" : ""))
                            .getAbsolutePath(),
                    "-jar", configuration.getOutput().getAbsolutePath(), "deploy-to-nexus", "--url",
                    "http://localhost:" + server.getAddress().getPort() + "/nexus", "--repo", repoName, "--user",
                    "admin", "--pass", "admin123", "--threads", "1", "--dir", m2.getAbsolutePath())
                            .inheritIO()
                            .start()
                            .waitFor());
        } finally {
            server.stop(0);
        }
        assertEquals(3, serverCalls.intValue());
    }

    @Test
    void uploadToNexusV3(@TempDir final File temporaryFolder)
            throws IOException, InterruptedException, NoSuchMethodException {
        final String repoName = "releases";
        final String pathToJar = "foo/bar/dummy/1.2/dummy-1.2.jar";
        final File m2 = temporaryFolder;
        final File dep = createTempJar(m2, pathToJar);
        final byte[] expected;
        try (final InputStream in = new FileInputStream(dep)) {
            expected = IO.readBytes(in);
        }

        final CarBundler.Configuration configuration = createConfiguration(temporaryFolder, dep);

        final AtomicInteger serverCalls = new AtomicInteger(0);
        HttpServer server = createTestServerV3(serverCalls, expected, repoName, pathToJar);
        try {
            server.start();
            assertEquals(0, new ProcessBuilder(
                    new File(System.getProperty("java.home"), "/bin/java" + (OS.WINDOWS.isCurrentOs() ? ".exe" : ""))
                            .getAbsolutePath(),
                    "-jar", configuration.getOutput().getAbsolutePath(), "deploy-to-nexus", "--url",
                    "http://localhost:" + server.getAddress().getPort(), "--repo", repoName, "--user", "admin",
                    "--pass", "admin123", "--threads", "1", "--dir", m2.getAbsolutePath())
                            .inheritIO()
                            .start()
                            .waitFor());
        } finally {
            server.stop(0);
        }
        assertEquals(3, serverCalls.intValue());
    }

    private CarBundler.Configuration prepareBundle(final File m2) throws IOException, NoSuchMethodException {
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
        configuration.setOutput(new File(m2, "output.jar"));
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
        return configuration;
    }

    private File createTempJar(final File m2, final String pathToJar) throws IOException {
        final File dep = new File(m2, pathToJar);
        dep.getParentFile().mkdirs();
        try (final JarOutputStream jos = new JarOutputStream(new FileOutputStream(dep))) {
            jos.putNextEntry(new JarEntry("test.txt"));
            jos.write("test".getBytes(StandardCharsets.UTF_8));
            jos.closeEntry();
        }

        return dep;
    }

    private CarBundler.Configuration createConfiguration(final File temporaryFolder, final File dep)
            throws NoSuchMethodException {
        final CarBundler.Configuration configuration = new CarBundler.Configuration();
        configuration.setVersion("1.2.3");
        configuration.setArtifacts(singletonMap("foo.bar:dummy:1.2", dep));
        configuration.setOutput(new File(temporaryFolder, "output.jar"));
        configuration.setMainGav("foo.bar:dummy:1.2");
        new CarBundler(configuration, new ReflectiveLog(log)).run();
        assertTrue(configuration.getOutput().exists());
        return configuration;
    }

    private HttpServer createTestServerV3(final AtomicInteger serverCalls, final byte[] expected, final String repoName,
            final String pathToJar) throws IOException {
        final HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/service/rest/beta/repositories").setHandler(httpExchange -> {
            httpExchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, 0);
            httpExchange.close();
            serverCalls.incrementAndGet();
        });
        server.createContext("/repository/" + repoName + "/" + pathToJar).setHandler(httpExchange -> {
            // we say that we don't have the jar. We need to receive it.
            if (httpExchange.getRequestMethod().equalsIgnoreCase("GET")) {
                httpExchange.sendResponseHeaders(HttpURLConnection.HTTP_NOT_FOUND, 0);
            } else if (httpExchange.getRequestMethod().equalsIgnoreCase("PUT")) {
                final byte[] bytes;
                try (final InputStream in = httpExchange.getRequestBody()) {
                    bytes = IO.readBytes(in);
                }
                assertArrayEquals(expected, bytes);
                httpExchange.sendResponseHeaders(HttpURLConnection.HTTP_CREATED, 0);
            }
            httpExchange.close();
            serverCalls.incrementAndGet();
        });
        return server;
    }

    private HttpServer createTestServerV2(final AtomicInteger serverCalls, final byte[] expected, final String repoName,
            final String pathToJar) throws IOException {
        final HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/nexus/service/local/status").setHandler(httpExchange -> {
            final byte[] bytes = getStatusResponseV2().getBytes();
            httpExchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, bytes.length);
            httpExchange.getResponseBody().write(bytes);
            httpExchange.close();
            serverCalls.incrementAndGet();
        });
        server.createContext("/nexus/content/repositories/" + repoName + "/" + pathToJar).setHandler(httpExchange -> {
            // we say that we don't have the jar. We need to receive it.
            if (httpExchange.getRequestMethod().equalsIgnoreCase("GET")) {
                httpExchange.sendResponseHeaders(HttpURLConnection.HTTP_NOT_FOUND, 0);
            } else if (httpExchange.getRequestMethod().equalsIgnoreCase("POST")) {
                final byte[] bytes;
                try (final InputStream in = httpExchange.getRequestBody()) {
                    bytes = IO.readBytes(in);
                }
                assertArrayEquals(expected, bytes);
                httpExchange.sendResponseHeaders(HttpURLConnection.HTTP_CREATED, 0);
            }
            httpExchange.close();
            serverCalls.incrementAndGet();
        });
        return server;
    }

    private String getStatusResponseV2() {
        return "{\"data\":{\"appName\":\"Nexus Repository Manager\",\"formattedAppName\":\"Nexus Repository Manager OSS 2.14.8-01\","
                + "\"version\":\"2.14.8-01\",\"apiVersion\":\"2.14.8-01\",\"editionLong\":\"\","
                + "\"editionShort\":\"OSS\",\"attributionsURL\":\"http://links.sonatype.com/products/nexus/oss/attributions\","
                + "\"purchaseURL\":\"http://links.sonatype.com/products/nexus/oss/store\","
                + "\"userLicenseURL\":\"http://links.sonatype.com/products/nexus/oss/EULA\","
                + "\"state\":\"STARTED\",\"initializedAt\":\"2018-05-02 05:44:04.337 UTC\","
                + "\"startedAt\":\"2018-05-02 05:44:05.951 UTC\","
                + "\"lastConfigChange\":\"2018-05-02 05:44:05.951 UTC\",\"firstStart\":false,\"instanceUpgraded\":false,"
                + "\"configurationUpgraded\":false,\"baseUrl\":\"http://localhost:8081/nexus\",\"licenseInstalled\":false,"
                + "\"licenseExpired\":false,\"trialLicense\":false}}";
    }
}
