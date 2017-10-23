/**
 *  Copyright (C) 2006-2017 Talend Inc. - www.talend.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.talend.components.runtime.beam.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.runners.TransformHierarchy;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.PBegin;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.POutput;
import org.apache.beam.sdk.values.PValue;

import lombok.Getter;

class CapturingPipeline extends Pipeline {

    @Getter
    private PTransform<? super PBegin, ?> root;

    CapturingPipeline(final PipelineOptions options) {
        super(options);
    }

    @Override
    public <OutputT extends POutput> OutputT apply(final PTransform<? super PBegin, OutputT> root) {
        this.root = root;
        return super.apply(root);
    }

    @Override
    public <OutputT extends POutput> OutputT apply(final String name, final PTransform<? super PBegin, OutputT> root) {
        this.root = root;
        return super.apply(name, root);
    }

    static class SinkExtractor implements PipelineVisitor {

        @Getter
        private Collection<DoFn<?, ?>> outputs = new ArrayList<>();

        @Override
        public void enterPipeline(final Pipeline p) {
            // no-op
        }

        @Override
        public CompositeBehavior enterCompositeTransform(final TransformHierarchy.Node node) {
            return CompositeBehavior.ENTER_TRANSFORM;
        }

        @Override
        public void leaveCompositeTransform(final TransformHierarchy.Node node) {
            // no-op
        }

        @Override
        public void visitPrimitiveTransform(final TransformHierarchy.Node node) {
            final PTransform<?, ?> transform = node.getTransform();
            if (!ParDo.MultiOutput.class.isInstance(transform)) {
                return;
            }
            final ParDo.MultiOutput<?, ?> multiOutput = ParDo.MultiOutput.class.cast(transform);
            final DoFn<?, ?> fn = multiOutput.getFn();

            outputs.add(fn);
        }

        @Override
        public void visitValue(final PValue value, final TransformHierarchy.Node producer) {
            // no-op
        }

        @Override
        public void leavePipeline(final Pipeline pipeline) {
            // no-op
        }
    }

    static class SourceExtractor implements PipelineVisitor {

        @Getter
        private PTransform<? super PBegin, ?> transform;

        @Getter
        private List<PTransform<PCollection<?>, ?>> transforms = new ArrayList<>();

        @Override
        public void enterPipeline(final Pipeline p) {
            // no-op
        }

        @Override
        public CompositeBehavior enterCompositeTransform(final TransformHierarchy.Node node) {
            return CompositeBehavior.ENTER_TRANSFORM;
        }

        @Override
        public void leaveCompositeTransform(final TransformHierarchy.Node node) {
            // no-op
        }

        @Override
        public void visitPrimitiveTransform(final TransformHierarchy.Node node) {
            final PTransform<?, ?> transform = node.getTransform();
            if (this.transform != null) {
                this.transforms.add((PTransform<PCollection<?>, PCollection<?>>) transform);
            } else {
                this.transform = (PTransform<? super PBegin, ?>) transform;
            }
        }

        @Override
        public void visitValue(final PValue value, final TransformHierarchy.Node producer) {
            // no-op
        }

        @Override
        public void leavePipeline(final Pipeline pipeline) {
            // no-op
        }
    }
}
