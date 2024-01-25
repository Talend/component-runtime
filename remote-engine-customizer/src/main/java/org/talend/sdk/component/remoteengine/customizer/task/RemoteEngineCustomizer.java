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
package org.talend.sdk.component.remoteengine.customizer.task;

import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.talend.sdk.component.remoteengine.customizer.lang.Reflects.asAccessible;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Stream;

import com.google.cloud.tools.jib.api.Containerizer;
import com.google.cloud.tools.jib.api.ImageReference;
import com.google.cloud.tools.jib.api.InvalidImageReferenceException;
import com.google.cloud.tools.jib.api.Jib;
import com.google.cloud.tools.jib.api.JibContainerBuilder;
import com.google.cloud.tools.jib.api.LayerConfiguration;
import com.google.cloud.tools.jib.api.LogEvent;
import com.google.cloud.tools.jib.api.buildplan.AbsoluteUnixPath;
import com.google.cloud.tools.jib.api.buildplan.FilePermissions;
import com.google.cloud.tools.jib.api.buildplan.ImageFormat;
import com.google.cloud.tools.jib.blob.Blobs;
import com.google.cloud.tools.jib.builder.ProgressEventDispatcher;
import com.google.cloud.tools.jib.builder.steps.StepsRunner;
import com.google.cloud.tools.jib.configuration.BuildContext;
import com.google.cloud.tools.jib.configuration.ImageConfiguration;
import com.google.cloud.tools.jib.event.EventHandlers;
import com.google.cloud.tools.jib.image.Image;
import com.google.cloud.tools.jib.image.Layer;
import com.google.common.io.ByteStreams;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.talend.sdk.component.remoteengine.customizer.Versions;
import org.talend.sdk.component.remoteengine.customizer.lang.Hex;
import org.talend.sdk.component.remoteengine.customizer.lang.IO;
import org.talend.sdk.component.remoteengine.customizer.lang.PathFactory;
import org.talend.sdk.component.remoteengine.customizer.model.DockerConfiguration;
import org.talend.sdk.component.remoteengine.customizer.model.ImageType;
import org.talend.sdk.component.remoteengine.customizer.model.RegistryConfiguration;
import org.talend.sdk.component.remoteengine.customizer.service.ConnectorLoader;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RemoteEngineCustomizer {

    // CHECKSTYLE:OFF
    public void registerComponents(final String remoteEngineDirConf, final String workDirConf,
            final String cacheDirConf, final String baseImageConf, final String targetImageConf,
            final Collection<String> carPaths, final ImageType fromImageType, final ImageType targetImageType,
            final DockerConfiguration dockerConfiguration, final RegistryConfiguration registryConfiguration,
            final ConnectorLoader connectorLoader, final boolean updateOriginalFile) {
        // CHECKSTYLE:ON
        final Path remoteEngineDir =
                PathFactory.get(requireNonNull(remoteEngineDirConf, "Missing remote engine folder"));
        final Path workDir = PathFactory.get(workDirConf);
        final Path cacheDir = cacheDirConf.startsWith("${remote.engine.dir}/")
                ? remoteEngineDir.resolve(cacheDirConf.substring("${remote.engine.dir}/".length()))
                : PathFactory.get(cacheDirConf);
        final Collection<Path> cars = carPaths.stream().map(PathFactory::get).collect(toList());
        final List<Path> missingCars = cars.stream().filter(it -> !Files.exists(it)).collect(toList());
        if (!missingCars.isEmpty()) {
            throw new IllegalArgumentException("Missing component archives: " + missingCars);
        }

        try {
            final Properties filtering = IO.loadProperties(remoteEngineDir.resolve(".env"));
            final Path compose = remoteEngineDir.resolve("docker-compose.yml");
            final List<String> lines = IO.readFile(compose);
            final ImageAndLine connectorsImageRef = findImage(lines, "connectors");
            final String fromConnectorsImage = ofNullable(baseImageConf)
                    .filter(it -> !"auto".equals(it))
                    .orElseGet(() -> filterPlaceholders(filtering, connectorsImageRef.image));
            final String toConnectorsImage = ofNullable(targetImageConf)
                    .filter(it -> !"auto".equals(it))
                    .orElseGet(() -> timestampImage(fromConnectorsImage));
            final Containerizer targetContainer = targetImageType == ImageType.DOCKER
                    ? Containerizer.to(dockerConfiguration.toImage(toConnectorsImage))
                    : Containerizer.to(registryConfiguration.toImage(toConnectorsImage));
            log.info("Building image '{}' from '{}' adding {}", toConnectorsImage, fromConnectorsImage, cars);
            final ExecutorService executor =
                    Executors.newFixedThreadPool(Math.max(Runtime.getRuntime().availableProcessors(), 4));
            try (final AutoCloseable ignored = IO.autoDir(workDir)) {
                final Path registry = workDir.resolve("component-registry.properties");
                final Path registryDigest = workDir.resolve("component-registry-digest.properties");

                final AbsoluteUnixPath rootContainerPath = AbsoluteUnixPath.get("/opt/talend/connectors");
                final Instant now = Instant.now();
                final Collection<ConnectorLoader.ConnectorLayer> connectorsLayer = cars
                        .stream()
                        .map(it -> connectorLoader.createConnectorLayer(rootContainerPath, workDir, it))
                        .collect(toList());

                final Path baseCache = cacheDir.resolve("base");
                final Path appCache = cacheDir.resolve("application");

                log.info("Looking for component-registry.properties configuration, this can be a bit long...");
                final Image image;
                try {
                    image = loadImage(fromConnectorsImage, toConnectorsImage, executor, baseCache, appCache,
                            dockerConfiguration, fromImageType);
                } catch (final ExecutionException ee) {
                    log
                            .error("Please validate the connectors container image is an official one, "
                                    + "we don't support customizations on custom images or set the from image type");
                    throw ee;
                }
                final Map<String, Properties> propertiesContents = image
                        .getLayers()
                        .reverse()
                        .stream()
                        .map(it -> extractProperties(it,
                                Stream
                                        .of(registry, registryDigest)
                                        .map(f -> "opt/talend/connectors/" + f.getFileName())
                                        .collect(toSet())))
                        .filter(it -> !it.isEmpty())
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException(
                                "No layer containing the component registry in '" + fromConnectorsImage + "'"));

                final Properties componentProperties =
                        requireNonNull(propertiesContents.get("/opt/talend/connectors/component-registry.properties"),
                                "Missing component-registry.properties");
                connectorsLayer.forEach(c -> componentProperties.put(c.getGav().split(":")[1], c.getGav()));

                final Optional<Properties> digestProperties = ofNullable(
                        propertiesContents.get("/opt/talend/connectors/component-registry-digest.properties"));
                digestProperties
                        .ifPresent(
                                digests -> connectorsLayer.forEach(cl -> cl.getDependencies().forEach((key, path) -> {
                                    try (final DigestOutputStream out = new DigestOutputStream(
                                            ByteStreams.nullOutputStream(), MessageDigest.getInstance("SHA-512"))) {
                                        java.nio.file.Files.copy(path, out);
                                        out.flush();
                                        final byte[] digest = out.getMessageDigest().digest();
                                        if (digests.put(key, Hex.hex(digest)) != null) {
                                            log
                                                    .info("'{}' digest will be overriding existing entry (entry='{}')",
                                                            key, cl.getGav());
                                        }
                                    } catch (final NoSuchAlgorithmException | IOException e) {
                                        throw new IllegalStateException(e);
                                    }
                                })));
                try (final Writer writer = Files.newBufferedWriter(registry)) {
                    componentProperties.store(writer, "Generated by " + getClass().getName());
                }
                if (digestProperties.isPresent()) {
                    try (final Writer writer = Files.newBufferedWriter(registryDigest)) {
                        digestProperties
                                .orElseThrow(IllegalStateException::new)
                                .store(writer, "Generated by " + getClass().getName());
                    }
                }

                log.info("Building image '{}'", toConnectorsImage);
                final JibContainerBuilder from = from(fromImageType, dockerConfiguration, fromConnectorsImage);
                connectorsLayer.stream().map(ConnectorLoader.ConnectorLayer::getLayer).forEach(from::addLayer);
                from
                        .addLayer(LayerConfiguration
                                .builder()
                                .addEntry(registry, rootContainerPath.resolve(registry.getFileName().toString()),
                                        FilePermissions.DEFAULT_FILE_PERMISSIONS, now)
                                .addEntry(registryDigest,
                                        rootContainerPath.resolve(registryDigest.getFileName().toString()),
                                        FilePermissions.DEFAULT_FILE_PERMISSIONS, now)
                                .build())
                        .setCreationTime(now)
                        .containerize(targetContainer
                                .setToolName("Talend Component Kit Remote Engine Customizer " + Versions.VERSION)
                                .setExecutorService(executor)
                                .setBaseImageLayersCache(baseCache)
                                .setApplicationLayersCache(appCache));

                if (updateOriginalFile) {
                    rewriteCompose(remoteEngineDir, compose, lines, connectorsImageRef, toConnectorsImage);
                    log.info("Restart your remote engine to take into account the new connector image");
                } else {
                    log.info("You can update '{}' connectors container with image '{}'", compose, toConnectorsImage);
                }
            } finally {
                executor.shutdownNow();
                if (!executor.awaitTermination(5, SECONDS)) {
                    log.warn("Executor is not terminated but exiting since it is not critical");
                }
            }
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (final Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private void rewriteCompose(final Path remoteEngineDir, final Path compose, final List<String> lines,
            final ImageAndLine connectorsImageRef, final String toConnectorsImage) throws IOException {
        final String originalLine = lines.get(connectorsImageRef.line);
        final Path backupPath = remoteEngineDir
                .resolve(".remote_engine_customizer/backup/docker-compose." + toConnectorsImage.split(":")[1] + ".yml");
        if (!Files.exists(backupPath.getParent())) {
            Files.createDirectories(backupPath.getParent());
        }
        if (!Files.exists(backupPath)) {
            Files
                    .write(backupPath, String.join("\n", lines).getBytes(StandardCharsets.UTF_8),
                            StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        }
        lines
                .set(connectorsImageRef.line,
                        // keep indentation
                        originalLine.substring(0, originalLine.indexOf(originalLine.trim())) + "image: "
                                + toConnectorsImage);
        Files
                .write(compose, String.join("\n", lines).getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE,
                        StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private Image loadImage(final String from, final String to, final ExecutorService executor, final Path baseCache,
            final Path appCache, final DockerConfiguration dockerConfiguration, final ImageType fromImageType)
            throws Exception {
        final StepsRunner steps = StepsRunner
                .begin(BuildContext
                        .builder()
                        .setBaseImageConfiguration(createBaseImage(from, dockerConfiguration, fromImageType))
                        .setTargetFormat(ImageFormat.OCI)
                        .setTargetImageConfiguration(ImageConfiguration.builder(ImageReference.parse(to)).build())
                        .setToolName("Talend Component Kit Remote Engine Customizer " + Versions.VERSION)
                        .setExecutorService(executor)
                        .setBaseImageLayersCacheDirectory(baseCache)
                        .setApplicationLayersCacheDirectory(appCache)
                        .build());
        final String description = "Extracting registry properties";
        final Field rootProgressDescription =
                asAccessible(StepsRunner.class.getDeclaredField("rootProgressDescription"));
        rootProgressDescription.set(steps, description);
        final List<Runnable> stepsInstance =
                (List<Runnable>) asAccessible(StepsRunner.class.getDeclaredField("stepsToRun")).get(steps);
        try {
            asAccessible(StepsRunner.class.getDeclaredMethod("addRetrievalSteps", boolean.class)).invoke(steps, true);
        } catch (final IllegalAccessException | NoSuchMethodException e) {
            throw new IllegalStateException(e);
        } catch (final InvocationTargetException e) {
            throw new IllegalStateException(e.getTargetException());
        }
        Stream.of("buildAndCacheApplicationLayers", "buildImage").forEach(method -> {
            stepsInstance.add(() -> {
                try {
                    asAccessible(StepsRunner.class.getDeclaredMethod(method)).invoke(steps);
                } catch (final IllegalAccessException | NoSuchMethodException e) {
                    throw new IllegalStateException(e);
                } catch (final InvocationTargetException e) {
                    throw new IllegalStateException(e.getTargetException());
                }
            });
        });
        try (final ProgressEventDispatcher progressEventDispatcher =
                ProgressEventDispatcher.newRoot(EventHandlers.builder().add(LogEvent.class, le -> {
                    switch (le.getLevel()) {
                    case WARN:
                        log.warn(le.getMessage());
                        break;
                    case DEBUG:
                        log.debug(le.getMessage());
                        break;
                    case ERROR:
                        log.error(le.getMessage());
                        break;
                    case INFO:
                        log.error(le.getMessage());
                        break;
                    default:
                        log.info("(" + le.getLevel() + ") " + le.getMessage());
                        break;
                    }
                }).build(), description, stepsInstance.size())) {
            asAccessible(StepsRunner.class.getDeclaredField("rootProgressDispatcher"))
                    .set(steps, progressEventDispatcher);
            stepsInstance.forEach(Runnable::run);
        }
        final Object stepResult = asAccessible(StepsRunner.class.getDeclaredField("results")).get(steps);
        return ((Future<Image>) asAccessible(stepResult.getClass().getDeclaredField("builtImage")).get(stepResult))
                .get();
    }

    private JibContainerBuilder from(final ImageType fromImageType, final DockerConfiguration dockerConfiguration,
            final String from) throws InvalidImageReferenceException {
        if (isFromDockerDaemon(fromImageType, dockerConfiguration, from)) {
            return Jib.from(dockerConfiguration.toImage(from));
        }
        return Jib.from(ImageReference.parse(from));
    }

    // timestamp, assume it is cached locally
    private boolean isFromDockerDaemon(final ImageType fromImageType, final DockerConfiguration dockerConfiguration,
            final String img) {
        return dockerConfiguration != null && fromImageType != ImageType.REGISTRY && !img.contains(".")
                && !img.startsWith("docker://");
    }

    private ImageConfiguration createBaseImage(final String from, final DockerConfiguration dockerConfiguration,
            final ImageType fromImageType) throws InvalidImageReferenceException {
        if (isFromDockerDaemon(fromImageType, dockerConfiguration, from)) {
            return ImageConfiguration
                    .builder(ImageReference.parse(from))
                    .setDockerClient(dockerConfiguration.toClient())
                    .build();
        }
        return ImageConfiguration.builder(ImageReference.parse(from)).build();
    }

    private Map<String, Properties> extractProperties(final Layer it, final Collection<String> paths) {
        final Map<String, Properties> contents = new HashMap<>();
        try {
            final byte[] bytes = Blobs.writeToByteArray(it.getBlob());
            try (final TarArchiveInputStream tis =
                    new TarArchiveInputStream(new GzipCompressorInputStream(new ByteArrayInputStream(bytes)))) {
                TarArchiveEntry entry;
                while ((entry = tis.getNextTarEntry()) != null) {
                    if (entry.isFile() && paths.contains(entry.getName())) {
                        final Properties properties = new Properties();
                        properties.load(tis);
                        contents.put((!entry.getName().startsWith("/") ? "/" : "") + entry.getName(), properties);
                        if (paths.size() == contents.size()) {
                            return contents;
                        }
                    }
                }
            }
        } catch (final IOException ioe) {
            throw new IllegalStateException(ioe);
        }
        return contents;
    }

    private String timestampImage(final String fromConnectorsImage) {
        final int idx = fromConnectorsImage.lastIndexOf(":");
        final String timestamp = ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        if (idx <= 0) {
            return fromConnectorsImage + ':' + timestamp;
        }
        return fromConnectorsImage.substring(0, idx) + ':' + timestamp;
    }

    private String filterPlaceholders(final Properties variables, final String value) {
        return value.startsWith("${") && value.endsWith("}")
                ? variables.getProperty(value.substring("${".length(), value.length() - "}".length()), value)
                : value;
    }

    private ImageAndLine findImage(final List<String> lines, final String container) {
        boolean inConnectorsBlock = false;
        int prefixLen = -1;
        int lineIdx = 0;
        for (final String line : lines) {
            final String trimmed = line.trim();
            if (trimmed.equals(container + ':')) {
                prefixLen = line.indexOf(container);
                inConnectorsBlock = true;
            } else if (inConnectorsBlock) {
                final int offset = line.indexOf(trimmed);
                if (offset > prefixLen && trimmed.startsWith("image:")) {
                    return new ImageAndLine(trimmed.substring("image:".length()).trim(), lineIdx);
                } else if (offset <= prefixLen) {
                    prefixLen = -1;
                    inConnectorsBlock = false;
                }
            }
            lineIdx++;
        }
        throw new IllegalArgumentException("Missing connectors image");
    }

    @AllArgsConstructor
    private static class ImageAndLine {

        private final String image;

        private final int line;
    }
}
