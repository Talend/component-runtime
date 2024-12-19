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
package org.talend.sdk.component.runtime.di.beam.components;

import static java.util.Objects.requireNonNull;

import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.values.PCollection;
import org.talend.sdk.component.runtime.base.Delegated;
import org.talend.sdk.component.runtime.di.JobStateAware;
import org.talend.sdk.component.runtime.di.beam.InMemoryQueueIO;
import org.talend.sdk.component.runtime.di.beam.LoopState;
import org.talend.sdk.component.runtime.output.Branches;
import org.talend.sdk.component.runtime.output.InputFactory;
import org.talend.sdk.component.runtime.output.OutputFactory;
import org.talend.sdk.component.runtime.output.Processor;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class QueueOutput implements Processor, JobStateAware, Supplier<DIPipeline>, Delegated {

    private final LoopState state;

    private final String plugin;

    private final String family;

    private final String name;

    private final PTransform<PCollection<?>, ?> transform;

    private State jobState;

    public QueueOutput(final String plugin, final String family, final String name,
            final PTransform<PCollection<?>, ?> transform) {
        this.plugin = plugin;
        this.family = family;
        this.name = name;
        this.transform = transform;
        this.state = LoopState.newTracker(plugin);
        log.debug("Associating state {} to {}#{}", this.state.getId(), family, name);
    }

    public String getStateId() {
        return state.getId();
    }

    @Override
    public void onNext(final InputFactory input, final OutputFactory output) {
        PipelineInit.lazyStart(jobState, this);
        state.push(input.read(Branches.DEFAULT_BRANCH));
    }

    @Override
    public void beforeGroup() {
        // no-op
    }

    @Override
    public void afterGroup(final OutputFactory output) {
        // no-op
    }

    @Override
    public String plugin() {
        return plugin;
    }

    @Override
    public String rootName() {
        return family;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public void start() {
        if (transform == null) {
            log.error("No transform for " + plugin + "#" + family + "#" + name);
            return;
        }
        if (log.isDebugEnabled()) {
            log.debug("Adding to beam pipeline:\n\n[{}] -> [{}]\n", family + '#' + name, transform.getName());
        }
        get()
                .apply(InMemoryQueueIO.from(state))
                // todo: json to pojo? for now assume the PTransform handles it
                .apply(transform);
    }

    @Override
    public void stop() {
        PipelineInit.lazyStart(jobState, this); // empty dataset case
        state.end();
        try {
            jobState.getPipelineDone().get();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (final ExecutionException e) {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    public void setState(final State state) {
        this.jobState = state;
    }

    @Override
    public DIPipeline get() {
        return PipelineInit.ensurePipeline(requireNonNull(jobState, "jobState must be non null"));
    }

    @Override
    public Object getDelegate() {
        return transform;
    }
}
