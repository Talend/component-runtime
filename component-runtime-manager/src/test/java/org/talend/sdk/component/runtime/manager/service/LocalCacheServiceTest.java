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
package org.talend.sdk.component.runtime.manager.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.talend.sdk.component.runtime.manager.test.Serializer.roundTrip;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
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
import org.talend.sdk.component.runtime.manager.util.MemoizingSupplier;

class LocalCacheServiceTest {

    private int defaultMaxSize;

    private int interval;

    private int maxEviction;

    private Supplier<ScheduledExecutorService> executorGetter;

    private LocalConfiguration cacheConfig = new LocalConfiguration() {

        private final Set<String> keys = new HashSet<>();
        {
            keys.add("test.talend.component.manager.services.cache.eviction.defaultEvictionTimeout");
            keys.add("test.talend.component.manager.services.cache.eviction.defaultMaxSize");
            keys.add("test.talend.component.manager.services.cache.eviction.maxDeletionPerEvictionRun");
        }

        @Override
        public String get(String key) {
            if ("test.talend.component.manager.services.cache.eviction.defaultEvictionTimeout".equals(key)) {
                return String.valueOf(interval);
            }
            if ("test.talend.component.manager.services.cache.eviction.defaultMaxSize".equals(key)) {
                return String.valueOf(defaultMaxSize);
            }
            if ("test.talend.component.manager.services.cache.eviction.maxDeletionPerEvictionRun".equals(key)) {
                return String.valueOf(maxEviction);
            }
            throw new IllegalArgumentException("unknown key '" + key + "'");
        }

        @Override
        public Set<String> keys() {
            return keys;
        }
    };

    private LocalCacheService cache;

    private long simulateCurrent = -1;

    private long getCurrentMillis() {
        if (this.simulateCurrent >= 0) {
            return this.simulateCurrent;
        }
        return System.currentTimeMillis();
    }

    @BeforeEach
    void init() {
        executorGetter = new MemoizingSupplier<>(this::buildExecutorService);
        cache = new LocalCacheService("LocalCacheServiceTest", this::getCurrentMillis, executorGetter);
        this.defaultMaxSize = -1;
        this.interval = -1;
        this.maxEviction = -1;

        final Map<Class<?>, Object> services = new HashMap<>(1);
        services
                .put(LocalConfiguration.class,
                        new LocalConfigurationService(Collections.singletonList(cacheConfig), "test"));

        final PropertyEditorRegistry propertyEditorRegistry = new PropertyEditorRegistry();
        Injector injector = new InjectorImpl("LocalCacheServiceTest",
                new ReflectionService(new ParameterModelService(propertyEditorRegistry), propertyEditorRegistry),
                new ProxyGenerator(), services);

        injector.inject(cache);

        Assertions.assertTrue(isCacheEmpty(), "not empty after creation");
    }

    @AfterEach
    void end() throws NoSuchFieldException, IllegalAccessException {
        this.cache.release();

        final Field field = LocalCacheService.class.getDeclaredField("cache");
        field.setAccessible(true);
        Object o = field.get(this.cache);
        if (o != null) {
            Map content = (Map) o;
            Assertions.assertTrue(content.isEmpty());
        }

        cache.clean();
    }

    @Test
    void conditionalEviction() {
        final String value = this.cache.computeIfAbsent(String.class, "foo", () -> "bar");
        assertEquals("bar", value);
        assertEquals("bar", cache.computeIfAbsent(String.class, "foo", () -> "test"));

        cache.evictIfValue("foo", value);
        assertEquals("renewed", cache.computeIfAbsent(String.class, "foo", () -> "renewed"));

        cache.evictIfValue("foo", "doNothing");
        assertEquals("renewed", cache.computeIfAbsent(String.class, "foo", () -> "renewed"));

        cache.evictIfValue("foo", null);
        assertEquals("renewed", cache.computeIfAbsent(String.class, "foo", () -> "renewed"));
    }

    @Test
    void eviction() {
        assertEquals("bar", cache.computeIfAbsent(String.class, "foo", () -> "bar"));
        cache.evict("foo");
        assertEquals("renewed", cache.computeIfAbsent(String.class, "foo", () -> "renewed"));
    }

    @Test
    void mustRemoved() {
        final boolean[] mustRemoved = new boolean[] { false };

        this.interval = 100;
        this.simulateCurrent = 100L;

        final String v1 = cache.computeIfAbsent(String.class, "key1", (Element e) -> mustRemoved[0], () -> "value1");
        final String v2 = cache.computeIfAbsent(String.class, "key1", () -> "value2");
        mustRemoved[0] = true;
        this.simulateCurrent = 300L;
        final String v3 = cache.computeIfAbsent(String.class, "key1", () -> "value3");
        assertEquals("value1", v1);
        assertEquals("value1", v2);
        assertEquals("value3", v3);
    }

    @Test
    void localTimeout() throws InterruptedException {
        final String v1 = cache.computeIfAbsent(String.class, "key1", 200L, () -> "value1");
        final String v2 = cache.computeIfAbsent(String.class, "key1", 200L, () -> "value2");

        final String valueKey2 = cache.computeIfAbsent(String.class, "key2", -1L, () -> "valueKey2");

        Thread.sleep(400L);

        final String v3 = cache.computeIfAbsent(String.class, "key1", 200L, () -> "value3");

        final String value2Key2 = cache.computeIfAbsent(String.class, "key2", () -> "value2Key2");

        assertEquals("value1", v1);
        assertEquals("value1", v2);
        assertEquals("value3", v3);

        assertEquals("valueKey2", valueKey2);
        assertEquals("valueKey2", value2Key2);
    }

    @Test
    void timeoutAuto() {
        this.interval = 200;

        final CountDownLatch latch = new CountDownLatch(1);
        final long start = System.currentTimeMillis();
        String value = cache.computeIfAbsent(String.class, "foo", (Element e) -> {
            latch.countDown();
            return true;
        }, () -> "bar");
        assertEquals("bar", value);
        final String v2 = cache.computeIfAbsent(String.class, "foo", () -> "value2");
        assertEquals("bar", v2);
        try {
            latch.await();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        final long end = System.currentTimeMillis();

        final long duration = end - start;
        assertEquals(200, duration, 200);

        final String v3 = cache.computeIfAbsent(String.class, "foo", () -> "value3");
        assertEquals("value3", v3);
    }

    @Test
    void serialize() throws IOException, ClassNotFoundException {
        DynamicContainerFinder.LOADERS.put("LocalCacheServiceTest", Thread.currentThread().getContextClassLoader());
        DynamicContainerFinder.SERVICES
                .put(LocalCache.class, new LocalCacheService("tmp", System::currentTimeMillis, executorGetter));
        try {
            final LocalCache cache =
                    new LocalCacheService("LocalCacheServiceTest", System::currentTimeMillis, executorGetter);
            final LocalCache copy = roundTrip(cache);
            assertNotNull(copy);
        } finally {
            DynamicContainerFinder.LOADERS.remove("LocalCacheServiceTest");
            DynamicContainerFinder.SERVICES.remove(LocalCache.class);
        }
    }

    @Test
    void getOrSet() {
        final AtomicInteger counter = new AtomicInteger(0);
        this.interval = 300;
        this.simulateCurrent = 100L;

        final boolean[] toDelete = new boolean[] { false };

        final Supplier<Integer> cacheUsage = () -> cache
                .computeIfAbsent(Integer.class, "foo", (Element at) -> toDelete[0], counter::incrementAndGet);
        for (int i = 0; i < 3; i++) {
            assertEquals(1, cacheUsage.get().intValue());
        }
        this.simulateCurrent = 450L;
        toDelete[0] = true;
        assertEquals(2, cacheUsage.get().intValue());
        this.simulateCurrent = 850L;

        cache.clean();
        Assertions.assertTrue(isCacheEmpty(), "not empty clean with toDelete");
    }

    @Test
    void cleanWithMax() throws InterruptedException {
        this.maxEviction = 10;
        this.interval = 10;

        final boolean[] toDelete = new boolean[] { false };
        for (int i = 0; i < 20; i++) {
            cache.computeIfAbsent(String.class, "k" + i, (Element e) -> toDelete[0], 3L, () -> "val");
        }
        Thread.sleep(60);
        Assertions.assertEquals(20, cacheSize());
        cache.clean();
        Assertions.assertEquals(20, cacheSize());
        toDelete[0] = true;
        cache.clean();
        Assertions.assertEquals(10, cacheSize());
        cache.clean();
        Assertions.assertTrue(isCacheEmpty(), "not empty after last clean");
    }

    @Test
    public void evictionAuto() {
        this.defaultMaxSize = 10;
        for (int i = 0; i < 20; i++) {
            cache.computeIfAbsent(String.class, "k" + i, (Element e) -> false, -1L, () -> "val");
        }
        Assertions.assertEquals(10, this.cacheSize());
    }

    private boolean isCacheEmpty() {
        return this.internalCacheMap().isEmpty();
    }

    private int cacheSize() {
        return this.internalCacheMap().size();
    }

    private Map<?, ?> internalCacheMap() {
        try {
            final Field cacheField = LocalCacheService.class.getDeclaredField("cache");
            cacheField.setAccessible(true);
            final Object cacheMap = cacheField.get(this.cache);
            return (Map<?, ?>) cacheMap;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalArgumentException("cache map access error", e);
        }
    }

    private ScheduledExecutorService buildExecutorService() {
        return Executors.newScheduledThreadPool(4, (Runnable r) -> {
            final Thread thread = new Thread(r, DefaultServiceProvider.class.getName() + "-eviction-" + hashCode());
            thread.setPriority(Thread.NORM_PRIORITY);
            return thread;
        });
    }
}
