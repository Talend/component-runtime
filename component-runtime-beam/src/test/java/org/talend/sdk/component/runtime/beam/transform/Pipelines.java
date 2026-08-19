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
package org.talend.sdk.component.runtime.beam.transform;

import static java.util.Collections.singletonList;
import static lombok.AccessLevel.PRIVATE;

import org.apache.beam.sdk.testing.TestPipeline;
import org.apache.beam.sdk.transforms.Create;
import org.apache.beam.sdk.transforms.MapElements;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.TypeDescriptor;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;
import org.talend.sdk.component.runtime.beam.coder.registry.SchemaRegistryCoder;
import org.talend.sdk.component.runtime.beam.spi.AvroRecordBuilderFactoryProvider;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = PRIVATE)
public final class Pipelines {

    static PCollection<Record> buildBasePipeline(final TestPipeline pipeline) {
        final RecordBuilderFactory factory = new AvroRecordBuilderFactoryProvider().apply(null);
        return pipeline
                .apply(Create.of("a", "b"))
                .apply(MapElements.into(TypeDescriptor.of(Record.class)).via((String input) -> {
                    final Record b1 = factory.newRecordBuilder().withString("foo", input).build();
                    final Record b2 = factory.newRecordBuilder().withString("bar", input).build();
                    return factory
                            .newRecordBuilder()
                            .withArray(factory
                                    .newEntryBuilder()
                                    .withName("b1")
                                    .withType(Schema.Type.ARRAY)
                                    .withElementSchema(b1.getSchema())
                                    .build(), singletonList(b1))
                            .withArray(factory
                                    .newEntryBuilder()
                                    .withName("b2")
                                    .withType(Schema.Type.ARRAY)
                                    .withElementSchema(b2.getSchema())
                                    .build(), singletonList(b2))
                            .build();
                }))
                .setCoder(SchemaRegistryCoder.of());
    }
}
