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
package org.talend.components.runtime.output;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.junit.Test;
import org.talend.component.api.processor.AfterGroup;
import org.talend.component.api.processor.BeforeGroup;
import org.talend.component.api.processor.ElementListener;
import org.talend.components.runtime.serialization.Serializer;

import lombok.AllArgsConstructor;
import lombok.Data;

public class ProcessorImplTest {

    private static final OutputFactory NO_OUTPUT = name -> value -> {
        // no-op
    };

    @Test
    public void lifecycle() {
        assertLifecycle(new SampleProcessor());
    }

    @Test
    public void lifecycleVoid() {
        assertLifecycle(new SampleOutput());
    }

    @Test
    public void serialization() throws IOException, ClassNotFoundException {
        final Processor processor = new ProcessorImpl("Root", "Test", "Plugin", new SampleOutput());
        final Processor copy = Serializer.roundTrip(processor);
        assertNotSame(copy, processor);
        assertEquals("Root", copy.rootName());
        assertEquals("Test", copy.name());
        assertEquals("Plugin", copy.plugin());
    }

    private void assertLifecycle(final Base delegate) {
        final Processor processor = new ProcessorImpl("Root", "Test", "Plugin", delegate);
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
        assertEquals(asList("start", "beforeGroup", "afterGroup", "beforeGroup", "next{1}", "next{2}", "afterGroup", "stop"),
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
}
