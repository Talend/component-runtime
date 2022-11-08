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
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static org.talend.sdk.component.api.record.Schema.Type.INT;
import static org.talend.sdk.component.api.record.Schema.Type.RECORD;
import static org.talend.sdk.component.api.record.Schema.Type.STRING;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.avro.generic.IndexedRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;
import org.talend.sdk.component.runtime.beam.spi.AvroRecordBuilderFactoryProvider;
import org.talend.sdk.component.runtime.record.RecordBuilderFactoryImpl;

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
        // get RecordBuilderFactory
        AvroRecordBuilderFactoryProvider recordBuilderFactoryProvider = new AvroRecordBuilderFactoryProvider();
        System.setProperty("talend.component.beam.record.factory.impl", "avro");
        RecordBuilderFactory recordBuilderFactory = recordBuilderFactoryProvider.apply("test");
        // customer record schema
        org.talend.sdk.component.api.record.Schema.Builder schemaBuilder =
                recordBuilderFactory.newSchemaBuilder(Schema.Type.RECORD);
        Schema.Entry nameEntry = recordBuilderFactory
                .newEntryBuilder()
                .withName("name")
                .withNullable(true)
                .withType(Schema.Type.STRING)
                .build();
        Schema.Entry ageEntry = recordBuilderFactory
                .newEntryBuilder()
                .withName("age")
                .withNullable(true)
                .withType(Schema.Type.INT)
                .build();
        Schema customerSchema = schemaBuilder.withEntry(nameEntry).withEntry(ageEntry).build();
        // record 1
        Record.Builder recordBuilder = recordBuilderFactory.newRecordBuilder(customerSchema);
        recordBuilder.withString("name", "Tom Cruise");
        recordBuilder.withInt("age", 58);
        Record record1 = recordBuilder.build();
        // record 2
        recordBuilder = recordBuilderFactory.newRecordBuilder(customerSchema);
        recordBuilder.withString("name", "Meryl Streep");
        recordBuilder.withInt("age", 63);
        Record record2 = recordBuilder.build();
        // list 1
        Collection<Record> list1 = new ArrayList<>();
        list1.add(record1);
        list1.add(record2);
        // record 3
        recordBuilder = recordBuilderFactory.newRecordBuilder(customerSchema);
        recordBuilder.withString("name", "Client Eastwood");
        recordBuilder.withInt("age", 89);
        Record record3 = recordBuilder.build();
        // record 4
        recordBuilder = recordBuilderFactory.newRecordBuilder(customerSchema);
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
        schemaBuilder = recordBuilderFactory.newSchemaBuilder(Schema.Type.ARRAY);
        Schema subListSchema = schemaBuilder.withElementSchema(customerSchema).build();
        // main record
        recordBuilder = recordBuilderFactory.newRecordBuilder();
        Schema.Entry entry = recordBuilderFactory
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
}
