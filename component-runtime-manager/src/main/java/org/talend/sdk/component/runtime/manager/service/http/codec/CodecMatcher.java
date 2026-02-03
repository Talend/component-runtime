/**
 * Copyright (C) 2006-2026 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.runtime.manager.service.http.codec;

import static java.util.Locale.ROOT;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.talend.sdk.component.api.service.http.ContentType;
import org.talend.sdk.component.api.service.http.Decoder;
import org.talend.sdk.component.api.service.http.Encoder;

/**
 * Codec matcher using content type defined with {@link ContentType} on {@link Decoder}, {@link Encoder} implementations
 *
 * @param <T> the type of object selected.
 */
public class CodecMatcher<T> {

    private final ConcurrentMap<String, T> cache = new ConcurrentHashMap<>();

    /**
     * select the suitable codec for the content type
     *
     * @param codecList map of codec
     * @param contentType content type to be handled
     * @return the suitable codec for the content type
     */
    public T select(final Map<String, T> codecList, final String contentType) {
        final String mediaType = extractMediaType(contentType);
        return cache.computeIfAbsent(mediaType, k -> {
            if (codecList.containsKey(mediaType)) { // exact match
                return codecList.get(mediaType);
            }

            // regex match
            final Optional<T> matched = codecList
                    .entrySet()
                    .stream()
                    .filter(e -> mediaType.matches(e.getKey().replace("+", "\\+").replace("*", ".+")))
                    .findFirst()
                    .map(Map.Entry::getValue);
            return matched
                    .orElseThrow(
                            () -> new IllegalStateException("No codec found for content-type: '" + contentType + "'"));

        });
    }

    private String extractMediaType(final String contentType) {
        if (contentType == null || contentType.isEmpty()) {
            return "*/*";
        }
        // content-type contains charset and/or boundary
        return ((contentType.contains(";")) ? contentType.split(";")[0] : contentType).toLowerCase(ROOT);

    }
}
