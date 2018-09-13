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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenTracingConfigSource implements ConfigSource {

    private final static Logger LOGGER = LoggerFactory.getLogger(OpenTracingConfigSource.class);

    private final String TRACING_ON = "TRACING_ON";

    private final String TRACING_SAMPLING_RATE = "TRACING_SAMPLING_RATE";

    private Map<String, String> configuration = new HashMap<String, String>() {

        {
            final boolean tracingOn =
                    System.getenv(TRACING_ON) != null && Boolean.parseBoolean(System.getenv(TRACING_ON));
            int tracingRate = 1;
            if (System.getenv(TRACING_SAMPLING_RATE) != null) {
                try {
                    tracingRate = Integer.parseInt(System.getenv(TRACING_SAMPLING_RATE));
                } catch (final NumberFormatException e) {
                    LOGGER.warn("Can't parse value of environment property TRACING_SAMPLING_RATE", e);
                }
            }
            final String isTracingOn = String.valueOf(tracingOn && tracingRate == 1);
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
        return "talend-opentracing";
    }
}
