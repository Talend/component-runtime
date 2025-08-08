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
import org.talend.sdk.component.runtime.di.schema.StudioTypes;

import routines.system.Document;
import routines.system.Dynamic;
import routines.system.DynamicMetadata;

import lombok.Data;
import lombok.Getter;

class DiRowStructVisitorTest extends VisitorsTest {

    private void createMetadata(final Dynamic dynamic, final String name, final String type, final Object value) {
        createMetadata(dynamic, name, type, value, false);
    }

    private void createMetadata(final Dynamic dynamic, final String name, final String type, final Object value,
            final boolean isKey) {
        createMetadata(dynamic, name, type, value, null, isKey);
    }

    private void createMetadata(final Dynamic dynamic, final String name, final String type, final Object value,
            final String datePattern, final boolean isKey) {
        createMetadata(dynamic, name, type, value, datePattern, isKey, null);
    }

    private void createMetadata(final Dynamic dynamic, final String name, final String type, final Object value,
            final String datePattern, final boolean isKey, final String dbName) {
        final DynamicMetadata meta = new DynamicMetadata();
        meta.setName(name);
        meta.setType(type);
        meta.setKey(isKey);
        meta.setFormat(datePattern);
        meta.setDbName(dbName);
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
                "2010-01-31", "yyyy-MM-dd", false);
        rowStruct.dynamic = dynamic;

        org.dom4j.Document doc = DocumentHelper.createDocument();
        doc.addElement("catalog").addComment("an XML catalog");
        DOCUMENT.setDocument(doc);
        rowStruct.document.setDocument(doc);
        rowStruct.emptyDocument = new Document();

        final DiRowStructVisitor visitor = new DiRowStructVisitor();
        final Record convertedRecord = visitor.get(rowStruct, factory);
        final Schema schema = convertedRecord.getSchema();
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

        // check list without original name
        final Entry listEntry = schema.getEntry("array0");
        assertEquals("id_List", listEntry.getProp(STUDIO_TYPE));
        assertEquals("array0", listEntry.getName());
        assertEquals("array0", listEntry.getOriginalFieldName());
        assertNull(listEntry.getRawName());

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
        assertEquals(":testing:", convertedRecord.getString("id"));
        assertEquals(NAME, convertedRecord.getString("name"));
        assertEquals(SHORT, convertedRecord.getInt("shortP"));
        assertEquals(SHORT, convertedRecord.getInt("shortC"));
        assertEquals(INT, convertedRecord.getInt("intP"));
        assertEquals(INT, convertedRecord.getInt("intC"));
        assertEquals(LONG, convertedRecord.getLong("longP"));
        assertEquals(LONG, convertedRecord.getLong("longC"));
        assertEquals(FLOAT, convertedRecord.getFloat("floatP"));
        assertEquals(FLOAT, convertedRecord.getFloat("floatC"));
        assertEquals(DOUBLE, convertedRecord.getDouble("doubleP"));
        assertEquals(DOUBLE, convertedRecord.getDouble("doubleC"));
        assertEquals(DATE.toInstant(), convertedRecord.getDateTime("date0").toInstant());
        assertNull(convertedRecord.getDateTime("date1"));
        assertEquals(ZONED_DATE_TIME, convertedRecord.getDateTime("date2"));
        assertEquals(1946, convertedRecord.getDateTime("date2").getYear());
        assertEquals(INSTANT, convertedRecord.getInstant("date4"));
        assertEquals(BIGDEC.doubleValue(), new BigDecimal(convertedRecord.getString("bigDecimal0")).doubleValue());
        assertEquals(BIGDEC.toString(), convertedRecord.getString("bigDecimal0"));
        assertFalse(convertedRecord.getBoolean("bool0"));
        assertTrue(convertedRecord.getBoolean("bool1"));
        assertArrayEquals(BYTES0, convertedRecord.getBytes("bytes0"));
        assertArrayEquals(BYTES0, convertedRecord.getBytes("dynBytes"));
        assertArrayEquals(BYTES0, convertedRecord.getBytes("dynBytesArray"));
        assertArrayEquals(BYTES0, convertedRecord.getBytes("dynBytesBuffer"));
        assertArrayEquals(String.valueOf(BYTES0).getBytes(), convertedRecord.getBytes("dynBytesWString"));
        assertEquals(BIGDEC.toString(), convertedRecord.getString("dynBigDecimal"));
        assertEquals(BIGDEC, new BigDecimal(convertedRecord.getString("dynBigDecimal")));
        assertEquals(DOCUMENT.toString(), convertedRecord.getString("dynDocument"));
        assertEquals(rowStruct.object0, convertedRecord.get(Object.class, "object0"));
        assertTrue(convertedRecord.getBoolean("hAshcOdEdIrtY"));
        assertEquals(NAME, convertedRecord.getString("h"));
        assertEquals(StudioTypes.CHARACTER, schema.getEntry("char0").getProp(STUDIO_TYPE));
        assertEquals(String.valueOf(Character.MAX_VALUE), convertedRecord.getString("char0"));
        assertEquals(dynObject, convertedRecord.get(Object.class, "dynObject"));
        assertEquals(STRINGS, convertedRecord.getArray(String.class, "STRINGS"));
        assertEquals(LONGS, convertedRecord.getArray(Long.class, "LONGS"));
        assertEquals(FLOATS, convertedRecord.getArray(Float.class, "FLOATS"));
        assertEquals(DOUBLES, convertedRecord.getArray(Double.class, "DOUBLES"));
        assertEquals(BOOLEANS, convertedRecord.getArray(Boolean.class, "BOOLEANS"));
        assertEquals(BYTES, convertedRecord.getArray(byte[].class, "BYTES"));
        assertEquals(DATES, convertedRecord.getArray(ZonedDateTime.class, "DATES"));
        assertEquals(RECORDS, convertedRecord.getArray(Record.class, "RECORDS"));
        convertedRecord.getArray(Record.class, "RECORDS").forEach(r -> {
            assertEquals(1, r.getInt("ntgr"));
            assertEquals("one", r.getString("str"));
        });
        assertEquals(BIG_DECIMALS, convertedRecord.getArray(BigDecimal.class, "BIG_DECIMALS"));
        assertEquals(3,
                schema.getEntries()
                        .stream()
                        .filter(entry -> entry.getName().matches("hAshcOdEdIrtY|h|id"))
                        .count());
        // check list combinations
        assertEquals(INTEGERS, convertedRecord.getArray(Integer.class, "array0"));
        assertEquals(LIST_INTEGERS, convertedRecord.getArray(List.class, "array1"));
        assertEquals(LIST_HETEROGENEOUS_INTEGER, convertedRecord.getArray(List.class, "array2"));
        assertEquals(LIST_HETEROGENEOUS_LIST, convertedRecord.getArray(List.class, "array3"));
        assertEquals(LIST_INTEGERS_EMPTY, convertedRecord.getArray(List.class, "array4"));
        assertEquals(LIST_3_DEEP, convertedRecord.getArray(List.class, "array5"));
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
        assertThrows(NullPointerException.class, () -> convertedRecord.getBoolean("hashCodeDirty"));
        assertNull(convertedRecord.getString("loopKey"));
        assertNull(convertedRecord.getString("lookKey"));
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

    @Test
    void visitArrayFieldsOriginalNameInDynamic() {
        @Getter
        class RowStructLocal implements routines.system.IPersistableRow {

            public Dynamic dynamic;

            public Integer dynamicLength() {
                return 20;
            }

            public Integer dynamicPrecision() {
                return 10;
            }

            @Override
            public void writeData(final ObjectOutputStream objectOutputStream) {
                throw new UnsupportedOperationException("#writeData()");
            }

            @Override
            public void readData(final ObjectInputStream objectInputStream) {
                throw new UnsupportedOperationException("#readData()");
            }
        }

        final RowStructLocal rowStruct = new RowStructLocal();

        // dynamic
        rowStruct.dynamic = new Dynamic();
        final String name1 = "STRINGS";
        final String name2 = "___";
        final String name3 = "____1";
        final String originalName2 = "санітизованийСписок";
        final String originalName3 = "санітизованийСписок";
        createMetadata(rowStruct.dynamic, name1, StudioTypes.LIST, STRINGS);
        createMetadata(rowStruct.dynamic, name2, StudioTypes.LIST, STRINGS, null, false, originalName2);
        createMetadata(rowStruct.dynamic, name3, StudioTypes.LIST, STRINGS, null, true, originalName3);

        final DiRowStructVisitor visitor = new DiRowStructVisitor();
        final Record convertedRecord = visitor.get(rowStruct, factory);

        // validation
        // schema
        final Schema schema = convertedRecord.getSchema();
        assertEquals(3, schema.getEntries().size());
        {
            final Entry testedEntry = schema.getEntry(name1);
            assertTrue(testedEntry.isNullable());
            assertEquals(Type.ARRAY, testedEntry.getType());
            assertEquals(StudioTypes.LIST, testedEntry.getProp(STUDIO_TYPE));
            assertEquals(name1, testedEntry.getOriginalFieldName());
            // expected to fail, when list start to populate those properties
            assertNull(testedEntry.getProp(IS_KEY));
            assertNull(testedEntry.getProp(SIZE));
            assertNull(testedEntry.getProp(SCALE));
        }
        {
            final Entry testedEntry = schema.getEntry(name2);
            assertTrue(testedEntry.isNullable());
            assertEquals(Type.ARRAY, testedEntry.getType());
            assertEquals(StudioTypes.LIST, testedEntry.getProp(STUDIO_TYPE));
            assertEquals(originalName2, testedEntry.getOriginalFieldName());
            // expected to fail, when list start to populate those properties
            assertNull(testedEntry.getProp(IS_KEY));
            assertNull(testedEntry.getProp(SIZE));
            assertNull(testedEntry.getProp(SCALE));
        }
        {
            final Entry testedEntry = schema.getEntry(name2);
            assertTrue(testedEntry.isNullable());
            assertEquals(Type.ARRAY, testedEntry.getType());
            assertEquals(StudioTypes.LIST, testedEntry.getProp(STUDIO_TYPE));
            assertEquals(originalName3, testedEntry.getOriginalFieldName());
            // expected to fail, when list start to populate those properties
            assertNull(testedEntry.getProp(IS_KEY));
            assertNull(testedEntry.getProp(SIZE));
            assertNull(testedEntry.getProp(SCALE));
        }

        // value
        assertEquals(STRINGS, convertedRecord.getArray(String.class, name1));
        assertEquals(STRINGS, convertedRecord.getArray(String.class, name2));
        assertEquals(STRINGS, convertedRecord.getArray(String.class, name3));
    }

    @Test
    void visitDecimalDynamicWithLengthAndScale() {
        @Getter
        class RowStructLocal implements routines.system.IPersistableRow {

            public Dynamic dynamic;

            public Integer dynamicLength() {
                return 20;
            }

            public Integer dynamicPrecision() {
                return 10;
            }

            @Override
            public void writeData(final ObjectOutputStream objectOutputStream) {
                throw new UnsupportedOperationException("#writeData()");
            }

            @Override
            public void readData(final ObjectInputStream objectInputStream) {
                throw new UnsupportedOperationException("#readData()");
            }
        }

        final RowStructLocal rowStruct = new RowStructLocal();

        // dynamic
        rowStruct.dynamic = new Dynamic();
        final String name1 = "someValue";
        final String name2 = "anotherValue";
        createMetadata(rowStruct.dynamic, name1, StudioTypes.BIGDECIMAL, BigDecimal.valueOf(42L));
        createMetadata(rowStruct.dynamic, name2, StudioTypes.BIGDECIMAL, BigDecimal.valueOf(42L), null, true);
        rowStruct.dynamic.metadatas.get(1).setLength(17);
        rowStruct.dynamic.metadatas.get(1).setPrecision(7);

        final DiRowStructVisitor visitor = new DiRowStructVisitor();
        final Record convertedRecord = visitor.get(rowStruct, factory);

        // validation
        // schema
        final Schema schema = convertedRecord.getSchema();
        assertEquals(2, schema.getEntries().size());
        {
            final Entry testedEntry = schema.getEntry(name1);
            assertTrue(testedEntry.isNullable());
            assertEquals(Type.DECIMAL, testedEntry.getType());
            assertEquals(StudioTypes.BIGDECIMAL, testedEntry.getProp(STUDIO_TYPE));
            assertEquals(name1, testedEntry.getOriginalFieldName());
            assertNull(testedEntry.getRawName());
            assertEquals("false", testedEntry.getProp(IS_KEY));
            assertEquals("20", testedEntry.getProp(SIZE));
            assertEquals("10", testedEntry.getProp(SCALE));
        }
        {
            final Entry testedEntry = schema.getEntry(name2);
            assertTrue(testedEntry.isNullable());
            assertEquals(Type.DECIMAL, testedEntry.getType());
            assertEquals(StudioTypes.BIGDECIMAL, testedEntry.getProp(STUDIO_TYPE));
            assertEquals(name2, testedEntry.getOriginalFieldName());
            assertNull(testedEntry.getRawName());
            assertEquals("true", testedEntry.getProp(IS_KEY));
            assertEquals("17", testedEntry.getProp(SIZE));
            assertEquals("7", testedEntry.getProp(SCALE));
        }

        // value
        assertEquals(BigDecimal.valueOf(42L), convertedRecord.getDecimal(name1));
        assertEquals(BigDecimal.valueOf(42L), convertedRecord.getDecimal(name2));
    }

    @Test
    void visitDecimalDynamicWithNullValue() {
        @Getter
        class RowStructLocal implements routines.system.IPersistableRow {

            public Dynamic dynamic;

            @Override
            public void writeData(final ObjectOutputStream objectOutputStream) {
                throw new UnsupportedOperationException("#writeData()");
            }

            @Override
            public void readData(final ObjectInputStream objectInputStream) {
                throw new UnsupportedOperationException("#readData()");
            }
        }

        final RowStructLocal rowStruct = new RowStructLocal();

        // dynamic
        rowStruct.dynamic = new Dynamic();
        final String name1 = "someValue";
        final String name2 = "anotherValue";
        createMetadata(rowStruct.dynamic, name1, StudioTypes.BIGDECIMAL, null);
        createMetadata(rowStruct.dynamic, name2, StudioTypes.BIGDECIMAL, BigDecimal.valueOf(42L));

        final DiRowStructVisitor visitor = new DiRowStructVisitor();
        final Record convertedRecord = visitor.get(rowStruct, factory);

        // validation
        // schema
        final Schema schema = convertedRecord.getSchema();
        assertEquals(2, schema.getEntries().size());
        {
            final Entry testedEntry = schema.getEntry(name1);
            assertTrue(testedEntry.isNullable());
            assertEquals(Type.DECIMAL, testedEntry.getType());
            assertEquals(StudioTypes.BIGDECIMAL, testedEntry.getProp(STUDIO_TYPE));
            assertEquals(name1, testedEntry.getOriginalFieldName());
            assertNull(testedEntry.getRawName());
            assertEquals("false", testedEntry.getProp(IS_KEY));
            assertEquals("-1", testedEntry.getProp(SIZE));
            assertEquals("-1", testedEntry.getProp(SCALE));
        }
        {
            final Entry testedEntry = schema.getEntry(name2);
            assertTrue(testedEntry.isNullable());
            assertEquals(Type.DECIMAL, testedEntry.getType());
            assertEquals(StudioTypes.BIGDECIMAL, testedEntry.getProp(STUDIO_TYPE));
            assertEquals(name2, testedEntry.getOriginalFieldName());
            assertNull(testedEntry.getRawName());
            assertEquals("false", testedEntry.getProp(IS_KEY));
            assertEquals("-1", testedEntry.getProp(SIZE));
            assertEquals("-1", testedEntry.getProp(SCALE));
        }

        // value
        assertTrue(convertedRecord.getOptionalDecimal(name1).isEmpty());
        assertEquals(BigDecimal.valueOf(42L), convertedRecord.getDecimal(name2));
    }

    @Test
    void visitArrayFieldsOriginalName() {
        @Getter
        class RowStructLocal implements routines.system.IPersistableRow {

            public List<Integer> array0;

            public List<List<Integer>> array1;

            public String array1OriginalDbColumnName() {
                return "Список1";
            }

            @Override
            public void writeData(final ObjectOutputStream objectOutputStream) {
                throw new UnsupportedOperationException("#writeData()");
            }

            @Override
            public void readData(final ObjectInputStream objectInputStream) {
                throw new UnsupportedOperationException("#readData()");
            }
        }

        final RowStructLocal rowStruct = new RowStructLocal();
        rowStruct.array0 = INTEGERS;
        rowStruct.array1 = LIST_INTEGERS;

        final DiRowStructVisitor visitor = new DiRowStructVisitor();
        final Record convertedRecord = visitor.get(rowStruct, factory);

        final Schema schema = convertedRecord.getSchema();
        assertEquals(2, schema.getEntries().size());

        // schema metadata
        // check list without original name
        {
            final Entry listEntry = schema.getEntry("array0");
            assertEquals("id_List", listEntry.getProp(STUDIO_TYPE));
            assertEquals("array0", listEntry.getName());
            assertEquals("array0", listEntry.getOriginalFieldName());
            assertNull(listEntry.getRawName());
        }

        // check list with original name
        {
            final Entry listEntry2 = schema.getEntry("array1");
            assertEquals("id_List", listEntry2.getProp(STUDIO_TYPE));
            assertEquals("array1", listEntry2.getName());
            assertEquals("Список1", listEntry2.getOriginalFieldName());
            assertEquals("Список1", listEntry2.getRawName());
        }

        // asserts Record
        // check list combinations
        assertEquals(INTEGERS, convertedRecord.getArray(Integer.class, "array0"));
        assertEquals(LIST_INTEGERS, convertedRecord.getArray(List.class, "array1"));

        // check their schemas
        assertSchemaArray0(schema);
        assertSchemaArray1(schema);
    }

    public static class Rcd {

        public String str = "one";

        public int ntgr = 1;
    }

    @Data
    public static class RowStructEmptyNull implements routines.system.IPersistableRow {

        public Integer meta_id; //NOSONAR

        public String FirstName; //NOSONAR

        public String lastName;

        public String City; //NOSONAR

        public String i;

        public String A; //NOSONAR

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