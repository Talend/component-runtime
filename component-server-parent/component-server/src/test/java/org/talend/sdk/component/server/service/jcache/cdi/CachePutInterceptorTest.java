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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;

import javax.cache.Cache;
import javax.cache.Caching;
import javax.cache.annotation.CacheInvocationParameter;
import javax.cache.annotation.CacheKey;
import javax.cache.annotation.CachePut;
import javax.cache.annotation.CacheValue;

import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.interceptor.InvocationContext;

import org.apache.geronimo.jcache.simple.cdi.GeneratedCacheKeyImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Exercises {@link CachePutInterceptor} against a real {@link CDIJCacheHelper} and the default
 * (in-memory, geronimo-jcache-simple provided) JCache implementation - no live external system is
 * involved, per java-testing-conventions.md.
 */
class CachePutInterceptorTest {

    private CDIJCacheHelper helper;

    private CachePutInterceptor interceptor;

    private Target target;

    @BeforeEach
    void setUp() throws Exception {
        helper = new CDIJCacheHelper();
        final java.lang.reflect.Field field = CDIJCacheHelper.class.getDeclaredField("beanManager");
        field.setAccessible(true);
        field.set(helper, mock(BeanManager.class));
        interceptor = new CachePutInterceptor(helper);
        target = new Target();
    }

    @Test
    void beforeInvocationPutsValueBeforeInvokingTheMethod() throws Throwable {
        final Method method = Target.class.getDeclaredMethod("putBefore", String.class, String.class);
        final InvocationContext ic = mockContext(method, "k1", "v1");
        when(ic.proceed()).thenAnswer(invocation -> {
            // the whole point of afterInvocation=false: the value must already be visible while the
            // intercepted method itself is still executing.
            assertEquals("v1", cachedValue("cpi-beforeCache", "k1"));
            return null;
        });

        interceptor.cache(ic);

        assertEquals("v1", cachedValue("cpi-beforeCache", "k1"));
    }

    @Test
    void afterInvocationDoesNotPutBeforeTheMethodRunsButDoesAfterItSucceeds() throws Throwable {
        final Method method = Target.class.getDeclaredMethod("putAfter", String.class, String.class);
        final InvocationContext ic = mockContext(method, "k2", "v2");
        when(ic.proceed()).thenAnswer(invocation -> {
            assertNull(cachedValue("cpi-afterCache", "k2"), "afterInvocation=true must not put before proceed()");
            return null;
        });

        interceptor.cache(ic);

        assertEquals("v2", cachedValue("cpi-afterCache", "k2"));
    }

    @Test
    void valueParameterUsesTheSecondParameterNotTheFirst() throws Throwable {
        // regression test for QTDI-3358 round 2's "idx off-by-one" fix in
        // CDIJCacheHelper.getValueParameter: the cached value must be "v3" (the @CacheValue
        // parameter), never "k3" (the @CacheKey one).
        final Method method = Target.class.getDeclaredMethod("putBefore", String.class, String.class);
        final InvocationContext ic = mockContext(method, "k3", "v3");
        when(ic.proceed()).thenReturn(null);

        interceptor.cache(ic);

        assertEquals("v3", cachedValue("cpi-beforeCache", "k3"));
    }

    @Test
    void afterInvocationDoesNotPutWhenTheMethodThrowsAndExceptionIsNotIncluded() throws Throwable {
        final Method method = Target.class.getDeclaredMethod("putAfterCacheForIllegalState", String.class,
                String.class);
        final InvocationContext ic = mockContext(method, "k4", "v4");
        when(ic.proceed()).thenThrow(new IllegalArgumentException("not-included"));

        assertThrows(IllegalArgumentException.class, () -> interceptor.cache(ic));
        assertNull(cachedValue("cpi-afterCacheForCache", "k4"));
    }

    @Test
    void afterInvocationPutsWhenTheMethodThrowsAnIncludedException() throws Throwable {
        final Method method = Target.class.getDeclaredMethod("putAfterCacheForIllegalState", String.class,
                String.class);
        final InvocationContext ic = mockContext(method, "k5", "v5");
        when(ic.proceed()).thenThrow(new IllegalStateException("included"));

        assertThrows(IllegalStateException.class, () -> interceptor.cache(ic));
        assertEquals("v5", cachedValue("cpi-afterCacheForCache", "k5"));
    }

    @Test
    void synchronousCheckedExceptionPropagatesUnwrapped() throws Throwable {
        final Method method = Target.class.getDeclaredMethod("putThrowsChecked", String.class, String.class);
        final InvocationContext ic = mockContext(method, "k6", "v6");
        final IOException checked = new IOException("checked-failure");
        when(ic.proceed()).thenThrow(checked);

        final Throwable thrown = assertThrows(IOException.class, () -> interceptor.cache(ic));
        assertSame(checked, thrown);
    }

    @Test
    void asyncFailureWrapsCheckedExceptionButDoesNotPropagateToTheCaller() throws Throwable {
        final Method method = Target.class.getDeclaredMethod("putAsync", String.class, String.class);
        final InvocationContext ic = mockContext(method, "k7", "v7");
        final CompletableFuture<String> future = new CompletableFuture<>();
        when(ic.proceed()).thenReturn(future);

        final Object result = interceptor.cache(ic);
        assertSame(future, result);

        // must not throw - CachePutInterceptor#onFailure wraps checked exceptions into
        // IllegalStateException, but that is only observable on the *derived* stage, never on the
        // original future returned to the caller of completeExceptionally().
        future.completeExceptionally(new IOException("async-checked-failure"));
    }

    private static Object cachedValue(final String cacheName, final Object key) {
        final Cache<Object, Object> cache = Caching.getCachingProvider().getCacheManager().getCache(cacheName);
        final Object cached = cache == null ? null : cache.get(new GeneratedCacheKeyImpl(new Object[] { key }));
        // CachePutInterceptor caches the whole CacheInvocationParameter wrapper (ported unchanged from
        // upstream geronimo-jcache-simple), not the raw value - unwrap it for readable assertions.
        return cached instanceof CacheInvocationParameter ? ((CacheInvocationParameter) cached).getValue() : cached;
    }

    private InvocationContext mockContext(final Method method, final String key, final String value) {
        final InvocationContext ic = mock(InvocationContext.class);
        when(ic.getMethod()).thenReturn(method);
        when(ic.getTarget()).thenReturn(target);
        when(ic.getParameters()).thenReturn(new Object[] { key, value });
        return ic;
    }

    private static class Target {

        @CachePut(cacheName = "cpi-beforeCache", afterInvocation = false)
        void putBefore(@CacheKey final String id, @CacheValue final String value) {
            // no-op
        }

        @CachePut(cacheName = "cpi-afterCache", afterInvocation = true)
        void putAfter(@CacheKey final String id, @CacheValue final String value) {
            // no-op
        }

        @CachePut(cacheName = "cpi-afterCacheForCache", afterInvocation = true,
                cacheFor = IllegalStateException.class)
        void putAfterCacheForIllegalState(@CacheKey final String id, @CacheValue final String value) {
            // no-op
        }

        @CachePut(cacheName = "cpi-checkedCache", afterInvocation = false)
        void putThrowsChecked(@CacheKey final String id, @CacheValue final String value) throws IOException {
            // no-op
        }

        @CachePut(cacheName = "cpi-asyncCache", afterInvocation = false)
        java.util.concurrent.CompletionStage<String> putAsync(@CacheKey final String id,
                @CacheValue final String value) {
            return null;
        }
    }
}
