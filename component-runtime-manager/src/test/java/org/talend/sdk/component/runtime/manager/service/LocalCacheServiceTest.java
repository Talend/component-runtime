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

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.apache.xbean.propertyeditor.PropertyEditorRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.service.cache.LocalCache;
import org.talend.sdk.component.api.service.cache.LocalCache.Element;
import org.talend.sdk.component.api.service.configuration.LocalConfiguration;
import org.talend.sdk.component.api.service.injector.Injector;
import org.talend.sdk.component.runtime.manager.asm.ProxyGenerator;
import org.talend.sdk.component.runtime.manager.reflect.ParameterModelService;
import org.talend.sdk.component.runtime.manager.reflect.ReflectionService;
import org.talend.sdk.component.runtime.manager.serialization.DynamicContainerFinder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.talend.sdk.component.runtime.manager.test.Serializer.roundTrip;

class LocalCacheServiceTest {


    private boolean active;

    private int  intervale;

    private LocalConfiguration cacheConfig = new LocalConfiguration() {
        @Override
        public String get(String key) {
            if ("test.talend.component.manager.services.cache.eviction.defaultEvictionInterval".equals(key)) {
                return String.valueOf(intervale);
            }
            if ("test.talend.component.manager.services.cache.eviction.active".equals(key)) {
                return String.valueOf(active);
            }
            throw new IllegalArgumentException("unknown key '" + key + "'");
        }

        @Override
        public Set<String> keys() {
            Set<String> keys = new HashSet<>();
            keys.add("test.talend.component.manager.services.cache.eviction.defaultEvictionInterval");
            keys.add("test.talend.component.manager.services.cache.eviction.active");
            return keys;
        }
    };

    private final LocalCacheService<String> cache = new LocalCacheService<>("LocalCacheServiceTest");


    @BeforeEach
    void init() {
        this.active = false;
        this.intervale = 0;
        final Map<Class<?>, Object> services = new HashMap<>(2);
       // services.put(LocalCache.class, new LocalCacheService("LocalCacheServiceTest"));
        services.put(LocalConfiguration.class,
                new LocalConfigurationService(Collections.singletonList(cacheConfig), "test"));


        final PropertyEditorRegistry propertyEditorRegistry = new PropertyEditorRegistry();
        Injector injector = new InjectorImpl("LocalCacheServiceTest",
                new ReflectionService(new ParameterModelService(propertyEditorRegistry), propertyEditorRegistry),
                new ProxyGenerator(), services);

        injector.inject(cache);
        Assertions.assertTrue(cache.isEmpty(), "not empty after creation");
    }

    @AfterEach
    void end() {
        cache.clean();
    }

    @Test
    void conditionalEviction() {
        final String value = this.cache.computeIfAbsent("foo", () -> "bar");
        assertEquals("bar", value);
        assertEquals("bar", cache.computeIfAbsent("foo", () -> "test"));

        cache.evictIfValue("foo", value);
        assertEquals("renewed", cache.computeIfAbsent("foo", () -> "renewed"));

        cache.evictIfValue("foo", "doNothing");
        assertEquals("renewed", cache.computeIfAbsent("foo", () -> "renewed"));

        cache.evictIfValue("foo", null);
        assertEquals("renewed", cache.computeIfAbsent("foo", () -> "renewed"));
    }

    @Test
    void eviction() {
        assertEquals("bar", cache.computeIfAbsent("foo", () -> "bar"));
        cache.evict("foo");
        assertEquals("renewed", cache.computeIfAbsent("foo", () -> "renewed"));
    }

    @Test
    void mustRemoved() {
        final boolean[] mustRemoved = new boolean[] { false };

        final String v1 = cache.computeIfAbsent("key1", (Element<String> e) -> mustRemoved[0], () -> "value1");
        final String v2 = cache.computeIfAbsent("key1", () -> "value2");
        mustRemoved[0] = true;
        final String v3 = cache.computeIfAbsent("key1", () -> "value3");
        assertEquals("value1", v1);
        assertEquals("value1", v2);
        assertEquals("value3", v3);
    }

    @Test
    void localTimeout() throws InterruptedException {
        final String v1 = cache.computeIfAbsent("key1", 200L, () -> "value1");
        final String v2 = cache.computeIfAbsent("key1", 200L,  () -> "value2");

        final String valueKey2 = cache.computeIfAbsent("key2", -1L, () -> "valueKey2");

        Thread.sleep(240L);
        final String v3 = cache.computeIfAbsent("key1", 200L, () -> "value3");

        final String value2Key2 = cache.computeIfAbsent("key2", () -> "value2Key2");

        assertEquals("value1", v1);
        assertEquals("value1", v2);
        assertEquals("value3", v3);

        assertEquals("valueKey2", valueKey2);
        assertEquals("valueKey2", value2Key2);
    }

    @Test
    void timeout() throws InterruptedException {
        this.active = true;
        this.intervale = 200;

        final String v1 = cache.computeIfAbsent("key1", () -> "value1");
        final String v2 = cache.computeIfAbsent("key1", () -> "value2");

        Thread.sleep(240L);
        final String v3 = cache.computeIfAbsent("key1", () -> "value3");
        assertEquals("value1", v1);
        assertEquals("value1", v2);
        assertEquals("value3", v3);
    }

    @Test
    void serialize() throws IOException, ClassNotFoundException {
        DynamicContainerFinder.LOADERS.put("LocalCacheServiceTest", Thread.currentThread().getContextClassLoader());
        DynamicContainerFinder.SERVICES.put(LocalCache.class, new LocalCacheService<>("tmp"));
        try {
            final LocalCache<String> cache = new LocalCacheService<>("LocalCacheServiceTest");
            final LocalCache<String> copy = roundTrip(cache);
            assertNotNull(copy);
        } finally {
            DynamicContainerFinder.LOADERS.remove("LocalCacheServiceTest");
            DynamicContainerFinder.SERVICES.remove(LocalCache.class);
        }
    }

    @Test
    void getOrSet() {
        final LocalCacheService<Integer> cache = new LocalCacheService<>("tmp");
        final AtomicInteger counter = new AtomicInteger(0);

        final boolean[] toDelete = new boolean[] { false };

        final Supplier<Integer> cacheUsage =
                () -> cache.computeIfAbsent("foo", (Element<Integer> at) -> toDelete[0], counter::incrementAndGet);
        for (int i = 0; i < 3; i++) {
            assertEquals(1, cacheUsage.get().intValue());
        }

        toDelete[0] = true;
        assertEquals(2, cacheUsage.get().intValue());

        cache.clean();
        assertTrue(cache.isEmpty());

    }

}
