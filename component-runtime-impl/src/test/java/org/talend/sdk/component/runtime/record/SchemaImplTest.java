/**
 * Copyright (C) 2006-2022 Talend Inc. - www.talend.com
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

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.json.Json;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.api.record.Schema.Builder;
import org.talend.sdk.component.api.record.Schema.EntriesOrder;
import org.talend.sdk.component.api.record.Schema.Entry;
import org.talend.sdk.component.api.record.Schema.Type;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;
import org.talend.sdk.component.runtime.record.SchemaImpl.BuilderImpl;
import org.talend.sdk.component.runtime.record.SchemaImpl.EntryImpl;

class SchemaImplTest {

    private final Schema.Entry data1 = new SchemaImpl.EntryImpl.BuilderImpl() //
            .withName("data1") //
            .withType(Schema.Type.INT) //
            .build();

    private final Schema.Entry data2 = new SchemaImpl.EntryImpl.BuilderImpl() //
            .withName("data2") //
            .withType(Schema.Type.STRING) //
            .withNullable(true) //
            .build();

    private final Schema.Entry meta1 = new SchemaImpl.EntryImpl.BuilderImpl() //
            .withName("meta1") //
            .withType(Schema.Type.INT) //
            .withMetadata(true) //
            .build();

    private final Schema.Entry meta2 = new SchemaImpl.EntryImpl.BuilderImpl() //
            .withName("meta2") //
            .withType(Schema.Type.STRING) //
            .withMetadata(true) //
            .withNullable(true) //
            .build();

    @Test
    void checkEquals() {
        final RecordBuilderFactory f = new RecordBuilderFactoryImpl("test");
        final Entry first = f.newEntryBuilder().withName("First").withType(Type.STRING).build();
        final Entry second = f.newEntryBuilder().withName("Second").withType(Type.STRING).build();
        EqualsVerifier.simple()
                .suppress(Warning.STRICT_HASHCODE) // Supress test hashcode use all fields used by equals (for legacy)
                .forClass(SchemaImpl.class)
                .withPrefabValues(Schema.Entry.class, first, second)
                .withPrefabValues(EntriesOrder.class, EntriesOrder.of("First"), EntriesOrder.of("Second"))
                .verify();
    }

    @Test
    void testEntries() {
        Assertions.assertFalse(data1.isMetadata(), "meta data should be false by default");
        Assertions.assertTrue(meta1.isMetadata(), "meta data should be true here");
    }

    @Test
    void getAllEntries() {
        final Schema schema = new BuilderImpl() //
                .withType(Type.RECORD) //
                .withEntry(data1) //
                .withEntry(meta1) //
                .withEntry(data2) //
                .withEntry(meta2) //
                .build();
        final List<Entry> entries = schema.getEntries();
        Assertions.assertEquals(2, entries.size());
        Assertions.assertTrue(entries.contains(this.data1));
        Assertions.assertTrue(entries.contains(this.data2));

        Assertions.assertEquals(4, schema.getAllEntries().count());
        final List<Entry> metaEntries = schema.getAllEntries().filter(Entry::isMetadata).collect(Collectors.toList());
        Assertions.assertEquals(2, metaEntries.size());
        Assertions.assertTrue(metaEntries.contains(this.meta1));
        Assertions.assertTrue(metaEntries.contains(this.meta2));
    }

    @Test
    void testEquals() {
        final Schema schema = new BuilderImpl() //
                .withType(Type.RECORD) //
                .withEntry(data1) //
                .withEntry(meta1) //
                .withEntry(meta2) //
                .build();

        final Schema schema1 = new BuilderImpl() //
                .withType(Type.RECORD) //
                .withEntry(data1) //
                .withEntry(meta1) //
                .withEntry(meta2) //
                .build();

        final Schema schemaDiff = new BuilderImpl() //
                .withType(Type.RECORD) //
                .withEntry(meta2) //
                .withEntry(meta1) //
                .build();
        Assertions.assertEquals(schema, schema1);
        Assertions.assertNotEquals(schema, schemaDiff);
    }

    @Test
    void testRecordWithMetadataFields() {
        final Schema schema = new BuilderImpl() //
                .withType(Type.RECORD) //
                .withEntry(new SchemaImpl.EntryImpl.BuilderImpl()
                        .withName("field1")
                        .withType(Type.STRING)
                        .withNullable(true)
                        .withRawName("field1")
                        .withDefaultValue(5)
                        .withComment("Comment")
                        .build())
                .withEntry(new SchemaImpl.EntryImpl.BuilderImpl().withName("record_id")
                        .withType(Type.INT)
                        .withMetadata(true)
                        .withProp("method", "FIFO")
                        .build())
                .withEntry(new SchemaImpl.EntryImpl.BuilderImpl().withName("field2")
                        .withType(Type.STRING)
                        .withMetadata(true)
                        .build())
                .build();
        final RecordImpl.BuilderImpl builder = new RecordImpl.BuilderImpl(schema);
        Record record = builder //
                .withInt("record_id", 34) //
                .withString("field1", "Aloa") //
                .withString("field2", "Hallo, wie gehst du ?") //
                .build();
        Schema recordSchema = record.getSchema();
        Assertions.assertEquals(1, recordSchema.getEntries().size());
        Assertions.assertEquals(2, recordSchema.getAllEntries().filter(e -> e.isMetadata()).count());
        Assertions.assertEquals(34, record.getInt("record_id"));
        Assertions.assertEquals("Aloa", record.getString("field1"));
        Assertions.assertEquals("Hallo, wie gehst du ?", record.getString("field2"));
    }

    @Test
    void testAntiCollision() {
        final Entry entry1 = this.newEntry("1name_b", "a_value");
        final Entry entry2 = this.newEntry("2name_b", "b_value");
        final Entry entry3 = this.newEntry("name_b", "c_value");

        final Schema schema = this.newSchema(entry1, entry2, entry3);

        final boolean checkNames = schema
                .getAllEntries()
                .allMatch((Entry e) -> ("1name_b".equals(e.getRawName()) && e.getName().matches("name_b_[12]")
                        && "a_value".equals(e.getDefaultValue())) //
                        || ("2name_b".equals(e.getRawName()) && e.getName().matches("name_b_[12]")
                                && "b_value".equals(e.getDefaultValue())) //
                        || (e.getRawName() == null && e.getName().equals("name_b")
                                && "c_value".equals(e.getDefaultValue())));
        Assertions.assertTrue(checkNames);
        Assertions.assertEquals(3, schema.getAllEntries().map(Entry::getName).distinct().count());

        final Entry entry3Bis = this.newEntry("name_b_1", "c_value");

        final Schema schemaBis = this.newSchema(entry1, entry2, entry3Bis);
        final boolean checkNamesBis = schemaBis
                .getAllEntries()
                .allMatch((Entry e) -> ("1name_b".equals(e.getRawName()) && e.getName().matches("name_b(_2)?")
                        && "a_value".equals(e.getDefaultValue())) //
                        || ("2name_b".equals(e.getRawName()) && e.getName().matches("name_b(_2)?")
                                && "b_value".equals(e.getDefaultValue())) //
                        || (e.getRawName() == null && e.getName().equals("name_b_1")
                                && "c_value".equals(e.getDefaultValue())));
        Assertions.assertTrue(checkNamesBis);
        Assertions.assertEquals(3, schemaBis.getAllEntries().map(Entry::getName).distinct().count());

        final Schema.Builder builder = new BuilderImpl().withType(Type.RECORD);
        for (int index = 1; index < 8; index++) {
            final Entry e = this.newEntry(index + "name_b", index + "_value");
            builder.withEntry(e);
        }
        final Entry last = this.newEntry("name_b_5", "last_value");
        builder.withEntry(last);
        final Schema schemaTer = builder.build();
        Assertions.assertEquals(8, schemaTer.getAllEntries().map(Entry::getName).distinct().count());
        Assertions
                .assertEquals(1,
                        schemaTer
                                .getAllEntries()
                                .map(Entry::getName)
                                .filter((String name) -> "name_b".equals(name))
                                .count());
        Assertions
                .assertEquals(7,
                        IntStream
                                .range(1, 8)
                                .mapToObj((int i) -> "name_b_" + i)
                                .flatMap((String name) -> schemaTer
                                        .getAllEntries()
                                        .filter((Entry e) -> Objects.equals(name, e.getName())))
                                .count());

        final Entry entry3Twin = new EntryImpl.BuilderImpl() //
                .withName("name_b") //
                .withType(Type.LONG) //
                .withDefaultValue(0L) //
                .build();
        Assertions.assertThrows(IllegalArgumentException.class, () -> this.newSchema(entry3, entry3Twin));
    }

    private Schema newSchema(Entry... entries) {
        final Schema.Builder builder = new BuilderImpl().withType(Type.RECORD);
        for (Entry e : entries) {
            builder.withEntry(e);
        }
        return builder.build();
    }

    private Entry newEntry(final String name, final String defaultValue) {
        return new EntryImpl.BuilderImpl() //
                .withName(name) //
                .withType(Type.STRING) //
                .withDefaultValue(defaultValue) //
                .build();
    }

    @Test
    void testOrder() {
        final Schema schema = new BuilderImpl() //
                .withType(Type.RECORD) //
                .withEntry(data1) //
                .withEntry(meta1) //
                .withEntry(data2) //
                .withEntry(meta2) //
                .moveAfter("meta1", "data1")
                .moveBefore("data1", "meta2")
                .build();
        assertEquals("meta1,meta2,data1,data2", getSchemaFields(schema));
        assertEquals("meta2,meta1,data1,data2", getSchemaFields(schema, EntriesOrder.of("meta2,meta1,data1,meta0")));
        assertEquals("meta1,meta2,data1,data2", getSchemaFields(schema));

        // test when move after last columns (TCOMP-2067)
        final Schema schemaLast = new BuilderImpl() //
                .withType(Type.RECORD) //
                .withEntry(data1) //
                .withEntry(meta2) //
                .withEntry(data2) //
                .withEntry(meta1) //
                .moveAfter("meta1", "meta2")
                .build();
        assertEquals("data1,data2,meta1,meta2", getSchemaFields(schemaLast));
    }

    @Test
    void testCustomComparatorEntriesOrder() {
        final Schema schema = new BuilderImpl() //
                .withType(Type.RECORD) //
                .withEntry(data1) //
                .withEntry(meta1) //
                .withEntry(data2) //
                .withEntry(meta2) //
                .build();
        assertEquals("data1,meta1,data2,meta2", getSchemaFields(schema));
        final Comparator<Entry> myComparator = (o1, o2) -> {
            if (o1.isMetadata() && o2.isMetadata()) {
                return 0;
            }
            if (o1.isMetadata()) {
                return -1;
            }
            if (o2.isMetadata()) {
                return 1;
            }
            return 0;
        };
        assertEquals("meta1,meta2,data1,data2", getSchemaFields(schema, myComparator));
    }

    @Test
    void testCustomEntriesOrder() {
        final Schema schema = new BuilderImpl() //
                .withType(Type.RECORD) //
                .withEntry(data1) //
                .withEntry(meta1) //
                .withEntry(data2) //
                .withEntry(meta2) //
                .build();
        assertEquals("data1,meta1,data2,meta2", getSchemaFields(schema));
        final EntriesOrder entriesOrder = EntriesOrder.of("meta1,meta2,data1,data2");
        assertEquals("meta1,meta2,data1,data2", getSchemaFields(schema, entriesOrder));
        entriesOrder.swap("meta1", "data2").moveBefore("meta2", "data1");
        assertEquals("data2,data1,meta2,meta1", getSchemaFields(schema, entriesOrder));
    }

    @Test
    void testBuilder() {
        final Schema schema = new BuilderImpl() //
                .withType(Type.RECORD) //
                .withEntry(data1) //
                .withEntryBefore("data1", meta1) //
                .withEntry(data2) //
                .withEntryAfter("meta1", meta2) //
                .build();
        assertEquals("meta1,meta2,data1,data2", getSchemaFields(schema));
        // failing
        final Schema.Builder builder = new BuilderImpl().withType(Type.RECORD);
        assertThrows(IllegalArgumentException.class, () -> builder.withEntryAfter("data1", meta1));
        assertThrows(IllegalArgumentException.class, () -> builder.withEntryBefore("data1", meta2));
    }

    @Test
    void testJsonPropForEntry() {
        final Map<String, String> testMap = new HashMap<>(3);
        testMap.put("key1", "value1");
        testMap.put("key2", Json.createObjectBuilder().add("Hello", 5).build().toString());
        testMap.put("key3", Json.createArrayBuilder().add(1).add(2).build().toString());

        final Entry entry = newEntry("key0", Type.STRING, testMap);
        Assertions.assertNull(entry.getJsonProp("unexist"));

        final JsonValue value1 = entry.getJsonProp("key1");
        Assertions.assertEquals(ValueType.STRING, value1.getValueType());
        Assertions.assertEquals("value1", ((JsonString) value1).getString());

        final JsonValue value2 = entry.getJsonProp("key2");
        Assertions.assertEquals(ValueType.OBJECT, value2.getValueType());
        Assertions.assertEquals(5, value2.asJsonObject().getJsonNumber("Hello").intValue());

        final JsonValue value3 = entry.getJsonProp("key3");
        Assertions.assertEquals(ValueType.ARRAY, value3.getValueType());
        Assertions.assertEquals(1, value3.asJsonArray().getJsonNumber(0).intValue());
        Assertions.assertEquals(2, value3.asJsonArray().getJsonNumber(1).intValue());
    }

    @Test
    void testToBuilder() {
        final Schema schemaOrigin = new BuilderImpl() //
                .withType(Type.RECORD) //
                .withEntry(data1) //
                .withEntry(meta1) //
                .withEntry(data2) //
                .withEntry(meta2) //
                .moveAfter("meta1", "data1")
                .moveBefore("data2", "meta2")
                .build();
        assertEquals("meta1,data1,meta2,data2", getSchemaFields(schemaOrigin));
        Builder builder = schemaOrigin.toBuilder();
        builder.withEntry(newEntry("data3", Type.STRING));
        builder.withEntry(new SchemaImpl.EntryImpl.BuilderImpl().withName("meta3")
                .withType(Type.STRING)
                .withMetadata(true)
                .build());
        final Schema schemaNew = builder.build();
        assertEquals(3, schemaNew.getMetadata().size());
        assertEquals(3, schemaNew.getEntries().size());
        assertEquals(6, schemaNew.getAllEntries().count());
        assertEquals("meta1,data1,meta2,data2,data3,meta3", getSchemaFields(schemaNew));
    }

    @Test
    void testAvoidCollision() {
        final Map<String, Schema.Entry> entries = new HashMap<>();
        for (int index = 1; index < 8; index++) {
            final Schema.Entry e = this.newEntry(index + "name_b", Type.STRING);
            final Schema.Entry realEntry = Schema.avoidCollision(e, entries::get, entries::put);
            entries.put(realEntry.getName(), realEntry);
        }
        final Entry last = this.newEntry("name_b_5", Type.STRING);
        final Schema.Entry realEntry = Schema.avoidCollision(last, entries::get, entries::put);
        entries.put(realEntry.getName(), realEntry);

        Assertions.assertEquals(8, entries.size());
        Assertions.assertEquals("name_b", entries.get("name_b").getName());
        Assertions
                .assertTrue(IntStream
                        .range(1, 8)
                        .mapToObj((int i) -> "name_b_" + i)
                        .allMatch((String name) -> entries.get(name).getName().equals(name)));

        final Map<String, Entry> entriesDuplicate = new HashMap<>();
        final Schema.Entry e1 = this.newEntry("goodName", Type.STRING);
        final Schema.Entry realEntry1 =
                Schema.avoidCollision(e1, entriesDuplicate::get, entriesDuplicate::put);
        Assertions.assertSame(e1, realEntry1);
        entriesDuplicate.put(realEntry1.getName(), realEntry1);
        final Schema.Entry e2 = this.newEntry("goodName", Type.STRING);
        final Schema.Entry realEntry2 =
                Schema.avoidCollision(e2, entriesDuplicate::get, entriesDuplicate::put);

        Assertions.assertSame(realEntry2, e2);
    }

    @RepeatedTest(20)
    void entriesOrderShouldBeDeterministic() {
        final List<Entry> entries = IntStream
                .range(0, 20)
                .mapToObj(i -> newEntry(String.format("data0%02d", i), Type.STRING))
                .collect(toList());
        entries.add(data1);
        entries.add(data2);
        entries.add(meta1);
        entries.add(meta2);
        Collections.shuffle(entries);
        final String shuffled = entries.stream()
                .map(e -> e.getName())
                .filter(s -> !s.matches("(data1|data2|meta1|meta2)"))
                .collect(joining(","));
        final Builder builder = new BuilderImpl().withType(Type.RECORD);
        entries.forEach(builder::withEntry);
        final Schema schema = builder.build();
        final String order = "meta1,meta2,data1,data2";
        final EntriesOrder entriesOrder = EntriesOrder.of(order);
        assertEquals(shuffled, getSchemaFields(schema, entriesOrder).replace(order + ",", ""));
        assertEquals(order, getSchemaFields(schema, entriesOrder).replaceAll(",data0.*", ""));
    }

    private String getSchemaFields(final Schema schema) {
        return schema.getEntriesOrdered().stream().map(e -> e.getName()).collect(joining(","));
    }

    private String getSchemaFields(final Schema schema, final EntriesOrder entriesOrder) {
        return schema.getEntriesOrdered(entriesOrder).stream().map(e -> e.getName()).collect(joining(","));
    }

    private String getSchemaFields(final Schema schema, final Comparator<Entry> entriesOrder) {
        return schema.getEntriesOrdered(entriesOrder).stream().map(e -> e.getName()).collect(joining(","));
    }

    private Schema.Entry newEntry(final String name, final Schema.Type type) {
        return new SchemaImpl.EntryImpl.BuilderImpl().withName(name).withType(type).build();
    }

    private Schema.Entry newEntry(final String name, final Schema.Type type, final Map<String, String> props) {
        return new SchemaImpl.EntryImpl.BuilderImpl().withName(name).withType(type).withProps(props).build();
    }

}