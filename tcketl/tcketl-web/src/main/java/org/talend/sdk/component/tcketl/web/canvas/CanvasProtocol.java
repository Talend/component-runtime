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

import java.util.Base64;
import java.util.List;
import java.util.function.Function;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.talend.sdk.component.tcketl.app.canvas.CanvasViewModel;
import org.talend.sdk.component.tcketl.app.canvas.NodePorts;
import org.talend.sdk.component.tcketl.app.canvas.Selection;
import org.talend.sdk.component.tcketl.domain.connector.ConnectorDefinition;
import org.talend.sdk.component.tcketl.domain.connector.ConnectorId;
import org.talend.sdk.component.tcketl.domain.connector.ConnectorRole;
import org.talend.sdk.component.tcketl.domain.pipeline.Endpoint;
import org.talend.sdk.component.tcketl.domain.pipeline.Link;
import org.talend.sdk.component.tcketl.domain.pipeline.Node;
import org.talend.sdk.component.tcketl.domain.pipeline.NodeId;
import org.talend.sdk.component.tcketl.domain.pipeline.Pipeline;
import org.talend.sdk.component.tcketl.domain.pipeline.Position;
import org.talend.sdk.component.tcketl.domain.pipeline.Resolution;
import org.talend.sdk.component.tcketl.domain.server.IconData;

/**
 * The server half of the canvas protocol: it renders the pipeline as the graph document the browser draws, and turns
 * the browser's intents into {@link CanvasViewModel} commands. The browser is never trusted: every event is
 * re-validated by the view model and the pipeline editor, and a malformed one is ignored.
 */
public final class CanvasProtocol {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final CanvasViewModel canvas;

    private final Function<ConnectorId, IconData> icons;

    public CanvasProtocol(final CanvasViewModel canvas,
            final Function<ConnectorId, IconData> icons) {
        this.canvas = canvas;
        this.icons = icons;
    }

    public String graph() {
        final Pipeline pipeline = canvas.editor().pipeline();
        final ObjectNode root = JSON.createObjectNode();
        final ArrayNode nodes = root.putArray("nodes");
        for (final Node node : pipeline.nodes()) {
            nodes.add(node(node));
        }
        final ArrayNode links = root.putArray("links");
        for (final Link link : pipeline.links()) {
            links.add(link(link));
        }
        root.set("selection", selection(canvas.selection().get()));
        return root.toString();
    }

    private ObjectNode node(final Node node) {
        final Resolution resolution = canvas.editor().resolution(node.id());
        final ObjectNode json = JSON.createObjectNode();
        json.put("id", node.id().value());
        json.put("label", node.label());
        json.put("x", node.position().x());
        json.put("y", node.position().y());
        json.put("state", resolution instanceof Resolution.Resolved ? "resolved"
                : resolution instanceof Resolution.Unresolved ? "unresolved" : "pending");
        if (resolution instanceof Resolution.Resolved resolved) {
            final ConnectorDefinition definition = resolved.definition();
            json.put("role", ConnectorRole.fromFlows(definition.inputFlows(), definition.outputFlows()).name());
        }
        final IconData icon = icons.apply(node.connector().id());
        if (icon != null) {
            json
                    .put("icon", "data:" + icon.mediaType() + ";base64,"
                            + Base64.getEncoder().encodeToString(icon.content()));
        }
        final NodePorts ports = canvas.ports(node.id());
        ports(json.putArray("inputs"), ports.inputs());
        ports(json.putArray("outputs"), ports.outputs());
        return json;
    }

    private static void ports(final ArrayNode target, final List<NodePorts.Entry> entries) {
        for (final NodePorts.Entry entry : entries) {
            target
                    .addObject()
                    .put("flow", entry.port().flow())
                    .put("label", entry.port().label())
                    .put("declared", entry.declared());
        }
    }

    private static ObjectNode link(final Link link) {
        return JSON
                .createObjectNode()
                .put("fromNode", link.from().node().value())
                .put("fromFlow", link.from().flow())
                .put("toNode", link.to().node().value())
                .put("toFlow", link.to().flow());
    }

    private static JsonNode selection(final Selection selection) {
        if (selection instanceof Selection.OfNode node) {
            return JSON.createObjectNode().put("kind", "node").put("id", node.id().value());
        }
        if (selection instanceof Selection.OfLink link) {
            return link(link.link()).put("kind", "link");
        }
        return JSON.nullNode();
    }

    /** Applies one browser event; returns false when it was malformed or unknown (and therefore ignored). */
    public boolean handle(final String detail) {
        final JsonNode event;
        try {
            event = JSON.readTree(detail);
        } catch (final java.io.IOException e) {
            return false;
        }
        if (event == null || !event.isObject()) {
            return false;
        }
        try {
            switch (event.path("type").asText()) {
                case "select":
                    return select(event);
                case "move":
                    canvas
                            .moveNode(new NodeId(text(event, "id")),
                                    new Position(number(event, "x"), number(event, "y")));
                    return true;
                case "drop":
                    canvas
                            .dropConnector(new ConnectorId(
                                    text(event, "connector")), new Position(number(event, "x"), number(event, "y")));
                    return true;
                case "rename":
                    canvas.editor().setLabel(new NodeId(text(event, "id")), text(event, "label"));
                    return true;
                case "link":
                    return link(event);
                case "delete":
                    canvas.deleteSelection();
                    return true;
                default:
                    return false;
            }
        } catch (final IllegalArgumentException e) {
            return false;
        }
    }

    private boolean select(final JsonNode event) {
        switch (event.path("kind").asText()) {
            case "node":
                canvas.select(new NodeId(text(event, "id")));
                return true;
            case "link":
                canvas.select(linkOf(event));
                return true;
            case "none":
                canvas.clearSelection();
                return true;
            default:
                return false;
        }
    }

    private boolean link(final JsonNode event) {
        final Link link = linkOf(event);
        if (canvas.beginLink(link.from())) {
            canvas.completeLink(link.to());
        }
        canvas.cancelLink();
        return true;
    }

    private static Link linkOf(final JsonNode event) {
        return new Link(new Endpoint(new NodeId(text(event, "fromNode")), text(event, "fromFlow")),
                new Endpoint(new NodeId(text(event, "toNode")), text(event, "toFlow")));
    }

    private static String text(final JsonNode event, final String field) {
        final JsonNode value = event.get(field);
        if (value == null || !value.isTextual() || value.asText().isBlank()) {
            throw new IllegalArgumentException("Missing " + field);
        }
        return value.asText();
    }

    private static double number(final JsonNode event, final String field) {
        final JsonNode value = event.get(field);
        if (value == null || !value.isNumber() || !Double.isFinite(value.asDouble())) {
            throw new IllegalArgumentException("Missing " + field);
        }
        return value.asDouble();
    }

}
