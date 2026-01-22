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
package org.talend.sdk.component.runtime.di.beam.components;

import static lombok.AccessLevel.PRIVATE;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.PipelineResult;
import org.apache.beam.sdk.PipelineRunner;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.runners.TransformHierarchy;
import org.talend.sdk.component.runtime.di.JobStateAware;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// share logic related to the pipeline
@Slf4j
@NoArgsConstructor(access = PRIVATE)
class PipelineInit {

    public static void lazyStart(final JobStateAware.State jobState, final Supplier<DIPipeline> pipelineSupplier) {
        final AtomicBoolean pipelineStarted = jobState.getPipelineStarted();
        if (!pipelineStarted.get() && pipelineStarted.compareAndSet(false, true)) {
            final Pipeline pipeline = pipelineSupplier.get();
            final TransformCounter counter = new TransformCounter();
            pipeline.traverseTopologically(counter);
            if (counter.transforms.get() > 0) {
                final PipelineResult result = pipeline.run();
                new Thread("talend-component-kit-di-pipeline-awaiter") {

                    @Override
                    public void run() {
                        log.debug("Starting to watch beam pipeline");
                        try {
                            result.waitUntilFinish();
                        } finally {
                            final PipelineResult.State state = result.getState();
                            log.debug("Exited pipeline with state {}", state.name());
                            if (state.isTerminal()) {
                                log.info("Beam pipeline ended");
                            } else {
                                log.debug("Beam pipeline ended by interruption");
                            }
                            jobState.getPipelineDone().complete(true);
                        }
                    }
                }.start();
            } else {
                jobState.getPipelineDone().complete(true);
                log.warn("A pipeline was created but not transform were found, is your job correctly configured?");
            }
        }
    }

    static DIPipeline ensurePipeline(final JobStateAware.State jobState) {
        DIPipeline pipeline = jobState.get(JobStateAware.IndirectInstances.Pipeline, DIPipeline.class);
        if (pipeline == null) {
            pipeline = createPipeline(readOptions());
            jobState.set(JobStateAware.IndirectInstances.Pipeline, pipeline);
        }
        return pipeline;
    }

    private static DIPipeline createPipeline(final PipelineOptions options) {
        // return Pipeline.create(options); // ensure we can track inputs to release the semaphore accordingly
        PipelineRunner.fromOptions(options);
        return new DIPipeline(options);
    }

    private static PipelineOptions readOptions() {
        final String[] args = Stream
                .concat(System
                        .getProperties()
                        .stringPropertyNames()
                        .stream()
                        .filter(s -> s.startsWith("talend.beam."))
                        .map(s -> s.substring("talend.beam.".length()) + "=" + System.getProperty(s)), enforcedArgs())
                .toArray(String[]::new);
        return PipelineOptionsFactory.fromArgs(args).create();
    }

    private static Stream<String> enforcedArgs() {
        if (Boolean.getBoolean("talend.runner.skip-defaults")) {
            return Stream.empty();
        }
        return Stream
                .of("--blockOnRun=false", "--enforceImmutability=false", "--enforceEncodability=false",
                        "--targetParallelism=" + Math.max(1, Runtime.getRuntime().availableProcessors()));
    }

    private static class TransformCounter extends Pipeline.PipelineVisitor.Defaults {

        private final AtomicInteger transforms = new AtomicInteger(0);

        @Override
        public Pipeline.PipelineVisitor.CompositeBehavior enterCompositeTransform(final TransformHierarchy.Node node) {
            if (node.isRootNode()) {
                return Pipeline.PipelineVisitor.CompositeBehavior.ENTER_TRANSFORM;
            }
            transforms.incrementAndGet();
            return CompositeBehavior.DO_NOT_ENTER_TRANSFORM; // we have count > 0 so we are good
        }

        @Override
        public void visitPrimitiveTransform(final TransformHierarchy.Node node) {
            transforms.incrementAndGet();
        }
    }
}
