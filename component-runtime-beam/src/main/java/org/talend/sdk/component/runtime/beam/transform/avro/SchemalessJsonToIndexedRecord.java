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
package org.talend.sdk.component.runtime.beam.transform.avro;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.apache.avro.Schema;
import org.apache.avro.generic.IndexedRecord;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.PCollection;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;

@AllArgsConstructor
public class SchemalessJsonToIndexedRecord extends PTransform<PCollection<JsonObject>, PCollection<IndexedRecord>> {

    private static final String NAMESPACE = "org.talend.generated.json2avro";

    private String rootRecordName;

    @Override
    public PCollection<IndexedRecord> expand(final PCollection<JsonObject> input) {
        return input.apply("SchemalessJsonToIndexedRecord", ParDo.of(new Fn(NAMESPACE + '.' + rootRecordName)));
    }

    @RequiredArgsConstructor
    public static class Fn extends DoFn<JsonObject, IndexedRecord> {

        private static final Schema BOOLEAN =
                Schema.createUnion(asList(Schema.create(Schema.Type.NULL), Schema.create(Schema.Type.BOOLEAN)));

        private static final Schema DOUBLE =
                Schema.createUnion(asList(Schema.create(Schema.Type.NULL), Schema.create(Schema.Type.DOUBLE)));

        private static final Schema LONG =
                Schema.createUnion(asList(Schema.create(Schema.Type.NULL), Schema.create(Schema.Type.LONG)));

        private static final Schema INT =
                Schema.createUnion(asList(Schema.create(Schema.Type.NULL), Schema.create(Schema.Type.INT)));

        private static final Schema STRING =
                Schema.createUnion(asList(Schema.create(Schema.Type.NULL), Schema.create(Schema.Type.STRING)));

        private static final Schema NULL = Schema.create(Schema.Type.NULL);

        private final String rootRecordName;

        @ProcessElement
        public void onRecord(final ProcessContext context) {
            final JsonObject element = context.element();
            context.output(toAvro(element));
        }

        public JsonIndexedRecord toAvro(final JsonObject element) {
            return new JsonIndexedRecord(element, guessSchema(rootRecordName, element));
        }

        private Schema guessSchema(final String recordName, final JsonValue element) {
            switch (element.getValueType()) {
            case STRING:
                return STRING;
            case NUMBER:
                final Number number = JsonNumber.class.cast(element).numberValue();
                if (Long.class.isInstance(number)) {
                    return LONG;
                }
                if (Integer.class.isInstance(number)) {
                    return INT;
                }
                return DOUBLE;
            case FALSE:
            case TRUE:
                return BOOLEAN;
            case NULL:
                return NULL;
            case OBJECT:
                final Schema record = Schema.createRecord(recordName, null, NAMESPACE, false);
                record
                        .setFields(element
                                .asJsonObject()
                                .entrySet()
                                .stream()
                                .map(it -> new Schema.Field(it.getKey(),
                                        guessSchema(buildNextName(recordName, it.getKey()), it.getValue()), null, null))
                                .collect(toList()));
                return record;
            case ARRAY:
                final JsonArray array = element.asJsonArray();
                if (!array.isEmpty()) {
                    return Schema.createArray(guessSchema(buildNextName(recordName, "Array"), array.iterator().next()));
                }
                return Schema.createArray(Schema.create(Schema.Type.NULL));
            default:
                throw new IllegalArgumentException("Unsupported: " + element.toString());
            }
        }

        private String buildNextName(final String recordName, final String key) {
            if (key.isEmpty()) { // weird but possible
                return recordName + "Empty";
            }
            final String normalized = key.replaceAll("[^a-zA-Z0-9]", "");
            return recordName + Character.toUpperCase(normalized.charAt(0)) + normalized.substring(1);
        }
    }
}
