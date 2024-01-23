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

import javax.json.JsonObject;

import org.apache.avro.Schema;
import org.apache.avro.generic.IndexedRecord;
import org.apache.beam.sdk.coders.AvroCoder;
import org.apache.beam.sdk.coders.Coder;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.PCollection;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;

@AllArgsConstructor
public class JsonToIndexedRecord extends PTransform<PCollection<JsonObject>, PCollection<IndexedRecord>> {

    private final Schema outputSchema;

    @Override
    public PCollection<IndexedRecord> expand(final PCollection<JsonObject> input) {
        return input.apply("JsonToIndexedRecord", ParDo.of(new Fn(outputSchema.toString())));
    }

    @Override
    protected Coder<?> getDefaultOutputCoder() {
        return AvroCoder.of(outputSchema);
    }

    @RequiredArgsConstructor
    public static class Fn extends DoFn<JsonObject, IndexedRecord> {

        private final String schemaJson;

        private Schema schema;

        @Setup
        public void setup() {
            schema = new Schema.Parser().parse(schemaJson);
        }

        @ProcessElement
        public void onRecord(final ProcessContext context) {
            context.output(new JsonIndexedRecord(context.element(), schema));
        }
    }
}
