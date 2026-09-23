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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.annotation.CachePut;
import javax.cache.annotation.CacheRemoveAll;
import javax.cache.configuration.MutableConfiguration;

import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.interceptor.InvocationContext;

import org.apache.geronimo.jcache.simple.cdi.GeneratedCacheKeyImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Exercises {@link CacheRemoveAllInterceptor} against a real {@link CDIJCacheHelper} and the default
 * (in-memory, geronimo-jcache-simple provided) JCache implementation - no live external system is
 * involved, per java-testing-conventions.md.
 */
class CacheRemoveAllInterceptorTest {

    private CDIJCacheHelper helper;

    private CacheRemoveAllInterceptor interceptor;

    private Target target;

    @BeforeEach
    void setUp() throws Exception {
        helper = new CDIJCacheHelper();
        final java.lang.reflect.Field field = CDIJCacheHelper.class.getDeclaredField("beanManager");
        field.setAccessible(true);
        field.set(helper, mock(BeanManager.class));
        interceptor = new CacheRemoveAllInterceptor(helper);
        target = new Target();
    }

    @Test
    void beforeInvocationClearsBeforeTheMethodRuns() throws Throwable {
        seed("crai-beforeCache", "k1", "v1");
        final Method method = Target.class.getDeclaredMethod("removeAllBefore");
        final InvocationContext ic = mockContext(method);
        when(ic.proceed()).thenAnswer(invocation -> {
            assertTrue(isEmpty("crai-beforeCache"), "afterInvocation=false must clear before proceed()");
            return null;
        });

        interceptor.cache(ic);

        assertTrue(isEmpty("crai-beforeCache"));
    }

    @Test
    void afterInvocationClearsOnlyAfterTheMethodSucceeds() throws Throwable {
        seed("crai-afterCache", "k2", "v2");
        final Method method = Target.class.getDeclaredMethod("removeAllAfter");
        final InvocationContext ic = mockContext(method);
        when(ic.proceed()).thenAnswer(invocation -> {
            assertFalse(isEmpty("crai-afterCache"), "afterInvocation=true must not clear before proceed()");
            return null;
        });

        interceptor.cache(ic);

        assertTrue(isEmpty("crai-afterCache"));
    }

    @Test
    void readsItsOwnAfterInvocationFlagEvenWhenACoLocatedCachePutSaysOtherwise() throws Throwable {
        // regression test for QTDI-3358 round 2 (commit ba099ea4579e): CacheRemoveAllInterceptor must
        // read @CacheRemoveAll's OWN afterInvocation() flag, never a co-located @CachePut's flag. The
        // target method below deliberately combines afterInvocation=false on @CacheRemoveAll with
        // afterInvocation=true on the co-located @CachePut, so a regression back to reading the wrong
        // annotation would flip this test's outcome.
        seed("crai-mixedCache", "k3", "v3");
        final Method method = Target.class.getDeclaredMethod("removeAllBeforeWithCoLocatedAfterPut", String.class,
                String.class);
        final InvocationContext ic = mockContext(method, "id3", "value3");
        when(ic.proceed()).thenAnswer(invocation -> {
            assertTrue(isEmpty("crai-mixedCache"),
                    "must honour @CacheRemoveAll(afterInvocation=false), not @CachePut(afterInvocation=true)");
            return null;
        });

        interceptor.cache(ic);
    }

    @Test
    void afterInvocationDoesNotClearWhenTheExceptionIsNotIncluded() throws Throwable {
        seed("crai-evictForCache", "k4", "v4");
        final Method method = Target.class.getDeclaredMethod("removeAllAfterEvictForIllegalState");
        final InvocationContext ic = mockContext(method);
        when(ic.proceed()).thenThrow(new IllegalArgumentException("not-included"));

        assertThrows(IllegalArgumentException.class, () -> interceptor.cache(ic));
        assertFalse(isEmpty("crai-evictForCache"));
    }

    @Test
    void afterInvocationClearsWhenTheExceptionIsIncluded() throws Throwable {
        seed("crai-evictForCache2", "k5", "v5");
        final Method method = Target.class.getDeclaredMethod("removeAllAfterEvictForIllegalState2");
        final InvocationContext ic = mockContext(method);
        when(ic.proceed()).thenThrow(new IllegalStateException("included"));

        assertThrows(IllegalStateException.class, () -> interceptor.cache(ic));
        assertTrue(isEmpty("crai-evictForCache2"));
    }

    @Test
    void synchronousCheckedExceptionPropagatesUnwrapped() throws Throwable {
        final Method method = Target.class.getDeclaredMethod("removeAllThrowsChecked");
        final InvocationContext ic = mockContext(method);
        final IOException checked = new IOException("checked-failure");
        when(ic.proceed()).thenThrow(checked);

        final Throwable thrown = assertThrows(IOException.class, () -> interceptor.cache(ic));
        assertSame(checked, thrown);
    }

    @Test
    void asyncFailureDoesNotPropagateToTheCaller() throws Throwable {
        final Method method = Target.class.getDeclaredMethod("removeAllAsync");
        final InvocationContext ic = mockContext(method);
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
    }

    private static boolean isEmpty(final String cacheName) {
        final Cache<Object, Object> cache = Caching.getCachingProvider().getCacheManager().getCache(cacheName);
        if (cache == null) {
            return true;
        }
        for (final Cache.Entry<Object, Object> ignored : cache) {
            return false;
        }
        return true;
    }

    private InvocationContext mockContext(final Method method, final Object... parameters) {
        final InvocationContext ic = mock(InvocationContext.class);
        when(ic.getMethod()).thenReturn(method);
        when(ic.getTarget()).thenReturn(target);
        when(ic.getParameters()).thenReturn(parameters);
        return ic;
    }

    private static class Target {

        @CacheRemoveAll(cacheName = "crai-beforeCache", afterInvocation = false)
        void removeAllBefore() {
            // no-op
        }

        @CacheRemoveAll(cacheName = "crai-afterCache", afterInvocation = true)
        void removeAllAfter() {
            // no-op
        }

        @CacheRemoveAll(cacheName = "crai-mixedCache", afterInvocation = false)
        @CachePut(cacheName = "crai-mixedCache", afterInvocation = true)
        void removeAllBeforeWithCoLocatedAfterPut(final String id, final String value) {
            // no-op
        }

        @CacheRemoveAll(cacheName = "crai-evictForCache", afterInvocation = true,
                evictFor = IllegalStateException.class)
        void removeAllAfterEvictForIllegalState() {
            // no-op
        }

        @CacheRemoveAll(cacheName = "crai-evictForCache2", afterInvocation = true,
                evictFor = IllegalStateException.class)
        void removeAllAfterEvictForIllegalState2() {
            // no-op
        }

        @CacheRemoveAll(cacheName = "crai-checkedCache", afterInvocation = false)
        void removeAllThrowsChecked() throws IOException {
            // no-op
        }

        @CacheRemoveAll(cacheName = "crai-asyncCache", afterInvocation = false)
        java.util.concurrent.CompletionStage<String> removeAllAsync() {
            return null;
        }
    }
}
