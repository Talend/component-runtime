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
package org.talend.sdk.component.runtime.di.record;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.talend.sdk.component.api.record.SchemaProperty.IS_KEY;
import static org.talend.sdk.component.api.record.SchemaProperty.PATTERN;
import static org.talend.sdk.component.api.record.SchemaProperty.SCALE;
import static org.talend.sdk.component.api.record.SchemaProperty.SIZE;
import static org.talend.sdk.component.api.record.SchemaProperty.STUDIO_TYPE;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;

import org.dom4j.DocumentHelper;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.api.record.Schema.Entry;
import org.talend.sdk.component.api.record.Schema.Type;
import org.talend.sdk.component.api.record.SchemaProperty;
import org.talend.sdk.component.runtime.di.schema.StudioTypes;

import routines.system.Document;
import routines.system.Dynamic;
import routines.system.DynamicMetadata;

import lombok.Data;

class DiRowStructVisitorTest extends VisitorsTest {

    private void createMetadata(final Dynamic dynamic, final String name, final String type, final Object value) {
        createMetadata(dynamic, name, type, value, false);
    }

    private void createMetadata(final Dynamic dynamic, final String name, final String type, final Object value,
            boolean isKey) {
        createMetadata(dynamic, name, type, value, null, isKey, null);
    }

    private void createMetadata(final Dynamic dynamic, final String name, final String type, final Object value,
            final String datePattern, boolean isKey, String logicalType) {
        final DynamicMetadata meta = new DynamicMetadata();
        meta.setName(name);
        meta.setType(type);
        meta.setKey(isKey);
        meta.setFormat(datePattern);
        meta.setLogicalType(logicalType);
        dynamic.metadatas.add(meta);
        dynamic.addColumnValue(value);
    }

    @Test
    void visit() {
        final RowStruct rowStruct = new RowStruct();
        rowStruct.id = ":testing:";
        rowStruct.name = NAME;
        rowStruct.shortP = SHORT;
        rowStruct.shortC = SHORT;
        rowStruct.intP = INT;
        rowStruct.intC = INT;
        rowStruct.longP = LONG;
        rowStruct.longC = LONG;
        rowStruct.floatP = FLOAT;
        rowStruct.floatC = FLOAT;
        rowStruct.doubleP = DOUBLE;
        rowStruct.doubleC = DOUBLE;
        rowStruct.bytes0 = BYTES0;
        rowStruct.date0 = DATE;
        rowStruct.date2 = Date.from(ZONED_DATE_TIME.toInstant());
        rowStruct.date4 = Timestamp.from(INSTANT);
        rowStruct.bigDecimal0 = BIGDEC;
        rowStruct.bool1 = Boolean.TRUE;
        rowStruct.array0 = INTEGERS;
        rowStruct.array1 = LIST_INTEGERS;
        rowStruct.array2 = LIST_HETEROGENEOUS_INTEGER;
        rowStruct.array3 = LIST_HETEROGENEOUS_LIST;
        rowStruct.array4 = LIST_INTEGERS_EMPTY;
        rowStruct.array5 = LIST_3_DEEP;
        rowStruct.object0 = new Rcd();
        rowStruct.hAshcOdEdIrtY = Boolean.TRUE;
        rowStruct.h = NAME;
        rowStruct.char0 = Character.MAX_VALUE;
        // dynamic
        final Dynamic dynamic = new Dynamic();
        createMetadata(dynamic, "dynString", StudioTypes.STRING, "stringy", true);
        createMetadata(dynamic, "dynInteger", StudioTypes.INTEGER, INT);
        createMetadata(dynamic, "dynDouble", StudioTypes.DOUBLE, DOUBLE);
        createMetadata(dynamic, "dynBytes", StudioTypes.BYTE_ARRAY, BYTES0);
        createMetadata(dynamic, "dynBytesArray", StudioTypes.BYTE_ARRAY, BYTES0);
        createMetadata(dynamic, "dynBytesBuffer", StudioTypes.BYTE_ARRAY, ByteBuffer.allocate(100).wrap(BYTES0));
        createMetadata(dynamic, "dynBytesWString", StudioTypes.BYTE_ARRAY, String.valueOf(BYTES0));
        createMetadata(dynamic, "dynBigDecimal", StudioTypes.BIGDECIMAL, BIGDEC);
        createMetadata(dynamic, "dynDocument", StudioTypes.DOCUMENT, DOCUMENT);
        Rcd dynObject = new Rcd();
        createMetadata(dynamic, "dynObject", StudioTypes.OBJECT, dynObject);
        createMetadata(dynamic, "STRINGS", StudioTypes.LIST, STRINGS);
        createMetadata(dynamic, "LONGS", StudioTypes.LIST, LONGS);
        createMetadata(dynamic, "FLOATS", StudioTypes.LIST, FLOATS);
        createMetadata(dynamic, "DOUBLES", StudioTypes.LIST, DOUBLES);
        createMetadata(dynamic, "BOOLEANS", StudioTypes.LIST, BOOLEANS);
        createMetadata(dynamic, "BYTES", StudioTypes.LIST, BYTES);
        createMetadata(dynamic, "DATES", StudioTypes.LIST, DATES);
        createMetadata(dynamic, "RECORDS", StudioTypes.LIST, RECORDS);
        createMetadata(dynamic, "BIG_DECIMALS", StudioTypes.LIST, BIG_DECIMALS);
        createMetadata(dynamic, "dynDate", StudioTypes.DATE, DATE);
        createMetadata(dynamic, "dynStringDate", StudioTypes.STRING,
                "2010-01-31", "yyyy-MM-dd", false, null);
        rowStruct.dynamic = dynamic;

        org.dom4j.Document doc = DocumentHelper.createDocument();
        doc.addElement("catalog").addComment("an XML catalog");
        DOCUMENT.setDocument(doc);
        rowStruct.document.setDocument(doc);
        rowStruct.emptyDocument = new Document();

        final DiRowStructVisitor visitor = new DiRowStructVisitor();
        final Record record = visitor.get(rowStruct, factory);
        final Schema schema = record.getSchema();
        // should have 3 excluded fields
        assertEquals(58, schema.getEntries().size());

        // schema metadata
        assertFalse(schema.getEntry("id").isNullable());
        assertEquals("true", schema.getEntry("id").getProp(IS_KEY));
        assertFalse(schema.getEntry("name").isNullable());
        assertEquals("namy", schema.getEntry("name").getOriginalFieldName());
        assertEquals("John", schema.getEntry("name").getDefaultValue());
        assertEquals("A small comment on name field...", schema.getEntry("name").getComment());
        assertEquals(StudioTypes.STRING, schema.getEntry("name").getProp(STUDIO_TYPE));
        assertEquals("true", schema.getEntry("name").getProp(IS_KEY));
        assertEquals(StudioTypes.FLOAT, schema.getEntry("floatP").getProp(STUDIO_TYPE));
        assertEquals("10", schema.getEntry("floatP").getProp(SIZE));
        assertEquals("2", schema.getEntry("floatP").getProp(SCALE));
        assertEquals(StudioTypes.DATE, schema.getEntry("date0").getProp(STUDIO_TYPE));
        assertEquals("false", schema.getEntry("date0").getProp(IS_KEY));
        assertEquals("YYYY-mm-dd HH:MM:ss", schema.getEntry("date0").getProp(PATTERN));
        assertEquals(StudioTypes.DATE, schema.getEntry("date4").getProp(STUDIO_TYPE));
        assertEquals(StudioTypes.BIGDECIMAL, schema.getEntry("bigDecimal0").getProp(STUDIO_TYPE));
        assertEquals("30", schema.getEntry("bigDecimal0").getProp(SIZE));
        assertEquals("10", schema.getEntry("bigDecimal0").getProp(SCALE));

        assertEquals(StudioTypes.DOCUMENT, schema.getEntry("document").getProp(STUDIO_TYPE));
        assertEquals(DOCUMENT.toString(), rowStruct.document.toString());
        // dyn
        assertTrue(schema.getEntry("dynString").isNullable());
        assertEquals("true", schema.getEntry("dynString").getProp(IS_KEY));
        assertEquals(StudioTypes.STRING, schema.getEntry("dynString").getProp(STUDIO_TYPE));
        assertEquals("dYnAmIc", schema.getEntry("dynString").getComment());
        assertEquals("false", schema.getEntry("dynDouble").getProp(IS_KEY));
        assertEquals(StudioTypes.DOUBLE, schema.getEntry("dynDouble").getProp(STUDIO_TYPE));
        assertEquals("30", schema.getEntry("dynDouble").getProp(SIZE));
        assertEquals("10", schema.getEntry("dynDouble").getProp(SCALE));
        assertEquals(StudioTypes.BIGDECIMAL, schema.getEntry("dynBigDecimal").getProp(STUDIO_TYPE));
        assertEquals("30", schema.getEntry("dynBigDecimal").getProp(SIZE));
        assertEquals("10", schema.getEntry("dynBigDecimal").getProp(SCALE));
        assertEquals(StudioTypes.DATE, schema.getEntry("dynDate").getProp(STUDIO_TYPE));
        assertEquals("YYYY-mm-ddTHH:MM", schema.getEntry("dynDate").getProp(PATTERN));
        assertEquals(StudioTypes.STRING, schema.getEntry("dynStringDate").getProp(STUDIO_TYPE));
        assertEquals("yyyy-MM-dd", schema.getEntry("dynStringDate").getProp(PATTERN));
        assertEquals(StudioTypes.DOCUMENT, schema.getEntry("dynDocument").getProp(STUDIO_TYPE));
        // asserts Record
        assertEquals(":testing:", record.getString("id"));
        assertEquals(NAME, record.getString("name"));
        assertEquals(SHORT, record.getInt("shortP"));
        assertEquals(SHORT, record.getInt("shortC"));
        assertEquals(INT, record.getInt("intP"));
        assertEquals(INT, record.getInt("intC"));
        assertEquals(LONG, record.getLong("longP"));
        assertEquals(LONG, record.getLong("longC"));
        assertEquals(FLOAT, record.getFloat("floatP"));
        assertEquals(FLOAT, record.getFloat("floatC"));
        assertEquals(DOUBLE, record.getDouble("doubleP"));
        assertEquals(DOUBLE, record.getDouble("doubleC"));
        assertEquals(DATE.toInstant(), record.getDateTime("date0").toInstant());
        assertNull(record.getDateTime("date1"));
        assertEquals(ZONED_DATE_TIME, record.getDateTime("date2"));
        assertEquals(1946, record.getDateTime("date2").getYear());
        assertEquals(INSTANT, record.getInstant("date4"));
        assertEquals(BIGDEC.doubleValue(), new BigDecimal(record.getString("bigDecimal0")).doubleValue());
        assertEquals(BIGDEC.toString(), record.getString("bigDecimal0"));
        assertFalse(record.getBoolean("bool0"));
        assertTrue(record.getBoolean("bool1"));
        assertArrayEquals(BYTES0, record.getBytes("bytes0"));
        assertArrayEquals(BYTES0, record.getBytes("dynBytes"));
        assertArrayEquals(BYTES0, record.getBytes("dynBytesArray"));
        assertArrayEquals(BYTES0, record.getBytes("dynBytesBuffer"));
        assertArrayEquals(String.valueOf(BYTES0).getBytes(), record.getBytes("dynBytesWString"));
        assertEquals(BIGDEC.toString(), record.getString("dynBigDecimal"));
        assertEquals(BIGDEC, new BigDecimal(record.getString("dynBigDecimal")));
        assertEquals(DOCUMENT.toString(), record.getString("dynDocument"));
        assertEquals(rowStruct.object0, record.get(Object.class, "object0"));
        assertTrue(record.getBoolean("hAshcOdEdIrtY"));
        assertEquals(NAME, record.getString("h"));
        assertEquals(StudioTypes.CHARACTER, schema.getEntry("char0").getProp(STUDIO_TYPE));
        assertEquals(String.valueOf(Character.MAX_VALUE), record.getString("char0"));
        assertEquals(dynObject, record.get(Object.class, "dynObject"));
        assertEquals(STRINGS, record.getArray(String.class, "STRINGS"));
        assertEquals(LONGS, record.getArray(Long.class, "LONGS"));
        assertEquals(FLOATS, record.getArray(Float.class, "FLOATS"));
        assertEquals(DOUBLES, record.getArray(Double.class, "DOUBLES"));
        assertEquals(BOOLEANS, record.getArray(Boolean.class, "BOOLEANS"));
        assertEquals(BYTES, record.getArray(byte[].class, "BYTES"));
        assertEquals(DATES, record.getArray(ZonedDateTime.class, "DATES"));
        assertEquals(RECORDS, record.getArray(Record.class, "RECORDS"));
        record.getArray(Record.class, "RECORDS").forEach(r -> {
            assertEquals(1, r.getInt("ntgr"));
            assertEquals("one", r.getString("str"));
        });
        assertEquals(BIG_DECIMALS, record.getArray(BigDecimal.class, "BIG_DECIMALS"));
        assertEquals(3,
                schema.getEntries()
                        .stream()
                        .filter(entry -> entry.getName().matches("hAshcOdEdIrtY|h|id"))
                        .count());
        // check list combinations
        assertEquals(INTEGERS, record.getArray(Integer.class, "array0"));
        assertEquals(LIST_INTEGERS, record.getArray(List.class, "array1"));
        assertEquals(LIST_HETEROGENEOUS_INTEGER, record.getArray(List.class, "array2"));
        assertEquals(LIST_HETEROGENEOUS_LIST, record.getArray(List.class, "array3"));
        assertEquals(LIST_INTEGERS_EMPTY, record.getArray(List.class, "array4"));
        assertEquals(LIST_3_DEEP, record.getArray(List.class, "array5"));
        // check their schemas
        assertSchemaArray0(schema);
        assertSchemaArray1(schema);
        assertSchemaArray2(schema);
        assertSchemaArray3(schema);
        assertSchemaArray4(schema);
        assertSchemaArray5(schema);

        // check we don't have any technical field in our schema/record
        assertEquals(0,
                schema
                        .getEntries()
                        .stream()
                        .filter(entry -> entry.getName().matches("hashCodeDirty|loopKey|lookKey"))
                        .count());
        assertThrows(NullPointerException.class, () -> record.getBoolean("hashCodeDirty"));
        assertNull(record.getString("loopKey"));
        assertNull(record.getString("lookKey"));
    }

    private static void assertSchemaArray0(final Schema schema) {
        final Entry schemaArray = schema.getEntry("array0");
        assertEquals(Type.ARRAY, schemaArray.getType());

        final Schema elSchema = schemaArray.getElementSchema();
        assertNotNull(elSchema);
        assertEquals(Type.INT, elSchema.getType());
    }

    private static void assertSchemaArray1(final Schema schema) {
        final Entry schemaArray = schema.getEntry("array1");
        assertEquals(Type.ARRAY, schemaArray.getType());

        final Schema elSchema = schemaArray.getElementSchema();
        assertNotNull(elSchema);
        assertEquals(Type.ARRAY, elSchema.getType());
        final Schema elSchemaNested = elSchema.getElementSchema();
        assertEquals(Type.INT, elSchemaNested.getType());
    }

    private static void assertSchemaArray2(final Schema schema) {
        final Entry schemaArray = schema.getEntry("array2");
        assertEquals(Type.ARRAY, schemaArray.getType());

        final Schema elSchema = schemaArray.getElementSchema();
        assertNotNull(elSchema);
        // this one, because we evaluate a schema by the first element in the List
        assertEquals(Type.INT, elSchema.getType());
    }

    private static void assertSchemaArray3(final Schema schema) {
        final Entry schemaArray = schema.getEntry("array3");
        assertEquals(Type.ARRAY, schemaArray.getType());

        final Schema elSchema = schemaArray.getElementSchema();
        assertNotNull(elSchema);
        assertEquals(Type.ARRAY, elSchema.getType());
        final Schema elSchemaNested = elSchema.getElementSchema();
        assertNotNull(elSchemaNested);
        assertEquals(Type.INT, elSchemaNested.getType());
    }

    private static void assertSchemaArray4(final Schema schema) {
        final Entry schemaArray = schema.getEntry("array4");
        assertEquals(Type.ARRAY, schemaArray.getType());

        final Schema elSchema = schemaArray.getElementSchema();
        assertNotNull(elSchema);
        assertEquals(Type.ARRAY, elSchema.getType());
        final Schema elSchemaNested = elSchema.getElementSchema();
        // the previous behavior when an empty element is evaluated with null element schema
        // it's kind of honest, but maybe we want anyway some type here. Maybe String?
        assertNull(elSchemaNested);
    }

    private static void assertSchemaArray5(final Schema schema) {
        final Entry schemaArray = schema.getEntry("array5");
        assertEquals(Type.ARRAY, schemaArray.getType());

        final Schema elSchema = schemaArray.getElementSchema();
        assertNotNull(elSchema);
        assertEquals(Type.ARRAY, elSchema.getType());
        final Schema elSchemaNested = elSchema.getElementSchema();
        assertNotNull(elSchemaNested);
        assertEquals(Type.ARRAY, elSchemaNested.getType());
        final Schema elSchemaNestedInNested = elSchemaNested.getElementSchema();
        assertNotNull(elSchemaNestedInNested);
        assertEquals(Type.INT, elSchemaNestedInNested.getType());
    }

    @Test
    void testLogicalType() {
        final DiRecordVisitorTest.RowStruct3 rowStruct = new DiRecordVisitorTest.RowStruct3();
        final Dynamic dynamic = new Dynamic();
        final Date value = new Date();
        createMetadata(dynamic, "date1", StudioTypes.DATE, value, null, false, SchemaProperty.LogicalType.DATE.key());
        createMetadata(dynamic, "time1", StudioTypes.DATE, value, null, false, SchemaProperty.LogicalType.TIME.key());
        createMetadata(dynamic, "timestamp1", StudioTypes.DATE, value, null, false,
                SchemaProperty.LogicalType.TIMESTAMP.key());
        createMetadata(dynamic, "date2", StudioTypes.DATE, value, null, false, null);
        rowStruct.dyn = dynamic;

        final DiRowStructVisitor visitor = new DiRowStructVisitor();
        final Schema schema = visitor.get(rowStruct, factory).getSchema();
        assertEquals(SchemaProperty.LogicalType.DATE.key(),
                schema.getEntry("date1").getLogicalType());
        assertEquals(SchemaProperty.LogicalType.TIME.key(),
                schema.getEntry("time1").getLogicalType());
        assertEquals(SchemaProperty.LogicalType.TIMESTAMP.key(),
                schema.getEntry("timestamp1").getLogicalType());
        assertNull(schema.getEntry("date2").getLogicalType());
    }

    @Test
    void visitEmptyAndNullFields() {
        final RowStructEmptyNull r1 = new RowStructEmptyNull();
        r1.meta_id = 1;
        r1.FirstName = "";
        r1.lastName = "";
        r1.City = "";
        r1.i = "";
        r1.A = "";
        final RowStructEmptyNull r2 = new RowStructEmptyNull();
        r2.meta_id = 2;
        r2.FirstName = "Bob";
        r2.lastName = "Kent";
        r2.City = "London";
        r2.i = "i";
        r2.A = "A";
        final RowStructEmptyNull r3 = new RowStructEmptyNull();
        r3.meta_id = 3;
        //
        final DiRowStructVisitor visitor = new DiRowStructVisitor();
        final Record rcd1 = visitor.get(r1, factory);
        final Record rcd2 = visitor.get(r2, factory);
        final Record rcd3 = visitor.get(r3, factory);
        assertEquals(1, rcd1.getInt("meta_id"));
        assertEquals("", rcd1.getString("FirstName"));
        assertEquals("", rcd1.getString("lastName"));
        assertEquals("", rcd1.getString("City"));
        assertEquals("", rcd1.getString("i"));
        assertEquals("", rcd1.getString("A"));
        assertEquals(2, rcd2.getInt("meta_id"));
        assertEquals("Bob", rcd2.getString("FirstName"));
        assertEquals("Kent", rcd2.getString("lastName"));
        assertEquals("London", rcd2.getString("City"));
        assertEquals("i", rcd2.getString("i"));
        assertEquals("A", rcd2.getString("A"));
        assertEquals(3, rcd3.getInt("meta_id"));
        assertNull(rcd3.getString("FirstName"));
        assertNull(rcd3.getString("lastName"));
        assertNull(rcd3.getString("City"));
        assertNull(rcd3.getString("i"));
        assertNull(rcd3.getString("A"));
    }

    public static class Rcd {

        public String str = "one";

        public int ntgr = 1;
    }

    @Data
    public static class RowStructEmptyNull implements routines.system.IPersistableRow {

        public Integer meta_id;

        public String FirstName;

        public String lastName;

        public String City;

        public String i;

        public String A;

        @Override
        public void writeData(final ObjectOutputStream objectOutputStream) {
            throw new UnsupportedOperationException("#writeData()");
        }

        @Override
        public void readData(final ObjectInputStream objectInputStream) {
            throw new UnsupportedOperationException("#readData()");
        }
    }
}