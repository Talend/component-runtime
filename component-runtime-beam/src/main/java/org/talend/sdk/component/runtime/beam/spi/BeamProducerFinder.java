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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

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

    private static final int QUEUE_SIZE = 3; // Integer.parseInt(System.getProperty("talend.beam.wrapper.capacity",
    // "1000"));

    private static final int BEAM_PARALLELISM = 10;

    private static final Map<UUID, ArrayBlockingQueue<Record>> QUEUE = new ConcurrentHashMap<>();

    private static final List<UUID> GENERATED_UUID = new ArrayList<>();

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
                GENERATED_UUID.add(uuid);
                ArrayBlockingQueue<Record> abq = new ArrayBlockingQueue<>(QUEUE_SIZE, true);
                log.info("ADD QUEUE - {}, thread: {}.", uuid, Thread.currentThread().getId());
                QUEUE.put(uuid, abq);
                log.info("** New BlockingQueue, nb:{}, uuid: {}", QUEUE.size(), uuid);
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

        // private final PipelineResult result;

        private boolean started;

        private boolean end;

        private Record next;

        private final UUID queueId;

        private int n = 0;

        // private Thread th;

        public BlockingQueueIterator(final Object delegate, final String rootName, final String name,
                final String plugin,
                final PTransform<PBegin, PCollection<Record>> transform, final UUID queueId) {
            this.transform = transform;
            this.queueId = queueId;
            runDataReadingPipeline();
        }

        @Override
        public boolean hasNext() {
            log.info("CONSUME hasNext {}, next:{}, started:{}", this.queueId, next, started);
            if (next == null && !started) {
                next = findNext();
                started = true;
            }
            if (next == null) {
                log.info("REMOVE QUEUE - {}, thread: {}.", this.queueId, Thread.currentThread().getId());
                QUEUE.remove(this.queueId);
                String allUUID =
                        GENERATED_UUID.stream()
                                .map(uuid -> "\t - " +
                                        uuid.toString())
                                .collect(Collectors.joining("\n"));
                log.info("All generated UUID : \n{}\n------", allUUID);
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

            if (n == 2000) {
                System.out.println("## ID: " + current.getString("id"));
                n = 0;
            } else {
                n++;
            }
            return current;
        }

        private Record findNext() {
            final ArrayBlockingQueue<Record> recordQueue = QUEUE.get(this.queueId);

            log.info("CONSUME - try to poll on {} queue from thread:{}.", this.queueId, Thread.currentThread().getId());
            // Record record = recordQueue.poll();
            Record record = null;
            try {
                record = recordQueue.take();
                if (record == END_OF_QUEUE) {
                    log.info("CONSUME END_OF_QUEUE for {}.", this.queueId);
                    return null;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            log.info("CONSUME - successful poll element {} on {} queue from thread:{}.", record, this.queueId,
                    Thread.currentThread().getId());

            /*
             * int index = 0;
             * while (record == null && (!end)) {
             * end = result != null && result.getState() != PipelineResult.State.RUNNING;
             * if (!end && index > 10) {
             * log.info("waitUntilFinish {}, thread: {}.", this.queueId, Thread.currentThread().getId());
             * result.waitUntilFinish();
             * } else {
             * index++;
             * log.debug("findNext NULL, retry : end={}; size:{}", end, recordQueue.size());
             * sleep();
             * }
             * record = recordQueue.poll();
             * }
             */
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
            log.info("** runDataReadingPipeline : " + this.queueId);
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
                // END_OF_QUEUE

                final UUID uid = this.queueId;
                final long waitms = 5000;
                Thread th = new Thread(() -> {
                    log.info("Start Thread {} for UUID {} and Wait {}.", Thread.currentThread().getId(), uid, waitms);
                    try {
                        Thread.sleep(waitms);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    log.info("RUN PIPELINE Thread {} for UUID {}", Thread.currentThread().getId(), uid);
                    PipelineResult pipelineResult = p.run();
                    pipelineResult.waitUntilFinish();
                    try {
                        QUEUE.get(this.queueId).put(END_OF_QUEUE);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });

                th.start();
                /*
                 * while (result[0] == null) {
                 * sleep();
                 * }
                 */
                // return result[0];
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
            log.info("************************************* NEW MYDOFN ({}) ****************************", queueId);
            this.queueId = queueId;
        }

        @ProcessElement
        public void processElement(final ProcessContext context) {
            final ArrayBlockingQueue<Record> recordQueue = QUEUE.get(this.queueId);
            try {
                Record record = context.element();
                log.info("PRODUCER - Queue {}, try to add element:{}, size:{}; thread:{}...", this.queueId, record,
                        recordQueue.size(),
                        Thread.currentThread().getId());
                recordQueue.put(record);
                log.info("\tPRODUCER - Queue {}, element added:{}, size:{}; thread:{}...", this.queueId, record,
                        recordQueue.size(),
                        Thread.currentThread().getId());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

    }

}
