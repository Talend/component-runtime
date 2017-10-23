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
package org.talend.components.runtime.beam;

import java.util.Map;

import org.apache.beam.sdk.coders.CannotProvideCoderException;
import org.apache.beam.sdk.coders.Coder;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.PCollectionView;
import org.talend.components.runtime.output.Processor;

import lombok.NoArgsConstructor;

public final class TalendFn {

    private TalendFn() {
        // no-op
    }

    public static <A, B> ProcessorTransform<A, B> asFn(final Processor processor, final Map<String, PCollectionView<?>> views) {
        return new ProcessorTransform<>(new ProcessorFn<>(processor, views));
    }

    // todo: migrate to timer/state API for @FinishBundle to unify stream and batch when supported by all runners
    @NoArgsConstructor
    private static class ProcessorFn<A, B> extends BaseProcessorFn<A, B> {

        ProcessorFn(final Processor processor, final Map<String, PCollectionView<?>> views) {
            super(processor, views);
        }

        @ProcessElement
        public void processElement(final ProcessContext context) throws Exception {
            processor.onNext(new BeamInputFactory(context, incomingViews), new BeamOutputFactory(context));
        }
    }

    private static class ProcessorTransform<A, B> extends PTransform<PCollection<? extends A>, PCollection<B>> {

        private final ProcessorFn<A, B> fn;

        ProcessorTransform(final ProcessorFn<A, B> fn) {
            this.fn = fn;
        }

        @Override
        public PCollection<B> expand(PCollection<? extends A> input) {
            return input.apply(ParDo.of(fn));
        }

        @Override
        protected Coder<?> getDefaultOutputCoder() throws CannotProvideCoderException {
            return TalendCoder.of();
        }
    }
}
