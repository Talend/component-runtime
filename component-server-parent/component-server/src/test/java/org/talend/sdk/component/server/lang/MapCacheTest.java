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
package org.talend.sdk.component.server.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class MapCacheTest {

    @ParameterizedTest
    @MethodSource("emptyCacheTestCases")
    void shouldEmptyCache(final ConcurrentMap<String, Object> cache, final int maxSize) {
        new MapCache().evictIfNeeded(cache, maxSize);
        assertTrue(cache.isEmpty());
    }

    public static Stream<Arguments> emptyCacheTestCases() {
        return Stream.of(
                Arguments.of(newMapWithNEntries(1), 0),
                Arguments.of(newMapWithNEntries(1), -1),
                Arguments.of(newMapWithNEntries(2), 0)
        );
    }

    @ParameterizedTest
    @MethodSource("updatedCacheTestCases")
    void shouldKeepTheExpectedNumberOfItems(final ConcurrentMap<String, Object> cache, final int maxSize) {
        new MapCache().evictIfNeeded(cache, maxSize);
        assertEquals(maxSize, cache.size());
    }

    public static Stream<Arguments> updatedCacheTestCases() {
        return Stream.of(
                Arguments.of(newMapWithNEntries(6), 1),
                Arguments.of(newMapWithNEntries(6), 2)
        );
    }

    @ParameterizedTest
    @MethodSource("unmodifiedCacheTestCases")
    void shouldNotUpdateTheCache(final ConcurrentMap<String, Object> cache, final int maxSize) {
        final int initialSize = cache.size();
        new MapCache().evictIfNeeded(cache, maxSize);
        assertEquals(initialSize, cache.size());
    }

    public static Stream<Arguments> unmodifiedCacheTestCases() {
        return Stream.of(
                Arguments.of(newMapWithNEntries(2), 3),
                Arguments.of(newMapWithNEntries(20), 21),
                Arguments.of(newMapWithNEntries(1), 4)
        );
    }

    private static @NonNull ConcurrentHashMap<String, String> newMapWithNEntries(int nbEntries) {
        final ConcurrentHashMap<String, String> map = new ConcurrentHashMap<>();
        for (int i = 0; i < nbEntries; i++) {
            map.put("key" + i, "value" + i);
        }
        return map;
    }

}
