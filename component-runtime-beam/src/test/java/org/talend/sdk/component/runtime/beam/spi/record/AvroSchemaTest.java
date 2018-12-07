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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.talend.sdk.component.api.record.Schema.Type.DATETIME;
import static org.talend.sdk.component.api.record.Schema.Type.RECORD;
import static org.talend.sdk.component.api.record.Schema.Type.STRING;

import java.util.Iterator;

import org.apache.avro.Schema;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;
import org.talend.sdk.component.runtime.beam.spi.AvroRecordBuilderFactoryProvider;

class AvroSchemaTest {

    @Test
    void emptySchema() {
        final org.apache.avro.Schema avro =
                AvroSchema.class.cast(new AvroSchemaBuilder().withType(RECORD).build()).getDelegate();
        assertTrue(avro.getFields().isEmpty());
    }

    @Test
    void checkDateConversion() {
        final RecordBuilderFactory factory = new AvroRecordBuilderFactoryProvider().apply("test");
        final org.apache.avro.Schema avro = AvroSchema.class
                .cast(new AvroSchemaBuilder()
                        .withType(RECORD)
                        .withEntry(factory.newEntryBuilder().withType(DATETIME).withName("date").build())
                        .build())
                .getDelegate();
        assertEquals(DATETIME, new AvroSchema(avro).getEntries().iterator().next().getType());
    }

    @Test
    void ensureNullableArePropagated() {
        final RecordBuilderFactory factory = new AvroRecordBuilderFactoryProvider().apply("test");
        { // nullable = true
            final org.talend.sdk.component.api.record.Schema sdkSchema = new AvroSchemaBuilder()
                    .withType(RECORD)
                    .withEntry(factory.newEntryBuilder().withType(STRING).withName("name").withNullable(true).build())
                    .build();
            final org.apache.avro.Schema avro = AvroSchema.class.cast(sdkSchema).getDelegate();
            final Schema schema = avro.getFields().iterator().next().schema();
            assertEquals(2, schema.getTypes().size());
            final Iterator<Schema> types = schema.getTypes().iterator();
            assertEquals(Schema.Type.NULL, types.next().getType());
            assertEquals(Schema.Type.STRING, types.next().getType());
            assertTrue(sdkSchema.getEntries().iterator().next().isNullable());
        }
        { // nullable = false
            final org.talend.sdk.component.api.record.Schema sdkSchema = new AvroSchemaBuilder()
                    .withType(RECORD)
                    .withEntry(factory.newEntryBuilder().withType(STRING).withName("name").withNullable(false).build())
                    .build();
            assertFalse(sdkSchema.getEntries().iterator().next().isNullable());
        }
    }
}
