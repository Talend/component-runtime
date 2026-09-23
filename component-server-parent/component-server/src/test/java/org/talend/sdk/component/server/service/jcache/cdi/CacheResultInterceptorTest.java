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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;

import javax.cache.annotation.CacheKey;
import javax.cache.annotation.CacheResult;

import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.interceptor.InvocationContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Exercises {@link CacheResultInterceptor} against a real {@link CDIJCacheHelper} and the default
 * (in-memory, geronimo-jcache-simple provided) JCache implementation - no live external system is
 * involved, per java-testing-conventions.md.
 */
class CacheResultInterceptorTest {

    private CDIJCacheHelper helper;

    private CacheResultInterceptor interceptor;

    private Target target;

    @BeforeEach
    void setUp() throws Exception {
        helper = new CDIJCacheHelper();
        final java.lang.reflect.Field field = CDIJCacheHelper.class.getDeclaredField("beanManager");
        field.setAccessible(true);
        field.set(helper, mock(BeanManager.class));
        interceptor = new CacheResultInterceptor(helper);
        target = new Target();
    }

    @Test
    void cacheMissInvokesMethodAndCachesResult() throws Throwable {
        final Method method = Target.class.getDeclaredMethod("load", String.class);
        final InvocationContext ic = mockContext(method, "k1");
        when(ic.proceed()).thenReturn("computed-1");

        final Object first = interceptor.cache(ic);
        assertEquals("computed-1", first);
        verify(ic, times(1)).proceed();
    }

    @Test
    void cacheHitSkipsSecondInvocation() throws Throwable {
        final Method method = Target.class.getDeclaredMethod("load", String.class);
        final InvocationContext ic1 = mockContext(method, "k2");
        when(ic1.proceed()).thenReturn("computed-2");
        interceptor.cache(ic1);

        final InvocationContext ic2 = mockContext(method, "k2");
        final Object second = interceptor.cache(ic2);

        assertEquals("computed-2", second);
        verify(ic2, never()).proceed();
    }

    @Test
    void differentKeysAreCachedIndependently() throws Throwable {
        final Method method = Target.class.getDeclaredMethod("load", String.class);
        final InvocationContext icA = mockContext(method, "a");
        when(icA.proceed()).thenReturn("value-a");
        interceptor.cache(icA);

        final InvocationContext icB = mockContext(method, "b");
        when(icB.proceed()).thenReturn("value-b");
        final Object result = interceptor.cache(icB);

        assertEquals("value-b", result);
        verify(icB, times(1)).proceed();
    }

    @Test
    void skipGetAlwaysInvokesTheMethodEvenWhenAlreadyCached() throws Throwable {
        final Method method = Target.class.getDeclaredMethod("loadSkipGet", String.class);
        final InvocationContext ic1 = mockContext(method, "k3");
        when(ic1.proceed()).thenReturn("first");
        interceptor.cache(ic1);

        final InvocationContext ic2 = mockContext(method, "k3");
        when(ic2.proceed()).thenReturn("second");
        final Object result = interceptor.cache(ic2);

        assertEquals("second", result);
        verify(ic2, times(1)).proceed();
    }

    @Test
    void includedExceptionIsCachedAndReplayedWithoutReInvokingTheMethod() throws Throwable {
        final Method method = Target.class.getDeclaredMethod("loadCachingExceptions", String.class);
        final InvocationContext ic1 = mockContext(method, "k4");
        final IllegalStateException failure = new IllegalStateException("boom");
        when(ic1.proceed()).thenThrow(failure);

        final Throwable firstThrown = assertThrows(IllegalStateException.class, () -> interceptor.cache(ic1));
        assertSame(failure, firstThrown);

        final InvocationContext ic2 = mockContext(method, "k4");
        final Throwable secondThrown = assertThrows(Throwable.class, () -> interceptor.cache(ic2));

        assertSame(failure, secondThrown);
        verify(ic2, never()).proceed();
    }

    @Test
    void nonIncludedExceptionIsNotCachedAndTheMethodIsReInvoked() throws Throwable {
        final Method method = Target.class.getDeclaredMethod("loadNotCachingExceptions", String.class);
        final InvocationContext ic1 = mockContext(method, "k5");
        when(ic1.proceed()).thenThrow(new IllegalArgumentException("first-failure"));
        assertThrows(IllegalArgumentException.class, () -> interceptor.cache(ic1));

        final InvocationContext ic2 = mockContext(method, "k5");
        when(ic2.proceed()).thenThrow(new IllegalArgumentException("second-failure"));
        assertThrows(IllegalArgumentException.class, () -> interceptor.cache(ic2));

        verify(ic2, times(1)).proceed();
    }

    @Test
    void asyncResultIsCachedAndFailureCallbackDoesNotPropagate() throws Throwable {
        final Method method = Target.class.getDeclaredMethod("loadAsync", String.class);
        final InvocationContext ic = mockContext(method, "k6");
        final CompletableFuture<String> future = new CompletableFuture<>();
        when(ic.proceed()).thenReturn(future);

        final Object result = interceptor.cache(ic);
        assertSame(future, result);

        // completing exceptionally must not throw back to the caller - onAsyncFailure runs as a
        // dependent stage action and its own (re-)throw is captured by the CompletionStage machinery.
        future.completeExceptionally(new IllegalStateException("async-boom"));

        final InvocationContext ic2 = mockContext(method, "k6");
        final Object second = interceptor.cache(ic2);
        assertSame(future, second, "the failed stage remains the cached value for this key");
        verify(ic2, never()).proceed();
    }

    private InvocationContext mockContext(final Method method, final String key) {
        final InvocationContext ic = mock(InvocationContext.class);
        when(ic.getMethod()).thenReturn(method);
        when(ic.getTarget()).thenReturn(target);
        when(ic.getParameters()).thenReturn(new Object[] { key });
        return ic;
    }

    private static class Target {

        @CacheResult(cacheName = "cri-resultCache")
        String load(@CacheKey final String id) {
            return null;
        }

        @CacheResult(cacheName = "cri-skipGetCache", skipGet = true)
        String loadSkipGet(@CacheKey final String id) {
            return null;
        }

        @CacheResult(cacheName = "cri-excCache", exceptionCacheName = "cri-excCache-ex",
                cachedExceptions = IllegalStateException.class)
        String loadCachingExceptions(@CacheKey final String id) {
            return null;
        }

        @CacheResult(cacheName = "cri-notCachedExcCache", exceptionCacheName = "cri-notCachedExcCache-ex",
                nonCachedExceptions = IllegalArgumentException.class)
        String loadNotCachingExceptions(@CacheKey final String id) {
            return null;
        }

        @CacheResult(cacheName = "cri-asyncCache", exceptionCacheName = "cri-asyncCache-ex",
                cachedExceptions = IllegalStateException.class)
        java.util.concurrent.CompletionStage<String> loadAsync(@CacheKey final String id) {
            return null;
        }
    }
}
