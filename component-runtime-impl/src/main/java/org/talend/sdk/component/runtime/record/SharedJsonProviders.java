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

import static lombok.AccessLevel.PRIVATE;

import java.util.ServiceConfigurationError;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import javax.json.bind.spi.JsonbProvider;
import javax.json.spi.JsonProvider;

import lombok.NoArgsConstructor;

/**
 * <b>Internal</b>, not a public API: may change or disappear without notice.
 * <p>
 * Holds one JVM wide {@link JsonProvider} and one {@link JsonbProvider}.
 * {@link JsonProvider#provider()} and {@link JsonbProvider#provider()} create a new
 * {@link java.util.ServiceLoader} at each call, which reads jars and takes locks: under heavy parallel load
 * this makes threads block each other. Both providers are thread safe so a single instance is enough.
 * <p>
 * The instances are resolved lazily on first use, with the classloader of the framework. If that lookup fails,
 * the call falls back on the caller context classloader <b>without caching the result</b>, so a provider coming
 * from an isolated container is never shared nor retained, and the next call tries the framework again. If
 * both lookups fail the framework failure is propagated (the other one is suppressed).
 * <p>
 * They are meant for framework internal call sites which have no container context and used to call
 * {@code provider()} directly. They must <b>not</b> be used to serve component code living in
 * an isolated container: those keep using the per container providers (see {@code ComponentManager}), so a
 * container embedding its own JSON-P implementation is never given the framework one.
 */
@NoArgsConstructor(access = PRIVATE)
public final class SharedJsonProviders {

    private static final Lazy<JsonProvider> JSON_PROVIDER =
            new Lazy<>(JsonProvider::provider, SharedJsonProviders.class.getClassLoader());

    private static final Lazy<JsonbProvider> JSONB_PROVIDER =
            new Lazy<>(JsonbProvider::provider, SharedJsonProviders.class.getClassLoader());

    public static JsonProvider jsonProvider() {
        return JSON_PROVIDER.get();
    }

    public static JsonbProvider jsonbProvider() {
        return JSONB_PROVIDER.get();
    }

    // provider() looks up the implementation through the thread context classloader: the first caller must not
    // bind the global instance to the classloader of an isolated container, so use the framework one.
    static <T> T withClassLoader(final Supplier<T> lookup, final ClassLoader classLoader) {
        final Thread thread = Thread.currentThread();
        final ClassLoader previous = thread.getContextClassLoader();
        try {
            thread.setContextClassLoader(classLoader);
            return lookup.get();
        } finally {
            thread.setContextClassLoader(previous);
        }
    }

    // not a static holder idiom on purpose: a failing initializer would surface as an
    // ExceptionInInitializerError then a NoClassDefFoundError, bypassing the callers fallbacks
    // (catch RuntimeException) and never allowing a retry.
    static final class Lazy<T> {

        private final Supplier<T> factory;

        private final ClassLoader framework;

        private final AtomicReference<T> value = new AtomicReference<>();

        Lazy(final Supplier<T> factory, final ClassLoader framework) {
            this.factory = factory;
            this.framework = framework;
        }

        T get() {
            final T instance = value.get();
            return instance != null ? instance : resolve();
        }

        private T resolve() {
            try {
                synchronized (this) {
                    T instance = value.get();
                    if (instance == null) {
                        instance = withClassLoader(factory, framework);
                        value.set(instance);
                    }
                    return instance;
                }
            } catch (final RuntimeException | ServiceConfigurationError frameworkFailure) {
                // The framework classloader does not provide an implementation: serve this call from the caller
                // context classloader, but never cache that result. It may belong to an isolated container, and
                // a global instance would then leak the container classloader and hand its implementation to
                // every other caller. The next call retries the framework lookup.
                final ClassLoader caller = Thread.currentThread().getContextClassLoader();
                if (caller == null || caller == framework) {
                    throw frameworkFailure;
                }
                try {
                    return withClassLoader(factory, caller);
                } catch (final RuntimeException | ServiceConfigurationError callerFailure) {
                    frameworkFailure.addSuppressed(callerFailure);
                    throw frameworkFailure;
                }
            }
        }
    }
}
