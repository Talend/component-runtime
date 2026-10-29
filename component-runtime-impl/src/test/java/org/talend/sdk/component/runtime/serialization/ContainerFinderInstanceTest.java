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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
}
