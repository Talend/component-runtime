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

public class DockerSecretConfigSource extends BaseConfigSource implements ConfigSource {

    public DockerSecretConfigSource() {
        this(InternalConfig.get(DockerSecretConfigSource.class.getName() + ".base", "/run/secrets"),
                Integer.parseInt(InternalConfig.get(DockerSecretConfigSource.class.getName() + ".ordinal", "100")));
    }

    public DockerSecretConfigSource(final String base, final int ordinal) {
        super(() -> reload(base), ordinal);
    }

    @Override
    public String getName() {
        return "docker-secrets";
    }

    private static Map<String, String> reload(final String base) {
        final Path from = Paths.get(base);
        if (!Files.exists(from)) {
            return emptyMap();
        }
        try {
            return Files.list(from).collect(toMap(it -> it.getFileName().toString(), it -> {
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
