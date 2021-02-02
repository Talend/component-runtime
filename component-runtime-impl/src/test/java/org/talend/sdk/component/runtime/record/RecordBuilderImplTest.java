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
package org.talend.sdk.component.runtime.record;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.function.Supplier;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.record.Schema;

class RecordBuilderImplTest {

    @Test
    void providedSchemaGetSchema() {
        final Schema schema = new SchemaImpl.BuilderImpl()
                .withType(Schema.Type.RECORD)
                .withEntry(new SchemaImpl.EntryImpl.BuilderImpl()
                        .withName("name")
                        .withNullable(true)
                        .withType(Schema.Type.STRING)
                        .build())
                .build();
        assertEquals(schema, new RecordImpl.BuilderImpl(schema).withString("name", "ok").build().getSchema());
    }

    @Test
    void recordEntryFromName() {
        final Schema schema = new SchemaImpl.BuilderImpl()
                .withType(Schema.Type.RECORD)
                .withEntry(new SchemaImpl.EntryImpl.BuilderImpl()
                        .withName("name")
                        .withNullable(true)
                        .withType(Schema.Type.STRING)
                        .build())
                .build();
        assertEquals("{\"record\":{\"name\":\"ok\"}}",
                new RecordImpl.BuilderImpl()
                        .withRecord("record", new RecordImpl.BuilderImpl(schema).withString("name", "ok").build())
                        .build()
                        .toString());
    }

    @Test
    void providedSchemaNullable() {
        final Supplier<RecordImpl.BuilderImpl> builder = () -> new RecordImpl.BuilderImpl(new SchemaImpl.BuilderImpl()
                .withType(Schema.Type.RECORD)
                .withEntry(new SchemaImpl.EntryImpl.BuilderImpl()
                        .withName("name")
                        .withNullable(true)
                        .withType(Schema.Type.STRING)
                        .build())
                .build());
        { // normal/valued
            final Record record = builder.get().withString("name", "ok").build();
            assertEquals(1, record.getSchema().getEntries().size());
            assertEquals("ok", record.getString("name"));
        }
        { // null
            final Record record = builder.get().withString("name", null).build();
            assertEquals(1, record.getSchema().getEntries().size());
            assertNull(record.getString("name"));
        }
        { // missing entry in the schema
            assertThrows(IllegalArgumentException.class, () -> builder.get().withString("name2", null).build());
        }
        { // invalid type entry
            assertThrows(IllegalArgumentException.class, () -> builder.get().withInt("name", 2).build());
        }
    }

    @Test
    void providedSchemaNotNullable() {
        final Supplier<RecordImpl.BuilderImpl> builder = () -> new RecordImpl.BuilderImpl(new SchemaImpl.BuilderImpl()
                .withType(Schema.Type.RECORD)
                .withEntry(new SchemaImpl.EntryImpl.BuilderImpl()
                        .withName("name")
                        .withNullable(false)
                        .withType(Schema.Type.STRING)
                        .build())
                .build());
        { // normal/valued
            final Record record = builder.get().withString("name", "ok").build();
            assertEquals(1, record.getSchema().getEntries().size());
            assertEquals("ok", record.getString("name"));
        }
        { // null
            assertThrows(IllegalArgumentException.class, () -> builder.get().withString("name", null).build());
        }
        { // missing entry value
            assertThrows(IllegalArgumentException.class, () -> builder.get().build());
        }
    }

    @Test
    void nullSupportString() {
        final RecordImpl.BuilderImpl builder = new RecordImpl.BuilderImpl();
        builder.withString("test", null);
        final Record record = builder.build();
        assertEquals(1, record.getSchema().getEntries().size());
        assertNull(record.getString("test"));
    }

    @Test
    void nullSupportDate() {
        final RecordImpl.BuilderImpl builder = new RecordImpl.BuilderImpl();
        builder.withDateTime("test", (Date) null);
        final Record record = builder.build();
        assertEquals(1, record.getSchema().getEntries().size());
        assertNull(record.getDateTime("test"));
    }

    @Test
    void nullSupportBytes() {
        final RecordImpl.BuilderImpl builder = new RecordImpl.BuilderImpl();
        builder.withBytes("test", null);
        final Record record = builder.build();
        assertEquals(1, record.getSchema().getEntries().size());
        assertNull(record.getBytes("test"));
    }

    @Test
    void nullSupportCollections() {
        final RecordImpl.BuilderImpl builder = new RecordImpl.BuilderImpl();
        builder
                .withArray(new SchemaImpl.EntryImpl("test", "test", Schema.Type.ARRAY, true, null,
                        new SchemaImpl(Schema.Type.STRING, null, null), null), null);
        final Record record = builder.build();
        assertEquals(1, record.getSchema().getEntries().size());
        assertNull(record.getArray(String.class, "test"));
    }

    @Test
    void notNullableNullBehavior() {
        final RecordImpl.BuilderImpl builder = new RecordImpl.BuilderImpl();
        assertThrows(IllegalArgumentException.class, () -> builder
                .withString(new SchemaImpl.EntryImpl.BuilderImpl().withNullable(false).withName("test").build(), null));
    }

    @Test
    void dateTime() {
        final Schema schema = new SchemaImpl.BuilderImpl()
                .withType(Schema.Type.RECORD)
                .withEntry(new SchemaImpl.EntryImpl.BuilderImpl()
                        .withName("date")
                        .withNullable(false)
                        .withType(Schema.Type.DATETIME)
                        .build())
                .build();
        final RecordImpl.BuilderImpl builder = new RecordImpl.BuilderImpl(schema);
        final Record record = builder.withDateTime("date", ZonedDateTime.now()).build();
        Assertions.assertNotNull(record.getDateTime("date"));

        final RecordImpl.BuilderImpl builder2 = new RecordImpl.BuilderImpl(schema);
        assertThrows(IllegalArgumentException.class, () -> builder2.withDateTime("date", (ZonedDateTime) null));
    }

    @Test
    void array() {
        final Schema schemaArray = new SchemaImpl.BuilderImpl().withType(Schema.Type.STRING).build();
        final Schema.Entry entry = new SchemaImpl.EntryImpl.BuilderImpl()
                .withName("data")
                .withNullable(false)
                .withType(Schema.Type.ARRAY)
                .withElementSchema(schemaArray)
                .build();
        final Schema schema = new SchemaImpl.BuilderImpl().withType(Schema.Type.RECORD).withEntry(entry).build();
        final RecordImpl.BuilderImpl builder = new RecordImpl.BuilderImpl(schema);

        final Record record = builder.withArray(entry, Arrays.asList("d1", "d2")).build();
        final Collection<String> data = record.getArray(String.class, "data");
        Assertions.assertEquals(2, data.size());
    }
}
