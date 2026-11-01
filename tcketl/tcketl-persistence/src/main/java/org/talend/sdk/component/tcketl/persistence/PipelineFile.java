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
package org.talend.sdk.component.tcketl.persistence;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.talend.sdk.component.tcketl.domain.Preconditions;
import org.talend.sdk.component.tcketl.domain.connector.ConnectorId;
import org.talend.sdk.component.tcketl.domain.connector.ConnectorRef;
import org.talend.sdk.component.tcketl.domain.pipeline.Endpoint;
import org.talend.sdk.component.tcketl.domain.pipeline.Link;
import org.talend.sdk.component.tcketl.domain.pipeline.Node;
import org.talend.sdk.component.tcketl.domain.pipeline.NodeId;
import org.talend.sdk.component.tcketl.domain.pipeline.Pipeline;
import org.talend.sdk.component.tcketl.domain.pipeline.Position;

/**
 * On-disk shape of a pipeline, format 1.x. The JSON names are the file contract: renaming a component of these records
 * changes the format. The node configuration is the flat TCK map and is stored as is.
 *
 * <p>
 * Reading is lenient where a sensible default exists (label, position, configuration, links) and strict about what
 * identifies a node and its connector, in particular the connector version that drives the migration.
 */
record PipelineFile(
        @JsonProperty(JsonFiles.VERSION_FIELD) String formatVersion,
        @JsonProperty("name") String name,
        @JsonProperty("nodes") List<NodeFile> nodes,
        @JsonProperty("links") List<LinkFile> links
) {

    static final FormatVersion CURRENT = new FormatVersion(1, 0);

    static PipelineFile from(final Pipeline pipeline) {
        return new PipelineFile(CURRENT.toString(), pipeline.name(),
                pipeline.nodes().stream().map(NodeFile::from).toList(),
                pipeline.links().stream().map(LinkFile::from).toList());
    }

    Pipeline toPipeline() throws FileFormatException {
        final List<NodeFile> fileNodes = orEmpty(nodes);
        final List<Node> domainNodes = new ArrayList<>();
        for (int i = 0; i < fileNodes.size(); i++) {
            if (fileNodes.get(i) == null) {
                throw new FileFormatException("'nodes[" + i + "]' is null");
            }
            domainNodes.add(fileNodes.get(i).toNode(i));
        }
        final List<LinkFile> fileLinks = orEmpty(links);
        final List<Link> domainLinks = new ArrayList<>();
        for (int i = 0; i < fileLinks.size(); i++) {
            if (fileLinks.get(i) == null) {
                throw new FileFormatException("'links[" + i + "]' is null");
            }
            domainLinks.add(fileLinks.get(i).toLink(i));
        }
        try {
            return new Pipeline(name, domainNodes, domainLinks);
        } catch (final IllegalArgumentException e) {
            throw new FileFormatException("Invalid pipeline: " + e.getMessage(), e);
        }
    }

    private static <T> List<T> orEmpty(final List<T> values) {
        return values == null ? List.of() : values;
    }

    private static <T> T required(final T value, final String what) {
        if (value == null) {
            throw new IllegalArgumentException("'" + what + "' is required");
        }
        return value;
    }

    record NodeFile(
            @JsonProperty("id") String id,
            @JsonProperty("label") String label,
            @JsonProperty("component") ComponentFile component,
            @JsonProperty("position") PositionFile position,
            @JsonProperty("configuration") Map<String, String> configuration
    ) {

        static NodeFile from(final Node node) {
            return new NodeFile(node.id().value(), node.label(), ComponentFile.from(node.connector()),
                    PositionFile.from(node.position()), node.configuration());
        }

        Node toNode(final int index) throws FileFormatException {
            try {
                final ConnectorRef connector = required(component, "component").toRef();
                return new Node(new NodeId(id), Preconditions.orDefault(label, connector.name()), connector,
                        position == null ? new Position(0, 0) : position.toPosition(),
                        configuration == null ? Map.of() : configuration);
            } catch (final IllegalArgumentException | NullPointerException e) {
                throw new FileFormatException("Invalid nodes[" + index + "]"
                        + (id == null || id.isBlank() ? "" : " (" + id + ")") + ": " + e.getMessage(), e);
            }
        }
    }

    record ComponentFile(
            @JsonProperty("id") String id,
            @JsonProperty("family") String family,
            @JsonProperty("name") String name,
            @JsonProperty("plugin") String plugin,
            @JsonProperty("version") Integer version
    ) {

        static ComponentFile from(final ConnectorRef connector) {
            return new ComponentFile(connector.id().value(), connector.family(), connector.name(),
                    connector.plugin(), connector.version());
        }

        ConnectorRef toRef() {
            return new ConnectorRef(new ConnectorId(id), family, name, plugin, required(version, "component.version"));
        }
    }

    record PositionFile(@JsonProperty("x") Double x, @JsonProperty("y") Double y) {

        static PositionFile from(final Position position) {
            return new PositionFile(position.x(), position.y());
        }

        Position toPosition() {
            return new Position(required(x, "position.x"), required(y, "position.y"));
        }
    }

    record LinkFile(@JsonProperty("from") EndpointFile from, @JsonProperty("to") EndpointFile to) {

        static LinkFile from(final Link link) {
            return new LinkFile(EndpointFile.from(link.from()), EndpointFile.from(link.to()));
        }

        Link toLink(final int index) throws FileFormatException {
            try {
                return new Link(required(from, "from").toEndpoint(), required(to, "to").toEndpoint());
            } catch (final IllegalArgumentException | NullPointerException e) {
                throw new FileFormatException("Invalid links[" + index + "]: " + e.getMessage(), e);
            }
        }
    }

    record EndpointFile(@JsonProperty("node") String node, @JsonProperty("flow") String flow) {

        static EndpointFile from(final Endpoint endpoint) {
            return new EndpointFile(endpoint.node().value(), endpoint.flow());
        }

        Endpoint toEndpoint() {
            return new Endpoint(new NodeId(node), flow);
        }
    }
}
