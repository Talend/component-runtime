// ============================================================================
//
// Copyright (C) 2006-2017 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
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
