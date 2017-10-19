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
package org.talend.components.runtime.manager.processor;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.Serializable;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.talend.component.api.processor.ElementListener;
import org.talend.components.runtime.manager.test.Serializer;
import org.talend.components.runtime.output.Processor;

import lombok.AllArgsConstructor;
import lombok.Data;

public class AdvancedProcessorImplTest {

    @Test
    public void serialization() throws IOException, ClassNotFoundException {
        final Processor processor = new AdvancedProcessorImpl("Root", "Test", "Plugin", new SampleOutput());
        final Processor copy = Serializer.roundTrip(processor);
        assertNotSame(copy, processor);
        assertEquals("Root", copy.rootName());
        assertEquals("Test", copy.name());
        assertEquals("Plugin", copy.plugin());
    }

    @Test
    public void subclassing() {
        final Processor processor = new AdvancedProcessorImpl("Root", "Test", "Plugin", new SampleOutput());
        final AtomicReference<Object> ref = new AtomicReference<>();
        processor.beforeGroup(); // just to enforce the init
        processor.onNext(name -> new Whatever(1), name -> value -> assertTrue(ref.compareAndSet(null, value)));
        final Object out = ref.get();
        assertNotNull(out);
        assertThat(out, instanceOf(String.class));
        assertEquals("1", out.toString());
    }

    public static class SampleOutput implements Serializable {

        @ElementListener
        public String onNext(final Sample sample) {
            return String.valueOf(sample.getData());
        }
    }

    @Data
    @AllArgsConstructor
    public static class Whatever {

        private int data;
    }

    @Data
    public static class Sample {

        private int data;
    }
}
