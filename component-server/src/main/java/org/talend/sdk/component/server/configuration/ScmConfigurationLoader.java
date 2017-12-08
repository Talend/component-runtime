/**
 * Copyright (C) 2006-2017 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.server.configuration;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

import org.apache.deltaspike.core.api.config.Source;
import org.apache.deltaspike.core.impl.config.MapConfigSource;

@Source
@ApplicationScoped
public class ScmConfigurationLoader extends MapConfigSource {

    public ScmConfigurationLoader() {
        super(new HashMap<>());
    }

    @PostConstruct
    private void init() {
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
            properties.stringPropertyNames().stream().collect(this::getProperties,
                    (m, k) -> m.put(k, properties.getProperty(k)), Map::putAll);
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public String getConfigName() {
        return "component-scm-configuration";
    }
}
