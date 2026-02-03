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

import static java.util.Collections.emptyMap;
import static java.util.Optional.ofNullable;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.function.Supplier;

import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.spi.JsonProvider;
import javax.json.stream.JsonGeneratorFactory;

import org.apache.johnzon.core.JsonProviderImpl;
import org.apache.johnzon.mapper.MapperBuilder;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;
import org.talend.sdk.component.runtime.record.json.PojoJsonbProvider;
import org.talend.sdk.component.runtime.record.json.RecordJsonGenerator;

public class PluralRecordExtension implements ParameterResolver, AfterEachCallback {

    private final RecordConverters converter = new RecordConverters();

    private final JsonProvider jsonProvider = JsonProvider.provider();

    private final JsonBuilderFactory jsonBuilderFactory = Json.createBuilderFactory(emptyMap());

    private final RecordBuilderFactoryImpl recordBuilderFactory = new RecordBuilderFactoryImpl("test");

    final Namespace GLOBAL_NAMESPACE = Namespace.create(getClass());

    private Jsonb getJsonb(Jsonb jsonb) {
        // create a Jsonb instance which is PojoJsonbProvider as in component-runtime-manager
        return Jsonb.class
                .cast(Proxy
                        .newProxyInstance(Thread.currentThread().getContextClassLoader(),
                                new Class<?>[] { Jsonb.class, PojoJsonbProvider.class }, (proxy, method, args) -> {
                                    if (method.getDeclaringClass() == Supplier.class) {
                                        return jsonb;
                                    }
                                    return method.invoke(jsonb, args);
                                }));
    }

    private Jsonb createPojoJsonb() {
        final JsonbBuilder jsonbBuilder = JsonbBuilder.newBuilder().withProvider(new JsonProviderImpl() {

            @Override
            public JsonGeneratorFactory createGeneratorFactory(final Map<String, ?> config) {
                return new RecordJsonGenerator.Factory(() -> new RecordBuilderFactoryImpl("test"),
                        () -> getJsonb(createPojoJsonb()), config);
            }
        });
        try { // to passthrough the writer, otherwise RecoderJsonGenerator is broken
            final Field mapper = jsonbBuilder.getClass().getDeclaredField("builder");
            if (!mapper.isAccessible()) {
                mapper.setAccessible(true);
            }
            MapperBuilder.class.cast(mapper.get(jsonbBuilder)).setDoCloseOnStreams(true);
        } catch (final Exception e) {
            throw new IllegalStateException(e);
        }
        return jsonbBuilder.build();
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        final Class<?> type = parameterContext.getParameter().getType();
        Boolean result = Jsonb.class == type || RecordConverters.class == type || JsonProvider.class == type
                || JsonBuilderFactory.class == type || RecordBuilderFactory.class == type;
        return result;
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        final Class<?> type = parameterContext.getParameter().getType();
        if (type == Jsonb.class) {
            return extensionContext
                    .getStore(GLOBAL_NAMESPACE)
                    .getOrComputeIfAbsent(Jsonb.class, k -> getJsonb(createPojoJsonb()));
        }
        if (type == RecordConverters.class) {
            return extensionContext
                    .getStore(GLOBAL_NAMESPACE)
                    .getOrComputeIfAbsent(RecordConverters.class, k -> converter);
        }
        if (type == JsonProvider.class) {
            return extensionContext
                    .getStore(GLOBAL_NAMESPACE)
                    .getOrComputeIfAbsent(JsonProvider.class, k -> jsonProvider);
        }
        if (type == JsonBuilderFactory.class) {
            return extensionContext
                    .getStore(GLOBAL_NAMESPACE)
                    .getOrComputeIfAbsent(JsonBuilderFactory.class, k -> jsonBuilderFactory);
        }
        if (type == RecordBuilderFactory.class) {
            return extensionContext
                    .getStore(GLOBAL_NAMESPACE)
                    .getOrComputeIfAbsent(RecordBuilderFactory.class, k -> recordBuilderFactory);
        }
        throw new ParameterResolutionException(
                String.format("Parameter for class %s not managed.", type.getCanonicalName()));
    }

    @Override
    public void afterEach(ExtensionContext extensionContext) throws Exception {
        ofNullable(extensionContext.getStore(GLOBAL_NAMESPACE).remove(Jsonb.class, Jsonb.class)).ifPresent(jsonb -> {
            try {
                jsonb.close();
            } catch (Exception e) {
            }
        });
    }
}
