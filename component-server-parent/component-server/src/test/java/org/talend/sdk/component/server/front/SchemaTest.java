/**
 * Copyright (C) 2006-2026 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.server.front;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.talend.sdk.component.api.record.Schema.Type.LONG;
import static org.talend.sdk.component.api.record.Schema.Type.STRING;

import java.io.StringReader;
import java.util.List;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.record.SchemaProperty;
import org.talend.sdk.component.api.record.SchemaProperty.LogicalType;
import org.talend.sdk.component.runtime.record.RecordBuilderFactoryImpl;
import org.talend.sdk.component.runtime.record.SchemaImpl;
import org.talend.sdk.component.server.front.model.Entry;
import org.talend.sdk.component.server.front.model.Schema;

class SchemaTest {

    private final Jsonb jsonb = JsonbBuilder.create();

    @Test
    void shouldSerializeSchemaImplAndDeserializeToJsonSchemaModel() {
        // given
        final org.talend.sdk.component.api.record.Schema schema = new SchemaImpl.BuilderImpl() //
                .withType(org.talend.sdk.component.api.record.Schema.Type.RECORD)
                .withEntry(new SchemaImpl.EntryImpl.BuilderImpl()
                        .withName("id")
                        .withType(STRING)
                        .build())
                .withEntry(new SchemaImpl.EntryImpl.BuilderImpl()
                        .withName("field2")
                        .withType(LONG)
                        .withNullable(false)
                        .withComment("field2 comment")
                        .build())
                .withProp("namespace", "test")
                .build();

        // when: serialize SchemaImpl
        String json = jsonb.toJson(schema);

        // then: sanity check JSON
        assertTrue(json.contains("\"type\":\"RECORD\""));
        assertTrue(json.contains("\"entries\""));

        // when: deserialize into Schema
        org.talend.sdk.component.server.front.model.Schema model = jsonb.fromJson(new StringReader(json),
                org.talend.sdk.component.server.front.model.Schema.class);

        // then
        assertEquals(org.talend.sdk.component.server.front.model.Schema.Type.RECORD, model.getType());
        assertEquals("test", model.getProps().get("namespace"));

        assertEquals(2, model.getEntries().size());
        assertEquals("id", model.getEntries().get(0).getName());
        assertEquals(org.talend.sdk.component.server.front.model.Schema.Type.STRING,
                model.getEntries().get(0).getType());
        assertEquals("field2", model.getEntries().get(1).getName());
        assertEquals(org.talend.sdk.component.server.front.model.Schema.Type.LONG, model.getEntries().get(1).getType());
        assertEquals("field2 comment", model.getEntries().get(1).getComment());
        assertFalse(model.getEntries().get(1).isNullable());

    }

    @Test
    void deserializeRecordSchemaWithEntriesAndMetadata() {
        String json =
                """
                {
                  "type": "RECORD",
                  "props": {
                    "p1": "v1"
                  },
                  "entries": [
                    {
                      "name": "id",
                      "rawName": "id",
                      "originalFieldName": "id",
                      "type": "INT",
                      "nullable": false,
                      "metadata": false,
                      "errorCapable": false,
                      "valid": true,
                      "defaultValue": 0,
                      "comment": "identifier",
                      "props": {
                        "logicalType": "int"
                      }
                    }
                  ],
                  "metadata": [
                    {
                      "name": "source",
                      "rawName": "source",
                      "originalFieldName": "source",
                      "type": "STRING",
                      "nullable": true,
                      "metadata": true,
                      "errorCapable": false,
                      "valid": true,
                      "comment": "meta field",
                      "props": {
                        "m": "v"
                      }
                    }
                  ]
                }
                """;

        org.talend.sdk.component.server.front.model.Schema schema = jsonb.fromJson(new StringReader(json),
                org.talend.sdk.component.server.front.model.Schema.class);

        assertNotNull(schema);
        assertEquals(org.talend.sdk.component.server.front.model.Schema.Type.RECORD, schema.getType());

        // ---- props
        assertEquals("v1", schema.getProp("p1"));
        assertEquals("v1", schema.getProps().get("p1"));

        // ---- entries
        List<Entry> entries = schema.getEntries();
        assertEquals(1, entries.size());

        Entry id = entries.get(0);
        assertEquals("id", id.getName());
        assertEquals(org.talend.sdk.component.server.front.model.Schema.Type.INT, id.getType());
        assertFalse(id.isNullable());
        assertEquals("identifier", id.getComment());
        assertEquals("int", id.getProp("logicalType"));

        // ---- metadata
        List<Entry> metadata = schema.getMetadata();
        assertEquals(1, metadata.size());

        Entry source = metadata.get(0);
        assertTrue(source.isMetadata());
        assertEquals("source", source.getName());
        assertEquals("v", source.getProp("m"));

        // ---- entry lookup
        assertSame(id, schema.getEntries().get(0));
        assertSame(source, schema.getMetadata().get(0));
    }

    @Test
    void deserializeArraySchemaWithElementSchema() {
        String json =
                """
                {
                  "type": "ARRAY",
                  "elementSchema": {
                    "type": "STRING"
                  }
                }
                """;

        org.talend.sdk.component.server.front.model.Schema schema = jsonb.fromJson(json,
                org.talend.sdk.component.server.front.model.Schema.class);

        assertEquals(org.talend.sdk.component.server.front.model.Schema.Type.ARRAY, schema.getType());
        assertNotNull(schema.getElementSchema());
        assertEquals(org.talend.sdk.component.server.front.model.Schema.Type.STRING,
                schema.getElementSchema().getType());
    }

    @Test
    void jsonPropIsParsedAsJsonWhenPossible() {
        String json =
                """
                {
                  "type": "RECORD",
                  "props": {
                    "config": "{\\"a\\":1}"
                  }
                }
                """;

        org.talend.sdk.component.server.front.model.Schema schema = jsonb.fromJson(json,
                org.talend.sdk.component.server.front.model.Schema.class);

        assertNotNull(schema.getProp("config"));
        assertEquals("{\"a\":1}", schema.getProp("config"));
    }

    @Test
    void unknownPropertyIsNull() {
        String json =
                """
                {
                  "type": "RECORD"
                }
                """;

        org.talend.sdk.component.server.front.model.Schema schema = jsonb.fromJson(json,
                org.talend.sdk.component.server.front.model.Schema.class);

        assertNull(schema.getProps());
    }

    @Test
    void testAllDataTypes1() {
        // given
        final org.talend.sdk.component.api.record.Schema schema = new SchemaImpl.BuilderImpl() //
                .withType(org.talend.sdk.component.api.record.Schema.Type.RECORD)
                .withEntry(new SchemaImpl.EntryImpl.BuilderImpl()
                        .withName("id")
                        .withType(org.talend.sdk.component.api.record.Schema.Type.INT)
                        .withNullable(false)
                        .withErrorCapable(true)
                        .build())
                .withEntry(new SchemaImpl.EntryImpl.BuilderImpl()
                        .withName("field_date")
                        .withType(org.talend.sdk.component.api.record.Schema.Type.DATETIME)
                        .withNullable(false)
                        .withErrorCapable(false)
                        .withComment("field date")
                        .build())
                .withEntry(new SchemaImpl.EntryImpl.BuilderImpl()
                        .withName("field_boolean")
                        .withType(org.talend.sdk.component.api.record.Schema.Type.BOOLEAN)
                        .withNullable(true)
                        .withComment("field boolean")
                        .build())
                .withEntry(new SchemaImpl.EntryImpl.BuilderImpl()
                        .withName("field_bytes")
                        .withType(org.talend.sdk.component.api.record.Schema.Type.BYTES)
                        .withComment("field bytes")
                        .withNullable(true)
                        .build())
                .withProp("namespace", "test")
                .build();

        // when: serialize SchemaImpl
        String json = jsonb.toJson(schema);

        // then: sanity check JSON
        assertTrue(json.contains("\"type\":\"RECORD\""));
        assertTrue(json.contains("\"entries\""));

        // when: deserialize into Schema
        org.talend.sdk.component.server.front.model.Schema model = jsonb.fromJson(new StringReader(json),
                org.talend.sdk.component.server.front.model.Schema.class);

        // then
        assertEquals(org.talend.sdk.component.server.front.model.Schema.Type.RECORD, model.getType());
        assertEquals("test", model.getProps().get("namespace"));

        assertEquals(4, model.getEntries().size());
        assertEquals("id", model.getEntries().get(0).getName());
        assertEquals(org.talend.sdk.component.server.front.model.Schema.Type.INT, model.getEntries().get(0).getType());
        assertFalse(model.getEntries().get(0).isNullable());
        assertTrue(model.getEntries().get(0).isErrorCapable());
        assertEquals("field_date", model.getEntries().get(1).getName());
        assertEquals(org.talend.sdk.component.server.front.model.Schema.Type.DATETIME,
                model.getEntries().get(1).getType());
        assertEquals("field date", model.getEntries().get(1).getComment());
        assertFalse(model.getEntries().get(1).isNullable());
        assertFalse(model.getEntries().get(1).isErrorCapable());

        assertEquals("field_boolean", model.getEntries().get(2).getName());
        assertEquals(org.talend.sdk.component.server.front.model.Schema.Type.BOOLEAN,
                model.getEntries().get(2).getType());
        assertEquals("field boolean", model.getEntries().get(2).getComment());
        assertTrue(model.getEntries().get(2).isNullable());
        assertFalse(model.getEntries().get(2).isErrorCapable());
        assertEquals("field_bytes", model.getEntries().get(3).getName());
        assertEquals(org.talend.sdk.component.server.front.model.Schema.Type.BYTES,
                model.getEntries().get(3).getType());
        assertEquals("field bytes", model.getEntries().get(3).getComment());
        assertTrue(model.getEntries().get(3).isNullable());

        assertEquals("id,field_date,field_boolean,field_bytes",
                model.getProps().get("talend.fields.order"));
    }

    @Test
    void testAllDataTypes2() {
        // given
        final org.talend.sdk.component.api.record.Schema schema = new SchemaImpl.BuilderImpl() //
                .withType(org.talend.sdk.component.api.record.Schema.Type.RECORD)
                .withEntry(new SchemaImpl.EntryImpl.BuilderImpl()
                        .withName("field_decimal")
                        .withType(org.talend.sdk.component.api.record.Schema.Type.DECIMAL)
                        .withComment("field decimal")
                        .withNullable(true)
                        .build())
                .withEntry(new SchemaImpl.EntryImpl.BuilderImpl()
                        .withName("field_double")
                        .withType(org.talend.sdk.component.api.record.Schema.Type.DOUBLE)
                        .withComment("field double")
                        .withNullable(true)
                        .build())
                .withEntry(new SchemaImpl.EntryImpl.BuilderImpl()
                        .withName("field_float")
                        .withType(org.talend.sdk.component.api.record.Schema.Type.FLOAT)
                        .withComment("field float")
                        .withNullable(true)
                        .build())
                .withProp("namespace", "test")
                .build();

        // when: serialize SchemaImpl
        String json = jsonb.toJson(schema);

        // then: sanity check JSON
        assertTrue(json.contains("\"type\":\"RECORD\""));
        assertTrue(json.contains("\"entries\""));

        // when: deserialize into Schema
        org.talend.sdk.component.server.front.model.Schema model = jsonb.fromJson(new StringReader(json),
                org.talend.sdk.component.server.front.model.Schema.class);

        // then
        assertEquals(org.talend.sdk.component.server.front.model.Schema.Type.RECORD, model.getType());
        assertEquals("test", model.getProps().get("namespace"));

        assertEquals(3, model.getEntries().size());

        assertEquals("field_decimal", model.getEntries().get(0).getName());
        assertEquals(org.talend.sdk.component.server.front.model.Schema.Type.DECIMAL,
                model.getEntries().get(0).getType());
        assertEquals("field decimal", model.getEntries().get(0).getComment());
        assertTrue(model.getEntries().get(0).isNullable());
        assertEquals("field_double", model.getEntries().get(1).getName());
        assertEquals(org.talend.sdk.component.server.front.model.Schema.Type.DOUBLE,
                model.getEntries().get(1).getType());
        assertEquals("field double", model.getEntries().get(1).getComment());
        assertTrue(model.getEntries().get(1).isNullable());

        assertEquals("field_float", model.getEntries().get(2).getName());
        assertEquals(org.talend.sdk.component.server.front.model.Schema.Type.FLOAT,
                model.getEntries().get(2).getType());
        assertEquals("field float", model.getEntries().get(2).getComment());
        assertTrue(model.getEntries().get(2).isNullable());

        assertEquals("field_decimal,field_double,field_float",
                model.getProps().get("talend.fields.order"));
    }

    @Test
    void shouldParseArrayEntryElementSchema() {
        org.talend.sdk.component.api.record.Schema.Entry nameEntry = new SchemaImpl.EntryImpl.BuilderImpl()
                .withName("name")
                .withNullable(true)
                .withType(org.talend.sdk.component.api.record.Schema.Type.STRING)
                .build();
        org.talend.sdk.component.api.record.Schema.Entry ageEntry = new SchemaImpl.EntryImpl.BuilderImpl()
                .withName("age")
                .withNullable(true)
                .withType(org.talend.sdk.component.api.record.Schema.Type.INT)
                .build();
        org.talend.sdk.component.api.record.Schema customerSchema = new SchemaImpl.BuilderImpl() //
                .withType(org.talend.sdk.component.api.record.Schema.Type.RECORD)
                .withEntry(nameEntry)
                .withEntry(ageEntry)
                .build();

        final org.talend.sdk.component.api.record.Schema schema = new SchemaImpl.BuilderImpl() //
                .withType(org.talend.sdk.component.api.record.Schema.Type.ARRAY) //
                .withElementSchema(customerSchema)
                .withProp("namespace", "test")
                .build();

        String json = jsonb.toJson(schema);
        // when: deserialize into Schema
        org.talend.sdk.component.server.front.model.Schema model = jsonb.fromJson(new StringReader(json),
                org.talend.sdk.component.server.front.model.Schema.class);

        org.talend.sdk.component.server.front.model.Schema innerSchema = model.getElementSchema();

        assertEquals(org.talend.sdk.component.server.front.model.Schema.Type.ARRAY, model.getType());
        assertNotNull(innerSchema);
        assertEquals(org.talend.sdk.component.server.front.model.Schema.Type.RECORD, innerSchema.getType());
        // check entry name
        final List<String> entryNames = innerSchema.getEntries()
                .stream()
                .map(Entry::getName)
                .toList();
        Assertions.assertEquals(2, entryNames.size());
        Assertions.assertTrue(entryNames.contains("name"));
        Assertions.assertTrue(entryNames.contains("age"));

        // check entry type
        final List<org.talend.sdk.component.server.front.model.Schema.Type> entryTypes =
                innerSchema.getEntries().stream().map(Entry::getType).toList();
        Assertions.assertTrue(entryTypes.contains(org.talend.sdk.component.server.front.model.Schema.Type.INT));
        Assertions.assertTrue(entryTypes.contains(org.talend.sdk.component.server.front.model.Schema.Type.STRING));
    }

    @Test
    void shouldMarkMetadataEntries() {
        org.talend.sdk.component.api.record.Schema.Entry meta1 = new SchemaImpl.EntryImpl.BuilderImpl() //
                .withName("meta1") //
                .withType(org.talend.sdk.component.api.record.Schema.Type.INT) //
                .withMetadata(true) //
                .build();

        org.talend.sdk.component.api.record.Schema.Entry meta2 = new SchemaImpl.EntryImpl.BuilderImpl() //
                .withName("meta2") //
                .withType(org.talend.sdk.component.api.record.Schema.Type.STRING) //
                .withMetadata(true) //
                .withNullable(true) //
                .build();

        org.talend.sdk.component.api.record.Schema.Entry data1 = new SchemaImpl.EntryImpl.BuilderImpl() //
                .withName("data1") //
                .withType(org.talend.sdk.component.api.record.Schema.Type.INT) //
                .build();
        org.talend.sdk.component.api.record.Schema.Entry data2 = new SchemaImpl.EntryImpl.BuilderImpl() //
                .withName("data2") //
                .withType(org.talend.sdk.component.api.record.Schema.Type.STRING) //
                .withNullable(true) //
                .build();

        org.talend.sdk.component.api.record.Schema schema = new SchemaImpl.BuilderImpl() //
                .withType(org.talend.sdk.component.api.record.Schema.Type.RECORD) //
                .withEntry(data1) //
                .withEntry(meta1) //
                .withEntry(data2) //
                .withEntry(meta2) //
                .build();

        String json = jsonb.toJson(schema);
        // when: deserialize into Schema
        org.talend.sdk.component.server.front.model.Schema model = jsonb.fromJson(new StringReader(json),
                org.talend.sdk.component.server.front.model.Schema.class);

        org.talend.sdk.component.server.front.model.Entry metaEntry = model.getMetadata().get(0);

        // check entry name
        final List<String> entryNames = model.getEntries()
                .stream()
                .map(Entry::getName)
                .toList();
        Assertions.assertEquals(2, entryNames.size());
        Assertions.assertTrue(entryNames.contains(data1.getName()));
        Assertions.assertTrue(entryNames.contains(data2.getName()));
        Assertions.assertEquals(4, schema.getAllEntries().count());

        // check entry type
        final List<String> entryTypes = model.getEntries()
                .stream()
                .map(Entry::getType)
                .map(org.talend.sdk.component.server.front.model.Schema.Type::name)
                .toList();
        Assertions.assertEquals(2, entryTypes.size());
        Assertions.assertTrue(entryTypes.contains(data1.getType().name()));
        Assertions.assertTrue(entryTypes.contains(data2.getType().name()));

        // check meta name
        final List<String> metaEntryNames = model.getMetadata()
                .stream()
                .map(Entry::getName)
                .toList();
        Assertions.assertEquals(2, metaEntryNames.size());
        Assertions.assertTrue(metaEntryNames.contains(meta1.getName()));
        Assertions.assertTrue(metaEntryNames.contains(meta2.getName()));

        // check meta type
        final List<String> metaEntryTypes = model.getMetadata()
                .stream()
                .map(Entry::getType)
                .map(org.talend.sdk.component.server.front.model.Schema.Type::name)
                .toList();
        Assertions.assertEquals(2, metaEntryTypes.size());
        Assertions.assertTrue(metaEntryTypes.contains(meta1.getType().name()));
        Assertions.assertTrue(metaEntryTypes.contains(meta2.getType().name()));

        assertTrue(metaEntry.isMetadata());
        assertEquals("meta1", metaEntry.getName());
    }

    @Test
    void deserializeCompleteSchemaWithAllTypesAndParameters() throws Exception {
        RecordBuilderFactoryImpl factory = new RecordBuilderFactoryImpl("test");

        // Build a comprehensive schema with all supported types and all field parameters
        org.talend.sdk.component.api.record.Schema schemaImpl =
                factory.newSchemaBuilder(org.talend.sdk.component.api.record.Schema.Type.RECORD)
                        .withProp("namespace", "com.talend.test")
                        .withProp("doc", "Comprehensive schema test")
                        .withProp("customProp1", "value1")
                        .withProp("customProp2", "value2")
                        // STRING type with various parameters
                        .withEntry(factory.newEntryBuilder()
                                .withName("stringField")
                                .withRawName("string_field")
                                .withType(org.talend.sdk.component.api.record.Schema.Type.STRING)
                                .withNullable(false)
                                .withDefaultValue("default string")
                                .withComment("String field with default value")
                                .withProp(SchemaProperty.SIZE, "255")
                                .withProp(SchemaProperty.PATTERN, "[a-zA-Z0-9]+")
                                .withProp(SchemaProperty.IS_KEY, "true")
                                .build())
                        // INT type
                        .withEntry(factory.newEntryBuilder()
                                .withName("intField")
                                .withRawName("int_field")
                                .withType(org.talend.sdk.component.api.record.Schema.Type.INT)
                                .withNullable(true)
                                .withDefaultValue(42)
                                .withComment("Integer field with default")
                                .withProp(SchemaProperty.ORIGIN_TYPE, "integer")
                                .build())
                        // LONG type with error handling
                        .withEntry(factory.newEntryBuilder()
                                .withName("longField")
                                .withRawName("long_field")
                                .withType(org.talend.sdk.component.api.record.Schema.Type.LONG)
                                .withNullable(false)
                                .withErrorCapable(true)
                                .withComment("Long field with error capability")
                                .withProp(SchemaProperty.IS_UNIQUE, "true")
                                .build())
                        // FLOAT type
                        .withEntry(factory.newEntryBuilder()
                                .withName("floatField")
                                .withRawName("float_field")
                                .withType(org.talend.sdk.component.api.record.Schema.Type.FLOAT)
                                .withNullable(true)
                                .withDefaultValue(3.14f)
                                .withComment("Float field")
                                .withProp(SchemaProperty.SCALE, "2")
                                .build())
                        // DOUBLE type
                        .withEntry(factory.newEntryBuilder()
                                .withName("doubleField")
                                .withRawName("double_field")
                                .withType(org.talend.sdk.component.api.record.Schema.Type.DOUBLE)
                                .withNullable(false)
                                .withDefaultValue(2.718281828)
                                .withComment("Double field with high precision")
                                .withProp(SchemaProperty.SCALE, "9")
                                .build())
                        // BOOLEAN type
                        .withEntry(factory.newEntryBuilder()
                                .withName("booleanField")
                                .withRawName("boolean_field")
                                .withType(org.talend.sdk.component.api.record.Schema.Type.BOOLEAN)
                                .withNullable(true)
                                .withDefaultValue(true)
                                .withComment("Boolean field")
                                .build())
                        // BYTES type
                        .withEntry(factory.newEntryBuilder()
                                .withName("bytesField")
                                .withRawName("bytes_field")
                                .withType(org.talend.sdk.component.api.record.Schema.Type.BYTES)
                                .withNullable(true)
                                .withComment("Bytes field for binary data")
                                .withProp(SchemaProperty.SIZE, "1024")
                                .build())
                        // DECIMAL type
                        .withEntry(factory.newEntryBuilder()
                                .withName("decimalField")
                                .withRawName("decimal_field")
                                .withType(org.talend.sdk.component.api.record.Schema.Type.DECIMAL)
                                .withNullable(false)
                                .withComment("Decimal field for precise calculations")
                                .withProp(SchemaProperty.SIZE, "10")
                                .withProp(SchemaProperty.SCALE, "2")
                                .build())
                        // DATETIME type with DATE logical type
                        .withEntry(factory.newEntryBuilder()
                                .withName("dateField")
                                .withRawName("date_field")
                                .withType(org.talend.sdk.component.api.record.Schema.Type.DATETIME)
                                .withLogicalType(LogicalType.DATE)
                                .withNullable(true)
                                .withComment("Date field with DATE logical type")
                                .withProp(SchemaProperty.PATTERN, "yyyy-MM-dd")
                                .build())
                        // DATETIME type with TIME logical type
                        .withEntry(factory.newEntryBuilder()
                                .withName("timeField")
                                .withRawName("time_field")
                                .withType(org.talend.sdk.component.api.record.Schema.Type.DATETIME)
                                .withLogicalType(LogicalType.TIME)
                                .withNullable(true)
                                .withComment("Time field with TIME logical type")
                                .withProp(SchemaProperty.PATTERN, "HH:mm:ss")
                                .build())
                        // DATETIME type with TIMESTAMP logical type
                        .withEntry(factory.newEntryBuilder()
                                .withName("timestampField")
                                .withRawName("timestamp_field")
                                .withType(org.talend.sdk.component.api.record.Schema.Type.DATETIME)
                                .withLogicalType(LogicalType.TIMESTAMP)
                                .withNullable(false)
                                .withComment("Timestamp field")
                                .withProp(SchemaProperty.PATTERN, "yyyy-MM-dd'T'HH:mm:ss.SSSZ")
                                .build())
                        // STRING type with UUID logical type
                        .withEntry(factory.newEntryBuilder()
                                .withName("uuidField")
                                .withRawName("uuid_field")
                                .withType(org.talend.sdk.component.api.record.Schema.Type.STRING)
                                .withLogicalType(LogicalType.UUID)
                                .withNullable(true)
                                .withComment("UUID field")
                                .withProp(SchemaProperty.IS_FOREIGN_KEY, "true")
                                .build())
                        // ARRAY type with primitive element schema
                        .withEntry(factory.newEntryBuilder()
                                .withName("stringArrayField")
                                .withRawName("string_array_field")
                                .withType(org.talend.sdk.component.api.record.Schema.Type.ARRAY)
                                .withElementSchema(
                                        factory.newSchemaBuilder(org.talend.sdk.component.api.record.Schema.Type.STRING)
                                                .build())
                                .withNullable(true)
                                .withComment("Array of strings")
                                .build())
                        // ARRAY type with complex element schema
                        .withEntry(factory.newEntryBuilder()
                                .withName("intArrayField")
                                .withRawName("int_array_field")
                                .withType(org.talend.sdk.component.api.record.Schema.Type.ARRAY)
                                .withElementSchema(
                                        factory.newSchemaBuilder(org.talend.sdk.component.api.record.Schema.Type.INT)
                                                .build())
                                .withNullable(false)
                                .withComment("Array of integers")
                                .build())
                        // RECORD type (nested record)
                        .withEntry(factory.newEntryBuilder()
                                .withName("nestedRecord")
                                .withRawName("nested_record")
                                .withType(org.talend.sdk.component.api.record.Schema.Type.RECORD)
                                .withElementSchema(
                                        factory.newSchemaBuilder(org.talend.sdk.component.api.record.Schema.Type.RECORD)
                                                .withProp("nestedNamespace", "com.talend.nested")
                                                .withEntry(factory.newEntryBuilder()
                                                        .withName("nestedString")
                                                        .withRawName("nested_string")
                                                        .withType(
                                                                org.talend.sdk.component.api.record.Schema.Type.STRING)
                                                        .withNullable(true)
                                                        .withComment("Nested string field")
                                                        .build())
                                                .withEntry(factory.newEntryBuilder()
                                                        .withName("nestedInt")
                                                        .withRawName("nested_int")
                                                        .withType(org.talend.sdk.component.api.record.Schema.Type.INT)
                                                        .withNullable(false)
                                                        .withDefaultValue(100)
                                                        .withComment("Nested int field")
                                                        .build())
                                                .withEntry(factory.newEntryBuilder()
                                                        .withName("nestedDate")
                                                        .withRawName("nested_date")
                                                        .withType(
                                                                org.talend.sdk.component.api.record.Schema.Type.DATETIME)
                                                        .withLogicalType(LogicalType.DATE)
                                                        .withNullable(true)
                                                        .withComment("Nested date field")
                                                        .build())
                                                .build())
                                .withNullable(true)
                                .withComment("Nested record structure")
                                .build())
                        // ARRAY of RECORD type
                        .withEntry(factory.newEntryBuilder()
                                .withName("arrayOfRecordsField")
                                .withRawName("array_of_records_field")
                                .withType(org.talend.sdk.component.api.record.Schema.Type.ARRAY)
                                .withElementSchema(
                                        factory.newSchemaBuilder(org.talend.sdk.component.api.record.Schema.Type.RECORD)
                                                .withEntry(factory.newEntryBuilder()
                                                        .withName("recordInArrayString")
                                                        .withType(
                                                                org.talend.sdk.component.api.record.Schema.Type.STRING)
                                                        .withNullable(true)
                                                        .build())
                                                .withEntry(factory.newEntryBuilder()
                                                        .withName("recordInArrayLong")
                                                        .withType(org.talend.sdk.component.api.record.Schema.Type.LONG)
                                                        .withNullable(false)
                                                        .build())
                                                .build())
                                .withNullable(true)
                                .withComment("Array of nested records")
                                .build())
                        // Metadata entry
                        .withEntry(factory.newEntryBuilder()
                                .withName("metadataSource")
                                .withRawName("metadata_source")
                                .withType(org.talend.sdk.component.api.record.Schema.Type.STRING)
                                .withMetadata(true)
                                .withNullable(true)
                                .withComment("Metadata field indicating source")
                                .withProp("metaProp1", "metaValue1")
                                .build())
                        .build();

        // Serialize the schema to JSON
        String json = jsonb.toJson(schemaImpl);

        // Deserialize using ObjectMapper (Jackson)
        ObjectMapper mapper = new ObjectMapper();
        Schema schema = mapper.readValue(json, Schema.class);

        // ===== Validate top-level schema properties =====
        assertNotNull(schema);
        assertEquals(Schema.Type.RECORD, schema.getType());
        assertNotNull(schema.getEntries());
        assertNotNull(schema.getProps());
        assertEquals("com.talend.test", schema.getProp("namespace"));
        assertEquals("Comprehensive schema test", schema.getProp("doc"));
        assertEquals("value1", schema.getProp("customProp1"));
        assertEquals("value2", schema.getProp("customProp2"));

        // We have 17 data entries + 1 metadata entry = 18 total
        // But entries list only contains data entries
        assertEquals(schemaImpl.getEntries().size(), schema.getEntries().size());
        assertEquals(schemaImpl.getMetadata().size(), schema.getMetadata().size());

        // ===== Validate STRING field =====
        Entry stringField = schema.getEntries().get(0);
        assertEquals("stringField", stringField.getName());
        assertEquals("string_field", stringField.getRawName());
        assertEquals(Schema.Type.STRING, stringField.getType());
        assertFalse(stringField.isNullable());
        assertEquals("default string", stringField.getDefaultValue());
        assertEquals("String field with default value", stringField.getComment());
        assertEquals("255", stringField.getProp(SchemaProperty.SIZE));
        assertEquals("[a-zA-Z0-9]+", stringField.getProp(SchemaProperty.PATTERN));
        assertEquals("true", stringField.getProp(SchemaProperty.IS_KEY));
        assertFalse(stringField.isMetadata());

        // ===== Validate INT field =====
        Entry intField = schema.getEntries().get(1);
        assertEquals("intField", intField.getName());
        assertEquals("int_field", intField.getRawName());
        assertEquals(Schema.Type.INT, intField.getType());
        assertTrue(intField.isNullable());
        assertEquals(42, intField.<Integer> getDefaultValue());
        assertEquals("Integer field with default", intField.getComment());
        assertEquals("integer", intField.getProp(SchemaProperty.ORIGIN_TYPE));

        // ===== Validate LONG field with error capability =====
        Entry longField = schema.getEntries().get(2);
        assertEquals("longField", longField.getName());
        assertEquals("long_field", longField.getRawName());
        assertEquals(Schema.Type.LONG, longField.getType());
        assertFalse(longField.isNullable());
        assertTrue(longField.isErrorCapable());
        assertEquals("Long field with error capability", longField.getComment());
        assertEquals("true", longField.getProp(SchemaProperty.IS_UNIQUE));

        // ===== Validate FLOAT field =====
        Entry floatField = schema.getEntries().get(3);
        assertEquals("floatField", floatField.getName());
        assertEquals("float_field", floatField.getRawName());
        assertEquals(Schema.Type.FLOAT, floatField.getType());
        assertTrue(floatField.isNullable());
        assertEquals(3.14f, floatField.<Float> getDefaultValue());
        assertEquals("Float field", floatField.getComment());
        assertEquals("2", floatField.getProp(SchemaProperty.SCALE));

        // ===== Validate DOUBLE field =====
        Entry doubleField = schema.getEntries().get(4);
        assertEquals("doubleField", doubleField.getName());
        assertEquals("double_field", doubleField.getRawName());
        assertEquals(Schema.Type.DOUBLE, doubleField.getType());
        assertFalse(doubleField.isNullable());
        assertEquals(2.718281828, doubleField.getDefaultValue());
        assertEquals("Double field with high precision", doubleField.getComment());
        assertEquals("9", doubleField.getProp(SchemaProperty.SCALE));

        // ===== Validate BOOLEAN field =====
        Entry booleanField = schema.getEntries().get(5);
        assertEquals("booleanField", booleanField.getName());
        assertEquals("boolean_field", booleanField.getRawName());
        assertEquals(Schema.Type.BOOLEAN, booleanField.getType());
        assertTrue(booleanField.isNullable());
        assertEquals(true, booleanField.getDefaultValue());
        assertEquals("Boolean field", booleanField.getComment());

        // ===== Validate BYTES field =====
        Entry bytesField = schema.getEntries().get(6);
        assertEquals("bytesField", bytesField.getName());
        assertEquals("bytes_field", bytesField.getRawName());
        assertEquals(Schema.Type.BYTES, bytesField.getType());
        assertTrue(bytesField.isNullable());
        assertEquals("Bytes field for binary data", bytesField.getComment());
        assertEquals("1024", bytesField.getProp(SchemaProperty.SIZE));

        // ===== Validate DECIMAL field =====
        Entry decimalField = schema.getEntries().get(7);
        assertEquals("decimalField", decimalField.getName());
        assertEquals("decimal_field", decimalField.getRawName());
        assertEquals(Schema.Type.DECIMAL, decimalField.getType());
        assertFalse(decimalField.isNullable());
        assertEquals("Decimal field for precise calculations", decimalField.getComment());
        assertEquals("10", decimalField.getProp(SchemaProperty.SIZE));
        assertEquals("2", decimalField.getProp(SchemaProperty.SCALE));

        // ===== Validate DATE field (DATETIME with DATE logical type) =====
        Entry dateField = schema.getEntries().get(8);
        assertEquals("dateField", dateField.getName());
        assertEquals("date_field", dateField.getRawName());
        assertEquals(Schema.Type.DATETIME, dateField.getType());
        assertEquals(LogicalType.DATE.key(), dateField.getProp(SchemaProperty.LOGICAL_TYPE));
        assertTrue(dateField.isNullable());
        assertEquals("Date field with DATE logical type", dateField.getComment());
        assertEquals("yyyy-MM-dd", dateField.getProp(SchemaProperty.PATTERN));

        // ===== Validate TIME field (DATETIME with TIME logical type) =====
        Entry timeField = schema.getEntries().get(9);
        assertEquals("timeField", timeField.getName());
        assertEquals("time_field", timeField.getRawName());
        assertEquals(Schema.Type.DATETIME, timeField.getType());
        assertEquals(LogicalType.TIME.key(), timeField.getProp(SchemaProperty.LOGICAL_TYPE));
        assertTrue(timeField.isNullable());
        assertEquals("Time field with TIME logical type", timeField.getComment());
        assertEquals("HH:mm:ss", timeField.getProp(SchemaProperty.PATTERN));

        // ===== Validate TIMESTAMP field (DATETIME with TIMESTAMP logical type) =====
        Entry timestampField = schema.getEntries().get(10);
        assertEquals("timestampField", timestampField.getName());
        assertEquals("timestamp_field", timestampField.getRawName());
        assertEquals(Schema.Type.DATETIME, timestampField.getType());
        assertEquals(LogicalType.TIMESTAMP.key(), timestampField.getProp(SchemaProperty.LOGICAL_TYPE));
        assertFalse(timestampField.isNullable());
        assertEquals("Timestamp field", timestampField.getComment());
        assertEquals("yyyy-MM-dd'T'HH:mm:ss.SSSZ", timestampField.getProp(SchemaProperty.PATTERN));

        // ===== Validate UUID field (STRING with UUID logical type) =====
        Entry uuidField = schema.getEntries().get(11);
        assertEquals("uuidField", uuidField.getName());
        assertEquals("uuid_field", uuidField.getRawName());
        assertEquals(Schema.Type.STRING, uuidField.getType());
        assertEquals(LogicalType.UUID.key(), uuidField.getProp(SchemaProperty.LOGICAL_TYPE));
        assertTrue(uuidField.isNullable());
        assertEquals("UUID field", uuidField.getComment());
        assertEquals("true", uuidField.getProp(SchemaProperty.IS_FOREIGN_KEY));

        // ===== Validate ARRAY field with STRING elements =====
        Entry stringArrayField = schema.getEntries().get(12);
        assertEquals("stringArrayField", stringArrayField.getName());
        assertEquals("string_array_field", stringArrayField.getRawName());
        assertEquals(Schema.Type.ARRAY, stringArrayField.getType());
        assertTrue(stringArrayField.isNullable());
        assertEquals("Array of strings", stringArrayField.getComment());
        assertNotNull(stringArrayField.getElementSchema());
        assertEquals(Schema.Type.STRING, stringArrayField.getElementSchema().getType());

        // ===== Validate ARRAY field with INT elements =====
        Entry intArrayField = schema.getEntries().get(13);
        assertEquals("intArrayField", intArrayField.getName());
        assertEquals("int_array_field", intArrayField.getRawName());
        assertEquals(Schema.Type.ARRAY, intArrayField.getType());
        assertFalse(intArrayField.isNullable());
        assertEquals("Array of integers", intArrayField.getComment());
        assertNotNull(intArrayField.getElementSchema());
        assertEquals(Schema.Type.INT, intArrayField.getElementSchema().getType());

        // ===== Validate nested RECORD field =====
        Entry nestedRecord = schema.getEntries().get(14);
        assertEquals("nestedRecord", nestedRecord.getName());
        assertEquals("nested_record", nestedRecord.getRawName());
        assertEquals(Schema.Type.RECORD, nestedRecord.getType());
        assertTrue(nestedRecord.isNullable());
        assertEquals("Nested record structure", nestedRecord.getComment());
        assertNotNull(nestedRecord.getElementSchema());
        assertEquals(Schema.Type.RECORD, nestedRecord.getElementSchema().getType());

        // Validate nested record properties
        Schema nestedSchema = nestedRecord.getElementSchema();
        assertEquals("com.talend.nested", nestedSchema.getProp("nestedNamespace"));
        assertEquals(3, nestedSchema.getEntries().size());

        // Validate nested string field
        Entry nestedString = nestedSchema.getEntries().get(0);
        assertEquals("nestedString", nestedString.getName());
        assertEquals("nested_string", nestedString.getRawName());
        assertEquals(Schema.Type.STRING, nestedString.getType());
        assertTrue(nestedString.isNullable());
        assertEquals("Nested string field", nestedString.getComment());

        // Validate nested int field
        Entry nestedInt = nestedSchema.getEntries().get(1);
        assertEquals("nestedInt", nestedInt.getName());
        assertEquals("nested_int", nestedInt.getRawName());
        assertEquals(Schema.Type.INT, nestedInt.getType());
        assertFalse(nestedInt.isNullable());
        assertEquals(100, nestedInt.<Integer> getDefaultValue());
        assertEquals("Nested int field", nestedInt.getComment());

        // Validate nested date field
        Entry nestedDate = nestedSchema.getEntries().get(2);
        assertEquals("nestedDate", nestedDate.getName());
        assertEquals("nested_date", nestedDate.getRawName());
        assertEquals(Schema.Type.DATETIME, nestedDate.getType());
        assertEquals(LogicalType.DATE.key(), nestedDate.getProp(SchemaProperty.LOGICAL_TYPE));
        assertTrue(nestedDate.isNullable());
        assertEquals("Nested date field", nestedDate.getComment());

        // ===== Validate ARRAY type with nested RECORD =====
        Entry arrayOfRecordsField = schema.getEntries().get(15);
        assertEquals("arrayOfRecordsField", arrayOfRecordsField.getName());
        assertEquals(Schema.Type.ARRAY, arrayOfRecordsField.getType());
        assertNotNull(arrayOfRecordsField.getElementSchema());
        assertEquals(Schema.Type.RECORD, arrayOfRecordsField.getElementSchema().getType());

        // ===== Validate metadata entry =====
        assertEquals(1, schema.getMetadata().size());
        Entry metadataEntry = schema.getMetadata().get(0);
        assertEquals("metadataSource", metadataEntry.getName());
        assertEquals("metadata_source", metadataEntry.getRawName());
        assertEquals(Schema.Type.STRING, metadataEntry.getType());
        assertTrue(metadataEntry.isMetadata());
        assertTrue(metadataEntry.isNullable());
        assertEquals("Metadata field indicating source", metadataEntry.getComment());
        assertEquals("metaValue1", metadataEntry.getProp("metaProp1"));
    }
}