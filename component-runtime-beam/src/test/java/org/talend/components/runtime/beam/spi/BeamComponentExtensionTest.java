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
package org.talend.components.runtime.beam.spi;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.apache.ziplock.JarLocation.jarLocation;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Collection;

import org.apache.beam.sdk.transforms.Create;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.values.PBegin;
import org.apache.beam.sdk.values.PCollection;
import org.junit.Test;
import org.talend.component.api.input.PartitionMapper;
import org.talend.components.runtime.base.Serializer;
import org.talend.components.runtime.beam.data.Sample;
import org.talend.components.runtime.input.Input;
import org.talend.components.runtime.input.Mapper;
import org.talend.components.runtime.manager.extension.ComponentContextImpl;
import org.talend.components.runtime.output.Processor;
import org.talend.components.runtime.serialization.EnhancedObjectInputStream;
import org.talend.components.spi.component.ComponentExtension;

public class BeamComponentExtensionTest {

    private final BeamComponentExtension extension = new BeamComponentExtension();

    @Test
    public void supports() {
        assertTrue(extension.supports(Mapper.class));
        assertTrue(extension.supports(Processor.class));
    }

    @Test
    public void toMapper() throws IOException, ClassNotFoundException {
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
            public String plugin() { // since we don't deploy the test-classes folder correctly we use the absolute path as id
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

    private void assertMapper(Mapper mapper) {
        assertNotNull(mapper);
        mapper.start();
        final Input input = mapper.create();
        input.start();
        assertEquals(new Sample("a"), input.next());
        assertEquals(new Sample("b"), input.next());
        assertNull(input.next());
        input.stop();
        mapper.stop();
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
}
