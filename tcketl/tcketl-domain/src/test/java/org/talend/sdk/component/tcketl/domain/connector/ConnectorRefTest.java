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
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ConnectorRefTest {

    private static final ConnectorId ID = new ConnectorId("id");

    @Test
    void familyAndNameAreMandatory() {
        assertThrows(IllegalArgumentException.class, () -> new ConnectorRef(ID, " ", "name", "plugin", 1));
        assertThrows(IllegalArgumentException.class, () -> new ConnectorRef(ID, "family", "", "plugin", 1));
        assertThrows(IllegalArgumentException.class, () -> new ConnectorId(" "));
    }

    @Test
    void versionCannotBeNegative() {
        assertThrows(IllegalArgumentException.class, () -> new ConnectorRef(ID, "family", "name", "plugin", -1));
    }

    @Test
    void aMissingPluginIsEmpty() {
        assertEquals("", new ConnectorRef(ID, "family", "name", null, 0).plugin());
    }
}
