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
package org.talend.sdk.component.runtime.beam.transform.avro;

import java.io.StringReader;

import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonReaderFactory;

import org.apache.avro.generic.IndexedRecord;
import org.apache.beam.sdk.coders.Coder;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.PCollection;
import org.talend.sdk.component.runtime.beam.coder.JsonpJsonObjectCoder;
import org.talend.sdk.component.runtime.manager.ComponentManager;

public class IndexedRecordToJson extends PTransform<PCollection<IndexedRecord>, PCollection<JsonObject>> {

    private final JsonReaderFactory factory;

    public IndexedRecordToJson() {
        this.factory = ComponentManager.instance().getJsonpReaderFactory();
    }

    @Override
    public PCollection<JsonObject> expand(final PCollection<IndexedRecord> input) {
        return input.apply("IndexedRecordToJson", ParDo.of(new Fn(factory)));
    }

    @Override
    protected Coder<?> getDefaultOutputCoder() {
        return JsonpJsonObjectCoder.of(null);
    }

    public static class Fn extends DoFn<IndexedRecord, JsonObject> {

        private final JsonReaderFactory factory;

        public Fn(final JsonReaderFactory factory) {
            this.factory = factory;
        }

        @ProcessElement
        public void onRecord(final ProcessContext context) {
            context.output(toJson(context.element()));
        }

        private JsonObject toJson(final IndexedRecord element) {
            try (final JsonReader reader = factory.createReader(new StringReader(element.toString()))) {
                return reader.readObject();
            }
        }
    }
}
