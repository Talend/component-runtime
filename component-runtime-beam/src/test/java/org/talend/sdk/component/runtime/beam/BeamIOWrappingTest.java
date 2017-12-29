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

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.apache.ziplock.JarLocation.jarLocation;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import org.apache.beam.sdk.PipelineResult;
import org.apache.beam.sdk.coders.Coder;
import org.apache.beam.sdk.io.FileBasedSink;
import org.apache.beam.sdk.io.TextIO;
import org.apache.beam.sdk.testing.PAssert;
import org.apache.beam.sdk.testing.TestPipeline;
import org.apache.beam.sdk.transforms.Create;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.transforms.windowing.GlobalWindow;
import org.apache.beam.sdk.values.PBegin;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.PDone;
import org.joda.time.Instant;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.talend.sdk.component.runtime.beam.data.Sample;
import org.talend.sdk.component.runtime.beam.impl.BeamMapperImpl;
import org.talend.sdk.component.runtime.beam.impl.BeamProcessorChainImpl;
import org.talend.sdk.component.runtime.beam.transform.DelegatingTransform;
import org.talend.sdk.component.runtime.input.Input;
import org.talend.sdk.component.runtime.input.Mapper;
import org.talend.sdk.component.runtime.manager.ComponentManager;
import org.talend.sdk.component.runtime.output.Branches;
import org.talend.sdk.component.runtime.output.Processor;
import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.input.PartitionMapper;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

public class BeamIOWrappingTest implements Serializable {

    @Rule
    public transient final TestPipeline pipeline = TestPipeline.create();

    @ClassRule
    public static transient final TemporaryFolder TEMPORARY_FOLDER = new TemporaryFolder();

    @Test
    public void nativeBeamPipeline() {
        final Object source = newComponent("beamio_input", ComponentManager.ComponentType.MAPPER);
        assertThat(source, instanceOf(BeamSource.class));

        final PCollection<Sample> out = pipeline.apply((PTransform<PBegin, PCollection<Sample>>) source);
        PAssert.that(out.apply(ParDo.of(new DoFn<Sample, String>() {

            @ProcessElement
            public void toData(final ProcessContext sample) {
                sample.output(sample.element().getData());
            }
        }))).containsInAnyOrder("a", "b");
        assertEquals(PipelineResult.State.DONE, pipeline.run().getState());
    }

    @Test
    public void mapper() {
        final Object source = newComponent("beamio_input", ComponentManager.ComponentType.MAPPER);
        assertThat(source, instanceOf(BeamSource.class));

        final Mapper mapper = new BeamMapperImpl((PTransform<PBegin, ?>) source, getPlugin(), "test", "beamio_input");
        mapper.start();
        assertEquals(2, mapper.assess());

        final Input input = mapper.create();
        assertNotNull(input);
        input.start();
        assertEquals(new Sample("a"), input.next());
        assertEquals(new Sample("b"), input.next());
        assertNull(input.next());
        input.stop();

        mapper.stop();
    }

    @Test
    public void inputChain() {
        MySink.DATA.clear();

        final Object source = newComponent("beamio_input_chain", ComponentManager.ComponentType.MAPPER);
        final Mapper mapper =
                new BeamMapperImpl((PTransform<PBegin, ?>) source, getPlugin(), "test", "beamio_input_chain");
        mapper.start();
        assertEquals(4, mapper.assess());

        final Input input = mapper.create();
        assertNotNull(input);
        input.start();
        assertNotNull(input.next());
        assertNotNull(input.next());
        assertNull(input.next());
        try {
            input.stop();
        } catch (final IllegalArgumentException iae) {
            // for now we ignore this error which is issuing an output in an after bundle
            assertEquals("chunk outputs are not yet supported", iae.getMessage());
        }

        mapper.stop();

        assertEquals(asList("setup", "start-bundle", "1a", "2b", "teardown"), MySink.DATA);
    }

    @Test
    public void outputChain() {
        MySink.DATA.clear();

        final Object source = newComponent("beamio_output_chain", ComponentManager.ComponentType.PROCESSOR);
        final Processor processor = new BeamProcessorChainImpl((PTransform<PCollection<?>, PDone>) source, null,
                getPlugin(), "test", "beamio_output");
        processor.start();
        processor.beforeGroup();

        Stream.of("tsrif", "dnoces").forEach(data -> processor.onNext(name -> {
            assertEquals(Branches.DEFAULT_BRANCH, name);
            return new Sample(data);
        }, name -> value -> MySink.DATA.add(value.toString())));

        processor.afterGroup(name -> {
            assertEquals(Branches.DEFAULT_BRANCH, name);
            return value -> MySink.DATA.add(value.toString());
        });
        processor.stop();

        assertEquals(asList("setup", "start-bundle", "first", "second", "finish-out", "finish-bundle", "teardown"),
                MySink.DATA);
        MySink.DATA.clear();
    }

    @Test
    public void processor() {
        MySink.DATA.clear();

        final Object source = newComponent("beamio_output", ComponentManager.ComponentType.PROCESSOR);
        final Processor processor = new BeamProcessorChainImpl((PTransform<PCollection<?>, ?>) source, null,
                getPlugin(), "test", "beamio_output");
        processor.start();
        processor.beforeGroup();

        Stream.of("tsrif", "dnoces").forEach(data -> processor.onNext(name -> {
            assertEquals(Branches.DEFAULT_BRANCH, name);
            return data;
        }, null));

        processor.afterGroup(name -> {
            assertEquals(Branches.DEFAULT_BRANCH, name);
            return value -> MySink.DATA.add(value.toString());
        });
        processor.stop();

        assertEquals(asList("setup", "start-bundle", "first", "second", "finish-out", "finish-bundle", "teardown"),
                MySink.DATA);
        MySink.DATA.clear();
    }

    @Test
    public void fileOutput() throws IOException {
        final Object source = newComponent("beamio_text", ComponentManager.ComponentType.PROCESSOR);
        final Processor processor = new BeamProcessorChainImpl((PTransform<PCollection<?>, PDone>) source, null,
                getPlugin(), "test", "beamio_text");
        processor.start();
        processor.beforeGroup();

        Stream.of("first", "second").forEach(data -> processor.onNext(name -> {
            assertEquals(Branches.DEFAULT_BRANCH, name);
            return data;
        }, name -> value -> fail(name + " >> " + value)));

        final AtomicReference<Object> output = new AtomicReference<>();
        processor.afterGroup(name -> {
            assertEquals(Branches.DEFAULT_BRANCH, name);
            return output::set;
        });
        processor.stop();
        final FileBasedSink.FileResult result = FileBasedSink.FileResult.class.cast(output.get());
        assertNotNull(result);
        final File file = new File(result.getTempFilename().toString());
        assertTrue(file.exists());
        assertEquals(file.getParentFile().getParentFile(), TEMPORARY_FOLDER.getRoot());
        assertEquals("first\nsecond", Files.lines(file.toPath()).collect(joining("\n")));
    }

    private Object newComponent(final String name, final ComponentManager.ComponentType type) {
        final Optional<Object> component =
                ComponentManager.instance().createComponent("test", name, type, 1, createConfig(name));
        assertTrue(component.isPresent());
        return component.get();
    }

    private HashMap<String, String> createConfig(String name) {
        return new HashMap<String, String>() {

            {
                if (name.endsWith("input")) {
                    put("values[0]", "a");
                    put("values[1]", "b");
                } else if (name.equalsIgnoreCase("beamio_text")) {
                    try {
                        put("output", TEMPORARY_FOLDER.newFile("test").getAbsolutePath());
                    } catch (final IOException e) {
                        throw new IllegalStateException(e);
                    }
                } else {
                    put("reverse", "true");
                }
            }
        };
    }

    private String getPlugin() {
        return jarLocation(BeamIOWrappingTest.class).getAbsolutePath();
    }

    @PartitionMapper(family = "test", name = "beamio_input")
    public static class BeamSource extends DelegatingTransform<PBegin, PCollection<Sample>> {

        public BeamSource(@Option("values") final Collection<String> values) {
            super(Create.of(values.stream().map(Sample::new).collect(toList())).withCoder(new SampleCoder()));
        }

        private static class SampleCoder extends Coder<Sample> {

            @Override
            public void encode(final Sample value, final OutputStream outStream) throws IOException {
                outStream.write(value.getData().getBytes(StandardCharsets.UTF_8));
            }

            @Override
            public Sample decode(final InputStream inStream) throws IOException {
                return new Sample(
                        new BufferedReader(new InputStreamReader(inStream, StandardCharsets.UTF_8)).readLine());
            }

            @Override
            public List<? extends Coder<?>> getCoderArguments() {
                return emptyList();
            }

            @Override
            public void verifyDeterministic() throws NonDeterministicException {
                // no-op
            }
        }
    }

    @PartitionMapper(family = "test", name = "beamio_input_chain")
    public static class BeamInputChain extends PTransform<PBegin, PCollection<String>> {

        @Override
        public PCollection<String> expand(final PBegin input) {
            return new BeamSource(asList("a1", "b2")).expand(input).apply(ParDo.of(new DoFn<Sample, String>() {

                @ProcessElement
                public void onElement(final ProcessContext ctx) {
                    ctx.output(ctx.element().getData());
                }
            })).apply(ParDo.of(new MySink(true)));
        }
    }

    @org.talend.sdk.component.api.processor.Processor(family = "test", name = "beamio_text")
    public static class BeamFileOutput extends DelegatingTransform<PCollection<String>, PDone> {

        public BeamFileOutput(@Option("output") final String output) {
            super(TextIO.write().withSuffix("test").to(FileBasedSink.convertToFileResourceIfPossible(output)));
        }
    }

    @org.talend.sdk.component.api.processor.Processor(family = "test", name = "beamio_output")
    public static class BeamSink extends PTransform<PCollection<String>, PDone> {

        private final boolean reverse;

        public BeamSink(@Option("reverse") final boolean reverse) {
            this.reverse = reverse;
        }

        @Override
        public PDone expand(final PCollection<String> input) {
            input.apply(ParDo.of(new MySink(reverse)));
            return PDone.in(input.getPipeline());
        }
    }

    @org.talend.sdk.component.api.processor.Processor(family = "test", name = "beamio_output_chain")
    public static class BeamChainOutput extends PTransform<PCollection<Sample>, PDone> {

        private final boolean reverse;

        public BeamChainOutput(@Option("reverse") final boolean reverse) {
            this.reverse = reverse;
        }

        @Override
        public PDone expand(final PCollection<Sample> input) {
            new BeamSink(reverse).expand(input.apply(ParDo.of(new DoFn<Sample, String>() {

                @ProcessElement
                public void onElement(final ProcessContext ctx) {
                    ctx.output(ctx.element().getData());
                }
            })));
            return PDone.in(input.getPipeline());
        }
    }

    @RequiredArgsConstructor
    public static class MySink extends DoFn<String, String> {

        static final List<String> DATA = new ArrayList<>();

        @Getter
        private final boolean reverse;

        @Setup
        public void onSetup() {
            DATA.add("setup");
        }

        @StartBundle
        public void onStartBundle(final StartBundleContext startBundleContext) {
            if (startBundleContext != null) {
                DATA.add("start-bundle");
            }
        }

        @ProcessElement
        public void onElement(final ProcessContext context) {
            synchronized (DATA) {
                final String element = context.element();
                DATA.add(reverse ? new StringBuilder(element).reverse().toString() : element);
            }
        }

        @FinishBundle
        public void onFinishBundle(final FinishBundleContext finishBundleContext) {
            if (finishBundleContext != null) {
                finishBundleContext.output("finish-out", Instant.now(), GlobalWindow.INSTANCE);
                DATA.add("finish-bundle");
            }
        }

        @Teardown
        public void onTearDown() {
            DATA.add("teardown");
        }
    }
}
