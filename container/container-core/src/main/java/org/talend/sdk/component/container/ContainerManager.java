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
package org.talend.sdk.component.container;

import static java.util.Collections.emptySet;
import static java.util.Collections.list;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static lombok.AccessLevel.PRIVATE;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.talend.sdk.component.classloader.ConfigurableClassLoader;
import org.talend.sdk.component.dependencies.Resolver;
import org.talend.sdk.component.dependencies.maven.Artifact;
import org.talend.sdk.component.jar.Jars;
import org.talend.sdk.component.lifecycle.Lifecycle;
import org.talend.sdk.component.lifecycle.LifecycleSupport;
import org.talend.sdk.component.path.PathFactory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ContainerManager implements Lifecycle {

    private static final Consumer<Container> NOOP_CUSTOMIZER = c -> {
    };

    private final ConcurrentMap<String, Container> containers = new ConcurrentHashMap<>();

    private final ClassLoaderConfiguration classLoaderConfiguration;

    @Getter
    private final Resolver resolver;

    private final Path rootRepositoryLocation;

    private final Consumer<Container> containerInitializer;

    private final LifecycleSupport lifecycle = new LifecycleSupport();

    private final Collection<ContainerListener> listeners = new CopyOnWriteArrayList<>();

    private final Map<String, String> nestedContainerMapping = new HashMap<>();

    @Getter
    private final String containerId = UUID.randomUUID().toString();

    private final Level logInfoLevelMapping;

    private final String[] jvmMarkers;

    private final boolean hasNestedRepository;

    private final Pattern versionWithJiraIssue = Pattern.compile("-[A-Z]{2,}-\\d+$");

    private final Pattern versionWithMilestone = Pattern.compile("M\\d+$");

    public ContainerManager(final DependenciesResolutionConfiguration dependenciesResolutionConfiguration,
            final ClassLoaderConfiguration classLoaderConfiguration, final Consumer<Container> containerInitializer,
            final Level logInfoLevelMapping) {
        this.logInfoLevelMapping = logInfoLevelMapping;
        this.containerInitializer = containerInitializer;
        this.resolver = dependenciesResolutionConfiguration.getResolver();
        this.rootRepositoryLocation = ofNullable(dependenciesResolutionConfiguration.getRootRepositoryLocation())
                .filter(Files::exists)
                .orElseGet(() -> PathFactory.get(System.getProperty("user.home", "")).resolve(".m2/repository"));

        if (log.isDebugEnabled()) {
            log.debug("Using root repository: " + this.rootRepositoryLocation.toAbsolutePath());
            getSystemInformation();
        }

        final String nestedPluginMappingResource = ofNullable(classLoaderConfiguration.getNestedPluginMappingResource())
                .orElse("TALEND-INF/plugins.properties");
        this.classLoaderConfiguration = new ClassLoaderConfiguration(
                ofNullable(classLoaderConfiguration.getParent()).orElseGet(ContainerManager.class::getClassLoader),
                ofNullable(classLoaderConfiguration.getClassesFilter()).orElseGet(() -> name -> true),
                ofNullable(classLoaderConfiguration.getParentClassesFilter()).orElseGet(() -> name -> true),
                ofNullable(classLoaderConfiguration.getParentResourcesFilter()).orElseGet(() -> name -> true),
                classLoaderConfiguration.isSupportsResourceDependencies(), nestedPluginMappingResource);
        if (classLoaderConfiguration.isSupportsResourceDependencies()) {
            try (final InputStream mappingStream =
                    classLoaderConfiguration.getParent().getResourceAsStream(nestedPluginMappingResource)) {
                if (mappingStream != null) {
                    if (log.isDebugEnabled()) {
                        final URL plug = classLoaderConfiguration.getParent().getResource(nestedPluginMappingResource);
                        if (plug != null) {
                            log.debug("[sysinfo] plugins mapping " + plug.toString());
                        }
                    }
                    final Properties properties = new Properties() {

                        {
                            info("Loading " + nestedPluginMappingResource);
                            load(mappingStream);
                        }
                    };
                    nestedContainerMapping
                            .putAll(properties
                                    .stringPropertyNames()
                                    .stream()
                                    .collect(toMap(identity(), properties::getProperty)));
                    info("Mapped " + getDefinedNestedPlugin() + " plugins");
                } else {
                    info("No " + nestedPluginMappingResource + " found, will use file resolution");
                }
            } catch (final IOException e) {
                throw new IllegalStateException(e);
            }
        } else {
            info("Container " + containerId + " not supporting nested plugin loading, skipping");
        }

        this.jvmMarkers = Stream
                .concat(Stream.concat(Stream.of(getJre()), getComponentModules()), getCustomJvmMarkers())
                .toArray(String[]::new);
        final URL nestedMvn = this.classLoaderConfiguration
                .getParent()
                .getResource(ConfigurableClassLoader.NESTED_MAVEN_REPOSITORY);
        this.hasNestedRepository = this.classLoaderConfiguration.isSupportsResourceDependencies() && nestedMvn != null;
        if (log.isDebugEnabled() && hasNestedRepository) {
            log.debug("[sysinfo] nested maven repository: " + nestedMvn);
        }
    }

    public File getRootRepositoryLocation() {
        return rootRepositoryLocation.toFile();
    }

    public Path getRootRepositoryLocationPath() {
        return rootRepositoryLocation;
    }

    private void info(final String msg) {
        switch (logInfoLevelMapping.intValue()) {
            case 500: // FINE
                log.debug(msg);
                break;
            case 800: // INFO
            default:
                log.info(msg);
        }
    }

    /**
     * @param task task to run avoiding it to stop the calling execution due to an exception.
     * @return false if no error occurred during invocation of the task, true otherwise
     */
    private static RuntimeException safeInvoke(final Runnable task) {
        try {
            task.run();
        } catch (final RuntimeException re) {
            log.error(re.getMessage(), re);
            return re;
        }
        return null;
    }

    public Set<String> getDefinedNestedPlugin() {
        return nestedContainerMapping.keySet();
    }

    public ContainerManager registerListener(final ContainerListener instance) {
        listeners.add(instance);
        return this;
    }

    public ContainerManager unregisterListener(final ContainerListener instance) {
        listeners.remove(instance);
        return this;
    }

    /**
     * @param id the container id (how to find it back from the manager).
     * @param module the module "reference", can be a nested resource
     * (MAVEN-INF/repository) or direct file path or m2 related path.
     * @return the newly created container.
     */
    public ContainerBuilder builder(final String id, final String module) {
        return new ContainerBuilder(id, module, null, emptySet());
    }

    public Path resolve(final String path) {
        final Path direct = PathFactory.get(path);
        if (Files.exists(direct)) {
            return direct;
        }

        final String[] coords = path.split(":");
        if (coords.length > 2) { // mvn gav
            final String relativePath = String
                    .format("%s/%s/%s/%s-%s%s.%s", coords[0].replace('.', '/'), coords[1], coords[2], coords[1],
                            coords[2], coords.length == 5 ? coords[4] : "", coords.length >= 4 ? coords[3] : "jar");
            final Path file = rootRepositoryLocation.resolve(relativePath);
            if (Files.exists(file)) {
                return file;
            }
        }

        final Path file = rootRepositoryLocation.resolve(path);
        if (Files.exists(file)) {
            return file;
        }

        // from job lib folder
        final Path libFile = rootRepositoryLocation.resolve(path.substring(path.lastIndexOf('/') + 1));
        if (Files.exists(libFile)) {
            return libFile;
        }

        // will be filtered later
        return file;
    }

    public ContainerBuilder builder(final String module) {
        return builder(buildAutoIdFromName(module), module);
    }

    public String buildAutoIdFromName(final String module) {
        final String[] segments = module.split(":");
        if (segments.length > 2) { // == 2 can be a windows path so enforce > 2 but then
            // assume it is mvn GAV
            return segments[1];
        }

        final int lastSep = module.replace(File.separatorChar, '/').lastIndexOf('/');
        String autoId = lastSep > 0 ? module.substring(lastSep + 1) : module;
        { // try removing maven versions from the id to support upgrades
            if (autoId.endsWith(".jar")) {
                autoId = autoId.substring(0, autoId.length() - ".jar".length());
            }
            if (autoId.endsWith("-SNAPSHOT")) {
                autoId = autoId.substring(0, autoId.length() - "-SNAPSHOT".length());
            }
            final Matcher jiraTicket = versionWithJiraIssue.matcher(autoId);
            if (jiraTicket.find()) {
                autoId = autoId.substring(0, jiraTicket.start());
            }
            final Matcher milestone = versionWithMilestone.matcher(autoId);
            if (milestone.find()) {
                autoId = autoId.substring(0, milestone.start());
            }
            // strip the version
            int end = autoId.length() - 1;
            for (int i = 0; i < 3; i++) {
                while (end > 0 && Character.isDigit(autoId.charAt(end))) {
                    end--;
                }
                if (end <= 0) {
                    end = autoId.length() - 1;
                    break;
                }
                final boolean valid;
                switch (i) {
                    case 2:
                        valid = autoId.charAt(end) == '-';
                        break;
                    default:
                        valid = autoId.charAt(end) == '.';
                        break;
                }
                if (!valid) {
                    if (i < 1) {
                        end = autoId.length() - 1;
                        break;
                    } else { // we accept only 2 digits
                        end--;
                        break;
                    }
                } else {
                    end--;
                }
            }
            autoId = autoId.substring(0, end + 1);
        }
        if (autoId.isEmpty()) {
            throw new IllegalArgumentException("Invalid name for plugin: " + module);
        }
        return autoId;
    }

    public Optional<Container> find(final String id) {
        return ofNullable(ofNullable(containers.get(id))
                .orElseGet(() -> id == null ? null : containers.get(buildAutoIdFromName(id))));
    }

    public Collection<Container> findAll() {
        return containers.values();
    }

    public List<String> getPluginsList() {
        return findAll()
                .stream()
                .map(Container::getId)
                .sorted()
                .collect(toList());
    }

    public String getPluginsHash() {
        final String plugins = String.join("", getPluginsList());
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(plugins.getBytes(StandardCharsets.UTF_8));
            final char[] hexChars = "0123456789abcdef".toCharArray();
            final StringBuilder out = new StringBuilder(hash.length * 2);
            for (final byte b : hash) {
                out.append(hexChars[b >> 4 & 15]).append(hexChars[b & 15]);
            }
            return out.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error("[getPluginsHash] {}", e.getMessage());
            throw new IllegalStateException(e);
        }
    }

    private Stream<String> getComponentModules() {
        try {
            return list(classLoaderConfiguration.getParent().getResources("META-INF/maven/org.talend.sdk.component/"))
                    .stream()
                    .map(Jars::toPath)
                    .filter(Objects::nonNull)
                    .map(it -> it.toAbsolutePath().toString());
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private Stream<String> getCustomJvmMarkers() {
        try {
            return list(classLoaderConfiguration
                    .getParent()
                    .getResources("TALEND-INF/org.talend.sdk.component.container.ContainerManager.jvmMarkers.txt"))
                    .stream()
                    .flatMap(it -> {
                        try (final BufferedReader reader =
                                new BufferedReader(new InputStreamReader(it.openStream()))) {
                            // tolist to materialize it before closing the resource
                            return reader.lines().collect(toList()).stream();
                        } catch (final IOException e) {
                            throw new IllegalStateException(e);
                        }
                    })
                    .map(String::trim)
                    .filter(it -> !it.isEmpty())
                    .filter(it -> !it.startsWith("#"));
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private String getJre() {
        return of(PathFactory.get(System.getProperty("java.home", "")))
                .map(it -> it.getFileName().toString().equals("jre") && it.getParent() != null
                        && Files.exists(it.getParent().resolve("lib/tools.jar")) ? it.getParent() : it)
                .map(it -> it.toAbsolutePath().toString())
                .orElseThrow(IllegalArgumentException::new);
    }

    private void getSystemInformation() {
        try {
            final RuntimeMXBean rt = ManagementFactory.getRuntimeMXBean();
            log.debug("[sysinfo] JVM arguments: " + rt.getInputArguments());
            try {
                log.debug("[sysinfo] Boot classpath: " + rt.getBootClassPath());
            } catch (Exception e) {
                // nop, will fail in some cases for boot classpath
            }
            log.debug("[sysinfo] Runtime classpath: " + rt.getClassPath());
            log.debug("[sysinfo] Runtime arguments: " + System.getProperty("sun.java.command"));
        } catch (Exception e) {
            log.debug("Unable to get JVM information: " + e.getMessage(), e);
        }
    }

    @Override
    public void close() {
        lifecycle.closeIfNeeded(() -> {
            containers.values().forEach(Container::close);
            containers.clear();
        });
    }

    @Override
    public boolean isClosed() {
        return lifecycle.isClosed();
    }

    @Getter
    @Builder(buildMethodName = "create")
    public static class DependenciesResolutionConfiguration {

        private final Resolver resolver;

        private final Path rootRepositoryLocation;
    }

    @Getter
    @Builder(buildMethodName = "create")
    public static class ClassLoaderConfiguration {

        private final ClassLoader parent;

        private final Predicate<String> classesFilter;

        private final Predicate<String> parentClassesFilter;

        private final Predicate<String> parentResourcesFilter;

        // is nested jar in jar supported (1 level only)
        private final boolean supportsResourceDependencies;

        private final String nestedPluginMappingResource;

        // note: we can add if needed resource filters too (to filter META-INF/services
        // for instance)
    }

    @AllArgsConstructor(access = PRIVATE)
    public static class Actions {

        private final Container self;

        public void reload() {
            final ContainerManager.ContainerBuilder builder = self.get(ContainerManager.ContainerBuilder.class);
            self.close();
            builder.create();
        }
    }

    @AllArgsConstructor(access = PRIVATE)
    public class ContainerBuilder {

        private final String id;

        private final String module;

        private Consumer<Container> customizer;

        private Collection<Artifact> additionalClasspath;

        public ContainerBuilder withAdditionalClasspath(final Collection<Artifact> additionalClasspath) {
            this.additionalClasspath = ofNullable(additionalClasspath).orElseGet(Collections::emptySet);
            return this;
        }

        public ContainerBuilder withCustomizer(final Consumer<Container> customizer) {
            this.customizer = customizer;
            return this;
        }

        public Container create() {
            if (lifecycle.isClosed()) {
                throw new IllegalStateException("ContainerManager already closed");
            }

            final String moduleLocation = classLoaderConfiguration.isSupportsResourceDependencies()
                    ? nestedContainerMapping.getOrDefault(module, module)
                    : module;
            final Path resolved = resolve(moduleLocation);
            info("Creating module " + moduleLocation + " (from " + module
                    + (Files.exists(resolved) ? ", location=" + resolved.toAbsolutePath().toString() : "") + ")");
            final Stream<Artifact> classpath = Stream
                    .concat(getBuiltInClasspath(moduleLocation),
                            additionalClasspath == null ? Stream.empty() : additionalClasspath.stream());

            final Container container = new Container(id, moduleLocation, classpath.toArray(Artifact[]::new),
                    classLoaderConfiguration, ContainerManager.this::resolve,
                    ofNullable(containerInitializer)
                            .orElse(NOOP_CUSTOMIZER)
                            .andThen(ofNullable(customizer).orElse(NOOP_CUSTOMIZER)),
                    jvmMarkers, hasNestedRepository) {

                @Override
                public void close() {
                    setState(State.UNDEPLOYING);
                    try {
                        listeners.forEach(l -> safeInvoke(() -> l.onClose(this)));
                    } finally {
                        try {
                            super.close();
                        } finally {
                            containers.remove(id);
                            setState(State.UNDEPLOYED);
                        }
                    }
                    info("Closed container " + id);
                }
            };
            container.setState(Container.State.CREATED);
            container.set(ContainerBuilder.class, this);
            container.set(Actions.class, new Actions(container));

            final Collection<RuntimeException> re = new ArrayList<>();
            final ConfigurableClassLoader loader = container.getLoader();
            final Thread thread = Thread.currentThread();
            final ClassLoader oldLoader = thread.getContextClassLoader();
            thread.setContextClassLoader(loader);
            try {
                final Collection<ContainerListener> calledListeners = listeners
                        .stream()
                        .filter(l -> !ofNullable(safeInvoke(() -> l.onCreate(container))).map(re::add).orElse(false))
                        .collect(toList());
                if (calledListeners.size() == listeners.size()) {
                    if (containers.putIfAbsent(id, container) != null) {
                        container.setState(Container.State.ON_ERROR);
                        calledListeners.forEach(l -> safeInvoke(() -> l.onClose(container)));
                        throw new IllegalArgumentException("Container '" + id + "' already exists");
                    }
                } else {
                    info("Failed creating container " + id);
                    calledListeners.forEach(l -> safeInvoke(() -> l.onClose(container)));
                    final IllegalArgumentException exception = new IllegalArgumentException(id + " can't be deployed");
                    re.forEach(exception::addSuppressed);
                    throw exception;
                }
            } finally {
                thread.setContextClassLoader(oldLoader);
            }

            container.setState(Container.State.DEPLOYED);
            info("Created container " + id);
            return container;
        }

        private Stream<Artifact> getBuiltInClasspath(final String moduleLocation) {
            return resolver.resolve(classLoaderConfiguration.getParent(), moduleLocation);
        }
    }
}