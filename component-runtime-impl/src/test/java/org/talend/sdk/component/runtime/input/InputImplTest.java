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
package org.talend.sdk.component.runtime.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.Serializable;
import java.util.stream.IntStream;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.input.Producer;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.runtime.serialization.Serializer;

import lombok.AllArgsConstructor;
import lombok.Data;

class InputImplTest {

    @Test
    void lifecycle() {
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
            final Object next = input.next();
            assertTrue(Record.class.isInstance(next));
            final Record record = Record.class.cast(next);
            assertEquals(Schema.Type.RECORD, record.getSchema().getType());
            assertEquals(1, record.getSchema().getEntries().size());
            final Schema.Entry data = record.getSchema().getEntries().iterator().next();
            assertEquals("data", data.getName());
            assertEquals(Schema.Type.DOUBLE, data.getType());
            assertEquals(i, record.get(Double.class, "data").doubleValue());
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
    void serialization() throws IOException, ClassNotFoundException {
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
