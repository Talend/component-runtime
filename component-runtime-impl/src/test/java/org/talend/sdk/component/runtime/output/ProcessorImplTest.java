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
package org.talend.sdk.component.runtime.output;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.IntStream;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.processor.AfterGroup;
import org.talend.sdk.component.api.processor.BeforeGroup;
import org.talend.sdk.component.api.processor.ElementListener;
import org.talend.sdk.component.api.processor.LastGroup;
import org.talend.sdk.component.api.processor.Output;
import org.talend.sdk.component.api.processor.OutputEmitter;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.runtime.record.RecordImpl;
import org.talend.sdk.component.runtime.serialization.Serializer;

import lombok.AllArgsConstructor;
import lombok.Data;

class ProcessorImplTest {

    private static final OutputFactory NO_OUTPUT = name -> value -> {
        // no-op
    };

    @Test
    void bulkGroup() {
        Bufferized.RECORDS = null;
        final Processor processor = new ProcessorImpl("Root", "Test", "Plugin", emptyMap(), new Bufferized());
        processor.start();
        for (int i = 0; i < 3; i++) {
            final Collection<Record> data = IntStream
                    .rangeClosed(1, 3)
                    .mapToObj(idx -> new RecordImpl.BuilderImpl().withInt("value", idx).build())
                    .collect(toList());
            processor.beforeGroup();
            data.forEach(it -> processor.onNext(n -> it, null));
            assertNull(Bufferized.RECORDS);
            processor.afterGroup(null);
            assertEquals(data, Bufferized.RECORDS);
            Bufferized.RECORDS = null;
        }
        processor.stop();
    }

    @Test
    void bulkGroupWithLastGroup() {
        Bufferized.RECORDS = null;
        final Processor processor = new ProcessorImpl("Root", "Test", "Plugin", emptyMap(), new SampleLastGroupOutput());
        processor.start();
        for (int i = 0; i < 3; i++) {
            final Collection<Record> data = IntStream
                    .rangeClosed(1, 3)
                    .mapToObj(idx -> new RecordImpl.BuilderImpl().withInt("value", idx).build())
                    .collect(toList());
            processor.beforeGroup();
            assertTrue(processor.isLastGroupUsed());
            assertFalse(SampleLastGroupOutput.isCalled);
        }
        processor.stop();
    }

    @Test
    void lifecycle() {
        assertLifecycle(new SampleProcessor());
    }

    @Test
    void lifecycleVoid() {
        assertLifecycle(new SampleOutput());
    }

    @Test
    void serialization() throws IOException, ClassNotFoundException {
        final Processor processor = new ProcessorImpl("Root", "Test", "Plugin", emptyMap(), new SampleOutput());
        final Processor copy = Serializer.roundTrip(processor);
        assertNotSame(copy, processor);
        assertEquals("Root", copy.rootName());
        assertEquals("Test", copy.name());
        assertEquals("Plugin", copy.plugin());
    }

    private void assertLifecycle(final Base delegate) {
        final Processor processor = new ProcessorImpl("Root", "Test", "Plugin", emptyMap(), delegate);
        assertEquals(emptyList(), delegate.stack);

        processor.start();
        assertEquals(singletonList("start"), delegate.stack);

        processor.beforeGroup();
        assertEquals(asList("start", "beforeGroup"), delegate.stack);

        processor.afterGroup(null);
        assertEquals(asList("start", "beforeGroup", "afterGroup"), delegate.stack);

        processor.beforeGroup();
        assertEquals(asList("start", "beforeGroup", "afterGroup", "beforeGroup"), delegate.stack);

        processor.onNext(name -> {
            assertEquals(Branches.DEFAULT_BRANCH, name);
            return new Sample(1);
        }, NO_OUTPUT);
        assertEquals(asList("start", "beforeGroup", "afterGroup", "beforeGroup", "next{1}"), delegate.stack);

        processor.onNext(name -> {
            assertEquals(Branches.DEFAULT_BRANCH, name);
            return new Sample(2);
        }, NO_OUTPUT);
        assertEquals(asList("start", "beforeGroup", "afterGroup", "beforeGroup", "next{1}", "next{2}"), delegate.stack);

        processor.afterGroup(null);
        assertEquals(asList("start", "beforeGroup", "afterGroup", "beforeGroup", "next{1}", "next{2}", "afterGroup"),
                delegate.stack);

        processor.stop();
        assertEquals(
                asList("start", "beforeGroup", "afterGroup", "beforeGroup", "next{1}", "next{2}", "afterGroup", "stop"),
                delegate.stack);
    }

    public static class Base implements Serializable {

        final Collection<String> stack = new ArrayList<>();

        @PostConstruct
        public void init() {
            stack.add("start");
        }

        @BeforeGroup
        public void beforeGroup() {
            stack.add("beforeGroup");
        }

        @AfterGroup
        public void afterGroup() {
            stack.add("afterGroup");
        }

        @PreDestroy
        public void destroy() {
            stack.add("stop");
        }
    }

    public static class SampleProcessor extends Base {

        @ElementListener
        public Sample onNext(final Sample sample) {
            stack.add("next{" + sample.data + "}");
            return sample;
        }
    }

    public static class Bufferized implements Serializable {

        private static Collection<Record> RECORDS;

        @AfterGroup
        public void onCommit(final Collection<Record> records) {
            RECORDS = records;
        }
    }

    public static class SampleOutput extends Base {

        @ElementListener
        public void onNext(final Sample sample) {
            stack.add("next{" + sample.data + "}");
        }
    }

    @Data
    @AllArgsConstructor
    public static class Sample {

        private int data;
    }

    public static class SampleLastGroupOutput  implements Serializable {
        private static boolean isCalled = false;

        @ElementListener
        public void onNext(final Sample sample) {

        }

        @AfterGroup
        public void afterGroup(@Output("REJECT") final OutputEmitter<Record> records, @LastGroup final boolean isLast) {
            isCalled = isLast;
        }
    }

}
