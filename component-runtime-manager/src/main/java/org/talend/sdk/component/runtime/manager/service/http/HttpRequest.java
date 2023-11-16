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
package org.talend.sdk.component.runtime.manager.service.http;

import static java.util.stream.Collectors.toMap;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.talend.sdk.component.api.service.http.Configurer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@AllArgsConstructor()
@EqualsAndHashCode(exclude = "bodyCache")
public class HttpRequest {

    private static final Object[] EMPTY_ARRAY = new Object[0];

    private static final Configurer.ConfigurerConfiguration EMPTY_CONFIGURER_OPTIONS =
            new Configurer.ConfigurerConfiguration() {

                @Override
                public Object[] configuration() {
                    return EMPTY_ARRAY;
                }

                @Override
                public <T> T get(final String name, final Class<T> type) {
                    return null;
                }
            };

    private final String url;

    private final String methodType;

    private final Collection<String> queryParams;

    private final Map<String, String> headers;

    private final Configurer configurer;

    private final Map<String, Function<Object[], Object>> configurerOptions;

    private final BiFunction<String, Object[], Optional<byte[]>> payloadProvider;

    private final Object[] params;

    private volatile Optional<byte[]> bodyCache;

    /**
     * encode payload only when requested
     *
     * @return bytes encoded payload
     */
    public Optional<byte[]> getBody() {
        if (bodyCache != null) {
            return bodyCache;
        }
        synchronized (this) {
            if (bodyCache != null) {
                return bodyCache;
            }
            return bodyCache = doGetBody();
        }
    }

    private Optional<byte[]> doGetBody() {
        if (payloadProvider == null) {
            return Optional.empty();
        }
        return payloadProvider.apply(headers.get("content-type"), params);
    }

    public Configurer.ConfigurerConfiguration getConfigurationOptions() {
        final Map<String, Object> options = configurerOptions
                .entrySet()
                .stream()
                .collect(toMap(Map.Entry::getKey, e -> e.getValue().apply(params)));

        return configurerOptions.isEmpty() ? EMPTY_CONFIGURER_OPTIONS : new Configurer.ConfigurerConfiguration() {

            @Override
            public Object[] configuration() {
                return options.values().toArray(new Object[0]);
            }

            @Override
            public <T> T get(final String name, final Class<T> type) {
                return type.cast(options.get(name));
            }
        };
    }
}
