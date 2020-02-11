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
package org.talend.sdk.component.api.service.cache;

import java.util.function.Supplier;

/**
 * Framework service injectable in components or services methods
 * to handle local caching.
 * Useful for actions when deployed in a long running instance
 * when actions are costly.
 */
public interface LocalCache {

    /**
     * Remove a cached entry.
     *
     * @param key key to evict.
     */
    void evict(String key);

    /**
     * Remove a cached entry if a particular value is in the cache.
     *
     * @param key key to evict.
     * @param expected expected value activating the eviction.
     */
    <T> void evictIfValue(String key, T expected);

    /**
     * Read or compute and save a value for a determined duration.
     *
     * @param key the cache key, must be unique accross the server.
     * @param timeoutMs the cache duration.
     * @param supplier the value provider if the cache get does a miss.
     * @param <T> the type of data to access/cache.
     * @return the cached or newly computed value.
     */
    <T> T computeIfAbsent(String key, long timeoutMs, Supplier<T> supplier);

    /**
     * Default the timeout to {@literal Integer.MAX_VALUE}.
     *
     * @param key the cache key, must be unique accross the server.
     * @param supplier the value provider if the cache get does a miss.
     * @param <T> the type of data to access/cache.
     * @return the cached or newly computed - and cached indefinitively - value.
     */
    default <T> T computeIfAbsent(String key, Supplier<T> supplier) {
        return computeIfAbsent(key, Integer.MAX_VALUE, supplier);
    }
}
