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
package org.talend.sdk.component.runtime.di.beam;

import static java.lang.Thread.sleep;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import javax.json.JsonObject;

import org.apache.beam.sdk.PipelineResult;
import org.apache.beam.sdk.coders.SerializableCoder;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.testing.TestPipeline;
import org.apache.beam.sdk.transforms.Create;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.ParDo;
import org.junit.Rule;
import org.junit.Test;
import org.talend.sdk.component.runtime.beam.coder.JsonpJsonObjectCoder;
import org.talend.sdk.component.runtime.manager.ComponentManager;

import lombok.AllArgsConstructor;
import lombok.Data;

public class InMemoryQueueIOTest implements Serializable {

    private static final Collection<JsonObject> INPUT_OUTPUTS = new CopyOnWriteArrayList<>();

    @Rule
    public transient final TestPipeline pipeline =
            TestPipeline.fromOptions(PipelineOptionsFactory.fromArgs("--blockOnRun=false").create());

    @Test(timeout = 60000)
    public void input() {
        INPUT_OUTPUTS.clear();

        final PipelineResult result;
        try (final InMemoryQueueIO.LoopState state = InMemoryQueueIO.newTracker(null)) {
            IntStream.range(0, 2).forEach(i -> state.push(new RowStruct(i)));

            pipeline.apply(InMemoryQueueIO.from(state)).apply(ParDo.of(new DoFn<JsonObject, Void>() {

                @ProcessElement
                public void onElement(final ProcessContext context) {
                    INPUT_OUTPUTS.add(context.element());
                }
            }));

            result = pipeline.run();

            IntStream.range(2, 5).forEach(i -> state.push(new RowStruct(i)));
            state.done(); // for inputs it is key to notify beam we are done

            final long end = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(2);
            while (INPUT_OUTPUTS.size() < 5 && end - System.currentTimeMillis() >= 0) {
                try {
                    sleep(150);
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        result.waitUntilFinish();
        assertEquals(5, INPUT_OUTPUTS.size());
        assertEquals(IntStream.range(0, 5).boxed().collect(toSet()),
                INPUT_OUTPUTS.stream().mapToInt(o -> o.getInt("id")).boxed().collect(toSet()));
    }

    @Test(timeout = 60000)
    public void output() {
        final Collection<JsonObject> objects = new ArrayList<>();
        try (final InMemoryQueueIO.LoopState state = InMemoryQueueIO.newTracker(null)) {
            pipeline
                    .apply(Create.of(IntStream.range(0, 5).mapToObj(RowStruct::new).collect(toList())))
                    .setCoder(SerializableCoder.of(RowStruct.class))
                    .apply(ParDo.of(new DoFn<RowStruct, JsonObject>() {

                        @ProcessElement
                        public void onElement(final ProcessContext context) {
                            final JsonObject object = ComponentManager
                                    .instance()
                                    .getJsonpBuilderFactory()
                                    .createObjectBuilder()
                                    .add("id", context.element().id)
                                    .build();
                            context.output(object);
                        }
                    }))
                    .setCoder(JsonpJsonObjectCoder.of(null))
                    .apply(InMemoryQueueIO.to(state));

            pipeline.run().waitUntilFinish();

            JsonObject next;
            do {
                next = state.next();
                if (next != null) {
                    objects.add(next);
                }
            } while (next != null);
        }
        assertEquals(5, objects.size());
        assertEquals(IntStream.range(0, 5).boxed().collect(toSet()),
                objects.stream().mapToInt(o -> o.getInt("id")).boxed().collect(toSet()));
    }

    @Data
    @AllArgsConstructor
    public static class RowStruct implements Serializable {

        public int id;
    }
}
