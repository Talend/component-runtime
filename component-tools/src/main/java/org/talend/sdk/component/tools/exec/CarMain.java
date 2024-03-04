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
package org.talend.sdk.component.tools.exec;

import static java.util.Locale.ROOT;
import static java.util.stream.Collectors.joining;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

// IMPORTANT: this class MUST not have ANY dependency, not even slf4j!
public class CarMain {

    public static final String COMPONENT_JAVA_COORDINATES = "component.java.coordinates";

    public static final String COMPONENT_COORDINATES = "component_coordinates";

    public static final String COMPONENT_SERVER_EXTENSIONS = "component.server.extensions";

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
        boolean forceOverwrite = false;
        for (String arg : args) {
            if ("-f".equals(arg)) {
                forceOverwrite = true;
                break;
            }
        }

        switch (args[0].toLowerCase(ROOT)) {
        case "studio-deploy":
            final String studioPath;
            if (args.length == 2) {
                studioPath = args[1];
            } else {
                studioPath = getArgument("--location", args);
            }
            if (studioPath == null || studioPath.isEmpty()) {
                System.err.println("Path to studio is not set. Use '--location <location>' to set it.");
                help();
                break;
            }
            deployInStudio(studioPath, forceOverwrite);
            break;
        case "maven-deploy":
            final String mavenPath;
            if (args.length == 2) {
                mavenPath = args[1];
            } else {
                mavenPath = getArgument("--location", args);
            }
            if (mavenPath == null || mavenPath.isEmpty()) {
                System.err.println("Path to maven repository is not set. Use '--location <location>' to set it.");
                help();
                break;
            }
            deployInM2(mavenPath, forceOverwrite);
            break;
        case "deploy-to-nexus":
            final String url = getArgument("--url", args);
            final String repo = getArgument("--repo", args);
            final String user = getArgument("--user", args);
            final String pass = getArgument("--pass", args);
            final String threads = getArgument("--threads", args);
            final int threadsNum;
            if (threads == null) {
                threadsNum = Runtime.getRuntime().availableProcessors();
            } else {
                threadsNum = Integer.parseInt(threads);
            }
            final String dir = getArgument("--dir", args);
            if (url == null || url.isEmpty()) {
                System.err.println("Nexus url is not set. Use '--url <url>' to set it");
                help();
                break;
            }
            if (repo == null || repo.isEmpty()) {
                System.err.println("Nexus repo is not set. Use '--repo <repository>' to set it");
                help();
                break;
            }
            deployToNexus(url, repo, user, pass, threadsNum, dir);
            break;
        default:
            help();
            throw new IllegalArgumentException("Unknown command '" + args[0] + "'");
        }
    }

    private static String getArgument(final String argumentPrefix, final String... args) {
        for (int i = 1; i < args.length - 1; i++) {
            if (args[i].equals(argumentPrefix)) {
                return args[i + 1];
            }
        }
        return null;
    }

    private static void deployInM2(final String m2, final boolean forceOverwrite) {
        final File m2File = new File(m2);
        if (!m2File.exists()) {
            throw new IllegalArgumentException(m2 + " doesn't exist");
        }
        final String component = installJars(m2File, forceOverwrite).getProperty(COMPONENT_COORDINATES);
        System.out
                .println("Installed " + jarLocation(CarMain.class).getName() + " in " + m2 + ", "
                        + "you can now register '" + component + "' component in your application.");
    }

    private static void deployInStudio(final String studioLocation, final boolean forceOverwrite) {
        System.out.println(String.format("Connector is being deployed to %s.", studioLocation));
        final File root = new File(studioLocation);
        if (!root.isDirectory()) {
            throw new IllegalArgumentException(studioLocation + " is not a valid directory");
        }

        final File config = new File(studioLocation, "configuration/config.ini");
        if (!config.exists()) {
            throw new IllegalArgumentException("No " + config + " found, is your studio location right?");
        }

        final Properties configuration = readProperties(config);
        final File m2Root;
        String m2RepoPath = System.getProperty("talend.studio.m2.repo", null);
        if (m2RepoPath != null) {
            m2Root = new File(m2RepoPath);
        } else {
            final String repositoryType = configuration.getProperty("maven.repository");
            if ("global".equals(repositoryType)) {
                // grab local maven setup, we use talend env first to override dev one then dev env setup
                // and finally this main system props as a more natural config but less likely set on a dev machine
                m2Root = Stream
                        .of("TALEND_STUDIO_MAVEN_HOME", "MAVEN_HOME", "M2_HOME",
                                "talend.component.server.maven.repository",
                                "talend.studio.m2")
                        .map(it -> it.contains(".") ? System.getProperty(it) : System.getenv(it))
                        .filter(Objects::nonNull)
                        .findFirst()
                        .map(mvnHome -> {
                            final File globalSettings = new File(mvnHome, "conf/settings.xml");
                            if (globalSettings.exists()) {
                                try {
                                    final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                                    factory.setFeature(javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING, true);
                                    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
                                    final DocumentBuilder builder = factory.newDocumentBuilder();
                                    final Document document = builder.parse(globalSettings);
                                    final NodeList localRepository = document.getElementsByTagName("localRepository");
                                    if (localRepository.getLength() == 1) {
                                        final Node node = localRepository.item(0);
                                        if (node != null) {
                                            final String repoPath = node.getTextContent();
                                            if (repoPath != null) {
                                                return new File(repoPath);
                                            }
                                        }
                                    }
                                } catch (final ParserConfigurationException | SAXException | IOException e) {
                                    throw new IllegalStateException(e);
                                }
                            }
                            return null;
                        })
                        .orElseGet(() -> new File(System.getProperty("user.home"), ".m2/repository/"));
            } else {
                m2Root = new File(studioLocation, "configuration/.m2/repository/");
            }
        }
        if (!m2Root.isDirectory()) {
            throw new IllegalArgumentException(m2Root + " does not exist, did you specify a valid m2 studio property?");
        }

        // install jars
        final Properties carProperties = installJars(m2Root, forceOverwrite);
        final String mainGav = carProperties.getProperty(COMPONENT_COORDINATES);
        final String type = carProperties.getProperty("type", "connector");

        // register the component/extension
        final String key =
                "extension".equalsIgnoreCase(type) ? COMPONENT_SERVER_EXTENSIONS : COMPONENT_JAVA_COORDINATES;
        final String components = configuration.getProperty(key);
        try {
            final List<String> configLines = Files.readAllLines(config.toPath());
            if (components == null) {
                final String original = configLines.stream().collect(joining("\n"));
                try (final Writer writer = new FileWriter(config)) {
                    writer.write(original + "\n" + key + " = " + mainGav);
                }
            } else {
                // backup
                final File backup = new File(config.getParentFile(), "backup/" + config.getName() + "_"
                        + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-mm-dd_HH-mm-ss")));
                backup.getParentFile().mkdirs();
                try (final OutputStream to = new BufferedOutputStream(new FileOutputStream(backup))) {
                    Files.copy(config.toPath(), to);
                }

                boolean skip = false;
                try (final Writer writer = new FileWriter(config)) {
                    for (final String line : configLines) {
                        if (line.trim().startsWith(key)) {
                            skip = true;
                            continue;
                        } else if (skip && line.trim().contains("=")) {
                            skip = false;
                        } else if (skip) {
                            continue;
                        }
                        writer.write(line + "\n");
                    }
                    final String toFilter = Stream
                            .of(mainGav.contains(":") ? mainGav.split(":") : mainGav.split("/"))
                            .limit(2)
                            .collect(Collectors.joining(":", "", ":"));
                    writer
                            .write(key + " = " + Stream
                                    .concat(Stream.of(mainGav),
                                            Stream
                                                    .of(components.trim().split(","))
                                                    .map(String::trim)
                                                    .filter(it -> !it.isEmpty())
                                                    .filter(it -> !it.startsWith(toFilter)))
                                    .collect(joining(",")));
                }
                System.out.println(type + " registered.");
            }
        } catch (final IOException ioe) {
            throw new IllegalStateException(ioe);
        }
        System.out.println(type + " deployed successfully.");
    }

    private static Properties installJars(final File m2Root, final boolean forceOverwrite) {
        String mainGav = null;
        final Properties properties = new Properties();
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
                    if (!output.getCanonicalPath().startsWith(m2Root.getCanonicalPath())) {
                        throw new IOException("The output file is not contained in the destination directory");
                    }
                    if (!output.exists() || forceOverwrite) {
                        output.getParentFile().mkdirs();
                        Files.copy(jar, output.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    }
                } else if ("TALEND-INF/metadata.properties".equals(entry.getName())) {
                    // mainGav
                    properties.load(jar);
                    mainGav = properties.getProperty(COMPONENT_COORDINATES);
                }
            }
        } catch (final IOException e) {
            throw new IllegalArgumentException(e);
        }
        if (mainGav == null || mainGav.trim().isEmpty()) {
            throw new IllegalArgumentException("Didn't find the component coordinates");
        }
        System.out.println(String.format("Connector %s and dependencies jars installed to %s.", mainGav, m2Root));
        return properties;
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
        System.err.println("Usage:\n\njava -jar " + jarLocation(CarMain.class).getName() + " [command] [arguments]");
        System.err.println("commands:");
        System.err.println("   studio-deploy");
        System.err.println("      arguments:");
        System.err.println("         --location <location>: path to studio");
        System.err.println("         or");
        System.err.println("         <location>: path to studio");
        System.err.println("         -f : force overwrite existing jars");
        System.err.println();
        System.err.println("   maven-deploy");
        System.err.println("      arguments:");
        System.err.println("         --location <location>: path to .m2 repository");
        System.err.println("         or");
        System.err.println("         <location>: path to .m2 repository");
        System.err.println("         -f : force overwrite existing jars");
        System.err.println();
        System.err.println("   deploy-to-nexus");
        System.err.println("      arguments:");
        System.err.println("         --url <nexusUrl>: nexus server url");
        System.err.println("         --repo <repositoryName>: nexus repository name to upload dependencies to");
        System.err.println("         --user <username>: username to connect to nexus(optional)");
        System.err.println("         --pass <password>: password to connect to nexus(optional)");
        System.err
                .println(
                        "         --threads <parallelThreads>: threads number to use during upload to nexus(optional)");
        System.err.println("         --dir <tempDirectory>: temporary directory to use during upload(optional)");
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
                return new File(
                        URLDecoder.decode(new URL(spec.substring(0, separator)).getFile(), "UTF-8"));
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

    private static File toFile(final String classFileName, final URL url) throws UnsupportedEncodingException {
        final String path = url.getFile();
        return new File(
                URLDecoder.decode(path.substring(0, path.length() - classFileName.length()), "UTF-8"));
    }

    private static void deployToNexus(final String serverUrl, final String repositoryName, final String username,
            final String password, final int parallelThreads, final String tempDirLocation) {
        String mainGav = null;
        final List<JarEntry> entriesToProcess = new ArrayList<>();
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
                    mainGav = properties.getProperty(COMPONENT_COORDINATES);
                }
            }
            if (mainGav == null || mainGav.trim().isEmpty()) {
                throw new IllegalArgumentException("Didn't find the component coordinates");
            }
            uploadEntries(serverUrl, repositoryName, username, password, entriesToProcess, jar, parallelThreads,
                    tempDirLocation);
        } catch (final IOException e) {
            throw new IllegalArgumentException(e);
        }
        System.out
                .println("Installed " + jarLocation(CarMain.class).getName() + " on " + serverUrl + ", "
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
            } catch (final IOException e1) {
                String message = "Could not create temp directory: " + e1.getMessage();
                throw new UnsupportedOperationException(message, e1);
            }
        } else {
            tempDirectory = new File(tempDirLocation, "car-deploy-to-nexus-" + UUID.randomUUID().toString());
            tempDirectory.mkdirs();
            if (!tempDirectory.exists() || !(tempDirectory.canWrite() && tempDirectory.canRead())) {
                throw new IllegalArgumentException("Cannot access temporary directory " + tempDirLocation);
            }
        }
        System.out.println(tempDirectory.getPath() + " will be used as temporary directory.");
        final String basicAuth = getAuthHeader(username, password);
        final String nexusVersion = getNexusVersion(serverUrl, username, password, basicAuth);
        final ExecutorService executor =
                Executors.newFixedThreadPool(Math.min(entriesToProcess.size(), parallelThreads));
        try {
            final CountDownLatch latch = new CountDownLatch(entriesToProcess.size());
            for (final JarEntry entry : entriesToProcess) {
                final String path = entry.getName().substring("MAVEN-INF/repository/".length());
                executor.execute(() -> {
                    try {
                        if (!artifactExists(nexusVersion, serverUrl, basicAuth, repositoryName, path)) {
                            final File extracted = extractJar(tempDirectory, jar, entry);
                            sendJar(nexusVersion, serverUrl, basicAuth, repositoryName, path, extracted);
                            sendPom(nexusVersion, serverUrl, basicAuth, repositoryName, path, extracted);
                        }
                    } catch (final Exception e) {
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
            } catch (final InterruptedException e) {
                System.err.println("Interrupted while awaiting for executor to shutdown.");
                Thread.currentThread().interrupt();
            }
            try {
                System.out.println("Removing " + tempDirectory.getPath());
                if (tempDirectory.exists()) {
                    removeTempDirectoryRecursively(tempDirectory);
                }
            } catch (Exception e) {
                System.err.println("Couldn't remove " + tempDirectory.getPath() + ": " + e.getMessage());
            }
        }
    }

    private static void removeTempDirectoryRecursively(final File file) {
        if (file.exists() && file.isFile()) {
            file.delete();
        } else if (file.isDirectory()) {
            final File[] files = file.listFiles();
            if (files != null) {
                for (final File child : files) {
                    removeTempDirectoryRecursively(child);
                }
            }
            file.delete();
        }
    }

    private static File extractJar(final File destDirectory, final JarFile jar, final JarEntry entry)
            throws IOException {
        File extracted;
        try (final InputStream is = jar.getInputStream(entry)) {
            final String fileName = entry.getName().substring(entry.getName().lastIndexOf("/") + 1);
            extracted = File.createTempFile("temp-", fileName, destDirectory);
            Files.copy(is, extracted.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        return extracted;
    }

    private static String getAuthHeader(final String username, final String password) {
        if (username == null || username.isEmpty()) {
            return null;
        }
        return "Basic " + Base64
                .getEncoder()
                .encodeToString((username + (password == null || password.isEmpty() ? "" : ":" + password)).getBytes());
    }

    private static boolean artifactExists(final String nexusVersion, final String serverUrl, final String basicAuth,
            final String repositoryName, final String path) throws IOException {
        HttpURLConnection conn = null;
        try {
            final URL url = new URL(getNexusUploadUrl(nexusVersion, serverUrl, repositoryName, path));
            conn = HttpURLConnection.class.cast(url.openConnection());
            conn.setDoOutput(true);
            conn.setRequestMethod("GET");
            if (basicAuth != null) {
                conn.setRequestProperty("Authorization", basicAuth);
            }
            conn.connect();
            if (conn.getResponseCode() == 404) {
                return false;
            } else if (conn.getResponseCode() == 401) {
                throw new IllegalArgumentException("Authentication failed!");
            } else if (conn.getResponseCode() == 400) {
                System.out.println("Ignoring " + path + ", it is likely not deployed on the right repository.");
            } else {
                System.out.println("Artifact " + path + " already exists on " + serverUrl + ". Skipping.");
            }
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        return true;
    }

    private static void sendPom(final String nexusVersion, final String serverUrl, final String basicAuth,
            final String repositoryName, final String path, final File jarFile) throws IOException {
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
        }
    }

    private static String getPomPathFromPath(final String path) {
        final String parentPath = path.substring(0, path.lastIndexOf("/"));
        final String version = parentPath.substring(parentPath.lastIndexOf("/") + 1);
        final String fileName = path.substring(path.lastIndexOf("/") + 1);
        final int versionIndex = fileName.indexOf(version);
        final String artifactName;
        if (versionIndex > 0) {
            artifactName = fileName.substring(0, versionIndex - 1);
        } else if (fileName.endsWith(".jar")) {
            artifactName = fileName.substring(0, fileName.length() - 4);
        } else {
            artifactName = fileName;
        }
        String group = parentPath.substring(0, parentPath.lastIndexOf(artifactName));
        if (group.startsWith("/")) {
            group = group.substring(1);
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
            if (basicAuth != null) {
                conn.setRequestProperty("Authorization", basicAuth);
            }
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

    // note: maybe move to restapi. see
    // https://support.sonatype.com/hc/en-us/articles/115006744008-How-can-I-programmatically-upload-files-into-Nexus-3-
    private static String getNexusUploadUrl(final String nexusVersion, final String serverUrl,
            final String repositoryName, final String path) {
        if (nexusVersion.equals("V2")) {
            return getUploadUrl(serverUrl, "content/repositories", repositoryName, path);
        } else if (nexusVersion.startsWith("V3")) {
            return getUploadUrl(serverUrl, "repository", repositoryName, path);
        }
        throw new IllegalArgumentException("Unknown Nexus version: " + nexusVersion);
    }

    private static String getUploadUrl(final String serverUrl, final String repositoriesLocation, final String repo,
            final String path) {
        return serverUrl + (serverUrl.endsWith("/") ? "" : "/") + repositoriesLocation + "/" + repo + "/" + path;
    }

    private static String getNexusVersion(final String serverUrl, final String username, final String password,
            final String auth) {
        System.out.println("Checking " + serverUrl + " API version.");
        final String version;
        if (isV2(serverUrl, username, password, auth)) {
            version = "V2";
        } else if (isStableV3(serverUrl, username, password, auth)) {
            version = "V3";
        } else if (isBetaV3(serverUrl, username, password, auth)) {
            version = "V3Beta";
        } else {
            throw new UnsupportedOperationException(
                    "Provided url doesn't respond neither to Nexus 2 nor to Nexus 3 endpoints.");
        }
        System.out.println("Nexus API version is recognized as " + version);
        return version;
    }

    private static boolean isV2(final String serverUrl, final String username, final String password,
            final String auth) {
        System.out.println("Checking for V2 version...");
        HttpURLConnection conn = null;
        try {
            conn = prepareGet(serverUrl, username, password, "service/local/status", "*/*", auth);
            if (conn.getResponseCode() >= 200 && conn.getResponseCode() <= 299) {
                try (InputStream is = conn.getInputStream()) {
                    final byte[] b = new byte[1024];
                    final StringBuilder out = new StringBuilder();
                    int read;
                    while ((read = is.read(b, 0, b.length)) > 0) {
                        out.append(new String(b, 0, read));
                    }
                    if (out.toString().contains("\"apiVersion\":\"2.")) {
                        System.out.println("version is v2");
                        return true;
                    }
                }
            }
        } catch (final IOException e) {
            // no-op
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        return false;
    }

    private static boolean isBetaV3(final String serverUrl, final String username, final String password,
            final String auth) {
        System.out.println("Checking for V3Beta version...");
        return get(serverUrl, username, password, "service/rest/beta/repositories", auth);
    }

    private static boolean isStableV3(final String serverUrl, final String username, final String password,
            final String auth) {
        System.out.println("Checking for V3 version...");
        return get(serverUrl, username, password, "service/rest/v1/repositories", auth);
    }

    private static boolean get(final String serverUrl, final String username, final String password, final String path,
            final String auth) {
        boolean passed = false;
        HttpURLConnection conn = null;
        try {
            conn = prepareGet(serverUrl, username, password, path, "application/json", auth);
            if (conn.getResponseCode() == 200) {
                passed = true;
            }
        } catch (final IOException e) {
            // no-op
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        return passed;
    }

    private static HttpURLConnection prepareGet(final String serverUrl, final String username, final String password,
            final String path, final String accept, final String auth) throws IOException {
        final URL url = new URL(serverUrl + (serverUrl.endsWith("/") ? "" : "/") + path);
        System.out.println("Sending GET request to " + url.getPath());
        final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoInput(true);
        final String userpass = username + ":" + password;
        final String basicAuth = "Basic " + Base64.getEncoder().encodeToString(userpass.getBytes());
        conn.setRequestMethod("GET");
        if (auth != null) {
            conn.setRequestProperty("Authorization", auth);
        }
        conn.setRequestProperty("Accept", accept);
        conn.connect();
        return conn;
    }

    private static String getRequestMethod(final String nexusVersion) {
        if ("V2".equals(nexusVersion)) {
            return "POST";
        } else if (nexusVersion.startsWith("V3")) {
            return "PUT";
        }
        throw new IllegalArgumentException("Unknown Nexus version: " + nexusVersion);
    }
}
