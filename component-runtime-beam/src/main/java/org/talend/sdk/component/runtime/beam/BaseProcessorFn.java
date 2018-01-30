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

import static java.util.Collections.emptyIterator;
import static java.util.stream.Collectors.toMap;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Consumer;

import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import javax.json.bind.Jsonb;

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

        private final Map<String, Iterator<JsonValue>> objects;

        BeamInputFactory(final DoFn<JsonObject, ?>.ProcessContext context) {
            objects = context.element().entrySet().stream().collect(
                    toMap(Map.Entry::getKey, e -> e.getValue().asJsonArray().iterator()));
        }

        @Override
        public Object read(final String name) {
            final Iterator<JsonValue> values = objects.getOrDefault(name, emptyIterator());
            return values.hasNext() ? values.next() : null;
        }
    }

    @RequiredArgsConstructor
    protected static final class BeamOutputFactory implements OutputFactory {

        private final Consumer<JsonObject> emit;

        private final JsonBuilderFactory factory;

        private final Jsonb jsonb;

        private Map<String, JsonArrayBuilder> outputs = new HashMap<>();

        @Override
        public OutputEmitter create(final String name) {
            return new BeamOutputEmitter(outputs.computeIfAbsent(name, k -> factory.createArrayBuilder()), jsonb);
        }

        public void postProcessing() {
            if (!outputs.isEmpty()) {
                emit.accept(outputs
                        .entrySet()
                        .stream()
                        .collect(factory::createObjectBuilder, (a, o) -> a.add(o.getKey(), o.getValue()),
                                JsonObjectBuilder::addAll)
                        .build());
            }
        }
    }

    @RequiredArgsConstructor
    private static class BeamOutputEmitter implements OutputEmitter {

        private final JsonArrayBuilder builder;

        private final Jsonb jsonb;

        @Override
        public void emit(final Object value) {
            builder.add(JsonObject.class.isInstance(value) ? JsonObject.class.cast(value)
                    : jsonb.fromJson(jsonb.toJson(value), JsonObject.class));
        }
    }
}
