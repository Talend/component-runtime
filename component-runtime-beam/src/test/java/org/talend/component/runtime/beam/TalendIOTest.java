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

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.beam.sdk.PipelineResult;
import org.apache.beam.sdk.testing.PAssert;
import org.apache.beam.sdk.testing.TestPipeline;
import org.apache.beam.sdk.transforms.Create;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.PCollection;
import org.junit.Rule;
import org.junit.Test;
import org.talend.component.runtime.input.Input;
import org.talend.component.runtime.input.Mapper;
import org.talend.component.runtime.output.Branches;
import org.talend.component.runtime.output.InputFactory;
import org.talend.component.runtime.output.OutputFactory;
import org.talend.component.runtime.output.Processor;

import lombok.RequiredArgsConstructor;

public class TalendIOTest implements Serializable {

    @Rule
    public transient final TestPipeline pipeline = TestPipeline.create();

    @Test
    public void input() {
        final PCollection<Sample> out = pipeline.apply(TalendIO.read(new TheTestMapper() {

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
        PAssert.that(out.apply(ParDo.of(new DoFn<Sample, String>() {

            @ProcessElement
            public void toData(final ProcessContext sample) {
                sample.output(sample.element().data);
            }
        }))).containsInAnyOrder("a", "b");
        assertEquals(PipelineResult.State.DONE, pipeline.run().getState());
    }

    @Test
    public void output() {
        Output.DATA.clear();
        pipeline.apply(Create.of(new Sample("a"), new Sample("b"))).apply(TalendIO.write(new BaseTestProcessor() {

            @Override
            public void onNext(final InputFactory input, final OutputFactory factory) {
                Output.DATA.add(Sample.class.cast(input.read(Branches.DEFAULT_BRANCH)).data);
            }
        }, emptyMap()));
        assertEquals(PipelineResult.State.DONE, pipeline.run().getState());
        assertThat(Output.DATA, containsInAnyOrder("a", "b"));
    }

    @Test
    public void processor() {
        final PCollection<SampleLength> out = pipeline.apply(Create.of(new Sample("a"), new Sample("bb")))
                .apply(TalendFn.asFn(new BaseTestProcessor() {

                    @Override
                    public void onNext(final InputFactory input, final OutputFactory factory) {
                        factory.create(Branches.DEFAULT_BRANCH)
                                .emit(new SampleLength(Sample.class.cast(input.read(Branches.DEFAULT_BRANCH)).data.length()));
                    }
                }, emptyMap()));
        PAssert.that(out.apply(ParDo.of(new DoFn<SampleLength, Integer>() {

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

    @RequiredArgsConstructor
    public static class Sample implements Serializable {

        private final String data;
    }

    @RequiredArgsConstructor
    public static class SampleLength implements Serializable {

        private final int len;
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
            return "test";
        }

        @Override
        public String rootName() {
            return "test";
        }

        @Override
        public String name() {
            return "test";
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
            return "test";
        }

        @Override
        public String rootName() {
            return "test";
        }

        @Override
        public String name() {
            return "test";
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
            return "test";
        }

        @Override
        public String rootName() {
            return "test";
        }

        @Override
        public String name() {
            return "test";
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
}
