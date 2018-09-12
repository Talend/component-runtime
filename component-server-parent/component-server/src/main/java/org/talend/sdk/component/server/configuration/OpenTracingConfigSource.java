/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.microprofile.config.spi.ConfigSource;

public class OpenTracingConfigSource implements ConfigSource {

    private Map<String, String> configuration = new HashMap<String, String>() {

        {
            final String isTracingOn = String
                    .valueOf(Boolean.getBoolean("TRACING_ON") && Integer.getInteger("TRACING_SAMPLING_RATATE", 0) != 0);
            put("geronimo.opentracing.filter.active", isTracingOn);
            put("span.converter.zipkin.active", isTracingOn);
            put("span.converter.zipkin.logger.active", isTracingOn);
        }
    };

    @Override
    public Map<String, String> getProperties() {
        return configuration;
    }

    @Override
    public int getOrdinal() {
        return 2000;
    }

    @Override
    public String getValue(final String propertyName) {
        return configuration.get(propertyName);
    }

    @Override
    public String getName() {
        return "component-opentracing";
    }
}
