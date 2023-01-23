/**
 * Copyright (C) 2006-2023 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.starter.server.configuration;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.enterprise.inject.Vetoed;

import org.eclipse.microprofile.config.spi.ConfigSource;

@Vetoed
public class ScmConfigurationLoader implements ConfigSource {

    private final Map<String, String> map = new HashMap<>();

    private volatile boolean init = false;

    private void ensureInit() {
        if (init) {
            return;
        }
        synchronized (this) {
            if (init) {
                return;
            }
            try (final InputStream stream =
                    Thread.currentThread().getContextClassLoader().getResourceAsStream("TALEND-INF/git.properties")) {
                final Properties properties = new Properties() {

                    {
                        try {
                            load(stream);
                        } catch (final IOException e) {
                            throw new IllegalStateException(e);
                        }
                    }
                };
                properties
                        .stringPropertyNames()
                        .stream()
                        .collect(() -> map, (m, k) -> m.put(k, properties.getProperty(k)), Map::putAll);
            } catch (final IOException e) {
                throw new IllegalStateException(e);
            }
            init = true;
        }
    }

    @Override
    public Map<String, String> getProperties() {
        ensureInit();
        return map;
    }

    @Override
    public Set<String> getPropertyNames() {
        ensureInit();
        return map.keySet();
    }

    @Override
    public int getOrdinal() {
        return 1000;
    }

    @Override
    public String getValue(final String propertyName) {
        ensureInit();
        return map.get(propertyName);
    }

    @Override
    public String getName() {
        return "component-scm-configuration";
    }
}
