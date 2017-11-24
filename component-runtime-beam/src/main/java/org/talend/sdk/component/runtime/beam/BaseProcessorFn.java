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
package org.talend.sdk.component.runtime.beam;

import static java.util.Collections.emptyIterator;
import static java.util.stream.Collectors.toMap;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.beam.sdk.transforms.DoFn;
import org.talend.sdk.component.api.processor.OutputEmitter;
import org.talend.sdk.component.runtime.output.InputFactory;
import org.talend.sdk.component.runtime.output.OutputFactory;
import org.talend.sdk.component.runtime.output.Processor;

import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@NoArgsConstructor
abstract class BaseProcessorFn<I, O> extends DoFn<I, O> {

    protected Processor processor;

    BaseProcessorFn(final Processor processor) {
        this.processor = processor;
    }

    @Setup
    public void setup() throws Exception {
        processor.start();
    }

    @StartBundle
    public void startBundle(final StartBundleContext context) throws Exception {
        processor.beforeGroup();
    }

    @Teardown
    public void tearDown() throws Exception {
        processor.stop();
    }

    protected static final class BeamInputFactory implements InputFactory {

        private final Map<String, Iterator<Serializable>> iterators;

        BeamInputFactory(final DoFn<Map<String, List<Serializable>>, ?>.ProcessContext context) {
            iterators = context.element().entrySet().stream().collect(toMap(Map.Entry::getKey, e -> e.getValue().iterator()));
        }

        @Override
        public Object read(final String name) {
            final Iterator<Serializable> values = iterators.getOrDefault(name, emptyIterator());
            return values.hasNext() ? values.next() : null;
        }
    }

    @RequiredArgsConstructor
    protected static final class BeamOutputFactory implements OutputFactory {

        private final Consumer<Map<String, List<Serializable>>> emit;

        private final Map<String, List<Serializable>> record = new HashMap<>();

        @Override
        public OutputEmitter create(final String name) {
            return new BeamOutputEmitter(record.computeIfAbsent(name, k -> new ArrayList<>()));
        }

        public void postProcessing() {
            if (!record.isEmpty()) {
                emit.accept(record);
            }
        }
    }

    @RequiredArgsConstructor
    private static class BeamOutputEmitter implements OutputEmitter {

        private final List<Serializable> aggregator;

        @Override
        public void emit(final Object value) {
            aggregator.add(Serializable.class.cast(value));
        }
    }
}
