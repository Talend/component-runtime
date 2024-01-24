/**
 * Copyright (C) 2006-2024 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.runtime.beam.spi;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.apache.ziplock.JarLocation.jarLocation;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;

import org.apache.avro.generic.IndexedRecord;
import org.apache.beam.sdk.transforms.Create;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.PBegin;
import org.apache.beam.sdk.values.PCollection;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.input.PartitionMapper;
import org.talend.sdk.component.api.processor.ElementListener;
import org.talend.sdk.component.api.processor.Output;
import org.talend.sdk.component.design.extension.flows.FlowsFactory;
import org.talend.sdk.component.runtime.base.Delegated;
import org.talend.sdk.component.runtime.base.Serializer;
import org.talend.sdk.component.runtime.beam.data.Sample;
import org.talend.sdk.component.runtime.input.Input;
import org.talend.sdk.component.runtime.input.Mapper;
import org.talend.sdk.component.runtime.manager.ComponentFamilyMeta;
import org.talend.sdk.component.runtime.manager.extension.ComponentContextImpl;
import org.talend.sdk.component.runtime.output.Processor;
import org.talend.sdk.component.runtime.serialization.EnhancedObjectInputStream;
import org.talend.sdk.component.spi.component.ComponentExtension;

class BeamComponentExtensionTest {

    private final BeamComponentExtension extension = new BeamComponentExtension();

    @Test
    void flowFactory() {
        final FlowsFactory factory = extension
                .unwrap(FlowsFactory.class,
                        new ComponentFamilyMeta.ProcessorMeta(
                                new ComponentFamilyMeta("test", emptyList(), null, "test", "test"), "beam", null, 1,
                                BeamMapper.class, Collections::emptyList, null, null, true, Collections.emptyMap()) {
                        });
        assertEquals(1, factory.getInputFlows().size());
        assertEquals(asList("main1", "main2"), factory.getOutputFlows());
    }

    @Test
    void supports() {
        assertTrue(extension.supports(Mapper.class));
        assertTrue(extension.supports(Processor.class));
    }

    @Test
    void toMapper() throws IOException, ClassNotFoundException {
        final ComponentContextImpl context = new ComponentContextImpl(BeamSource.class);
        context.setCurrentExtension(extension);
        extension.onComponent(context);
        context.setCurrentExtension(null);
        assertTrue(context.isNoValidation());
        assertEquals(extension, context.getOwningExtension());

        final Mapper mapper = extension.convert(new ComponentExtension.ComponentInstance() {

            @Override
            public Object instance() {
                return new BeamSource(asList("a", "b"));
            }

            @Override
            public String plugin() { // since we don't deploy the test-classes folder correctly we use the absolute
                                     // path as id
                return jarLocation(BeamComponentExtensionTest.class).getAbsolutePath();
            }

            @Override
            public String family() {
                return "test";
            }

            @Override
            public String name() {
                return "extension";
            }
        }, Mapper.class);
        assertMapper(mapper);

        // ensure the mapper is serializable even if not intended to be used this way
        final byte[] bytes = Serializer.toBytes(mapper);
        try (final ObjectInputStream ois = new EnhancedObjectInputStream(new ByteArrayInputStream(bytes),
                Thread.currentThread().getContextClassLoader())) {
            final Serializable deserialized = Serializable.class.cast(ois.readObject());
            assertMapper(Mapper.class.cast(deserialized));
        }
    }

    private void assertMapper(final Mapper mapper) {
        assertNotNull(mapper);
        assertThat(mapper, instanceOf(Delegated.class));
        assertNotNull(Delegated.class.cast(mapper).getDelegate());
        try {
            mapper.start();
            fail();
        } catch (final IllegalStateException ise) {
            // ok
        }
    }

    @PartitionMapper(family = "test", name = "extension")
    public static class BeamSource extends PTransform<PBegin, PCollection<Sample>> {

        private final Create.Values<Sample> source;

        public BeamSource(final Collection<String> values) {
            this.source = Create.of(values.stream().map(Sample::new).collect(toList()));
        }

        @Override
        public PCollection<Sample> expand(final PBegin input) {
            return source.expand(input);
        }
    }

    public static class BeamMapper extends PTransform<PCollection<IndexedRecord>, PCollection<?>> {

        @Override
        public PCollection<?> expand(final PCollection<IndexedRecord> input) {
            final MyDoFn fn = new MyDoFn();
            return input.apply(ParDo.of(fn));
        }

        @ElementListener
        public void designDefinition(final IndexedRecord input, @Output("main1") final IndexedRecord ignored1,
                @Output("main2") final IndexedRecord ignored2) {
            // not used, here just for the design
        }

        public static class MyDoFn extends DoFn<IndexedRecord, String> {

            @ProcessElement
            public void onElement(@Element final IndexedRecord record, final OutputReceiver<String> output) {
                // no-op
            }
        }
    }
}
