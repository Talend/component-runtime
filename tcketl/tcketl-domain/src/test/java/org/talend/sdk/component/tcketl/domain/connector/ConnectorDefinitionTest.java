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

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.tcketl.domain.property.PropertyDefinition;
import org.talend.sdk.component.tcketl.domain.property.PropertyType;

class ConnectorDefinitionTest {

    @Test
    void portsFollowTheDeclaredFlowOrder() {
        final ConnectorDefinition join = TestConnectors.join("Join");

        assertEquals(List.of(Port.input("__default__"), Port.input("lookup")), join.inputPorts());
        assertEquals(List.of(Port.output("__default__")), join.outputPorts());
        assertEquals(ConnectorRole.PROCESSOR, join.role());
    }

    @Test
    void duplicatedFlowsAreCollapsed() {
        final ConnectorDefinition definition = TestConnectors.definition("Dup", List.of("a", "a", "b"), List.of());

        assertEquals(List.of("a", "b"), definition.inputFlows());
        assertEquals(ConnectorRole.OUTPUT, definition.role());
    }

    @Test
    void flowsAreCopiedAndUnmodifiable() {
        final List<String> flows = new ArrayList<>(List.of("a"));
        final ConnectorDefinition definition = TestConnectors.definition("Copy", flows, List.of());

        flows.add("b");

        assertEquals(List.of("a"), definition.inputFlows());
        assertThrows(UnsupportedOperationException.class, () -> definition.inputFlows().add("c"));
    }

    @Test
    void flowLookupIsExact() {
        final ConnectorDefinition join = TestConnectors.join("Join");

        assertTrue(join.hasInputFlow("lookup"));
        assertFalse(join.hasInputFlow("LOOKUP"));
        assertFalse(join.hasInputFlow(null));
        assertTrue(join.hasOutputFlow("__default__"));
        assertFalse(join.hasOutputFlow("REJECT"));
    }

    @Test
    void missingOptionalFieldsFallBackToDefaults() {
        final ConnectorDefinition definition =
                new ConnectorDefinition(TestConnectors.ref("Tech"), " ", List.of(), List.of(), false, false, null);

        assertEquals("Tech", definition.displayName());
        assertEquals("", definition.documentation());
        assertEquals(ConnectorRole.STANDALONE, definition.role());
    }

    @Test
    void blankFlowNamesAreRefused() {
        assertThrows(IllegalArgumentException.class,
                () -> TestConnectors.definition("Bad", List.of(" "), List.of()));
    }

    @Test
    void propertiesAndActionsAreCopiedAndUnmodifiable() {
        final List<PropertyDefinition> properties =
                new ArrayList<>(List.of(PropertyDefinition.builder("config", PropertyType.OBJECT).build()));
        final ConnectorDefinition definition = new ConnectorDefinition(TestConnectors.ref("Cfg"), "Cfg", List.of(),
                List.of("__default__"), false, false, "", properties, List.of());

        properties.add(PropertyDefinition.builder("other", PropertyType.OBJECT).build());

        assertEquals(1, definition.properties().size());
        assertThrows(UnsupportedOperationException.class, () -> definition.actions().add(null));
        assertTrue(TestConnectors.definition("Plain", List.of(), List.of()).properties().isEmpty());
    }
}
