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

import static org.talend.sdk.component.runtime.beam.spi.record.SchemaIdGenerator.generateRecordName;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.IndexedRecord;
import org.apache.beam.sdk.coders.AvroCoder;
import org.apache.beam.sdk.coders.CustomCoder;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.runtime.beam.avro.AvroSchemas;
import org.talend.sdk.component.runtime.beam.spi.record.AvroRecord;
import org.talend.sdk.component.runtime.manager.service.api.Unwrappable;
import org.talend.sdk.component.runtime.record.Schemas;

// advantage is that it does not need any record mutation but
// it implies a mutation of the binary format on persistence
// todo: make it better
public class SchemaRegistryCoder extends CustomCoder<Record> {

    private static final GenericData.Record EMPTY_RECORD = new GenericData.Record(AvroSchemas.getEmptySchema());

    @Override
    public void encode(final Record value, final OutputStream outputStream) throws IOException {
        final org.talend.sdk.component.api.record.Schema schema =
                value == null ? Schemas.EMPTY_RECORD : value.getSchema();
        final Schema avro =
                value == null ? AvroSchemas.getEmptySchema() : Unwrappable.class.cast(schema).unwrap(Schema.class);
        final String id = generateRecordName(avro.getFields());
        // write the id first
        outputStream.write(id.getBytes(StandardCharsets.UTF_8));
        outputStream.write('\n');

        // then the record with the default avro coder
        registry().putIfAbsent(id, schema);
        if (value != null) {
            getCoder(avro).encode(Unwrappable.class.cast(value).unwrap(IndexedRecord.class), outputStream);
        }
        outputStream.flush();
    }

    @Override
    public Record decode(final InputStream inputStream) throws IOException {
        final ByteArrayOutputStream firstLineData = new ByteArrayOutputStream();
        int b;
        while ((b = inputStream.read()) >= 0 && b != '\n') {
            firstLineData.write(b);
        }
        final String id = firstLineData.toString("UTF-8");
        final org.talend.sdk.component.api.record.Schema schema = registry().get(id);
        if (schema == null) {
            throw new IllegalStateException("Invalid schema id: '" + id + "'");
        }
        final Schema unwrappedSchema = Unwrappable.class.cast(schema).unwrap(Schema.class);
        if (Schemas.EMPTY_RECORD == schema) {
            return new AvroRecord(EMPTY_RECORD);
        }
        final IndexedRecord decoded = getCoder(unwrappedSchema).decode(inputStream);
        return new AvroRecord(decoded);
    }

    @Override
    public int hashCode() {
        return SchemaRegistryCoder.class.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        return SchemaRegistryCoder.class.isInstance(obj);
    }

    // todo: add caching here
    private AvroCoder<IndexedRecord> getCoder(final Schema avro) {
        return AvroCoder.of(IndexedRecord.class, avro);
    }

    private SchemaRegistry registry() { // don't serialize
        return SchemaRegistry.Instance.get();
    }

    public static SchemaRegistryCoder of() {
        return new SchemaRegistryCoder();
    }
}
