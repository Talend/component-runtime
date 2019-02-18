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

import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

// just a way to have internal config for the sources in the app and not only system props
// we can't use Config here so back to the plain old singleton pattern
final class InternalConfig {

    private static final InternalConfig CONFIG = new InternalConfig();

    private final Map<String, String> config;

    private InternalConfig() {
        final Thread thread = Thread.currentThread();
        final ClassLoader old = thread.getContextClassLoader();
        thread.setContextClassLoader(InternalConfig.class.getClassLoader());
        try {
            final Properties properties = loadInternalProperties();
            config = properties.stringPropertyNames().stream().collect(toMap(identity(), properties::getProperty));
        } finally {
            thread.setContextClassLoader(old);
        }
    }

    static String get(final String key, final String defaultValue) {
        return ofNullable(CONFIG.config.get(key)).orElseGet(() -> System.getProperty(key, defaultValue));
    }

    private Properties loadInternalProperties() {
        final Properties properties = new Properties();
        final ClassLoader loader =
                ofNullable(Thread.currentThread().getContextClassLoader()).orElseGet(ClassLoader::getSystemClassLoader);
        final InputStream stream = loader.getResourceAsStream("META-INF/talend/docker/configuration.properties");
        if (stream != null) {
            try {
                properties.load(stream);
            } catch (final IOException e) {
                throw new IllegalStateException(e);
            } finally {
                try {
                    stream.close();
                } catch (final IOException e) {
                    // no-op
                }
            }
        }
        return properties;
    }
}
