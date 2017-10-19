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
import org.talend.components.runtime.output.Branches;
import org.talend.components.runtime.output.InputFactory;
import org.talend.components.runtime.output.OutputFactory;
import org.talend.components.runtime.output.Processor;

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
