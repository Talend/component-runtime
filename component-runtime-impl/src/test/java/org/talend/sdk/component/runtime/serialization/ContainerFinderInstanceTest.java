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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ContainerFinderInstanceTest {

    private AtomicReference<ContainerFinder> finder;

    private Field lookupDone;

    private ContainerFinder initialFinder;

    private boolean initialLookupDone;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void save() throws Exception {
        final Field finderField = ContainerFinder.Instance.class.getDeclaredField("FINDER");
        finderField.setAccessible(true);
        finder = (AtomicReference<ContainerFinder>) finderField.get(null);
        lookupDone = ContainerFinder.Instance.class.getDeclaredField("lookupDone");
        lookupDone.setAccessible(true);
        initialFinder = finder.get();
        initialLookupDone = lookupDone.getBoolean(null);
    }

    @AfterEach
    void restore() throws Exception {
        finder.set(initialFinder);
        lookupDone.setBoolean(null, initialLookupDone);
    }

    @Test
    void lookupDoneAfterFirstCall() throws Exception {
        finder.set(null);
        lookupDone.setBoolean(null, false);

        assertNotNull(ContainerFinder.Instance.get());
        assertTrue(lookupDone.getBoolean(null));
        assertTrue(ContainerFinder.Instance.isInitialized()); // test classpath provides a ContainerFinder service
    }

    @Test
    void noServiceLoaderLookupOnceAlreadyDone() throws Exception {
        // lookup was done and no service was found
        finder.set(null);
        lookupDone.setBoolean(null, true);

        // a service is available on the classpath, but must not be looked up again
        assertInstanceOf(TCCLContainerFinder.class, ContainerFinder.Instance.get());
        assertFalse(ContainerFinder.Instance.isInitialized());
    }
}
