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
package org.talend.sdk.component.runtime.beam.transform.avro;

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.stream.StreamSupport;

import javax.json.JsonBuilderFactory;

import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.generic.IndexedRecord;
import org.apache.beam.sdk.PipelineResult;
import org.apache.beam.sdk.testing.PAssert;
import org.apache.beam.sdk.testing.TestPipeline;
import org.apache.beam.sdk.transforms.Create;
import org.junit.Rule;
import org.junit.Test;
import org.talend.sdk.component.runtime.beam.coder.JsonpJsonObjectCoder;
import org.talend.sdk.component.runtime.manager.ComponentManager;

public class JsonToIndexedRecordTest {

    @Rule
    public final transient TestPipeline pipeline = TestPipeline.create();

    @Test
    public void test() {
        final JsonBuilderFactory factory = ComponentManager.instance().getJsonpBuilderFactory();
        PAssert
                .that(pipeline
                        .apply(Create
                                .of(factory
                                        .createObjectBuilder()
                                        .add("name", "first")
                                        .add("foo", factory.createObjectBuilder().add("age", 30))
                                        .build(),
                                        factory
                                                .createObjectBuilder()
                                                .add("name", "second")
                                                .add("foo", factory.createObjectBuilder().add("age", 20).build())
                                                .build())
                                .withCoder(JsonpJsonObjectCoder.of(null)))
                        .apply(new JsonToIndexedRecord(createSchema())))
                .satisfies(values -> {
                    final List<IndexedRecord> records =
                            StreamSupport.stream(values.spliterator(), false).collect(toList());
                    assertEquals(2, records.size());
                    return null;
                });
        assertEquals(PipelineResult.State.DONE, pipeline.run().waitUntilFinish());
    }

    private Schema createSchema() {
        return SchemaBuilder
                .record("IndexedRecordToJsonTest")
                .fields()
                .name("name")
                .type(SchemaBuilder.nullable().stringType())
                .noDefault()
                .endRecord();
    }
}
