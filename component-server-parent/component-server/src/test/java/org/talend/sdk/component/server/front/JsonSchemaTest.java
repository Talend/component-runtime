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
package org.talend.sdk.component.server.front;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import static org.talend.sdk.component.api.record.Schema.Type.LONG;
import static org.talend.sdk.component.api.record.Schema.Type.STRING;
import static org.junit.jupiter.api.Assertions.*;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import org.junit.jupiter.api.Test;

import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.runtime.record.SchemaImpl;
import org.talend.sdk.component.server.front.model.JsonEntryModel;
import org.talend.sdk.component.server.front.model.JsonSchemaModel;

class JsonSchemaTest {

    private final Jsonb jsonb = JsonbBuilder.create();

    @Test
    void shouldSerializeSchemaImplAndDeserializeToJsonSchemaModel() {
        // given
        final Schema schema =  new SchemaImpl.BuilderImpl() //
                .withType(Schema.Type.RECORD)
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

        // when: deserialize into JsonSchemaModel
        JsonSchemaModel model = new JsonSchemaModel(json);

        // then
        assertEquals(JsonSchemaModel.Type.RECORD, model.getType());
        assertEquals("test", model.getProps().get("namespace"));

        assertEquals(2, model.getEntries().size());
        assertEquals("id", model.getEntries().get(0).getName());
        assertEquals(JsonSchemaModel.Type.STRING, model.getEntries().get(0).getType());
        assertEquals("field2", model.getEntries().get(1).getName());
        assertEquals(JsonSchemaModel.Type.LONG, model.getEntries().get(1).getType());
        assertEquals("field2 comment", model.getEntries().get(1).getComment());
        assertEquals(false, model.getEntries().get(1).isNullable());

        // entryMap built correctly
        assertTrue(model.getEntryMap().containsKey("id"));
    }

    @Test
    void testAllDataTypes() {
        // given
        final Schema schema =  new SchemaImpl.BuilderImpl() //
                .withType(Schema.Type.RECORD)
                .withEntry(new SchemaImpl.EntryImpl.BuilderImpl()
                        .withName("id")
                        .withType(Schema.Type.INT)
                        .withNullable(false)
                        .withErrorCapable(true)
                        .build())
                .withEntry(new SchemaImpl.EntryImpl.BuilderImpl()
                        .withName("field_date")
                        .withType(Schema.Type.DATETIME)
                        .withNullable(false)
                        .withErrorCapable(false)
                        .withComment("field date")
                        .build())
                .withEntry(new SchemaImpl.EntryImpl.BuilderImpl()
                        .withName("field_boolean")
                        .withType(Schema.Type.BOOLEAN)
                        .withNullable(true)
                        .withComment("field boolean")
                        .build())
                .withEntry(new SchemaImpl.EntryImpl.BuilderImpl()
                        .withName("field_bytes")
                        .withType(Schema.Type.BYTES)
                        .withComment("field bytes")
                        .withNullable(true)
                        .build())
                .withEntry(new SchemaImpl.EntryImpl.BuilderImpl()
                        .withName("field_decimal")
                        .withType(Schema.Type.DECIMAL)
                        .withComment("field decimal")
                        .withNullable(true)
                        .build())
                .withEntry(new SchemaImpl.EntryImpl.BuilderImpl()
                        .withName("field_double")
                        .withType(Schema.Type.DOUBLE)
                        .withComment("field double")
                        .withNullable(true)
                        .build())
                .withEntry(new SchemaImpl.EntryImpl.BuilderImpl()
                        .withName("field_float")
                        .withType(Schema.Type.FLOAT)
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

        // when: deserialize into JsonSchemaModel
        JsonSchemaModel model = new JsonSchemaModel(json);

        // then
        assertEquals(JsonSchemaModel.Type.RECORD, model.getType());
        assertEquals("test", model.getProps().get("namespace"));

        assertEquals(7, model.getEntries().size());
        assertEquals("id", model.getEntries().get(0).getName());
        assertEquals(JsonSchemaModel.Type.INT, model.getEntries().get(0).getType());
        assertFalse(model.getEntries().get(0).isNullable());
        assertTrue(model.getEntries().get(0).isErrorCapable());
        assertEquals("field_date", model.getEntries().get(1).getName());
        assertEquals(JsonSchemaModel.Type.DATETIME, model.getEntries().get(1).getType());
        assertEquals("field date", model.getEntries().get(1).getComment());
        assertFalse(model.getEntries().get(1).isNullable());
        assertFalse(model.getEntries().get(1).isErrorCapable());

        assertEquals("field_boolean", model.getEntries().get(2).getName());
        assertEquals(JsonSchemaModel.Type.BOOLEAN, model.getEntries().get(2).getType());
        assertEquals("field boolean", model.getEntries().get(2).getComment());
        assertTrue(model.getEntries().get(2).isNullable());
        assertFalse(model.getEntries().get(2).isErrorCapable());
        assertEquals("field_bytes", model.getEntries().get(3).getName());
        assertEquals(JsonSchemaModel.Type.BYTES, model.getEntries().get(3).getType());
        assertEquals("field bytes", model.getEntries().get(3).getComment());
        assertTrue(model.getEntries().get(3).isNullable());

        assertEquals("field_decimal", model.getEntries().get(4).getName());
        assertEquals(JsonSchemaModel.Type.DECIMAL, model.getEntries().get(4).getType());
        assertEquals("field decimal", model.getEntries().get(4).getComment());
        assertTrue(model.getEntries().get(4).isNullable());
        assertEquals("field_double", model.getEntries().get(5).getName());
        assertEquals(JsonSchemaModel.Type.DOUBLE, model.getEntries().get(5).getType());
        assertEquals("field double", model.getEntries().get(5).getComment());
        assertTrue(model.getEntries().get(5).isNullable());

        assertEquals("field_float", model.getEntries().get(6).getName());
        assertEquals(JsonSchemaModel.Type.FLOAT, model.getEntries().get(6).getType());
        assertEquals("field float", model.getEntries().get(6).getComment());
        assertTrue(model.getEntries().get(6).isNullable());
        // entryMap built correctly
        assertTrue(model.getEntryMap().containsKey("id"));
        assertEquals("id,field_date,field_boolean,field_bytes,field_decimal,field_double,field_float",
                model.getProps().get("talend.fields.order"));
    }

    @Test
    void shouldParseArrayEntryElementSchema() {
        Schema.Entry nameEntry = new SchemaImpl.EntryImpl.BuilderImpl()
                .withName("name")
                .withNullable(true)
                .withType(Schema.Type.STRING)
                .build();
        Schema.Entry ageEntry = new SchemaImpl.EntryImpl.BuilderImpl()
                .withName("age")
                .withNullable(true)
                .withType(Schema.Type.INT)
                .build();
        Schema customerSchema = new SchemaImpl.BuilderImpl() //
                .withType(Schema.Type.RECORD)
                .withEntry(nameEntry)
                .withEntry(ageEntry)
                .build();

        final Schema schema = new SchemaImpl.BuilderImpl() //
                .withType(Schema.Type.ARRAY) //
                .withElementSchema(customerSchema)
                .withProp("namespace", "test")
                .build();

        String json = jsonb.toJson(schema);
        JsonSchemaModel model = new JsonSchemaModel(json);

        JsonSchemaModel innerSchema = model.getElementSchema();

        assertEquals(JsonSchemaModel.Type.ARRAY, model.getType());
        assertNotNull(innerSchema);
        assertEquals(JsonSchemaModel.Type.RECORD, innerSchema.getType());
        //check entry name
        final List<String> entryNames = innerSchema.getEntries().stream().map(JsonEntryModel::getName)
                .toList();
        Assertions.assertEquals(2, entryNames.size());
        Assertions.assertTrue(entryNames.contains("name"));
        Assertions.assertTrue(entryNames.contains("age"));

        //check entry type
        final List<JsonSchemaModel.Type> entryTypes = innerSchema.getEntries().stream().map(JsonEntryModel::getType)
                .toList();
        Assertions.assertTrue(entryTypes.contains(JsonSchemaModel.Type.INT));
        Assertions.assertTrue(entryTypes.contains(JsonSchemaModel.Type.STRING));
    }


    @Test
    void shouldMarkMetadataEntries() {
        Schema.Entry meta1 = new SchemaImpl.EntryImpl.BuilderImpl() //
                .withName("meta1") //
                .withType(Schema.Type.INT) //
                .withMetadata(true) //
                .build();

        Schema.Entry meta2 = new SchemaImpl.EntryImpl.BuilderImpl() //
                .withName("meta2") //
                .withType(Schema.Type.STRING) //
                .withMetadata(true) //
                .withNullable(true) //
                .build();

        Schema.Entry data1 = new SchemaImpl.EntryImpl.BuilderImpl() //
                .withName("data1") //
                .withType(Schema.Type.INT) //
                .build();
        Schema.Entry data2 = new SchemaImpl.EntryImpl.BuilderImpl() //
                .withName("data2") //
                .withType(Schema.Type.STRING) //
                .withNullable(true) //
                .build();

        Schema schema = new SchemaImpl.BuilderImpl() //
                .withType(Schema.Type.RECORD) //
                .withEntry(data1) //
                .withEntry(meta1) //
                .withEntry(data2) //
                .withEntry(meta2) //
                .build();

        String json = jsonb.toJson(schema);
        JsonSchemaModel model = new JsonSchemaModel(json);

        JsonEntryModel metaEntry = model.getMetadataEntries().get(0);

        //check entry name
        final List<String> entryNames = model.getEntries().stream().map(JsonEntryModel::getName)
                .toList();
        Assertions.assertEquals(2, entryNames.size());
        Assertions.assertTrue(entryNames.contains(data1.getName()));
        Assertions.assertTrue(entryNames.contains(data2.getName()));
        Assertions.assertEquals(4, schema.getAllEntries().count());

        //check entry type
        final List<String> entryTypes = model.getEntries().stream().map(JsonEntryModel::getType)
                .map(JsonSchemaModel.Type::name)
                .toList();
        Assertions.assertEquals(2, entryTypes.size());
        Assertions.assertTrue(entryTypes.contains(data1.getType().name()));
        Assertions.assertTrue(entryTypes.contains(data2.getType().name()));

        //check meta name
        final List<String> metaEntryNames = model.getMetadataEntries().stream().map(JsonEntryModel::getName)
                .toList();
        Assertions.assertEquals(2, metaEntryNames.size());
        Assertions.assertTrue(metaEntryNames.contains(meta1.getName()));
        Assertions.assertTrue(metaEntryNames.contains(meta2.getName()));

        //check meta type
        final List<String> metaEntryTypes = model.getMetadataEntries().stream().map(JsonEntryModel::getType)
                .map(JsonSchemaModel.Type::name)
                .toList();
        Assertions.assertEquals(2, metaEntryTypes.size());
        Assertions.assertTrue(metaEntryTypes.contains(meta1.getType().name()));
        Assertions.assertTrue(metaEntryTypes.contains(meta2.getType().name()));

        assertTrue(metaEntry.isMetadata());
        assertEquals("meta1", metaEntry.getName());
    }

    @Test
    void shouldParseEntryProps() {
        Schema schema = new SchemaImpl.BuilderImpl() //
                .withType(Schema.Type.RECORD)
                //.type(Schema.Type.RECORD)
                .withEntry(new SchemaImpl.EntryImpl.BuilderImpl()
                        .withName("field")
                        .withType(Schema.Type.STRING)
                        .withProp("format", "email")
                        .build())
                .build();

        String json = jsonb.toJson(schema);
        JsonSchemaModel model = new JsonSchemaModel(json);

        JsonEntryModel entry = model.getEntries().get(0);

        assertEquals("email", entry.getProps().get("format"));
    }

    @Test
    void shouldFailForInvalidEntryType() {
        String invalidJson = """
        {
          "type": "RECORD",
          "entries": [
            { "name": "x", "type": "INVALID" }
          ]
        }
        """;

        assertThrows(IllegalArgumentException.class,
                () -> new JsonSchemaModel(invalidJson));
    }


    @Test
    void shouldFailForInvalidSchemaType() {
        String invalidJson = """
        {
          "type": "NOT_VALID",
          "entries": []
        }
        """;

        assertThrows(IllegalArgumentException.class,
                () -> new JsonSchemaModel(invalidJson));
    }

}


