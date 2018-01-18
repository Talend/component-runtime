/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.form.model.jsonschema.JsonSchema;

import lombok.Data;

class JsonSchemaConverterTest {

    @Test
    void ensurePrimitiveSerialization() throws Exception {
        try (final Jsonb jsonb = JsonbBuilder.create()) {
            final JsonSchema schema = new JsonSchema();

            schema.setDefaultValue(5);
            assertEquals("{\"default\":5}", jsonb.toJson(schema));

            schema.setDefaultValue("yes");
            assertEquals("{\"default\":\"yes\"}", jsonb.toJson(schema));

            /*
             * see org.talend.sdk.component.form.internal.converter.impl.JsonSchemaConverter.convertDefaultValue
             * schema.setDefaultValue(asList("a", "b"));
             * assertNull(jsonb.toJson(schema));
             */

            final Model model = new Model();
            model.id = "1";
            schema.setDefaultValue(model);
            assertEquals("{\"default\":{\"id\":\"1\"}}", jsonb.toJson(schema));
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
    @Disabled("see org.talend.sdk.component.form.internal.converter.impl.JsonSchemaConverter.convertDefaultValue")
    void emptyArrayDefaultValue() throws Exception {
        final Object converted = convert("array", "[]");
        assertTrue(Object[].class.isInstance(converted));
        assertEquals(0, Object[].class.cast(converted).length);
    }

    @Test
    @Disabled("see org.talend.sdk.component.form.internal.converter.impl.JsonSchemaConverter.convertDefaultValue")
    void primitiveArrayDefaultValue() throws Exception {
        final Object converted = convert("array", "[\"test\"]");
        assertTrue(Object[].class.isInstance(converted));
        final Object[] array = Object[].class.cast(converted);
        assertEquals(1, array.length);
        assertEquals("test", array[0]);
    }

    @Test
    @Disabled("see org.talend.sdk.component.form.internal.converter.impl.JsonSchemaConverter.convertDefaultValue")
    void objectArrayDefaultValue() throws Exception {
        final Object converted = convert("array", "[{\"id\":1}]");
        assertTrue(Object[].class.isInstance(converted));
        final Object[] array = Object[].class.cast(converted);
        assertEquals(1, array.length);
        assertEquals(1, Map.class.cast(array[0]).get("id"));
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
