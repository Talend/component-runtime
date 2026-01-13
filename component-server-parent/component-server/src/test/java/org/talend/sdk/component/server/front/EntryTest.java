/**
 * Copyright (C) 2006-2025 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.server.front;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.record.SchemaProperty;
import org.talend.sdk.component.api.record.SchemaProperty.LogicalType;
import org.talend.sdk.component.runtime.record.RecordBuilderFactoryImpl;
import org.talend.sdk.component.server.front.model.Entry;
import org.talend.sdk.component.server.front.model.Schema;

/**
 * Unit tests for {@link Entry}.
 */
class EntryTest {

    private Entry createValidEntry() {
        Map<String, String> props = new LinkedHashMap<>(0);
        props.put("p1", "v1");
        return new Entry("name", "raw", Schema.Type.STRING, true, false, true,
                true, null, "comment", props, "default");
    }

    // ----------------------------------------------------------------------
    // Builder
    // ----------------------------------------------------------------------

    @Test
    void builderCreatesValidEntry() {
        Entry entry = createValidEntry();

        assertEquals("name", entry.getName());
        assertEquals("raw", entry.getRawName());
        assertEquals("raw", entry.getOriginalFieldName());
        assertEquals(Schema.Type.STRING, entry.getType());
        assertTrue(entry.isNullable());
        assertFalse(entry.isMetadata());
        assertTrue(entry.isErrorCapable());
        assertTrue(entry.isValid());
        assertEquals("comment", entry.getComment());
        assertEquals("default", entry.getDefaultValue());
        assertEquals("v1", entry.getProps().get("p1"));
    }

    // ----------------------------------------------------------------------
    // Accessors
    // ----------------------------------------------------------------------

    @Test
    void getDefaultValueIsTyped() {
        Entry entry = createValidEntry();

        String value = entry.getDefaultValue();
        assertEquals("default", value);
    }

    @Test
    void getPropReturnsProperty() {
        Entry entry = createValidEntry();
        assertEquals("v1", entry.getProp("p1"));
        assertNull(entry.getProp("k1"));
    }

    // ----------------------------------------------------------------------
    // JSON deserialization
    // ----------------------------------------------------------------------

    @Test
    void deserializeEntryFromJson() throws Exception {
        RecordBuilderFactoryImpl factory = new RecordBuilderFactoryImpl("test");
        org.talend.sdk.component.api.record.Schema.Entry entryImpl = factory.newEntryBuilder()
                .withName("éèfield")
                .withLogicalType(LogicalType.UUID)
                .withNullable(false)
                .withMetadata(false)
                .withErrorCapable(false)
                .withComment("test comment")
                .withProps(Map.of("p1", "v1"))
                .withDefaultValue("defaultValue")
                .build();

        try (Jsonb jsonb = JsonbBuilder.create()) {
            String json = jsonb.toJson(entryImpl);

            ObjectMapper mapper = new ObjectMapper();
            Entry entry = mapper.readValue(json, Entry.class);

            assertEquals("_field", entry.getName());
            assertEquals("éèfield", entry.getRawName());
            assertEquals("éèfield", entry.getOriginalFieldName());
            assertEquals(Schema.Type.STRING, entry.getType());
            assertEquals(LogicalType.UUID.key(), entry.getProp(SchemaProperty.LOGICAL_TYPE));

            assertFalse(entry.isNullable());
            assertFalse(entry.isMetadata());
            assertFalse(entry.isErrorCapable());
            assertTrue(entry.isValid());

            assertEquals("test comment", entry.getComment());
            assertEquals("v1", entry.getProps().get("p1"));
            assertEquals("defaultValue", entry.getDefaultValue());
        }
    }

}