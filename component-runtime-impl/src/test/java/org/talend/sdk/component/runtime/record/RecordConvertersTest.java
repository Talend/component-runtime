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
package org.talend.sdk.component.runtime.record;

import static java.time.format.DateTimeFormatter.ISO_ZONED_DATE_TIME;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.Collection;

import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;
import javax.json.bind.config.BinaryDataStrategy;
import javax.json.spi.JsonProvider;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.record.Record;

class RecordConvertersTest {

    private final RecordConverters converter = new RecordConverters();

    private final JsonProvider jsonProvider = JsonProvider.provider();

    private final JsonBuilderFactory jsonBuilderFactory = Json.createBuilderFactory(emptyMap());

    private final RecordBuilderFactoryImpl recordBuilderFactory = new RecordBuilderFactoryImpl("test");

    @Test
    void nullSupport() throws Exception {
        final Record record = recordBuilderFactory.newRecordBuilder().withString("value", null).build();
        try (final Jsonb jsonb = JsonbBuilder.create()) {
            final JsonObject json = JsonObject.class
                    .cast(converter
                            .toType(record, JsonObject.class, () -> jsonBuilderFactory, () -> jsonProvider,
                                    () -> jsonb));
            assertNull(json.getJsonString("value"));
        }
    }

    @Test
    void booleanRoundTrip() throws Exception {
        final Record record = recordBuilderFactory.newRecordBuilder().withBoolean("value", true).build();
        try (final Jsonb jsonb = JsonbBuilder.create()) {
            final JsonObject json = JsonObject.class
                    .cast(converter
                            .toType(record, JsonObject.class, () -> jsonBuilderFactory, () -> jsonProvider,
                                    () -> jsonb));
            assertTrue(json.getBoolean("value"));
            final Record toRecord = converter.toRecord(json, () -> jsonb, () -> recordBuilderFactory);
            assertTrue(toRecord.getBoolean("value"));
        }
    }

    @Test
    void stringRoundTrip() throws Exception {
        final Record record = recordBuilderFactory.newRecordBuilder().withString("value", "yes").build();
        try (final Jsonb jsonb = JsonbBuilder.create()) {
            final JsonObject json = JsonObject.class
                    .cast(converter
                            .toType(record, JsonObject.class, () -> jsonBuilderFactory, () -> jsonProvider,
                                    () -> jsonb));
            assertEquals("yes", json.getString("value"));
            final Record toRecord = converter.toRecord(json, () -> jsonb, () -> recordBuilderFactory);
            assertEquals("yes", toRecord.getString("value"));
        }
    }

    @Test
    void bytesRoundTrip() throws Exception {
        final byte[] bytes = new byte[] { 1, 2, 3 };
        final Record record = recordBuilderFactory.newRecordBuilder().withBytes("value", bytes).build();
        try (final Jsonb jsonb =
                JsonbBuilder.create(new JsonbConfig().withBinaryDataStrategy(BinaryDataStrategy.BASE_64))) {
            final JsonObject json = JsonObject.class
                    .cast(converter
                            .toType(record, JsonObject.class, () -> jsonBuilderFactory, () -> jsonProvider,
                                    () -> jsonb));
            assertEquals(Base64.getEncoder().encodeToString(bytes), json.getString("value"));
            final Record toRecord = converter.toRecord(json, () -> jsonb, () -> recordBuilderFactory);
            assertArrayEquals(bytes, toRecord.getBytes("value"));

            // now studio generator kind of convertion
            final BytesStruct struct = jsonb.fromJson(json.toString(), BytesStruct.class);
            assertArrayEquals(bytes, struct.value);
            final String jsonFromStruct = jsonb.toJson(struct);
            assertEquals("{\"value\":\"AQID\"}", jsonFromStruct);
            final Record structToRecordFromJson = converter.toRecord(json, () -> jsonb, () -> recordBuilderFactory);
            assertArrayEquals(bytes, structToRecordFromJson.getBytes("value"));
        }
    }

    @Test
    void convertDateToString() {
        final ZonedDateTime dateTime = ZonedDateTime.of(2017, 7, 17, 9, 0, 0, 0, ZoneId.of("GMT"));
        final String stringValue = dateTime.format(ISO_ZONED_DATE_TIME);
        new RecordConverters().coerce(ZonedDateTime.class, stringValue, "foo");
        final ZonedDateTime asDate = new RecordConverters().coerce(ZonedDateTime.class, stringValue, "foo");
        assertEquals(dateTime, asDate);
        final String asString = new RecordConverters().coerce(String.class, stringValue, "foo");
        assertEquals(stringValue, asString);
    }

    @Test
    void convertListString() throws Exception {
        try (final Jsonb jsonb = JsonbBuilder.create()) {
            final Record record = converter
                    .toRecord(Json
                            .createObjectBuilder()
                            .add("list",
                                    Json
                                            .createArrayBuilder()
                                            .add(Json.createValue("a"))
                                            .add(Json.createValue("b"))
                                            .build())
                            .build(), () -> jsonb, () -> new RecordBuilderFactoryImpl("test"));
            final Collection<String> list = record.getArray(String.class, "list");
            assertEquals(asList("a", "b"), list);
        }
    }

    @Test
    void convertListObject() throws Exception {
        try (final Jsonb jsonb = JsonbBuilder.create()) {
            final Record record = converter
                    .toRecord(Json
                            .createObjectBuilder()
                            .add("list",
                                    Json
                                            .createArrayBuilder()
                                            .add(Json.createObjectBuilder().add("name", "a").build())
                                            .add(Json.createObjectBuilder().add("name", "b").build())
                                            .build())
                            .build(), () -> jsonb, () -> new RecordBuilderFactoryImpl("test"));
            final Collection<Record> list = record.getArray(Record.class, "list");
            assertEquals(asList("a", "b"), list.stream().map(it -> it.getString("name")).collect(toList()));
        }
    }

    public static class BytesStruct {

        public byte[] value;
    }
}
