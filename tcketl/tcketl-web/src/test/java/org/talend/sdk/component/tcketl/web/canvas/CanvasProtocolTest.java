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
package org.talend.sdk.component.tcketl.web.canvas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.tcketl.app.async.AsyncRunner;
import org.talend.sdk.component.tcketl.app.canvas.CanvasViewModel;
import org.talend.sdk.component.tcketl.app.session.ServerSession;
import org.talend.sdk.component.tcketl.domain.connector.ConnectorId;
import org.talend.sdk.component.tcketl.domain.connector.ConnectorRef;
import org.talend.sdk.component.tcketl.domain.pipeline.NodeId;
import org.talend.sdk.component.tcketl.domain.pipeline.PipelineEditor;
import org.talend.sdk.component.tcketl.domain.pipeline.Position;

class CanvasProtocolTest {

    private final PipelineEditor editor = new PipelineEditor();

    private final CanvasViewModel canvas = new CanvasViewModel(editor, new ServerSession(AsyncRunner.direct()));

    private final CanvasProtocol protocol = new CanvasProtocol(canvas, id -> null);

    private final NodeId node = editor
            .addNode(new ConnectorRef(new ConnectorId("c1"), "files", "Reader", "plugin", 1), "Reader",
                    new Position(40, 40));

    private JsonNode graph() throws Exception {
        return new ObjectMapper().readTree(protocol.graph());
    }

    @Test
    void aNodeWithoutDefinitionIsDrawnAsPending() throws Exception {
        final JsonNode nodes = graph().get("nodes");

        assertEquals(1, nodes.size());
        assertEquals("pending", nodes.get(0).get("state").asText());
        assertEquals("Reader", nodes.get(0).get("label").asText());
        assertTrue(graph().get("selection").isNull());
    }

    @Test
    void selectionAndMoveAreAppliedAndSnapped() throws Exception {
        assertTrue(protocol.handle("{\"type\":\"select\",\"kind\":\"node\",\"id\":\"" + node.value() + "\"}"));
        assertEquals("node", graph().get("selection").get("kind").asText());

        assertTrue(protocol.handle("{\"type\":\"move\",\"id\":\"" + node.value() + "\",\"x\":133,\"y\":149}"));
        assertEquals(new Position(140, 140), editor.node(node).orElseThrow().position());
    }

    @Test
    void malformedOrUnknownEventsAreIgnored() {
        assertFalse(protocol.handle("not json"));
        assertFalse(protocol.handle("[]"));
        assertFalse(protocol.handle("{\"type\":\"explode\"}"));
        assertFalse(protocol.handle("{\"type\":\"move\",\"id\":\"" + node.value() + "\",\"x\":\"a\",\"y\":1}"));
        assertFalse(protocol.handle("{\"type\":\"drop\",\"x\":1,\"y\":1}"));
        assertEquals(new Position(40, 40), editor.node(node).orElseThrow().position());
    }
}
