/**
 * Copyright (C) 2006-2022 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.remoteengine.customizer.service;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.stream.Collector;

import com.google.cloud.tools.jib.api.LayerConfiguration;
import com.google.cloud.tools.jib.api.LayerEntry;
import com.google.cloud.tools.jib.api.buildplan.AbsoluteUnixPath;
import com.google.cloud.tools.jib.api.buildplan.FilePermissions;

import org.talend.sdk.component.remoteengine.customizer.lang.IO;

import lombok.Data;

public class ConnectorLoader {

    public ConnectorLayer createConnectorLayer(final AbsoluteUnixPath rootContainerPath, final Path workDir,
            final Path car) {
        String gav = null;
        final Map<String, Path> toCopy = new HashMap<>();
        try (final JarInputStream jarInputStream = new JarInputStream(Files.newInputStream(car))) {
            JarEntry entry;
            while ((entry = jarInputStream.getNextJarEntry()) != null) {
                if ("TALEND-INF/metadata.properties".equals(entry.getName())) {
                    // don't close, it is the jar!
                    gav = IO
                            .loadProperties(new BufferedReader(new InputStreamReader(jarInputStream))
                                    .lines()
                                    .collect(joining("\n")))
                            .getProperty("component_coordinates");
                    continue;
                }
                if (!entry.getName().startsWith("MAVEN-INF/repository/")) {
                    continue;
                }
                final String relativeName = entry.getName().substring("MAVEN-INF/repository/".length());
                final Path local = workDir.resolve(relativeName);
                if (entry.isDirectory()) {
                    if (!Files.exists(local)) {
                        Files.createDirectories(local);
                    }
                } else {
                    if (local.getParent() != null && !Files.exists(local.getParent())) {
                        Files.createDirectories(local.getParent());
                    }
                    Files.copy(jarInputStream, local);
                    if (entry.getLastModifiedTime() != null) {
                        Files.setLastModifiedTime(local, entry.getLastModifiedTime());
                    }
                    toCopy.put(relativeName, local);
                }
            }
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
        return new ConnectorLayer(
                toCopy.entrySet().stream().collect(Collector.of(LayerConfiguration::builder, (builder, entry) -> {
                    try {
                        builder
                                .addEntry(new LayerEntry(entry.getValue(), rootContainerPath.resolve(entry.getKey()),
                                        FilePermissions.DEFAULT_FILE_PERMISSIONS,
                                        Instant.ofEpochMilli(Files.getLastModifiedTime(entry.getValue()).toMillis())));
                    } catch (final IOException e) {
                        throw new IllegalStateException(e);
                    }
                }, (builder1, builder2) -> {
                    builder2.build().getLayerEntries().forEach(builder1::addEntry);
                    return builder1;
                })).build(), toCopy,
                requireNonNull(gav, "GAV was not found in '" + car + "', ensure it is a valid component archive."));
    }

    @Data
    public static class ConnectorLayer {

        private final LayerConfiguration layer;

        private final Map<String, Path> dependencies;

        private final String gav;
    }
}
