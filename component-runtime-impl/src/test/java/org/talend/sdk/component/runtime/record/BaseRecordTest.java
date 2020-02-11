/*
 * Copyright (C) 2006-2020 Talend Inc. - www.talend.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 */
package org.talend.sdk.component.runtime.record;

import static java.util.Collections.emptyMap;

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
import org.talend.sdk.component.runtime.record.json.PojoJsonbProvider;
import org.talend.sdk.component.runtime.record.json.RecordJsonGenerator;

public class BaseRecordTest {
    protected final RecordConverters converter = new RecordConverters();

    protected final JsonProvider jsonProvider = JsonProvider.provider();

    protected final JsonBuilderFactory jsonBuilderFactory = Json.createBuilderFactory(emptyMap());

    protected final RecordBuilderFactoryImpl recordBuilderFactory = new RecordBuilderFactoryImpl("test");


    protected Jsonb getJsonb(Jsonb jsonb) {
        // create a Jsonb instance which is PojoJsonbProvider as in component-runtime-manager
        return Jsonb.class
                .cast(Proxy
                        .newProxyInstance(Thread.currentThread().getContextClassLoader(),
                                new Class<?>[]{ Jsonb.class, PojoJsonbProvider.class }, (proxy, method, args) -> {
                                    if (method.getDeclaringClass() == Supplier.class) {
                                        return jsonb;
                                    }
                                    return method.invoke(jsonb, args);
                                }));
    }

    protected Jsonb createPojoJsonb() {
        final JsonbBuilder jsonbBuilder = JsonbBuilder.newBuilder().withProvider(new JsonProviderImpl() {

            @Override
            public JsonGeneratorFactory createGeneratorFactory(final Map<String, ?> config) {
                return new RecordJsonGenerator.Factory(() -> new RecordBuilderFactoryImpl("test"),
                        ()-> getJsonb(createPojoJsonb()), config);
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
}
