/**
 * Copyright (C) 2006-2017 Talend Inc. - www.talend.com
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

import static java.util.stream.Collectors.toList;
import static org.apache.beam.sdk.annotations.Experimental.Kind.SOURCE_SINK;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Consumer;

import org.apache.beam.sdk.annotations.Experimental;
import org.apache.beam.sdk.coders.Coder;
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
import org.joda.time.Instant;
import org.talend.sdk.component.runtime.base.Lifecycle;
import org.talend.sdk.component.runtime.input.Input;
import org.talend.sdk.component.runtime.input.Mapper;
import org.talend.sdk.component.runtime.output.Processor;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Experimental(SOURCE_SINK)
public final class TalendIO {

    public static <T extends Serializable> Base<PBegin, PCollection<T>, Mapper> read(final Mapper mapper) {
        return mapper.isStream() ? new InfiniteRead<>(mapper) : new Read<>(mapper);
    }

    public static Write write(final Processor output) {
        return new Write(output);
    }

    private static abstract class Base<A extends PInput, B extends POutput, D extends Lifecycle>
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
    }

    private static class Read<T> extends Base<PBegin, PCollection<T>, Mapper> {

        private Read(final Mapper delegate) {
            super(delegate);
        }

        @Override
        public PCollection<T> expand(final PBegin incoming) {
            return incoming.apply(org.apache.beam.sdk.io.Read.from(new BoundedSourceImpl<>(delegate)));
        }
    }

    private static class InfiniteRead<T> extends Base<PBegin, PCollection<T>, Mapper> {

        private InfiniteRead(final Mapper delegate) {
            super(delegate);
        }

        @Override
        public PCollection<T> expand(final PBegin incoming) {
            return incoming.apply(org.apache.beam.sdk.io.Read.from(new UnBoundedSourceImpl<>(delegate)));
        }
    }

    public static class Write extends Base<PCollection<Map<String, List<Serializable>>>, PDone, Processor> {

        private Write(final Processor delegate) {
            super(delegate);
        }

        @Override
        public PDone expand(final PCollection<Map<String, List<Serializable>>> incoming) {
            incoming.apply(ParDo.of(new WriteFn(delegate)));
            return PDone.in(incoming.getPipeline());
        }
    }

    @NoArgsConstructor
    private static class WriteFn extends BaseProcessorFn<Map<String, List<Serializable>>, Void> {

        private static final Consumer<Map<String, List<Serializable>>> NOOP_CONSUMER = record -> {
        };

        WriteFn(final Processor processor) {
            super(processor);
        }

        @ProcessElement
        public void processElement(final ProcessContext context) throws Exception {
            processor.onNext(new BeamInputFactory(context), new BeamOutputFactory(NOOP_CONSUMER));
        }

        @FinishBundle
        public void finishBundle(final FinishBundleContext context) throws Exception {
            processor.afterGroup(new BeamOutputFactory(NOOP_CONSUMER));
        }
    }

    @NoArgsConstructor
    @AllArgsConstructor
    private static class BoundedSourceImpl<T> extends BoundedSource<T> {

        private Mapper mapper;

        @Override
        public List<? extends BoundedSource<T>> split(final long desiredBundleSizeBytes, final PipelineOptions options)
            throws Exception {
            mapper.start();
            try {
                return mapper.split(desiredBundleSizeBytes).stream().map(i -> new BoundedSourceImpl<T>(i))
                    .collect(toList());
            } finally {
                mapper.stop();
            }
        }

        @Override
        public long getEstimatedSizeBytes(final PipelineOptions options) throws Exception {
            mapper.start();
            try {
                return mapper.assess();
            } finally {
                mapper.stop();
            }
        }

        @Override
        public BoundedReader<T> createReader(final PipelineOptions options) throws IOException {
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
        public Coder<T> getDefaultOutputCoder() {
            return (Coder<T>) TalendCoder.of();
        }
    }

    @NoArgsConstructor
    @AllArgsConstructor
    private static class UnBoundedSourceImpl<T> extends UnboundedSource<T, EmptyCheckMark> {

        private Mapper mapper;

        @Override
        public List<? extends UnboundedSource<T, EmptyCheckMark>> split(final int desiredNumSplits,
            final PipelineOptions options) throws Exception {
            mapper.start();
            try {
                return mapper.split(desiredNumSplits).stream().map(i -> new UnBoundedSourceImpl<T>(i))
                    .collect(toList());
            } finally {
                mapper.stop();
            }
        }

        @Override
        public UnboundedReader<T> createReader(final PipelineOptions options, final EmptyCheckMark checkpointMark)
            throws IOException {
            return new UnBoundedReaderImpl<>(this, mapper.create());
        }

        @Override
        public Coder<T> getDefaultOutputCoder() {
            return (Coder<T>) TalendCoder.of();
        }

        @Override
        public Coder getCheckpointMarkCoder() {
            return TalendCoder.of();
        }
    }

    private static class BoundedReaderImpl<T> extends BoundedSource.BoundedReader<T> {

        private BoundedSource<T> source;

        private Input input;

        private Object current;

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
        public boolean advance() throws IOException {
            current = input.next();
            return current != null;
        }

        @Override
        public T getCurrent() throws NoSuchElementException {
            return (T) current;
        }

        @Override
        public void close() throws IOException {
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

        UnBoundedReaderImpl(final UnboundedSource<T, ?> source, final Input input) {
            this.source = source;
            this.input = input;
        }

        @Override
        public boolean start() throws IOException {
            input.start();
            return advance();
        }

        @Override
        public boolean advance() throws IOException {
            current = input.next();
            return current != null;
        }

        @Override
        public T getCurrent() throws NoSuchElementException {
            return (T) current;
        }

        @Override
        public void close() throws IOException {
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
            return new EmptyCheckMark();
        }

        @Override
        public UnboundedSource<T, ?> getCurrentSource() {
            return source;
        }
    }

    private static final class EmptyCheckMark implements UnboundedSource.CheckpointMark {

        @Override
        public void finalizeCheckpoint() throws IOException {
            // no-op
        }

        @Override
        public int hashCode() {
            return 0;
        }

        @Override
        public boolean equals(final Object obj) {
            return EmptyCheckMark.class.isInstance(obj);
        }
    }
}
