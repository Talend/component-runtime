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
package org.talend.sdk.component.runtime.server.vault.proxy.service.jcache;

import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.cache.Cache;
import javax.cache.event.CacheEntryCreatedListener;
import javax.cache.event.CacheEntryEvent;
import javax.cache.event.CacheEntryExpiredListener;
import javax.cache.event.CacheEntryListenerException;
import javax.cache.event.CacheEntryRemovedListener;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.talend.sdk.component.runtime.server.vault.proxy.service.DecryptedValue;

import lombok.Getter;

@Getter
@ApplicationScoped
public class CacheSizeManager implements CacheEntryCreatedListener<String, DecryptedValue>,
        CacheEntryExpiredListener<String, DecryptedValue>, CacheEntryRemovedListener<String, DecryptedValue> {

    @Inject
    @ConfigProperty(name = "talend.vault.cache.jcache.maxCacheSize", defaultValue = "100000")
    private Integer maxCacheSize; // not strict constraint - for perf - but we ensure it is bound to avoid issues

    @Inject
    private Cache<String, DecryptedValue> cache;

    private final CopyOnWriteArrayList<String> keys = new CopyOnWriteArrayList<>();

    @Override
    public void onCreated(final Iterable<CacheEntryEvent<? extends String, ? extends DecryptedValue>> cacheEntryEvents)
            throws CacheEntryListenerException {
        cacheEntryEvents.forEach(it -> keys.add(it.getKey()));
        if (keys.size() > maxCacheSize) {
            final Iterator<String> iterator = keys.iterator();
            synchronized (this) {
                while (keys.size() > maxCacheSize && iterator.hasNext()) {
                    cache.remove(iterator.next());
                    iterator.remove();
                }
            }
        }
    }

    @Override
    public void onExpired(final Iterable<CacheEntryEvent<? extends String, ? extends DecryptedValue>> cacheEntryEvents)
            throws CacheEntryListenerException {
        onRemoved(cacheEntryEvents);
    }

    @Override
    public void onRemoved(final Iterable<CacheEntryEvent<? extends String, ? extends DecryptedValue>> cacheEntryEvents)
            throws CacheEntryListenerException {
        cacheEntryEvents.forEach(it -> keys.remove(it.getKey()));
    }
}
