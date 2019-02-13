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
import java.util.Map;

import org.eclipse.microprofile.config.spi.ConfigSource;

public class DockerSecretConfigSource implements ConfigSource {

    private final Map<String, String> entries;

    private final int ordinal;

    public DockerSecretConfigSource() {
        this(System.getProperty("talend.docker.secrets.base", "/run/secrets"),
                Integer.getInteger("talend.docker.secrets.ordinal", 100));
    }

    public DockerSecretConfigSource(final String base, final int ordinal) {
        this.ordinal = ordinal;

        final Path from = Paths.get(base);
        if (!Files.exists(from)) {
            entries = emptyMap();
        } else {
            try {
                entries = Files.list(from).collect(toMap(it -> it.getFileName().toString(), it -> {
                    try {
                        return new String(Files.readAllBytes(it), StandardCharsets.UTF_8);
                    } catch (final IOException e) {
                        throw new IllegalStateException(e);
                    }
                }));
            } catch (final IOException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    @Override
    public int getOrdinal() {
        return ordinal;
    }

    @Override
    public Map<String, String> getProperties() {
        return entries;
    }

    @Override
    public String getValue(final String propertyName) {
        return entries.get(propertyName);
    }

    @Override
    public String getName() {
        return "docker-secrets";
    }
}
