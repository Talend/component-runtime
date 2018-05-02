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
package org.talend.sdk.component.tools.exec;

import static java.util.Locale.ROOT;
import static java.util.stream.Collectors.joining;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.stream.Stream;

import javax.xml.bind.DatatypeConverter;

// IMPORTANT: this class MUST not have ANY dependency, not even slf4j!
public class CarMain {

    private CarMain() {
        // no-op
    }

    public static void main(final String[] args) {
        if (args == null || args.length < 2) {
            help();
            return;
        }
        if (Stream.of(args).anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("No argument can be null");
        }
        switch (args[0].toLowerCase(ROOT)) {
        case "studio-deploy":
            deployInStudio(args[1]);
            break;
        case "maven-deploy":
            deployInM2(args[1]);
            break;
        case "deploy-to-nexus":
            if (args.length == 5) {
                deployToNexus(args[1], args[2], args[3], args[4], Runtime.getRuntime().availableProcessors(), null);
            } else if (args.length == 6) {
                deployToNexus(args[1], args[2], args[3], args[4], Integer.parseInt(args[5]), null);
            } else if (args.length == 7) {
                deployToNexus(args[1], args[2], args[3], args[4], Integer.parseInt(args[5]), args[6]);
            }
            break;
        default:
            help();
            throw new IllegalArgumentException("Unknown command '" + args[0] + "'");
        }
    }

    private static void deployInM2(final String m2) {
        final File m2File = new File(m2);
        if (m2File.exists()) {
            throw new IllegalArgumentException(m2 + " doesn't exist");
        }
        final String component = installJars(m2File);
        System.out.println("Installed " + jarLocation(CarMain.class).getName() + " in " + m2 + ", "
                + "you can now register '" + component + "' component in your application.");
    }

    private static File findServerM2(final String serverHome) {
        // talend.component.server.maven.repository
        return null;
    }

    private static void deployInStudio(final String studioLocation) {
        final File root = new File(studioLocation);
        if (!root.isDirectory()) {
            throw new IllegalArgumentException(studioLocation + " is not a valid directory");
        }

        final File m2Root = new File(studioLocation, "configuration/.m2/repository/");
        if (!m2Root.isDirectory()) {
            throw new IllegalArgumentException(m2Root + " does not exist, did you specify a valid studio home?");
        }

        final File config = new File(studioLocation, "configuration/config.ini");
        if (!config.exists()) {
            throw new IllegalArgumentException("No " + config + " found, is your studio location right?");
        }

        // install jars
        final String mainGav = installJars(m2Root);

        // register the component
        final Properties configuration = readProperties(config);
        final String components = configuration.getProperty("component.java.coordinates");
        try {
            final List<String> configLines = Files.readAllLines(config.toPath());
            if (components == null) {
                final String original = configLines.stream().collect(joining("\n"));
                try (final Writer writer = new FileWriter(config)) {
                    writer.write(original + "\ncomponent.java.coordinates = " + mainGav);
                }
            } else {
                // backup
                final File backup = new File(config.getParentFile(), "backup/" + config.getName() + "_"
                        + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-mm-dd_HH-mm-ss")));
                backup.getParentFile().mkdirs();
                Files.copy(config.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING/* unlikely */);

                boolean skip = false;
                try (final Writer writer = new FileWriter(config)) {
                    for (final String line : configLines) {
                        if (line.trim().startsWith("component.java.coordinates")) {
                            skip = true;
                            continue;
                        } else if (skip && line.trim().contains("=")) {
                            skip = false;
                        } else if (skip) {
                            continue;
                        }
                        writer.write(line);
                    }
                    writer.write("component.java.coordinates = "
                            + (components.trim().isEmpty() ? "" : (components + ',')) + mainGav);
                }
            }
        } catch (final IOException ioe) {
            throw new IllegalStateException(ioe);
        }
    }

    private static String installJars(final File m2Root) {
        String mainGav = null;
        try (final JarInputStream jar =
                new JarInputStream(new BufferedInputStream(new FileInputStream(jarLocation(CarMain.class))))) {
            JarEntry entry;
            while ((entry = jar.getNextJarEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                if (entry.getName().startsWith("MAVEN-INF/repository/")) {
                    final String path = entry.getName().substring("MAVEN-INF/repository/".length());
                    final File output = new File(m2Root, path);
                    if (output.exists()) {
                        System.out.println(output + " already exists, skipping");
                    } else {
                        output.getParentFile().mkdirs();
                        Files.copy(jar, output.toPath());
                    }
                } else if ("TALEND-INF/metadata.properties".equals(entry.getName())) {
                    // mainGav
                    final Properties properties = new Properties();
                    properties.load(jar);
                    mainGav = properties.getProperty("component_coordinates");
                }
            }
        } catch (final IOException e) {
            throw new IllegalArgumentException(e);
        }
        if (mainGav == null || mainGav.trim().isEmpty()) {
            throw new IllegalArgumentException("Didn't find the component coordinates");
        }
        return mainGav;
    }

    private static Properties readProperties(final File config) {
        final Properties configuration = new Properties();
        try (final InputStream stream = new FileInputStream(config)) {
            configuration.load(stream);
        } catch (final IOException e) {
            throw new IllegalArgumentException(e);
        }
        return configuration;
    }

    private static void help() {
        System.err.println(
                "Usage:\n\n   java -jar " + jarLocation(CarMain.class).getName() + " studio-deploy /path/to/studio");
        System.err.println("   java -jar " + jarLocation(CarMain.class).getName() + " maven-deploy /path/to/.m2");
        System.err.println("   java -jar " + jarLocation(CarMain.class).getName()
                + " deploy-to-nexus NexusUrl repositoryName username password [parallelThreads [tempDirectory]]");
    }

    private static File jarLocation(final Class clazz) {
        try {
            final String classFileName = clazz.getName().replace(".", "/") + ".class";
            final ClassLoader loader = clazz.getClassLoader();
            return jarFromResource(loader, classFileName);
        } catch (final RuntimeException e) {
            throw e;
        } catch (final Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static File jarFromResource(final ClassLoader loader, final String resourceName) {
        try {
            final URL url = loader.getResource(resourceName);
            if (url == null) {
                throw new IllegalStateException("didn't find " + resourceName);
            }
            if ("jar".equals(url.getProtocol())) {
                final String spec = url.getFile();
                final int separator = spec.indexOf('!');
                return new File(decode(new URL(spec.substring(0, separator)).getFile()));

            } else if ("file".equals(url.getProtocol())) {
                return toFile(resourceName, url);
            } else {
                throw new IllegalArgumentException("Unsupported URL scheme: " + url.toExternalForm());
            }
        } catch (final RuntimeException e) {
            throw e;
        } catch (final Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static File toFile(final String classFileName, final URL url) {
        final String path = url.getFile();
        return new File(decode(path.substring(0, path.length() - classFileName.length())));
    }

    private static String decode(final String fileName) {
        if (fileName.indexOf('%') == -1) {
            return fileName;
        }

        final StringBuilder result = new StringBuilder(fileName.length());
        final ByteArrayOutputStream out = new ByteArrayOutputStream();

        for (int i = 0; i < fileName.length();) {
            final char c = fileName.charAt(i);

            if (c == '%') {
                out.reset();
                do {
                    if (i + 2 >= fileName.length()) {
                        throw new IllegalArgumentException("Incomplete % sequence at: " + i);
                    }

                    final int d1 = Character.digit(fileName.charAt(i + 1), 16);
                    final int d2 = Character.digit(fileName.charAt(i + 2), 16);

                    if (d1 == -1 || d2 == -1) {
                        throw new IllegalArgumentException(
                                "Invalid % sequence (" + fileName.substring(i, i + 3) + ") at: " + String.valueOf(i));
                    }

                    out.write((byte) ((d1 << 4) + d2));

                    i += 3;

                } while (i < fileName.length() && fileName.charAt(i) == '%');

                result.append(out.toString());

                continue;
            } else {
                result.append(c);
            }

            i++;
        }
        return result.toString();
    }

    private static void deployToNexus(final String serverUrl, final String repositoryName, final String username,
            final String password, final int parallelThreads, final String tempDirLocation) {
        String mainGav = null;
        List<JarEntry> entriesToProcess = new ArrayList<>();
        try (JarFile jar = new JarFile(jarLocation(CarMain.class))) {
            Enumeration<JarEntry> entries = jar.entries();
            JarEntry entry;
            while (entries.hasMoreElements()) {
                entry = entries.nextElement();
                if (entry.isDirectory()) {
                    continue;
                }
                if (entry.getName().startsWith("MAVEN-INF/repository/")) {
                    entriesToProcess.add(entry);
                } else if ("TALEND-INF/metadata.properties".equals(entry.getName())) {
                    // mainGav
                    final Properties properties = new Properties();
                    properties.load(jar.getInputStream(entry));
                    mainGav = properties.getProperty("component_coordinates");
                }
            }
            uploadEntries(serverUrl, repositoryName, username, password, entriesToProcess, jar, parallelThreads,
                    tempDirLocation);
            if (mainGav == null || mainGav.trim().isEmpty()) {
                throw new IllegalArgumentException("Didn't find the component coordinates");
            }
        } catch (final IOException e) {
            throw new IllegalArgumentException(e);
        }
        System.out.println("Installed " + jarLocation(CarMain.class).getName() + " on " + serverUrl + ", "
                + "you can now register '" + mainGav + "' component in your application.");
    }

    private static void uploadEntries(final String serverUrl, final String repositoryName, final String username,
            final String password, final List<JarEntry> entriesToProcess, final JarFile jar, final int parallelThreads,
            final String tempDirLocation) {
        if (entriesToProcess.isEmpty()) {
            return;
        }
        final File tempDirectory;
        if (tempDirLocation == null || tempDirLocation.isEmpty()) {
            System.out.println("No temporary directory is set. Creating a new one...");
            try {
                tempDirectory = Files.createTempDirectory("car-deploy-to-nexus").toFile();
            } catch (IOException e1) {
                String message = "Could not create temp directory: " + e1.getMessage();
                throw new UnsupportedOperationException(message, e1);
            }
        } else {
            tempDirectory = new File(tempDirLocation, "car-deploy-to-nexus-" + UUID.randomUUID().toString());
            tempDirectory.mkdir();
            if (!tempDirectory.exists() || !(tempDirectory.canWrite() && tempDirectory.canRead())) {
                throw new IllegalArgumentException("Cannot access temporary directory " + tempDirLocation);
            }
        }
        System.out.println(tempDirectory.getPath() + " will be used as temporary directory.");
        final String nexusVersion = getNexusVersion(serverUrl, username, password);
        final ExecutorService executor =
                Executors.newFixedThreadPool(Math.min(entriesToProcess.size(), parallelThreads));
        try {
            final String basicAuth = getAuthHeader(username, password);
            final CountDownLatch latch = new CountDownLatch(entriesToProcess.size());
            for (final JarEntry entry : entriesToProcess) {
                final String path = entry.getName().substring("MAVEN-INF/repository/".length());
                executor.execute(() -> {
                    try {
                        if (!artifactExists(nexusVersion, serverUrl, basicAuth, repositoryName, path)) {
                            File extracted = extractJar(tempDirectory, jar, entry);
                            sendJar(nexusVersion, serverUrl, basicAuth, repositoryName, path, extracted);
                            sendPom(nexusVersion, serverUrl, basicAuth, repositoryName, path, extracted);
                        }
                    } catch (Exception e) {
                        System.err.println("A problem occured while uploading artifact: " + e.getMessage());
                    } finally {
                        latch.countDown();
                    }
                });
            }
            try {
                latch.await();
            } catch (InterruptedException e) {
                System.err.println("Exception caught while awaiting for latch: " + e.getMessage());
            }
        } finally {
            executor.shutdown();
            try {
                executor.awaitTermination(1, TimeUnit.DAYS);
            } catch (InterruptedException e) {
                System.err.println("Interrupted while awaiting for executor to shutdown.");
            }
            try {
                System.out.println("Removing " + tempDirectory.getPath());
                removeTempDirectoryRecursively(tempDirectory);
            } catch (Exception e) {
                System.err.println("Couldn't remove " + tempDirectory.getPath() + ": " + e.getMessage());
            }
        }
    }

    private static void removeTempDirectoryRecursively(final File file) {
        if (file.exists() && file.isFile()) {
            file.delete();
            return;
        } else if (file.isDirectory()) {
            String[] files = file.list();
            for (String filename : files) {
                removeTempDirectoryRecursively(new File(file, filename));
            }
            file.delete();
        }
    }

    private static File extractJar(final File destDirectory, final JarFile jar, final JarEntry entry)
            throws IOException {
        File extracted = null;
        try (InputStream is = jar.getInputStream(entry)) {
            String fileName = entry.getName().substring(entry.getName().lastIndexOf("/") + 1);
            extracted = File.createTempFile("temp-", fileName, destDirectory);
            Files.copy(is, extracted.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        return extracted;
    }

    private static String getAuthHeader(final String username, final String password) {
        return "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
    }

    private static boolean artifactExists(final String nexusVersion, final String serverUrl, final String basicAuth,
            final String repositoryName, final String path) throws IOException {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(getNexusUploadUrl(nexusVersion, serverUrl, repositoryName, path));
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", basicAuth);
            conn.connect();
            if (conn.getResponseCode() == 404) {
                return false;
            } else if (conn.getResponseCode() == 401) {
                throw new IllegalArgumentException("Authentication failed!");
            }
            System.out.println("Artifact " + path + " already exists on " + serverUrl + ". Skipping.");
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        return true;
    }

    private static void sendPom(final String nexusVersion, final String serverUrl, final String basicAuth,
            final String repositoryName, final String path, final File jarFile) throws IOException {
        HttpURLConnection conn = null;
        final String pomPath = getPomPathFromPath(path);
        System.out.println("Path of pom file resolved: " + pomPath);
        try (final JarFile jar = new JarFile(jarFile)) {
            JarEntry entry = jar.getJarEntry(pomPath);
            if (entry == null) {
                throw new FileNotFoundException("Could not find " + pomPath + " inside " + jar.getName());
            }
            try (final InputStream jarIs = jar.getInputStream(entry)) {
                final String serverPomPath = path.substring(0, path.lastIndexOf(".")) + ".pom";
                sendData(nexusVersion, serverUrl, basicAuth, repositoryName, serverPomPath, jarIs);
            }
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private static String getPomPathFromPath(final String path) {
        String parentPath = path.substring(0, path.lastIndexOf("/"));
        String version = parentPath.substring(parentPath.lastIndexOf("/") + 1);
        String fileName = path.substring(path.lastIndexOf("/") + 1);
        int versionIndex = fileName.indexOf(version);
        String artifactName = fileName;
        if (versionIndex > 0) {
            artifactName = fileName.substring(0, versionIndex - 1);
        } else if (fileName.endsWith(".jar")) {
            artifactName = fileName.substring(0, fileName.length() - 4);
        }
        String group = parentPath.substring(0, parentPath.lastIndexOf(artifactName));
        if (group.startsWith("/")) {
            group = group.substring(1, group.length());
        }
        if (group.endsWith("/")) {
            group = group.substring(0, group.length() - 1);
        }
        group = group.replace("/", ".");
        return "META-INF/maven/" + group + "/" + artifactName + "/pom.xml";
    }

    private static void sendJar(final String nexusVersion, final String serverUrl, final String basicAuth,
            final String repositoryName, final String path, final File jarFile) throws IOException {
        try (InputStream is = new FileInputStream(jarFile)) {
            sendData(nexusVersion, serverUrl, basicAuth, repositoryName, path, is);
        }
    }

    private static void sendData(final String nexusVersion, final String serverUrl, final String basicAuth,
            final String repositoryName, final String path, final InputStream is) throws IOException {
        System.out.println("Uploading " + path + " to " + serverUrl);
        HttpURLConnection conn = null;
        try {
            URL url = new URL(getNexusUploadUrl(nexusVersion, serverUrl, repositoryName, path));
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod(getRequestMethod(nexusVersion));
            conn.setRequestProperty("Authorization", basicAuth);
            conn.setRequestProperty("Content-Type", "multipart/form-data");
            conn.setRequestProperty("Accept", "*/*");
            conn.connect();
            try (OutputStream out = conn.getOutputStream()) {
                byte[] buffer = new byte[1024];
                int bytesRead = -1;
                while ((bytesRead = is.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
                out.flush();
                if (conn.getResponseCode() != 201) {
                    throw new IOException(conn.getResponseCode() + " - " + conn.getResponseMessage());
                }
            }
            System.out.println(path + " Uploaded");
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private static String getNexusUploadUrl(final String nexusVersion, final String serverUrl,
            final String repositoryName, final String path) {
        if (nexusVersion.equals("V2")) {
            return getUploadUrl(serverUrl, "content/repositories", repositoryName, path);
        } else if (nexusVersion.equals("V3")) {
            return getUploadUrl(serverUrl, "repository", repositoryName, path);
        }
        throw new IllegalArgumentException("Unknown Nexus version: " + nexusVersion);
    }

    private static String getUploadUrl(final String serverUrl, final String repositoriesLocation,
            final String repositoryName, final String path) {
        return serverUrl + (serverUrl.endsWith("/") ? "" : "/") + repositoriesLocation + "/" + repositoryName + "/"
                + path;
    }

    private static String getNexusVersion(final String serverUrl, final String username, final String password) {
        System.out.println("Checking " + serverUrl + " API version.");
        String version = null;
        if (isV2(serverUrl, username, password)) {
            version = "V2";
        } else if (isV3(serverUrl, username, password)) {
            version = "V3";
        }
        if (version == null) {
            throw new UnsupportedOperationException(
                    "Provided url doesn't respond neither to Nexus 2 nor to Nexus 3 endpoints.");
        }
        System.out.println("Nexus API version is recognized as " + version);
        return version;
    }

    private static boolean isV2(final String serverUrl, final String username, final String password) {
        System.out.println("Checking for V2 version...");
        boolean v2 = false;
        HttpURLConnection conn = null;
        try {
            URL url = new URL(serverUrl + (serverUrl.endsWith("/") ? "" : "/") + "service/local/status");
            System.out.println("Sending GET request to " + url.getPath());
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoInput(true);
            String userpass = username + ":" + password;
            String basicAuth = "Basic " + DatatypeConverter.printBase64Binary(userpass.getBytes());
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", basicAuth);
            conn.setRequestProperty("Accept", "application/json");
            conn.connect();
            if (conn.getResponseCode() != 404) {
                try (InputStream is = conn.getInputStream()) {
                    byte[] b = new byte[1024];
                    final StringBuilder out = new StringBuilder();
                    int read = 0;
                    while ((read = is.read(b, 0, b.length)) > 0) {
                        out.append(new String(b), 0, read);
                        b = new byte[1024];
                    }
                    if (out.toString().contains("\"apiVersion\":\"2.")) {
                        System.out.println("version is v2");
                        v2 = true;
                    }
                }
            }
        } catch (IOException e) {
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        return v2;
    }

    private static boolean isV3(final String serverUrl, final String username, final String password) {
        System.out.println("Checking for V3 version...");
        boolean v3 = false;
        HttpURLConnection conn = null;
        try {
            URL url = new URL(serverUrl + (serverUrl.endsWith("/") ? "" : "/") + "service/rest/beta/repositories");
            System.out.println("Sending GET request to " + url.getPath());
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoInput(true);
            String userpass = username + ":" + password;
            String basicAuth = "Basic " + DatatypeConverter.printBase64Binary(userpass.getBytes());
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", basicAuth);
            conn.setRequestProperty("Accept", "*/*");
            conn.connect();
            if (conn.getResponseCode() == 200) {
                v3 = true;
            }
        } catch (IOException e) {
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        return v3;
    }

    private static String getRequestMethod(final String nexusVersion) {
        if ("V2".equals(nexusVersion)) {
            return "POST";
        } else if ("V3".equals(nexusVersion)) {
            return "PUT";
        }
        throw new IllegalArgumentException("Unknown Nexus version: " + nexusVersion);
    }
}
