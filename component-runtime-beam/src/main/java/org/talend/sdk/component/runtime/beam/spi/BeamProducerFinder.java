/**
 * Copyright (C) 2006-2023 Talend Inc. - www.talend.com
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
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

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

    static final int CAPACITY = Integer.parseInt(System.getProperty("talend.beam.wrapper.capacity", "100000"));

    private static List<Queue<Record>> queue = new ArrayList<>();

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
                queue.add(new ArrayBlockingQueue<>(CAPACITY, true));
                return new QueueInput(delegate, familyName, inputName, familyName, PTransform.class.cast(delegate),
                        queue.size() - 1);
            }
            throw new IllegalStateException(e);
        }
    }

    Object writeReplace() throws ObjectStreamException {
        return new SerializableService(plugin, ProducerFinder.class.getName());
    }

    static class QueueInput implements Iterator<Record>, Serializable {

        private final PTransform<PBegin, PCollection<Record>> transform;

        private final PipelineResult result;

        private boolean started;

        private boolean end;

        private Record next;

        private final int queueIndex;

        private Thread th;

        public QueueInput(final Object delegate, final String rootName, final String name, final String plugin,
                final PTransform<PBegin, PCollection<Record>> transform, final int queueIndex) {
            // super(delegate, rootName, name, plugin);
            System.out.println("QueueInput constructor thread:" + Thread.currentThread().getId());
            this.transform = transform;
            this.queueIndex = queueIndex;
            result = runDataReadingPipeline();

            // System.out.println("result " + result.getState().name() );
            System.out.println("QUEUE " + queue.get(this.queueIndex).size());
            System.out.flush();
        }

        @Override
        public boolean hasNext() {
            if (next == null && !started) {
                next = findNext();
                started = true;
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
            System.out.println("Find Next, Thread " + Thread.currentThread().getId() + "; Queue size " + queue.size());
            System.out.flush();
            Record record = queue.get(this.queueIndex).poll();
            int index = 0;
            while (record == null && (!end)) {
                end = result != null && result.getState() != PipelineResult.State.RUNNING;
                if (!end && index > 10) {
                    result.waitUntilFinish();
                }
                index++;
                System.out.println("findNext NULL, retry : end=" + end + "; size:" + queue.get(this.queueIndex).size());
                sleep();

                record = queue.get(this.queueIndex).poll();
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
        private PipelineResult runDataReadingPipeline() {
            final ClassLoader beamAwareClassLoader = Pipeline.class.getClassLoader();
            final ClassLoader callerClassLoader = Thread.currentThread().getContextClassLoader();

            try {
                Thread.currentThread().setContextClassLoader(beamAwareClassLoader);
                DirectOptions options = PipelineOptionsFactory.as(DirectOptions.class);
                options.setRunner(DirectRunner.class);
                options.setTargetParallelism(10);
                options.setBlockOnRun(false);
                MyDoFn pushRecord = new MyDoFn(queueIndex);
                ParDo.SingleOutput<Record, Void> of = ParDo.of(pushRecord);
                Pipeline p = Pipeline.create(options);
                p.apply(transform).apply(of);

                final PipelineResult[] result = new PipelineResult[1];
                th = new Thread(() -> {
                    result[0] = p.run();
                });
                this.th.start();
                System.out.println("Run pipeline");
                while (result[0] == null) {
                    System.out.println("WAIT RESULT");
                    sleep();
                }
                return result[0];
            } finally {
                Thread.currentThread().setContextClassLoader(callerClassLoader);
            }
        }

        private void sleep() {
            try {
                Thread.sleep(100L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    static class MyDoFn extends DoFn<Record, Void> {

        private final int queueIndex;

        public MyDoFn(final int queueIndex) {
            this.queueIndex = queueIndex;
        }

        @ProcessElement
        public void processElement(final ProcessContext context) {

            boolean ok = queue.get(this.queueIndex).offer(context.element());
            System.out.println("queue injected " + queue.get(this.queueIndex).size() + "; ok=" + ok + "; thread:"
                    + Thread.currentThread().getId());
            System.out.flush();
            while (!ok) {
                if (queue.get(this.queueIndex).size() >= CAPACITY) {
                    System.out.println("######### ERROR #######");
                    final String msg = String.format(
                            "Wrapper queue if full (capacity: %d). Consider increasing it according data with talend.beam.wrapper.capacity property.",
                            CAPACITY);
                    log.error("[processElement] {}", msg);
                    throw new IllegalStateException(msg);
                }
                sleep();
                ok = queue.get(this.queueIndex).offer(context.element());
                System.out.println("\tqueue injected retry " + queue.get(this.queueIndex).size() + "; ok=" + ok
                        + "; thread:" + Thread.currentThread().getId());
            }
        }

        private void sleep() {
            try {
                Thread.sleep(100L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

    }

}