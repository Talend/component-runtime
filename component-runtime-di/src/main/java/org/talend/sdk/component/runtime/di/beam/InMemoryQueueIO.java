/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
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
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

import javax.json.JsonObject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbConfig;

import org.apache.beam.sdk.coders.Coder;
import org.apache.beam.sdk.io.BoundedSource;
import org.apache.beam.sdk.io.Read;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.PBegin;
import org.apache.beam.sdk.values.PCollection;
import org.talend.sdk.component.runtime.beam.coder.JsonpJsonObjectCoder;
import org.talend.sdk.component.runtime.manager.ComponentManager;

import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@NoArgsConstructor(access = PRIVATE)
public final class InMemoryQueueIO {

    private static final Map<String, LoopState> STATES = new ConcurrentHashMap<>();

    public static PTransform<PBegin, PCollection<JsonObject>> from(final LoopState state) {
        return Read.from(new QueuedInput(state.id));
    }

    public static PTransform<PCollection<JsonObject>, PCollection<Void>> to(final LoopState state) {
        return new QueuedOutputTransform(state.id);
    }

    public static LoopState newTracker(final String plugin) {
        final String id = UUID.randomUUID().toString();
        final LoopState state = new LoopState(id, plugin);
        STATES.putIfAbsent(id, state);
        return state;
    }

    @RequiredArgsConstructor
    public static class LoopState implements AutoCloseable {

        private final String id;

        private final AtomicInteger referenceCounting = new AtomicInteger();

        private final String plugin;

        private final Queue<JsonObject> queue = new ConcurrentLinkedQueue<>();

        private final Semaphore semaphore = new Semaphore(0);

        private volatile Jsonb jsonb;

        private volatile boolean done;

        public void push(final Object value) {
            queue.add(JsonObject.class.isInstance(value) ? JsonObject.class.cast(value) : toJsonObject(value));
            semaphore.release();
        }

        public JsonObject next() {
            try {
                semaphore.acquire();
                return queue.poll();
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }

        public synchronized void done() {
            if (done) {
                return;
            }
            done = true;
            semaphore.release();
        }

        @Override
        public void close() {
            ofNullable(STATES.remove(id)).ifPresent(v -> {
                if (!done) {
                    done();
                }
                ofNullable(jsonb).ifPresent(j -> {
                    try {
                        j.close();
                    } catch (final Exception e) {
                        // no-op
                    }
                });
            });
        }

        private JsonObject toJsonObject(final Object value) {
            if (jsonb == null) {
                synchronized (this) {
                    if (jsonb == null) {
                        final ComponentManager manager = ComponentManager.instance();
                        jsonb = manager
                                .getJsonbProvider()
                                .create()
                                .withProvider(manager.getJsonpProvider())
                                .withConfig(new JsonbConfig().setProperty("johnzon.cdi.activated", false))
                                .build();
                    }
                }
            }
            return jsonb.fromJson(jsonb.toJson(value), JsonObject.class);
        }
    }

    public static class QueuedOutputTransform extends PTransform<PCollection<JsonObject>, PCollection<Void>> {

        private String stateId;

        protected QueuedOutputTransform() {
            // no-op
        }

        protected QueuedOutputTransform(final String stateId) {
            this.stateId = stateId;
        }

        @Override
        public PCollection<Void> expand(final PCollection<JsonObject> input) {
            return input.apply(ParDo.of(new QueuedOutput(stateId)));
        }
    }

    public static class QueuedOutput extends DoFn<JsonObject, Void> {

        private String stateId;

        protected QueuedOutput() {
            // no-op
        }

        protected QueuedOutput(final String stateId) {
            this.stateId = stateId;
        }

        @Setup
        public void onInit() {
            STATES.get(stateId).referenceCounting.incrementAndGet();
        }

        @ProcessElement
        public void onElement(final ProcessContext context) {
            STATES.get(stateId).push(context.element());
        }

        @Teardown
        public void onTeardown() {
            ofNullable(STATES.get(stateId)).filter(s -> s.referenceCounting.decrementAndGet() == 0).ifPresent(
                    LoopState::close);
        }
    }

    public static class QueuedInput extends BoundedSource<JsonObject> {

        private JsonpJsonObjectCoder coder;

        private String stateId;

        protected QueuedInput() {
            // no-op
        }

        protected QueuedInput(final String stateId) {
            this.stateId = stateId;
            this.coder = JsonpJsonObjectCoder.of(STATES.get(stateId).plugin);
        }

        @Override
        public Coder<JsonObject> getOutputCoder() {
            return coder;
        }

        @Override
        public List<? extends BoundedSource<JsonObject>> split(final long desiredBundleSizeBytes,
                final PipelineOptions options) {
            return singletonList(this);
        }

        @Override
        public long getEstimatedSizeBytes(final PipelineOptions options) {
            return 1L;
        }

        @Override
        public BoundedReader<JsonObject> createReader(final PipelineOptions options) {
            return new QueuedInputReader(this);
        }

        private static class QueuedInputReader extends BoundedReader<JsonObject> {

            private final QueuedInput source;

            private final LoopState state;

            private JsonObject current;

            private QueuedInputReader(final QueuedInput source) {
                this.source = source;
                this.state = STATES.get(source.stateId);
                this.state.referenceCounting.incrementAndGet();
            }

            @Override
            public boolean start() {
                return advance();
            }

            @Override
            public boolean advance() {
                current = state.next();
                return current != null;
            }

            @Override
            public JsonObject getCurrent() throws NoSuchElementException {
                if (current == null) {
                    throw new NoSuchElementException();
                }
                return current;
            }

            @Override
            public void close() {
                if (this.state.referenceCounting.decrementAndGet() == 0) {
                    this.state.close();
                }
            }

            @Override
            public BoundedSource<JsonObject> getCurrentSource() {
                return source;
            }
        }
    }
}
