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
package org.talend.sdk.component.studio;

import static java.lang.Thread.sleep;
import static java.util.Optional.ofNullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

import org.eclipse.m2e.core.MavenPlugin;
import org.talend.core.runtime.maven.MavenConstants;
import org.talend.osgi.hook.maven.MavenResolver;
import org.talend.sdk.component.studio.lang.LocalLock;
import org.talend.sdk.component.studio.lang.StringPropertiesTokenizer;

import lombok.Getter;

public class ProcessManager implements AutoCloseable {

    private final String groupId;

    private final String artifactId;

    private final File studioConfigDir;

    private MavenResolver mavenResolver;

    @Getter
    private int port;

    private Process process;

    private Thread hook;

    private volatile CountDownLatch ready;

    public ProcessManager(final String groupId, final String artifactId, final MavenResolver resolver,
            final File studioConfigDir) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.mavenResolver = resolver;
        this.studioConfigDir = studioConfigDir;
    }

    public void waitForServer(final Runnable healthcheck) { // useful for the client, to ensure we are ready
        final int steps = 250;
        for (int i = 0; i < TimeUnit.MINUTES.toMillis(10) / steps; i++) {
            try {
                if (ready.await(steps, TimeUnit.MILLISECONDS)) {
                    healthcheck.run();
                    return;
                }
                if (i > 0 && i % 12 == 0) {
                    final Process process = this.process;
                    if (process != null) {
                        try {
                            final int exitValue = process.exitValue();
                            System.err.println("Component server didn't start properly (exit code=" + exitValue
                                    + "), please check the log before");
                            Lookups.configuration().disable();
                            break;
                        } catch (final IllegalThreadStateException itse) {
                            // expected
                        }
                    }
                    System.out.println("Component server not yet ready, will wait again"); // no logger!
                }
                sleep(steps);
            } catch (final InterruptedException e) {
                Thread.interrupted();
                break;
            } catch (final RuntimeException re) {
                try {
                    sleep(500); // wait and retry, the healthcheck failed
                } catch (final InterruptedException e) {
                    Thread.interrupted();
                    break;
                }
            }
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
        final Collection<String> paths;
        try {
            paths = createClasspath();
        } catch (final IOException e) {
            throw new IllegalArgumentException(e);
        }

        final String java = findJava();
        final Collection<String> jvmOptions =
                new StringPropertiesTokenizer(System.getProperty("component.java.options", "-Xmx256m")).tokens();
        final Collection<String> arguments =
                new StringPropertiesTokenizer(System.getProperty("component.java.arguments", "")).tokens();

        final File log4j2Config = Stream
                .of(new File(studioConfigDir, "log4j2-components.xml"), new File(studioConfigDir, "log4j2.xml"))
                .filter(File::exists)
                .findFirst()
                .orElse(null);

        String m2Repo = System.getProperty("component.java.m2");
        if (m2Repo == null) {
            m2Repo = MavenPlugin.getMaven().getLocalRepositoryPath();
        }

        final String components = System.getProperty("component.java.coordinates");
        final String registry = System.getProperty("component.java.registry");

        final List<String> command = new ArrayList<>();
        command.add(java);
        command.addAll(jvmOptions);

        if (log4j2Config != null) {
            command.add("-Dlog4j.configurationFile=" + log4j2Config.getAbsolutePath());
        } // else it will log into the console
        if (m2Repo != null) {
            command.add("-Dtalend.component.server.maven.repository=" + m2Repo);
        }
        if (components != null) {
            command.add("-Dtalend.component.server.component.coordinates=" + components);
        }
        if (registry != null) {
            command.add("-Dtalend.component.server.component.registry=" + registry);
        }
        if (Boolean.getBoolean("component.java.debug")) {
            command.add("-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address="
                    + Integer.getInteger("component.java.debug.port", 5005));
        }
        // passthrough names matching the server config, can be redundant with previous
        // component.java.xx but easier to understand
        System
                .getProperties()
                .stringPropertyNames()
                .stream()
                .filter(n -> n.startsWith("talend.component.server."))
                .forEach(key -> command.add("-D" + key + "=" + System.getProperty(key, "")));

        // local instance, no need of any security
        command.add("-Dtalend.component.server.security.connection.handler=securityNoopHandler");
        command.add("-Dtalend.component.server.security.command.handler=securityNoopHandler");

        command.add("-classpath");
        command.add(paths.stream().collect(Collectors.joining(File.pathSeparator)));
        command.add("org.apache.meecrowave.runner.Cli");

        final Lock lock = new LocalLock(
                ofNullable(System.getProperty("component.lock.location")).map(File::new).orElseGet(
                        () -> new File(System.getProperty("user.home"), ".talend/locks/" + GAV.ARTIFACT_ID + ".lock")),
                null);
        lock.lock();
        port = newPort();

        if (!arguments.contains("--http")) {
            command.add("--http");
            command.add(Integer.toString(port));
        }
        command.addAll(arguments);

        ready = new CountDownLatch(1);

        final ProcessBuilder pb = new ProcessBuilder(command);
        pb.inheritIO();
        try {
            process = pb.start();
        } catch (final IOException e) {
            lock.unlock();
            throw new IllegalArgumentException(e);
        }

        hook = new Thread() { // in case of a ctrl+C/kill+X on the studio

            {
                setName(getClass().getName() + "-shutdown-hook");
            }

            @Override
            public void run() {
                lock.unlock();
                close();
            }
        };
        new Thread() { // just a healthcheck to be able to ensure the server is up when starting to use
            // it (ou client)

            {
                setName(getClass().getName() + "-readiness-checker");
            }

            @Override
            public void run() {
                final long end = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(15);
                while (end - System.currentTimeMillis() >= 0) {
                    if (!process.isAlive()) {
                        lock.unlock();
                        throw new IllegalStateException("Component server process failed: " + process.exitValue());
                    }

                    try (final Socket s = new Socket("localhost", port)) {
                        new URL("http://localhost:" + port + "/api/v1/environment").openStream().close();
                        lock.unlock();
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

                    lock.unlock();
                }
                throw new IllegalStateException("Process " + command + " didnt start in 15mn!");
            }
        }.start();
        Runtime.getRuntime().addShutdownHook(hook);
    }

    private Collection<String> createClasspath() throws IOException {
        final File serverJar = resolve(groupId + ":component-server:jar:" + GAV.VERSION);
        if (!serverJar.exists()) {
            throw new IllegalArgumentException(serverJar + " doesn't exist");
        }

        final Collection<String> paths = new ArrayList<>();
        if (serverJar.isDirectory()) {
            try (final InputStream deps = new FileInputStream(new File(serverJar, "TALEND-INF/dependencies.txt"))) {
                addDependencies(paths, deps);
            }
        } else {
            try (final JarFile jar = new JarFile(serverJar)) {
                final ZipEntry entry = jar.getEntry("TALEND-INF/dependencies.txt");
                try (final InputStream deps = jar.getInputStream(entry)) {
                    addDependencies(paths, deps);
                }
            }
        }
        // we use the Cli as main so we need it
        paths.add(resolve("commons-cli:commons-cli:jar:" + GAV.CLI_VERSION).getAbsolutePath());
        paths.add(serverJar.getAbsolutePath());
        return paths;
    }

    private void addDependencies(final Collection<String> paths, final InputStream deps) {
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

    private File resolve(final String gav) {
        try { // convert to pax-url syntax
            final String[] split = gav.split("\\:"); // assuming we dont use classifiers for now
            final String paxUrl =
                    "mvn:" + MavenConstants.LOCAL_RESOLUTION_URL + '!' + split[0] + '/' + split[1] + '/' + split[3];
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
                .filter(File::isFile)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Didn't find java executable, maybe set component.java.exe to point to your java binary in config.ini"));
        return java.getAbsolutePath();
    }
}
