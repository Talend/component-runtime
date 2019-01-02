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
package org.talend.sdk.component.runtime.manager.service;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

import org.talend.sdk.component.api.service.cache.LocalCache;
import org.talend.sdk.component.runtime.serialization.SerializableService;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class LocalCacheService implements LocalCache, Serializable {

    private final String plugin;

    private final ConcurrentMap<String, Element> cache = new ConcurrentHashMap<>();

    @Override
    public <T> T computeIfAbsent(final String key, final long timeoutMs, final Supplier<T> value) {
        final String internalKey = internalKey(key);
        final Element element = cache
                .compute(internalKey,
                        (k, e) -> e == null || e.isExpired()
                                ? new Element(value.get(), System.currentTimeMillis() + timeoutMs)
                                : e);
        if (element == null) {
            return null;
        }
        return (T) element.value;
    }

    private String internalKey(final String key) {
        return plugin + '@' + key;
    }

    @AllArgsConstructor
    public static class Element {

        private final Object value;

        private final long endOfValidity;

        private boolean isExpired() {
            return System.currentTimeMillis() > endOfValidity;
        }
    }

    Object writeReplace() throws ObjectStreamException {
        return new SerializableService(plugin, LocalCache.class.getName());
    }
}
