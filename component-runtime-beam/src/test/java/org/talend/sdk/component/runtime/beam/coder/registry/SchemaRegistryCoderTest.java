/**
 * Copyright (C) 2006-2020 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.runtime.beam.coder.registry;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;
import org.talend.sdk.component.runtime.beam.spi.AvroRecordBuilderFactoryProvider;
import org.talend.sdk.component.runtime.beam.spi.record.AvroRecord;
import org.talend.sdk.component.runtime.beam.spi.record.SchemaIdGenerator;
import org.talend.sdk.component.runtime.manager.service.api.Unwrappable;
import org.talend.sdk.component.runtime.record.RecordConverters;
import org.talend.sdk.component.runtime.record.RecordImpl;
import org.talend.sdk.component.runtime.record.SchemaImpl;

class SchemaRegistryCoderTest {

    @Test
    void changingSchema() throws Exception {
        try (final Jsonb jsonb = JsonbBuilder.create()) {
            final RecordBuilderFactory factory = new AvroRecordBuilderFactoryProvider().apply("test");
            final RecordConverters converters = new RecordConverters();
            final JsonBuilderFactory builderFactory = Json.createBuilderFactory(emptyMap());
            final Record record1 = converters
                    .toRecord(new RecordConverters.MappingMetaRegistry(),
                            builderFactory.createObjectBuilder().add("value", "firstSchemaUsesAString").build(),
                            () -> jsonb, () -> factory);
            // go through the encoder with a schema which will change
            final ByteArrayOutputStream buffer1 = new ByteArrayOutputStream();
            SchemaRegistryCoder.of().encode(record1, buffer1);

            final Record decoded1 = SchemaRegistryCoder.of().decode(new ByteArrayInputStream(buffer1.toByteArray()));
            assertEquals("firstSchemaUsesAString", decoded1.getString("value"));

            final Record record2 = converters
                    .toRecord(new RecordConverters.MappingMetaRegistry(),
                            builderFactory
                                    .createObjectBuilder()
                                    .add("value",
                                            Json
                                                    .createArrayBuilder()
                                                    .add(Json.createValue("a"))
                                                    .add(Json.createValue("b"))
                                                    .build())
                                    .build(),
                            () -> jsonb, () -> factory);
            final ByteArrayOutputStream buffer2 = new ByteArrayOutputStream();
            SchemaRegistryCoder.of().encode(record2, buffer2);

            final Record decoded2 = SchemaRegistryCoder.of().decode(new ByteArrayInputStream(buffer2.toByteArray()));
            assertEquals(asList("a", "b"), decoded2.getArray(String.class, "value"));
        }
    }

    @Test
    void avoidNPE() {
        final RecordBuilderFactory factory = new AvroRecordBuilderFactoryProvider().apply("test");
        final Schema.Entry entry = factory
                .newEntryBuilder()
                .withName("createdBy")
                .withType(Schema.Type.RECORD)
                .withElementSchema(factory.newSchemaBuilder(Schema.Type.STRING).build())
                .build();
        final Schema schema = factory.newSchemaBuilder(Schema.Type.RECORD).withEntry(entry).build();

        final org.apache.avro.Schema unwrapped = Unwrappable.class.cast(schema).unwrap(org.apache.avro.Schema.class);
        final String name = SchemaIdGenerator.generateRecordName(unwrapped.getFields());
        assertEquals("org.talend.sdk.component.schema.generated.Record_1_n_5166783486129187498", name);
    }

    @Test
    void codecString() throws IOException {
        final Record record = new AvroRecord(new RecordImpl.BuilderImpl().withString("test", "data").build());

        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        SchemaRegistryCoder.of().encode(record, buffer);

        final Record decoded = SchemaRegistryCoder.of().decode(new ByteArrayInputStream(buffer.toByteArray()));
        assertEquals("data", decoded.getString("test"));
    }

    @Test
    void codecArrayRecord() throws IOException {
        final AvroRecord nestedRecord = new AvroRecord(new RecordImpl.BuilderImpl().withDouble("len", 2D).build());
        final Record record = new AvroRecord(new RecordImpl.BuilderImpl()
                .withArray(new SchemaImpl.EntryImpl("__default__", "__default__", Schema.Type.ARRAY, true, null,
                        nestedRecord.getSchema(), null), singletonList(nestedRecord))
                .build());

        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        SchemaRegistryCoder.of().encode(record, buffer);

        final Record decoded = SchemaRegistryCoder.of().decode(new ByteArrayInputStream(buffer.toByteArray()));
        final double actual = decoded.getArray(Record.class, "__default__").iterator().next().getDouble("len");
        assertEquals(2., actual);
    }
}
