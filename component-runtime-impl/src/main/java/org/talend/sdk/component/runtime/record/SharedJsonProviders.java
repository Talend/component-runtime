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

import javax.json.bind.spi.JsonbProvider;
import javax.json.spi.JsonProvider;

import lombok.NoArgsConstructor;

/**
 * Holds one {@link JsonProvider} and one {@link JsonbProvider} per classloader of the framework.
 * <p>
 * {@link JsonProvider#provider()} and {@link JsonbProvider#provider()} create a new
 * {@link java.util.ServiceLoader} at each call, which reads jars and takes locks: under heavy parallel load
 * this makes threads block each other. Both providers are thread safe so a single instance is enough.
 * <p>
 * Scope: this holder is a global (framework classloader) singleton, resolved lazily on first use. It is only
 * meant for framework internal call sites which have no container context and used to call
 * {@code provider()} directly. It must <b>not</b> be used to serve component code living in an isolated
 * container: those keep using the per container providers (see {@code ComponentManager} and the
 * {@code DefaultServiceProvider}), so a container embedding its own JSON-P implementation is never given the
 * framework one.
 */
@NoArgsConstructor(access = PRIVATE)
public final class SharedJsonProviders {

    public static JsonProvider jsonProvider() {
        return JsonProviderHolder.INSTANCE;
    }

    public static JsonbProvider jsonbProvider() {
        return JsonbProviderHolder.INSTANCE;
    }

    private static final class JsonProviderHolder {

        private static final JsonProvider INSTANCE = JsonProvider.provider();
    }

    private static final class JsonbProviderHolder {

        private static final JsonbProvider INSTANCE = JsonbProvider.provider();
    }
}
