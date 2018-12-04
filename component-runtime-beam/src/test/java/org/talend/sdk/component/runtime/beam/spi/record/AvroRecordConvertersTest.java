/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.talend.sdk.component.runtime.beam.spi.record;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonReaderFactory;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;
import org.talend.sdk.component.runtime.beam.coder.registry.SchemaRegistryCoder;
import org.talend.sdk.component.runtime.beam.spi.AvroRecordBuilderFactoryProvider;
import org.talend.sdk.component.runtime.record.RecordConverters;

class AvroRecordConvertersTest {

    @Test
    void convertListString() throws Exception {
        try (final Jsonb jsonb = JsonbBuilder.create()) {
            final Record record =
                    new RecordConverters()
                            .toRecord(
                                    Json
                                            .createObjectBuilder()
                                            .add("list",
                                                    Json
                                                            .createArrayBuilder()
                                                            .add(Json.createValue("a"))
                                                            .add(Json.createValue("b"))
                                                            .build())
                                            .build(),
                                    () -> jsonb, () -> new AvroRecordBuilderFactoryProvider().apply("test"));
            final Collection<String> list = record.getArray(String.class, "list");
            assertEquals(asList("a", "b"), list);
        }
    }

    @Test
    void avroRecordArrays() throws Exception {
        try (final Jsonb jsonb = JsonbBuilder.create()) {
            final RecordBuilderFactory factory = new AvroRecordBuilderFactoryProvider().apply("test");
            final RecordConverters converters = new RecordConverters();
            final JsonBuilderFactory builderFactory = Json.createBuilderFactory(emptyMap());
            final Record record = converters
                    .toRecord(builderFactory
                            .createObjectBuilder()
                            .add("value",
                                    builderFactory
                                            .createObjectBuilder()
                                            .add("somekey", builderFactory.createArrayBuilder().build()))
                            .build(), () -> jsonb, () -> factory);
            final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            SchemaRegistryCoder.of().encode(record, buffer);
            assertTrue(SchemaRegistryCoder
                    .of()
                    .decode(new ByteArrayInputStream(buffer.toByteArray()))
                    .getRecord("value")
                    .getArray(Object.class, "somekey")
                    .isEmpty());
        }
    }

    @Test
    @Disabled("we only support one schema per array for now")
    void notHomogeneousArray() throws Exception {
        try (final Jsonb jsonb = JsonbBuilder.create()) {
            final RecordBuilderFactory factory = new AvroRecordBuilderFactoryProvider().apply("test");
            final RecordConverters converters = new RecordConverters();
            final JsonReaderFactory readerFactory = Json.createReaderFactory(emptyMap());
            final Record record = converters
                    .toRecord(readerFactory
                            .createReader(new StringReader(
                                    "{\"custom_attributes\":[" + "{\"attribute_code\":\"color\",\"value\":\"49\"},"
                                            + "{\"attribute_code\":\"category_ids\",\"value\":[]}]}"))
                            .readObject(), () -> jsonb, () -> factory);
            final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            SchemaRegistryCoder.of().encode(record, buffer);
            final Record decoded = SchemaRegistryCoder.of().decode(new ByteArrayInputStream(buffer.toByteArray()));
            final List<Record> array = new ArrayList<>(decoded.getArray(Record.class, "custom_attributes"));
            assertTrue(array.get(1).getArray(Object.class, "value2").isEmpty());
        }
    }
}
