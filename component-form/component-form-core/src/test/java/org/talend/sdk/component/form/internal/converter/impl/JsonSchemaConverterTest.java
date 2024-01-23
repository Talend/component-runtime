/**
 * Copyright (C) 2006-2024 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.form.internal.converter.impl;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.form.api.Client;
import org.talend.sdk.component.form.api.UiSpecService;
import org.talend.sdk.component.form.model.Ui;
import org.talend.sdk.component.form.model.jsonschema.JsonSchema;
import org.talend.sdk.component.server.front.model.ConfigTypeNodes;

import lombok.Data;

class JsonSchemaConverterTest {

    @Test
    void ensurePrimitiveSerialization() throws Exception {
        try (final Jsonb jsonb = JsonbBuilder.create()) {
            final JsonSchema schema = new JsonSchema();

            schema.setDefaultValue(5);
            assertEquals("{\"default\":5,\"type\":\"object\"}", jsonb.toJson(schema));

            schema.setDefaultValue("yes");
            assertEquals("{\"default\":\"yes\",\"type\":\"object\"}", jsonb.toJson(schema));

            schema.setDefaultValue(asList("a", "b"));
            assertEquals("{\"default\":[\"a\",\"b\"],\"type\":\"object\"}", jsonb.toJson(schema));

            final Model model = new Model();
            model.id = "1";
            schema.setDefaultValue(model);
            assertEquals("{\"default\":{\"id\":\"1\"},\"type\":\"object\"}", jsonb.toJson(schema));

            schema.setDefaultValue(singletonList(model));
            assertEquals("{\"default\":[{\"id\":\"1\"}],\"type\":\"object\"}", jsonb.toJson(schema));
        }
    }

    @Test
    void enumValues() throws Exception {
        try (final Jsonb jsonb = JsonbBuilder.create();
                final InputStream stream =
                        Thread.currentThread().getContextClassLoader().getResourceAsStream("config.json")) {
            final ConfigTypeNodes nodes = jsonb.fromJson(stream, ConfigTypeNodes.class);
            final Ui payload = new UiSpecService<>(null, jsonb)
                    .convert("test", "en", nodes.getNodes().get("U2VydmljZU5vdyNkYXRhc2V0I3RhYmxl"), null)
                    .toCompletableFuture()
                    .get();
            final JsonSchema jsonSchema = payload.getJsonSchema();
            final JsonSchema schema = jsonSchema
                    .getProperties()
                    .get("tableDataSet")
                    .getProperties()
                    .get("commonConfig")
                    .getProperties()
                    .get("tableName");
            assertEquals(5, schema.getEnumValues().size());
        }
    }

    @Test
    void dynamicValues() throws Exception {
        try (final Jsonb jsonb = JsonbBuilder.create();
                final InputStream stream =
                        Thread.currentThread().getContextClassLoader().getResourceAsStream("config.json")) {
            final ConfigTypeNodes nodes = jsonb.fromJson(stream, ConfigTypeNodes.class);
            final Ui payload = new UiSpecService<>(new Client<Object>() {

                @Override
                public CompletionStage<Map<String, Object>> action(final String family, final String type,
                        final String action, final String lang, final Map<String, Object> params,
                        final Object ignored) {
                    if ("test".equals(family) && "dynamic_values".equals(type) && "GetTableFields".equals(action)) {
                        final Map<String, String> item = new HashMap<>();
                        item.put("id", "some");
                        item.put("label", "Some");
                        return CompletableFuture.completedFuture(singletonMap("items", singleton(item)));
                    }
                    return CompletableFuture.completedFuture(emptyMap());
                }

                @Override
                public void close() {
                    // no-op
                }
            }, jsonb)
                    .convert("test", "en", nodes.getNodes().get("U2VydmljZU5vdyNkYXRhc2V0I3RhYmxl"), null)
                    .toCompletableFuture()
                    .get();
            final JsonSchema jsonSchema = payload.getJsonSchema();
            final JsonSchema schema = jsonSchema
                    .getProperties()
                    .get("tableDataSet")
                    .getProperties()
                    .get("commonConfig")
                    .getProperties()
                    .get("fields")
                    .getItems();
            assertEquals(1, schema.getEnumValues().size());
            assertEquals("some", schema.getEnumValues().iterator().next());
        }
    }

    @Test
    void booleanDefaultValue() throws Exception {
        assertEquals(true, convert("boolean", "true"));
    }

    @Test
    void numberDefaultValue() throws Exception {
        assertEquals(1.1, convert("number", "1.1"));
    }

    @Test
    void stringDefaultValue() throws Exception {
        assertEquals("test", convert("string", "test"));
    }

    @Test
    void emptyArrayDefaultValue() throws Exception {
        final Object converted = convert("array", "[]");
        assertTrue(Object[].class.isInstance(converted));
        assertEquals(0, Object[].class.cast(converted).length);
    }

    @Test
    void primitiveArrayDefaultValue() throws Exception {
        final Object converted = convert("array", "[\"test\"]");
        assertTrue(Object[].class.isInstance(converted));
        final Object[] array = Object[].class.cast(converted);
        assertEquals(1, array.length);
        assertEquals("test", array[0]);
    }

    @Test
    void objectArrayDefaultValue() throws Exception {
        final Object converted = convert("array", "[{\"id\":1}]");
        assertTrue(Object[].class.isInstance(converted));
        final Object[] array = Object[].class.cast(converted);
        assertEquals(1, array.length);
        assertEquals(1, BigDecimal.class.cast(Map.class.cast(array[0]).get("id")).intValue());
    }

    @Test
    void objectDefaultValue() throws Exception {
        final Object converted = convert("object", "{\"id\":1}");
        assertEquals(1, Map.class.cast(converted).get("id"));
    }

    private Object convert(final String type, final String value) throws Exception {
        try (final Jsonb jsonb = JsonbBuilder.create()) {
            final JsonSchemaConverter instance = new JsonSchemaConverter(jsonb, new JsonSchema(), emptyList());
            final Method convertDefaultValue =
                    JsonSchemaConverter.class.getDeclaredMethod("convertDefaultValue", String.class, String.class);
            if (!convertDefaultValue.isAccessible()) {
                convertDefaultValue.setAccessible(true);
            }
            return Optional.class.cast(convertDefaultValue.invoke(instance, type, value)).orElse(null);
        }
    }

    @Data
    public static class Model {

        private String id;
    }
}
