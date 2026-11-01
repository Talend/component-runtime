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

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.talend.sdk.component.tcketl.domain.Preconditions;

/**
 * Immutable snapshot of what is persisted: the nodes and the links. Link rules that depend on the connector
 * definitions are enforced by the {@link PipelineEditor}; the snapshot only guarantees structural integrity.
 */
public record Pipeline(String name, List<Node> nodes, List<Link> links) {

    public static final String DEFAULT_NAME = "Untitled";

    public Pipeline {
        name = Preconditions.orDefault(name, DEFAULT_NAME);
        nodes = Preconditions.immutableCopy(nodes, "nodes");
        links = Preconditions.immutableCopy(links, "links");
        final Set<NodeId> ids = new HashSet<>();
        for (final Node node : nodes) {
            if (!ids.add(node.id())) {
                throw new IllegalArgumentException("Duplicate node id: " + node.id());
            }
        }
        final Set<Link> distinct = new HashSet<>();
        for (final Link link : links) {
            if (!ids.contains(link.from().node()) || !ids.contains(link.to().node())) {
                throw new IllegalArgumentException("Link refers to an unknown node: " + link);
            }
            if (!distinct.add(link)) {
                throw new IllegalArgumentException("Duplicate link: " + link);
            }
        }
    }

    public static Pipeline empty() {
        return new Pipeline(DEFAULT_NAME, List.of(), List.of());
    }

    public Optional<Node> node(final NodeId id) {
        return nodes.stream().filter(node -> node.id().equals(id)).findFirst();
    }

    public List<Link> outgoing(final NodeId id) {
        return links.stream().filter(link -> link.from().node().equals(id)).toList();
    }

    public List<Link> incoming(final NodeId id) {
        return links.stream().filter(link -> link.to().node().equals(id)).toList();
    }
}
