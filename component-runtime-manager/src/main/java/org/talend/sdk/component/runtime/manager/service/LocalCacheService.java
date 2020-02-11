/**
 * Copyright (C) 2006-2020 Talend Inc. - www.talend.com
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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.talend.sdk.component.api.service.cache.LocalCache;
import org.talend.sdk.component.runtime.serialization.SerializableService;

import lombok.AllArgsConstructor;

import static java.util.stream.Collectors.toList;

@AllArgsConstructor
public class LocalCacheService<T> implements LocalCache<T>, Serializable {

    private final String plugin;

    private final ConcurrentMap<String, Element<T>> cache = new ConcurrentHashMap<>();

    @Override
    public T computeIfAbsent(String key, final Predicate<T> toRemove, final Supplier<T> value) {
        final String internalKey = internalKey(key);

        final Element<T> element = cache
                .compute(internalKey, (String k,
                        Element<T> e) -> e == null || e.mustBeRemoved() ? new Element<>(value.get(), toRemove) : e);

        return element.value;
    }

    private String internalKey(final String key) {
        return plugin + '@' + key;
    }

    public void clean() {
        final List<String> removableElements = this.cache
                .entrySet()
                .stream()
                .filter(e -> e.getValue().mustBeRemoved())
                .map(Entry::getKey)
                .collect(toList());// materialize before actually removing it
        removableElements.forEach(this.cache::remove);
    }

    public boolean isEmpty() {
        return this.cache.isEmpty();
    }

    @AllArgsConstructor
    public static class Element<T> {

        private final T value;

        private final Predicate<T> toRemove;

        private boolean mustBeRemoved() {
            return this.toRemove.test(value);
        }
    }

    Object writeReplace() throws ObjectStreamException {
        return new SerializableService(plugin, LocalCache.class.getName());
    }
}
