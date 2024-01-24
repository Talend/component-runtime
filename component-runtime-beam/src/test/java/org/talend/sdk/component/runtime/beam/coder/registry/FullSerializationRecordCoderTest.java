/**
 * Copyright (C) 2006-2024 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.runtime.beam.coder.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.runtime.beam.coder.record.FullSerializationRecordCoder;
import org.talend.sdk.component.runtime.beam.spi.record.AvroRecord;
import org.talend.sdk.component.runtime.record.RecordImpl;

class FullSerializationRecordCoderTest {

    @Test
    void roundTrip() throws IOException {
        final Record record = new AvroRecord(new RecordImpl.BuilderImpl()
                .withString("test", "data")
                .withInt("age", 30)
                .withLong("duration", 300000L)
                .build());

        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        FullSerializationRecordCoder.of().encode(record, buffer);

        final Record decoded = FullSerializationRecordCoder.of().decode(new ByteArrayInputStream(buffer.toByteArray()));
        assertEquals("data", decoded.getString("test"));
        assertEquals(30, decoded.getInt("age"));
        assertEquals(300000L, decoded.getLong("duration"));
    }
}
