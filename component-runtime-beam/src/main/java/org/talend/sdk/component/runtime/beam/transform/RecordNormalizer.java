/**
 * Copyright (C) 2006-2022 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.runtime.beam.transform;

import static java.util.Collections.singletonList;
import static lombok.AccessLevel.PROTECTED;

import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.values.PCollection;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;
import org.talend.sdk.component.runtime.beam.coder.registry.SchemaRegistryCoder;
import org.talend.sdk.component.runtime.beam.transform.service.ServiceLookup;
import org.talend.sdk.component.runtime.manager.ComponentManager;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * Allows to convert an input to a normal output wrapping the value in a __default__ container.
 */
@AllArgsConstructor
@NoArgsConstructor(access = PROTECTED)
public class RecordNormalizer extends DoFn<Record, Record> {

    private RecordBuilderFactory factory;

    @ProcessElement
    public void onElement(final ProcessContext context) {
        final Record record = toMap(context.element());
        context.output(record);
    }

    private Record toMap(final Record element) {
        final Schema nestedSchema = element.getSchema();
        return factory
                .newRecordBuilder()
                .withArray(factory
                        .newEntryBuilder()
                        .withName("__default__")
                        .withType(Schema.Type.ARRAY)
                        .withElementSchema(nestedSchema)
                        .build(), singletonList(element))
                .build();
    }

    public static PTransform<PCollection<Record>, PCollection<Record>> of(final String plugin) {
        final RecordBuilderFactory lookup =
                ServiceLookup.lookup(ComponentManager.instance(), plugin, RecordBuilderFactory.class);
        return new RecordParDoTransformCoderProvider<>(SchemaRegistryCoder.of(), new RecordNormalizer(lookup));
    }
}
