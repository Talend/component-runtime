/**
 *  Copyright (C) 2006-2017 Talend Inc. - www.talend.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.talend.component.server.configuration;

import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.CDI;

import org.apache.deltaspike.core.api.config.Source;
import org.apache.deltaspike.core.spi.config.ConfigSource;
import org.apache.meecrowave.Meecrowave;
import org.apache.meecrowave.runner.cli.CliOption;

import lombok.Data;

@Source
@ApplicationScoped
public class ComponentConfigurationLoader implements ConfigSource {

    private final Map<String, String> map = new HashMap<>();

    @PostConstruct
    private void init() {
        final Meecrowave.Builder builder = CDI.current().select(Meecrowave.Builder.class).get();
        map.putAll(asMap(builder.getProperties()));
        ofNullable(builder.getExtension(Cli.class).getConfiguration()).ifPresent(configuration -> {
            final File file = new File(configuration);
            if (file.exists()) {
                try (final InputStream is = new FileInputStream(file)) {
                    map.putAll(load(is));
                } catch (final IOException e) {
                    throw new IllegalArgumentException(e);
                }
            } else {
                final ClassLoader loader = Thread.currentThread().getContextClassLoader();
                try (final InputStream is = loader.getResourceAsStream(configuration)) {
                    if (is != null) {
                        map.putAll(load(is));
                    }
                } catch (final IOException e) {
                    throw new IllegalArgumentException(e);
                }
            }
        });
    }

    @Override
    public Map<String, String> getProperties() {
        return map;
    }

    @Override
    public String getPropertyValue(final String key) {
        return getProperties().get(key);
    }

    @Override
    public String getConfigName() {
        return "component-configuration";
    }

    @Override
    public int getOrdinal() {
        return 1000;
    }

    @Override
    public boolean isScannable() {
        return true;
    }

    private Map<String, String> load(final InputStream is) throws IOException {
        final Properties properties = new Properties();
        properties.load(is);
        return asMap(properties);
    }

    private Map<String, String> asMap(final Properties properties) {
        return properties.stringPropertyNames().stream().collect(toMap(identity(), properties::getProperty));
    }

    @Data
    public static class Cli {

        @CliOption(name = "component-configuration", description = "The file containing application configuration")
        private String configuration;
    }
}
