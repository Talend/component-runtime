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
package org.talend.sdk.component.runtime.beam.spi;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.beam.runners.direct.DirectOptions;
import org.apache.beam.runners.direct.DirectRunner;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.PipelineResult;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.PBegin;
import org.apache.beam.sdk.values.PCollection;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.api.service.source.ProducerFinder;
import org.talend.sdk.component.runtime.base.Delegated;
import org.talend.sdk.component.runtime.input.Input;
import org.talend.sdk.component.runtime.input.Mapper;
import org.talend.sdk.component.runtime.manager.service.ProducerFinderImpl;
import org.talend.sdk.component.runtime.manager.service.api.ComponentInstantiator;
import org.talend.sdk.component.runtime.serialization.SerializableService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BeamProducerFinder extends ProducerFinderImpl {

    private static final int QUEUE_SIZE = Integer.parseInt(System.getProperty("talend.beam.wrapper.capacity", "1000"));

    private static final int BEAM_PARALLELISM = 10;

    private static final Map<UUID, ArrayBlockingQueue<Record>> QUEUE = new ConcurrentHashMap<>();

    private static final Record END_OF_QUEUE = new Record() {

        @Override
        public Schema getSchema() {
            return null;
        }

        @Override
        public <T> T get(final Class<T> expectedType, final String name) {
            return null;
        }
    };

    @Override
    public Iterator<Record> find(final String familyName, final String inputName, final int version,
            final Map<String, String> configuration) {
        final ComponentInstantiator instantiator = getInstantiator(familyName, inputName);
        final Mapper mapper = findMapper(instantiator, version, configuration);
        try {
            final Input input = mapper.create();
            return iterator(input);
        } catch (Exception e) {
            log.warn("Component Kit Mapper instantiation failed, trying to wrap native beam mapper...");
            final Object delegate = Delegated.class.cast(mapper).getDelegate();
            if (PTransform.class.isInstance(delegate)) {
                final UUID uuid = UUID.randomUUID();
                ArrayBlockingQueue<Record> abq = new ArrayBlockingQueue<>(QUEUE_SIZE, true);
                log.debug("Create and add blocking queue {}.", uuid);
                QUEUE.put(uuid, abq);
                return new BlockingQueueIterator(delegate, familyName, inputName, familyName,
                        PTransform.class.cast(delegate),
                        uuid);
            }
            throw new IllegalStateException(e);
        }
    }

    Object writeReplace() throws ObjectStreamException {
        return new SerializableService(plugin, ProducerFinder.class.getName());
    }

    static class BlockingQueueIterator implements Iterator<Record>, Serializable {

        private final PTransform<PBegin, PCollection<Record>> transform;

        private boolean started;

        private Record next;

        private final UUID queueId;

        public BlockingQueueIterator(final Object delegate, final String rootName, final String name,
                final String plugin,
                final PTransform<PBegin, PCollection<Record>> transform, final UUID queueId) {
            this.transform = transform;
            this.queueId = queueId;
            runDataReadingPipeline();
        }

        @Override
        public boolean hasNext() {
            if (next == null && !started) {
                next = findNext();
                started = true;
            }
            if (next == null) {
                log.debug("Remove blocking queue {}.", this.queueId);
                QUEUE.remove(this.queueId);
            }
            return next != null;
        }

        @Override
        public Record next() {
            if (!hasNext()) {
                return null;
            }
            final Record current = next;
            next = findNext();

            return current;
        }

        private Record findNext() {
            final ArrayBlockingQueue<Record> recordQueue = QUEUE.get(this.queueId);

            Record record = null;
            try {
                log.debug("Take next element from blocking queue: {}, thread: {}.", this.queueId,
                        Thread.currentThread().getId());
                record = recordQueue.take();
                if (record == END_OF_QUEUE) {
                    log.debug("END_OF_QUEUE reached for blocking queue {}, in thread: {}.", this.queueId,
                            Thread.currentThread().getId());
                    return null;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            return record;
        }

        /**
         * <p>
         * Runs a pipeline to read data from an input connector implemented using Beam APIs.
         * </p>
         * <p>
         * Explicit care must be taken to use the appropriate class loader before running
         * the pipeline, as the underlying {@link org.apache.beam.sdk.options.PipelineOptions.DirectRunner}
         * must dynamically find some service implementations using the {@link java.util.ServiceLoader} API.
         * </p>
         * <p>
         * Not specifying the appropriate classloader can lead to weird exceptions like:
         * </p>
         *
         * <pre>
         * No translator known for org.apache.beam.repackaged.direct_java.runners.core.construction.SplittableParDo$PrimitiveBoundedRead
         * </pre>
         *
         * <p>
         * <i>Note: the input connector code is in fact correctly called in its dedicated classloader thanks the
         * various TCK framework wrappers that are in place.</i>
         * </p>
         */
        private void runDataReadingPipeline() {
            final ClassLoader beamAwareClassLoader = Pipeline.class.getClassLoader();
            final ClassLoader callerClassLoader = Thread.currentThread().getContextClassLoader();

            try {
                Thread.currentThread().setContextClassLoader(beamAwareClassLoader);
                DirectOptions options = PipelineOptionsFactory.as(DirectOptions.class);
                options.setRunner(DirectRunner.class);
                options.setTargetParallelism(BEAM_PARALLELISM);
                options.setBlockOnRun(true);
                MyDoFn pushRecord = new MyDoFn(this.queueId);
                ParDo.SingleOutput<Record, Void> of = ParDo.of(pushRecord);
                Pipeline p = Pipeline.create(options);
                p.apply(transform).apply(of);

                Thread th = new Thread(() -> {
                    log.debug("Start thread {} to produce elements in blocking queue {}.",
                            Thread.currentThread().getId(), this.queueId);
                    PipelineResult pipelineResult = p.run();
                    pipelineResult.waitUntilFinish();
                    try {
                        QUEUE.get(this.queueId).put(END_OF_QUEUE);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });

                th.start();

            } finally {
                Thread.currentThread().setContextClassLoader(callerClassLoader);
            }
        }

        private void sleep() {
            try {
                Thread.sleep(30L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    static class MyDoFn extends DoFn<Record, Void> {

        private final UUID queueId;

        public MyDoFn(final UUID queueId) {
            this.queueId = queueId;
        }

        @ProcessElement
        public void processElement(final ProcessContext context) {
            final ArrayBlockingQueue<Record> recordQueue = QUEUE.get(this.queueId);
            try {
                log.debug("Add an element in blocking queue: {}, thread: {}.", this.queueId,
                        Thread.currentThread().getId());
                Record record = context.element();
                recordQueue.put(record);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

}
