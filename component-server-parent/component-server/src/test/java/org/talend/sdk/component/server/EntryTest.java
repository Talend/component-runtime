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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;

import java.util.Map;

import org.junit.jupiter.api.Test;
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
         true, null,"comment", props, "default");
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

