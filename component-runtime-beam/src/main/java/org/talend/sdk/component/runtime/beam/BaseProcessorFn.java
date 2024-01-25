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
package org.talend.sdk.component.runtime.beam;

import static java.util.Collections.emptyIterator;
import static java.util.stream.Collectors.toMap;
import static org.talend.sdk.component.api.record.Schema.sanitizeConnectionName;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Consumer;

import javax.json.bind.Jsonb;

import org.apache.beam.sdk.transforms.DoFn;
import org.talend.sdk.component.api.processor.OutputEmitter;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;
import org.talend.sdk.component.runtime.beam.spi.record.RecordCollectors;
import org.talend.sdk.component.runtime.output.InputFactory;
import org.talend.sdk.component.runtime.output.OutputFactory;
import org.talend.sdk.component.runtime.output.Processor;
import org.talend.sdk.component.runtime.output.ProcessorImpl;
import org.talend.sdk.component.runtime.record.RecordConverters;
import org.talend.sdk.component.runtime.record.Schemas;
import org.talend.sdk.component.runtime.serialization.ContainerFinder;
import org.talend.sdk.component.runtime.serialization.LightContainer;

import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor
abstract class BaseProcessorFn<O> extends DoFn<Record, O> {

    protected Processor processor;

    @Setter
    protected int maxBatchSize = -1; // ui enforces it so we set it to -1 to enable to deactivate it

    protected int currentCount;

    protected volatile RecordBuilderFactory recordFactory;

    protected volatile Jsonb jsonb;

    BaseProcessorFn(final Processor processor) {
        this.processor = processor;
        if (ProcessorImpl.class.isInstance(processor)) {
            ProcessorImpl.class
                    .cast(processor)
                    .getInternalConfiguration()
                    .entrySet()
                    .stream()
                    .filter(it -> it.getKey().endsWith("$maxBatchSize") && it.getValue() != null
                            && !it.getValue().trim().isEmpty())
                    .findFirst()
                    .ifPresent(val -> {
                        try {
                            maxBatchSize = Integer.parseInt(val.getValue().trim());
                        } catch (final NumberFormatException nfe) {
                            log.warn("Invalid configuration: " + val);
                        }
                    });
        }
    }

    protected abstract Consumer<Record> toEmitter(ProcessContext context);

    protected abstract BeamOutputFactory getFinishBundleOutputFactory(FinishBundleContext context);

    @Setup
    public void setup() throws Exception {
        processor.start();
    }

    @ProcessElement
    public void processElement(final ProcessContext context) {
        ensureInit();
        if (currentCount == 0) {
            processor.beforeGroup();
        }
        final BeamOutputFactory output = new BeamSingleOutputFactory(toEmitter(context), recordFactory, jsonb);
        processor.onNext(new BeamInputFactory(context), output);
        output.postProcessing();
        currentCount++;
        if (maxBatchSize > 0 && currentCount >= maxBatchSize) {
            currentCount = 0;
            final BeamOutputFactory ago = new BeamMultiOutputFactory(toEmitter(context), recordFactory, jsonb);
            processor.afterGroup(output);
            ago.postProcessing();
        }
    }

    @FinishBundle
    public void finishBundle(final FinishBundleContext context) {
        if (currentCount > 0) {
            ensureInit();
            currentCount = 0;
            final BeamOutputFactory output = getFinishBundleOutputFactory(context);
            processor.afterGroup(output);
            output.postProcessing();
        }
    }

    @Teardown
    public void tearDown() {
        processor.stop();
    }

    private void ensureInit() {
        if (jsonb == null) {
            synchronized (this) {
                if (jsonb == null) {
                    final LightContainer container = ContainerFinder.Instance.get().find(processor.plugin());
                    recordFactory = container.findService(RecordBuilderFactory.class);
                    jsonb = container.findService(Jsonb.class);
                }
            }
        }
    }

    protected static final class BeamInputFactory implements InputFactory {

        private final Map<String, Iterator<Record>> objects;

        BeamInputFactory(final DoFn<Record, ?>.ProcessContext context) {
            final Record element = context.element();
            objects = element
                    .getSchema()
                    .getAllEntries()
                    .filter(e -> !e.getName().startsWith("__talend_internal"))
                    .collect(toMap(Schema.Entry::getName, e -> element.getArray(Record.class, e.getName()).iterator()));
        }

        @Override
        public Object read(final String name) {
            final Iterator<Record> values = objects.getOrDefault(sanitizeConnectionName(name), emptyIterator());
            return values.hasNext() ? values.next() : null;
        }
    }

    @RequiredArgsConstructor
    protected abstract static class BeamOutputFactory implements OutputFactory {

        protected final Consumer<Record> emit;

        protected final RecordBuilderFactory factory;

        protected final Jsonb jsonb;

        private final Map<String, Collection<Record>> outputs = new HashMap<>();

        @Override
        public OutputEmitter create(final String name) {
            return new BeamOutputEmitter(outputs.computeIfAbsent(sanitizeConnectionName(name), k -> new ArrayList<>()),
                    factory, jsonb);
        }

        public abstract void postProcessing();
    }

    protected static final class BeamSingleOutputFactory extends BeamOutputFactory {

        private final Map<String, Collection<Record>> outputs = new HashMap<>();

        protected BeamSingleOutputFactory(final Consumer<Record> emit, final RecordBuilderFactory factory,
                final Jsonb jsonb) {
            super(emit, factory, jsonb);
        }

        @Override
        public OutputEmitter create(final String name) {
            return new BeamOutputEmitter(outputs.computeIfAbsent(sanitizeConnectionName(name), k -> new ArrayList<>()),
                    factory, jsonb);
        }

        @Override
        public void postProcessing() {
            if (!outputs.isEmpty()) {
                final Record record = outputs.entrySet().stream().collect(factory::newRecordBuilder, (a, o) -> {
                    final Record firstElement = o.getValue().isEmpty() ? null : o.getValue().iterator().next();
                    a
                            .withArray(factory
                                    .newEntryBuilder()
                                    .withName(o.getKey())
                                    .withType(Schema.Type.ARRAY)
                                    .withElementSchema(
                                            firstElement == null ? Schemas.EMPTY_RECORD : firstElement.getSchema())
                                    .build(), o.getValue());
                }, RecordCollectors::merge).build();
                emit.accept(record);
            }
        }
    }

    protected static final class BeamMultiOutputFactory extends BeamOutputFactory {

        private final Collection<Record> outputs = new ArrayList<>();

        protected BeamMultiOutputFactory(final Consumer<Record> emit, final RecordBuilderFactory factory,
                final Jsonb jsonb) {
            super(emit, factory, jsonb);
        }

        @Override
        public OutputEmitter create(final String name) {
            return value -> {
                final Collection<Record> values = new ArrayList<>();
                new BeamOutputEmitter(values, factory, jsonb) {

                    @Override
                    public void emit(final Object value) {
                        super.emit(value);
                        final Record first = values.isEmpty() ? null : values.iterator().next();
                        outputs
                                .add(factory
                                        .newRecordBuilder()
                                        .withArray(factory
                                                .newEntryBuilder()
                                                .withName(name)
                                                .withType(Schema.Type.ARRAY)
                                                .withElementSchema(
                                                        first == null ? Schemas.EMPTY_RECORD : first.getSchema())
                                                .build(), values)
                                        .build());
                    }
                }.emit(value);
            };
        }

        public void postProcessing() {
            if (!outputs.isEmpty()) {
                outputs.forEach(emit::accept);
            }
        }
    }

    @RequiredArgsConstructor
    private static class BeamOutputEmitter implements OutputEmitter {

        private final Collection<Record> builder;

        private final RecordBuilderFactory recordBuilderFactory;

        private final Jsonb jsonb;

        private final RecordConverters converters = new RecordConverters();

        private final RecordConverters.MappingMetaRegistry registry = new RecordConverters.MappingMetaRegistry();

        @Override
        public void emit(final Object value) {
            if (value == null) {
                return;
            }
            builder.add(toRecord(value));
        }

        private Record toRecord(final Object value) {
            return Record.class.cast(converters.toRecord(registry, value, () -> jsonb, () -> recordBuilderFactory));
        }
    }
}
