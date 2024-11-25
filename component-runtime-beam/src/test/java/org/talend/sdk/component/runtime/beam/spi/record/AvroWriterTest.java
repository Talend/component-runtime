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
package org.talend.sdk.component.runtime.beam.spi.record;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.apache.avro.Schema;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.IndexedRecord;
import org.apache.avro.io.DatumWriter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;
import org.talend.sdk.component.runtime.beam.spi.AvroRecordBuilderFactoryProvider;

public class AvroWriterTest {

    private final RecordBuilderFactory recordBuilderFactory = new AvroRecordBuilderFactoryProvider()
            .apply("noContainerId");

    @Test
    public void testAvroStorage() {
        Record headers = recordBuilderFactory
                .newRecordBuilder()
                .withString("Accept",
                        "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
                .withString("Accept-Encoding", "gzip, deflate")
                .withString("Accept-Encoding", "gzip, deflate")
                .withString("Accept-Language", "en-US,en;q=0.5")
                .withString("Host", "httpbin.org")
                .withString("Upgrade-Insecure-Requests", "1")
                .withString("User-Agent", "Mozilla")
                .withString("X-Amzn-Trace-Id", "Root=")
                .build();

        Record args = recordBuilderFactory
                .newRecordBuilder()
                .build();

        Record files = recordBuilderFactory
                .newRecordBuilder()
                .build();

        Record form = recordBuilderFactory
                .newRecordBuilder()
                .build();

        Record record = recordBuilderFactory
                .newRecordBuilder()
                .withRecord("args", args)
                .withString("data", "")
                .withRecord("files", files)
                .withRecord("form", form)
                .withRecord("headers", headers)
                .withString("json", null)
                .withString("method", "GET")
                .withString("origin", "90.60.65.229")
                .withString("url", "http://httpbin.org/anything")
                .build();

        IndexedRecord indexedRecord = ((AvroRecord) record).unwrap(IndexedRecord.class);

        Schema schema = indexedRecord.getSchema();

        final byte[] bytes = serializeIndexedRecords(Collections.singletonList(indexedRecord), schema);
        Assertions.assertTrue(bytes.length != 0);
    }

    /**
     * Write Records to MinIO, this method is used with the Native Runner to replace the fileIO BeamRunner
     * The method throws an Avro exception when we try to write the above records :
     * org.apache.avro.file.DataFileWriter$AppendWriteException: org.apache.avro.UnresolvedUnionException:
     * Not in union
     * ["null",{"type":"record","name":"EmptyRecord","namespace":"org.talend.sdk.component.schema.generated","fields":[]}]:
     * {}
     */
    private static byte[] serializeIndexedRecords(List<IndexedRecord> records, Schema schema) {
        DatumWriter<IndexedRecord> datumWriter = new GenericDatumWriter<>(schema);
        try (ByteArrayOutputStream buffer = new ByteArrayOutputStream(100 * records.size());
                DataFileWriter<IndexedRecord> dataFileWriter = new DataFileWriter<>(datumWriter)) {
            dataFileWriter.create(schema, buffer);
            for (IndexedRecord record : records) {
                dataFileWriter.append(record);
            }
            dataFileWriter.close();
            return buffer.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Error while serializing Avro records", e);
        }
    }
}
