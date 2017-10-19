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
package org.talend.components.runtime.base;

import static org.junit.Assert.assertEquals;

import java.io.Serializable;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.junit.Test;

public class LifecycleImplTest {

    @Test
    public void name() {
        final Lifecycle impl = new LifecycleImpl(new NoLifecycle(), "Root", "Test", "Plugin");
        assertEquals("Test", impl.name());
    }

    @Test
    public void ignoreIfNotUsed() {
        final Lifecycle impl = new LifecycleImpl(new NoLifecycle(), "Root", "Test", "Plugin");
        impl.start();
        impl.stop();
        // no assert but ensures there is no exception when hooks are not here at all
    }

    @Test
    public void start() {
        final StartOnly delegate = new StartOnly();
        final Lifecycle impl = new LifecycleImpl(delegate, "Root", "Test", "Plugin");
        assertEquals(0, delegate.counter);
        impl.start();
        assertEquals(1, delegate.counter);
        impl.stop();
        assertEquals(1, delegate.counter);
    }

    @Test
    public void stop() {
        final StopOnly delegate = new StopOnly();
        final Lifecycle impl = new LifecycleImpl(delegate, "Root", "Test", "Plugin");
        assertEquals(0, delegate.counter);
        impl.start();
        assertEquals(0, delegate.counter);
        impl.stop();
        assertEquals(1, delegate.counter);
    }

    @Test
    public void both() {
        final StartStop delegate = new StartStop();
        final Lifecycle impl = new LifecycleImpl(delegate, "Root", "Test", "Plugin");
        assertEquals(0, delegate.counter);
        impl.start();
        assertEquals(1, delegate.counter);
        impl.stop();
        assertEquals(2, delegate.counter);
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
}
