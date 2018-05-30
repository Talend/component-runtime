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
package org.talend.sdk.component.proxy.jcache;

import static java.util.Collections.emptyIterator;
import static java.util.Collections.emptyMap;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.configuration.CacheEntryListenerConfiguration;
import javax.cache.configuration.Configuration;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.integration.CompletionListener;
import javax.cache.processor.EntryProcessor;
import javax.cache.processor.EntryProcessorException;
import javax.cache.processor.EntryProcessorResult;

class EmptyCache<A, B> implements Cache<A, B> {

    private final String name;

    private final Configuration<A, B> configuration;

    private final CacheManager manager;

    EmptyCache(final String name, final CacheManager manager) {
        this.name = name;
        this.configuration = new MutableConfiguration<>();
        this.manager = manager;
    }

    @Override
    public B get(final A key) {
        return null;
    }

    @Override
    public Map<A, B> getAll(final Set<? extends A> keys) {
        return null;
    }

    @Override
    public boolean containsKey(final A key) {
        return false;
    }

    @Override
    public void loadAll(final Set<? extends A> keys, final boolean replaceExistingValues,
            final CompletionListener completionListener) {
        // no-op
    }

    @Override
    public void put(final A key, final B value) {
        // no-op
    }

    @Override
    public B getAndPut(final A key, final B value) {
        return null;
    }

    @Override
    public void putAll(final Map<? extends A, ? extends B> map) {
        // no-op
    }

    @Override
    public boolean putIfAbsent(final A key, final B value) {
        return false;
    }

    @Override
    public boolean remove(final A key) {
        return false;
    }

    @Override
    public boolean remove(final A key, final B oldValue) {
        return false;
    }

    @Override
    public B getAndRemove(final A key) {
        return null;
    }

    @Override
    public boolean replace(final A key, final B oldValue, final B newValue) {
        return false;
    }

    @Override
    public boolean replace(final A key, final B value) {
        return false;
    }

    @Override
    public B getAndReplace(final A key, final B value) {
        return null;
    }

    @Override
    public void removeAll(final Set<? extends A> keys) {
        // no-op
    }

    @Override
    public void removeAll() {
        // no-op
    }

    @Override
    public void clear() {
        // no-op
    }

    @Override
    public <T> T invoke(final A key, final EntryProcessor<A, B, T> entryProcessor, final Object... arguments)
            throws EntryProcessorException {
        return null;
    }

    @Override
    public <T> Map<A, EntryProcessorResult<T>> invokeAll(final Set<? extends A> keys,
            final EntryProcessor<A, B, T> entryProcessor, final Object... arguments) {
        return emptyMap();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public CacheManager getCacheManager() {
        return manager;
    }

    @Override
    public void close() {
        // no-op
    }

    @Override
    public boolean isClosed() {
        return false;
    }

    @Override
    public <T> T unwrap(final Class<T> clazz) {
        return clazz.isInstance(this) ? clazz.cast(this) : null;
    }

    @Override
    public void
            registerCacheEntryListener(final CacheEntryListenerConfiguration<A, B> cacheEntryListenerConfiguration) {
        // no-op
    }

    @Override
    public void
            deregisterCacheEntryListener(final CacheEntryListenerConfiguration<A, B> cacheEntryListenerConfiguration) {
        // no-op
    }

    @Override
    public Iterator<Entry<A, B>> iterator() {
        return emptyIterator();
    }

    @Override
    public <C extends Configuration<A, B>> C getConfiguration(final Class<C> clazz) {
        if (clazz.isInstance(configuration)) {
            return clazz.cast(configuration);
        }
        return null;
    }
}
