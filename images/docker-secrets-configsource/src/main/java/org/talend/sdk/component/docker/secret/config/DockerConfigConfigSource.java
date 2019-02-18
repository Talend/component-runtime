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
package org.talend.sdk.component.docker.secret.config;

import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toMap;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.eclipse.microprofile.config.spi.ConfigSource;

public class DockerConfigConfigSource extends BaseConfigSource implements ConfigSource {

    public DockerConfigConfigSource() {
        this(InternalConfig.get(DockerConfigConfigSource.class.getName() + ".base", "/"),
                Integer.parseInt(InternalConfig.get(DockerConfigConfigSource.class.getName() + ".ordinal", "100")),
                Stream
                        .of(InternalConfig.get(DockerConfigConfigSource.class.getName() + ".prefixes", "").split(","))
                        .map(String::trim)
                        .filter(it -> !it.isEmpty())
                        .toArray(String[]::new));
    }

    public DockerConfigConfigSource(final String base, final int ordinal, final String... prefixes) {
        super(() -> reload(base, prefixes), ordinal);
    }

    @Override
    public String getName() {
        return "docker-configs";
    }

    private static Map<String, String> reload(final String base, final String... prefixes) {
        final Path from = Paths.get(base);
        if (!Files.exists(from)) {
            return emptyMap();
        }
        final Predicate<Path> matches =
                // if no prefix ensure it is not default unix folders or not supported config files
                prefixes.length == 0
                        ? path -> !Files.isDirectory(path) && Stream
                                .of(".xml", ".properties", ".yml", ".yaml", ".so", ".json", ".old", ".img", "vmlinuz",
                                        "core")
                                .noneMatch(ext -> path.getFileName().toString().endsWith(ext))
                        : path -> Stream
                                .of(prefixes)
                                .anyMatch(prefix -> path.getFileName().toString().startsWith(prefix));
        try {
            return Files
                    .list(from)
                    .filter(matches)
                    .map(path -> new AbstractMap.SimpleEntry<>(path.getFileName().toString(), read(path)))
                    .filter(e -> e.getValue() != null)
                    .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String read(final Path path) {
        try {
            return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        } catch (final Exception e) {
            return null;
        }
    }
}
