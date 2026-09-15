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
package org.talend.sdk.component.server.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FatalStateTest {

    private FatalState fatalState;

    @BeforeEach
    void setUp() {
        fatalState = new FatalState();
    }

    @Test
    void initiallyNoFatalError() {
        assertFalse(fatalState.hasFatalError());
        assertNull(fatalState.getCause());
    }

    @Test
    void markFatalSetsCause() {
        fatalState.markFatal("OutOfMemoryError during /api/v1/component/index");

        assertTrue(fatalState.hasFatalError());
        assertEquals("OutOfMemoryError during /api/v1/component/index", fatalState.getCause());
    }

    @Test
    void markFatalIsIdempotentFirstCauseWins() {
        fatalState.markFatal("first error");
        fatalState.markFatal("second error");

        assertEquals("first error", fatalState.getCause());
    }
}
