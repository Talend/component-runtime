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
package org.talend.sdk.component.tools;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.talend.sdk.component.dependencies.maven.MvnCoordinateToFileConverter;
import org.talend.sdk.component.tools.exec.CarMain;
import org.talend.sdk.component.tools.exec.Versions;

import lombok.Data;

/**
 * Defines how to bundle a standard component archive.
 * Currently the layout is the following one:
 * - TALEND-INF/lib/*.jar.
 * - TALEND-INF/metadata.properties.
 */
public class CarBundler implements Runnable {

    private final Configuration configuration;

    private final Log log;

    public CarBundler(final Configuration configuration, final Object log) {
        this.configuration = configuration;
        try {
            this.log = Log.class.isInstance(log) ? Log.class.cast(log) : new ReflectiveLog(log);
        } catch (final NoSuchMethodException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public void run() {
        final String date = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.now(ZoneId.of("UTC")));
        final Properties metadata = new Properties();
        metadata.put("date", date);
        metadata.put("version", ofNullable(configuration.version).orElse("NC"));
        metadata.put("CarBundlerVersion", Versions.KIT_VERSION);
        metadata
                .put("component_coordinates", ofNullable(configuration.mainGav)
                        .orElseThrow(() -> new IllegalArgumentException("No component coordinates specified")));
        metadata.put("type", ofNullable(configuration.type).orElse("connector"));
        if (configuration.getCustomMetadata() != null) {
            configuration.getCustomMetadata().forEach(metadata::setProperty);
        }

        configuration.getOutput().getParentFile().mkdirs();

        final Manifest manifest = new Manifest();
        manifest.getMainAttributes().putValue("Main-Class", CarMain.class.getName());
        manifest.getMainAttributes().putValue("Created-By", "Talend Component Kit Tooling");
        manifest.getMainAttributes().putValue("Manifest-Version", "1.0");
        manifest.getMainAttributes().putValue("Build-Date", date);

        try (final JarOutputStream zos = new JarOutputStream(
                new BufferedOutputStream(new FileOutputStream(configuration.getOutput())), manifest)) {
            final Collection<String> created = new HashSet<>();

            // folders
            Stream.of("TALEND-INF/", "META-INF/", "MAVEN-INF/", "MAVEN-INF/repository/").forEach(folder -> {
                try {
                    zos.putNextEntry(new JarEntry(folder));
                    zos.closeEntry();
                } catch (final IOException e) {
                    throw new IllegalStateException(e);
                }
                created.add(folder.substring(0, folder.length() - 1));
            });

            // libs
            final MvnCoordinateToFileConverter converter = new MvnCoordinateToFileConverter();
            configuration.getArtifacts().forEach((gav, file) -> {
                final String path = "MAVEN-INF/repository/" + converter.toArtifact(gav).toPath();
                try {
                    createFolders(zos, created, path.split("/"));

                    zos.putNextEntry(new JarEntry(path));
                    Files.copy(file.toPath(), zos);
                    zos.closeEntry();
                } catch (final IOException ioe) {
                    throw new IllegalStateException(ioe);
                }
            });

            // meta
            zos.putNextEntry(new JarEntry("TALEND-INF/metadata.properties"));
            metadata.store(zos, "Generated metadata by Talend Component Kit Car Bundle");
            zos.closeEntry();

            // executable
            final String main = CarMain.class.getName().replace('.', '/') + ".class";
            createFolders(zos, created, main.split("/"));
            zos.putNextEntry(new JarEntry(main));
            try (final InputStream stream = CarBundler.class.getClassLoader().getResourceAsStream(main)) {
                byte[] buffer = new byte[1024];
                int read;
                while ((read = stream.read(buffer)) >= 0) {
                    zos.write(buffer, 0, read);
                }
            }
            zos.closeEntry();
        } catch (final IOException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }

        log.info("Created " + configuration.getOutput());
    }

    private void createFolders(final JarOutputStream zos, final Collection<String> created, final String[] parts) {
        IntStream
                .range(0, parts.length - 1)
                .mapToObj(i -> Stream.of(parts).limit(i + 1).collect(joining("/")))
                .filter(p -> !created.contains(p))
                .peek(folder -> {
                    try {
                        zos.putNextEntry(new JarEntry(folder + '/'));
                        zos.closeEntry();
                    } catch (final IOException e) {
                        throw new IllegalStateException(e);
                    }
                })
                .forEach(created::add);
    }

    @Data
    public static class Configuration {

        private String mainGav;

        private String version;

        private Map<String, File> artifacts;

        private Map<String, String> customMetadata;

        private File output;

        private String type;
    }
}
