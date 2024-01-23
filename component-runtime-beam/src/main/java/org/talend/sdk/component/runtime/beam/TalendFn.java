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
package org.talend.sdk.component.runtime.beam;

import java.util.function.Consumer;

import org.apache.beam.sdk.coders.Coder;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.transforms.windowing.GlobalWindow;
import org.apache.beam.sdk.values.PCollection;
import org.joda.time.Instant;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.runtime.beam.coder.registry.SchemaRegistryCoder;
import org.talend.sdk.component.runtime.output.Processor;

import lombok.NoArgsConstructor;

public final class TalendFn {

    private TalendFn() {
        // no-op
    }

    public static PTransform<PCollection<Record>, PCollection<Record>> asFn(final Processor processor) {
        return new ProcessorTransform(new ProcessorFn(processor));
    }

    @NoArgsConstructor
    private static class ProcessorFn extends BaseProcessorFn<Record> {

        ProcessorFn(final Processor processor) {
            super(processor);
        }

        @Override
        protected Consumer<Record> toEmitter(final ProcessContext context) {
            return context::output;
        }

        @Override
        protected BeamOutputFactory getFinishBundleOutputFactory(final FinishBundleContext context) {
            return new BeamMultiOutputFactory(record -> context.output(record, Instant.now(), GlobalWindow.INSTANCE),
                    recordFactory, jsonb);
        }
    }

    private static class ProcessorTransform extends PTransform<PCollection<Record>, PCollection<Record>> {

        private final ProcessorFn fn;

        ProcessorTransform(final ProcessorFn fn) {
            this.fn = fn;
        }

        @Override
        public PCollection<Record> expand(final PCollection<Record> input) {
            return input.apply(ParDo.of(fn));
        }

        @Override
        protected Coder<?> getDefaultOutputCoder() {
            return SchemaRegistryCoder.of();
        }
    }
}
