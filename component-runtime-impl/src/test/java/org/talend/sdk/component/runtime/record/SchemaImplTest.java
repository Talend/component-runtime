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
package org.talend.sdk.component.runtime.record;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.api.record.Schema.Entry;
import org.talend.sdk.component.api.record.Schema.Type;
import org.talend.sdk.component.runtime.record.SchemaImpl.BuilderImpl;

class SchemaImplTest {

    private final Schema.Entry dataEntry1 = new Schema.Entry.Builder() //
            .withName("data1") //
            .withType(Schema.Type.INT) //
            .build();

    private final Schema.Entry dataEntry2 = new Schema.Entry.Builder() //
            .withName("data2") //
            .withType(Schema.Type.STRING) //
            .withNullable(true) //
            .build();

    private final Schema.Entry meta1 = new Schema.Entry.Builder() //
            .withName("meta1") //
            .withType(Schema.Type.INT) //
            .withMetadata(true) //
            .build();

    private final Schema.Entry meta2 = new Schema.Entry.Builder() //
            .withName("meta2") //
            .withType(Schema.Type.STRING) //
            .withMetadata(true) //
            .withNullable(true) //
            .build();

    @Test
    void testEntries() {
        Assertions.assertFalse(dataEntry1.isMetadata(), "meta data should be false by default");
        Assertions.assertTrue(meta1.isMetadata(), "meta data should be true here");
    }

    @Test
    void getAllEntries() {
        final Schema schema = new BuilderImpl() //
                .withType(Type.RECORD) //
                .withEntry(dataEntry1) //
                .withEntry(meta1) //
                .withEntry(dataEntry2) //
                .withEntry(meta2) //
                .build();
        final List<Entry> entries = schema.getEntries();
        Assertions.assertEquals(2, entries.size());
        Assertions.assertTrue(entries.contains(this.dataEntry1));
        Assertions.assertTrue(entries.contains(this.dataEntry2));

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
                .withEntry(dataEntry1) //
                .withEntry(meta1) //
                .withEntry(meta2) //
                .build();

        final Schema schema1 = new BuilderImpl() //
                .withType(Type.RECORD) //
                .withEntry(dataEntry1) //
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
                .withEntry(newEntry("field1", Type.STRING)
                        .withNullable(true)
                        .withRawName("field1")
                        .withDefaultValue(5)
                        .withComment("Comment")
                        .build())
                .withEntry(newEntry("record_id", Type.INT).withMetadata(true).withProp("method", "FIFO").build())
                .withEntry(newEntry("field2", Type.STRING).withMetadata(true).build())
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

        Assertions.assertThrows(IllegalArgumentException.class, () -> this.newSchema(entry3, entry3));
    }

    private Schema newSchema(Entry... entries) {
        final Schema.Builder builder = new BuilderImpl().withType(Type.RECORD);
        for (Entry e : entries) {
            builder.withEntry(e);
        }
        return builder.build();
    }

    private Entry newEntry(final String name, final String defaultValue) {
        return new Entry.Builder() //
                .withName(name) //
                .withType(Type.STRING) //
                .withDefaultValue(defaultValue) //
                .build();
    }

    private Schema.Entry.Builder newEntry(final String name, final Schema.Type type) {
        return new Schema.Entry.Builder().withName(name).withType(type);
    }

}