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

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.apache.ziplock.JarLocation.jarLocation;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.json.JsonObject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.apache.beam.sdk.PipelineResult;
import org.apache.beam.sdk.testing.PAssert;
import org.apache.beam.sdk.testing.TestPipeline;
import org.apache.beam.sdk.transforms.Create;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.PCollection;
import org.junit.Rule;
import org.junit.Test;
import org.talend.sdk.component.runtime.beam.coder.JsonbCoder;
import org.talend.sdk.component.runtime.beam.coder.JsonpJsonObjectCoder;
import org.talend.sdk.component.runtime.beam.transform.ViewsMappingTransform;
import org.talend.sdk.component.runtime.input.Input;
import org.talend.sdk.component.runtime.input.Mapper;
import org.talend.sdk.component.runtime.output.Branches;
import org.talend.sdk.component.runtime.output.InputFactory;
import org.talend.sdk.component.runtime.output.OutputFactory;
import org.talend.sdk.component.runtime.output.Processor;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class TalendIOTest implements Serializable {

    private static final String PLUGIN = jarLocation(TalendIOTest.class).getAbsolutePath();

    private static final Jsonb JSONB = JsonbBuilder.create();

    @Rule
    public transient final TestPipeline pipeline = TestPipeline.create();

    @Test
    public void input() {
        final PCollection<JsonObject> out = pipeline.apply(TalendIO.read(new TheTestMapper() {

            @Override
            public Input create() {
                return new BaseTestInput() {

                    private transient Iterator<String> chain;

                    @Override
                    public Object next() {
                        if (chain == null) {
                            chain = asList("a", "b").iterator();
                        }
                        return chain.hasNext() ? new Sample(chain.next()) : null;
                    }
                };
            }
        }));
        PAssert.that(out.apply(UUID.randomUUID().toString(), ParDo.of(new DoFn<JsonObject, String>() {

            @ProcessElement
            public void toData(final ProcessContext sample) {
                sample.output(JSONB.fromJson(sample.element().toString(), Sample.class).data);
            }
        }))).containsInAnyOrder("a", "b");
        assertEquals(PipelineResult.State.DONE, pipeline.run().getState());
    }

    @Test
    public void output() {
        Output.DATA.clear();
        pipeline
                .apply(Create.of(new Sample("a"), new Sample("b")).withCoder(JsonbCoder.of(Sample.class, jsonb())))
                .apply(UUID.randomUUID().toString(), ParDo.of(new DoFn<Sample, JsonObject>() {

                    @ProcessElement
                    public void toData(final ProcessContext sample) {
                        sample.output(JSONB.fromJson(JSONB.toJson(sample.element()), JsonObject.class));
                    }
                }))
                .setCoder(JsonpJsonObjectCoder.of(PLUGIN))
                .apply(new ViewsMappingTransform(emptyMap(), PLUGIN))
                .apply(TalendIO.write(new BaseTestProcessor() {

                    @Override
                    public void onNext(final InputFactory input, final OutputFactory factory) {
                        Output.DATA
                                .add(JSONB.fromJson(input.read(Branches.DEFAULT_BRANCH).toString(), Sample.class).data);
                    }
                }));
        assertEquals(PipelineResult.State.DONE, pipeline.run().getState());
        assertThat(Output.DATA, containsInAnyOrder("a", "b"));
    }

    @Test
    public void processor() {
        final PCollection<SampleLength> out = pipeline
                .apply(Create.of(new Sample("a"), new Sample("bb")).withCoder(JsonbCoder.of(Sample.class, jsonb())))
                .apply(UUID.randomUUID().toString(), ParDo.of(new DoFn<Sample, JsonObject>() {

                    @ProcessElement
                    public void toData(final ProcessContext sample) {
                        sample.output(JSONB.fromJson(JSONB.toJson(sample.element()), JsonObject.class));
                    }
                }))
                .setCoder(JsonpJsonObjectCoder.of(PLUGIN))
                .apply(new ViewsMappingTransform(emptyMap(), PLUGIN))
                .apply(TalendFn.asFn(new BaseTestProcessor() {

                    @Override
                    public void onNext(final InputFactory input, final OutputFactory factory) {
                        factory.create(Branches.DEFAULT_BRANCH).emit(new SampleLength(
                                JSONB.fromJson(input.read(Branches.DEFAULT_BRANCH).toString(), Sample.class).data
                                        .length()));
                    }
                }))
                .setCoder(JsonpJsonObjectCoder.of(PLUGIN))
                .apply(ParDo.of(new DoFn<JsonObject, SampleLength>() {

                    @ProcessElement
                    public void onElement(final ProcessContext ctx) {
                        ctx.output(JSONB.fromJson(ctx.element().getJsonArray("__default__").getJsonObject(0).toString(),
                                SampleLength.class));
                    }
                }));
        PAssert.that(out.apply(UUID.randomUUID().toString(), ParDo.of(new DoFn<SampleLength, Integer>() {

            @ProcessElement
            public void toInt(final ProcessContext pc) {
                pc.output(pc.element().len);
            }
        }))).containsInAnyOrder(1, 2);
        assertEquals(PipelineResult.State.DONE, pipeline.run().getState());
    }

    @Test
    public void processorMulti() {
        final PCollection<SampleLength> out = pipeline
                .apply(Create.of(new Sample("a"), new Sample("bb")).withCoder(JsonbCoder.of(Sample.class, jsonb())))
                .apply(UUID.randomUUID().toString(), ParDo.of(new DoFn<Sample, JsonObject>() {

                    @ProcessElement
                    public void toData(final ProcessContext sample) {
                        sample.output(JSONB.fromJson(JSONB.toJson(sample.element()), JsonObject.class));
                    }
                }))
                .setCoder(JsonpJsonObjectCoder.of(PLUGIN))
                .apply(new ViewsMappingTransform(emptyMap(), PLUGIN))
                .apply(TalendFn.asFn(new BaseTestProcessor() {

                    @Override
                    public void onNext(final InputFactory input, final OutputFactory factory) {
                        factory.create(Branches.DEFAULT_BRANCH).emit(new SampleLength(
                                JSONB.fromJson(input.read(Branches.DEFAULT_BRANCH).toString(), Sample.class).data
                                        .length()));
                    }
                }))
                .apply(ParDo.of(new DoFn<JsonObject, SampleLength>() {

                    @ProcessElement
                    public void onElement(final ProcessContext ctx) {
                        ctx.output(JSONB.fromJson(ctx.element().getJsonArray("__default__").getJsonObject(0).toString(),
                                SampleLength.class));
                    }
                }));
        PAssert.that(out.apply(UUID.randomUUID().toString(), ParDo.of(new DoFn<SampleLength, Integer>() {

            @ProcessElement
            public void toInt(final ProcessContext pc) {
                pc.output(pc.element().len);
            }
        }))).containsInAnyOrder(1, 2);
        assertEquals(PipelineResult.State.DONE, pipeline.run().getState());
    }

    private static final class Output {

        private static final Collection<String> DATA = new CopyOnWriteArrayList<>();
    }

    private static Jsonb jsonb() { // ensure it is serializable
        return Jsonb.class.cast(Proxy.newProxyInstance(TalendIOTest.class.getClassLoader(),
                new Class<?>[] { Jsonb.class, Serializable.class }, new JsonbInvocationHandler()));
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Sample {

        private String data;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SampleLength implements Serializable {

        private int len;
    }

    private static abstract class BaseTestProcessor implements Serializable, Processor {

        @Override
        public void beforeGroup() {
            // no-op
        }

        @Override
        public void afterGroup(final OutputFactory output) {
            // no-op
        }

        @Override
        public String plugin() {
            return PLUGIN;
        }

        @Override
        public String rootName() {
            return "test-classes";
        }

        @Override
        public String name() {
            return "test-classes";
        }

        @Override
        public void start() {
            // no-op
        }

        @Override
        public void stop() {
            // no-op
        }
    }

    private static abstract class BaseTestInput implements Serializable, Input {

        @Override
        public String plugin() {
            return PLUGIN;
        }

        @Override
        public String rootName() {
            return "test-classes";
        }

        @Override
        public String name() {
            return "test-classes";
        }

        @Override
        public void start() {
            // no-op
        }

        @Override
        public void stop() {
            // no-op
        }
    }

    private static abstract class TheTestMapper implements Serializable, Mapper {

        @Override
        public boolean isStream() {
            return false;
        }

        @Override
        public long assess() {
            return 1;
        }

        @Override
        public List<Mapper> split(final long desiredSize) {
            return new ArrayList<>(singletonList(this));
        }

        @Override
        public String plugin() {
            return PLUGIN;
        }

        @Override
        public String rootName() {
            return "test-classes";
        }

        @Override
        public String name() {
            return "test-classes";
        }

        @Override
        public void start() {
            // no-op
        }

        @Override
        public void stop() {
            // no-op
        }
    }

    private static class JSONBReplacement implements Serializable {

        Object readResolve() throws ObjectStreamException {
            return new JsonbInvocationHandler();
        }
    }

    private static class JsonbInvocationHandler implements InvocationHandler, Serializable {

        @Override
        public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
            try {
                return method.invoke(JSONB, args);
            } catch (final InvocationTargetException ite) {
                throw ite.getTargetException();
            }
        }

        Object writeReplace() throws ObjectStreamException {
            return new JSONBReplacement();
        }
    }
}
