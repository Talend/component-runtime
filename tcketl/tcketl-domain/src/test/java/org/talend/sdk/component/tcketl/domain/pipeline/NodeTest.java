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
package org.talend.sdk.component.tcketl.domain.pipeline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.tcketl.domain.connector.TestConnectors;

class NodeTest {

    private static Node node(final Map<String, String> configuration) {
        return new Node(new NodeId("n-1"), "Label", TestConnectors.ref("X"), new Position(1, 2), configuration);
    }

    @Test
    void configurationIsSortedCopiedAndUnmodifiable() {
        final Map<String, String> input = new HashMap<>();
        input.put("b", "2");
        input.put("a", "1");
        input.put("c", "3");

        final Node node = node(input);
        input.put("d", "4");

        assertEquals(List.of("a", "b", "c"), List.copyOf(node.configuration().keySet()));
        assertFalse(node.configuration().containsKey("d"));
        assertThrows(UnsupportedOperationException.class, () -> node.configuration().put("e", "5"));
    }

    @Test
    void nullKeysAndValuesAreRefused() {
        final Map<String, String> nullValue = new HashMap<>();
        nullValue.put("a", null);
        final Map<String, String> nullKey = new HashMap<>();
        nullKey.put(null, "x");

        assertThrows(NullPointerException.class, () -> node(nullValue));
        assertThrows(NullPointerException.class, () -> node(nullKey));
    }

    @Test
    void labelMustNotBeBlank() {
        assertThrows(IllegalArgumentException.class,
                () -> new Node(new NodeId("n-1"), " ", TestConnectors.ref("X"), new Position(0, 0), Map.of()));
    }

    @Test
    void withersReturnModifiedCopies() {
        final Node node = node(Map.of());

        assertEquals("New", node.withLabel("New").label());
        assertEquals(new Position(5, 6), node.withPosition(new Position(5, 6)).position());
        assertEquals(Map.of("k", "v"), node.withConfiguration(Map.of("k", "v")).configuration());
        assertEquals("Label", node.label());
        assertEquals(new Position(1, 2), node.position());
    }

    @Test
    void positionMustBeFinite() {
        assertThrows(IllegalArgumentException.class, () -> new Position(Double.NaN, 0));
        assertThrows(IllegalArgumentException.class, () -> new Position(0, Double.POSITIVE_INFINITY));
    }
}
