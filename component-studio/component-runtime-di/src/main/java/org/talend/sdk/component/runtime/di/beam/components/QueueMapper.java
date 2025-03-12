/**
 * Copyright (C) 2006-2025 Talend Inc. - www.talend.com
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

import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.values.PBegin;
import org.apache.beam.sdk.values.PCollection;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.runtime.base.Delegated;
import org.talend.sdk.component.runtime.di.JobStateAware;
import org.talend.sdk.component.runtime.di.beam.InMemoryQueueIO;
import org.talend.sdk.component.runtime.di.beam.LoopState;
import org.talend.sdk.component.runtime.input.Input;
import org.talend.sdk.component.runtime.input.Mapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class QueueMapper implements Mapper, JobStateAware, Supplier<DIPipeline>, Delegated {

    private final LoopState state;

    private final String plugin;

    private final String family;

    private final String name;

    private final PTransform<PBegin, PCollection<Record>> transform;

    private State jobState;

    public QueueMapper(final String plugin, final String family, final String name,
            final PTransform<PBegin, PCollection<Record>> transform) {
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
    public long assess() {
        return 1;
    }

    @Override
    public List<Mapper> split(final long desiredSize) {
        return singletonList(this);
    }

    @Override
    public Input create() {
        return new QueueInput(this);
    }

    @Override
    public boolean isStream() {
        return false; // todo?
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
            log.debug("Adding to beam pipeline:\n\n[{}] -> [{}]\n", transform.getName(), family + '#' + name);
        }
        final DIPipeline diPipeline = get();
        diPipeline.withState(getStateId(), () -> diPipeline.apply(transform).apply(InMemoryQueueIO.to(state)));
    }

    @Override
    public void stop() {
        // no-op
    }

    @Override
    public void setState(final State state) {
        this.jobState = state;
        get().registerStateForTransform(requireNonNull(transform, "transform can't be null"), jobState);
    }

    @Override
    public DIPipeline get() {
        return PipelineInit.ensurePipeline(requireNonNull(jobState, "jobState must be non null"));
    }

    @Override
    public Object getDelegate() {
        return transform;
    }

    @RequiredArgsConstructor
    private static class QueueInput implements Input {

        private final QueueMapper parent;

        @Override
        public Object next() {
            PipelineInit.lazyStart(parent.jobState, parent);
            return parent.state.next();
        }

        @Override
        public String plugin() {
            return parent.plugin;
        }

        @Override
        public String rootName() {
            return parent.family;
        }

        @Override
        public String name() {
            return parent.name;
        }

        @Override
        public void start() {
            parent.jobState.getPipelineDone().thenAccept(done -> parent.state.end());
        }

        @Override
        public void stop() {
            // no-op
        }

    }
}
