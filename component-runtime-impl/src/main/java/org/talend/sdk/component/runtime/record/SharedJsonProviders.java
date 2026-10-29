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
 * The instances are resolved lazily on first use, with the classloader of the framework (falling back on the
 * caller context classloader if that lookup fails). If both lookups fail the failure is propagated as is and
 * the next call tries again. They are meant for framework internal call sites which have no container context
 * and used to call {@code provider()} directly. They must <b>not</b> be used to serve component code living in
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
    // bind the global instance to the classloader of an isolated container, so use the framework one first.
    // If it does not provide an implementation, fallback on the context classloader of the caller.
    static <T> T withFrameworkClassLoader(final Supplier<T> lookup, final ClassLoader framework) {
        final Thread thread = Thread.currentThread();
        final ClassLoader caller = thread.getContextClassLoader();
        try {
            thread.setContextClassLoader(framework);
            return lookup.get();
        } catch (final RuntimeException | ServiceConfigurationError frameworkFailure) {
            if (caller == null || caller == framework) {
                throw frameworkFailure;
            }
            thread.setContextClassLoader(caller);
            try {
                return lookup.get();
            } catch (final RuntimeException | ServiceConfigurationError callerFailure) {
                frameworkFailure.addSuppressed(callerFailure);
                throw frameworkFailure;
            }
        } finally {
            thread.setContextClassLoader(caller);
        }
    }

    // not a static holder idiom on purpose: a failing initializer would surface as an
    // ExceptionInInitializerError then a NoClassDefFoundError, bypassing the callers fallbacks
    // (catch RuntimeException) and never allowing a retry.
    static final class Lazy<T> {

        private final Supplier<T> factory;

        private final ClassLoader framework;

        private volatile T value;

        Lazy(final Supplier<T> factory, final ClassLoader framework) {
            this.factory = factory;
            this.framework = framework;
        }

        T get() {
            T instance = value;
            if (instance == null) {
                synchronized (this) {
                    instance = value;
                    if (instance == null) {
                        instance = withFrameworkClassLoader(factory, framework);
                        value = instance;
                    }
                }
            }
            return instance;
        }
    }
}
