/**
 * Copyright (C) 2006-2025 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.runtime.testing.spark.internal;

import static java.lang.Thread.sleep;
import static java.util.Arrays.asList;
import static java.util.Locale.ENGLISH;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.ziplock.JarLocation.jarLocation;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

import org.apache.xbean.finder.UrlSet;
import org.apache.ziplock.ClassLoaders;
import org.jboss.shrinkwrap.resolver.api.maven.ConfigurableMavenResolverSystem;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.jboss.shrinkwrap.resolver.api.maven.ScopeType;
import org.jboss.shrinkwrap.resolver.api.maven.strategy.AcceptScopesStrategy;
import org.jboss.shrinkwrap.resolver.api.maven.strategy.MavenResolutionStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.talend.sdk.component.runtime.testing.spark.SparkClusterRule;

import lombok.AllArgsConstructor;
import lombok.Getter;

public abstract class BaseSpark<T extends BaseSpark<?>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SparkClusterRule.class);

    private final ThreadLocal<ClusterConfig> config = new ThreadLocal<>();

    private int slaves = 1;

    private String scalaVersion = SparkVersions.SPARK_SCALA_VERSION.getValue();

    private String sparkVersion = SparkVersions.SPARK_VERSION.getValue();

    private String hadoopBase = "https://github.com/steveloughran/winutils/blob/master";

    private String hadoopVersion = "2.6.4";

    private boolean installWinUtils = true;

    protected abstract void fail(final String message);

    protected abstract void assertTrue(final String message, final boolean value);

    protected abstract File getRoot();

    protected final static String EXTRA_JVM_ARGS =
            "--add-opens=java.base/java.nio=ALL-UNNAMED --add-opens=java.base/sun.nio.ch=ALL-UNNAMED";

    public T withSlaves(final int slaves) {
        this.slaves = slaves;
        return (T) this;
    }

    public T withScalaVersion(final String scalaVersion) {
        this.scalaVersion = scalaVersion;
        return (T) this;
    }

    public T withSparkVersion(final String sparkVersion) {
        this.sparkVersion = sparkVersion;
        return (T) this;
    }

    public T withInstallWinUtils(final boolean installWinUtils) {
        this.installWinUtils = installWinUtils;
        return (T) this;
    }

    public T withHadoopVersion(final String version) {
        this.hadoopVersion = version;
        return (T) this;
    }

    public T withHadoopBase(final String base) {
        this.hadoopBase = base;
        return (T) this;
    }

    public T skipWinUtils() {
        this.installWinUtils = false;
        return (T) this;
    }

    protected Instances start() {
        // prepare needed files on the file system (this sucks but is needed....)
        final Version version = Version.find(sparkVersion);
        final File sparkHome = buildSparkHome(version);
        LOGGER.info("Copied spark libraries in " + sparkHome);

        String masterHost;
        try {
            masterHost = InetAddress.getLocalHost().getHostName();
        } catch (final UnknownHostException e) {
            masterHost = "localhost";
        }

        final int masterPort = newPort();
        final int webMasterPort = newPort();
        final Collection<Runnable> closingTasks = new ArrayList<>();
        final ClusterConfig localConfig =
                new ClusterConfig(masterHost, masterPort, webMasterPort, sparkHome, closingTasks, version);
        config.set(localConfig);
        closingTasks.add(config::remove);

        final String host = masterHost;
        Throwable exception = null;
        try {
            final SparkProcessMonitor master = new SparkProcessMonitor(localConfig, "spark-master-monitor",
                    () -> isOpen(host, masterPort), "org.apache.spark.deploy.master.Master", "--host", masterHost,
                    "--port", Integer.toString(masterPort), "--webui-port", Integer.toString(webMasterPort));
            final Thread masterHook = new Thread(master::close);
            Runtime.getRuntime().addShutdownHook(masterHook);
            closingTasks.add(() -> Runtime.getRuntime().removeShutdownHook(masterHook));
            closingTasks.add(master::close);
            master.start();
            assertTrue("master didn't start", master.isStarted());

            LOGGER.info("Started Master on " + getSparkMaster());

            // todo: enhance it if using slaves > 1, we need to lock all ports together
            final int firstSlavePort = newPort();
            final List<SparkProcessMonitor> slaves = IntStream.range(0, this.slaves).mapToObj(i -> {
                final int slavePort = firstSlavePort + 1 + (2 * i);
                return new SparkProcessMonitor(localConfig, "spark-slave-" + i + "-monitor",
                        () -> isOpen(host, slavePort), "org.apache.spark.deploy.worker.Worker", "--host", host,
                        "--port", Integer.toString(slavePort), "--webui-port", Integer.toString(slavePort + 1),
                        getSparkMaster());
            }).collect(toList());
            slaves.stream().peek(s -> closingTasks.add(s::close)).map(m -> new Thread(m::close)).forEach(t -> {
                Runtime.getRuntime().addShutdownHook(t);
                closingTasks.add(() -> Runtime.getRuntime().removeShutdownHook(t));
            });
            slaves.forEach(SparkProcessMonitor::start);
            if (slaves.stream().anyMatch(m -> !m.isStarted())) {
                fail("Some slave(s) didn't start");
            }
        } catch (final Throwable error) {
            exception = error;
        }
        return new Instances(() -> closingTasks.forEach(r -> {
            try {
                r.run();
            } catch (final RuntimeException re) {
                LOGGER.warn(re.getMessage(), re);
            }
        }), false, exception);
    }

    private File buildSparkHome(final Version version) {
        final File sparkHome = new File(getRoot(), "spark/");
        Stream.of(version.libFolder(), "conf").map(n -> new File(sparkHome, n)).forEach(File::mkdirs);

        // deps
        final File libFolder = new File(sparkHome, version.libFolder());
        final ConfigurableMavenResolverSystem resolver = Maven.configureResolver();
        final MavenResolutionStrategy resolutionStrategy =
                new AcceptScopesStrategy(ScopeType.COMPILE, ScopeType.RUNTIME);
        Stream
                .of("org.apache.spark:spark-core_" + scalaVersion + ":" + sparkVersion,
                        "org.apache.spark:spark-streaming_" + scalaVersion + ":" + sparkVersion)
                .peek(dep -> LOGGER.info("Resolving " + dep + "..."))
                .flatMap(dep -> Stream.of(resolver.resolve(dep).using(resolutionStrategy).asFile()))
                .distinct()
                .forEach(dep -> {
                    try {
                        LOGGER.debug("Copying " + dep.getName() + " dependency");
                        Files
                                .copy(dep.toPath(), new File(libFolder, dep.getName()).toPath(),
                                        StandardCopyOption.REPLACE_EXISTING);
                    } catch (final IOException e) {
                        fail(e.getMessage());
                    }
                });

        if (version == Version.SPARK_1) {
            try {
                Files
                        .write(new File(sparkHome, "RELEASE").toPath(),
                                "fake release file cause it is tested in 1.6.3".getBytes(StandardCharsets.UTF_8),
                                StandardOpenOption.CREATE_NEW);
            } catch (final IOException e) {
                fail(e.getMessage());
            }

            try (final JarOutputStream file = new JarOutputStream(new FileOutputStream(new File(sparkHome,
                    version.libFolder() + "/spark-assembly-" + sparkVersion + "-hadoop2.6.0.jar")))) {
                file.putNextEntry(new ZipEntry("META-INF/marker"));
                file.write("just to let spark find the jar".getBytes(StandardCharsets.UTF_8));
            } catch (final IOException e) {
                fail(e.getMessage());
            }
        }

        if (isWin() && installWinUtils) {
            LOGGER.info("Downloading Hadoop winutils");

            // ensure hadoop winutils is locatable
            final String dll = hadoopBase + "/hadoop-" + hadoopVersion + "/bin/hadoop.dll";
            final String exe = hadoopBase + "/hadoop-" + hadoopVersion + "/bin/winutils.exe";
            new File(sparkHome, "bin").mkdirs();
            Stream.of(dll, exe).forEach(from -> {
                final File target = new File(sparkHome, "bin/" + from.substring(from.lastIndexOf('/') + 1));

                try {
                    final URL url = new URL(from);
                    try (final InputStream stream = url.openStream();
                            final OutputStream out = new FileOutputStream(target)) {
                        final byte[] buffer = new byte[8192];
                        int read;
                        while ((read = stream.read(buffer)) >= 0) {
                            out.write(read);
                        }
                    } catch (final IOException e) {
                        throw new IllegalStateException(e);
                    }
                } catch (final MalformedURLException e) {
                    throw new IllegalArgumentException(e);
                }
            });
        }

        return sparkHome;
    }

    /**
     * Same as {@link BaseSpark#submit(Class, String...)} but automatically
     * set {@code --jars} arguments and bundle on the fly folders into jars.
     *
     * @param main
     * the main to execute.
     * @param args
     * potential arguments to pass to spark submit.
     */
    public void submitClasspath(final Class<?> main, final Predicate<File> classpathFilter, final String... args) {
        final Set<File> files;
        try {
            final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
            files = new UrlSet(ClassLoaders.findUrls(contextClassLoader))
                    .excludeJvm()
                    .getUrls()
                    .stream()
                    .map(ClassLoaders::toFile)
                    .collect(toSet());
        } catch (final IOException e) {
            throw new IllegalArgumentException(e);
        }
        final String classpath = files.stream().filter(classpathFilter).map(file -> {
            if (file.isDirectory()) {
                // bundle it to let spark submit it
                return config.get().jarCache.computeIfAbsent(file, dir -> {
                    final File cache = new File(getRoot(), file.getName() + "_generated_" + System.nanoTime() + ".jar");
                    try (final JarOutputStream out = new JarOutputStream(new FileOutputStream(cache))) {
                        zip(file, out, "");
                    } catch (final IOException e) {
                        fail(e.getMessage());
                    }
                    return cache;
                }).getAbsolutePath();
            }
            return file.getAbsolutePath();
        }).collect(joining(File.pathSeparator));
        submit(main,
                Stream
                        .concat(args == null ? Stream.empty() : Stream.of(args), Stream.of("--jars", classpath))
                        .toArray(String[]::new));
    }

    /**
     * See {@link #submitClasspath(Class, Predicate, String...)}.
     */
    public void submitClasspath(final Class<?> main, final String... args) {
        submitClasspath(main, file -> true, args);
    }

    /**
     * Submits a main to spark cluster.
     *
     * @param main
     * the main to submit to spark.
     * @param args
     * the spark submit arguments, some values have defaults like memory,
     * cores number and deploy mode.
     */
    public void submit(final Class<?> main, final String... args) {
        final String bundle = of(main)
                .map(c -> c.getName().replace('.', '/') + ".class")
                .flatMap(resource -> config.get().jarCache
                        .entrySet()
                        .stream()
                        .filter(jar -> new File(jar.getKey(), resource).exists())
                        .findAny()
                        .map(Map.Entry::getValue))
                .orElseGet(() -> of(jarLocation(main)) // shade case in IT
                        .flatMap(f -> f.getName().endsWith(".jar") ? Optional.of(f)
                                : Optional
                                        .ofNullable(f.getParentFile().listFiles())
                                        .map(Stream::of)
                                        .orElseGet(Stream::empty)
                                        .filter(file -> file.getName().endsWith(".jar")
                                                && !file.getName().startsWith("original-"))
                                        .findFirst())
                        .orElseThrow(() -> new IllegalStateException("No bundle jar found from " + main
                                + ", run tests after packaging (IT with failsafe for instance)")))
                .getAbsolutePath();

        final String[] submitArgs = Stream
                .concat(Stream
                        .concat(Stream
                                .concat(Stream.of("org.apache.spark.deploy.SparkSubmit", "--verbose"),
                                        new HashMap<String, String>() {

                                            { // overridable by args, it is just defaults
                                                put("--executor-memory", "512m"); // 256m fails so don't reduce it too
                                                                                  // much if you try
                                                // to go that way
                                                put("--driver-memory", "512m");
                                                put("--total-executor-cores", "1");
                                                put("--deploy-mode", "cluster");
                                            }
                                        }
                                                .entrySet()
                                                .stream()
                                                .filter(e -> Stream.of(args).noneMatch(p -> p.equals(e.getKey())))
                                                .flatMap(e -> Stream.of(e.getKey(), e.getValue()))),
                                Stream.of("--master", getSparkMaster(), "--class", main.getName(), bundle)),
                        args == null ? Stream.empty() : Stream.of(args))
                .toArray(String[]::new);
        LOGGER.info("Submitting: " + asList(submitArgs));
        final SparkProcessMonitor monitor = new SparkProcessMonitor(config.get(),
                "spark-submit-" + main.getSimpleName() + "-monitor", () -> true, submitArgs);
        final Thread hook = new Thread(monitor::close);
        final Runnable shutdownHookCleanup = () -> Runtime.getRuntime().removeShutdownHook(hook);
        config.get().cleanupTasks.add(shutdownHookCleanup);
        monitor.start();
        assertTrue("monitor is not started", monitor.isStarted());
        int retries = 500;
        while (monitor.process != null && retries-- > 0) {
            try {
                LOGGER.info("Submit result: " + monitor.process.exitValue());
                monitor.close();
                config.get().cleanupTasks.remove(shutdownHookCleanup);
                Runtime.getRuntime().removeShutdownHook(hook);
            } catch (final IllegalThreadStateException itse) {
                try {
                    sleep(750);
                } catch (final InterruptedException e) {
                    fail(e.getMessage());
                    break;
                }
            }
        }
    }

    private void zip(final File root, final JarOutputStream out, final String prefix) {
        final Path rootPath = root.toPath();
        ofNullable(new File(root, prefix).listFiles())
                .map(Stream::of)
                .orElseGet(Stream::empty)
                .filter(f -> !f.getName().startsWith("."))
                .map(File::getAbsoluteFile)
                .forEach(f -> {
                    final Path asPath = f.toPath();
                    final String name = rootPath.relativize(asPath).toString().replace(File.separatorChar, '/');
                    try {
                        if (f.isDirectory()) {
                            out.putNextEntry(new JarEntry(name + '/'));
                            zip(root, out, prefix + '/' + f.getName());
                        } else {
                            out.putNextEntry(new JarEntry(name));
                            Files.copy(asPath, out);
                        }
                    } catch (final IOException e) {
                        fail(e.getMessage());
                    }
                });
    }

    // todo: add a light http client to get status and logs (stderr in particular)

    public String getSparkMasterHttp(final String... path) {
        return "http://" + getMasterHost() + ":" + getWebMasterPort()
                + (path == null || path.length == 0 ? "" : Stream.of(path).collect(joining("/")));
    }

    public String getSparkMaster() {
        return "spark://" + getMasterHost() + ":" + getMasterPort();
    }

    public int getWebMasterPort() {
        return config.get().masterWebPort;
    }

    public String getMasterHost() {
        return config.get().masterHost;
    }

    public int getMasterPort() {
        return config.get().masterPort;
    }

    private static boolean isWin() {
        return System.getProperty("os.name").toLowerCase(ENGLISH).contains("win");
    }

    private static boolean isOpen(final String host, final int port) {
        try (final Socket client = new Socket(host, port)) {
            client.getInputStream().close();
            return true;
        } catch (final IOException ioe) {
            return false;
        }
    }

    private int newPort() { // we can enhance it to preallocate N ports at once if needed and store
        // previously allocated
        // port
        try {
            try (final ServerSocket serverSocket = new ServerSocket(0)) {
                return serverSocket.getLocalPort();
            }
        } catch (final IOException e) {
            fail(e.getMessage());
            return -1;
        }
    }

    private static class ClusterConfig {

        private final String masterHost;

        private final int masterPort;

        private final int masterWebPort;

        private final File sparkHome;

        private final Collection<Runnable> cleanupTasks;

        private final Map<File, File> jarCache = new HashMap<>();

        private final Version version;

        private ClusterConfig(final String masterHost, final int masterPort, final int masterWebPort,
                final File sparkHome, final Collection<Runnable> cleanupTasks, final Version version) {
            this.masterHost = masterHost;
            this.masterPort = masterPort;
            this.masterWebPort = masterWebPort;
            this.sparkHome = sparkHome;
            this.cleanupTasks = cleanupTasks;
            this.version = version;
        }
    }

    private class SparkProcessMonitor extends Thread implements AutoCloseable {

        private final ClusterConfig config;

        private final String[] mainAndArgs;

        private final BooleanSupplier healthCheck;

        private final CountDownLatch started = new CountDownLatch(1);

        private volatile Process process;

        private volatile boolean quit;

        private SparkProcessMonitor(final ClusterConfig config, final String name, final BooleanSupplier healthCheck,
                final String... mainAndArgs) {
            setName(name);
            this.config = config;
            this.mainAndArgs = mainAndArgs;
            this.healthCheck = healthCheck;
        }

        @Override
        public synchronized void run() {
            if (quit) {
                return;
            }

            final File sparkHome = config.sparkHome;
            try {
                final String classpath = Stream
                        .of(ofNullable(new File(sparkHome, config.version.libFolder()).listFiles())
                                .orElseThrow(
                                        () -> new IllegalArgumentException("No spark dependencies in " + sparkHome)))
                        .map(File::getAbsolutePath)
                        .collect(joining(File.pathSeparator));
                LOGGER.debug("Launching " + asList(mainAndArgs));
                final ProcessBuilder builder = new ProcessBuilder()
                        .redirectErrorStream(true)
                        .command(Stream
                                .concat(Stream
                                        .of(new File(System.getProperty("java.home"), "bin/java").getAbsolutePath(),
                                                "-cp", classpath),
                                        Stream.of(mainAndArgs))
                                .collect(toList()));
                final Map<String, String> environment = builder.environment();
                final String jvmVersion = System.getProperty("java.version", "1.8");
                // poor check - suppose using at least jvm 8...
                final Boolean isJava8 = jvmVersion.startsWith("1.8.") || jvmVersion.startsWith("8.");
                if (!isJava8) { // j8
                    environment.put("_JAVA_OPTIONS", EXTRA_JVM_ARGS);
                }
                environment.put("SPARK_HOME", sparkHome.getAbsolutePath());
                environment.put("SPARK_SCALA_VERSION", scalaVersion); // using jarLocation we can determine it if needed
                if (config.version == Version.SPARK_1) { // classpath is relying on assemblies not on maven so force the
                    // right
                    // classpath - todo: move to --driver-class-path
                    environment.put("SPARK_CLASSPATH", classpath);
                }
                if (isWin() && installWinUtils) {
                    environment.put("HADOOP_HOME", sparkHome.getAbsolutePath());
                }
                process = builder.start();

                new Thread(new SurefireWorkaroundOutput(getName(), process.getInputStream())).start();

                int maxRetries = 500;
                // First try will always return true in Windows, even if the port isn't
                // accessible.
                while ((!healthCheck.getAsBoolean() || !healthCheck.getAsBoolean()) && maxRetries-- > 0
                        && process.isAlive()) {
                    try {
                        sleep(500);
                    } catch (final InterruptedException e) {
                        break;
                    }
                }

                LOGGER.info(getName() + " done");
            } catch (final IOException e) {
                throw new IllegalStateException(e);
            } finally {
                started.countDown();
            }
        }

        private boolean isStarted() {
            final long end = System.currentTimeMillis()
                    + TimeUnit.MINUTES.toMillis(Integer.getInteger("talend.junit.spark.timeout", 5));
            while (!quit && end - System.currentTimeMillis() > 0) {
                try {
                    if (started.await(500, TimeUnit.MILLISECONDS)) {
                        break;
                    }
                } catch (final InterruptedException e) {
                    throw new IllegalStateException(e);
                }
            }
            return healthCheck.getAsBoolean();
        }

        @Override
        public synchronized void close() {
            if (quit) {
                return;
            }
            quit = true;
            if (process == null) {
                return;
            }
            try {
                process.exitValue();
            } catch (final IllegalThreadStateException itse) {
                process.destroyForcibly();
                try {
                    process.exitValue();
                } catch (final IllegalThreadStateException itse2) {
                    // no-op
                }
            } finally {
                process = null;
            }
        }
    }

    private static class SurefireWorkaroundOutput implements Runnable {

        private final String name;

        private final InputStream stream;

        private final ByteArrayOutputStream builder = new ByteArrayOutputStream();

        private SurefireWorkaroundOutput(final String name, final InputStream stream) {
            this.name = name;
            this.stream = stream;
        }

        @Override
        public void run() {
            try {
                final byte[] buf = new byte[64];
                int num;
                while ((num = stream.read(buf)) != -1) { // todo: rework it to handle EOL
                    for (int i = 0; i < num; i++) {
                        if (buf[i] == '\r' || buf[i] == '\n') {
                            doLog();
                            builder.reset();
                        } else {
                            builder.write(buf[i]);
                        }
                    }
                }
                if (builder.size() > 0) {
                    doLog();
                }
            } catch (final IOException e) {
                // no-op
            }
        }

        private void doLog() {
            final String string = builder.toString().trim();
            if (string.isEmpty()) {
                return;
            }
            LOGGER.info("[" + name + "] " + string);
        }

    }

    public enum Version {

        SPARK_1 {

            @Override
            String libFolder() {
                return "lib";
            }
        },
        SPARK_2 {

            @Override
            String libFolder() {
                return "jars";
            }
        };

        abstract String libFolder();

        public static Version find(final String sparkVersion) {
            return sparkVersion.startsWith("1.") ? SPARK_1 : SPARK_2;
        }
    }

    @AllArgsConstructor
    protected static class Instances implements AutoCloseable {

        private final AutoCloseable delegate;

        private boolean closed;

        @Getter
        private final Throwable exception;

        @Override
        public synchronized void close() throws Exception {
            if (closed) {
                return;
            }
            closed = true;
            delegate.close();
        }
    }
}
