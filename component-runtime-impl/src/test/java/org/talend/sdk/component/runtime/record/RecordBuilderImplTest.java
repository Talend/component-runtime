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
package org.talend.sdk.component.runtime.record;

import static java.util.stream.Collectors.joining;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.json.bind.annotation.JsonbTransient;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.api.record.Schema.EntriesOrder;
import org.talend.sdk.component.api.record.Schema.Entry;
import org.talend.sdk.component.api.record.Schema.Type;
import org.talend.sdk.component.api.record.SchemaCompanionUtil;
import org.talend.sdk.component.api.record.SchemaProperty;
import org.talend.sdk.component.runtime.record.SchemaImpl.BuilderImpl;
import org.talend.sdk.component.runtime.record.SchemaImpl.EntryImpl;

import lombok.AllArgsConstructor;

class RecordBuilderImplTest {

    @Test
    void providedSchemaGetSchema() {
        final Schema schema = new SchemaImpl.BuilderImpl()
                .withType(Schema.Type.RECORD)
                .withEntry(new SchemaImpl.EntryImpl.BuilderImpl()
                        .withName("name")
                        .withNullable(true)
                        .withType(Schema.Type.STRING)
                        .build())
                .build();
        assertEquals(schema, new RecordImpl.BuilderImpl(schema).withString("name", "ok").build().getSchema());

        Schema.EntriesOrder e = Schema.EntriesOrder.of(new RecordImpl.BuilderImpl().getCurrentEntries()
                .stream()
                .map(Schema.Entry::getName)
                .collect(Collectors.toList()));
    }

    @Test
    void getValue() {
        final RecordImpl.BuilderImpl builder = new RecordImpl.BuilderImpl();
        Assertions.assertNull(builder.getValue("name"));
        final Entry entry = new EntryImpl.BuilderImpl() //
                .withName("name") //
                .withNullable(true) //
                .withType(Type.STRING) //
                .build();//

        // now tck STRING accept any object as TCOMP-2292
        // assertThrows(IllegalArgumentException.class, () -> builder.with(entry, 234L));

        builder.with(entry, "value");
        Assertions.assertEquals("value", builder.getValue("name"));

        final Entry entryTime = new EntryImpl.BuilderImpl() //
                .withName("time") //
                .withNullable(true) //
                .withType(Type.DATETIME) //
                .build();//
        final ZonedDateTime now = ZonedDateTime.now();
        builder.with(entryTime, now);
        Assertions.assertEquals(now.toInstant().toEpochMilli(), builder.getValue("time"));

        final Long next = now.toInstant().toEpochMilli() + 1000L;
        builder.with(entryTime, next);
        Assertions.assertEquals(next, builder.getValue("time"));

        Date date = new Date(next + TimeUnit.DAYS.toMillis(1));
        builder.with(entryTime, date);
        Assertions.assertEquals(date.toInstant().toEpochMilli(), builder.getValue("time"));

        final Instant instant = Timestamp.valueOf("2021-04-19 13:37:07.752345").toInstant();
        builder.with(entryTime, instant);
        Assertions.assertEquals(instant, builder.getValue("time"));
    }

    @Test
    void recordEntryFromName() {
        final Schema schema = new SchemaImpl.BuilderImpl()
                .withType(Schema.Type.RECORD)
                .withEntry(new SchemaImpl.EntryImpl.BuilderImpl()
                        .withName("name")
                        .withNullable(true)
                        .withType(Schema.Type.STRING)
                        .build())
                .build();
        assertEquals("{\"record\":{\"name\":\"ok\"}}",
                new RecordImpl.BuilderImpl()
                        .withRecord("record", new RecordImpl.BuilderImpl(schema).withString("name", "ok").build())
                        .build()
                        .toString());
    }

    @Test
    void providedSchemaNullable() {
        final Supplier<RecordImpl.BuilderImpl> builder = () -> new RecordImpl.BuilderImpl(new SchemaImpl.BuilderImpl()
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
        { // missing entry in the schema
            assertThrows(IllegalArgumentException.class, () -> builder.get().withString("name2", null).build());
        }
        { // invalid type entry
            assertThrows(IllegalArgumentException.class, () -> builder.get().withInt("name", 2).build());
        }
    }

    @Test
    void providedSchemaNotNullable() {
        final Supplier<RecordImpl.BuilderImpl> builder = () -> new RecordImpl.BuilderImpl(new SchemaImpl.BuilderImpl()
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
        { // missing entry value
            assertThrows(IllegalArgumentException.class, () -> builder.get().build());
        }
    }

    @Test
    void nullSupportString() {
        final RecordImpl.BuilderImpl builder = new RecordImpl.BuilderImpl();
        builder.withString("test", null);
        final Record record = builder.build();
        assertEquals(1, record.getSchema().getEntries().size());
        assertNull(record.getString("test"));
    }

    @Test
    void nullSupportDate() {
        final RecordImpl.BuilderImpl builder = new RecordImpl.BuilderImpl();
        builder.withDateTime("test", (Date) null);
        final Record record = builder.build();
        assertEquals(1, record.getSchema().getEntries().size());
        assertNull(record.getDateTime("test"));
    }

    @Test
    void nullSupportBytes() {
        final RecordImpl.BuilderImpl builder = new RecordImpl.BuilderImpl();
        builder.withBytes("test", null);
        final Record record = builder.build();
        assertEquals(1, record.getSchema().getEntries().size());
        assertNull(record.getBytes("test"));
    }

    @Test
    void nullSupportCollections() {
        final RecordImpl.BuilderImpl builder = new RecordImpl.BuilderImpl();
        final Schema innerArray = new BuilderImpl().withType(Type.STRING).build();
        final Entry arrayEntry = new EntryImpl.BuilderImpl() //
                .withName("test") //
                .withRawName("test") //
                .withType(Type.ARRAY) //
                .withNullable(true) //
                .withElementSchema(innerArray) //
                .build();
        builder.withArray(arrayEntry, null);
        final Record record = builder.build();
        assertEquals(1, record.getSchema().getEntries().size());
        assertNull(record.getArray(String.class, "test"));
    }

    @Test
    void notNullableNullBehavior() {
        final RecordImpl.BuilderImpl builder = new RecordImpl.BuilderImpl();
        assertThrows(IllegalArgumentException.class, () -> builder
                .withString(new SchemaImpl.EntryImpl.BuilderImpl().withNullable(false).withName("test").build(), null));
    }

    @Test
    void disableNullableCheck_no() {
        RecordBuilderFactoryImpl recordBuilderFactory = new RecordBuilderFactoryImpl("test");
        Schema.Builder schemaBuilder = recordBuilderFactory.newSchemaBuilder(Type.RECORD);
        Schema schema = schemaBuilder.withEntry(
                        recordBuilderFactory.newEntryBuilder().withType(Type.STRING).withName("c1").withNullable(false).build())
                .withEntry(recordBuilderFactory.newEntryBuilder()
                        .withName("c2")
                        .withType(Type.STRING)
                        .withNullable(false)
                        .build())
                .build();
        Record.Builder recordBuilder = recordBuilderFactory.newRecordBuilder(schema);
        recordBuilder.withString("c1", "v1");
        assertThrows(IllegalArgumentException.class, () -> recordBuilder.withString("c2", null));
    }

    @Test
    void disableNullableCheck_yes() {
        System.setProperty("talend.sdk.skip.nullable.check", "true");
        RecordBuilderFactoryImpl recordBuilderFactory = new RecordBuilderFactoryImpl("test");
        Schema.Builder schemaBuilder = recordBuilderFactory.newSchemaBuilder(Type.RECORD);
        Schema schema = schemaBuilder.withEntry(
                        recordBuilderFactory.newEntryBuilder().withType(Type.STRING).withName("c1").withNullable(false).build())
                .withEntry(recordBuilderFactory.newEntryBuilder()
                        .withName("c2")
                        .withType(Type.STRING)
                        .withNullable(false)
                        .build())
                .build();
        Record.Builder recordBuilder = recordBuilderFactory.newRecordBuilder(schema);
        recordBuilder.withString("c1", "v1");
        recordBuilder.withString("c2", null);
        Record record = recordBuilder.build();
        assertNull(record.getString("c2"));
        System.clearProperty("talend.sdk.skip.nullable.check");
    }

    @Test
    void dateTime() {
        final Schema schema = new SchemaImpl.BuilderImpl()
                .withType(Schema.Type.RECORD)
                .withEntry(new SchemaImpl.EntryImpl.BuilderImpl()
                        .withName("date")
                        .withNullable(false)
                        .withType(Schema.Type.DATETIME)
                        .build())
                .build();
        final RecordImpl.BuilderImpl builder = new RecordImpl.BuilderImpl(schema);
        final Record record = builder.withDateTime("date", ZonedDateTime.now()).build();
        assertNotNull(record.getDateTime("date"));

        final RecordImpl.BuilderImpl builder2 = new RecordImpl.BuilderImpl(schema);
        assertThrows(IllegalArgumentException.class, () -> builder2.withDateTime("date", (ZonedDateTime) null));
    }

    @Test
    void withErrorWhenNotSupported() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> withError("false"));
    }

    @Test
    void withError() {
        Record record = withError("true");

        assertFalse(record.isValid());

        final Entry retrievedDateEntry = record.getSchema().getEntry("date");
        assertNotNull(retrievedDateEntry);
        Assertions.assertFalse(retrievedDateEntry.isValid());
        assertEquals(
                "Entry 'date' of type DATETIME is not compatible with given value of type 'java.lang.String': 'not a date'.",
                retrievedDateEntry.getErrorMessage());
        Assertions.assertNull(record.getDateTime("date"));

        final Entry retrievedIntEntry = record.getSchema().getEntry("intValue");
        assertNotNull(retrievedIntEntry);
        Assertions.assertFalse(retrievedIntEntry.isValid());
        assertEquals(
                "Entry 'intValue' of type INT is not compatible with given value of type 'java.lang.String': 'wrong int value'.",
                retrievedIntEntry.getErrorMessage());
        Assertions.assertNull(record.getDateTime("intValue"));

        final Entry retrievedStringEntry = record.getSchema().getEntry("normal");
        assertNotNull(retrievedStringEntry);
        Assertions.assertTrue(retrievedStringEntry.isValid());
        Assertions.assertEquals("No error", record.getString("normal"));

    }

    private Record withError(final String supported) {
        final String errorSupportBackup = System.getProperty(Record.RECORD_ERROR_SUPPORT);
        System.setProperty(Record.RECORD_ERROR_SUPPORT, supported);

        Entry dateEntry = new EntryImpl.BuilderImpl()
                .withName("date")
                .withNullable(false)
                .withErrorCapable(true)
                .withType(Type.DATETIME)
                .build();
        Entry stringEntry = new EntryImpl.BuilderImpl()
                .withName("normal")
                .withNullable(true)
                .withErrorCapable(true)
                .withType(Type.STRING)
                .build();
        Entry intEntry = new EntryImpl.BuilderImpl()
                .withName("intValue")
                .withNullable(false)
                .withErrorCapable(true)
                .withType(Type.INT)
                .build();
        final Schema schema = new SchemaImpl.BuilderImpl()
                .withType(Schema.Type.RECORD)
                .withEntry(dateEntry)
                .withEntry(stringEntry)
                .withEntry(intEntry)
                .build();

        final RecordImpl.BuilderImpl builder = new RecordImpl.BuilderImpl(schema);

        builder.with(stringEntry, "No error");
        builder.with(dateEntry, "not a date");
        builder.with(intEntry, "wrong int value");
        final Record record = builder.build();

        System.setProperty(Record.RECORD_ERROR_SUPPORT, errorSupportBackup == null ? "false" : errorSupportBackup);

        return record;
    }

    @Test
    void testWithWrongEntryType() {
        Entry entry = new SchemaImpl.EntryImpl.BuilderImpl()
                .withName("date")
                .withNullable(false)
                .withType(Schema.Type.DATETIME)
                .build();
        final Schema schema = new SchemaImpl.BuilderImpl()
                .withType(Schema.Type.RECORD)
                .withEntry(entry)
                .build();
        final RecordImpl.BuilderImpl builder = new RecordImpl.BuilderImpl(schema);
        assertNotNull(builder.getEntry("date"));

        assertThrows(IllegalArgumentException.class, () -> builder.with(entry, "String"));
    }

    @Test
    void zonedDateTimeVSInstant() {
        final Schema schema = new SchemaImpl.BuilderImpl()
                .withType(Schema.Type.RECORD)
                .withEntry(new SchemaImpl.EntryImpl.BuilderImpl()
                        .withName("time")
                        .withNullable(false)
                        .withType(Type.DATETIME)
                        .build())
                .build();
        // getInstant("x") with an initial record made withDateTime("x", zdt), and lose precision
        final RecordImpl.BuilderImpl builder = new RecordImpl.BuilderImpl(schema);
        final Record record = builder.withDateTime("time", ZonedDateTime.parse("2021-04-19T13:37:07.752345Z")).build();
        assertNotNull(record.getDateTime("time"));
        assertEquals(ZonedDateTime.parse("2021-04-19T13:37:07.752Z").toInstant(), record.getInstant("time"));

        // getDateTime("x") with an initial record made withInstant("x", v), and keep precision
        java.sql.Timestamp time = java.sql.Timestamp.valueOf("2021-04-19 13:37:07.752345");
        final RecordImpl.BuilderImpl builder3 = new RecordImpl.BuilderImpl(schema);
        final Record record3 = builder3.withInstant("time", time.toInstant()).build();

        assertNotNull(record3.getDateTime("time"));
        assertEquals(ZonedDateTime.ofInstant(time.toInstant(), ZoneId.of("UTC")), record3.getDateTime("time"));

    }

    @Test
    void timestampExceedMillisecond() {
        final Schema schema = new SchemaImpl.BuilderImpl()
                .withType(Schema.Type.RECORD)
                .withEntry(new SchemaImpl.EntryImpl.BuilderImpl()
                        .withName("time")
                        .withNullable(false)
                        .withType(Type.DATETIME)
                        .build())
                .build();
        final RecordImpl.BuilderImpl builder = new RecordImpl.BuilderImpl(schema);
        java.sql.Timestamp time = java.sql.Timestamp.valueOf("2021-04-19 13:37:07.752345");
        final Record record = builder.withInstant("time", time.toInstant()).build();

        assertEquals(time.toInstant(), record.get(Instant.class, "time"));
        assertEquals(time.toInstant(), record.getInstant("time"));

        int nano = time.toInstant().getNano();
        long natime = time.toInstant().toEpochMilli();/// 1000 * 1000_000_000 +nano;
        long ntime = time.toInstant().getEpochSecond();

        Instant back1 = Instant.ofEpochSecond(ntime, nano);
        Instant back2 = Instant.ofEpochSecond(natime, nano);
        assertEquals(time.toInstant(), back1);

    }

    @Test
    void decimal() {
        final Schema schema = new SchemaImpl.BuilderImpl()
                .withType(Schema.Type.RECORD)
                .withEntry(new SchemaImpl.EntryImpl.BuilderImpl()
                        .withName("decimal")
                        .withNullable(true)
                        .withType(Schema.Type.DECIMAL)
                        .build())
                .build();
        final RecordImpl.BuilderImpl builder = new RecordImpl.BuilderImpl(schema);
        final Record record = builder.withDecimal("decimal", new BigDecimal("123456789.123456789")).build();
        assertEquals(new BigDecimal("123456789.123456789"), record.getDecimal("decimal"));
        assertEquals(new BigDecimal("123456789.123456789"), record.get(Object.class, "decimal"));
        assertEquals("123456789.123456789", record.getString("decimal"));
    }

    @AllArgsConstructor
    static class NonSerObject {

        // jsonb.tojson will miss this info as JsonbTransient
        @JsonbTransient
        private String content;

        private boolean allowAccessSecretInfo;

        private String secretInfo;

        public String getContent() {
            return content;
        }

        // jsonb.tojson default depend on get method, this not work
        public String getSecretInfo() {
            if (allowAccessSecretInfo) {
                return secretInfo;
            }
            throw new RuntimeException("this is a secret info, don't allow access");
        }

    }

    @Test
    void object() {
        // jsonb can't process it and also not java Serializable
        NonSerObject value = new NonSerObject("the content", false, "secret info");

        final Schema schema = new SchemaImpl.BuilderImpl()
                .withType(Schema.Type.RECORD)
                .withEntry(new SchemaImpl.EntryImpl.BuilderImpl()
                        .withName("value")
                        .withNullable(true)
                        .withType(Type.STRING)
                        .withProp(SchemaProperty.STUDIO_TYPE, "id_Object")
                        .build())
                .build();
        final RecordImpl.BuilderImpl builder = new RecordImpl.BuilderImpl(schema);
        final Record record = builder.with(schema.getEntry("value"), value).build();
        assertTrue(value == record.get(Object.class, "value"));
    }

    @Test
    void array() {
        final Schema schemaArray = new SchemaImpl.BuilderImpl().withType(Schema.Type.STRING).build();
        final Schema.Entry entry = new SchemaImpl.EntryImpl.BuilderImpl()
                .withName("data")
                .withNullable(false)
                .withType(Schema.Type.ARRAY)
                .withElementSchema(schemaArray)
                .build();
        final Schema schema = new SchemaImpl.BuilderImpl().withType(Schema.Type.RECORD).withEntry(entry).build();
        final RecordImpl.BuilderImpl builder = new RecordImpl.BuilderImpl(schema);

        final Record record = builder.withArray(entry, Arrays.asList("d1", "d2")).build();
        final Collection<String> data = record.getArray(String.class, "data");
        assertEquals(2, data.size());
    }

    @Test
    void withProps() {
        final LinkedHashMap<String, String> rootProps = new LinkedHashMap<>();
        IntStream.range(0, 10).forEach(i -> rootProps.put("key" + i, "value" + i));
        final LinkedHashMap<String, String> fieldProps = new LinkedHashMap<>();
        fieldProps.put("org.talend.components.metadata.one", "one_1");
        fieldProps.put("org.talend.components.metadata.two", "two_2");
        final Schema schema = new BuilderImpl()
                .withType(Type.RECORD)
                .withProps(rootProps)
                .withEntry(new EntryImpl.BuilderImpl().withName("f01").withType(Type.STRING).build())
                .withEntry(
                        new EntryImpl.BuilderImpl().withName("f02").withType(Type.STRING).withProps(fieldProps).build())
                .build();
        final RecordImpl.BuilderImpl builder = new RecordImpl.BuilderImpl(schema);
        final Record record = builder.withString("f01", "field-one").withString("f02", "field-two").build();
        final Schema rSchema = record.getSchema();
        assertEquals("field-one", record.getString("f01"));
        assertEquals("field-two", record.getString("f02"));
        assertEquals(rootProps, rSchema.getProps());
        assertEquals(0, schema.getEntries().get(0).getProps().size());
        assertEquals(2, schema.getEntries().get(1).getProps().size());
        assertEquals(fieldProps, schema.getEntries().get(1).getProps());
        assertEquals("one_1", schema.getEntries().get(1).getProp("org.talend.components.metadata.one"));
        assertEquals("two_2", schema.getEntries().get(1).getProp("org.talend.components.metadata.two"));
        assertEquals(schema, rSchema);
    }

    @Test
    void withProp() {
        final Schema schema = new BuilderImpl()
                .withType(Type.RECORD)
                .withProp("rootProp1", "rootPropValue1")
                .withProp("rootProp2", "rootPropValue2")
                .withEntry(new EntryImpl.BuilderImpl()
                        .withName("f01")
                        .withType(Type.STRING)
                        .withProp("dqType", "semantic-test1")
                        .build())
                .withEntry(new EntryImpl.BuilderImpl()
                        .withName("f02")
                        .withType(Type.STRING)
                        .withProp("dqType", "semantic-test2")
                        .build())
                .build();
        final RecordImpl.BuilderImpl builder = new RecordImpl.BuilderImpl(schema);
        final Record record = builder.withString("f01", "field-one").withString("f02", "field-two").build();
        final Schema rSchema = record.getSchema();
        assertEquals(schema, rSchema);
        assertEquals("field-one", record.getString("f01"));
        assertEquals("field-two", record.getString("f02"));
        assertEquals(3, rSchema.getProps().size());
        assertEquals("f01,f02", rSchema.getProp("talend.fields.order"));
        assertEquals("rootPropValue1", rSchema.getProp("rootProp1"));
        assertEquals("rootPropValue2", rSchema.getProp("rootProp2"));
        assertEquals(1, rSchema.getEntries().get(0).getProps().size());
        assertEquals("semantic-test1", rSchema.getEntries().get(0).getProp("dqType"));
        assertEquals(1, rSchema.getEntries().get(1).getProps().size());
        assertEquals("semantic-test2", rSchema.getEntries().get(1).getProp("dqType"));
    }

    @Test
    void withEntryProp() {
        final Schema schema = new BuilderImpl()
                .withType(Type.RECORD)
                .withEntry(new EntryImpl.BuilderImpl()
                        .withName("ID")
                        .withRawName("THE ID")
                        .withType(Type.INT)
                        .withProp(SchemaProperty.IS_KEY, "true")
                        .withProp(SchemaProperty.ORIGIN_TYPE, "VARCHAR2")
                        .withProp(SchemaProperty.SIZE, "10")
                        .build())
                .withEntry(new EntryImpl.BuilderImpl()
                        .withName("NAME")
                        .withType(Type.STRING)
                        .withProp(SchemaProperty.ORIGIN_TYPE, "VARCHAR2")
                        .withProp(SchemaProperty.SIZE, "64")
                        .build())
                .withEntry(new EntryImpl.BuilderImpl()
                        .withName("PHONE")
                        .withType(Type.STRING)
                        .withProp(SchemaProperty.ORIGIN_TYPE, "VARCHAR2")
                        .withProp(SchemaProperty.SIZE, "64")
                        .withProp(SchemaProperty.IS_UNIQUE, "true")
                        .build())
                .withEntry(new EntryImpl.BuilderImpl()
                        .withName("CREDIT")
                        .withType(Type.DECIMAL)
                        .withProp(SchemaProperty.ORIGIN_TYPE, "DECIMAL")
                        .withProp(SchemaProperty.SIZE, "10")
                        .withProp(SchemaProperty.SCALE, "2")
                        .build())
                .withEntry(new EntryImpl.BuilderImpl()
                        .withName("ADDRESS_ID")
                        .withType(Type.INT)
                        .withProp(SchemaProperty.ORIGIN_TYPE, "INT")
                        .withProp(SchemaProperty.SIZE, "10")
                        .withProp(SchemaProperty.IS_FOREIGN_KEY, "true")
                        .build())
                .build();
        final RecordImpl.BuilderImpl builder = new RecordImpl.BuilderImpl(schema);
        final Record record = builder
                .withInt("ID", 1)
                .withString("NAME", "Wang Wei")
                .withString("PHONE", "18611111111")
                .withDecimal("CREDIT", new BigDecimal("123456789.00"))
                .withInt("ADDRESS_ID", 10101)
                .build();
        final Schema rSchema = record.getSchema();
        assertEquals(schema, rSchema);
        assertEquals("THE ID", rSchema.getEntry("ID").getOriginalFieldName());
        assertEquals("true", rSchema.getEntry("ID").getProp(SchemaProperty.IS_KEY));
        assertEquals("VARCHAR2", rSchema.getEntry("ID").getProp(SchemaProperty.ORIGIN_TYPE));
        assertEquals("10", rSchema.getEntry("ID").getProp(SchemaProperty.SIZE));
        assertNull(rSchema.getEntry("ID").getProp(SchemaProperty.SCALE));

        assertEquals("true", rSchema.getEntry("PHONE").getProp(SchemaProperty.IS_UNIQUE));

        assertEquals("2", rSchema.getEntry("CREDIT").getProp(SchemaProperty.SCALE));

        assertEquals("true", rSchema.getEntry("ADDRESS_ID").getProp(SchemaProperty.IS_FOREIGN_KEY));
    }

    @Test
    void withPropsMerging() {
        final LinkedHashMap<String, String> rootProps = new LinkedHashMap<>();
        IntStream.range(0, 10).forEach(i -> rootProps.put("key" + i, "value" + i));
        final LinkedHashMap<String, String> fieldProps = new LinkedHashMap<>();
        fieldProps.put("dqType", "one_1");
        fieldProps.put("org.talend.components.metadata.two", "two_2");
        final Schema schema = new BuilderImpl()
                .withType(Type.RECORD)
                .withProp("key9", "rootPropValue9")
                .withProps(rootProps)
                .withProp("key1", "rootPropValue1")
                .withProp("key2", "rootPropValue2")
                .withProp("rootProp2", "rootPropValue2")
                .withEntry(new EntryImpl.BuilderImpl()
                        .withName("f01")
                        .withType(Type.STRING)
                        .withProp("dqType", "semantic-test1")
                        .withProps(fieldProps)
                        .build())
                .withEntry(new EntryImpl.BuilderImpl()
                        .withName("f02")
                        .withType(Type.STRING)
                        .withProps(fieldProps)
                        .withProp("dqType", "semantic-test2")
                        .build())
                .build();
        final RecordImpl.BuilderImpl builder = new RecordImpl.BuilderImpl(schema);
        final Record record = builder.withString("f01", "field-one").withString("f02", "field-two").build();
        final Schema rSchema = record.getSchema();
        assertEquals(schema, rSchema);
        assertEquals("field-one", record.getString("f01"));
        assertEquals("field-two", record.getString("f02"));
        assertEquals(12, rSchema.getProps().size());
        assertEquals("f01,f02", rSchema.getProp("talend.fields.order"));
        assertEquals("rootPropValue1", rSchema.getProp("key1"));
        assertEquals("rootPropValue2", rSchema.getProp("key2"));
        assertEquals("value3", rSchema.getProp("key3"));
        assertEquals("value9", rSchema.getProp("key9"));
        assertEquals("rootPropValue2", rSchema.getProp("rootProp2"));
        assertEquals(2, rSchema.getEntries().get(0).getProps().size());
        assertEquals("one_1", rSchema.getEntries().get(0).getProp("dqType"));
        assertEquals("two_2", rSchema.getEntries().get(0).getProp("org.talend.components.metadata.two"));
        assertEquals(2, rSchema.getEntries().get(1).getProps().size());
        assertEquals("semantic-test2", rSchema.getEntries().get(1).getProp("dqType"));
        assertEquals("two_2", rSchema.getEntries().get(1).getProp("org.talend.components.metadata.two"));
    }

    @Test
    void entries() {
        final RecordImpl.BuilderImpl builder = new RecordImpl.BuilderImpl();
        builder.withString("field1", "Hello").withInt("fieldInt", 20);
        final List<Entry> entries = builder.getCurrentEntries();
        Assertions.assertEquals(2, entries.size());
        final Entry entry = entries.stream().filter((Entry e) -> "field1".equals(e.getName())).findFirst().get();
        Assertions.assertSame(Schema.Type.STRING, entry.getType());

        final Entry entry1 = entries.stream().filter((Entry e) -> "fieldInt".equals(e.getName())).findFirst().get();
        Assertions.assertSame(Schema.Type.INT, entry1.getType());

        final Schema schema = new BuilderImpl() //
                .withType(Type.RECORD) //
                .withEntry(new EntryImpl.BuilderImpl()
                        .withName("field1")
                        .withRawName("field1")
                        .withType(Type.INT)
                        .withNullable(true)
                        .withDefaultValue(5)
                        .withComment("Comment")
                        .build())
                .build();
        final RecordImpl.BuilderImpl builder1 = new RecordImpl.BuilderImpl(schema);
        final List<Entry> entries1 = builder1.getCurrentEntries();
        Assertions.assertEquals(1, entries1.size());
    }

    @Test
    void removeEntries() {
        final RecordImpl.BuilderImpl builder = new RecordImpl.BuilderImpl();
        builder.withString("field1", "Hello").withInt("fieldInt", 20);
        final List<Entry> entries = builder.getCurrentEntries();
        Assertions.assertEquals(2, entries.size());

        final Entry entry = entries.stream().filter((Entry e) -> "field1".equals(e.getName())).findFirst().get();
        builder.removeEntry(entry);
        Assertions.assertEquals(1, builder.getCurrentEntries().size());
        assertTrue(entries.stream().anyMatch((Entry e) -> "fieldInt".equals(e.getName())));

        Schema.Entry unknownEntry = newEntry("fieldUnknown", "fieldUnknown", Type.STRING, true, "unknown", "Comment");
        assertThrows(IllegalArgumentException.class, () -> builder.removeEntry(unknownEntry));

        final Schema schema = new BuilderImpl() //
                .withType(Type.RECORD) //
                .withEntry(newEntry("field1", "field1", Type.INT, true, 5, "Comment"))
                .withEntry(newMetaEntry("meta1", "meta1", Type.INT, true, 5, "Comment"))
                .build();
        final RecordImpl.BuilderImpl builder1 = new RecordImpl.BuilderImpl(schema);
        final List<Entry> entries1 = builder1.getCurrentEntries();
        Assertions.assertEquals(2, entries1.size());
        final Entry entry1 = entries1.stream().filter((Entry e) -> "field1".equals(e.getName())).findFirst().get();
        Record.Builder newBuilder = builder1.removeEntry(entry1);
        final Entry meta1 = entries1.stream().filter((Entry e) -> "meta1".equals(e.getName())).findFirst().get();
        Record.Builder newBuilder2 = newBuilder.removeEntry(meta1);
        Assertions.assertEquals(0, newBuilder2.getCurrentEntries().size());
    }

    @Test
    void updateEntryByName_fromEntries() {
        final RecordImpl.BuilderImpl builder = new RecordImpl.BuilderImpl();
        builder.withString("field1", "Hello").withInt("fieldInt", 20);
        final List<Entry> entries = builder.getCurrentEntries();
        Assertions.assertEquals(2, entries.size());

        final Entry entry = newEntry("field2", "newFieldName", Type.STRING, true, 5, "Comment");
        builder.updateEntryByName("field1", entry);
        Assertions.assertEquals(2, builder.getCurrentEntries().size());
        assertTrue(builder.getCurrentEntries()
                .stream()
                .anyMatch((Entry e) -> "field2".equals(e.getName()) && "newFieldName".equals(e.getRawName())));
        assertEquals("Hello", builder.getValue("field2"));

        final Entry entryTypeNotCompatible = newEntry("field3", "newFieldName", Type.INT, true, 5, "Comment");
        assertThrows(IllegalArgumentException.class, () -> builder.updateEntryByName("field2", entryTypeNotCompatible));

        Schema.Entry unknownEntry = newEntry("fieldUnknown", "fieldUnknown", Type.STRING, true, "unknown", "Comment");
        assertThrows(IllegalArgumentException.class, () -> builder.updateEntryByName("fieldUnknown", unknownEntry));
    }

    @Test
    void updateEntryByName_fromProvidedSchema() {
        final Schema schema = new BuilderImpl() //
                .withType(Type.RECORD) //
                .withEntry(newEntry("field1", "field1", Type.STRING, true, 5, "Comment"))
                .build();
        final RecordImpl.BuilderImpl builder1 = new RecordImpl.BuilderImpl(schema);
        builder1.with(schema.getEntry("field1"), "10");
        final List<Entry> entries1 = builder1.getCurrentEntries();
        Assertions.assertEquals(1, entries1.size());
        final Entry entry1 = newEntry("field2", "newFieldName", Type.STRING, true, 5, "Comment");
        Record.Builder newBuilder = builder1.updateEntryByName("field1", entry1);
        Assertions.assertEquals(1, newBuilder.getCurrentEntries().size());
        assertTrue(newBuilder
                .getCurrentEntries()
                .stream()
                .anyMatch((Entry e) -> "field2".equals(e.getName()) && "newFieldName".equals(e.getRawName())
                        && Type.STRING.equals(e.getType())));
        assertEquals("10", newBuilder.getValue("field2"));
    }

    @Test
    void updateEntryByName_fromEntries_withTypeConversion() {
        final RecordImpl.BuilderImpl builder = new RecordImpl.BuilderImpl();
        builder.withString("field1", "10").withInt("fieldInt", 20);
        final List<Entry> entries = builder.getCurrentEntries();
        Assertions.assertEquals(2, entries.size());

        final Entry entry = newEntry("field2", "newFieldName", Type.INT, true, 5, "Comment");
        builder.updateEntryByName("field1", entry, value -> Integer.valueOf(value.toString()));
        Assertions.assertEquals(2, builder.getCurrentEntries().size());
        assertTrue(builder.getCurrentEntries()
                .stream()
                .anyMatch((Entry e) -> "field2".equals(e.getName()) && "newFieldName".equals(e.getRawName())));
        assertEquals(10, builder.getValue("field2"));
    }

    @Test
    void updateEntryByName_fromEntries_withTypeConversion_NotCompatible() {
        final RecordImpl.BuilderImpl builder = new RecordImpl.BuilderImpl();
        builder.withString("field1", "abc");
        final List<Entry> entries = builder.getCurrentEntries();
        Assertions.assertEquals(1, entries.size());

        final Entry entryTypeNotCompatible = newEntry("field2", "newFieldName", Type.INT, true, 5, "Comment");
        assertThrows(IllegalArgumentException.class, () -> builder.updateEntryByName("field1", entryTypeNotCompatible,
                value -> Integer.valueOf(value.toString())));
    }

    @Test
    void updateEntryByName_fromProvidedSchema_withTypeConversion() {
        final Schema schema = new BuilderImpl() //
                .withType(Type.RECORD) //
                .withEntry(newEntry("field1", "field1", Type.STRING, true, 5, "Comment"))
                .build();
        final RecordImpl.BuilderImpl builder1 = new RecordImpl.BuilderImpl(schema);
        builder1.with(schema.getEntry("field1"), "10");
        final List<Entry> entries1 = builder1.getCurrentEntries();
        Assertions.assertEquals(1, entries1.size());
        final Entry entry1 = newEntry("field2", "newFieldName", Type.INT, true, 5, "Comment");
        Record.Builder newBuilder =
                builder1.updateEntryByName("field1", entry1, value -> Integer.valueOf(value.toString()));
        Assertions.assertEquals(1, newBuilder.getCurrentEntries().size());
        assertTrue(newBuilder
                .getCurrentEntries()
                .stream()
                .anyMatch((Entry e) -> "field2".equals(e.getName()) && "newFieldName".equals(e.getRawName())
                        && Type.INT.equals(e.getType())));
        assertEquals(10, newBuilder.getValue("field2"));
    }

    @Test
    void updateEntryByName_preservesOrder() {
        // Given
        final Record.Builder builder = new RecordImpl.BuilderImpl()
                .withString("firstColumn", "hello")
                .withInt("secondColumn", 20)
                .withString("thirdColumn", "foo");

        // When
        final Entry renamedEntry = newEntry("firstColumn_renamed", Type.STRING);
        builder.updateEntryByName("firstColumn", renamedEntry);

        // Then order is preserved in the builder
        Assertions.assertEquals(3, builder.getCurrentEntries().size());
        final List<String> builderEntriesName =
                builder.getCurrentEntries().stream().map(Entry::getName).collect(Collectors.toList());
        assertEquals(Arrays.asList("firstColumn_renamed", "secondColumn", "thirdColumn"), builderEntriesName);

        // Then order is also preserved in the built Record
        final Record outputRecord = builder.build();
        final Schema outputRecordSchema = outputRecord.getSchema();
        final List<String> outputEntriesName =
                outputRecordSchema.getEntriesOrdered().stream().map(Schema.Entry::getName).collect(Collectors.toList());

        assertEquals(Arrays.asList("firstColumn_renamed", "secondColumn", "thirdColumn"), outputEntriesName);
    }

    @Test
    void withRecordFromEntryWithNullValue() {
        final Entry recordEntry = new EntryImpl.BuilderImpl()
                .withType(Type.RECORD)
                .withName("record")
                .withNullable(true)
                .withElementSchema(new SchemaImpl.BuilderImpl()
                        .withType(Schema.Type.RECORD)
                        .withEntry(new SchemaImpl.EntryImpl.BuilderImpl()
                                .withName("name")
                                .withNullable(true)
                                .withType(Schema.Type.STRING)
                                .build())
                        .build())
                .build();
        Record r = new RecordImpl.BuilderImpl().withRecord(recordEntry, null).build();
        assertNotNull(r.getSchema().getEntry("record").getElementSchema());
        assertNull(r.getRecord("record"));
    }

    @Test
    void withRecordFromNameWithNullValue() {
        assertThrows(IllegalArgumentException.class, () -> new RecordImpl.BuilderImpl().withRecord("record", null));
    }

    @Test
    void collisionWithSameNameAndType() {
        // Case with collision without sanitize.
        final Record record = new RecordImpl.BuilderImpl()
                .withString("goodName", "v1")
                .withString("goodName", "v2")
                .build();
        Assertions.assertEquals(1, record.getSchema().getEntries().size());
        Assertions.assertEquals("v2", record.getString("goodName"));
    }

    @Test
    void simpleCollision() {
        // Case with collision and sanitize.
        final Record recordSanitize = new RecordImpl.BuilderImpl()
                .withString("70歳以上", "value70")
                .withString("60歳以上", "value60")
                .build();
        Assertions.assertEquals(2, recordSanitize.getSchema().getEntries().size());

        // both names are sanitized to the one name, but with replacement mechanism inside and prefixes the ordering
        // will be changed
        // last entered will take the simpler name
        final String name1 = SchemaCompanionUtil.sanitizeName("70歳以上");
        Assertions.assertEquals("value60", recordSanitize.getString(name1));
        Assertions.assertEquals("value70", recordSanitize.getString(name1 + "_1"));
    }

    @Test
    void testUsedSameEntry() {
        final RecordImpl.BuilderImpl builder = new RecordImpl.BuilderImpl();
        final Schema.Entry e1 = new EntryImpl.BuilderImpl().withName("_0000").withType(Type.STRING).build();

        builder.withString(e1, "value1");
        final Schema.Entry e2 =
                new EntryImpl.BuilderImpl().withName("_0001").withRawName("_0000_number").withType(Type.STRING).build();
        builder.withString(e2, "value2");
        builder.withString(e2, "value3");
        final Record record = builder.build();
        assertNotNull(record);

        Assertions.assertEquals("value3", record.getString("_0001"));
        Assertions.assertEquals(2, record.getSchema().getEntries().size());
    }

    @Test
    void recordWithNewSchema() {
        final Schema schema0 = new BuilderImpl() //
                .withType(Type.RECORD) //
                .withEntry(dataEntry1) //
                .withEntryBefore("data1", meta1) //
                .withEntry(dataEntry2) //
                .withEntryAfter("meta1", meta2) //
                .build();
        final RecordImpl.BuilderImpl builder0 = new RecordImpl.BuilderImpl(schema0);
        builder0.withInt("data1", 101)
                .withString("data2", "102")
                .withInt("meta1", 103)
                .withString("meta2", "104");
        final Record record0 = builder0.build();
        assertEquals(101, record0.getInt("data1"));
        assertEquals("102", record0.getString("data2"));
        assertEquals(103, record0.getInt("meta1"));
        assertEquals("104", record0.getString("meta2"));
        assertEquals("meta1,meta2,data1,data2", getSchemaFields(record0.getSchema()));
        assertEquals("103,104,101,102", getRecordValues(record0));
        // get a new schema from record
        final Schema schema1 = record0
                .getSchema() //
                .toBuilder() //
                .withEntryBefore("data1", newMetaEntry("meta3", Type.STRING)) //
                .withEntryAfter("meta3", newEntry("data3", Type.STRING)) //
                .build();
        assertEquals("meta1,meta2,meta3,data3,data1,data2", getSchemaFields(schema1));
        // test new record1
        final Record record1 = record0 //
                .withNewSchema(schema1) //
                .withString("data3", "data3") //
                .withString("meta3", "meta3") //
                .build();
        assertEquals(101, record1.getInt("data1"));
        assertEquals("102", record1.getString("data2"));
        assertEquals(103, record1.getInt("meta1"));
        assertEquals("104", record1.getString("meta2"));
        assertEquals("data3", record1.getString("data3"));
        assertEquals("meta3", record1.getString("meta3"));
        assertEquals("meta1,meta2,meta3,data3,data1,data2", getSchemaFields(record1.getSchema()));
        assertEquals("103,104,meta3,data3,101,102", getRecordValues(record1));
        // remove latest additions
        final Schema schema2 = record1
                .getSchema()
                .toBuilder()
                .withEntryBefore("data1", newEntry("data0", Type.STRING))
                .withEntryBefore("meta1", newEntry("meta0", Type.STRING))
                .remove("data3")
                .remove("meta3")
                .build();
        assertEquals("meta0,meta1,meta2,data0,data1,data2", getSchemaFields(schema2));
        final Record record2 = record1 //
                .withNewSchema(schema2) //
                .withString("data0", "data0") //
                .withString("meta0", "meta0") //
                .build();
        assertEquals("meta0,103,104,data0,101,102", getRecordValues(record2));
    }

    @Test
    void recordAfterBefore() {
        final Record record = new RecordImpl.BuilderImpl()
                .withString("_10", "10")
                .withString("_20", "20")
                .withString("_30", "30")
                .withString("_40", "40")
                .withString("_50", "50")
                .before("_10")
                .withString("_00", "0")
                .after("_20")
                .withString("_25", "25")
                .after("_50")
                .withString("_55", "55")
                .before("_55")
                .withString("_53", "53")
                .build();
        assertEquals("_00,_10,_20,_25,_30,_40,_50,_53,_55", getSchemaFields(record.getSchema()));
        assertEquals("_00,_10,_20,_25,_30,_40,_50,_53,_55", record.getSchema().naturalOrder().toFields());
        assertEquals("0,10,20,25,30,40,50,53,55", getRecordValues(record));
    }

    @Test
    void recordOrderingWithProvidedSchema() {
        final Schema schema = new RecordImpl.BuilderImpl()
                .withString("_10", "10")
                .withString("_20", "20")
                .withString("_30", "30")
                .withString("_40", "40")
                .withString("_50", "50")
                .withString("_00", "0")
                .withString("_25", "25")
                .withString("_55", "55")
                .withString("_53", "53")
                .build()
                .getSchema();
        final EntriesOrder order = schema.naturalOrder()
                .moveBefore("_10", "_00")
                .moveAfter("_20", "_25")
                .swap("_53", "_55");
        assertEquals("_00,_10,_20,_25,_30,_40,_50,_53,_55", order.toFields());
        final Record record = new RecordImpl.BuilderImpl(schema.toBuilder().build(order))
                .withString("_10", "10")
                .withString("_20", "20")
                .withString("_30", "30")
                .withString("_40", "40")
                .withString("_50", "50")
                .withString("_00", "0")
                .withString("_25", "25")
                .withString("_55", "55")
                .before("_30")
                .withString("_53", "53")
                .build();
        assertTrue(RecordImpl.class.isInstance(record));
        assertEquals("_00,_10,_20,_25,_53,_30,_40,_50,_55", getSchemaFields(record.getSchema()));
        assertEquals("_00,_10,_20,_25,_53,_30,_40,_50,_55", record.getSchema().naturalOrder().toFields());
        assertEquals("0,10,20,25,53,30,40,50,55", getRecordValues(record));
        final Schema newSchema = record
                .getSchema()
                .toBuilder()
                .remove("_00")
                .remove("_10")
                .remove("_20")
                .withEntry(newEntry("_60", Type.INT))
                .withEntry(newEntry("_56", Type.INT))
                .build();
        assertEquals("_25,_53,_30,_40,_50,_55,_60,_56", getSchemaFields(newSchema));
        assertEquals("_25,_53,_30,_40,_50,_55,_60,_56", newSchema.naturalOrder().toFields());
        // provide an order w/ obsolete/missing entries
        final List<String> newOrder = record.getSchema().naturalOrder().getFieldsOrder().collect(Collectors.toList());
        Collections.sort(newOrder);
        Collections.reverse(newOrder);
        assertEquals("_55,_53,_50,_40,_30,_25,_20,_10,_00", newOrder.stream().collect(joining(",")));
        //
        final Schema newSchemaBis = newSchema.toBuilder().build(EntriesOrder.of(newOrder));
        assertEquals("_55,_53,_50,_40,_30,_25,_60,_56", getSchemaFields(newSchemaBis));
        assertEquals("_55,_53,_50,_40,_30,_25,_60,_56", newSchemaBis.naturalOrder().toFields());
        //
        final Record newRecord = record.withNewSchema(newSchemaBis)
                .after("_40")
                .withInt("_60", 60)
                .before("_60")
                .withInt("_56", 56)
                .build();
        assertEquals("_55,_53,_50,_40,_56,_60,_30,_25", getSchemaFields(newRecord.getSchema()));
        assertEquals("_55,_53,_50,_40,_56,_60,_30,_25", newRecord.getSchema().naturalOrder().toFields());
        assertEquals("55,53,50,40,56,60,30,25", getRecordValues(newRecord));
    }

    @Test
    void recordAfterBeforeFail() {
        assertThrows(IllegalArgumentException.class, () -> new RecordImpl.BuilderImpl()
                .withString("_10", "10")
                .before("_50")
                .withString("_45", "45")
                .build());
        assertThrows(IllegalArgumentException.class, () -> new RecordImpl.BuilderImpl()
                .withString("_10", "10")
                .after("_50")
                .withString("_55", "55")
                .build());
    }

    private String getSchemaFields(final Schema schema) {
        return schema.getEntriesOrdered().stream().map(e -> e.getName()).collect(joining(","));
    }

    private String getRecordValues(final Record record) {
        return record
                .getSchema()
                .getEntriesOrdered()
                .stream()
                .map(e -> record.get(String.class, e.getName()))
                .collect(joining(","));
    }

    private final Schema.Entry dataEntry1 = new SchemaImpl.EntryImpl.BuilderImpl() //
            .withName("data1") //
            .withType(Schema.Type.INT) //
            .build();

    private final Schema.Entry dataEntry2 = new SchemaImpl.EntryImpl.BuilderImpl() //
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

    private Entry newEntry(final String name, String rawname, Schema.Type type, boolean nullable, Object defaultValue,
            String comment) {
        return new EntryImpl.BuilderImpl()
                .withName(name)
                .withRawName(rawname)
                .withType(type)
                .withNullable(nullable)
                .withDefaultValue(defaultValue)
                .withComment(comment)
                .build();
    }

    private Entry newEntry(final String name, Schema.Type type) {
        return newEntry(name, name, type, true, "", "");
    }

    private Entry newMetaEntry(final String name, String rawname, Schema.Type type, boolean nullable,
            Object defaultValue, String comment) {
        return new EntryImpl.BuilderImpl()
                .withName(name)
                .withRawName(rawname)
                .withType(type)
                .withNullable(nullable)
                .withDefaultValue(defaultValue)
                .withComment(comment)
                .withMetadata(true)
                .build();
    }

    private Entry newMetaEntry(final String name, Schema.Type type) {
        return newMetaEntry(name, name, type, true, "", "");
    }

}
