/**
 * Copyright (C) 2006-2025 Talend Inc. - www.talend.com
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
package org.talend.sdk.components.vault.jcache;

import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import javax.cache.Cache;
import javax.cache.event.CacheEntryCreatedListener;
import javax.cache.event.CacheEntryEvent;
import javax.cache.event.CacheEntryExpiredListener;
import javax.cache.event.CacheEntryListenerException;
import javax.cache.event.CacheEntryRemovedListener;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CacheSizeManager<K, V> implements CacheEntryCreatedListener<K, V>, CacheEntryExpiredListener<K, V>,
        CacheEntryRemovedListener<K, V>, Consumer<Cache<K, V>> {

    private final int maxCacheSize;

    private Cache<K, V> cache;

    private final CopyOnWriteArrayList<K> keys = new CopyOnWriteArrayList<>();

    @Override
    public void onCreated(final Iterable<CacheEntryEvent<? extends K, ? extends V>> cacheEntryEvents)
            throws CacheEntryListenerException {
        cacheEntryEvents.forEach(it -> keys.add(it.getKey()));
        if (keys.size() > maxCacheSize) {
            final Iterator<K> iterator = keys.iterator();
            synchronized (this) {
                while (keys.size() > maxCacheSize && iterator.hasNext()) {
                    cache.remove(iterator.next());
                    iterator.remove();
                }
            }
        }
    }

    @Override
    public void onExpired(final Iterable<CacheEntryEvent<? extends K, ? extends V>> cacheEntryEvents)
            throws CacheEntryListenerException {
        onRemoved(cacheEntryEvents);
    }

    @Override
    public void onRemoved(final Iterable<CacheEntryEvent<? extends K, ? extends V>> cacheEntryEvents)
            throws CacheEntryListenerException {
        cacheEntryEvents.forEach(it -> keys.remove(it.getKey()));
    }

    @Override
    public void accept(final Cache<K, V> cache) {
        this.cache = cache;
    }
}
