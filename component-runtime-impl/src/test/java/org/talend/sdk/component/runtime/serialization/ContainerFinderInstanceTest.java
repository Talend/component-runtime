/**
 * Copyright (C) 2006-2026 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.runtime.serialization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.Enumeration;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

// ContainerFinder.Instance is a JVM global
@ResourceLock("ContainerFinder.Instance")
class ContainerFinderInstanceTest {

    @BeforeEach
    @AfterEach
    void reset() {
        ContainerFinder.Instance.reset();
    }

    @Test
    void lookupResultIsCached() {
        final ContainerFinder first = ContainerFinder.Instance.get();
        assertNotNull(first);
        assertSame(first, ContainerFinder.Instance.get());
    }

    @Test
    void explicitFinderWinsOverLookup() {
        final ContainerFinder explicit = plugin -> null;
        ContainerFinder.Instance.set(() -> explicit);
        assertTrue(ContainerFinder.Instance.isInitialized());
        assertSame(explicit, ContainerFinder.Instance.get());
    }

    @Test
    void resetForgetsFinderAndLookup() {
        ContainerFinder.Instance.set(() -> plugin -> null);
        ContainerFinder.Instance.reset();
        assertFalse(ContainerFinder.Instance.isInitialized());
    }

    @Test
    void unsuccessfulLookupIsDoneOnceAndFallbackIsReused() throws Exception {
        // a private copy of ContainerFinder (so of its static state) which does not see any service descriptor
        final URL classes = ContainerFinder.class.getProtectionDomain().getCodeSource().getLocation();
        try (final HiddenDescriptorClassLoader loader =
                new HiddenDescriptorClassLoader(classes, ContainerFinder.class.getClassLoader())) {
            final Class<?> instance = loader.loadClass(ContainerFinder.Instance.class.getName());
            assertSame(loader, instance.getClassLoader());
            final Method get = instance.getMethod("get");

            final Object first = get.invoke(null);
            assertEquals(TCCLContainerFinder.class.getName(), first.getClass().getName());
            assertSame(loader, first.getClass().getClassLoader());
            assertSame(first, get.invoke(null));
            assertSame(first, get.invoke(null));

            assertEquals(1, loader.descriptorLookups.get());
        }
    }

    private static class HiddenDescriptorClassLoader extends URLClassLoader {

        private static final String PACKAGE = ContainerFinder.class.getPackage().getName() + '.';

        private static final String DESCRIPTOR = "META-INF/services/" + ContainerFinder.class.getName();

        private final AtomicInteger descriptorLookups = new AtomicInteger();

        private HiddenDescriptorClassLoader(final URL classes, final ClassLoader parent) {
            super(new URL[] { classes }, parent);
        }

        @Override
        protected Class<?> loadClass(final String name, final boolean resolve) throws ClassNotFoundException {
            if (!name.startsWith(PACKAGE)) {
                return super.loadClass(name, resolve);
            }
            synchronized (getClassLoadingLock(name)) {
                Class<?> loaded = findLoadedClass(name);
                if (loaded == null) {
                    try {
                        loaded = findClass(name); // child first
                    } catch (final ClassNotFoundException cnfe) {
                        return super.loadClass(name, resolve);
                    }
                }
                if (resolve) {
                    resolveClass(loaded);
                }
                return loaded;
            }
        }

        @Override
        public Enumeration<URL> getResources(final String name) throws IOException {
            if (DESCRIPTOR.equals(name)) {
                descriptorLookups.incrementAndGet();
                return Collections.emptyEnumeration();
            }
            return super.getResources(name);
        }
    }
}
