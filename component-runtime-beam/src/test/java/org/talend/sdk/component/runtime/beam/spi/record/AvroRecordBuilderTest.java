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

import static java.util.stream.Collectors.joining;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static org.talend.sdk.component.api.record.Schema.Type.ARRAY;
import static org.talend.sdk.component.api.record.Schema.Type.INT;
import static org.talend.sdk.component.api.record.Schema.Type.RECORD;
import static org.talend.sdk.component.api.record.Schema.Type.STRING;
import static org.talend.sdk.component.runtime.record.SchemaImpl.ENTRIES_ORDER_PROP;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;

import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.IndexedRecord;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.io.JsonEncoder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.api.record.Schema.EntriesOrder;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;
import org.talend.sdk.component.runtime.beam.spi.AvroRecordBuilderFactoryProvider;
import org.talend.sdk.component.runtime.record.RecordBuilderFactoryImpl;
import org.talend.sdk.component.runtime.record.SchemaImpl;
import org.talend.sdk.component.runtime.record.SchemaImpl.EntryImpl;

@TestInstance(PER_CLASS)
class AvroRecordBuilderTest {

    private final RecordBuilderFactory factory = new AvroRecordBuilderFactoryProvider().apply(null);

    private final Schema address = factory
            .newSchemaBuilder(RECORD)
            .withEntry(
                    factory.newEntryBuilder().withName("street").withRawName("current street").withType(STRING).build())
            .withEntry(factory.newEntryBuilder().withName("number").withType(INT).build())
            .build();

    private final Schema baseSchema = factory
            .newSchemaBuilder(RECORD)
            .withEntry(factory.newEntryBuilder().withName("name").withRawName("current name").withType(STRING).build())
            .withEntry(factory.newEntryBuilder().withName("age").withType(INT).build())
            .withEntry(
                    factory.newEntryBuilder().withName("@address").withType(RECORD).withElementSchema(address).build())
            .build();

    @Test
    void copySchema() {
        final Schema custom = factory
                .newSchemaBuilder(baseSchema)
                .withEntry(factory.newEntryBuilder().withName("custom").withType(STRING).build())
                .build();
        assertEquals("name/STRING/current name,age/INT/null,address/RECORD/@address,custom/STRING/null",
                custom
                        .getEntries()
                        .stream()
                        .map(it -> it.getName() + '/' + it.getType() + '/' + it.getRawName())
                        .collect(joining(",")));
    }

    @Test
    void copyRecord() {
        final Schema customSchema = factory
                .newSchemaBuilder(baseSchema)
                .withEntry(factory.newEntryBuilder().withName("custom").withType(STRING).build())
                .build();
        final Record baseRecord = factory
                .newRecordBuilder(baseSchema)
                .withString("name", "Test")
                .withInt("age", 33)
                .withRecord("address",
                        factory.newRecordBuilder(address).withString("street", "here").withInt("number", 1).build())
                .build();
        final Record output = factory.newRecordBuilder(customSchema, baseRecord).withString("custom", "added").build();
        assertEquals(
                "AvroRecord{delegate={\"name\": \"Test\", \"age\": 33, \"address\": {\"street\": \"here\", \"number\": 1}, \"custom\": \"added\"}}",
                output.toString());
    }

    @Test
    void avroTest() {
        // customer record schema
        org.talend.sdk.component.api.record.Schema.Builder schemaBuilder = factory.newSchemaBuilder(Schema.Type.RECORD);
        Schema.Entry nameEntry = factory
                .newEntryBuilder()
                .withName("name")
                .withNullable(true)
                .withType(Schema.Type.STRING)
                .build();
        Schema.Entry ageEntry = factory
                .newEntryBuilder()
                .withName("age")
                .withNullable(true)
                .withType(Schema.Type.INT)
                .build();
        Schema customerSchema = schemaBuilder.withEntry(nameEntry).withEntry(ageEntry).build();
        // record 1
        Record.Builder recordBuilder = factory.newRecordBuilder(customerSchema);
        recordBuilder.withString("name", "Tom Cruise");
        recordBuilder.withInt("age", 58);
        Record record1 = recordBuilder.build();
        // record 2
        recordBuilder = factory.newRecordBuilder(customerSchema);
        recordBuilder.withString("name", "Meryl Streep");
        recordBuilder.withInt("age", 63);
        Record record2 = recordBuilder.build();
        // list 1
        Collection<Record> list1 = new ArrayList<>();
        list1.add(record1);
        list1.add(record2);
        // record 3
        recordBuilder = factory.newRecordBuilder(customerSchema);
        recordBuilder.withString("name", "Client Eastwood");
        recordBuilder.withInt("age", 89);
        Record record3 = recordBuilder.build();
        // record 4
        recordBuilder = factory.newRecordBuilder(customerSchema);
        recordBuilder.withString("name", "Jessica Chastain");
        recordBuilder.withInt("age", 36);
        Record record4 = recordBuilder.build();
        // list 2
        Collection<Record> list2 = new ArrayList<>();
        list2.add(record3);
        list2.add(record4);
        // main list
        Collection<Object> list3 = new ArrayList<>();
        list3.add(list1);
        list3.add(list2);
        // schema of sub list
        schemaBuilder = factory.newSchemaBuilder(Schema.Type.ARRAY);
        Schema subListSchema = schemaBuilder.withElementSchema(customerSchema).build();
        // main record
        recordBuilder = factory.newRecordBuilder();
        Schema.Entry entry = factory
                .newEntryBuilder()
                .withName("customers")
                .withNullable(true)
                .withType(Schema.Type.ARRAY)
                .withElementSchema(subListSchema)
                .build();
        recordBuilder.withArray(entry, list3);
        Record record = recordBuilder.build();
        Assertions.assertNotNull(record);

        final Collection<Collection> customers = record.getArray(Collection.class, "customers");

        AtomicInteger counter = new AtomicInteger(0);
        final boolean allMatch = customers
                .stream() //
                .flatMap(Collection::stream) //
                .allMatch((Object rec) -> {
                    counter.incrementAndGet();
                    return rec instanceof Record;
                });
        Assertions.assertTrue(allMatch);
        Assertions.assertEquals(4, counter.get());
    }

    @Test
    void mixedRecordTest() {
        final AvroRecordBuilderFactoryProvider recordBuilderFactoryProvider = new AvroRecordBuilderFactoryProvider();
        System.setProperty("talend.component.beam.record.factory.impl", "avro");
        final RecordBuilderFactory recordBuilderFactory = recordBuilderFactoryProvider.apply("test");

        final RecordBuilderFactory otherFactory = new RecordBuilderFactoryImpl("test");
        final Schema schema = otherFactory
                .newSchemaBuilder(RECORD)
                .withEntry(otherFactory.newEntryBuilder().withName("e1").withType(INT).build())
                .build();

        final Schema arrayType = recordBuilderFactory //
                .newSchemaBuilder(Schema.Type.ARRAY) //
                .withElementSchema(schema)
                .build();
        Assertions.assertNotNull(arrayType);
    }

    @Test
    void recordWithNewSchema() {
        final Schema schema0 = new AvroSchemaBuilder()//
                .withType(RECORD) //
                .withEntry(dataEntry1) //
                .withEntryBefore("data1", meta1) //
                .withEntry(dataEntry2) //
                .withEntryAfter("meta1", meta2) //
                .build();

        final Record.Builder builder0 = factory.newRecordBuilder(schema0);
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
                .withEntryBefore("data1", newMetaEntry("meta3", STRING)) //
                .withEntryAfter("meta3", newEntry("data3", STRING)) //
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
                .withEntryBefore("data1", newEntry("data0", STRING))
                .withEntryBefore("meta1", newEntry("meta0", STRING))
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
    void arrayTest() throws IOException {
        final Schema.Entry f1 = this.factory.newEntryBuilder()
                .withName("f1")
                .withNullable(true)
                .withType(STRING)
                .build();
        final Schema innerSchema = this.factory.newSchemaBuilder(RECORD).withEntry(f1).build();

        final Record record1 = this.factory.newRecordBuilder(innerSchema)
                .withString(f1, "value1")
                .build();
        final Record record2 = this.factory.newRecordBuilder(innerSchema)
                .withString(f1, "value2")
                .build();

        final Schema arraySchema = this.factory.newSchemaBuilder(ARRAY)
                .withElementSchema(innerSchema)
                .build();
        final List<Record> records = Arrays.asList(record1, null, record2);
        final List<List<Record>> metaList = Collections.singletonList(records);

        final Schema.Entry array = this.factory.newEntryBuilder()
                .withElementSchema(arraySchema)
                .withName("doubleArray")
                .withType(ARRAY)
                .build();
        final Schema recordSchema = this.factory.newSchemaBuilder(RECORD).withEntry(array).build();
        final Record record = this.factory.newRecordBuilder(recordSchema)
                .withArray(array, metaList)
                .build();
        Assertions.assertTrue(record instanceof AvroRecord);
        final IndexedRecord avroRecord = ((AvroRecord) record).unwrap(IndexedRecord.class);
        final GenericDatumWriter<IndexedRecord> writer = new GenericDatumWriter<>(avroRecord.getSchema());

        ByteArrayOutputStream outputArray = new ByteArrayOutputStream();
        JsonEncoder encoder = EncoderFactory.get().jsonEncoder(avroRecord.getSchema(), outputArray, true);
        writer.write(avroRecord, encoder);
        encoder.flush();
        String chain = new String(outputArray.toByteArray(), StandardCharsets.UTF_8);

        JsonObject jsonObject = Json.createReader(new StringReader(chain)).readObject();
        JsonArray jsonArray = jsonObject.getJsonArray("doubleArray").get(0).asJsonObject().getJsonArray("array");
        Assertions.assertEquals(3, jsonArray.size());
        Assertions.assertEquals(2, jsonArray.stream().filter(JsonObject.class::isInstance).count());
    }

    @Test
    void recordAfterBefore() {
        final Record record = factory.newRecordBuilder()
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
        assertEquals("0,10,20,25,30,40,50,53,55", getRecordValues(record));
    }

    @Test
    void recordOrderingWithProvidedSchema() {
        final Schema schema = factory.newRecordBuilder()
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
        final Record record = factory.newRecordBuilder(schema.toBuilder().build(order))
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
        assertTrue(AvroRecord.class.isInstance(record));
        assertEquals("_00,_10,_20,_25,_53,_30,_40,_50,_55", getSchemaFields(record.getSchema()));
        assertEquals("_00,_10,_20,_25,_53,_30,_40,_50,_55", record.getSchema().naturalOrder().toFields());
        assertEquals("0,10,20,25,53,30,40,50,55", getRecordValues(record));
        final Schema newSchema = record
                .getSchema()
                .toBuilder()
                .remove("_00")
                .remove("_10")
                .remove("_20")
                .withEntry(newEntry("_60", INT))
                .withEntry(newEntry("_56", INT))
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
        //
        IndexedRecord idx = AvroRecord.class.cast(newRecord).unwrap(IndexedRecord.class);
        assertNotNull(idx);
        assertEquals("_55,_53,_50,_40,_56,_60,_30,_25", idx.getSchema().getProp(ENTRIES_ORDER_PROP));
    }

    @Test
    void recordAfterBeforeFail() {
        assertThrows(IllegalArgumentException.class, () -> factory.newRecordBuilder()
                .withString("_10", "10")
                .before("_50")
                .withString("_45", "45")
                .build());
        assertThrows(IllegalArgumentException.class, () -> factory.newRecordBuilder()
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

    private Schema.Entry newEntry(final String name, Schema.Type type) {
        return newEntry(name, name, type, true, "", "");
    }

    private Schema.Entry newEntry(final String name, String rawname, Schema.Type type, boolean nullable,
            Object defaultValue,
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

    private Schema.Entry newMetaEntry(final String name, Schema.Type type) {
        return newMetaEntry(name, name, type, true, "", "");
    }

    private Schema.Entry newMetaEntry(final String name, String rawname, Schema.Type type, boolean nullable,
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

    private final Schema.Entry dataEntry1 = new SchemaImpl.EntryImpl.BuilderImpl() //
            .withName("data1") //
            .withType(INT) //
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

}
