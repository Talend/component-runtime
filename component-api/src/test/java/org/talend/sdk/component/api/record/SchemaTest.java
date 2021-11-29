/**
 * Copyright (C) 2006-2021 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.api.record;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.record.Schema.Entry;
import org.talend.sdk.component.api.record.Schema.Type;

import lombok.RequiredArgsConstructor;

class SchemaTest {

    @Test
    void testGetEntry() {
        final Schema sc1 = new SchemaExample(null, Collections.emptyMap());
        Assertions.assertNull(sc1.getEntry("unknown"));
        final Entry e1 = new EntryExample("e1", Type.STRING);
        final Entry e2 = new EntryExample("e2", Type.STRING);
        final Schema sc2 = new SchemaExample(Arrays.asList(e1, e2), Collections.emptyMap());
        Assertions.assertNull(sc2.getEntry("unknown"));
        Assertions.assertSame(e1, sc2.getEntry("e1"));
        Assertions.assertSame(e2, sc2.getEntry("e2"));
    }

    @Test
    void testJsonProp() {
        final Map<String, String> testMap = new HashMap<>(3);
        testMap.put("key1", "value1");
        testMap.put("key2", Json.createObjectBuilder().add("Hello", 5).build().toString());
        testMap.put("key3", Json.createArrayBuilder().add(1).add(2).build().toString());

        final Schema sc1 = new SchemaExample(null, testMap);
        Assertions.assertNull(sc1.getJsonProp("unexist"));

        final JsonValue value1 = sc1.getJsonProp("key1");
        Assertions.assertEquals(ValueType.STRING, value1.getValueType());
        Assertions.assertEquals("value1", ((JsonString) value1).getString());

        final JsonValue value2 = sc1.getJsonProp("key2");
        Assertions.assertEquals(ValueType.OBJECT, value2.getValueType());
        Assertions.assertEquals(5, value2.asJsonObject().getJsonNumber("Hello").intValue());

        final JsonValue value3 = sc1.getJsonProp("key3");
        Assertions.assertEquals(ValueType.ARRAY, value3.getValueType());
        Assertions.assertEquals(1, value3.asJsonArray().getJsonNumber(0).intValue());
        Assertions.assertEquals(2, value3.asJsonArray().getJsonNumber(1).intValue());
    }

    @RequiredArgsConstructor
    class SchemaExample implements Schema {

        private final List<Entry> entries;

        private final Map<String, String> props;

        @Override
        public Type getType() {
            return Type.RECORD;
        }

        @Override
        public Schema getElementSchema() {
            return null;
        }

        @Override
        public List<Entry> getEntries() {
            return entries;
        }

        @Override
        public List<Entry> getMetadata() {
            return Collections.emptyList();
        }

        @Override
        public Stream<Entry> getAllEntries() {
            return Optional.ofNullable(this.entries).map(List::stream).orElse(Stream.empty());
        }

        @Override
        public Builder toBuilder() {
            throw new UnsupportedOperationException("#toBuilder()");
        }

        @Override
        public EntriesOrder naturalOrder() {
            throw new UnsupportedOperationException("#naturalOrder()");
        }

        @Override
        public Map<String, String> getProps() {
            return props;
        }

        @Override
        public String getProp(final String property) {
            return props.get(property);
        }
    }

    @RequiredArgsConstructor
    class EntryExample implements Entry {

        private final String name;

        private final Type type;

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getRawName() {
            throw new UnsupportedOperationException("#getRawName()");
        }

        @Override
        public String getOriginalFieldName() {
            throw new UnsupportedOperationException("#getOriginalFieldName()");
        }

        @Override
        public Type getType() {
            throw new UnsupportedOperationException("#getType()");
        }

        @Override
        public boolean isNullable() {
            throw new UnsupportedOperationException("#isNullable()");
        }

        @Override
        public boolean isMetadata() {
            throw new UnsupportedOperationException("#isMetadata()");
        }

        @Override
        public <T> T getDefaultValue() {
            throw new UnsupportedOperationException("#getDefaultValue()");
        }

        @Override
        public Schema getElementSchema() {
            throw new UnsupportedOperationException("#getElementSchema()");
        }

        @Override
        public String getComment() {
            throw new UnsupportedOperationException("#getComment()");
        }

        @Override
        public Map<String, String> getProps() {
            throw new UnsupportedOperationException("#getProps()");
        }

        @Override
        public String getProp(final String property) {
            throw new UnsupportedOperationException("#getProp()");
        }

        @Override
        public JsonValue getJsonProp(final String name) {
            return Entry.super.getJsonProp(name);
        }

        @Override
        public Builder toBuilder() {
            throw new UnsupportedOperationException("#toBuilder()");
        }
    }

    @Test
    void testTypes() {
        final Record rec = new Record() {

            @Override
            public Schema getSchema() {
                return null;
            }

            @Override
            public Builder withNewSchema(final Schema schema) {
                throw new UnsupportedOperationException("#withNewSchema()");
            }

            @Override
            public <T> T get(final Class<T> expectedType, final String name) {
                return null;
            }
        };
        Assertions.assertTrue(Type.RECORD.isCompatible(rec));
        Assertions.assertFalse(Type.RECORD.isCompatible(1234));

        Assertions.assertTrue(Type.ARRAY.isCompatible(Collections.emptyList()));
        Assertions.assertFalse(Schema.Type.ARRAY.isCompatible(new int[] {}));

        Assertions.assertTrue(Type.STRING.isCompatible("Hello"));
        Assertions.assertFalse(Schema.Type.STRING.isCompatible(new int[] {}));

        Assertions.assertTrue(Type.BYTES.isCompatible("Hello".getBytes()));
        Assertions.assertTrue(Type.BYTES.isCompatible(new Byte[] {}));
        Assertions.assertFalse(Schema.Type.BYTES.isCompatible(new int[] {}));

        Assertions.assertTrue(Schema.Type.INT.isCompatible(null));
        Assertions.assertTrue(Schema.Type.INT.isCompatible(123));
        Assertions.assertFalse(Schema.Type.INT.isCompatible(234L));
        Assertions.assertFalse(Schema.Type.INT.isCompatible("Hello"));

        Assertions.assertTrue(Schema.Type.LONG.isCompatible(123L));
        Assertions.assertFalse(Schema.Type.LONG.isCompatible(234));

        Assertions.assertTrue(Schema.Type.FLOAT.isCompatible(123.2f));
        Assertions.assertFalse(Schema.Type.FLOAT.isCompatible(234.1d));

        Assertions.assertTrue(Schema.Type.DOUBLE.isCompatible(123.2d));
        Assertions.assertFalse(Schema.Type.DOUBLE.isCompatible(Float.valueOf(3.4f)));

        Assertions.assertTrue(Schema.Type.BOOLEAN.isCompatible(Boolean.TRUE));
        Assertions.assertFalse(Schema.Type.BOOLEAN.isCompatible(10));

        Assertions.assertTrue(Type.DATETIME.isCompatible(System.currentTimeMillis()));
        Assertions.assertTrue(Type.DATETIME.isCompatible(new Date()));
        Assertions.assertTrue(Type.DATETIME.isCompatible(ZonedDateTime.now()));
        Assertions.assertFalse(Schema.Type.DATETIME.isCompatible(10));
    }

}
