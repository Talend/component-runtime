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
package org.talend.sdk.component.runtime.beam.transform;

import static java.util.Collections.singletonList;
import static lombok.AccessLevel.PRIVATE;
import static lombok.AccessLevel.PROTECTED;

import org.apache.beam.sdk.coders.Coder;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.transforms.join.CoGbkResult;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;
import org.talend.sdk.component.runtime.beam.coder.registry.SchemaRegistryCoder;
import org.talend.sdk.component.runtime.beam.spi.record.RecordCollectors;
import org.talend.sdk.component.runtime.serialization.ContainerFinder;
import org.talend.sdk.component.runtime.serialization.LightContainer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Adapter to convert a ProcessContext coming from a CoGBK to a
 * {@code Map<String, List<?>>}
 */
@AllArgsConstructor
@NoArgsConstructor(access = PROTECTED)
public class CoGroupByKeyResultMappingTransform<K>
        extends PTransform<PCollection<KV<K, CoGbkResult>>, PCollection<Record>> {

    private String plugin;

    private boolean propagateKey;

    @Override
    public PCollection<Record> expand(final PCollection<KV<K, CoGbkResult>> input) {
        return input.apply(ParDo.of(new CoGBKMappingFn<>(plugin, propagateKey, null)));
    }

    @Override
    protected Coder<?> getDefaultOutputCoder() {
        return SchemaRegistryCoder.of();
    }

    @AllArgsConstructor(access = PRIVATE)
    @NoArgsConstructor(access = PROTECTED)
    public static class CoGBKMappingFn<K> extends DoFn<KV<K, CoGbkResult>, Record> {

        private String plugin;

        private boolean propagateKey;

        private volatile RecordBuilderFactory builderFactory;

        @ProcessElement
        public void onElement(final ProcessContext context) {
            context.output(createMap(context));
        }

        private Record createMap(final ProcessContext context) {
            final KV<K, CoGbkResult> element = context.element();
            final CoGbkResult result = element.getValue();
            final RecordBuilderFactory builderFactory = builderFactory();
            final Record.Builder builder = result
                    .getSchema()
                    .getTupleTagList()
                    .getAll()
                    .stream()
                    .map(key -> new Pair<>(key.getId(), Record.class.cast(result.getOnly(key, null))))
                    .filter(p -> p.getSecond() != null)
                    .collect(builderFactory::newRecordBuilder, (b, p) -> {
                        final Record record = p.getSecond();
                        final Schema.Entry entry = builderFactory
                                .newEntryBuilder()
                                .withName(p.getFirst())
                                .withType(Schema.Type.ARRAY)
                                .withElementSchema(record.getSchema())
                                .build();
                        b.withArray(entry, singletonList(record));
                    }, RecordCollectors::merge);
            if (propagateKey) {
                final Record internalRecord =
                        builderFactory.newRecordBuilder().withString("key", String.valueOf(element.getKey())).build();
                builder
                        .withRecord(builderFactory
                                .newEntryBuilder()
                                .withName("__talend_internal")
                                .withType(Schema.Type.RECORD)
                                .withElementSchema(internalRecord.getSchema())
                                .build(), internalRecord);
            }
            return builder.build();
        }

        private RecordBuilderFactory builderFactory() {
            if (builderFactory == null) {
                synchronized (this) {
                    if (builderFactory == null) {
                        final LightContainer container = ContainerFinder.Instance.get().find(plugin);
                        builderFactory = container.findService(RecordBuilderFactory.class);
                    }
                }
            }
            return builderFactory;
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    private static class Pair<A, B> {

        private A first;

        private B second;
    }
}
