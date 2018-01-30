/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
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

import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.bind.Jsonb;

import org.apache.beam.sdk.coders.CannotProvideCoderException;
import org.apache.beam.sdk.coders.Coder;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.transforms.windowing.GlobalWindow;
import org.apache.beam.sdk.values.PCollection;
import org.joda.time.Instant;
import org.talend.sdk.component.runtime.beam.coder.JsonpJsonObjectCoder;
import org.talend.sdk.component.runtime.output.Processor;
import org.talend.sdk.component.runtime.serialization.ContainerFinder;
import org.talend.sdk.component.runtime.serialization.LightContainer;

import lombok.NoArgsConstructor;

public final class TalendFn {

    private TalendFn() {
        // no-op
    }

    public static ProcessorTransform asFn(final Processor processor) {
        return new ProcessorTransform(new ProcessorFn(processor));
    }

    @NoArgsConstructor
    private static class ProcessorFn extends BaseProcessorFn<JsonObject, JsonObject> {

        private volatile JsonBuilderFactory factory;

        private volatile Jsonb jsonb;

        ProcessorFn(final Processor processor) {
            super(processor);
        }

        @ProcessElement
        public void processElement(final ProcessContext context) throws Exception {
            ensureInit();
            final BeamOutputFactory output = new BeamOutputFactory(context::output, factory, jsonb);
            processor.onNext(new BeamInputFactory(context), output);
            output.postProcessing();
        }

        @FinishBundle
        public void finishBundle(final FinishBundleContext context) throws Exception {
            ensureInit();
            processor.afterGroup(new BeamOutputFactory(
                    record -> context.output(record, Instant.now(), GlobalWindow.INSTANCE), factory, jsonb));
        }

        private void ensureInit() {
            if (factory == null) {
                synchronized (this) {
                    if (factory == null) {
                        final LightContainer container = ContainerFinder.Instance.get().find(processor.plugin());
                        factory = container.findService(JsonBuilderFactory.class);
                        jsonb = container.findService(Jsonb.class);
                    }
                }
            }
        }
    }

    private static class ProcessorTransform extends PTransform<PCollection<JsonObject>, PCollection<JsonObject>> {

        private final ProcessorFn fn;

        ProcessorTransform(final ProcessorFn fn) {
            this.fn = fn;
        }

        @Override
        public PCollection<JsonObject> expand(final PCollection<JsonObject> input) {
            return input.apply(ParDo.of(fn));
        }

        @Override
        protected Coder<?> getDefaultOutputCoder() throws CannotProvideCoderException {
            return JsonpJsonObjectCoder.of(fn.processor.plugin());
        }
    }
}
