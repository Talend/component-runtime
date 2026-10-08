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

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.reflect.Proxy;
import java.util.Map;

import jakarta.json.Json;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;
import jakarta.json.bind.spi.JsonbProvider;
import jakarta.json.spi.JsonProvider;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.record.RecordPointerFactory;
import org.talend.sdk.component.api.service.Service;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;
import org.talend.sdk.component.runtime.manager.json.PreComputedJsonpProvider;
import org.talend.sdk.component.runtime.record.RecordBuilderFactoryImpl;

class DefaultServiceProviderTest {

    private final RecordBuilderFactory factory = new RecordBuilderFactoryImpl(null);

    private DefaultServiceProvider provider(final JsonbProvider jsonbProvider) {
        return new DefaultServiceProvider(null, JsonProvider.provider(), Json.createGeneratorFactory(emptyMap()),
                Json.createReaderFactory(emptyMap()), Json.createBuilderFactory(emptyMap()),
                Json.createParserFactory(emptyMap()), Json.createWriterFactory(emptyMap()), new JsonbConfig(),
                jsonbProvider, null, null, emptyList(), t -> factory, null);
    }

    private <T> T lookup(final DefaultServiceProvider provider, final Class<T> api) {
        return provider.lookup("test", Thread.currentThread().getContextClassLoader(), null, null, api, null, null);
    }

    @Test
    void jsonpProvider() {
        assertInstanceOf(PreComputedJsonpProvider.class, lookup(provider(JsonbProvider.provider()), JsonProvider.class));
    }

    @Test
    void simpleServices() {
        final DefaultServiceProvider provider = provider(JsonbProvider.provider());
        assertNotNull(lookup(provider, RecordBuilderFactory.class));
        assertNotNull(lookup(provider, RecordPointerFactory.class));
        assertNotNull(lookup(provider, ContainerInfo.class));
        assertNull(lookup(provider, Service.class));
    }

    @Test
    void jsonbWithJohnzon() throws Exception {
        final Jsonb jsonb = lookup(provider(JsonbProvider.provider()), Jsonb.class);
        assertNotNull(jsonb);
        assertEquals("{\"a\":\"b\"}", jsonb.toJson(Map.of("a", "b")));
        jsonb.close();
    }

    @Test
    void jsonbWithNonJohnzonBuilder() throws Exception {
        final JsonbProvider delegate = JsonbProvider.provider();
        final JsonbProvider wrapping = new JsonbProvider() {

            @Override
            public JsonbBuilder create() {
                return wrap(delegate.create());
            }
        };
        final Jsonb jsonb = lookup(provider(wrapping), Jsonb.class);
        assertNotNull(jsonb);
        assertEquals("{\"a\":\"b\"}", jsonb.toJson(Map.of("a", "b")));
        jsonb.close();
    }

    private static JsonbBuilder wrap(final JsonbBuilder builder) {
        return (JsonbBuilder) Proxy
                .newProxyInstance(DefaultServiceProviderTest.class.getClassLoader(),
                        new Class<?>[] { JsonbBuilder.class }, (proxy, method, args) -> {
                            final Object result = method.invoke(builder, args);
                            return result == builder ? proxy : result;
                        });
    }
}
