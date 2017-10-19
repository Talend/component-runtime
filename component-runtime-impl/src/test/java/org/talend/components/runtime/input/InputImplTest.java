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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.Serializable;
import java.util.stream.IntStream;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.junit.Test;
import org.talend.component.api.input.Producer;
import org.talend.components.runtime.serialization.Serializer;

import lombok.AllArgsConstructor;
import lombok.Data;

public class InputImplTest {

    @Test
    public void lifecycle() {
        final Component delegate = new Component();
        final Input input = new InputImpl("Root", "Test", "Plugin", delegate);
        assertFalse(delegate.start);
        assertFalse(delegate.stop);
        assertEquals(0, delegate.count);

        input.start();
        assertTrue(delegate.start);
        assertFalse(delegate.stop);
        assertEquals(0, delegate.count);

        IntStream.range(0, 10).forEach(i -> {
            assertEquals(i, Sample.class.cast(input.next()).getData());
            assertTrue(delegate.start);
            assertFalse(delegate.stop);
            assertEquals(i + 1, delegate.count);
        });

        input.stop();
        assertTrue(delegate.start);
        assertTrue(delegate.stop);
        assertEquals(10, delegate.count);
    }

    @Test
    public void serialization() throws IOException, ClassNotFoundException {
        final Component delegate = new Component();
        final Input input = new InputImpl("Root", "Test", "Plugin", delegate);
        final Input copy = Serializer.roundTrip(input);
        assertNotSame(copy, input);
        assertEquals("Root", copy.rootName());
        assertEquals("Test", copy.name());
        assertEquals("Plugin", copy.plugin());
    }

    public static class Component implements Serializable {

        private boolean stop;

        private boolean start;

        private int count;

        @PostConstruct
        public void init() {
            start = true;
        }

        @Producer
        public Sample produces() {
            return new Sample(count++);
        }

        @PreDestroy
        public void destroy() {
            stop = true;
        }
    }

    @Data
    @AllArgsConstructor
    public static class Sample {

        private int data;
    }
}
