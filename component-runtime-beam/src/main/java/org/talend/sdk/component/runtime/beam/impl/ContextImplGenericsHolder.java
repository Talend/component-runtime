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
package org.talend.sdk.component.runtime.beam.impl;

import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.windowing.BoundedWindow;
import org.apache.beam.sdk.transforms.windowing.PaneInfo;
import org.apache.beam.sdk.values.PCollectionView;
import org.apache.beam.sdk.values.TupleTag;
import org.joda.time.Instant;
import org.talend.sdk.component.runtime.output.Branches;
import org.talend.sdk.component.runtime.output.InputFactory;
import org.talend.sdk.component.runtime.output.OutputFactory;

import lombok.RequiredArgsConstructor;

public class ContextImplGenericsHolder<I, O> extends DoFn<I, O> {

    public ProcessContext newContext(final PipelineOptions options, final InputFactory inputs,
            final OutputFactory outputs) {
        return new ProcessContextImpl(options, inputs, outputs);
    }

    public StartBundleContext newStartContext(final PipelineOptions options) {
        return new StartContextImpl(options);
    }

    public FinishBundleContext newFinishContext(final PipelineOptions options, final OutputFactory outputs) {
        return new FinishContextImpl(options, outputs);
    }

    @RequiredArgsConstructor
    public class StartContextImpl extends DoFn<I, O>.StartBundleContext {

        private final PipelineOptions options;

        @Override
        public PipelineOptions getPipelineOptions() {
            return options;
        }
    }

    @RequiredArgsConstructor
    public class FinishContextImpl extends DoFn<I, O>.FinishBundleContext {

        private final PipelineOptions options;

        private final OutputFactory outputs;

        @Override
        public PipelineOptions getPipelineOptions() {
            return options;
        }

        @Override
        public void output(final O output, final Instant timestamp, final BoundedWindow window) {
            outputs.create(Branches.DEFAULT_BRANCH).emit(output);
        }

        @Override
        public <T> void output(final TupleTag<T> tag, final T output, final Instant timestamp,
                final BoundedWindow window) {
            outputs.create(tag.getId()).emit(output);
        }
    }

    @RequiredArgsConstructor
    public class ProcessContextImpl extends DoFn<I, O>.ProcessContext {

        private final PipelineOptions options;

        private final InputFactory inputs;

        private final OutputFactory outputs;

        @Override
        public I element() {
            return (I) inputs.read(Branches.DEFAULT_BRANCH);
        }

        @Override
        public Instant timestamp() {
            return Instant.now();
        }

        @Override
        public <T> T sideInput(final PCollectionView<T> view) {
            throw new UnsupportedOperationException("side inputs not yet supported");
        }

        @Override
        public PaneInfo pane() {
            return PaneInfo.NO_FIRING;
        }

        @Override
        public void updateWatermark(final Instant watermark) {
            throw new UnsupportedOperationException("watermark not supported");
        }

        @Override
        public PipelineOptions getPipelineOptions() {
            return options;
        }

        @Override
        public void output(final Object output) {
            outputs.create(Branches.DEFAULT_BRANCH).emit(output);
        }

        @Override
        public <T> void output(final TupleTag<T> tag, final T output) {
            outputs.create(tag.getId()).emit(output);
        }

        @Override
        public <T> void outputWithTimestamp(final TupleTag<T> tag, final T output, final Instant timestamp) {
            output(output);
        }

        @Override
        public void outputWithTimestamp(final Object output, final Instant timestamp) {
            output(output);
        }
    }

}
