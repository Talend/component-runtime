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
package org.talend.sdk.component.remoteengine.customizer.lang;

import static java.util.stream.Collectors.toList;
import static lombok.AccessLevel.PRIVATE;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Properties;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor(access = PRIVATE)
public final class IO {

    public static Properties loadProperties(final Path env) throws IOException {
        final Properties filtering = new Properties();
        if (Files.exists(env)) {
            try (final BufferedReader reader = Files.newBufferedReader(env)) {
                filtering.load(reader);
            }
        }
        return filtering;
    }

    public static Properties loadProperties(final String value) {
        final Properties properties = new Properties();
        if (value != null) {
            try (final BufferedReader reader = new BufferedReader(new StringReader(value))) {
                properties.load(reader);
            } catch (final IOException e) {
                throw new IllegalStateException(e);
            }
        }
        return properties;
    }

    public static List<String> readFile(final Path dockerCompose) throws IOException {
        if (!Files.exists(dockerCompose)) {
            throw new IllegalArgumentException("Missing file: " + dockerCompose);
        }
        try (final BufferedReader reader = Files.newBufferedReader(dockerCompose)) {
            return reader.lines().collect(toList());
        }
    }

    public static AutoCloseable autoDir(final Path workDir) throws IOException {
        final boolean exists = Files.exists(workDir);
        if (exists) {
            return () -> {
            };
        }
        log.debug("Creating '{}'", workDir);
        Files.createDirectories(workDir);
        return () -> Files.walkFileTree(workDir, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return super.visitFile(file, attrs);
            }

            @Override
            public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) throws IOException {
                Files.delete(dir);
                if (dir.equals(workDir)) {
                    log.debug("Deleted '{}'", dir);
                }
                return super.postVisitDirectory(dir, exc);
            }
        });
    }
}
