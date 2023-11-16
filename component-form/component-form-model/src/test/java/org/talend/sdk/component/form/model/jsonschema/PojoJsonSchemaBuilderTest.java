/**
 * Copyright (C) 2006-2023 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.form.model.jsonschema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.talend.sdk.component.form.model.jsonschema.JsonSchema.jsonSchemaFrom;

import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

class PojoJsonSchemaBuilderTest {

    @Test
    void flat() {
        final JsonSchema schema = jsonSchemaFrom(Form1.class).build();
        final Map<String, JsonSchema> properties = schema.getProperties();
        assertForm1(properties);
    }

    @Test
    void nested() {
        final JsonSchema schema = jsonSchemaFrom(Form2.class).build();
        final Map<String, JsonSchema> properties = schema.getProperties();
        assertEquals(2, properties.size());
        Stream.of("name", "form1").forEach(k -> assertTrue(properties.containsKey(k)));
        assertEquals("string", properties.get("name").getType());
        assertEquals("object", properties.get("form1").getType());
        assertForm1(properties.get("form1").getProperties());
    }

    private void assertForm1(final Map<String, JsonSchema> properties) {
        assertEquals(2, properties.size());
        Stream.of("name", "age").forEach(k -> assertTrue(properties.containsKey(k)));
        assertEquals("string", properties.get("name").getType());
        assertEquals("number", properties.get("age").getType());
    }

    public static class Form1 {

        private String name;

        private int age;
    }

    public static class Form2 {

        private String name;

        private Form1 form1;
    }
}
