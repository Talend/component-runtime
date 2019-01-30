/**
 * Copyright (C) 2006-2019 Talend Inc. - www.talend.com
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

import static java.lang.Long.MAX_VALUE;
import static java.util.Comparator.comparing;
import static java.util.Locale.ENGLISH;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.apache.maven.plugins.annotations.LifecyclePhase.PACKAGE;
import static org.apache.maven.plugins.annotations.ResolutionScope.TEST;
import static org.talend.sdk.component.maven.api.Audience.Type.TALEND_INTERNAL;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.google.cloud.tools.jib.api.Containerizer;
import com.google.cloud.tools.jib.api.DockerDaemonImage;
import com.google.cloud.tools.jib.api.Jib;
import com.google.cloud.tools.jib.api.JibContainerBuilder;
import com.google.cloud.tools.jib.api.RegistryImage;
import com.google.cloud.tools.jib.configuration.CacheDirectoryCreationException;
import com.google.cloud.tools.jib.configuration.LayerConfiguration;
import com.google.cloud.tools.jib.configuration.Port;
import com.google.cloud.tools.jib.event.EventHandlers;
import com.google.cloud.tools.jib.filesystem.AbsoluteUnixPath;
import com.google.cloud.tools.jib.image.ImageReference;
import com.google.cloud.tools.jib.image.InvalidImageReferenceException;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.crypto.DefaultSettingsDecryptionRequest;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.apache.maven.settings.crypto.SettingsDecryptionResult;
import org.eclipse.aether.artifact.Artifact;
import org.talend.sdk.component.maven.api.Audience;

import lombok.Data;

@Audience(TALEND_INTERNAL)
@Mojo(name = "image", defaultPhase = PACKAGE, threadSafe = true, requiresDependencyResolution = TEST)
public class ImageM2Mojo extends BuildComponentM2RepositoryMojo {

    @Parameter(property = "talend-image.fromImage", defaultValue = "openjdk:8-jre-alpine")
    private String fromImage;

    @Parameter(property = "talend-image.creationTime")
    private String creationTime;

    @Parameter(property = "talend-image.workingDirectory", defaultValue = "/opt/talend")
    private String workingDirectory;

    @Parameter(property = "talend-image.toImage")
    private String toImage;

    @Parameter(property = "talend-image.repository")
    private String repository;

    @Parameter(property = "talend-image.versionProperty")
    private String versionProperty;

    @Parameter(property = "talend-image.mainDependenciesScope", defaultValue = "compile")
    private String mainDependenciesScope;

    @Parameter
    private Map<String, String> labels;

    @Parameter
    private Map<String, String> environment;

    @Parameter
    private List<String> entryPoint;

    @Parameter(property = "talend-image.layersCacheDirectory",
            defaultValue = "${project.build.directory}/maven/build/cache")
    private File layersCacheDirectory;

    @Parameter(property = "talend-image.dockerExecutable")
    private File dockerExecutable;

    @Parameter(property = "talend-image.laggyPushWorkaroundRetries", defaultValue = "0")
    private int laggyPushWorkaround;

    @Parameter
    private Map<String, String> dockerEnvironment;

    @Parameter
    private List<Integer> ports; // useful for debugging, not for default delivery

    @Parameter(property = "talend-image.mainLibFolder", defaultValue = "main-libs")
    private String mainLibFolder;

    @Parameter(defaultValue = "${session}", readonly = true)
    private MavenSession session;

    @Component
    private SettingsDecrypter settingsDecrypter;

    @Override
    public void doExecute() throws MojoExecutionException {
        final ExecutorService executor = Executors.newCachedThreadPool(new ThreadFactory() {

            private final AtomicInteger id = new AtomicInteger(1);

            @Override
            public Thread newThread(final Runnable runnable) {
                return new Thread(runnable, String.format("%s %d", getClass().getName() + "-", id.getAndIncrement()));
            }
        });
        try {
            final JibContainerBuilder builder = Jib.from(ImageReference.parse(fromImage));
            builder
                    .setCreationTime(creationTime == null || creationTime.trim().isEmpty() ? Instant.now()
                            : Instant.parse(creationTime));
            builder
                    .setWorkingDirectory(AbsoluteUnixPath
                            .get(workingDirectory == null ? createWorkingDirectory() : workingDirectory));
            if (environment != null) {
                builder.setEnvironment(environment);
            }

            final String projectVersion = project.getVersion();
            final String tag = projectVersion.endsWith("-SNAPSHOT")
                    ? projectVersion.replace("-SNAPSHOT", "") + "_"
                            + ofNullable(project.getProperties().getProperty("git.branch"))
                                    .map(it -> it.replace('/', '_') + '_')
                                    .orElse("")
                            + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date())
                    : projectVersion;
            final String image = toImage == null ? project.getArtifactId() : toImage;
            final String imageName =
                    ((repository == null || repository.trim().isEmpty()) ? "" : (repository + '/')) + image + ':' + tag;

            if (labels != null) {
                builder
                        .setLabels(labels
                                .entrySet()
                                .stream()
                                .peek(it -> it.setValue(it.getValue().replace("@imageName@", imageName)))
                                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue)));
            }
            addLayers(builder);

            if (ports != null) {
                ports.stream().map(Port::tcp).forEach(builder::addExposedPort);
            }

            getLog().info("Creating the image (can be long)");

            if (repository != null) { // push
                if (laggyPushWorkaround > 0) {
                    toLocalDocker(executor, builder, tag, imageName);

                    hackyPush(tag, imageName);
                } else {
                    final RegistryImage registryImage = RegistryImage.named(imageName);
                    Server credentials = session.getSettings().getServer(repository);
                    if (credentials != null) {
                        credentials =
                                ofNullable(settingsDecrypter.decrypt(new DefaultSettingsDecryptionRequest(credentials)))
                                        .map(SettingsDecryptionResult::getServer)
                                        .orElse(credentials);
                        registryImage.addCredential(credentials.getUsername(), credentials.getPassword());
                    }
                    builder.containerize(configureContainer(Containerizer.to(registryImage), executor));
                    getLog().info("Pushed image='" + imageName + "', tag='" + tag + "'");
                }
            } else {
                toLocalDocker(executor, builder, tag, imageName);
            }

            if (versionProperty != null) {
                final String repo = repository == null ? "" : repository;
                project.getProperties().put(versionProperty, imageName);
                project.getProperties().put(versionProperty + ".repository", repo);
                project.getProperties().put(versionProperty + ".repositoryPrefixed", repo.isEmpty() ? "" : repo + '/');
                project.getProperties().put(versionProperty + ".image", image);
                project.getProperties().put(versionProperty + ".version", tag);
            }
        } catch (final Exception e) {
            throw new MojoExecutionException(e.getMessage(), e);
        } finally {
            executor.shutdownNow();
            if (m2Root.exists()) {
                try {
                    Files.walkFileTree(m2Root.toPath(), new SimpleFileVisitor<Path>() {

                        @Override
                        public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs)
                                throws IOException {
                            file.toFile().delete();
                            return super.visitFile(file, attrs);
                        }

                        @Override
                        public FileVisitResult postVisitDirectory(final Path dir, final IOException exc)
                                throws IOException {
                            dir.toFile().delete();
                            return super.postVisitDirectory(dir, exc);
                        }
                    });
                } catch (final IOException e) {
                    getLog().warn(e.getMessage(), e);
                }
            }
        }
    }

    // until jib supports retries this is a CI workaround
    private void hackyPush(final String tag, final String imageName)
            throws IOException, InterruptedException, MojoExecutionException {
        getLog()
                .warn("Using push workaround for nasty registries (using exec directly on 'docker'), "
                        + "it is highly recommended to not use laggyPushWorkaround configuration if you can");

        Server credentials = session.getSettings().getServer(repository);
        credentials = ofNullable(credentials)
                .map(it -> settingsDecrypter.decrypt(new DefaultSettingsDecryptionRequest(it)))
                .map(SettingsDecryptionResult::getServer)
                .orElse(credentials);
        if (credentials != null) {
            final File credFile =
                    new File(project.getBuild().getDirectory(), "talend-component-docker-credentials.temp");
            credFile.getParentFile().mkdirs(); // should be useless but just in case
            try (final FileWriter writer = new FileWriter(credFile)) {
                writer.write(credentials.getPassword());
            }
            try {
                final ProcessBuilder processBuilder = new ProcessBuilder("docker", "login", repository, "--username",
                        credentials.getUsername(), "--password-stdin");
                processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT);
                processBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
                processBuilder.redirectInput(ProcessBuilder.Redirect.from(credFile));
                final int exitCode = processBuilder.start().waitFor();
                if (exitCode != 0) {
                    getLog().warn("Can't login, got status: " + exitCode);
                }
            } finally {
                if (!credFile.delete()) {
                    credFile.deleteOnExit();
                }
            }
        }

        boolean ok = false;
        for (int i = 0; i < laggyPushWorkaround; i++) {
            final int exit = new ProcessBuilder("docker", "push", imageName).inheritIO().start().waitFor();
            if (exit == 0) {
                ok = true;
                getLog().info("Pushed image='" + imageName + "', tag='" + tag + "'");
                break;
            } else {
                getLog().warn("Push #" + (i + 1) + " got " + exit + " exit status");
            }
        }
        if (!ok) {
            throw new MojoExecutionException("Push didn't succeed");
        }
    }

    private void toLocalDocker(final ExecutorService executor, final JibContainerBuilder builder, final String tag,
            final String imageName) throws InvalidImageReferenceException, InterruptedException, ExecutionException,
            IOException, CacheDirectoryCreationException {
        final DockerDaemonImage docker = DockerDaemonImage.named(imageName);
        if (dockerEnvironment != null) {
            docker.setDockerEnvironment(dockerEnvironment);
        }
        if (dockerExecutable != null) {
            docker.setDockerExecutable(dockerExecutable.toPath());
        }
        builder.containerize(configureContainer(Containerizer.to(docker), executor));
        getLog().info("Built local image='" + imageName + "', tag='" + tag + "'");
    }

    private Containerizer configureContainer(final Containerizer to, final ExecutorService executor) {
        return to
                .setExecutorService(executor)
                .setApplicationLayersCache(layersCacheDirectory.toPath())
                .setBaseImageLayersCache(layersCacheDirectory.toPath())
                .setToolName("Talend Image Maven Plugin")
                .setEventHandlers(new EventHandlers());
    }

    private String createWorkingDirectory() {
        final String wd = "/opt/talend/" + project.getArtifactId().replace("-docker", "");
        getLog().info("Automatic working directory set to '" + wd + "', set <workingDirectory> to force its value");
        return wd;
    }

    // we create these layers - note that docker does not allow more than 128 layers
    // 1. one layer per component stack (not including the component module code)
    // 2. one layer with all our components
    // 3. one layer for the main dependencies
    // 4. one layer for the main
    private void addLayers(final JibContainerBuilder builder) {
        final Set<Artifact> components = getComponentArtifacts();
        final Set<Artifact> cars = getComponentsCar(components);

        // 1
        final List<String> coordinates = cars.stream().map(car -> {
            final LayerConfiguration.Builder layerBuilder = LayerConfiguration.builder();
            layerBuilder.setName(car.getArtifactId() + " component stack");
            final AtomicLong size = new AtomicLong();
            final String gav = copyComponentDependencies(car, (entry, read) -> {
                final String depPath = entry.getName().substring("MAVEN-INF/repository/".length());
                final File src = copyFile(entry, read, depPath);
                size.addAndGet(src.length());
                layerBuilder
                        .addEntry(src.toPath().toAbsolutePath(),
                                AbsoluteUnixPath.get(workingDirectory).resolve(depPath));
            });
            return gav == null ? null : new Layer(layerBuilder.build(), size.get(), gav);
        })
                .filter(Objects::nonNull)
                .distinct()
                .sorted(comparing(Layer::getSize).reversed())
                .peek(it -> builder.addLayer(it.layerConfiguration))
                .peek(it -> getLog().info("Prepared layer for '" + it.gav + "' dependencies (" + toSize(it.size) + ")"))
                .map(it -> it.gav)
                .collect(toList());

        // 2
        final LayerConfiguration.Builder componentsLayerBuilder = LayerConfiguration
                .builder()
                .setName("Components " + components.stream().sorted(comparing(Artifact::toString)).collect(toList()));
        final AtomicLong componentSize = new AtomicLong();
        components.forEach(it -> {
            final Path from = it.getFile().toPath().toAbsolutePath();
            componentSize.addAndGet(it.getFile().length());
            componentsLayerBuilder
                    .addEntry(from,
                            AbsoluteUnixPath
                                    .get(workingDirectory)
                                    .resolve(repositorySystemSession
                                            .getLocalRepository()
                                            .getBasedir()
                                            .toPath()
                                            .toAbsolutePath()
                                            .relativize(from)));
        });
        // the registry (only depends on components so belongs to this layer)
        writeRegistry(getNewComponentRegistry(coordinates));
        final File registryLocation = getRegistry();
        componentsLayerBuilder
                .addEntry(registryLocation.toPath().toAbsolutePath(),
                        AbsoluteUnixPath.get(workingDirectory).resolve(registryLocation.getName()));
        builder.addLayer(componentsLayerBuilder.build());
        getLog()
                .info("Prepared layer for components " + cars.toString().replace(":car", "") + " ("
                        + toSize(componentSize.get()) + ")");

        // finally add the project binary
        if (project.getArtifact() != null && project.getArtifact().getFile() != null
                && !"pom".equals(project.getArtifact().getType())) {
            // 3
            final LayerConfiguration.Builder dependenciesLayer =
                    LayerConfiguration.builder().setName("Main Dependencies");
            final Path path = project.getArtifact().getFile().toPath().toAbsolutePath();
            final AbsoluteUnixPath mainLibs =
                    mainLibFolder != null && !mainLibFolder.trim().isEmpty() && mainLibFolder.startsWith("/")
                            ? AbsoluteUnixPath.get(mainLibFolder)
                            : AbsoluteUnixPath.get(workingDirectory).resolve(mainLibFolder);
            final AtomicLong mainDepSize = new AtomicLong();
            final List<String> classpath = project
                    .getArtifacts()
                    .stream()
                    .filter(it -> mainDependenciesScope == null
                            || mainDependenciesScope.equalsIgnoreCase(it.getScope()))
                    .map(it -> {
                        final Path dep = it.getFile().toPath().toAbsolutePath();
                        final String relativized = repositorySystemSession
                                .getLocalRepository()
                                .getBasedir()
                                .toPath()
                                .toAbsolutePath()
                                .relativize(dep)
                                .toString();
                        mainDepSize.addAndGet(it.getFile().length());
                        final AbsoluteUnixPath target = mainLibs.resolve(relativized.replace(File.separatorChar, '/'));
                        dependenciesLayer.addEntry(dep, target);
                        return target.toString();
                    })
                    .collect(toList());
            builder.addLayer(dependenciesLayer.build());
            getLog().info("Prepared layer for main dependencies (" + toSize(mainDepSize.get()) + ")");

            // 4
            final AbsoluteUnixPath mainPath = mainLibs.resolve(path.getFileName());
            classpath.add(mainPath.toString());
            builder
                    .addLayer(LayerConfiguration
                            .builder()
                            .setName(project.getArtifactId() + " @" + project.getVersion())
                            .addEntry(path, mainPath)
                            .build());
            getLog().info("Prepared layer for main artifact (" + toSize(path.toFile().length()) + ")");

            if (getLog().isDebugEnabled()) {
                getLog().debug("> classpath=" + classpath);
            }

            // finally set the entry point if we have one
            if (entryPoint != null) {
                final String cp = String.join(":", classpath);
                final List<String> newEntrypoint =
                        entryPoint.stream().map(it -> replaceEntrypointPlaceholders(it, cp)).collect(toList());
                builder.setEntrypoint(newEntrypoint);
                if (getLog().isDebugEnabled()) {
                    getLog().debug("Entrypoint set to " + newEntrypoint);
                }
            }
        } else {
            getLog().info("No artifact attached to this project");
        }
    }

    private String toSize(final long size) {
        final Size b = new Size(size, Size.SizeUnit.BYTES);
        final Size mb = b.to(Size.SizeUnit.MEGABYTES);
        Size kb = b.to(Size.SizeUnit.KILOBYTES);
        if (mb.size > 0) {
            return mb.toString();
        }
        if (kb.size > 0) {
            return kb.toString();
        }
        return b.toString();
    }

    private String replaceEntrypointPlaceholders(final String it, final String cp) {
        return it.replace("@classpath@", cp);
    }

    // from tomitribe-util
    public static class Size {

        private final long size;

        private final SizeUnit unit;

        public Size(final long size, final SizeUnit unit) {
            this.size = size;
            this.unit = unit;
        }

        public Size to(final SizeUnit unit) {
            return new Size(unit.convert(this.size, this.unit), unit);
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            sb.append(size);
            if (unit != null) {
                sb.append(" ");
                sb.append(unit.name().toLowerCase(ENGLISH));
            }
            return sb.toString();
        }

        public enum SizeUnit {
            BYTES {

                @Override
                public long toBytes(final long s) {
                    return s;
                }

                @Override
                public long toKilobytes(final long s) {
                    return s / (B1 / B0);
                }

                @Override
                public long toMegabytes(final long s) {
                    return s / (B2 / B0);
                }

                @Override
                public long convert(final long s, final SizeUnit u) {
                    return u.toBytes(s);
                }

                @Override
                public String toString() {
                    return "b";
                }
            },

            KILOBYTES {

                @Override
                public long toBytes(final long s) {
                    return x(s, B1 / B0, MAX_VALUE / (B1 / B0));
                }

                @Override
                public long toKilobytes(final long s) {
                    return s;
                }

                @Override
                public long toMegabytes(final long s) {
                    return s / (B2 / B1);
                }

                @Override
                public long convert(final long s, final SizeUnit u) {
                    return u.toKilobytes(s);
                }

                @Override
                public String toString() {
                    return "kb";
                }
            },

            MEGABYTES {

                @Override
                public long toBytes(final long s) {
                    return x(s, B2 / B0, MAX_VALUE / (B2 / B0));
                }

                @Override
                public long toKilobytes(final long s) {
                    return x(s, B2 / B1, MAX_VALUE / (B2 / B1));
                }

                @Override
                public long toMegabytes(final long s) {
                    return s;
                }

                @Override
                public long convert(final long s, final SizeUnit u) {
                    return u.toMegabytes(s);
                }

                @Override
                public String toString() {
                    return "mb";
                }
            };

            private static final long B0 = 1L;

            private static final long B1 = B0 * 1024L;

            private static final long B2 = B1 * 1024L;

            private static long x(final long d, final long m, final long over) {
                if (d > over) {
                    return MAX_VALUE;
                }
                if (d < -over) {
                    return Long.MIN_VALUE;
                }
                return d * m;
            }

            public abstract long toBytes(long size);

            public abstract long toKilobytes(long size);

            public abstract long toMegabytes(long size);

            public abstract long convert(long sourceSize, SizeUnit sourceUnit);
        }
    }

    @Data
    private static class Layer {

        private final LayerConfiguration layerConfiguration;

        private final long size;

        private final String gav;
    }
}
