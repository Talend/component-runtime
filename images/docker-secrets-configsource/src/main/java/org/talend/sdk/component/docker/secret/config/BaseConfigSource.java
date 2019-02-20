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

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import java.io.IOException;
import java.io.StringReader;
import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.Supplier;
import java.util.logging.Logger;

import org.eclipse.microprofile.config.spi.ConfigSource;

abstract class BaseConfigSource implements ConfigSource {

    private final Supplier<Map<String, String>> loader;

    private final int ordinal;

    private final Clock clock = Clock.systemUTC();

    private final long updateInterval;

    private final Map<String, String> mapping;

    private volatile Map<String, String> entries;

    private volatile long lastUpdate = -1;

    BaseConfigSource(final Supplier<Map<String, String>> loader, final int ordinal) {
        this.loader = loader;
        this.ordinal = ordinal;
        this.updateInterval = Long.parseLong(InternalConfig.get(getClass().getName() + ".updateInterval", "10000"));
        this.mapping = asMap(InternalConfig.get(getClass().getName() + ".keyMapping", ""));
        doLoad();
    }

    @Override
    public int getOrdinal() {
        return ordinal;
    }

    @Override
    public Map<String, String> getProperties() {
        reloadIfNeeded();
        return new HashMap<>(entries);
    }

    @Override
    public String getValue(final String propertyName) {
        reloadIfNeeded();
        return entries.get(propertyName);
    }

    private Map<String, String> asMap(final String props) {
        final Properties properties = new Properties();
        try (final StringReader reader = new StringReader(props)) {
            properties.load(reader);
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
        return properties.stringPropertyNames().stream().collect(toMap(identity(), properties::getProperty));
    }

    private void doLoad() {
        lastUpdate = clock.millis(); // first to avoid concurrent updates
        final Map<String, String> entries = loader.get();
        if (mapping.isEmpty()) {
            this.entries = entries;
        } else {
            this.entries = entries
                    .entrySet()
                    .stream()
                    .collect(toMap(it -> mapping.getOrDefault(it.getKey(), it.getKey()), Map.Entry::getValue));
        }
    }

    private void reloadIfNeeded() {
        if (clock.millis() - lastUpdate > updateInterval) {
            final long start = clock.millis();
            doLoad();
            final long end = clock.millis();
            final long duration = end - start;
            if (duration > updateInterval) {
                Logger
                        .getLogger(getClass().getName())
                        .warning(() -> "Reloading the configuration took more than expected: " + duration + "ms");
            }
        }
    }
}
