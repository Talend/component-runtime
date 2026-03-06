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
package org.talend.sdk.component.runtime.di.beam;

import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static lombok.AccessLevel.PRIVATE;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Supplier;

import org.apache.beam.sdk.coders.Coder;
import org.apache.beam.sdk.io.Read;
import org.apache.beam.sdk.io.UnboundedSource;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.transforms.windowing.BoundedWindow;
import org.apache.beam.sdk.values.PBegin;
import org.apache.beam.sdk.values.PCollection;
import org.joda.time.Instant;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.runtime.beam.coder.NoCheckpointCoder;
import org.talend.sdk.component.runtime.beam.coder.registry.SchemaRegistryCoder;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = PRIVATE)
public final class InMemoryQueueIO {

    public static PTransform<PBegin, PCollection<Record>> from(final LoopState state) {
        return Read.from(new UnboundedQueuedInput(state.id));
    }

    public static PTransform<PCollection<Record>, PCollection<Void>> to(final LoopState state) {
        return new QueuedOutputTransform(state.id);
    }

    public static class QueuedOutputTransform extends PTransform<PCollection<Record>, PCollection<Void>> {

        private String stateId;

        protected QueuedOutputTransform(final String stateId) {
            this.stateId = stateId;
        }

        @Override
        public PCollection<Void> expand(final PCollection<Record> input) {
            return input.apply(ParDo.of(new QueuedOutput(stateId)));
        }
    }

    public static class QueuedOutput extends DoFn<Record, Void> {

        private String stateId;

        private transient LoopState state;

        protected QueuedOutput(final String stateId) {
            this.stateId = stateId;
        }

        @Setup
        public void onInit() {
            getState().referenceCounting.incrementAndGet();
        }

        @ProcessElement
        public void onElement(final ProcessContext context) {
            final LoopState state = getState();
            state.push(context.element());
            if (state.getRecordCount().decrementAndGet() == 0 && state.isDone()) {
                state.end();
            }
        }

        @Teardown
        public void onTeardown() {
            ofNullable(getState()).filter(s -> s.referenceCounting.decrementAndGet() == 0).ifPresent(LoopState::close);
        }

        private LoopState getState() {
            return state == null ? state = LoopState.lookup(stateId) : state;
        }
    }

    public static class UnboundedQueuedInput extends UnboundedSource<Record, UnboundedSource.CheckpointMark> {

        private SchemaRegistryCoder coder;

        private String stateId;

        protected UnboundedQueuedInput(final String stateId) {
            this.stateId = stateId;
            this.coder = SchemaRegistryCoder.of();
        }

        @Override
        public List<? extends UnboundedSource<Record, CheckpointMark>> split(final int desiredNumSplits,
                final PipelineOptions options) {
            return singletonList(this);
        }

        @Override
        public UnboundedReader<Record> createReader(final PipelineOptions options,
                final UnboundedSource.CheckpointMark checkpointMark) {
            return new UnboundedQueuedReader(this);
        }

        @Override
        public Coder<Record> getOutputCoder() {
            return coder;
        }

        @Override
        public Coder<CheckpointMark> getCheckpointMarkCoder() {
            return new NoCheckpointCoder();
        }

        private static class UnboundedQueuedReader extends UnboundedReader<Record> {

            private final UnboundedQueuedInput source;

            private final LoopState state;

            private volatile Supplier<Instant> waterMarkProvider;

            private Record current;

            private UnboundedQueuedReader(final UnboundedQueuedInput source) {
                this.source = source;
                this.state = LoopState.lookup(source.stateId);
                if (this.state != null) {
                    this.state.referenceCounting.incrementAndGet();
                } else {
                    this.waterMarkProvider = () -> BoundedWindow.TIMESTAMP_MAX_VALUE;
                }
            }

            @Override
            public boolean start() {
                return advance();
            }

            @Override
            public boolean advance() {
                if (state == null) {
                    return false;
                }

                current = state.next();
                if (current != null) {
                    return true;
                }
                this.waterMarkProvider = () -> BoundedWindow.TIMESTAMP_MAX_VALUE;
                return false;
            }

            @Override
            public Record getCurrent() throws NoSuchElementException {
                return current;
            }

            @Override
            public Instant getCurrentTimestamp() throws NoSuchElementException {
                return Instant.now();
            }

            @Override
            public void close() {
                // no-op
            }

            @Override
            public Instant getWatermark() {
                if (waterMarkProvider == null) {
                    waterMarkProvider = Instant::now;
                }
                return waterMarkProvider.get();
            }

            @Override
            public CheckpointMark getCheckpointMark() {
                return CheckpointMark.NOOP_CHECKPOINT_MARK;
            }

            @Override
            public UnboundedSource<Record, ?> getCurrentSource() {
                return source;
            }
        }
    }
}
