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
package org.talend.sdk.component.runtime.record;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.bind.Jsonb;
import javax.json.bind.spi.JsonbProvider;
import javax.json.spi.JsonProvider;

import org.junit.jupiter.api.Test;

class SharedJsonProvidersTest {

    @Test
    void sameInstanceAcrossCalls() {
        assertNotNull(SharedJsonProviders.jsonProvider());
        assertSame(SharedJsonProviders.jsonProvider(), SharedJsonProviders.jsonProvider());
        assertNotNull(SharedJsonProviders.jsonbProvider());
        assertSame(SharedJsonProviders.jsonbProvider(), SharedJsonProviders.jsonbProvider());
    }

    @Test
    void sameInstanceAcrossThreads() throws Exception {
        final int threads = 16;
        final JsonProvider expectedJson = SharedJsonProviders.jsonProvider();
        final JsonbProvider expectedJsonb = SharedJsonProviders.jsonbProvider();
        final ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            final CountDownLatch start = new CountDownLatch(1);
            final List<Future<Object[]>> futures = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                futures.add(pool.submit(() -> {
                    start.await();
                    return new Object[] { SharedJsonProviders.jsonProvider(), SharedJsonProviders.jsonbProvider() };
                }));
            }
            start.countDown();
            for (final Future<Object[]> future : futures) {
                final Object[] providers = future.get(30, TimeUnit.SECONDS);
                assertSame(expectedJson, providers[0]);
                assertSame(expectedJsonb, providers[1]);
            }
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void providersAreUsable() throws Exception {
        final JsonObject object = SharedJsonProviders.jsonProvider().createObjectBuilder().add("a", "b").build();
        try (final Jsonb jsonb = SharedJsonProviders.jsonbProvider().create().build()) {
            assertTrue(jsonb.toJson(object).contains("\"a\""));
        }
    }

    @Test
    void lookupUsesFrameworkClassLoaderAndRestoresCaller() throws Exception {
        final Thread thread = Thread.currentThread();
        final ClassLoader caller = thread.getContextClassLoader();
        try (final URLClassLoader isolated = new URLClassLoader(new URL[0], null);
                final URLClassLoader framework = new URLClassLoader(new URL[0], null)) {
            thread.setContextClassLoader(isolated);
            final SharedJsonProviders.Lazy<ClassLoader> lazy =
                    new SharedJsonProviders.Lazy<>(() -> Thread.currentThread().getContextClassLoader(), framework);
            assertSame(framework, lazy.get());
            assertSame(isolated, thread.getContextClassLoader());
        } finally {
            thread.setContextClassLoader(caller);
        }
    }

    @Test
    void realProvidersAreNotBoundToIsolatedContextClassLoader() throws Exception {
        final Thread thread = Thread.currentThread();
        final ClassLoader caller = thread.getContextClassLoader();
        final ClassLoader framework = SharedJsonProviders.class.getClassLoader();
        try (final URLClassLoader isolated = new URLClassLoader(new URL[0], null)) {
            thread.setContextClassLoader(isolated);
            final JsonProvider json = new SharedJsonProviders.Lazy<>(JsonProvider::provider, framework).get();
            final JsonbProvider jsonb = new SharedJsonProviders.Lazy<>(JsonbProvider::provider, framework).get();
            assertNotNull(json);
            assertNotNull(jsonb);
            assertNotSame(isolated, json.getClass().getClassLoader());
            assertNotSame(isolated, jsonb.getClass().getClassLoader());
        } finally {
            thread.setContextClassLoader(caller);
        }
    }

    @Test
    void callerClassLoaderFallbackIsNeverCached() throws Exception {
        final Thread thread = Thread.currentThread();
        final ClassLoader caller = thread.getContextClassLoader();
        try (final URLClassLoader framework = new URLClassLoader(new URL[0], null);
                final URLClassLoader container = new URLClassLoader(new URL[0], null)) {
            final AtomicBoolean frameworkAvailable = new AtomicBoolean();
            final SharedJsonProviders.Lazy<String> lazy = new SharedJsonProviders.Lazy<>(() -> {
                final ClassLoader current = Thread.currentThread().getContextClassLoader();
                if (current == framework && !frameworkAvailable.get()) {
                    throw new JsonException("not found in the framework classloader");
                }
                return current == framework ? "from-framework" : "from-container";
            }, framework);

            thread.setContextClassLoader(container);
            // served from the caller, as provider() did, and the caller loader is put back
            assertEquals("from-container", lazy.get());
            assertSame(container, thread.getContextClassLoader());
            // not retained: still resolved again, from the caller
            assertEquals("from-container", lazy.get());

            // once the framework loader provides an implementation it wins and is the one cached
            frameworkAvailable.set(true);
            assertEquals("from-framework", lazy.get());
            assertEquals("from-framework", lazy.get());
            assertSame(container, thread.getContextClassLoader());
        } finally {
            thread.setContextClassLoader(caller);
        }
    }

    @Test
    void originalFailureIsPropagatedWithCallerFailureSuppressed() throws Exception {
        final Thread thread = Thread.currentThread();
        final ClassLoader caller = thread.getContextClassLoader();
        try (final URLClassLoader framework = new URLClassLoader(new URL[0], null);
                final URLClassLoader custom = new URLClassLoader(new URL[0], null)) {
            thread.setContextClassLoader(custom);
            final AtomicInteger calls = new AtomicInteger();
            final SharedJsonProviders.Lazy<String> lazy = new SharedJsonProviders.Lazy<>(() -> {
                throw new JsonException("failure #" + calls.incrementAndGet());
            }, framework);
            final JsonException error = assertThrows(JsonException.class, lazy::get);
            assertEquals("failure #1", error.getMessage());
            assertEquals(1, error.getSuppressed().length);
            assertEquals("failure #2", error.getSuppressed()[0].getMessage());
            assertSame(custom, thread.getContextClassLoader());
        } finally {
            thread.setContextClassLoader(caller);
        }
    }

    @Test
    void initializationFailureIsRethrownAsIsAndRetried() {
        final ClassLoader framework = SharedJsonProviders.class.getClassLoader();
        final AtomicInteger calls = new AtomicInteger();
        final SharedJsonProviders.Lazy<String> lazy = new SharedJsonProviders.Lazy<>(() -> {
            if (calls.incrementAndGet() == 1) {
                throw new JsonException("not ready");
            }
            return "ok";
        }, framework);

        // a RuntimeException (not an ExceptionInInitializerError) so callers fallbacks can catch it
        assertThrows(JsonException.class, lazy::get);
        // not cached: next call retries and then stays resolved
        assertEquals("ok", lazy.get());
        assertSame(lazy.get(), lazy.get());
        assertEquals(2, calls.get());
    }
}
