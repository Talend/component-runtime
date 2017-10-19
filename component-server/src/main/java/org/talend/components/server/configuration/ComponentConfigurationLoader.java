// ============================================================================
//
// Copyright (C) 2006-2017 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.components.server.configuration;

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
