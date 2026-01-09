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
package org.talend.sdk.component.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.server.front.model.Entry;
import org.talend.sdk.component.server.front.model.Schema;

/**
 * Unit tests for {@link Entry}.
 */
class EntryTest {

     private Entry createValidEntry() {
        return Entry.builder()
                .name("name")
                .rawName("raw")
                .originalFieldName("original")
                .type(Schema.Type.STRING)
                .isNullable(true)
                .isMetadata(false)
                .isErrorCapable(true)
                .isValid(true)
                .comment("comment")
                .putProps("p1", "v1")
                .internalDefaultValue("default")
                .build();
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
        assertEquals("default", entry.getInternalDefaultValue());
        assertEquals("v1", entry.getProps().get("p1"));
    }

    // ----------------------------------------------------------------------
    // withXxx methods
    // ----------------------------------------------------------------------

    @Test
    void withNameSameValueReturnsSameInstance() {
        Entry entry = createValidEntry();
        assertSame(entry, entry.withName("name"));
    }

    @Test
    void withNameDifferentValueReturnsNewInstance() {
        Entry entry = createValidEntry();
        Entry modified = entry.withName("other");

        assertNotSame(entry, modified);
        assertEquals("other", modified.getName());
        assertEquals(entry.getRawName(), modified.getRawName());
    }

    @Test
    void withIsNullableChangesValue() {
        Entry entry = createValidEntry();
        Entry modified = entry.withIsNullable(false);

        assertFalse(modified.isNullable());
        assertTrue(entry.isNullable());
    }

    @Test
    void withPropsReplacesMap() {
        Entry entry = createValidEntry();

        Map<String, String> newProps = new HashMap<>();
        newProps.put("a", "b");

        Entry modified = entry.withProps(newProps);

        assertEquals(Map.of("a", "b"), modified.getProps());
        assertEquals(Map.of("p1", "v1"), entry.getProps());
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
    // copyOf
    // ----------------------------------------------------------------------

    @Test
    void copyOfReturnsSameInstanceForEntry() {
        Entry entry = createValidEntry();
        assertSame(entry, Entry.copyOf(entry));
    }

    // ----------------------------------------------------------------------
    // JSON deserialization
    // ----------------------------------------------------------------------

    @Test
    void deserializeEntryFromJson() throws Exception {
        String json = """
        {
          "name": "field",
          "rawName": "field_raw",
          "type": "STRING",
          "nullable": true,
          "metadata": false,
          "errorCapable": true,
          "valid": true,
          "elementSchema": {
            "type": "STRING"
          },
          "comment": "test comment",
          "props": {
            "p1": "v1"
          },
          "internalDefaultValue": "defaultValue"
        }
        """;

        ObjectMapper mapper = new ObjectMapper();
        Entry entry = mapper.readValue(json, Entry.class);

        assertEquals("field", entry.getName());
        assertEquals("field_raw", entry.getRawName());
        assertEquals("field_raw", entry.getOriginalFieldName());
        assertEquals(Schema.Type.STRING, entry.getType());

        assertTrue(entry.isNullable());
        assertFalse(entry.isMetadata());
        assertTrue(entry.isErrorCapable());
        assertTrue(entry.isValid());

        assertNotNull(entry.getElementSchema());
        assertEquals(Schema.Type.STRING, entry.getElementSchema().getType());

        assertEquals("test comment", entry.getComment());
        assertEquals("v1", entry.getProps().get("p1"));
        assertEquals("defaultValue", entry.getInternalDefaultValue());
    }
}

