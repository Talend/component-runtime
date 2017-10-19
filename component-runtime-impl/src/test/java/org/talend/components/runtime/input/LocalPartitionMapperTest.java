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
package org.talend.components.runtime.input;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.Serializable;

import org.junit.Test;
import org.talend.component.api.input.Producer;
import org.talend.components.runtime.serialization.Serializer;

public class LocalPartitionMapperTest {

    @Test
    public void split() {
        final Mapper mapper = new LocalPartitionMapper("Root", "Test", "Plugin", null);
        assertEquals(1, mapper.assess());
        assertEquals(singletonList(mapper), mapper.split(1));
        assertEquals(singletonList(mapper), mapper.split(10));
        assertEquals(singletonList(mapper), mapper.split(-159));
    }

    @Test
    public void createReader() {
        final Mapper mapper = new LocalPartitionMapper("Root", "Test", "Plugin", new Component());
        final Input input = mapper.create();
        assertTrue(Sample.class.isInstance(input.next()));
        assertNotSame(input.next(), input.next());
    }

    @Test
    public void serialization() throws IOException, ClassNotFoundException {
        final LocalPartitionMapper mapper = Serializer
                .roundTrip(new LocalPartitionMapper("Root", "Test", "Plugin", new Component()));
        assertEquals("Root", mapper.rootName());
        assertEquals("Test", mapper.name());
        assertEquals("Plugin", mapper.plugin());

        final Input input = mapper.create();
        assertNotNull(input);
    }

    public static class Component implements Serializable {

        @Producer
        public Sample next() {
            return new Sample();
        }
    }

    public static class Sample {
    }
}
