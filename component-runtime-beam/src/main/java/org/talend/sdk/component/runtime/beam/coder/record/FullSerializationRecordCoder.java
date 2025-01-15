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
package org.talend.sdk.component.runtime.beam.coder.record;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.avro.Schema;
import org.apache.avro.file.DataFileReader;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.file.SeekableByteArrayInput;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.IndexedRecord;
import org.apache.avro.io.DatumReader;
import org.apache.beam.sdk.coders.CustomCoder;
import org.apache.commons.compress.utils.IOUtils;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.runtime.beam.avro.AvroSchemas;
import org.talend.sdk.component.runtime.beam.io.NoCloseOutputStream;
import org.talend.sdk.component.runtime.beam.spi.record.AvroRecord;
import org.talend.sdk.component.runtime.manager.service.api.Unwrappable;
import org.talend.sdk.component.runtime.record.Schemas;

// simple coder serializing any record in a file container with its schema
// NOTE: in prod prefer a flavor not requiring to serialize the schema
public class FullSerializationRecordCoder extends CustomCoder<Record> {

    private static final GenericData.Record EMPTY_RECORD = new GenericData.Record(AvroSchemas.getEmptySchema());

    @Override
    public void encode(final Record value, final OutputStream outputStream) throws IOException {
        final org.talend.sdk.component.api.record.Schema schema =
                value == null ? Schemas.EMPTY_RECORD : value.getSchema();
        final Schema avro =
                value == null ? AvroSchemas.getEmptySchema() : Unwrappable.class.cast(schema).unwrap(Schema.class);
        final IndexedRecord record =
                value == null ? EMPTY_RECORD : Unwrappable.class.cast(value).unwrap(IndexedRecord.class);
        try (final DataFileWriter<IndexedRecord> writer = new DataFileWriter<>(new GenericDatumWriter<>(avro))) {
            writer.create(avro, new NoCloseOutputStream(outputStream));
            writer.append(record);
        }
    }

    @Override
    public Record decode(final InputStream inputStream) throws IOException {
        final DatumReader<IndexedRecord> datumReader = new GenericDatumReader<>();
        try (final DataFileReader<IndexedRecord> reader =
                new DataFileReader<>(new SeekableByteArrayInput(IOUtils.toByteArray(inputStream)), datumReader)) {
            return new AvroRecord(reader.next());
        }
    }

    @Override
    public int hashCode() {
        return FullSerializationRecordCoder.class.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        return FullSerializationRecordCoder.class.isInstance(obj);
    }

    public static FullSerializationRecordCoder of() {
        return new FullSerializationRecordCoder();
    }
}
