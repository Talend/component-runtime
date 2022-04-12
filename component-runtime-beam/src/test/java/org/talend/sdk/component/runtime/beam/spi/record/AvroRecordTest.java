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
package org.talend.sdk.component.runtime.beam.spi.record;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.apache.beam.sdk.util.SerializableUtils.ensureSerializableByCoder;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.function.Supplier;

import org.apache.avro.Conversions;
import org.apache.avro.Schema.Field;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.IndexedRecord;
import org.apache.avro.reflect.ReflectData;
import org.apache.avro.util.Utf8;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.transforms.Create;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.PCollection;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.api.record.Schema.Entry;
import org.talend.sdk.component.api.record.TypePropertyKey;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;
import org.talend.sdk.component.runtime.beam.coder.registry.SchemaRegistryCoder;
import org.talend.sdk.component.runtime.beam.spi.AvroRecordBuilderFactoryProvider;
import org.talend.sdk.component.runtime.beam.transform.RecordNormalizer;
import org.talend.sdk.component.runtime.manager.service.api.Unwrappable;
import org.talend.sdk.component.runtime.record.RecordBuilderFactoryImpl;
import org.talend.sdk.component.runtime.record.RecordImpl;
import org.talend.sdk.component.runtime.record.SchemaImpl;

import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonReaderFactory;

class AvroRecordTest {

    @Test
    void recordEntryFromName() {
        assertEquals("{\"record\": {\"name\": \"ok\"}}",
                Unwrappable.class
                        .cast(new AvroRecordBuilder()
                                .withRecord("record", new AvroRecordBuilder().withString("name", "ok").build())
                                .build())
                        .unwrap(IndexedRecord.class)
                        .toString());
    }

    @Test
    void providedSchemaGetSchema() {
        final Schema schema = new AvroSchemaBuilder()
                .withType(Schema.Type.RECORD)
                .withEntry(new SchemaImpl.EntryImpl.BuilderImpl()
                        .withName("name")
                        .withNullable(true)
                        .withType(Schema.Type.STRING)
                        .build())
                .build();
        final Record record = new AvroRecordBuilder(schema).withString("name", "ok").build();
        assertEquals(schema, record.getSchema());
    }

    @Test
    void providedSchemaNullable() {
        final Supplier<AvroRecordBuilder> builder = () -> new AvroRecordBuilder(new AvroSchemaBuilder()
                .withType(Schema.Type.RECORD)
                .withEntry(new SchemaImpl.EntryImpl.BuilderImpl()
                        .withName("name")
                        .withNullable(true)
                        .withType(Schema.Type.STRING)
                        .build())
                .build());
        { // normal/valued
            final Record record = builder.get().withString("name", "ok").build();
            assertEquals(1, record.getSchema().getEntries().size());
            assertEquals("ok", record.getString("name"));
        }
        { // null
            final Record record = builder.get().withString("name", null).build();
            assertEquals(1, record.getSchema().getEntries().size());
            assertNull(record.getString("name"));
        }
        { // missing entry
            assertThrows(IllegalArgumentException.class, () -> builder.get().withString("name2", null).build());
        }
        { // invalid type entry
            assertThrows(IllegalArgumentException.class, () -> builder.get().withInt("name", 2).build());
        }
    }

    @Test
    void providedSchemaNullableDate() {
        final Supplier<AvroRecordBuilder> builder = () -> new AvroRecordBuilder(new AvroSchemaBuilder()
                .withType(Schema.Type.RECORD)
                .withEntry(new SchemaImpl.EntryImpl.BuilderImpl()
                        .withName("name")
                        .withNullable(true)
                        .withType(Schema.Type.DATETIME)
                        .build())
                .build());
        { // null
            final Record record = builder.get().withDateTime("name", (Date) null).build();
            assertEquals(1, record.getSchema().getEntries().size());
            assertNull(record.getDateTime("name"));
        }
    }

    @Test
    void providedSchemaNullableDecimal() {
        final Supplier<AvroRecordBuilder> builder = () -> new AvroRecordBuilder(new AvroSchemaBuilder()
                .withType(Schema.Type.RECORD)
                .withEntry(new SchemaImpl.EntryImpl.BuilderImpl()
                        .withName("decimal")
                        .withNullable(true)
                        .withType(Schema.Type.DECIMAL)
                        .withProp(TypePropertyKey.PRECISION, "32")
                        .build())
                .build());
        { // null
            final Record record = builder.get().withDecimal("decimal", (BigDecimal) null).build();
            assertEquals(1, record.getSchema().getEntries().size());
            assertNull(record.getDecimal("decimal"));
        }
    }

    @Test
    void providedSchemaNotNullable() {
        final Supplier<RecordImpl.BuilderImpl> builder = () -> new AvroRecordBuilder(new AvroSchemaBuilder()
                .withType(Schema.Type.RECORD)
                .withEntry(new SchemaImpl.EntryImpl.BuilderImpl()
                        .withName("name")
                        .withNullable(false)
                        .withType(Schema.Type.STRING)
                        .build())
                .build());
        { // normal/valued
            final Record record = builder.get().withString("name", "ok").build();
            assertEquals(1, record.getSchema().getEntries().size());
            assertEquals("ok", record.getString("name"));
        }
        { // null
            assertThrows(IllegalArgumentException.class, () -> builder.get().withString("name", null).build());
        }
    }

    @Test
    void providedSchemaNotNullableDate() {
        final Supplier<AvroRecordBuilder> builder = () -> new AvroRecordBuilder(new AvroSchemaBuilder()
                .withType(Schema.Type.RECORD)
                .withEntry(new SchemaImpl.EntryImpl.BuilderImpl()
                        .withName("name")
                        .withNullable(false)
                        .withType(Schema.Type.DATETIME)
                        .build())
                .build());
        { // normal/valued
            final Record record = builder.get().withDateTime("name", new Date()).build();
            assertEquals(1, record.getSchema().getEntries().size());
            assertNotNull(record.getDateTime("name"));
        }
        { // null
            assertThrows(IllegalArgumentException.class, () -> builder.get().withString("name", null).build());
        }
    }

    @Test
    void withNullEntry() {
        final AvroRecordBuilderFactoryProvider provider = new AvroRecordBuilderFactoryProvider();
        final String oldValue = System.setProperty("talend.component.beam.record.factory.impl", "avro");
        try {
            final RecordBuilderFactory factory = provider.apply("test");

            final Schema schema = factory.newSchemaBuilder(Schema.Type.RECORD)
                    .withEntry(factory.newEntryBuilder()
                            .withName("field")
                            .withType(Schema.Type.STRING)
                            .withProp("k1", "v1")
                            .build())
                    .withProp("schemaK1", "schemaV1")
                    .build();

            final Entry field = factory.newEntryBuilder()
                    .withName("Hello")
                    .withNullable(true)
                    .withType(Schema.Type.RECORD)
                    .withElementSchema(schema)
                    .build();

            final Schema schemaBis = factory.newSchemaBuilder(Schema.Type.RECORD)
                    .withEntry(field)
                    .build();

            Schema hello = schemaBis.getEntry("Hello").getElementSchema();

            final Record record = factory.newRecordBuilder(hello)
                    .withString("field", "world")
                    .build();

            Assertions.assertNotNull(record);
            Assertions.assertEquals("world", record.getString("field"));
            Assertions.assertEquals("schemaV1", record.getSchema().getProp("schemaK1"));
            Assertions.assertEquals("v1", record.getSchema().getEntry("field").getProp("k1"));
        } finally {
            if (oldValue == null) {
                System.clearProperty("talend.component.beam.record.factory.impl");
            } else {
                System.setProperty("talend.component.beam.record.factory.impl", oldValue);
            }
        }
    }

    @Test
    void bytes() {
        final byte[] array = { 0, 1, 2, 3, 4 };
        final Record record = new AvroRecordBuilder().withBytes("bytes", array).build();
        assertArrayEquals(array, record.getBytes("bytes"));

        final Record copy = ensureSerializableByCoder(SchemaRegistryCoder.of(), record, "test");
        assertArrayEquals(array, copy.getBytes("bytes"));
    }

    @Test
    void stringGetObject() {
        final GenericData.Record avro = new GenericData.Record(org.apache.avro.Schema
                .createRecord(getClass().getName() + ".StringTest", null, null, false,
                        singletonList(new org.apache.avro.Schema.Field("str",
                                org.apache.avro.Schema.create(org.apache.avro.Schema.Type.STRING), null, null))));
        avro.put(0, new Utf8("test"));
        final Record record = new AvroRecord(avro);
        final Object str = record.get(Object.class, "str");
        assertFalse(str.getClass().getName(), Utf8.class.isInstance(str));
        assertEquals("test", str);
    }

    @Test
    void testLabel() {
        final Field f = new org.apache.avro.Schema.Field("str",
                org.apache.avro.Schema.create(org.apache.avro.Schema.Type.STRING), null, null);
        f.addProp(KeysForAvroProperty.LABEL, "my label");
        final GenericData.Record avro = new GenericData.Record(org.apache.avro.Schema
                .createRecord(getClass().getName() + ".StringTest", null, null, false, singletonList(f)));
        avro.put(0, new Utf8("test"));
        final Record record = new AvroRecord(avro);

        final Schema schema = record.getSchema();
        final List<Schema.Entry> entries = schema.getEntries();
        assertEquals("my label", entries.get(0).getRawName());
    }

    @Test
    void schemaRegistryCoder() throws Exception {
        final org.apache.avro.Schema datetime = org.apache.avro.SchemaBuilder
                .record("datetimes")
                .prop("rootProp1", "rootValue1")
                .prop("rootProp2", "rootValue2")
                .fields()
                .name("f1")
                .prop("logicalType", "timestamp-millis")
                .prop("talend.component.DATETIME", "true")
                .prop("fieldProp1", "fieldValue1")
                .prop("fieldProp2", "fieldValue2")
                .type()
                .unionOf()
                .nullType()
                .and()
                .longType()
                .endUnion()
                .noDefault()
                //
                .name("f2")
                .prop("logicalType", "date")
                .prop("talend.component.DATETIME", "true")
                .type()
                .unionOf()
                .nullType()
                .and()
                .longType()
                .endUnion()
                .noDefault()
                //
                .name("f3")
                .prop("logicalType", "date")
                .prop("talend.component.DATETIME", "true")
                .type()
                .unionOf()
                .nullType()
                .and()
                .longType()
                .endUnion()
                .noDefault()
                //
                .endRecord();
        final ZonedDateTime zdt = ZonedDateTime.of(2020, 01, 24, 15, 0, 1, 0, ZoneId.of("UTC"));
        final Date date = new Date();
        final GenericData.Record avro = new GenericData.Record(datetime);
        avro.put(0, zdt.toInstant().toEpochMilli());
        avro.put(1, date.getTime());
        avro.put(2, null);
        final Record record = new AvroRecord(avro);
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        SchemaRegistryCoder.of().encode(record, buffer);
        final Record decoded = SchemaRegistryCoder.of().decode(new ByteArrayInputStream(buffer.toByteArray()));
        assertEquals(zdt, decoded.getDateTime("f1"));
        assertEquals(date.getTime(), decoded.getDateTime("f2").toInstant().toEpochMilli());
        assertNull(decoded.getDateTime("f3"));
        // schema props
        final Schema s = decoded.getSchema();
        assertEquals(2, s.getProps().size());
        assertEquals("rootValue1", s.getProp("rootProp1"));
        assertEquals("rootValue2", s.getProp("rootProp2"));
        // field props
        final Entry sf = s.getEntries().get(0);
        assertEquals("timestamp-millis", sf.getProp("logicalType"));
        assertEquals("true", sf.getProp("talend.component.DATETIME"));
        assertEquals("fieldValue1", sf.getProp("fieldProp1"));
        assertEquals("fieldValue2", sf.getProp("fieldProp2"));
    }

    @Test
    void pipelineDateTimeFields() throws Exception {
        final RecordBuilderFactory factory = new AvroRecordBuilderFactoryProvider().apply(null);
        final Record.Builder builder = factory.newRecordBuilder();
        final Date date = new Date(new java.text.SimpleDateFormat("yyyy-MM-dd").parse("2018-12-6").getTime());
        final Date datetime = new Date();
        final Date time = new Date(1000 * 60 * 60 * 15 + 1000 * 60 * 20 + 39000); // 15:20:39
        builder.withDateTime("t_date", date);
        builder.withDateTime("t_datetime", datetime);
        builder.withDateTime("t_time", time);
        final Record rec = builder.build();
        final Pipeline pipeline = Pipeline.create();
        final PCollection<Record> input = pipeline.apply(Create.of(asList(rec)).withCoder(SchemaRegistryCoder.of())); //
        final PCollection<Record> output = input.apply(new RecordToRecord());
        assertEquals(org.apache.beam.sdk.PipelineResult.State.DONE, pipeline.run().waitUntilFinish());
    }

    @Test
    void pipelineDecimalFieldsWithAvroRecord() throws Exception {
        final RecordBuilderFactory factory = new AvroRecordBuilderFactoryProvider().apply(null);

        // must provide schema firstly, can't guess if decimal value maybe be null, and decimal must have fixed/same
        // precision and scale
        // final Record.Builder builder = factory.newRecordBuilder();

        // in schema, precision and scale must match the value
        final Schema.Entry f1 = factory.newEntryBuilder()
                .withType(Schema.Type.DECIMAL)
                .withName("t_decimal")
                .withProp(TypePropertyKey.PRECISION, "3")
                .withProp(TypePropertyKey.SCALE, "2")
                .withNullable(true)
                .build();
        final Schema schema = factory.newSchemaBuilder(Schema.Type.RECORD).withEntry(f1).build();

        final Record.Builder builder = factory.newRecordBuilder(schema);

        final BigDecimal decimal = new BigDecimal("1.23");
        builder.withDecimal("t_decimal", decimal);

        final Record rec = builder.build();
        final Pipeline pipeline = Pipeline.create();

        // data processing platform need to add this statement to enable decimal support for beam compiler
        // how to add this for cross threads/cross vm, maybe we need to do a fix for beam LazyAvroCoder/AvroCoder class
        // and all codec class like SchemaRegistryCoder too, to add this
        GenericData.get().addLogicalTypeConversion(new Conversions.DecimalConversion());

        // should not use ReflectData for any GenericRecord implements
        // ReflectData.get().addLogicalTypeConversion(new Conversions.DecimalConversion());

        final PCollection<Record> input = pipeline.apply(Create.of(asList(rec)).withCoder(SchemaRegistryCoder.of())); //
        final PCollection<Record> output = input.apply(new RecordToRecord());
        assertEquals(org.apache.beam.sdk.PipelineResult.State.DONE, pipeline.run().waitUntilFinish());
    }

    @Test
    void arrayOfRecords() {
        final String before = System.getProperty("talend.component.beam.record.factory.impl");
        try {
            System.setProperty("talend.component.beam.record.factory.impl", "avro");
            final RecordBuilderFactory factory = new AvroRecordBuilderFactoryProvider().apply("test");

            final Schema.Entry f1 = factory.newEntryBuilder().withType(Schema.Type.STRING).withName("f1").build();
            final Schema innerSchema = factory.newSchemaBuilder(Schema.Type.RECORD).withEntry(f1).build();

            final Schema.Entry array = factory
                    .newEntryBuilder()
                    .withType(Schema.Type.ARRAY) //
                    .withName("array") //
                    .withElementSchema(innerSchema) //
                    .build();
            final Schema recordSchema = factory.newSchemaBuilder(Schema.Type.RECORD).withEntry(array).build();

            final Record record = factory.newRecordBuilder(innerSchema).withString(f1, "Hello").build();
            final Record record1 = factory.newRecordBuilder(recordSchema).withArray(array, asList(record)).build();
            final Collection<Record> records = record1.getArray(Record.class, "array");

            Assertions.assertEquals(1, records.size());
            final Record next = records.iterator().next();
            Assertions.assertTrue(next instanceof Record);
        } finally {
            if (before == null) {
                System.clearProperty("talend.component.beam.record.factory.impl");
            } else {
                System.setProperty("talend.component.beam.record.factory.impl", before);
            }
        }
    }

    @ParameterizedTest
    @ValueSource(ints = { 2, 3, 4, 5 }) // number of nested arrays.
    void arrayOfArrayOfRecords(final int level) {
        final String before = System.getProperty("talend.component.beam.record.factory.impl");
        try {
            System.setProperty("talend.component.beam.record.factory.impl", "avro");
            final RecordBuilderFactory factory = new AvroRecordBuilderFactoryProvider().apply("test");

            final Schema.Entry f1 = factory.newEntryBuilder().withType(Schema.Type.STRING).withName("f1").build();
            final Schema innerSchema = factory.newSchemaBuilder(Schema.Type.RECORD).withEntry(f1).build();
            Schema currentSchema = innerSchema;
            for (int i = 1; i < level; i++) {
                currentSchema = factory.newSchemaBuilder(Schema.Type.ARRAY).withElementSchema(currentSchema).build();
            }
            final Schema fieldSchema = currentSchema;
            final Schema.Entry array = factory
                    .newEntryBuilder()
                    .withType(Schema.Type.ARRAY) //
                    .withName("array") //
                    .withElementSchema(fieldSchema) //
                    .build();
            final Schema recordSchema = factory.newSchemaBuilder(Schema.Type.RECORD).withEntry(array).build();

            final Record record = factory.newRecordBuilder(innerSchema).withString(f1, "Hello").build();
            Collection inputRecords = asList(record);
            for (int i = 1; i < level; i++) {
                inputRecords = asList(inputRecords);
            }
            final Record record1 = factory
                    .newRecordBuilder(recordSchema)
                    .withArray(array, inputRecords) //
                    .build();
            final Collection<?> records = record1.getArray(Collection.class, "array");
            Assertions.assertEquals(1, records.size());
            Object next = records;
            for (int i = 1; i < level; i++) {
                next = ((Collection) next).iterator().next();
                Assertions.assertTrue(next instanceof Collection);
            }

            final Object rec = ((Collection) next).iterator().next();
            Assertions.assertTrue(rec instanceof Record);
        } finally {
            if (before == null) {
                System.clearProperty("talend.component.beam.record.factory.impl");
            } else {
                System.setProperty("talend.component.beam.record.factory.impl", before);
            }
        }
    }

    public class RecordToRecord extends PTransform<PCollection<Record>, PCollection<Record>> {

        private final RecordBuilderFactory factory;

        public RecordToRecord() {
            factory = new AvroRecordBuilderFactoryProvider().apply(null);
        }

        @Override
        public PCollection<Record> expand(final PCollection<Record> input) {
            return input.apply("RecordToRecord", ParDo.of(new RecordNormalizer(factory)));
        }
    }

    @Test
    void testConstructor() {
        // Preparation
        final RecordBuilderFactory stdFactory = new RecordBuilderFactoryImpl("test");
        final Schema.Entry field = stdFactory
                .newEntryBuilder() //
                .withName("field1") //
                .withType(Schema.Type.STRING) //
                .withNullable(true) //
                .withRawName("raw") //
                .build();
        final Schema.Entry oid = stdFactory
                .newEntryBuilder() //
                .withName("$oid") //
                .withType(Schema.Type.STRING) //
                .withNullable(true) //
                .build();
        final Schema.Entry meta = stdFactory
                .newEntryBuilder() //
                .withName("meta1") //
                .withType(Schema.Type.STRING) //
                .withNullable(true) //
                .withMetadata(true)
                .withRawName("metaRaw") //
                .build();

        final Schema schema =
                stdFactory.newSchemaBuilder(Schema.Type.RECORD).withEntry(field).withEntry(oid).withEntry(meta).build();

        final Record record = stdFactory
                .newRecordBuilder(schema) //
                .withString(field, "Hello") //
                .withString(meta, "myMeta") //
                .withString(oid, "oidValue") //
                .build();

        // Test
        final AvroRecord avrRec = new AvroRecord(record);

        // Check
        final Schema schemaAvro = avrRec.getSchema();
        Assertions.assertEquals(3, schemaAvro.getAllEntries().count());
        Assertions.assertEquals("Hello", avrRec.getString("field1"));
        Assertions.assertEquals("myMeta", avrRec.getString("meta1"));
        Assertions.assertEquals("oidValue", avrRec.getString(oid.getRawName()));
    }

    @Test
    void recordCollisionName() {
        // Case with collision without sanitize.
        final Record record = new AvroRecordBuilder() //
                .withString("field", "value1") //
                .withInt("field", 234) //
                .build();
        final Object value = record.get(Object.class, "field");
        Assertions.assertEquals(Integer.valueOf(234), value);

        // Case with collision and sanitize.
        final Record recordSanitize = new AvroRecordBuilder() //
                .withString("70歳以上", "value70") //
                .withString("60歳以上", "value60") //
                .build();
        Assertions.assertEquals(2, recordSanitize.getSchema().getEntries().size());
        final String name1 = Schema.sanitizeConnectionName("70歳以上");
        Assertions.assertEquals("value70", recordSanitize.getString(name1));
        Assertions.assertEquals("value60", recordSanitize.getString(name1 + "_1"));
    }

    @Test
    void testArray() {
        final RecordBuilderFactory stdFactory = new RecordBuilderFactoryImpl("test");
        final Schema.Entry field = stdFactory
                .newEntryBuilder() //
                .withName("field1") //
                .withType(Schema.Type.STRING) //
                .withNullable(true) //
                .withRawName("raw") //
                .build();
        final Schema.Entry oid = stdFactory
                .newEntryBuilder() //
                .withName("$oid") //
                .withType(Schema.Type.STRING) //
                .withNullable(true) //
                .build();
        final Schema.Entry meta = stdFactory
                .newEntryBuilder() //
                .withName("meta1") //
                .withType(Schema.Type.STRING) //
                .withNullable(true) //
                .withMetadata(true)
                .withRawName("metaRaw") //
                .build();

        final Schema schemaArray =
                stdFactory.newSchemaBuilder(Schema.Type.RECORD).withEntry(field).withEntry(oid).withEntry(meta).build();
        final Schema.Entry arrayEntry = stdFactory
                .newEntryBuilder()
                .withType(Schema.Type.ARRAY)
                .withName("array")
                .withElementSchema(schemaArray)
                .withMetadata(true)
                .build();

        final Schema schema = stdFactory.newSchemaBuilder(Schema.Type.RECORD).withEntry(arrayEntry).build();
        final Record record1 = stdFactory
                .newRecordBuilder(schemaArray) //
                .withString(field, "Hello") //
                .withString(meta, "myMeta") //
                .withString(oid, "oidValue") //
                .build();
        final Record record2 = stdFactory
                .newRecordBuilder(schemaArray) //
                .withString(field, "Priviet") //
                .withString(meta, "OtherMeta") //
                .withString(oid, "Oid2") //
                .build();
        final Record record = stdFactory //
                .newRecordBuilder(schema) //
                .withArray(arrayEntry, Arrays.asList(record1, record2)) //
                .build();

        // Test
        final AvroRecord avrRec = new AvroRecord(record);

        // Check
        final Schema schemaAvro = avrRec.getSchema();
        Assertions.assertEquals(1, schemaAvro.getAllEntries().count());
        final Schema.Entry entry = avrRec.getSchema().getAllEntries().findFirst().get();
        Assertions.assertEquals("array", entry.getName());
        final Collection<Record> records = avrRec.getArray(Record.class, "array");
        Assertions.assertEquals(2, records.size());
    }
}
