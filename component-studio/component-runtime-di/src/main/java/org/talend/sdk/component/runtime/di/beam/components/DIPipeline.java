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

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.BoundedSource;
import org.apache.beam.sdk.io.Read;
import org.apache.beam.sdk.io.UnboundedSource;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.values.PBegin;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.POutput;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.runtime.di.JobStateAware;
import org.talend.sdk.component.runtime.di.beam.DelegatingBoundedSource;
import org.talend.sdk.component.runtime.di.beam.DelegatingUnBoundedSource;
import org.talend.sdk.component.runtime.di.beam.InMemoryQueueIO;
import org.talend.sdk.component.runtime.di.beam.SettableSourceListener;
import org.talend.sdk.component.runtime.di.beam.SourceListener;

public class DIPipeline extends Pipeline {

    private final Collection<PTransform<?, ?>> transformStack = new ArrayList<>();

    private final Map<PTransform<?, ?>, JobStateAware.State> states = new ConcurrentHashMap<>();

    private String currentState;

    public DIPipeline(final PipelineOptions options) {
        super(options);
    }

    @Override
    public <OutputT extends POutput> OutputT apply(final PTransform<? super PBegin, OutputT> root) {
        transformStack.add(root);
        try {
            return super.apply(wrapTransformIfNeeded(root));
        } finally {
            transformStack.remove(root);
        }
    }

    @Override
    public <OutputT extends POutput> OutputT apply(final String name, final PTransform<? super PBegin, OutputT> root) {
        transformStack.add(root);
        try {
            return super.apply(name, wrapTransformIfNeeded(root));
        } finally {
            transformStack.remove(root);
        }
    }

    private <PT extends POutput> PTransform<? super PBegin, PT>
            wrapTransformIfNeeded(final PTransform<? super PBegin, PT> root) {
        if (Read.Bounded.class.isInstance(root)) {
            final BoundedSource source = Read.Bounded.class.cast(root).getSource();
            final DelegatingBoundedSource boundedSource = new DelegatingBoundedSource(source, null);
            setState(boundedSource);
            return Read.from(boundedSource);
        }
        if (Read.Unbounded.class.isInstance(root)) {
            final UnboundedSource source = Read.Unbounded.class.cast(root).getSource();
            if (InMemoryQueueIO.UnboundedQueuedInput.class.isInstance(source)) {
                return root;
            }

            final DelegatingUnBoundedSource unBoundedSource = new DelegatingUnBoundedSource(source, null);
            setState(unBoundedSource);
            return Read.from(unBoundedSource);
        }
        return root;
    }

    private void setState(final SettableSourceListener settableSourceListener) {
        final Optional<PTransform<?, ?>> transform = transformStack.stream().filter(states::containsKey).findFirst();
        if (!transform.isPresent()) {
            throw new IllegalStateException("No state for transforms " + transformStack);
        }
        transform.ifPresent(s -> {
            final SourceListener.Tracker tracker = new SourceListener.Tracker();
            SourceListener.TRACKERS.put(tracker.getId(), tracker);
            final SourceListener.StateReleaserSourceListener listener = new SourceListener.StateReleaserSourceListener(
                    tracker.getId(), requireNonNull(currentState, "currentState is not set"), null, null);
            settableSourceListener.setSourceListener(listener);
        });
    }

    public void registerStateForTransform(final PTransform<PBegin, PCollection<Record>> transform,
            final JobStateAware.State jobState) {
        states.put(transform, jobState);
    }

    public void withState(final String stateId, final Runnable task) {
        currentState = stateId;
        try {
            task.run();
        } finally {
            currentState = null;
        }
    }
}
