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
package org.talend.sdk.component.runtime.manager.service;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PreDestroy;

import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.service.cache.LocalCache;
import org.talend.sdk.component.api.service.configuration.Configuration;
import org.talend.sdk.component.runtime.serialization.SerializableService;

import lombok.Data;

/**
 * Implementation of LocalCache with in memory concurrent map.
 */
public class LocalCacheService implements LocalCache, Serializable {

    /** plugin name for this cache */
    private final String plugin;

    private final Supplier<Long> timer;

    private final ConcurrentMap<String, ElementImpl> cache = new ConcurrentHashMap<>();

    @Configuration("talend.component.manager.services.cache.eviction")
    private Supplier<CacheConfiguration> configuration;

    // scheduler we use to evict tokens
    private transient Supplier<ScheduledExecutorService> threadServiceGetter;

    public LocalCacheService(final String plugin, final Supplier<Long> timer,
            final Supplier<ScheduledExecutorService> threadServiceGetter) {
        this.plugin = plugin;
        this.timer = timer;
        this.threadServiceGetter = threadServiceGetter;
    }

    /**
     * Evict an object.
     * 
     * @param key key to evict.
     */
    @Override
    public void evict(final String key) {
        final String realKey = internalKey(key);

        // use compute to be able to call release.
        cache.compute(realKey, (String oldKey, ElementImpl oldElement) -> {
            if (oldElement != null && oldElement.canBeEvict()) {
                // ok to evict, so do release.
                oldElement.release();
                return null;
            }
            return oldElement;
        });
    }

    @Override
    public void evictIfValue(final String key, final Object expected) {
        final String realKey = internalKey(key);

        // use compute to be able to call release.
        cache.compute(realKey, (String oldKey, ElementImpl oldElement) -> {
            if (oldElement != null && (Objects.equals(oldElement.getValue(), expected) || oldElement.canBeEvict())) {
                // ok to evit, so do release.
                oldElement.release();
                return null;
            }
            return oldElement;
        });

    }

    @Override
    public <T> T computeIfAbsent(final Class<T> expectedClass, final String key, final Predicate<Element> toRemove,
            final long timeoutMs, final Supplier<T> value) {

        final Integer maxSize = this.getConfigValue(CacheConfiguration::getDefaultMaxSize, -1);
        if (maxSize > 0 && this.cache.size() >= maxSize) {
            this.clean(); // clean before add one element.
            if (this.cache.size() >= maxSize) {
                synchronized (this.cache) {
                    while (this.cache.size() >= maxSize) {
                        final String keyToRemove = this.cache.keySet().iterator().next();
                        this.cache.remove(keyToRemove);
                    }
                }
            }
        }

        final ScheduledFuture<?> task = timeoutMs > 0 ? this.evictionTask(key, timeoutMs) : null;

        final long endOfValidity = this.calcEndOfValidity(timeoutMs);
        final ElementImpl element =
                this.addToMap(key, () -> new ElementImpl(value, toRemove, endOfValidity, task, this.timer));

        return element.getValue(expectedClass);
    }

    @Override
    public <T> T computeIfAbsent(final Class<T> expectedClass, final String key, final Predicate<Element> toRemove,
            final Supplier<T> value) {

        final long timeout = this.getConfigValue(CacheConfiguration::getDefaultEvictionTimeout, -1L);
        return this.computeIfAbsent(expectedClass, key, toRemove, timeout, value);
    }

    @Override
    public <T> T computeIfAbsent(final Class<T> expectedClass, final String key, final long timeoutMs,
            final Supplier<T> value) {
        return this.computeIfAbsent(expectedClass, key, null, timeoutMs, value);
    }

    private ElementImpl addToMap(final String key, final Supplier<ElementImpl> builder) {
        final String internalKey = internalKey(key);
        return cache.compute(internalKey, (String k, ElementImpl old) -> //
        old == null || old.mustBeRemoved() ? builder.get() : old);
    }

    @Override
    public <T> T computeIfAbsent(final Class<T> expectedClass, final String key, final Supplier<T> value) {
        final long timeOut = this.getConfigValue(CacheConfiguration::getDefaultEvictionTimeout, -1L);
        return computeIfAbsent(expectedClass, key, null, timeOut, value);
    }

    @PreDestroy
    public void release() {
        this.cache.forEach((String k, ElementImpl e) -> e.release());
        this.cache.clear();
    }

    private long calcEndOfValidity(final long timeoutMs) {
        return timeoutMs > 0 ? this.timer.get() + timeoutMs : -1;
    }

    private String internalKey(final String key) {
        return plugin + '@' + key;
    }

    public void clean() {
        Stream<Entry<String, ElementImpl>> elements = //
                this.cache
                        .entrySet() //
                        .stream() //
                        .filter(e -> e.getValue().mustBeRemoved());

        final int maxEviction = this.getConfigValue(CacheConfiguration::getMaxDeletionPerEvictionRun, -1);
        if (maxEviction > 0) {
            elements = elements.limit(maxEviction);
        }
        final List<String> removableElements = elements.map(Entry::getKey).collect(Collectors.toList());// materialize
                                                                                                        // before
                                                                                                        // actually
                                                                                                        // removing it
        removableElements.forEach(this.cache::remove);
    }

    private ScheduledExecutorService getThreadService() {
        return this.threadServiceGetter.get();
    }

    /**
     * Schedule an eviction task for a key.
     * 
     * @param key : key to evict from cache.
     * @param delayMillis : delay in millis before triggered.
     * @return result of task.
     */
    private ScheduledFuture<?> evictionTask(final String key, final long delayMillis) {
        return this
                .getThreadService()
                .schedule(() -> this.evict(key), //
                        delayMillis, //
                        TimeUnit.MILLISECONDS); //
    }

    private <T> T getConfigValue(final Function<CacheConfiguration, T> getter, final T defaultValue) {
        return Optional
                .ofNullable(this.getConfig()) //
                .map(getter) //
                .orElse(defaultValue);
    }

    private CacheConfiguration getConfig() {
        return this.configuration != null ? this.configuration.get() : null;
    }

    /**
     * Cache configuration.
     */
    @Data
    public static class CacheConfiguration implements Serializable {

        @Option
        private long defaultEvictionTimeout;

        @Option
        private int maxDeletionPerEvictionRun;

        @Option
        private int defaultMaxSize;
    }

    /**
     * Wrapper for each cached object.
     */
    private static class ElementImpl implements Element {

        /** cached object */
        private final Object value;

        /** function, if exists, that authorize to remove object. */
        private final Predicate<Element> canBeRemoved;

        /** give time object can be release (infinity if < 0) */
        private final long endOfValidity;

        /** scheduled task to remove object if nedeed (to cancel if removed before) */
        private final ScheduledFuture<?> removedTask;

        private final Supplier<Long> serviceTimer;

        public <T> ElementImpl(final Supplier<T> value, final Predicate<Element> canBeRemoved, final long endOfValidity,
                final ScheduledFuture<?> removedTask, final Supplier<Long> timer) {
            this.value = value.get();
            this.canBeRemoved = canBeRemoved;
            this.endOfValidity = endOfValidity;
            this.removedTask = removedTask;
            this.serviceTimer = timer;
        }

        @Override
        public <T> T getValue(final Class<T> expectedType) {
            if (this.value != null && !expectedType.isInstance(this.value)) {
                throw new ClassCastException(
                        this.value.getClass().getName() + " cannot be cast to " + expectedType.getName());
            }
            return expectedType.cast(this.value);
        }

        @Override
        public long getLastValidityTimestamp() {
            return this.endOfValidity;
        }

        public boolean mustBeRemoved() {
            return (this.endOfValidity > 0 && this.endOfValidity <= this.serviceTimer.get()) // time out passed
                    && this.canBeEvict(); // or function indicate to remove.
        }

        public boolean canBeEvict() {
            return this.canBeRemoved == null || this.canBeRemoved.test(this);
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
