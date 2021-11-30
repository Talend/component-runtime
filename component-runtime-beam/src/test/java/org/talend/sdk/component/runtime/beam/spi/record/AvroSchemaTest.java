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
package org.talend.sdk.component.runtime.beam.spi.record;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.joining;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.talend.sdk.component.api.record.Schema.Type.DATETIME;
import static org.talend.sdk.component.api.record.Schema.Type.RECORD;
import static org.talend.sdk.component.api.record.Schema.Type.STRING;

import java.util.Iterator;
import java.util.List;

import org.apache.avro.LogicalTypes;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.record.Schema.Builder;
import org.talend.sdk.component.api.record.Schema.EntriesOrder;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;
import org.talend.sdk.component.runtime.beam.spi.AvroRecordBuilderFactoryProvider;
import org.talend.sdk.component.runtime.record.SchemaImpl;

class AvroSchemaTest {

    @Test
    void getRecordType() {
        final Schema.Field field = new Schema.Field("nf",
                Schema.createUnion(Schema.create(Schema.Type.NULL), Schema.create(Schema.Type.STRING)), null, null);
        field.addProp(KeysForAvroProperty.LABEL, "n f");

        final Schema delegate = Schema
                .createUnion(Schema.create(Schema.Type.NULL),
                        Schema.createRecord("foo", null, null, false, singletonList(field)));
        final AvroSchema schema = new AvroSchema(delegate);
        final List<org.talend.sdk.component.api.record.Schema.Entry> entries = schema.getEntries();
        assertEquals(RECORD, schema.getType());
        assertEquals(1, entries.size());
        final org.talend.sdk.component.api.record.Schema.Entry entry = entries.iterator().next();
        assertEquals(STRING, entry.getType());
        assertTrue(entry.isNullable());
        assertEquals("nf", entry.getName());
        assertEquals("n f", entry.getRawName());
        assertEquals("n f", entry.getOriginalFieldName());
    }

    @Test
    void schemaProps() {
        final Schema.Field field = new Schema.Field("nf",
                Schema.createUnion(Schema.create(Schema.Type.NULL), Schema.create(Schema.Type.STRING)), null, null);
        field.addProp(KeysForAvroProperty.LABEL, "n f");
        field.addProp("one", "_1");
        field.addProp("two", "_2");

        final Schema delegate = Schema.createRecord("foo", null, null, false, singletonList(field));
        delegate.addProp("root", "toor");
        final AvroSchema schema = new AvroSchema(delegate);
        assertEquals("toor", schema.getProp("root"));
        final List<org.talend.sdk.component.api.record.Schema.Entry> entries = schema.getEntries();
        final org.talend.sdk.component.api.record.Schema.Entry entry = entries.iterator().next();
        assertEquals("n f", entry.getProp(KeysForAvroProperty.LABEL));
        assertEquals("_1", entry.getProp("one"));
        assertEquals("_2", entry.getProp("two"));
    }

    @Test
    void nullField() {
        final AvroSchema schema = new AvroSchema(Schema
                .createRecord(singletonList(new Schema.Field("nf", Schema.create(Schema.Type.NULL), null, null))));
        assertTrue(schema.getEntries().isEmpty());
        assertEquals("AvroSchema(delegate={\"type\":\"record\",\"fields\":[{\"name\":\"nf\",\"type\":\"null\"}]})",
                schema.toString());
    }

    @Test
    void emptySchema() {
        final org.apache.avro.Schema avro =
                AvroSchema.class.cast(new AvroSchemaBuilder().withType(RECORD).build()).getDelegate();
        assertTrue(avro.getFields().isEmpty());
    }

    @Test
    void checkDateConversion() {
        final RecordBuilderFactory factory = new AvroRecordBuilderFactoryProvider().apply("test");
        final org.apache.avro.Schema avro = AvroSchema.class
                .cast(new AvroSchemaBuilder()
                        .withType(RECORD)
                        .withEntry(factory.newEntryBuilder().withType(DATETIME).withName("date").build())
                        .build())
                .getDelegate();
        assertEquals(DATETIME, new AvroSchema(avro).getEntries().iterator().next().getType());
        assertEquals(LogicalTypes.timestampMillis(), LogicalTypes.fromSchema(avro.getField("date").schema()));
    }

    @Test
    void checkDateConversionFromExternalAvro() {
        final org.apache.avro.Schema avro = SchemaBuilder
                .record("test")
                .fields()
                .name("date")
                .type(LogicalTypes.timestampMillis().addToSchema(Schema.create(Schema.Type.LONG)))
                .noDefault()
                .endRecord();
        assertEquals(DATETIME, new AvroSchema(avro).getEntries().iterator().next().getType());
    }

    @Test
    void ensureNullableArePropagated() {
        final RecordBuilderFactory factory = new AvroRecordBuilderFactoryProvider().apply("test");
        { // nullable = true
            final org.talend.sdk.component.api.record.Schema sdkSchema = new AvroSchemaBuilder()
                    .withType(RECORD)
                    .withEntry(factory.newEntryBuilder().withType(STRING).withName("name").withNullable(true).build())
                    .build();
            final org.apache.avro.Schema avro = AvroSchema.class.cast(sdkSchema).getDelegate();
            final Schema schema = avro.getFields().iterator().next().schema();
            assertEquals(2, schema.getTypes().size());
            final Iterator<Schema> types = schema.getTypes().iterator();
            assertEquals(Schema.Type.NULL, types.next().getType());
            assertEquals(Schema.Type.STRING, types.next().getType());
            assertTrue(sdkSchema.getEntries().iterator().next().isNullable());
        }
        { // nullable = false
            final org.talend.sdk.component.api.record.Schema sdkSchema = new AvroSchemaBuilder()
                    .withType(RECORD)
                    .withEntry(factory.newEntryBuilder().withType(STRING).withName("name").withNullable(false).build())
                    .build();
            assertFalse(sdkSchema.getEntries().iterator().next().isNullable());
        }
    }

    @Test
    void testOrder() {
        final org.talend.sdk.component.api.record.Schema schema = new AvroSchemaBuilder()//
                .withType(RECORD) //
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
    }

    @Test
    void testToBuilder() {
        final org.talend.sdk.component.api.record.Schema schemaOrigin = new AvroSchemaBuilder() //
                .withType(RECORD) //
                .withEntry(data1) //
                .withEntry(meta1) //
                .withEntry(data2) //
                .withEntry(meta2) //
                .moveAfter("meta1", "data1")
                .moveBefore("data2", "meta2")
                .build();
        assertEquals("meta1,data1,meta2,data2", getSchemaFields(schemaOrigin));
        Builder builder = schemaOrigin.toBuilder();
        builder.withEntry(newEntry("data3", org.talend.sdk.component.api.record.Schema.Type.STRING).build());
        builder.withEntry(
                newEntry("meta3", org.talend.sdk.component.api.record.Schema.Type.STRING).withMetadata(true).build());
        final org.talend.sdk.component.api.record.Schema schemaNew = builder.build();
        assertEquals(3, schemaNew.getMetadata().size());
        assertEquals(3, schemaNew.getEntries().size());
        assertEquals(6, schemaNew.getAllEntries().count());
        assertEquals("meta1,data1,meta2,data2,data3,meta3", getSchemaFields(schemaNew));
    }

    @Test
    void testBuilder() {
        final org.talend.sdk.component.api.record.Schema schema = new AvroSchemaBuilder() //
                .withType(RECORD) //
                .withEntry(data1) //
                .withEntryBefore("data1", meta1) //
                .withEntry(data2) //
                .withEntryAfter("meta1", meta2) //
                .build();
        assertEquals("meta1,meta2,data1,data2", getSchemaFields(schema));
        // failing
        final Builder builder = new AvroSchemaBuilder().withType(RECORD);
        assertThrows(IllegalArgumentException.class, () -> builder.withEntryAfter("data1", meta1));
        assertThrows(IllegalArgumentException.class, () -> builder.withEntryBefore("data1", meta2));
    }

    @Test
    void testCustomEntriesOrder() {
        final org.talend.sdk.component.api.record.Schema schema = new AvroSchemaBuilder() //
                .withType(RECORD) //
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

    private String getSchemaFields(final org.talend.sdk.component.api.record.Schema schema) {
        return schema.getEntriesOrdered().stream().map(e -> e.getName()).collect(joining(","));
    }

    private String getSchemaFields(final org.talend.sdk.component.api.record.Schema schema,
            final EntriesOrder entriesOrder) {
        return schema.getEntriesOrdered(entriesOrder).stream().map(e -> e.getName()).collect(joining(","));
    }

    private org.talend.sdk.component.api.record.Schema.Entry.Builder newEntry(final String name,
            final org.talend.sdk.component.api.record.Schema.Type type) {
        return new SchemaImpl.EntryImpl.BuilderImpl().withName(name).withType(type);
    }

    private final org.talend.sdk.component.api.record.Schema.Entry data1 =
            new SchemaImpl.EntryImpl.BuilderImpl() //
                    .withName("data1") //
                    .withType(org.talend.sdk.component.api.record.Schema.Type.INT) //
                    .build();

    private final org.talend.sdk.component.api.record.Schema.Entry data2 =
            new SchemaImpl.EntryImpl.BuilderImpl() //
                    .withName("data2") //
                    .withType(org.talend.sdk.component.api.record.Schema.Type.STRING) //
                    .withNullable(true) //
                    .build();

    private final org.talend.sdk.component.api.record.Schema.Entry meta1 =
            new SchemaImpl.EntryImpl.BuilderImpl() //
                    .withName("meta1") //
                    .withType(org.talend.sdk.component.api.record.Schema.Type.INT) //
                    .withMetadata(true) //
                    .build();

    private final org.talend.sdk.component.api.record.Schema.Entry meta2 =
            new SchemaImpl.EntryImpl.BuilderImpl() //
                    .withName("meta2") //
                    .withType(org.talend.sdk.component.api.record.Schema.Type.STRING) //
                    .withMetadata(true) //
                    .withNullable(true) //
                    .build();

}
