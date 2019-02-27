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
package org.talend.sdk.component.server.extension.stitch.server;

import static java.util.Arrays.asList;
import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class ProcessCommandMapper {

    @Inject
    private Config config;

    @Inject
    @ConfigProperty(name = "talend.stitch.service.command.mapping", defaultValue = "")
    private String mappingConfig;

    private Map<String, List<String>> mapping;

    @PostConstruct
    private void init() {
        try (final StringReader reader = new StringReader(mappingConfig)) {
            final Properties properties = new Properties();
            properties.load(reader);
            mapping = properties
                    .stringPropertyNames()
                    .stream()
                    .collect(toMap(identity(),
                            it -> Stream.of(properties.getProperty(it).split(" ")).collect(toList())));
        } catch (final IOException e) {
            throw new IllegalArgumentException(e);
        }
        mapping.putIfAbsent("_default", asList("${tap}", "-c", "${configurationFile}"));
        log.info("Command mapping: {}", mapping);
    }

    public List<String> toCommand(final String tap, final String configPath) {
        return ofNullable(mapping.get(tap)).orElseGet(() -> mapping.get("_default")).stream().map(it -> {
            switch (it) {
            case "${configurationFile}":
                return configPath;
            case "${tap}":
                return tap;
            default:
                if (it.startsWith("${") && it.endsWith("}")) {
                    return config.getOptionalValue(it.substring(2, it.length() - 1), String.class).orElse(it);
                }
                return it;
            }
        }).collect(toList());
    }
}
