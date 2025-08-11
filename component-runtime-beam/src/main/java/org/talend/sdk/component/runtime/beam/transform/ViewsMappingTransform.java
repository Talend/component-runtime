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

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toMap;
import static lombok.AccessLevel.PROTECTED;
import static org.talend.sdk.component.api.record.SchemaCompanionUtil.sanitizeName;

import java.util.Map;

import org.apache.beam.sdk.coders.Coder;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.PCollectionView;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;
import org.talend.sdk.component.runtime.beam.coder.registry.SchemaRegistryCoder;
import org.talend.sdk.component.runtime.serialization.ContainerFinder;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * Adapter to convert a ProcessContext to a
 * {@code Map<String, List<?>>} encapsulated in a {@code JsonObject}.
 */
@AllArgsConstructor
@NoArgsConstructor(access = PROTECTED)
public class ViewsMappingTransform extends PTransform<PCollection<Record>, PCollection<Record>> {

    private Map<String, PCollectionView<?>> views;

    private String plugin;

    @Override
    public PCollection<Record> expand(final PCollection<Record> input) {
        return input.apply(ParDo.of(new MappingViewsFn(views, plugin)));
    }

    @Override
    protected Coder<?> getDefaultOutputCoder() {
        return SchemaRegistryCoder.of();
    }

    @NoArgsConstructor(access = PROTECTED)
    public static class MappingViewsFn extends DoFn<Record, Record> {

        private volatile RecordBuilderFactory builderFactory;

        private String plugin;

        private Map<String, PCollectionView<?>> views;

        private MappingViewsFn(final String plugin) {
            this(emptyMap(), plugin);
        }

        private MappingViewsFn(final Map<String, PCollectionView<?>> views, final String plugin) {
            this.views = views
                    .entrySet()
                    .stream()
                    .collect(toMap(e -> sanitizeName(e.getKey()), Map.Entry::getValue));
            this.plugin = plugin;
        }

        @ProcessElement
        public void onElement(final ProcessContext context) {
            context.output(createMap(context));
        }

        private Record createMap(final ProcessContext context) {
            final RecordBuilderFactory factory = builderFactory();
            final Record.Builder builder = factory.newRecordBuilder();
            final Record element = context.element();
            builder
                    .withArray(factory
                            .newEntryBuilder()
                            .withName("__default__")
                            .withType(Schema.Type.ARRAY)
                            .withElementSchema(element.getSchema())
                            .build(), singletonList(element));
            views.forEach((n, v) -> {
                final Record sideInput = Record.class.cast(context.sideInput(v));
                builder
                        .withArray(factory
                                .newEntryBuilder()
                                .withName(n)
                                .withType(Schema.Type.ARRAY)
                                .withElementSchema(element.getSchema())
                                .build(), singletonList(sideInput));
            });
            return builder.build();
        }

        private RecordBuilderFactory builderFactory() {
            if (builderFactory == null) {
                synchronized (this) {
                    if (builderFactory == null) {
                        builderFactory =
                                ContainerFinder.Instance.get().find(plugin).findService(RecordBuilderFactory.class);
                    }
                }
            }
            return builderFactory;
        }
    }
}
