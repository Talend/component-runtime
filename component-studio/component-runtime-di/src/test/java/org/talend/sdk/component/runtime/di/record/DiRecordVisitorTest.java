/**
 * Copyright (C) 2006-2021 Talend Inc. - www.talend.com
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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.record.Schema.Type;

import lombok.extern.slf4j.Slf4j;

@Slf4j
class DiRecordVisitorTest extends VisitorsTest {

    @Test
    void visit() {
        final Record record = factory
                .newRecordBuilder()
                .withString("id", ":testing:")
                .withString("name", NAME)
           //  FIXME :    .withInt("shortP", SHORT)
           //  FIXME :   .withInt("shortC", SHORT)
                .withInt("intP", INT)
                .withInt("intC", INT)
                .withLong("longP", LONG)
                .withLong("longC", LONG)
                .withFloat("floatP", FLOAT)
                .withFloat("floatC", FLOAT)
                .withDouble("doubleP", DOUBLE)
                .withDouble("doubleC", DOUBLE)
                .withBytes("bytes0", BYTES0)
                .withString("bytes1", new String(BYTES1))
                .withDateTime("date0", DATE)
                .withString("date1", ZONED_DATE_TIME.toString())
                .withDateTime("date2", ZONED_DATE_TIME)
                .withLong("date3", ZONED_DATE_TIME.toInstant().toEpochMilli())
                .withString("bigDecimal0", BIGDEC.toString())
                .withBoolean("bool1", true)
                .withString("dynString", "stringy")
                .withInt("dynInteger", INT)
                .withDouble("dynDouble", DOUBLE)
                .withBytes("dynBytes", BYTES0)
                .withBytes("dynBytesArray", BYTES0)
                .withBytes("dynBytesBuffer", ByteBuffer.allocate(100).wrap(BYTES0).array())
                .withBytes("dynBytesWString", String.valueOf(BYTES0).getBytes())
                .withRecord("object0", RECORD)
                .withRecord("RECORD", RECORD)
                .withArray(factory
                        .newEntryBuilder()
                        .withName("array0")
                        .withType(Type.ARRAY)
                        .withElementSchema(factory.newSchemaBuilder(Type.INT).build())
                        .build(), INTEGERS)
                .withArray(factory
                        .newEntryBuilder()
                        .withName("STRINGS")
                        .withType(Type.ARRAY)
                        .withElementSchema(factory.newSchemaBuilder(Type.STRING).build())
                        .build(), STRINGS)
                .withArray(factory
                        .newEntryBuilder()
                        .withName("LONGS")
                        .withType(Type.ARRAY)
                        .withElementSchema(factory.newSchemaBuilder(Type.LONG).build())
                        .build(), LONGS)
                .withArray(factory
                        .newEntryBuilder()
                        .withName("FLOATS")
                        .withType(Type.ARRAY)
                        .withElementSchema(factory.newSchemaBuilder(Type.FLOAT).build())
                        .build(), FLOATS)
                .withArray(factory
                        .newEntryBuilder()
                        .withName("DOUBLES")
                        .withType(Type.ARRAY)
                        .withElementSchema(factory.newSchemaBuilder(Type.DOUBLE).build())
                        .build(), DOUBLES)
                .withArray(factory
                        .newEntryBuilder()
                        .withName("BOOLEANS")
                        .withType(Type.ARRAY)
                        .withElementSchema(factory.newSchemaBuilder(Type.BOOLEAN).build())
                        .build(), BOOLEANS)
                .withArray(factory
                        .newEntryBuilder()
                        .withName("BYTES")
                        .withType(Type.ARRAY)
                        .withElementSchema(factory.newSchemaBuilder(Type.BYTES).build())
                        .build(), BYTES)
                .withArray(factory
                        .newEntryBuilder()
                        .withName("DATES")
                        .withType(Type.ARRAY)
                        .withElementSchema(factory.newSchemaBuilder(Type.DATETIME).build())
                        .build(), DATES)
                .withArray(factory
                        .newEntryBuilder()
                        .withName("RECORDS")
                        .withType(Type.ARRAY)
                        .withElementSchema(factory.newSchemaBuilder(Type.RECORD).build())
                        .build(), RECORDS)
                //
                .build();
        //
        final DiRecordVisitor visitor = new DiRecordVisitor(RowStruct.class, Collections.emptyMap());
        final RowStruct rowStruct = RowStruct.class.cast(visitor.visit(record));
        assertNotNull(rowStruct);
        log.info("[get] {}", rowStruct);
        // asserts rowStruct::members
        assertEquals(":testing:", rowStruct.id);
        assertEquals(NAME, rowStruct.name);
        assertEquals(SHORT, rowStruct.shortP);
        assertEquals(SHORT, rowStruct.shortC);
        assertEquals(INT, rowStruct.intP);
        assertEquals(INT, rowStruct.intC);
        assertEquals(LONG, rowStruct.longP);
        assertEquals(LONG, rowStruct.longC);
        assertEquals(FLOAT, rowStruct.floatP);
        assertEquals(FLOAT, rowStruct.floatC);
        assertEquals(DOUBLE, rowStruct.doubleP);
        assertEquals(DOUBLE, rowStruct.doubleC);
        assertEquals(DATE.toInstant(), rowStruct.date0.toInstant());
        assertEquals(ZONED_DATE_TIME.toInstant(), rowStruct.date1.toInstant());
        assertEquals(ZONED_DATE_TIME.toInstant(), rowStruct.date2.toInstant());
        assertEquals(ZONED_DATE_TIME.toInstant(), rowStruct.date3.toInstant());
        assertEquals(BIGDEC.doubleValue(), rowStruct.bigDecimal0.doubleValue());
        assertEquals(BIGDEC, rowStruct.bigDecimal0);
        assertFalse(rowStruct.bool0);
        assertTrue(rowStruct.bool1);
        assertArrayEquals(BYTES0, rowStruct.bytes0);
        assertArrayEquals(BYTES1, rowStruct.bytes1);
        assertEquals(RECORD, rowStruct.object0);
        // asserts rowStruct::dynamic
        assertNotNull(rowStruct.dynamic);
        assertNotNull(rowStruct.dynamic.metadatas);
        assertArrayEquals(BYTES0, (byte[]) rowStruct.dynamic.getColumnValue("dynBytes"));
        assertArrayEquals(BYTES0, (byte[]) rowStruct.dynamic.getColumnValue("dynBytesArray"));
        assertArrayEquals(BYTES0, (byte[]) rowStruct.dynamic.getColumnValue("dynBytesBuffer"));
        assertArrayEquals(String.valueOf(BYTES0).getBytes(),
                (byte[]) rowStruct.dynamic.getColumnValue("dynBytesWString"));
        assertEquals(INTEGERS, rowStruct.array0);
        assertEquals(RECORD, rowStruct.dynamic.getColumnValue("RECORD"));
        assertEquals("one", ((Record) rowStruct.dynamic.getColumnValue("RECORD")).getString("str"));
        assertEquals(1, ((Record) rowStruct.dynamic.getColumnValue("RECORD")).getInt("ntgr"));
        assertEquals(STRINGS, rowStruct.dynamic.getColumnValue("STRINGS"));
        assertEquals(LONGS, rowStruct.dynamic.getColumnValue("LONGS"));
        assertEquals(FLOATS, rowStruct.dynamic.getColumnValue("FLOATS"));
        assertEquals(DOUBLES, rowStruct.dynamic.getColumnValue("DOUBLES"));
        assertEquals(BOOLEANS, rowStruct.dynamic.getColumnValue("BOOLEANS"));
        assertEquals(BYTES, rowStruct.dynamic.getColumnValue("BYTES"));
        assertEquals(DATES, rowStruct.dynamic.getColumnValue("DATES"));
        assertEquals(RECORDS, rowStruct.dynamic.getColumnValue("RECORDS"));
        final List<Record> records = (List<Record>) rowStruct.dynamic.getColumnValue("RECORDS");
        records.forEach(r -> {
            assertEquals(1, r.getInt("ntgr"));
            assertEquals("one", r.getString("str"));
        });

    }
}