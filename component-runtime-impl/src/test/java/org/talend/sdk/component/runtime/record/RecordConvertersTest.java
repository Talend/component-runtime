/**
 * Copyright (C) 2006-2019 Talend Inc. - www.talend.com
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

import static java.time.format.DateTimeFormatter.ISO_ZONED_DATE_TIME;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.Collection;
import java.util.Date;

import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;
import javax.json.bind.config.BinaryDataStrategy;
import javax.json.bind.config.PropertyOrderStrategy;
import javax.json.spi.JsonProvider;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.record.Schema;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import routines.system.IPersistableRow;

class RecordConvertersTest {

    private final RecordConverters converter = new RecordConverters();

    private final JsonProvider jsonProvider = JsonProvider.provider();

    private final JsonBuilderFactory jsonBuilderFactory = Json.createBuilderFactory(emptyMap());

    private final RecordBuilderFactoryImpl recordBuilderFactory = new RecordBuilderFactoryImpl("test");

    @Test
    void studioTypes() throws Exception {
        final SimpleRowStruct record = new SimpleRowStruct();
        record.character = 'a';
        record.character2 = 'a';
        record.notLong = 100;
        record.notLong2 = 100;
        record.binary = 100;
        record.binary2 = 100;
        record.bd = BigDecimal.TEN;
        record.today = new Date(0);
        try (final Jsonb jsonb = JsonbBuilder
                .create(new JsonbConfig().withPropertyOrderStrategy(PropertyOrderStrategy.LEXICOGRAPHICAL))) {
            final Record recordModel = Record.class
                    .cast(converter
                            .toType(new RecordConverters.MappingMetaRegistry(), record, Record.class,
                                    () -> jsonBuilderFactory, () -> jsonProvider, () -> jsonb,
                                    () -> recordBuilderFactory));
            assertEquals(
                    "{\"bd\":10.0,\"binary\":100.0,\"binary2\":100.0," + "\"character\":\"a\",\"character2\":\"a\","
                            + "\"notLong\":100.0,\"notLong2\":100.0," + "\"today\":\"1970-01-01T00:00:00Z[UTC]\"}",
                    recordModel.toString());
            final SimpleRowStruct deserialized = SimpleRowStruct.class
                    .cast(converter
                            .toType(new RecordConverters.MappingMetaRegistry(), recordModel, SimpleRowStruct.class,
                                    () -> jsonBuilderFactory, () -> jsonProvider, () -> jsonb,
                                    () -> recordBuilderFactory));
            if (record.bd.doubleValue() == deserialized.bd.doubleValue()) { // equals fails on this one
                deserialized.bd = record.bd;
            }
            assertEquals(record, deserialized);
        }
    }

    @Test
    void studioTypes2() throws Exception {
        final RowStruct rowStruct = new RowStruct();
        rowStruct.col1int = 10;
        rowStruct.col2string = "stringy";
        rowStruct.col3char = 'a';
        rowStruct.col4bool = Boolean.TRUE;
        rowStruct.col5byte = 100;
        rowStruct.col6short = Short.MAX_VALUE;
        rowStruct.col7long = 1971L;
        rowStruct.col8float = 19.71f;
        rowStruct.col9double = 19.234;
        rowStruct.col10bigdec = java.math.BigDecimal.TEN;
        rowStruct.col11date = new java.util.Date();
        try (final Jsonb jsonb = JsonbBuilder
                .create(new JsonbConfig().withPropertyOrderStrategy(PropertyOrderStrategy.LEXICOGRAPHICAL))) {
            final Record record = Record.class
                    .cast(converter
                            .toType(new RecordConverters.MappingMetaRegistry(), rowStruct, Record.class,
                                    () -> jsonBuilderFactory, () -> jsonProvider, () -> jsonb,
                                    () -> recordBuilderFactory));
            final RowStruct deserialized = RowStruct.class
                    .cast(converter
                            .toType(new RecordConverters.MappingMetaRegistry(), record, RowStruct.class,
                                    () -> jsonBuilderFactory, () -> jsonProvider, () -> jsonb,
                                    () -> recordBuilderFactory));
            if (rowStruct.col10bigdec.doubleValue() == deserialized.col10bigdec.doubleValue()) {
                deserialized.col10bigdec = rowStruct.col10bigdec;
            }
            if (rowStruct.col11date.equals(deserialized.col11date)) {
                deserialized.col11date = rowStruct.col11date;
            }
            assertEquals(rowStruct, deserialized);
        }
    }

    @Test
    void pojo2Record() throws Exception {
        final Wrapper record = new Wrapper();
        record.value = "hey";
        try (final Jsonb jsonb = JsonbBuilder.create()) {
            final Record json = Record.class
                    .cast(converter
                            .toType(new RecordConverters.MappingMetaRegistry(), record, Record.class,
                                    () -> jsonBuilderFactory, () -> jsonProvider, () -> jsonb,
                                    () -> recordBuilderFactory));
            assertEquals("hey", json.getString("value"));
        }
    }

    @Test
    void nullSupport() throws Exception {
        final Record record = recordBuilderFactory.newRecordBuilder().withString("value", null).build();
        try (final Jsonb jsonb = JsonbBuilder.create()) {
            final JsonObject json = JsonObject.class
                    .cast(converter
                            .toType(new RecordConverters.MappingMetaRegistry(), record, JsonObject.class,
                                    () -> jsonBuilderFactory, () -> jsonProvider, () -> jsonb,
                                    () -> recordBuilderFactory));
            assertNull(json.getJsonString("value"));
        }
    }

    @Test
    void booleanRoundTrip() throws Exception {
        final Record record = recordBuilderFactory.newRecordBuilder().withBoolean("value", true).build();
        try (final Jsonb jsonb = JsonbBuilder.create()) {
            final JsonObject json = JsonObject.class
                    .cast(converter
                            .toType(new RecordConverters.MappingMetaRegistry(), record, JsonObject.class,
                                    () -> jsonBuilderFactory, () -> jsonProvider, () -> jsonb,
                                    () -> recordBuilderFactory));
            assertTrue(json.getBoolean("value"));
            final Record toRecord = converter
                    .toRecord(new RecordConverters.MappingMetaRegistry(), json, () -> jsonb,
                            () -> recordBuilderFactory);
            assertTrue(toRecord.getBoolean("value"));
        }
    }

    @Test
    void intRoundTrip() throws Exception {
        final Record record = recordBuilderFactory.newRecordBuilder().withInt("value", 2).build();
        try (final Jsonb jsonb = JsonbBuilder.create()) {
            final IntStruct struct = IntStruct.class
                    .cast(converter
                            .toType(new RecordConverters.MappingMetaRegistry(), record, IntStruct.class,
                                    () -> jsonBuilderFactory, () -> jsonProvider, () -> jsonb,
                                    () -> recordBuilderFactory));
            final Record toRecord = converter
                    .toRecord(new RecordConverters.MappingMetaRegistry(), struct, () -> jsonb,
                            () -> recordBuilderFactory);
            assertEquals(Schema.Type.INT, toRecord.getSchema().getEntries().iterator().next().getType());
            assertEquals(2, toRecord.getInt("value"));
        }
    }

    @Test
    void booleanRoundTripPojo() throws Exception {
        final Record record = recordBuilderFactory.newRecordBuilder().withBoolean("value", true).build();
        try (final Jsonb jsonb = JsonbBuilder.create()) {
            final BoolStruct struct = BoolStruct.class
                    .cast(converter
                            .toType(new RecordConverters.MappingMetaRegistry(), record, BoolStruct.class,
                                    () -> jsonBuilderFactory, () -> jsonProvider, () -> jsonb,
                                    () -> recordBuilderFactory));
            final Record toRecord = converter
                    .toRecord(new RecordConverters.MappingMetaRegistry(), struct, () -> jsonb,
                            () -> recordBuilderFactory);
            assertEquals(Schema.Type.BOOLEAN, toRecord.getSchema().getEntries().iterator().next().getType());
            assertTrue(toRecord.getBoolean("value"));
        }
    }

    @Test
    void stringRoundTrip() throws Exception {
        final Record record = recordBuilderFactory.newRecordBuilder().withString("value", "yes").build();
        try (final Jsonb jsonb = JsonbBuilder.create()) {
            final JsonObject json = JsonObject.class
                    .cast(converter
                            .toType(new RecordConverters.MappingMetaRegistry(), record, JsonObject.class,
                                    () -> jsonBuilderFactory, () -> jsonProvider, () -> jsonb,
                                    () -> recordBuilderFactory));
            assertEquals("yes", json.getString("value"));
            final Record toRecord = converter
                    .toRecord(new RecordConverters.MappingMetaRegistry(), json, () -> jsonb,
                            () -> recordBuilderFactory);
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
                            .toType(new RecordConverters.MappingMetaRegistry(), record, JsonObject.class,
                                    () -> jsonBuilderFactory, () -> jsonProvider, () -> jsonb,
                                    () -> recordBuilderFactory));
            assertEquals(Base64.getEncoder().encodeToString(bytes), json.getString("value"));
            final Record toRecord = converter
                    .toRecord(new RecordConverters.MappingMetaRegistry(), json, () -> jsonb,
                            () -> recordBuilderFactory);
            assertArrayEquals(bytes, toRecord.getBytes("value"));

            // now studio generator kind of convertion
            final BytesStruct struct = jsonb.fromJson(json.toString(), BytesStruct.class);
            assertArrayEquals(bytes, struct.value);
            final String jsonFromStruct = jsonb.toJson(struct);
            assertEquals("{\"value\":\"AQID\"}", jsonFromStruct);
            final Record structToRecordFromJson = converter
                    .toRecord(new RecordConverters.MappingMetaRegistry(), json, () -> jsonb,
                            () -> recordBuilderFactory);
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
                    .toRecord(new RecordConverters.MappingMetaRegistry(),
                            Json
                                    .createObjectBuilder()
                                    .add("list",
                                            Json
                                                    .createArrayBuilder()
                                                    .add(Json.createValue("a"))
                                                    .add(Json.createValue("b"))
                                                    .build())
                                    .build(),
                            () -> jsonb, () -> new RecordBuilderFactoryImpl("test"));
            final Collection<String> list = record.getArray(String.class, "list");
            assertEquals(asList("a", "b"), list);
        }
    }

    @Test
    void convertListObject() throws Exception {
        try (final Jsonb jsonb = JsonbBuilder.create()) {
            final Record record = converter
                    .toRecord(new RecordConverters.MappingMetaRegistry(),
                            Json
                                    .createObjectBuilder()
                                    .add("list",
                                            Json
                                                    .createArrayBuilder()
                                                    .add(Json.createObjectBuilder().add("name", "a").build())
                                                    .add(Json.createObjectBuilder().add("name", "b").build())
                                                    .build())
                                    .build(),
                            () -> jsonb, () -> new RecordBuilderFactoryImpl("test"));
            final Collection<Record> list = record.getArray(Record.class, "list");
            assertEquals(asList("a", "b"), list.stream().map(it -> it.getString("name")).collect(toList()));
        }
    }

    public static class Wrapper {

        public String value;
    }

    public static class BytesStruct {

        public byte[] value;
    }

    // not @Data, we don't want getters here
    @ToString
    @EqualsAndHashCode
    public static class SimpleRowStruct {

        public char character;

        public Character character2;

        public short notLong;

        public Short notLong2;

        public byte binary;

        public Byte binary2;

        public BigDecimal bd;

        public Date today;
    }

    public static class IntStruct implements IPersistableRow {

        public int value;
    }

    public static class BoolStruct implements IPersistableRow {

        public boolean value;
    }

    @ToString
    @EqualsAndHashCode
    public static class RowStruct implements routines.system.IPersistableRow {

        public Integer col1int;

        public String col2string;

        public Character col3char;

        public Boolean col4bool;

        public Byte col5byte;

        public Short col6short;

        public Long col7long;

        public Float col8float;

        public Double col9double;

        public java.math.BigDecimal col10bigdec;

        public java.util.Date col11date;
    }
}
