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
package org.talend.sdk.component.server.extension.stitch.server.configuration;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

import org.eclipse.microprofile.config.spi.ConfigSource;

public class CommandMappingSource implements ConfigSource {

    private final String value;

    public CommandMappingSource() {
        try (final BufferedReader reader = findCommandMapping()) {
            if (reader != null) {
                this.value = reader.lines().collect(joining("\n"));
            } else {
                this.value = null;
            }
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private BufferedReader findCommandMapping() {
        return ofNullable(System.getProperty("talend.stitch.service.command.mapping.location"))
                .map(Paths::get)
                .filter(Files::exists)
                .map(it -> {
                    try {
                        return Files.newBufferedReader(it);
                    } catch (final IOException e) {
                        throw new IllegalStateException(e);
                    }
                })
                .orElseGet(() -> ofNullable(Thread.currentThread().getContextClassLoader())
                        .map(loader -> loader
                                .getResourceAsStream("TALEND-INF/talend.stitch.service.command.mapping.properties"))
                        .map(stream -> new BufferedReader(new InputStreamReader(stream)))
                        .orElse(null));
    }

    @Override
    public Map<String, String> getProperties() {
        return value != null ? singletonMap("talend.stitch.service.command.mapping", value) : emptyMap();
    }

    @Override
    public String getValue(final String propertyName) {
        return "talend.stitch.service.command.mapping".equals(propertyName) ? value : null;
    }

    @Override
    public String getName() {
        return "talend.stitch.service.command.mapping";
    }
}
