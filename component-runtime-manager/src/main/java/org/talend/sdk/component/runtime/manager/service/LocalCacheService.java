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
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.service.cache.LocalCache;
import org.talend.sdk.component.api.service.configuration.Configuration;
import org.talend.sdk.component.runtime.serialization.SerializableService;

import lombok.RequiredArgsConstructor;
import lombok.Data;

import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toList;

/**
 * Implementation of LocalCache with in memory conccurent map.
 * @param <T>   the type of cached data.
 */
@RequiredArgsConstructor
public class LocalCacheService<T> implements LocalCache<T>, Serializable {

    /** plugin name for this cache */
    private final String plugin;

    private final ConcurrentMap<String, ElementImpl<T>> cache = new ConcurrentHashMap<>();

    @Configuration("talend.component.manager.services.cache.eviction")
    private Supplier<CacheConfiguration> configuration;

    // scheduler we use to evict tokens
    private volatile ScheduledExecutorService threadService;

    /**
     * Evict an object.
     * @param key key to evict.
     */
    @Override
    public void evict(final String key) {
        final ElementImpl<T> removedElement = cache.remove(internalKey(key));
        if (removedElement != null) {
            removedElement.release();
        }
    }

    @Override
    public void evictIfValue(final String key, final T expected) {
        final String realKey = internalKey(key);

        // use compute to be able to call release.
        cache.compute(realKey,
                (String oldKey , ElementImpl<T> oldElement) -> {
                    if (oldElement != null && Objects.equals(oldElement.getValue(), expected)) {
                        // ok to evit, so do release.
                        oldElement.release();
                        return null;
                    }
                    return oldElement;
                });

    }



    @Override
    public T computeIfAbsent(String key,
            final Predicate<Element<T>> toRemove,
            final Supplier<T> value) {

        final ElementImpl<T> element = this.addToMap(key,
                () -> new ElementImpl<T>(value, toRemove, -1, null));

        return element.value;
    }

    @Override
    public T computeIfAbsent(String key, long timeoutMs, Supplier<T> value) {

        final long endOfValidity = timeoutMs > 0 ? System.currentTimeMillis() + timeoutMs : -1;
        final ScheduledFuture<?> task = endOfValidity > 0 ? evictionTask(key, endOfValidity) : null;

        final ElementImpl<T> element = addToMap(key,
                () -> new ElementImpl<T>(value, null, endOfValidity, task));

        return element.value;
    }

    private ElementImpl<T> addToMap(String key, Supplier<ElementImpl<T>> builder) {
        final String internalKey = internalKey(key);
        return cache.compute(
                internalKey,
                (String k, ElementImpl<T> old) ->    //
                        old == null || old.mustBeRemoved() ?
                                builder.get() :
                                old
        );
    }

    @Override
    public T computeIfAbsent(String key, Supplier<T> value) {
        final CacheConfiguration config = this.configuration.get();
        long timeOut = config.isActive() ? config.getDefaultEvictionInterval() : -1;
        return computeIfAbsent(key, timeOut, value);
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

    private ScheduledExecutorService getThreadService() {
        if (this.threadService == null) {
            synchronized (this) {
                if (this.threadService == null) {
                    this.threadService = Executors.newSingleThreadScheduledExecutor((Runnable r) -> {
                        final Thread thread = new Thread(r, this.getClass().getName() + "-eviction-" + hashCode());
                        thread.setPriority(Thread.NORM_PRIORITY);
                        return thread;
                    });
                }
            }
        }
        return this.threadService;
    }

    private ScheduledFuture<?> evictionTask(String key, long end) {
        ScheduledFuture<?> task = null;
        final CacheConfiguration config = this.configuration.get();
        if (config != null && config.isActive()) {
            long realEnd = end;
            task = this.getThreadService().schedule(() -> this.evict(key), end, SECONDS);
        }
        return task;
    }

    /**
     * Cache configuration.
     */
    @Data
    public static class CacheConfiguration implements Serializable {

        @Option
        private long defaultEvictionInterval;

        @Option
        private boolean active;
    }

    /**
     * Wrapper for each cached object.
     * @param <T> : type of cached object.
     */
    private static class ElementImpl<T> implements Element<T> {

        /** cached object */
        private final T value;

        /** function, if exists, that authorize to remove object. */
        private final Predicate<Element<T>> toRemove;

        /** give time object can be release (infinity if < 0) */
        private final long endOfValidity;

        /** scheduled task to remove object if nedeed (to cancel if removed before) */
        private final ScheduledFuture<?> removedTask;

        public ElementImpl(Supplier<T> value,
                Predicate<Element<T>> toRemove,
                long endOfValidity,
                ScheduledFuture<?> removedTask) {
            this.value = value.get();
            this.toRemove = toRemove;
            this.endOfValidity = endOfValidity;
            this.removedTask = removedTask;
        }

        @Override
        public T getValue() {
            return this.value;
        }

        @Override
        public long getEndOfValidity() {
            return this.endOfValidity;
        }

        public boolean mustBeRemoved() {
            return (this.endOfValidity > 0 && this.endOfValidity <= System.currentTimeMillis()) // time out passed
                    ||  (this.toRemove != null && this.toRemove.test(this)); // or function indicate to remove.
        }

        /**
         * Release this object because removed.
         */
        public synchronized void release() {
            if (this.removedTask != null) {
                this.removedTask.cancel(false);
            }
        }

        @Override
        public boolean equals(final Object o) { // consider only value
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            return Objects.equals(ElementImpl.class.cast(o).value, value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }
    }

    Object writeReplace() throws ObjectStreamException {
        return new SerializableService(plugin, LocalCache.class.getName());
    }
}
