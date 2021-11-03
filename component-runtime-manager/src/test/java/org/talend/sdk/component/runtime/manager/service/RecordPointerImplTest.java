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
package org.talend.sdk.component.runtime.manager.service;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.runtime.record.RecordImpl;
import org.talend.sdk.component.runtime.record.SchemaImpl;

class RecordPointerImplTest {

    @Test
    void extractFlat() {
        final Record record = new RecordImpl.BuilderImpl()
                .withString("foo", "foo_1234")
                .withInt("bar", 1324)
                .withArray(new Schema.Entry.Builder() //
                        .withName("array1") //
                        .withRawName("array1") //
                        .withType(Schema.Type.ARRAY) //
                        .withNullable(false) //
                        .withElementSchema(new SchemaImpl.BuilderImpl().withType(Schema.Type.STRING).build()) //
                        .build(), asList("a", "b"))
                .withArray(new Schema.Entry.Builder() //
                        .withName("array2")
                        .withRawName("array2")
                        .withType(Schema.Type.ARRAY) //
                        .withNullable(false)
                        .withElementSchema(new SchemaImpl.BuilderImpl()
                                .withType(Schema.Type.RECORD)
                                .withEntry(new Schema.Entry.Builder() //
                                        .withName("item") //
                                        .withRawName("item") //
                                        .withType(Schema.Type.STRING) //
                                        .withNullable(false)//
                                        .build())
                                .build())
                        .build(), //
                        asList(new RecordImpl.BuilderImpl().withString("v1", "first").build(),
                                new RecordImpl.BuilderImpl().withString("v2", "second").build()) //
                )
                .build();
        assertEquals(record, new RecordPointerFactoryImpl("test").apply("").getValue(record, Object.class));
        assertEquals(record, new RecordPointerFactoryImpl("test").apply("/").getValue(record, Object.class));
        assertEquals(record.getString("foo"),
                new RecordPointerFactoryImpl("test").apply("/foo").getValue(record, Object.class));
        assertEquals(record.getInt("bar"),
                new RecordPointerFactoryImpl("test").apply("/bar").getValue(record, Object.class));
        assertEquals("a", new RecordPointerFactoryImpl("test").apply("/array1/0").getValue(record, Object.class));
        assertEquals("b", new RecordPointerFactoryImpl("test").apply("/array1/1").getValue(record, Object.class));
        assertEquals("first",
                new RecordPointerFactoryImpl("test").apply("/array2/0/v1").getValue(record, Object.class));
        assertEquals("second",
                new RecordPointerFactoryImpl("test").apply("/array2/1/v2").getValue(record, Object.class));
        assertThrows(IllegalArgumentException.class,
                () -> new RecordPointerFactoryImpl("test").apply("/array2/1/v1").getValue(record, Object.class));
    }
}
