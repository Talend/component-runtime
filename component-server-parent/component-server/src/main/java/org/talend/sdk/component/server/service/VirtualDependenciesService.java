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
package org.talend.sdk.component.server.service;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableSet;
import static java.util.Locale.ROOT;
import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static lombok.AccessLevel.PACKAGE;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;

import org.talend.sdk.component.api.service.configuration.LocalConfiguration;
import org.talend.sdk.component.classloader.ConfigurableClassLoader;
import org.talend.sdk.component.container.Container;
import org.talend.sdk.component.dependencies.maven.Artifact;
import org.talend.sdk.component.path.PathFactory;
import org.talend.sdk.component.runtime.manager.ComponentManager;
import org.talend.sdk.component.server.configuration.ComponentServerConfiguration;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class VirtualDependenciesService {

    @Getter(PACKAGE)
    private final String virtualGroupId = "virtual.talend.component.server.generated.";

    @Getter(PACKAGE)
    private final String configurationArtifactIdPrefix = "user-local-configuration-";

    private final Enrichment noCustomization = new Enrichment(false, null, null, null);

    @Inject
    private ComponentServerConfiguration configuration;

    private final Map<String, Enrichment> enrichmentsPerContainer = new HashMap<>();

    @Getter
    private final Map<Artifact, Path> artifactMapping = new ConcurrentHashMap<>();

    private final Map<Artifact, Supplier<InputStream>> configurationArtifactMapping = new ConcurrentHashMap<>();

    private Path provisioningM2Base;

    @PostConstruct
    private void init() {
        final String m2 = configuration.getUserExtensionsAutoM2Provisioning();
        switch (m2) {
            case "skip":
                provisioningM2Base = null;
                break;
            case "auto":
                provisioningM2Base = findStudioM2();
                break;
            default:
                provisioningM2Base = PathFactory.get(m2);
        }
        log.debug("m2 provisioning base: {}", provisioningM2Base);
    }

    public void onDeploy(final String pluginId) {
        if (!configuration.getUserExtensions().isPresent()) {
            enrichmentsPerContainer.put(pluginId, noCustomization);
            return;
        }
        final Path extensions = PathFactory
                .get(configuration.getUserExtensions().orElseThrow(IllegalArgumentException::new))
                .resolve(pluginId);
        if (!Files.exists(extensions)) {
            log.debug("'{}' does not exist so no extension will be added to family '{}'", extensions, pluginId);
            enrichmentsPerContainer.put(pluginId, noCustomization);
            return;
        }

        final Path userConfig = extensions.resolve("user-configuration.properties");
        final Map<Artifact, Path> userJars = findJars(extensions, pluginId);
        final Properties userConfiguration = loadUserConfiguration(pluginId, userConfig, userJars);
        if (userConfiguration.isEmpty() && userJars.isEmpty()) {
            log.debug("No customization for container '{}'", pluginId);
            enrichmentsPerContainer.put(pluginId, noCustomization);
            return;
        }

        final Map<String, String> customConfigAsMap = userConfiguration
                .stringPropertyNames()
                .stream()
                .collect(toMap(identity(), userConfiguration::getProperty));
        log
                .debug("Set up customization for container '{}' (has-configuration={}, jars={})", pluginId,
                        !userConfiguration.isEmpty(), userJars);

        if (userConfiguration.isEmpty()) {
            enrichmentsPerContainer.put(pluginId, new Enrichment(true, customConfigAsMap, null, userJars.keySet()));
        } else {
            final byte[] localConfigurationJar = generateConfigurationJar(pluginId, userConfiguration);
            final Artifact configurationArtifact;
            try {
                configurationArtifact = new Artifact(groupIdFor(pluginId), configurationArtifactIdPrefix + pluginId,
                        "jar", "", Long.toString(Files.getLastModifiedTime(userConfig).toMillis()), "compile");
            } catch (final IOException e) {
                throw new IllegalStateException(e);
            }
            doProvision(configurationArtifact, () -> new ByteArrayInputStream(localConfigurationJar));
            enrichmentsPerContainer
                    .put(pluginId, new Enrichment(true, customConfigAsMap, configurationArtifact, userJars.keySet()));
            configurationArtifactMapping
                    .put(configurationArtifact, () -> new ByteArrayInputStream(localConfigurationJar));
        }
        userJars.forEach((artifact, file) -> doProvision(artifact, () -> {
            try {
                return Files.newInputStream(file, StandardOpenOption.READ);
            } catch (final IOException e) {
                throw new IllegalStateException(e);
            }
        }));

        artifactMapping.putAll(userJars);
    }

    public void onUnDeploy(final Container plugin) {
        final Enrichment enrichment = enrichmentsPerContainer.remove(plugin.getId());
        if (enrichment == null || enrichment == noCustomization) {
            return;
        }

        if (enrichment.userArtifacts != null) {
            enrichment.userArtifacts.forEach(artifactMapping::remove);
            enrichment.userArtifacts.clear();
        }
        if (enrichment.configurationArtifact != null) {
            configurationArtifactMapping.remove(enrichment.configurationArtifact);
        }
    }

    public boolean isVirtual(final String gav) {
        return gav.startsWith(virtualGroupId);
    }

    public Enrichment getEnrichmentFor(final String pluginId) {
        return enrichmentsPerContainer.get(pluginId);
    }

    public Stream<Artifact> userArtifactsFor(final String pluginId) {
        final Enrichment enrichment = enrichmentsPerContainer.get(pluginId);
        if (enrichment == null || !enrichment.customized) {
            return Stream.empty();
        }
        final Stream<Artifact> userJars = enrichment.userArtifacts.stream();
        if (enrichment.configurationArtifact != null) {
            // config is added but will be ignored cause not physically here
            // however it ensures our rest service returns right data
            return Stream.concat(userJars, Stream.of(enrichment.configurationArtifact));
        }
        return userJars;
    }

    public Supplier<InputStream> retrieveArtifact(final Artifact artifact) {
        return ofNullable(artifactMapping.get(artifact)).map(it -> (Supplier<InputStream>) () -> {
            try {
                return Files.newInputStream(it);
            } catch (final IOException e) {
                throw new IllegalStateException(e);
            }
        }).orElseGet(() -> configurationArtifactMapping.get(artifact));
    }

    public String groupIdFor(final String family) {
        return virtualGroupId + sanitizedForGav(family);
    }

    private byte[] generateConfigurationJar(final String family, final Properties userConfiguration) {
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        final Manifest manifest = new Manifest();
        final Attributes mainAttributes = manifest.getMainAttributes();
        mainAttributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        mainAttributes.putValue("Created-By", "Talend Component Kit Server");
        mainAttributes.putValue("Talend-Time", Long.toString(System.currentTimeMillis()));
        mainAttributes.putValue("Talend-Family-Name", family);
        try (final JarOutputStream jar = new JarOutputStream(new BufferedOutputStream(outputStream), manifest)) {
            jar.putNextEntry(new JarEntry("TALEND-INF/local-configuration.properties"));
            userConfiguration.store(jar, "Configuration of the family " + family);
            jar.closeEntry();
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
        return outputStream.toByteArray();
    }

    private Properties loadUserConfiguration(final String plugin, final Path userConfig,
            final Map<Artifact, Path> userJars) {
        final Properties properties = new Properties();
        if (!Files.exists(userConfig)) {
            return properties;
        }
        final String content;
        try (final BufferedReader stream = Files.newBufferedReader(userConfig)) {
            content = stream.lines().collect(joining("\n"));
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
        try (final Reader reader = new StringReader(replaceByGav(plugin, content, userJars))) {
            properties.load(reader);
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
        return properties;
    }

    // handle "userJar(name)" function in the config, normally not needed since jars are in the context already
    // note: if we make it more complex, switch to a real parser or StrSubstitutor
    String replaceByGav(final String plugin, final String content, final Map<Artifact, Path> userJars) {
        final StringBuilder output = new StringBuilder();
        final String prefixFn = "userJar(";
        int fnIdx = content.indexOf(prefixFn);
        int previousEnd = 0;
        if (fnIdx < 0) {
            output.append(content);
        } else {
            while (fnIdx >= 0) {
                final int end = content.indexOf(')', fnIdx);
                output.append(content, previousEnd, fnIdx);
                output.append(toGav(plugin, content.substring(fnIdx + prefixFn.length(), end), userJars));
                fnIdx = content.indexOf(prefixFn, end);
                if (fnIdx < 0) {
                    if (end < content.length() - 1) {
                        output.append(content, end + 1, content.length());
                    }
                } else {
                    previousEnd = end + 1;
                }
            }
        }
        return output.toString();
    }

    private String toGav(final String plugin, final String jarNameWithoutExtension,
            final Map<Artifact, Path> userJars) {
        return groupIdFor(plugin) + ':' + jarNameWithoutExtension + ":jar:"
                + userJars
                        .keySet()
                        .stream()
                        .filter(it -> it.getArtifact().equals(jarNameWithoutExtension))
                        .findFirst()
                        .map(Artifact::getVersion)
                        .orElse("unknown");
    }

    private Map<Artifact, Path> findJars(final Path familyFolder, final String family) {
        if (!Files.isDirectory(familyFolder)) {
            return emptyMap();
        }
        try {
            return Files
                    .list(familyFolder)
                    .filter(file -> file.getFileName().toString().endsWith(".jar"))
                    .collect(toMap(it -> {
                        try {
                            return new Artifact(groupIdFor(family), toArtifact(it), "jar", "",
                                    Long.toString(Files.getLastModifiedTime(it).toMillis()), "compile");
                        } catch (final IOException e) {
                            throw new IllegalStateException(e);
                        }
                    }, identity()));
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private String toArtifact(final Path file) {
        final String name = file.getFileName().toString();
        return name.substring(0, name.length() - ".jar".length());
    }

    private String sanitizedForGav(final String name) {
        return name.replace(' ', '_').toLowerCase(ROOT);
    }

    // note: we don't want to provision based on our real m2, only studio one for now
    private Path findStudioM2() {
        if (System.getProperty("talend.studio.version") != null && System.getProperty("osgi.bundles") != null) {
            final Path localM2 = PathFactory.get(System.getProperty("talend.component.server.maven.repository", ""));
            if (Files.isDirectory(localM2)) {
                return localM2;
            }
        }
        return null;
    }

    private void doProvision(final Artifact artifact, final Supplier<InputStream> newInputStream) {
        if (provisioningM2Base == null) {
            log.debug("No m2 to provision, skipping {}", artifact);
            return;
        }
        final Path target = provisioningM2Base.resolve(artifact.toPath());
        if (target.toFile().exists()) {
            log.debug("{} already exists, skipping", target);
            return;
        }
        final Path parentFile = target.getParent();
        if (!Files.exists(parentFile)) {
            try {
                Files.createDirectories(parentFile);
            } catch (final IOException e) {
                throw new IllegalArgumentException("Can't create " + parentFile, e);
            }
        }
        try (final InputStream stream = new BufferedInputStream(newInputStream.get())) {
            Files.copy(stream, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @RequiredArgsConstructor
    private static class Enrichment {

        private final boolean customized;

        private final Map<String, String> customConfiguration;

        private final Artifact configurationArtifact;

        private final Collection<Artifact> userArtifacts;
    }

    public static class LocalConfigurationImpl implements LocalConfiguration {

        private final VirtualDependenciesService delegate;

        public LocalConfigurationImpl() {
            delegate = CDI.current().select(VirtualDependenciesService.class).get();
        }

        @Override
        public String get(final String key) {
            if (key == null || !key.contains(".")) {
                return null;
            }
            final String plugin = key.substring(0, key.indexOf('.'));
            final Enrichment enrichment = delegate.getEnrichmentFor(plugin);
            if (enrichment == null || enrichment.customConfiguration == null) {
                return null;
            }
            return enrichment.customConfiguration.get(key.substring(plugin.length() + 1));
        }

        @Override
        public Set<String> keys() {
            final ClassLoader loader = Thread.currentThread().getContextClassLoader();
            if (!ConfigurableClassLoader.class.isInstance(loader)) {
                return emptySet();
            }
            final String id = ConfigurableClassLoader.class.cast(loader).getId();
            final Enrichment enrichment = delegate.getEnrichmentFor(id);
            if (enrichment == null || enrichment.customConfiguration == null) {
                return emptySet();
            }
            return unmodifiableSet(enrichment.customConfiguration.keySet());
        }
    }

    public static class UserContainerClasspathContributor implements ComponentManager.ContainerClasspathContributor {

        private final VirtualDependenciesService delegate;

        public UserContainerClasspathContributor() {
            delegate = CDI.current().select(VirtualDependenciesService.class).get();
        }

        @Override
        public Collection<Artifact> findContributions(final String pluginId) {
            delegate.onDeploy(pluginId);
            return delegate.userArtifactsFor(pluginId).collect(toList());
        }

        @Override
        public boolean canResolve(final String path) {
            return delegate.isVirtual(path.replace('/', '.'));
        }

        @Override
        public Path resolve(final String path) {
            if (path.contains('/' + delegate.getConfigurationArtifactIdPrefix())) {
                return null; // not needed, will be enriched on the fly, see LocalConfigurationImpl
            }
            final String[] segments = path.split("/");
            if (segments.length < 9) {
                return null;
            }
            // ex: virtual.talend.component.server.generated.<plugin id>:<artifact>:jar:dynamic
            final String group = delegate
                    .groupIdFor(Stream.of(segments).skip(5).limit(segments.length - 5 - 3).collect(joining(".")));
            final String artifact = segments[segments.length - 3];
            return delegate
                    .getArtifactMapping()
                    .entrySet()
                    .stream()
                    .filter(it -> it.getKey().getGroup().equals(group) && it.getKey().getArtifact().equals(artifact))
                    .findFirst()
                    .map(Map.Entry::getValue)
                    .orElse(null);
        }
    }
}
