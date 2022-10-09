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
package org.talend.sdk.component.runtime.beam;

import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;
import static org.apache.beam.sdk.annotations.Experimental.Kind.SOURCE_SINK;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.json.bind.Jsonb;

import org.apache.beam.sdk.annotations.Experimental;
import org.apache.beam.sdk.coders.Coder;
import org.apache.beam.sdk.io.BoundedReadFromUnboundedSource;
import org.apache.beam.sdk.io.BoundedSource;
import org.apache.beam.sdk.io.UnboundedSource;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.PBegin;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.PDone;
import org.apache.beam.sdk.values.PInput;
import org.apache.beam.sdk.values.POutput;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.talend.sdk.component.api.processor.OutputEmitter;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;
import org.talend.sdk.component.runtime.base.Lifecycle;
import org.talend.sdk.component.runtime.beam.coder.NoCheckpointCoder;
import org.talend.sdk.component.runtime.beam.coder.registry.SchemaRegistryCoder;
import org.talend.sdk.component.runtime.input.Input;
import org.talend.sdk.component.runtime.input.Mapper;
import org.talend.sdk.component.runtime.input.PartitionMapperImpl;
import org.talend.sdk.component.runtime.input.Streaming;
import org.talend.sdk.component.runtime.input.Streaming.StopConfiguration;
import org.talend.sdk.component.runtime.output.Processor;
import org.talend.sdk.component.runtime.record.RecordConverters;
import org.talend.sdk.component.runtime.serialization.ContainerFinder;
import org.talend.sdk.component.runtime.serialization.LightContainer;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Experimental(SOURCE_SINK)
public final class TalendIO {

    public static Base<PBegin, PCollection<Record>, Mapper> read(final Mapper mapper) {
        return read(mapper, emptyMap());
    }

    public static Base<PBegin, PCollection<Record>, Mapper> read(final Mapper mapper,
            final Map<String, String> mapperConfiguration) {
        if (mapper.isStream()) {
            String maxRecords = null;
            String maxDurationMs = null;
            boolean hasInternalConfParams = false;
            if (PartitionMapperImpl.class.isInstance(mapper)) {
                Map<String, String> conf = PartitionMapperImpl.class.cast(mapper).getInternalConfiguration();
                hasInternalConfParams = conf.keySet()
                        .stream()
                        .filter(k -> k.equals("$maxRecords") || k.equals("$maxDurationMs"))
                        .count() > 0;
                maxRecords = conf.get("$maxRecords");
                maxDurationMs = conf.get("$maxDurationMs");
            }
            if (mapperConfiguration != null && !hasInternalConfParams) {
                if (mapperConfiguration
                        .keySet()
                        .stream()
                        .anyMatch(it -> Stream.of("maxRecords", "maxDurationMs").noneMatch(k -> k.equals(it)))) {
                    throw new IllegalArgumentException("Unsupported configuration: " + mapperConfiguration);
                }
                maxRecords = mapperConfiguration.get("maxRecords");
                maxDurationMs = mapperConfiguration.get("maxDurationMs");
            }
            return new InfiniteRead(mapper, maxRecords == null ? -1 : Long.parseLong(maxRecords.trim()),
                    maxDurationMs == null ? -1 : Long.parseLong(maxDurationMs.trim()));
        }
        if (!mapperConfiguration.isEmpty()) {
            throw new IllegalArgumentException("Unsupported configuration: " + mapperConfiguration);
        }
        return new Read(mapper);
    }

    public static Write write(final Processor output) {
        return new Write(output);
    }

    public static abstract class Base<A extends PInput, B extends POutput, D extends Lifecycle>
            extends PTransform<A, B> {

        protected D delegate;

        protected Base(final D delegate) {
            this.delegate = delegate;
        }

        protected Base() {
            // no-op
        }

        @Override
        public void validate(final PipelineOptions options) {
            // no-op
        }

        @Override
        protected String getKindString() {
            return "Talend[" + getName() + "]";
        }

        @Override
        public String getName() {
            return delegate.rootName() + "/" + delegate.name();
        }

        @Override
        protected Coder<?> getDefaultOutputCoder() {
            return SchemaRegistryCoder.of();
        }
    }

    private static class Read extends Base<PBegin, PCollection<Record>, Mapper> {

        private Read(final Mapper delegate) {
            super(delegate);
        }

        @Override
        public PCollection<Record> expand(final PBegin incoming) {
            return incoming.apply(org.apache.beam.sdk.io.Read.from(new BoundedSourceImpl(delegate)));
        }
    }

    private static class InfiniteRead extends Base<PBegin, PCollection<Record>, Mapper> {

        private final long maxRecords;

        private final long maxDurationMs;

        private InfiniteRead(final Mapper delegate, final long maxRecordCount, final long maxDuration) {
            super(delegate);
            // ensure we consider localConfiguration
            final Map<String, String> internalConf = PartitionMapperImpl.class.isInstance(delegate)
                    ? PartitionMapperImpl.class.cast(delegate).getInternalConfiguration()
                    : emptyMap();
            StopConfiguration fromLocalConf =
                    (StopConfiguration) Streaming.loadStopStrategy(delegate.plugin(), internalConf);
            // job properties win first!
            if (maxRecordCount == -1 && fromLocalConf.getMaxReadRecords() != -1) {
                maxRecords = fromLocalConf.getMaxReadRecords();
            } else {
                maxRecords = maxRecordCount;
            }
            if (maxDuration == -1 && fromLocalConf.getMaxActiveTime() != -1) {
                maxDurationMs = fromLocalConf.getMaxActiveTime();
            } else {
                maxDurationMs = maxDuration;
            }
            log.debug("[InfiniteRead] Created with maxRecords: {}, maxDurationMs: {}.", maxRecords, maxDurationMs);
        }

        @Override
        public PCollection<Record> expand(final PBegin incoming) {
            PTransform<PBegin, PCollection<Record>> unbounded =
                    org.apache.beam.sdk.io.Read.from(new UnBoundedSourceImpl(delegate));
            if (maxRecords > 0) {
                unbounded = ((org.apache.beam.sdk.io.Read.Unbounded<Record>) unbounded).withMaxNumRecords(maxRecords);
            }
            if (maxDurationMs > 0) {
                if (UnboundedSource.class.isInstance(unbounded)) {
                    unbounded = ((org.apache.beam.sdk.io.Read.Unbounded<Record>) unbounded)
                            .withMaxReadTime(Duration.millis(maxDurationMs));
                } else { // maxrecords set
                    unbounded = ((BoundedReadFromUnboundedSource<Record>) unbounded)
                            .withMaxReadTime(Duration.millis(maxDurationMs));
                }
            }
            return incoming.apply(unbounded);
        }
    }

    public static class Write extends Base<PCollection<Record>, PDone, Processor> {

        private Write(final Processor delegate) {
            super(delegate);
        }

        @Override
        public PDone expand(final PCollection<Record> incoming) {
            final WriteFn fn = new WriteFn(delegate);
            incoming.apply(ParDo.of(fn));
            return PDone.in(incoming.getPipeline());
        }
    }

    @NoArgsConstructor
    private static class WriteFn extends BaseProcessorFn<Void> {

        private static final Consumer<Record> NOOP_CONSUMER = record -> {
        };

        private static final OutputEmitter NOOP_OUTPUT_EMITTER = value -> {
            // no-op
        };

        private static final BeamOutputFactory NOOP_OUTPUT_FACTORY = new BeamOutputFactory(null, null, null) {

            @Override
            public OutputEmitter create(final String name) {
                return NOOP_OUTPUT_EMITTER;
            }

            @Override
            public void postProcessing() {
                // no-op
            }
        };

        WriteFn(final Processor processor) {
            super(processor);
        }

        @Override
        protected Consumer<Record> toEmitter(final ProcessContext context) {
            return NOOP_CONSUMER;
        }

        @Override
        protected BeamOutputFactory getFinishBundleOutputFactory(final FinishBundleContext context) {
            return NOOP_OUTPUT_FACTORY;
        }
    }

    @NoArgsConstructor
    @AllArgsConstructor
    private static class BoundedSourceImpl extends BoundedSource<Record> {

        private Mapper mapper;

        @Override
        public List<? extends BoundedSource<Record>> split(final long desiredBundleSizeBytes,
                final PipelineOptions options) {
            mapper.start();
            try {
                return mapper.split(desiredBundleSizeBytes).stream().map(BoundedSourceImpl::new).collect(toList());
            } finally {
                mapper.stop();
            }
        }

        @Override
        public long getEstimatedSizeBytes(final PipelineOptions options) {
            mapper.start();
            try {
                return mapper.assess();
            } finally {
                mapper.stop();
            }
        }

        @Override
        public BoundedReader<Record> createReader(final PipelineOptions options) {
            mapper.start();
            try {
                return new BoundedReaderImpl<>(this, mapper.create());
            } finally {
                mapper.stop();
            }
        }

        @Override
        public void validate() {
            // no-op
        }

        @Override
        public Coder<Record> getOutputCoder() {
            return SchemaRegistryCoder.of();
        }
    }

    @NoArgsConstructor
    @AllArgsConstructor
    private static class UnBoundedSourceImpl extends UnboundedSource<Record, UnboundedSource.CheckpointMark> {

        private Mapper mapper;

        @Override
        public List<? extends UnboundedSource<Record, UnboundedSource.CheckpointMark>> split(final int desiredNumSplits,
                final PipelineOptions options) {
            mapper.start();
            try {
                return mapper.split(desiredNumSplits).stream().map(UnBoundedSourceImpl::new).collect(toList());
            } finally {
                mapper.stop();
            }
        }

        @Override
        public UnboundedReader<Record> createReader(final PipelineOptions options,
                final UnboundedSource.CheckpointMark checkpointMark) {
            return new UnBoundedReaderImpl<>(this, mapper.create());
        }

        @Override
        public Coder<Record> getOutputCoder() {
            return SchemaRegistryCoder.of();
        }

        @Override
        public Coder<CheckpointMark> getCheckpointMarkCoder() {
            return new NoCheckpointCoder();
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            return mapper.equals(UnBoundedSourceImpl.class.cast(o).mapper);
        }

        @Override
        public int hashCode() {
            return mapper.hashCode();
        }
    }

    private static class Converter {

        private final RecordConverters converters;

        private final RecordConverters.MappingMetaRegistry registry = new RecordConverters.MappingMetaRegistry();

        private final RecordBuilderFactory recordBuilder;

        private final Jsonb jsonb;

        private Converter(final LightContainer container) {
            recordBuilder = container.findService(RecordBuilderFactory.class);
            jsonb = container.findService(Jsonb.class);
            converters = new RecordConverters();
        }

        private Object convert(final Object next) {
            return converters.toRecord(registry, next, () -> jsonb, () -> recordBuilder);
        }
    }

    private static class BoundedReaderImpl<T> extends BoundedSource.BoundedReader<T> {

        private BoundedSource<T> source;

        private Input input;

        private Object current;

        private volatile Converter converter;

        BoundedReaderImpl(final BoundedSource<T> source, final Input input) {
            this.source = source;
            this.input = input;
        }

        @Override
        public boolean start() throws IOException {
            input.start();
            return advance();
        }

        @Override
        public boolean advance() {
            final Object next = input.next();
            if (next != null && !Record.class.isInstance(next)) {
                if (converter == null) {
                    synchronized (this) {
                        if (converter == null) {
                            converter = new Converter(ContainerFinder.Instance.get().find(input.plugin()));
                        }
                    }
                }
                current = converter.convert(next);
            } else {
                current = next;
            }
            return current != null;
        }

        @Override
        public T getCurrent() throws NoSuchElementException {
            return (T) current;
        }

        @Override
        public void close() {
            input.stop();
        }

        @Override
        public BoundedSource<T> getCurrentSource() {
            return source;
        }
    }

    private static class UnBoundedReaderImpl<T> extends UnboundedSource.UnboundedReader<T> {

        private UnboundedSource<T, ?> source;

        private Input input;

        private Object current;

        private volatile Converter converter;

        UnBoundedReaderImpl(final UnboundedSource<T, ?> source, final Input input) {
            this.source = source;
            this.input = input;
        }

        @Override
        public boolean start() {
            input.start();
            return advance();
        }

        @Override
        public boolean advance() {
            final Object next = input.next();
            if (next != null && !Record.class.isInstance(next)) {
                if (converter == null) {
                    synchronized (this) {
                        if (converter == null) {
                            converter = new Converter(ContainerFinder.Instance.get().find(input.plugin()));
                        }
                    }
                }
                current = converter.convert(next);
            } else {
                current = next;
            }
            return current != null;
        }

        @Override
        public T getCurrent() throws NoSuchElementException {
            return (T) current;
        }

        @Override
        public void close() {
            input.stop();
        }

        @Override // we can add @Timestamp later on current model if needed, let's start without
        public Instant getCurrentTimestamp() throws NoSuchElementException {
            return Instant.now();
        }

        @Override
        public Instant getWatermark() {
            return Instant.now();
        }

        @Override // we can add a @Checkpoint method on the emitter if needed, let's start without
        public UnboundedSource.CheckpointMark getCheckpointMark() {
            return UnboundedSource.CheckpointMark.NOOP_CHECKPOINT_MARK;
        }

        @Override
        public UnboundedSource<T, ?> getCurrentSource() {
            return source;
        }
    }
}
