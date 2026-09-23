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
package org.talend.sdk.component.server.service.jcache.cdi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.annotation.CacheKey;
import javax.cache.annotation.CacheRemove;
import javax.cache.configuration.MutableConfiguration;

import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.interceptor.InvocationContext;

import org.apache.geronimo.jcache.simple.cdi.GeneratedCacheKeyImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Exercises {@link CacheRemoveInterceptor} against a real {@link CDIJCacheHelper} and the default
 * (in-memory, geronimo-jcache-simple provided) JCache implementation - no live external system is
 * involved, per java-testing-conventions.md.
 */
class CacheRemoveInterceptorTest {

    private CDIJCacheHelper helper;

    private CacheRemoveInterceptor interceptor;

    private Target target;

    @BeforeEach
    void setUp() throws Exception {
        helper = new CDIJCacheHelper();
        final java.lang.reflect.Field field = CDIJCacheHelper.class.getDeclaredField("beanManager");
        field.setAccessible(true);
        field.set(helper, mock(BeanManager.class));
        interceptor = new CacheRemoveInterceptor(helper);
        target = new Target();
    }

    @Test
    void beforeInvocationRemovesBeforeTheMethodRuns() throws Throwable {
        seed("cri-beforeCache", "k1", "v1");
        final Method method = Target.class.getDeclaredMethod("removeBefore", String.class);
        final InvocationContext ic = mockContext(method, "k1");
        when(ic.proceed()).thenAnswer(invocation -> {
            assertNull(cachedValue("cri-beforeCache", "k1"), "afterInvocation=false must remove before proceed()");
            return null;
        });

        interceptor.cache(ic);

        assertNull(cachedValue("cri-beforeCache", "k1"));
    }

    @Test
    void afterInvocationRemovesOnlyAfterTheMethodSucceeds() throws Throwable {
        seed("cri-afterCache", "k2", "v2");
        final Method method = Target.class.getDeclaredMethod("removeAfter", String.class);
        final InvocationContext ic = mockContext(method, "k2");
        when(ic.proceed()).thenAnswer(invocation -> {
            assertEquals("v2", cachedValue("cri-afterCache", "k2"),
                    "afterInvocation=true must not remove before proceed()");
            return null;
        });

        interceptor.cache(ic);

        assertNull(cachedValue("cri-afterCache", "k2"));
    }

    @Test
    void afterInvocationDoesNotRemoveWhenTheExceptionIsNotIncluded() throws Throwable {
        seed("cri-evictForCache", "k3", "v3");
        final Method method = Target.class.getDeclaredMethod("removeAfterEvictForIllegalState", String.class);
        final InvocationContext ic = mockContext(method, "k3");
        when(ic.proceed()).thenThrow(new IllegalArgumentException("not-included"));

        assertThrows(IllegalArgumentException.class, () -> interceptor.cache(ic));
        assertEquals("v3", cachedValue("cri-evictForCache", "k3"));
    }

    @Test
    void afterInvocationRemovesWhenTheExceptionIsIncluded() throws Throwable {
        seed("cri-evictForCache", "k4", "v4");
        final Method method = Target.class.getDeclaredMethod("removeAfterEvictForIllegalState", String.class);
        final InvocationContext ic = mockContext(method, "k4");
        when(ic.proceed()).thenThrow(new IllegalStateException("included"));

        assertThrows(IllegalStateException.class, () -> interceptor.cache(ic));
        assertNull(cachedValue("cri-evictForCache", "k4"));
    }

    @Test
    void synchronousCheckedExceptionPropagatesUnwrapped() throws Throwable {
        final Method method = Target.class.getDeclaredMethod("removeThrowsChecked", String.class);
        final InvocationContext ic = mockContext(method, "k5");
        final IOException checked = new IOException("checked-failure");
        when(ic.proceed()).thenThrow(checked);

        final Throwable thrown = assertThrows(IOException.class, () -> interceptor.cache(ic));
        assertSame(checked, thrown);
    }

    @Test
    void asyncFailureDoesNotPropagateToTheCaller() throws Throwable {
        seed("cri-asyncCache", "k6", "v6");
        final Method method = Target.class.getDeclaredMethod("removeAsync", String.class);
        final InvocationContext ic = mockContext(method, "k6");
        final CompletableFuture<String> future = new CompletableFuture<>();
        when(ic.proceed()).thenReturn(future);

        final Object result = interceptor.cache(ic);
        assertSame(future, result);

        future.completeExceptionally(new IOException("async-checked-failure"));
    }

    private static void seed(final String cacheName, final String key, final String value) {
        final CacheManager manager = Caching.getCachingProvider().getCacheManager();
        Cache<Object, Object> cache = manager.getCache(cacheName);
        if (cache == null) {
            cache = manager.createCache(cacheName, new MutableConfiguration<>().setStoreByValue(false));
        }
        cache.put(new GeneratedCacheKeyImpl(new Object[] { key }), value);
        assertNotNull(cachedValue(cacheName, key), "seeding must be visible before the interceptor runs");
    }

    private static Object cachedValue(final String cacheName, final Object key) {
        final Cache<Object, Object> cache = Caching.getCachingProvider().getCacheManager().getCache(cacheName);
        return cache == null ? null : cache.get(new GeneratedCacheKeyImpl(new Object[] { key }));
    }

    private InvocationContext mockContext(final Method method, final String key) {
        final InvocationContext ic = mock(InvocationContext.class);
        when(ic.getMethod()).thenReturn(method);
        when(ic.getTarget()).thenReturn(target);
        when(ic.getParameters()).thenReturn(new Object[] { key });
        return ic;
    }

    private static class Target {

        @CacheRemove(cacheName = "cri-beforeCache", afterInvocation = false)
        void removeBefore(@CacheKey final String id) {
            // no-op
        }

        @CacheRemove(cacheName = "cri-afterCache", afterInvocation = true)
        void removeAfter(@CacheKey final String id) {
            // no-op
        }

        @CacheRemove(cacheName = "cri-evictForCache", afterInvocation = true,
                evictFor = IllegalStateException.class)
        void removeAfterEvictForIllegalState(@CacheKey final String id) {
            // no-op
        }

        @CacheRemove(cacheName = "cri-checkedCache", afterInvocation = false)
        void removeThrowsChecked(@CacheKey final String id) throws IOException {
            // no-op
        }

        @CacheRemove(cacheName = "cri-asyncCache", afterInvocation = false)
        java.util.concurrent.CompletionStage<String> removeAsync(@CacheKey final String id) {
            return null;
        }
    }
}
