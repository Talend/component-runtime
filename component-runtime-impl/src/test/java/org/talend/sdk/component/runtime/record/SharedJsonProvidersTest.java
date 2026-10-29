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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.json.JsonObject;
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
        final ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            final CountDownLatch start = new CountDownLatch(1);
            final Set<Object> providers = ConcurrentHashMap.newKeySet();
            final List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                futures.add(pool.submit(() -> {
                    start.await();
                    providers.add(SharedJsonProviders.jsonProvider());
                    providers.add(SharedJsonProviders.jsonbProvider());
                    return null;
                }));
            }
            start.countDown();
            for (final Future<?> future : futures) {
                future.get(30, TimeUnit.SECONDS);
            }
            // one JsonProvider + one JsonbProvider
            assertEquals(2, providers.size(), () -> "unexpected providers: " + providers);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void providersAreUsable() throws Exception {
        final JsonObject object = SharedJsonProviders.jsonProvider().createObjectBuilder().add("a", "b").build();
        try (final javax.json.bind.Jsonb jsonb = SharedJsonProviders.jsonbProvider().create().build()) {
            assertTrue(jsonb.toJson(object).contains("\"a\""));
        }
    }

    @Test
    void initializationIgnoresIsolatedContextClassLoader() throws Exception {
        final URL[] classpath = frameworkClasspath();
        final Thread thread = Thread.currentThread();
        final ClassLoader caller = thread.getContextClassLoader();
        // fresh holder class (own static state) in a loader which does not delegate SharedJsonProviders
        try (final URLClassLoader framework = new URLClassLoader(classpath, ClassLoader.getPlatformClassLoader());
                final URLClassLoader isolated = new URLClassLoader(new URL[0], null)) {
            thread.setContextClassLoader(isolated);
            final Object provider = framework
                    .loadClass(SharedJsonProviders.class.getName())
                    .getMethod("jsonProvider")
                    .invoke(null);
            assertNotNull(provider);
            // bound to the framework classloader, not to the isolated container one
            assertSame(framework, provider.getClass().getClassLoader());
            assertSame(isolated, thread.getContextClassLoader());
        } finally {
            thread.setContextClassLoader(caller);
        }
    }

    // surefire may use an isolated classloader (manifest-only jar), so java.class.path alone is not enough:
    // also use the active test classloader and the code sources of the classes the holder needs
    private static URL[] frameworkClasspath() throws Exception {
        final Set<URL> urls = new LinkedHashSet<>();
        final ClassLoader testLoader = SharedJsonProvidersTest.class.getClassLoader();
        if (testLoader instanceof URLClassLoader) {
            urls.addAll(Arrays.asList(((URLClassLoader) testLoader).getURLs()));
        }
        for (final String entry : System.getProperty("java.class.path").split(File.pathSeparator)) {
            if (!entry.isEmpty()) {
                urls.add(new File(entry).toURI().toURL());
            }
        }
        for (final Class<?> clazz : Arrays
                .<Class<?>> asList(SharedJsonProviders.class, JsonProvider.class,
                        SharedJsonProviders.jsonProvider().getClass(), JsonbProvider.class,
                        SharedJsonProviders.jsonbProvider().getClass())) {
            final CodeSource source = clazz.getProtectionDomain().getCodeSource();
            if (source != null && source.getLocation() != null) {
                urls.add(source.getLocation());
            }
        }
        return urls.toArray(new URL[0]);
    }
}
