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
package org.talend.sdk.component.junit.beam;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static lombok.AccessLevel.PRIVATE;
import static lombok.AccessLevel.PROTECTED;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.bind.Jsonb;
import javax.json.spi.JsonProvider;

import org.apache.beam.sdk.coders.Coder;
import org.apache.beam.sdk.coders.MapCoder;
import org.apache.beam.sdk.coders.StringUtf8Coder;
import org.apache.beam.sdk.transforms.Create;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.PCollection;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;
import org.talend.sdk.component.runtime.beam.coder.JsonbCoder;
import org.talend.sdk.component.runtime.beam.coder.JsonpJsonObjectCoder;
import org.talend.sdk.component.runtime.beam.coder.registry.SchemaRegistryCoder;
import org.talend.sdk.component.runtime.beam.spi.record.RecordCollectors;
import org.talend.sdk.component.runtime.record.RecordConverters;
import org.talend.sdk.component.runtime.serialization.ContainerFinder;
import org.talend.sdk.component.runtime.serialization.LightContainer;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = PRIVATE)
public class Data {

    public static final RecordConverters.MappingMetaRegistry REGISTRY = new RecordConverters.MappingMetaRegistry();

    public static <T> PTransform<PCollection<Record>, PCollection<Map<String, T>>> map(final String plugin,
            final Class<T> expectedRecordType) {
        return new DataMapper<>(plugin, expectedRecordType);
    }

    public static Create.Values<Record> of(final String plugin, final Iterable<Map<String, List<?>>> elems) {
        return Create.of(() -> {
            final Iterator<Map<String, List<?>>> delegate = elems.iterator();
            return new Iterator<Record>() {

                private transient volatile RecordConverters converters;

                private transient volatile Jsonb jsonb;

                private transient volatile RecordBuilderFactory recordBuilderFactory;

                @Override
                public boolean hasNext() {
                    return delegate.hasNext();
                }

                @Override
                public Record next() {
                    return map(delegate.next());
                }

                private Record map(final Map<String, List<?>> next) {
                    if (next == null) {
                        return null;
                    }
                    if (converters == null) {
                        synchronized (this) {
                            if (converters == null) {
                                final LightContainer container = ContainerFinder.Instance.get().find(plugin);
                                recordBuilderFactory = container.findService(RecordBuilderFactory.class);
                                jsonb = container.findService(Jsonb.class);
                                converters = new RecordConverters();
                            }
                        }
                    }
                    return next
                            .entrySet()
                            .stream()
                            .filter(it -> !it.getValue().isEmpty())
                            .collect(recordBuilderFactory::newRecordBuilder, (aggregator, entry) -> {
                                final List<Record> list = entry
                                        .getValue()
                                        .stream()
                                        .map(it -> Record.class
                                                .cast(converters
                                                        .toRecord(REGISTRY, it, () -> jsonb,
                                                                () -> recordBuilderFactory)))
                                        .collect(toList());
                                aggregator
                                        .withArray(recordBuilderFactory
                                                .newEntryBuilder()
                                                .withName(entry.getKey())
                                                .withType(Schema.Type.ARRAY)
                                                .withElementSchema(list.iterator().next().getSchema())
                                                .build(), list)
                                        .build();
                            }, RecordCollectors::merge)
                            .build();
                }
            };
        }).withCoder(SchemaRegistryCoder.of());
    }

    @NoArgsConstructor(access = PROTECTED)
    @AllArgsConstructor(access = PRIVATE)
    private static class DataMapper<T> extends PTransform<PCollection<Record>, PCollection<Map<String, T>>> {

        private String plugin;

        private Class<T> type;

        @Override
        protected Coder<?> getDefaultOutputCoder() {
            return MapCoder.of(StringUtf8Coder.of(), SchemaRegistryCoder.of());
        }

        @Override
        public PCollection<Map<String, T>> expand(final PCollection<Record> collection) {
            return collection
                    .apply(ParDo
                            .of(new DataMapperFn<>(JsonpJsonObjectCoder.of(plugin), JsonbCoder.of(type, plugin), plugin,
                                    new RecordConverters(), new RecordConverters.MappingMetaRegistry())));
        }
    }

    @NoArgsConstructor(access = PROTECTED)
    @AllArgsConstructor(access = PRIVATE)
    private static class DataMapperFn<T> extends DoFn<Record, Map<String, T>> {

        private Coder<JsonObject> jsonpCoder;

        private JsonbCoder<T> jsonbCoder;

        private String plugin;

        private RecordConverters converters;

        private volatile RecordConverters.MappingMetaRegistry registry;

        @ProcessElement
        public void onElement(final ProcessContext context) {
            context.output(map(context.element()));
        }

        private Map<String, T> map(final Record object) {
            if (registry == null) {
                registry = new RecordConverters.MappingMetaRegistry();
            }
            return object.getSchema().getAllEntries().collect(toMap(Schema.Entry::getName, e -> {
                try {
                    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    final Record record = object.getArray(Record.class, e.getName()).iterator().next();
                    final JsonObject jsonObject = JsonObject.class
                            .cast(converters
                                    .toType(registry, record, JsonObject.class, this::getJsonBuilder,
                                            this::getJsonProvider, this::getJsonb, this::getRecordBuilderFactory));
                    if (Record.class == jsonbCoder.getType()) {
                        return (T) new RecordConverters()
                                .toRecord(REGISTRY, jsonObject, this::getJsonb, this::getRecordBuilderFactory);
                    }
                    jsonpCoder.encode(jsonObject, baos);
                    return jsonbCoder.decode(new ByteArrayInputStream(baos.toByteArray()));
                } catch (final IOException ex) {
                    throw new IllegalArgumentException(ex);
                }
            }));

        }

        private LightContainer getContainer() {
            return ContainerFinder.Instance.get().find(plugin);
        }

        private RecordBuilderFactory getRecordBuilderFactory() {
            return getContainer().findService(RecordBuilderFactory.class);
        }

        private JsonBuilderFactory getJsonBuilder() {
            return getContainer().findService(JsonBuilderFactory.class);
        }

        private JsonProvider getJsonProvider() {
            return getContainer().findService(JsonProvider.class);
        }

        private Jsonb getJsonb() {
            return getContainer().findService(Jsonb.class);
        }
    }
}
