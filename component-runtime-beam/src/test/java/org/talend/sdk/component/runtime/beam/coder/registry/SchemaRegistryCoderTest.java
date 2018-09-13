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
package org.talend.sdk.component.runtime.beam.coder.registry;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.runtime.beam.spi.record.AvroRecord;
import org.talend.sdk.component.runtime.record.RecordImpl;
import org.talend.sdk.component.runtime.record.SchemaImpl;

class SchemaRegistryCoderTest {

    @Test
    void codecString() throws IOException {
        final Record record = new AvroRecord(new RecordImpl.BuilderImpl().withString("test", "data").build());

        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        SchemaRegistryCoder.of().encode(record, buffer);

        final Record decoded = SchemaRegistryCoder.of().decode(new ByteArrayInputStream(buffer.toByteArray()));
        assertEquals("data", decoded.getString("test"));
    }

    @Test
    void codecArrayRecord() throws IOException {
        final AvroRecord nestedRecord = new AvroRecord(new RecordImpl.BuilderImpl().withDouble("len", 2).build());
        final Record record = new AvroRecord(new RecordImpl.BuilderImpl()
                .withArray(new SchemaImpl.EntryImpl("__default__", Schema.Type.ARRAY, true, null,
                        nestedRecord.getSchema(), null), singletonList(nestedRecord))
                .build());

        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        SchemaRegistryCoder.of().encode(record, buffer);

        final Record decoded = SchemaRegistryCoder.of().decode(new ByteArrayInputStream(buffer.toByteArray()));
        final double actual = decoded.getArray(Record.class, "__default__").iterator().next().getDouble("len");
        assertEquals(2., actual);
    }
}
