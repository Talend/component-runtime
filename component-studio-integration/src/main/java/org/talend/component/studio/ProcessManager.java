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
package org.talend.component.studio;

import static java.util.Arrays.asList;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.URIUtil;
import org.talend.core.runtime.maven.MavenConstants;
import org.talend.osgi.hook.maven.MavenResolver;

public class ProcessManager implements AutoCloseable {

    private final String groupId;

    private final String artifactId;

    private MavenResolver mavenResolver;

    private int port;

    private Process process;

    private Thread hook;

    private CountDownLatch ready;

    public ProcessManager(final String groupId, final String artifactId, final MavenResolver resolver) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.mavenResolver = resolver;
    }

    public void waitForServer() { // useful for the client, to ensure we are ready
        if (ready == null) {
            return;
        }
        try {
            ready.await(10, TimeUnit.MINUTES);
        } catch (final InterruptedException e) {
            Thread.interrupted();
        }
    }

    @Override
    public synchronized void close() {
        if (hook != null) {
            Runtime.getRuntime().removeShutdownHook(hook);
            hook = null;
        }

        if (process == null) {
            return;
        }

        try {
            process.exitValue();
        } catch (final IllegalThreadStateException itse) {
            if (process.isAlive()) {
                try {
                    process.destroyForcibly().waitFor();
                } catch (final InterruptedException e) {
                    if (process.isAlive()) {
                        process.destroy();
                    }
                }
            }
        } finally {
            process = null;
            hook = null;
        }
    }

    public synchronized void start() {
        final File studioConfigDir;
        try {
            studioConfigDir = URIUtil.toFile(Platform.getConfigurationLocation().getURL().toURI());
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("bad configuration configuration", e);
        }

        final Collection<String> paths;
        try {
            paths = createClasspath();
        } catch (final IOException e) {
            throw new IllegalArgumentException(e);
        }

        final String java = findJava();
        port = newPort();
        final String[] jvmOptions = Stream.of(System.getProperty("component.java.options", "-Xmx256m").split(" "))
                .map(String::trim).filter(o -> !o.isEmpty()).toArray(String[]::new);
        final String[] arguments = Stream.of(System.getProperty("component.java.arguments", "").split(" ")).map(String::trim)
                .filter(o -> !o.isEmpty()).toArray(String[]::new);

        final File log4j2Config = Stream
                .of(new File(studioConfigDir, "log4j2-components.xml"), new File(studioConfigDir, "log4j2.xml"))
                .filter(File::exists).findFirst().orElse(null);

        final List<String> command = new ArrayList<>();
        command.add(java);
        command.addAll(asList(jvmOptions));
        if (log4j2Config != null) {
            command.add("-Dlog4j.configurationFile=" + log4j2Config.getAbsolutePath());
        } // else it will log into the console
        command.add("-classpath");
        command.add(paths.stream().collect(Collectors.joining(File.pathSeparator)));
        command.add("org.apache.meecrowave.runner.Cli");
        command.add("--http");
        command.add(Integer.toString(port));
        command.addAll(asList(arguments));

        final ProcessBuilder pb = new ProcessBuilder(command);
        pb.inheritIO();
        try {
            process = pb.start();
        } catch (final IOException e) {
            throw new IllegalArgumentException(e);
        }

        ready = new CountDownLatch(1);

        hook = new Thread() { // in case of a ctrl+C/kill+X on the studio

            {
                setName(getClass().getName() + "-shutdown-hook");
            }

            @Override
            public void run() {
                close();
            }
        };
        new Thread() { // just a healthcheck to be able to ensure the server is up when starting to use it (ou client)

            {
                setName(getClass().getName() + "-readiness-checker");
            }

            @Override
            public void run() {
                final long end = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(15);
                while (end - System.currentTimeMillis() >= 0) {
                    if (!process.isAlive()) {
                        throw new IllegalStateException("Component server process failed: " + process.exitValue());
                    }

                    try (final Socket s = new Socket("localhost", port)) {
                        s.getInputStream().close();
                        ready.countDown();
                        return; // opened :)
                    } catch (final IOException e) {
                        try { // try again
                            Thread.sleep(500);
                        } catch (final InterruptedException e1) {
                            Thread.interrupted();
                            break;
                        }
                    }
                }
                throw new IllegalStateException("Process " + command + " didnt start in 15mn!");
            }
        }.start();
        Runtime.getRuntime().addShutdownHook(hook);
    }

    private Collection<String> createClasspath() throws IOException {
        final File serverJar = resolve(groupId + ":component-server:jar:" + findVersion());
        if (!serverJar.exists()) {
            throw new IllegalArgumentException(serverJar + " doesn't exist");
        }

        final Collection<String> paths = new ArrayList<>();
        try (final JarFile jar = new JarFile(serverJar)) {
            final ZipEntry entry = jar.getEntry("TALEND-INF/dependencies.txt");
            try (final InputStream deps = jar.getInputStream(entry)) {
                try (final BufferedReader reader = new BufferedReader(new InputStreamReader(deps))) {
                    String line;

                    do {
                        line = reader.readLine();
                        if (line == null) {
                            break;
                        }
                        line = line.trim();
                        if (line.isEmpty()) {
                            continue;
                        }

                        if (line.split(":").length < 4) {
                            continue;
                        }
                        if (line.endsWith(":test") || line.endsWith(":provided")) {
                            continue;
                        }

                        final String[] segments = line.split(":");
                        if (segments.length < 4) {
                            throw new IllegalArgumentException("Invalid coordinate: " + line);
                        }

                        paths.add(resolve(line).getAbsolutePath());
                    } while (true);
                } catch (final IOException e) {
                    throw new IllegalStateException(e);
                }
            }
        }
        paths.add(resolve("commons-cli:commons-cli:jar:1.4").getAbsolutePath()); // we use the Cli as main so we need it
        paths.add(serverJar.getAbsolutePath());
        return paths;
    }

    public File resolve(final String gav) {
        try { // convert to pax-url syntax
            final String[] split = gav.split("\\:"); // assuming we dont use classifiers for now
            final String paxUrl = "mvn:" + MavenConstants.LOCAL_RESOLUTION_URL + '!' + split[0] + '/' + split[1] + '/' + split[3];
            return mavenResolver.resolve(paxUrl);
        } catch (final IOException e) {
            throw new IllegalArgumentException("can't resolve '" + gav + "', "
                    + "in development ensure you are using maven.repository=global in configuration/config.ini, "
                    + "in a standalone installation, ensure the studio maven repository contains this dependency", e);
        }
    }

    private Integer newPort() {
        final Integer port = Integer.getInteger("component.java.port", -1);
        if (port <= 0) {
            try (final ServerSocket socket = new ServerSocket(0)) {
                return socket.getLocalPort();
            } catch (final IOException e) {
                throw new IllegalStateException(e);
            }
        }
        return port;
    }

    private String findJava() {
        final String home = System.getProperty("java.home");
        final File java = Stream
                .of(new File(home, "bin/java"), new File(home, "bin/java.exe"),
                        new File(System.getProperty("component.java.exe", "java")))
                .filter(File::isFile).findFirst().orElseThrow(() -> new IllegalArgumentException(
                        "Didn't find java executable, maybe set component.java.exe to point to your java binary in config.ini"));
        return java.getAbsolutePath();
    }

    private String findVersion() {
        try (final InputStream stream = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("META-INF/maven/" + groupId + "/" + artifactId + "/pom.properties")) {
            if (stream == null) {
                throw new IllegalStateException("Can't find artifact " + groupId + ':' + artifactId);
            }
            final Properties properties = new Properties();
            properties.load(stream);
            return properties.getProperty("version");
        } catch (final IOException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
