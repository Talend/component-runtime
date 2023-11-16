/**
 * Copyright (C) 2006-2023 Talend Inc. - www.talend.com
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
import static java.util.stream.Collectors.toList;
import static org.apache.ziplock.JarLocation.jarLocation;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.beam.sdk.PipelineResult;
import org.apache.beam.sdk.coders.Coder;
import org.apache.beam.sdk.io.FileBasedSink;
import org.apache.beam.sdk.io.GenerateSequence;
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
import org.hamcrest.MatcherAssert;
import org.joda.time.Instant;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.input.PartitionMapper;
import org.talend.sdk.component.runtime.beam.data.Sample;
import org.talend.sdk.component.runtime.beam.transform.DelegatingTransform;
import org.talend.sdk.component.runtime.manager.ComponentManager;

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
        MatcherAssert.assertThat(source, instanceOf(BeamSource.class));

        final PCollection<Sample> out = pipeline.apply((PTransform<PBegin, PCollection<Sample>>) source);
        PAssert.that(out.apply(ParDo.of(new DoFn<Sample, String>() {

            @ProcessElement
            public void toData(final ProcessContext sample) {
                sample.output(sample.element().getData());
            }
        }))).containsInAnyOrder("a", "b");
        assertEquals(PipelineResult.State.DONE, pipeline.run().getState());
    }

    private Object newComponent(final String name, final ComponentManager.ComponentType type) {
        final Optional<Object> component =
                ComponentManager.instance().createComponent("test", name, type, 1, createConfig(name));
        assertTrue(component.isPresent());
        return component.get();
    }

    private Map<String, String> createConfig(String name) {
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

    @PartitionMapper(family = "test", name = "beamio_unbounded_input")
    public static class BeamUnboundedSource extends DelegatingTransform<PBegin, PCollection<Long>> {

        public BeamUnboundedSource(@Option("from") final long from) {
            super(GenerateSequence.from(from));
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
