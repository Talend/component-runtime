/**
 * Copyright (C) 2006-2025 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.runtime.base;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.junit.jupiter.api.Test;

class LifecycleImplTest {

    @Test
    void name() {
        final Lifecycle impl = new LifecycleImpl(new NoLifecycle(), "Root", "Test", "Plugin");
        assertEquals("Test", impl.name());
    }

    @Test
    void ignoreIfNotUsed() {
        final Lifecycle impl = new LifecycleImpl(new NoLifecycle(), "Root", "Test", "Plugin");
        impl.start();
        impl.stop();
        // no assert but ensures there is no exception when hooks are not here at all
    }

    @Test
    void start() {
        final StartOnly delegate = new StartOnly();
        final Lifecycle impl = new LifecycleImpl(delegate, "Root", "Test", "Plugin");
        assertEquals(0, delegate.counter);
        impl.start();
        assertEquals(1, delegate.counter);
        impl.stop();
        assertEquals(1, delegate.counter);
    }

    @Test
    void stop() {
        final StopOnly delegate = new StopOnly();
        final Lifecycle impl = new LifecycleImpl(delegate, "Root", "Test", "Plugin");
        assertEquals(0, delegate.counter);
        impl.start();
        assertEquals(0, delegate.counter);
        impl.stop();
        assertEquals(1, delegate.counter);
    }

    @Test
    void both() {
        final StartStop delegate = new StartStop();
        final Lifecycle impl = new LifecycleImpl(delegate, "Root", "Test", "Plugin");
        assertEquals(0, delegate.counter);
        impl.start();
        assertEquals(1, delegate.counter);
        impl.stop();
        assertEquals(2, delegate.counter);
    }

    @Test
    void startWithArgumentError() {
        final StartOnlyWithArguments delegate = new StartOnlyWithArguments();
        final Lifecycle impl = new LifecycleImpl(delegate, "Root", "Test", "Plugin");
        assertThrows(IllegalArgumentException.class, impl::start);
    }

    @Test
    void startWithArgument() {
        final String argument = "foo";
        final StartOnlyWithArguments delegate = new StartOnlyWithArguments();
        final Lifecycle impl = new LifecycleImpl(delegate, "Root", "Test", "Plugin") {

            @Override
            protected Object[] evaluateParameters(final Class<? extends Annotation> marker, final Method method) {
                return new Object[] { argument };
            }
        };
        assertEquals(0, delegate.counter);
        assertNull(delegate.argAsField);
        impl.start();
        assertEquals(1, delegate.counter);
        assertEquals(argument, delegate.argAsField);
        impl.stop();
        assertEquals(1, delegate.counter);
        assertEquals(argument, delegate.argAsField);
    }

    public static class NoLifecycle implements Serializable {
    }

    public static class StartOnly implements Serializable {

        private int counter;

        @PostConstruct
        public void start() {
            counter++;
        }
    }

    public static class StopOnly implements Serializable {

        private int counter;

        @PreDestroy
        public void stop() {
            counter++;
        }
    }

    public static class StartStop implements Serializable {

        private int counter;

        @PostConstruct
        public void start() {
            counter++;
        }

        @PreDestroy
        public void stop() {
            counter++;
        }
    }

    public static class StartOnlyWithArguments implements Serializable {

        private int counter;

        private String argAsField;

        @PostConstruct
        public void start(String arg0) {
            argAsField = arg0;
            counter++;
        }
    }

}
