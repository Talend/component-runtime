/**
 * Copyright (C) 2006-2026 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.runtime.beam.transform.avro;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;

import java.util.stream.StreamSupport;

import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.IndexedRecord;
import org.apache.beam.sdk.PipelineResult;
import org.apache.beam.sdk.coders.AvroCoder;
import org.apache.beam.sdk.testing.PAssert;
import org.apache.beam.sdk.testing.TestPipeline;
import org.apache.beam.sdk.transforms.Create;
import org.junit.Rule;
import org.junit.Test;

public class IndexedRecordToJsonTest {

    @Rule
    public final transient TestPipeline pipeline = TestPipeline.create();

    @Test
    public void test() {
        PAssert
                .that(pipeline
                        .apply(Create
                                .of(newIndexedRecord("first"), newIndexedRecord("second"))
                                .withCoder(AvroCoder.of(IndexedRecord.class, getSchema())))
                        .apply(new IndexedRecordToJson()))
                .satisfies(values -> {
                    assertEquals(asList("first", "second"),
                            StreamSupport
                                    .stream(values.spliterator(), false)
                                    .map(k -> k.getString("name"))
                                    .sorted()
                                    .collect(toList()));
                    return null;
                });
        assertEquals(PipelineResult.State.DONE, pipeline.run().waitUntilFinish());
    }

    private IndexedRecord newIndexedRecord(final String name) {
        final GenericData.Record record = new GenericData.Record(getSchema());
        record.put("name", name);
        return record;
    }

    private Schema getSchema() {
        return SchemaBuilder
                .record("IndexedRecordToJsonTest")
                .fields()
                .name("name")
                .type(SchemaBuilder.nullable().stringType())
                .noDefault()
                .endRecord();
    }
}
