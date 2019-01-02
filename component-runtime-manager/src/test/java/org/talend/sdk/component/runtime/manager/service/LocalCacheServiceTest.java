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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.talend.sdk.component.runtime.manager.test.Serializer.roundTrip;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.service.cache.LocalCache;
import org.talend.sdk.component.runtime.manager.serialization.DynamicContainerFinder;

class LocalCacheServiceTest {

    @Test
    void serialize() throws IOException, ClassNotFoundException {
        DynamicContainerFinder.LOADERS.put("LocalCacheServiceTest", Thread.currentThread().getContextClassLoader());
        DynamicContainerFinder.SERVICES.put(LocalCache.class, new LocalCacheService("tmp"));
        try {
            final LocalCache cache = new LocalCacheService("LocalCacheServiceTest");
            final LocalCache copy = roundTrip(cache);
            assertNotNull(copy);
        } finally {
            DynamicContainerFinder.LOADERS.remove("LocalCacheServiceTest");
            DynamicContainerFinder.SERVICES.remove(LocalCache.class);
        }
    }

    @Test
    void getOrSet() {
        final LocalCacheService cache = new LocalCacheService("tmp");
        final AtomicInteger counter = new AtomicInteger(0);
        final Supplier<Integer> cacheUsage = () -> cache.computeIfAbsent("foo", 500, counter::incrementAndGet);
        for (int i = 0; i < 3; i++) {
            assertEquals(1, cacheUsage.get().intValue());
        }
        try {
            Thread.sleep(800);
        } catch (final InterruptedException e) {
            fail(e.getMessage());
        }
        assertEquals(2, cacheUsage.get().intValue());
    }
}
