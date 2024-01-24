/**
 * Copyright (C) 2006-2024 Talend Inc. - www.talend.com
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

import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Framework service injectable in components or services methods
 * to handle local caching.
 * Useful for actions when deployed in a long running instance
 * when actions are costly.
 */
public interface LocalCache {

    /**
     * Use to enrich object with meta-data
     * (help to choice if attached object has to be removed from cache)
     */
    interface Element {

        /**
         * Get the cached object.
         * 
         * @param expectedType : expected type of object.
         * @param <T> : Type.
         * @return cached object.
         */
        <T> T getValue(Class<T> expectedType);

        default Object getValue() {
            return this.getValue(Object.class);
        }

        /**
         * time when this will be no longer valid.
         * 
         * @return Last validity timestamp.
         */
        long getLastValidityTimestamp();
    }

    /**
     * Read or compute and save a value for a determined duration and predicate.
     * 
     * @param expectedClass : cached instance class.
     * @param key : the cache key, must be unique accross the server.
     * @param toRemove : is the object to be removed.
     * @param timeoutMs : duration of cache value.
     * @param value : the value provider if the cache get does a miss.
     * @param <T> class of cached instance.
     * @return the cached or newly computed value.
     */
    <T> T computeIfAbsent(Class<T> expectedClass, String key, Predicate<Element> toRemove, long timeoutMs,
            Supplier<T> value);

    /**
     * Read or compute and save a value until remove predicate go to remove.
     * 
     * @param expectedClass : cached instance class.
     * @param key : the cache key, must be unique accross the server.
     * @param toRemove : is the object to be removed.
     * @param value : the value provider if the cache get does a miss.
     * @param <T> class of cached instance.
     * @return the cached or newly computed value.
     */
    <T> T computeIfAbsent(Class<T> expectedClass, String key, Predicate<Element> toRemove, Supplier<T> value);

    /**
     * Read or compute and save a value for a determined duration.
     * 
     * @param expectedClass : cached instance class.
     * @param key : the cache key, must be unique accross the server.
     * @param timeoutMs : duration of cache value.
     * @param value : value provider.
     * @param <T> class of cached instance.
     * @return the cached or newly computed value.
     */
    <T> T computeIfAbsent(Class<T> expectedClass, String key, long timeoutMs, Supplier<T> value);

    /**
     * Compute and save a value, if key not present, for undetermined duration.
     * 
     * @param expectedClass : cached instance class.
     * @param key : the cache key, must be unique accross the server.
     * @param value : value provider.
     * @param <T> class of cached instance.
     * @return the cached or newly computed value.
     */
    <T> T computeIfAbsent(Class<T> expectedClass, String key, Supplier<T> value);

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
    void evictIfValue(String key, Object expected);

}
