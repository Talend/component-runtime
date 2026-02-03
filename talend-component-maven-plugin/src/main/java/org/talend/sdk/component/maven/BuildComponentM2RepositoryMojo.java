/**
 * Copyright (C) 2006-2026 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.maven;

import static java.nio.file.Files.copy;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.newBufferedWriter;
import static java.nio.file.Files.newInputStream;
import static java.nio.file.Files.setLastModifiedTime;
import static java.nio.file.Files.walkFileTree;
import static java.nio.file.attribute.FileTime.from;
import static java.time.Instant.ofEpochMilli;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.io.FileUtils.deleteDirectory;
import static org.apache.commons.io.output.NullOutputStream.NULL_OUTPUT_STREAM;
import static org.apache.maven.plugins.annotations.LifecyclePhase.PACKAGE;
import static org.apache.maven.plugins.annotations.ResolutionScope.COMPILE;
import static org.talend.sdk.component.maven.api.Audience.Type.TALEND_INTERNAL;
import static org.talend.sdk.component.maven.api.Constants.CAR_EXTENSION;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.talend.sdk.component.maven.api.Audience;
import org.talend.sdk.component.maven.api.Constants;

/**
 * Finds all the cars:
 * <ul>
 * <li>defined as talend-component-maven-plugin's dependencies</li>
 * <li>with scope compile</li>
 * <li>with classifier {@link Constants#CAR_EXTENSION}</li>
 * </ul>
 * Then copies all the jars inside (app jars and dependencies) in a pseudo maven local repository.
 * This repository can then be included in a Docker image to be mounted in a component-server so that
 * the component-server loads all the components.
 */
@Audience(TALEND_INTERNAL)
@Mojo(name = "prepare-repository", defaultPhase = PACKAGE, threadSafe = true, requiresDependencyResolution = COMPILE)
public class BuildComponentM2RepositoryMojo extends ComponentDependenciesBase {

    private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();

    @Parameter(defaultValue = "true", property = "talend.connectors.write")
    private Boolean writeVersion;

    @Parameter(defaultValue = "${project.version}", property = "talend.connectors.version")
    private String version;

    @Parameter(defaultValue = "CONNECTORS_VERSION", property = "talend.connectors.file")
    private String connectorsVersionFile;

    @Parameter(property = "talend-m2.registryBase")
    private File componentRegistryBase;

    private Path componentRegistryBasePath; // Can't get a Path directly from a @Parameter...

    @Parameter(property = "talend-m2.root",
            defaultValue = "${maven.multiModuleProjectDirectory}/target/talend-component-kit/maven")
    protected File m2Root;

    protected Path m2RootPath; // Can't get a Path directly from a @Parameter...

    @Parameter(property = "talend-m2.clean", defaultValue = "true")
    private boolean cleanBeforeGeneration;

    @Parameter(defaultValue = "true", property = "talend.repository.createDigestRegistry")
    private boolean createDigestRegistry;

    @Parameter(defaultValue = "SHA-512", property = "talend.repository.digestAlgorithm")
    private String digestAlgorithm;

    @Override
    public void doExecute() throws MojoExecutionException {
        final Set<Artifact> cars = getCars();

        m2RootPath = Paths.get(m2Root.getAbsolutePath());
        componentRegistryBasePath = componentRegistryBase == null
                ? null
                : Paths.get(componentRegistryBase.getAbsolutePath());

        try {
            if (cleanBeforeGeneration && exists(m2RootPath)) {
                deleteDirectory(m2RootPath.toFile()); // java.nio.Files.delete fails if dir is not empty
            }
            createDirectories(m2RootPath);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }

        cars.forEach(this::copyComponentDependencies);

        if (cars.isEmpty()) {
            throw new IllegalStateException(
                    "No components found, check the component cars are included in your dependencies with scope compile");
        } else {
            final String coordinates = cars
                    .stream()
                    .map(this::computeCoordinates)
                    .collect(joining(","));

            getLog().info("Included components " + coordinates);
        }

        writeRegistry(getNewComponentRegistry(cars));
        if (createDigestRegistry) {
            writeDigest(getDigests());
        }

        if (writeVersion) {
            writeConnectorsVersion();
            getLog().info(connectorsVersionFile + " set to " + version);
        }

        getLog().info("Created component repository at " + m2Root);
    }

    protected Set<Artifact> getCars() {
        final String talendComponentPluginId = "talend-component-maven-plugin";
        final Plugin talendComponentPlugin = project.getBuild()
                .getPlugins()
                .stream()
                .filter(plugin -> plugin.getArtifactId().equals(talendComponentPluginId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "The plugin " + talendComponentPluginId + " was not found in your pom"));

        return talendComponentPlugin
                .getDependencies()
                .stream()
                .filter(car -> CAR_EXTENSION.equals(car.getType()))
                .filter(car -> COMPILE.id().equals(car.getScope()))
                .map(car -> new DefaultArtifact(
                        car.getGroupId(),
                        car.getArtifactId(),
                        car.getClassifier(),
                        CAR_EXTENSION,
                        car.getVersion()))
                .map(this::resolveArtifactOnRemoteRepositories) // No resolve, no file
                .collect(toSet());
    }

    protected Properties getNewComponentRegistry(final Set<Artifact> cars) {
        final Properties components = new Properties();
        if (componentRegistryBasePath != null && exists(componentRegistryBasePath)) {
            try (final InputStream source = newInputStream(componentRegistryBasePath)) {
                components.load(source);
            } catch (final IOException e) {
                throw new IllegalStateException(e);
            }
        }
        cars.forEach(car -> components.put(car.getArtifactId(), computeCoordinates(car)));

        return components;
    }

    private void writeProperties(final Properties content, final Path location) {
        try (final Writer output = newBufferedWriter(location)) {
            content.store(output, "Generated by Talend Component Kit " + getClass().getSimpleName());
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
    }

    protected Properties getDigests() {
        final Properties index = new Properties();
        try {
            walkFileTree(m2RootPath, new SimpleFileVisitor<Path>() {

                @Override
                public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                    if (!file.getFileName().toString().startsWith(".")) {
                        index.setProperty(m2RootPath.relativize(file).toString(), hash(file));
                    }
                    return super.visitFile(file, attrs);
                }
            });
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
        return index;
    }

    protected void writeDigest(final Properties digestRegistry) {
        writeProperties(digestRegistry, getDigestRegistry());
    }

    protected void writeRegistry(final Properties components) {
        writeProperties(components, getRegistry());
    }

    private void writeConnectorsVersion() {
        try (final Writer output = newBufferedWriter(getConnectorsVersionFile())) {
            output.write(version);
            output.flush();
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private String hash(final Path file) {
        try (final DigestOutputStream out =
                new DigestOutputStream(NULL_OUTPUT_STREAM, MessageDigest.getInstance(digestAlgorithm))) {
            copy(file, out);
            out.flush();
            return hex(out.getMessageDigest().digest());
        } catch (final NoSuchAlgorithmException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String hex(final byte[] data) {
        final StringBuilder out = new StringBuilder(data.length * 2);
        for (final byte b : data) {
            out.append(HEX_CHARS[b >> 4 & 15]).append(HEX_CHARS[b & 15]);
        }
        return out.toString();
    }

    /**
     * Copies a dependency from a car in {@link BuildComponentM2RepositoryMojo#m2Root}.
     */
    private void copyDependency(final ZipEntry zipEntry, final ZipInputStream zipStream) {
        final String relativeDependencyPath = zipEntry
                .getName()
                .substring("MAVEN-INF/repository/".length());

        final Path m2DependencyPath = m2RootPath.resolve(relativeDependencyPath);

        try {
            createDirectories(m2DependencyPath.getParent());
            copy(zipStream, m2DependencyPath, StandardCopyOption.REPLACE_EXISTING);

            final long lastModified = zipEntry.getTime();
            if (lastModified > 0) {
                setLastModifiedTime(m2DependencyPath, from(ofEpochMilli(lastModified)));
            }
            if (getLog().isDebugEnabled()) {
                getLog().debug("Adding " + m2DependencyPath);
            }
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Takes a car and copies all of its dependencies in {@link BuildComponentM2RepositoryMojo#m2Root}.
     */
    protected void copyComponentDependencies(final Artifact car) {
        try (final FileInputStream fileStream = new FileInputStream(car.getFile());
                final BufferedInputStream bufferedStream = new BufferedInputStream(fileStream);
                final ZipInputStream zipStream = new ZipInputStream(bufferedStream)) {

            ZipEntry zipEntry;
            while ((zipEntry = zipStream.getNextEntry()) != null) {
                if (zipEntry.isDirectory()) {
                    continue;
                }

                if (!zipEntry.getName().startsWith("MAVEN-INF/repository/")) {
                    continue;
                }

                copyDependency(zipEntry, zipStream);
            }
        } catch (final IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private Path getConnectorsVersionFile() {
        return m2RootPath.resolve(connectorsVersionFile);
    }

    protected Path getRegistry() {
        return m2RootPath.resolve("component-registry.properties");
    }

    protected Path getDigestRegistry() {
        return m2RootPath.resolve("component-registry-digest.properties");
    }
}
