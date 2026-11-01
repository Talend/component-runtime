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
package org.talend.sdk.component.tcketl.domain.connector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PortTest {

    @Test
    void defaultFlowIsLabelledMain() {
        final Port port = Port.output(Port.DEFAULT_FLOW);

        assertTrue(port.isDefault());
        assertEquals("main", port.label());
        assertEquals("__default__", port.flow());
    }

    @Test
    void otherFlowsKeepTheirServerName() {
        final Port port = Port.input("lookup");

        assertFalse(port.isDefault());
        assertEquals("lookup", port.label());
    }

    @Test
    void rejectFlowIsRecognisedIgnoringTheCase() {
        assertTrue(Port.output("REJECT").isReject());
        assertTrue(Port.output("reject").isReject());
        assertFalse(Port.output("__default__").isReject());
    }

    @Test
    void blankFlowIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> Port.input(" "));
    }
}
