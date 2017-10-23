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
package org.talend.component.runtime.beam;

import static java.util.Optional.ofNullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.windowing.GlobalWindow;
import org.apache.beam.sdk.values.PCollectionView;
import org.apache.beam.sdk.values.TupleTag;
import org.joda.time.Instant;
import org.talend.component.api.processor.OutputEmitter;
import org.talend.component.runtime.output.Branches;
import org.talend.component.runtime.output.InputFactory;
import org.talend.component.runtime.output.OutputFactory;
import org.talend.component.runtime.output.Processor;

import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@NoArgsConstructor
abstract class BaseProcessorFn<I, O> extends DoFn<I, O> {

    protected Map<String, PCollectionView<?>> incomingViews;

    protected Processor processor;

    BaseProcessorFn(final Processor processor, final Map<String, PCollectionView<?>> incomingViews) {
        this.processor = processor;
        this.incomingViews = incomingViews;
    }

    @Setup
    public void setup() throws Exception {
        processor.start();
    }

    @StartBundle
    public void startBundle(final StartBundleContext context) throws Exception {
        processor.beforeGroup();
    }

    @FinishBundle
    public void finishBundle(final FinishBundleContext context) throws Exception {
        processor.afterGroup(new BeamFinishOutputFactory(context));
    }

    @Teardown
    public void tearDown() throws Exception {
        processor.stop();
    }

    @RequiredArgsConstructor
    protected static final class BeamInputFactory implements InputFactory {

        private final DoFn.ProcessContext context;

        private final Map<String, PCollectionView<?>> views;

        @Override
        public Object read(final String name) {
            return Branches.DEFAULT_BRANCH.equals(name) ? context.element() : readSide(name);
        }

        private Object readSide(final String name) {
            return context.sideInput(
                    ofNullable(views.get(name)).orElseThrow(() -> new IllegalArgumentException("No view for " + name)));
        }
    }

    @RequiredArgsConstructor
    protected static final class BeamFinishOutputFactory implements OutputFactory {

        private final DoFn.FinishBundleContext context;

        private final ConcurrentMap<String, OutputEmitter> emitters = new ConcurrentHashMap<>();

        @Override
        public OutputEmitter create(final String name) {
            return emitters.computeIfAbsent(name,
                    n -> Branches.DEFAULT_BRANCH.equals(n) ? new BeamFinishDefaultOutputEmitter(context)
                            : new BeamFinishOutputEmitter(new TupleTag<>(name), context));
        }
    }

    @RequiredArgsConstructor
    protected static final class BeamOutputFactory implements OutputFactory {

        private final DoFn.ProcessContext context;

        private final ConcurrentMap<String, OutputEmitter> emitters = new ConcurrentHashMap<>();

        @Override
        public OutputEmitter create(final String name) {
            return emitters.computeIfAbsent(name, n -> Branches.DEFAULT_BRANCH.equals(n) ? new BeamDefaultOutputEmitter(context)
                    : new BeamOutputEmitter(new TupleTag<>(name), context));
        }
    }

    @RequiredArgsConstructor
    private static class BeamDefaultOutputEmitter implements OutputEmitter {

        private final DoFn.ProcessContext context;

        @Override
        public void emit(final Object value) {
            context.output(value);
        }
    }

    @RequiredArgsConstructor
    private static class BeamFinishDefaultOutputEmitter implements OutputEmitter {

        private final DoFn.FinishBundleContext context;

        @Override
        public void emit(final Object value) {
            context.output(value, Instant.now(), GlobalWindow.INSTANCE);
        }
    }

    @RequiredArgsConstructor
    private static class BeamOutputEmitter implements OutputEmitter {

        private final TupleTag<Object> key;

        private final DoFn.ProcessContext context;

        @Override
        public void emit(final Object value) {
            context.output(key, value);
        }
    }

    @RequiredArgsConstructor
    private static class BeamFinishOutputEmitter implements OutputEmitter {

        private final TupleTag<Object> key;

        private final DoFn.FinishBundleContext context;

        @Override
        public void emit(final Object value) {
            context.output(key, value, Instant.now(), GlobalWindow.INSTANCE);
        }
    }
}
