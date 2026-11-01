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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.talend.sdk.component.tcketl.domain.Preconditions;
import org.talend.sdk.component.tcketl.domain.connector.ConnectorDefinition;
import org.talend.sdk.component.tcketl.domain.connector.ConnectorRef;
import org.talend.sdk.component.tcketl.domain.connector.ConnectorRole;
import org.talend.sdk.component.tcketl.domain.connector.Port;
import org.talend.sdk.component.tcketl.domain.pipeline.PipelineProblem.Kind;
import org.talend.sdk.component.tcketl.domain.pipeline.PipelineProblem.Severity;

/**
 * Single writer of a pipeline: applies the edit commands, enforces the link rules and notifies the listeners after
 * each change. It is not thread-safe, use it from one thread only (the JavaFX application thread in the designer).
 *
 * <p>
 * {@link #revision()} counts the changes of the persisted content (name, nodes, links), so a project can derive its
 * dirty flag from it. The resolution of the connector definitions is runtime state and never changes it.
 */
public final class PipelineEditor {

    private static final System.Logger LOGGER = System.getLogger(PipelineEditor.class.getName());

    private static final Pattern GENERATED_ID = Pattern.compile("n-(\\d{1,9})");

    private final Map<NodeId, Node> nodes = new LinkedHashMap<>();

    private final Map<NodeId, Resolution> resolutions = new HashMap<>();

    private final List<Link> links = new ArrayList<>();

    private final List<PipelineListener> listeners = new CopyOnWriteArrayList<>();

    private String name;

    private long revision;

    private int nextNodeNumber = 1;

    private Pipeline snapshot;

    public PipelineEditor() {
        this(Pipeline.DEFAULT_NAME);
    }

    public PipelineEditor(final String name) {
        this.name = Preconditions.orDefault(name, Pipeline.DEFAULT_NAME);
    }

    public Pipeline pipeline() {
        if (snapshot == null) {
            snapshot = new Pipeline(name, new ArrayList<>(nodes.values()), new ArrayList<>(links));
        }
        return snapshot;
    }

    public long revision() {
        return revision;
    }

    public Optional<Node> node(final NodeId id) {
        return Optional.ofNullable(nodes.get(id));
    }

    public Resolution resolution(final NodeId id) {
        requireNode(id);
        return resolutions.get(id);
    }

    public Optional<ConnectorDefinition> definition(final NodeId id) {
        return resolutions.get(id) instanceof Resolution.Resolved resolved ? Optional.of(resolved.definition())
                : Optional.empty();
    }

    public void addListener(final PipelineListener listener) {
        listeners.add(Preconditions.requireNonNull(listener, "listener"));
    }

    public void removeListener(final PipelineListener listener) {
        listeners.remove(listener);
    }

    public void rename(final String newName) {
        final String stripped = Preconditions.requireNonBlank(newName, "name").strip();
        if (!stripped.equals(name)) {
            name = stripped;
            changed();
            fire(new PipelineEvent.Renamed(stripped));
        }
    }

    /** Replaces the content. Every node goes back to {@link Resolution#PENDING}, the links are kept as stored. */
    public void load(final Pipeline pipeline) {
        Preconditions.requireNonNull(pipeline, "pipeline");
        nodes.clear();
        resolutions.clear();
        links.clear();
        int highestGenerated = 0;
        for (final Node node : pipeline.nodes()) {
            nodes.put(node.id(), node);
            resolutions.put(node.id(), Resolution.PENDING);
            final Matcher generated = GENERATED_ID.matcher(node.id().value());
            if (generated.matches()) {
                highestGenerated = Math.max(highestGenerated, Integer.parseInt(generated.group(1)));
            }
        }
        links.addAll(pipeline.links());
        name = pipeline.name();
        nextNodeNumber = highestGenerated + 1;
        changed();
        fire(new PipelineEvent.Reset(pipeline()));
    }

    /** Adds a node whose definition is still to be loaded. */
    public NodeId addNode(final ConnectorRef connector, final String label, final Position position) {
        Preconditions.requireNonNull(connector, "connector");
        final NodeId id = nextNodeId();
        insert(new Node(id, Preconditions.orDefault(label, connector.name()), connector, position, Map.of()),
                Resolution.PENDING);
        return id;
    }

    /** Adds a node from a definition that is already loaded. */
    public NodeId addNode(final ConnectorDefinition definition, final Position position) {
        Preconditions.requireNonNull(definition, "definition");
        final NodeId id = nextNodeId();
        insert(new Node(id, definition.displayName(), definition.ref(), position, Map.of()),
                new Resolution.Resolved(definition));
        return id;
    }

    public void bind(final NodeId id, final ConnectorDefinition definition) {
        final Node node = requireNode(id);
        Preconditions.requireNonNull(definition, "definition");
        if (!node.connector().id().equals(definition.id())) {
            throw new IllegalArgumentException(
                    "Definition " + definition.id() + " is not the connector of node " + id + " ("
                            + node.connector().id() + ")");
        }
        resolve(id, new Resolution.Resolved(definition));
    }

    public void markUnresolved(final NodeId id, final String reason) {
        requireNode(id);
        resolve(id, new Resolution.Unresolved(reason));
    }

    public void moveNode(final NodeId id, final Position position) {
        final Node node = requireNode(id);
        if (!node.position().equals(position)) {
            replace(node.withPosition(position));
            fire(new PipelineEvent.NodeMoved(id, position));
        }
    }

    public void setLabel(final NodeId id, final String label) {
        final Node node = requireNode(id);
        final String stripped = Preconditions.requireNonBlank(label, "label").strip();
        if (!node.label().equals(stripped)) {
            replace(node.withLabel(stripped));
            fire(new PipelineEvent.NodeRelabeled(id, stripped));
        }
    }

    /** Replaces the flat configuration of a node. Nothing happens when the content is unchanged. */
    public void setConfiguration(final NodeId id, final Map<String, String> configuration) {
        final Node node = requireNode(id);
        final Node updated = node.withConfiguration(configuration);
        if (!updated.configuration().equals(node.configuration())) {
            replace(updated);
            fire(new PipelineEvent.NodeConfigured(id, updated.configuration()));
        }
    }

    /**
     * Records that the configuration of a node was migrated by the server to a version of its connector: the node keeps
     * the new version and the migrated configuration.
     */
    public void migrated(final NodeId id, final int version, final Map<String, String> configuration) {
        final Node node = requireNode(id);
        final ConnectorRef current = node.connector();
        final Node updated = new Node(node.id(), node.label(),
                new ConnectorRef(current.id(), current.family(), current.name(), current.plugin(), version),
                node.position(), configuration);
        if (!updated.equals(node)) {
            replace(updated);
            fire(new PipelineEvent.NodeConfigured(id, updated.configuration()));
        }
    }

    /** Removes a node together with the links attached to it, which are returned. */
    public List<Link> removeNode(final NodeId id) {
        final Node node = requireNode(id);
        final List<Link> removed = links.stream().filter(link -> touches(link, id)).toList();
        links.removeIf(link -> touches(link, id));
        nodes.remove(id);
        resolutions.remove(id);
        changed();
        fire(new PipelineEvent.NodeRemoved(node, removed));
        return removed;
    }

    public Optional<LinkRejection> checkLink(final Link link) {
        return Optional.ofNullable(rejectionOf(Preconditions.requireNonNull(link, "link")));
    }

    /** Adds a link or throws a {@link LinkRejectedException} telling which rule it breaks. */
    public void connect(final Link link) {
        final LinkRejection rejection = rejectionOf(Preconditions.requireNonNull(link, "link"));
        if (rejection != null) {
            throw new LinkRejectedException(rejection);
        }
        links.add(link);
        changed();
        fire(new PipelineEvent.LinkAdded(link));
    }

    public boolean disconnect(final Link link) {
        if (!links.remove(link)) {
            return false;
        }
        changed();
        fire(new PipelineEvent.LinkRemoved(link));
        return true;
    }

    /**
     * Non-blocking findings. The "no input" check is skipped while a definition is still loading because the roles
     * are not all known yet.
     */
    public List<PipelineProblem> problems() {
        final List<PipelineProblem> problems = new ArrayList<>();
        boolean complete = true;
        boolean hasSource = false;
        for (final Node node : nodes.values()) {
            final Resolution resolution = resolutions.get(node.id());
            if (resolution instanceof Resolution.Resolved resolved) {
                final ConnectorRole role = resolved.definition().role();
                hasSource = hasSource || role == ConnectorRole.INPUT || role == ConnectorRole.STANDALONE;
                collectProblems(node, resolved.definition(), problems);
            } else {
                complete = false;
                if (resolution instanceof Resolution.Unresolved unresolved) {
                    problems
                            .add(new PipelineProblem(Severity.ERROR, Kind.NODE_UNRESOLVED, node.id(), "",
                                    "Connector '" + node.connector().name() + "' is not available: "
                                            + unresolved.reason()));
                }
            }
        }
        if (complete && !nodes.isEmpty() && !hasSource) {
            problems
                    .add(new PipelineProblem(Severity.WARNING, Kind.NO_INPUT_NODE, null, "",
                            "The pipeline has no input connector"));
        }
        return List.copyOf(problems);
    }

    private void collectProblems(final Node node, final ConnectorDefinition definition,
            final List<PipelineProblem> problems) {
        final ConnectorRole role = definition.role();
        if (role == ConnectorRole.INPUT && !definition.optionalRow()
                && links.stream().noneMatch(link -> link.from().node().equals(node.id()))) {
            problems
                    .add(new PipelineProblem(Severity.WARNING, Kind.INPUT_NOT_CONNECTED, node.id(), "",
                            "Input '" + node.label() + "' is not linked to any connector"));
        }
        if (role == ConnectorRole.PROCESSOR || role == ConnectorRole.OUTPUT) {
            for (final Port port : definition.inputPorts()) {
                final Endpoint endpoint = new Endpoint(node.id(), port.flow());
                if (links.stream().noneMatch(link -> link.to().equals(endpoint))) {
                    problems
                            .add(new PipelineProblem(Severity.WARNING, Kind.INPUT_PORT_NOT_CONNECTED, node.id(),
                                    port.flow(),
                                    "Input port '" + port.label() + "' of '" + node.label() + "' is not linked"));
                }
            }
        }
        for (final Link link : links) {
            if (link.from().node().equals(node.id()) && !definition.hasOutputFlow(link.from().flow())) {
                problems.add(unknownFlow(node, "output", link.from().flow()));
            }
            if (link.to().node().equals(node.id()) && !definition.hasInputFlow(link.to().flow())) {
                problems.add(unknownFlow(node, "input", link.to().flow()));
            }
        }
    }

    private PipelineProblem unknownFlow(final Node node, final String direction, final String flow) {
        return new PipelineProblem(Severity.ERROR, Kind.LINK_FLOW_UNKNOWN, node.id(), flow,
                "'" + node.label() + "' has no " + direction + " flow named '" + flow + "'");
    }

    private LinkRejection rejectionOf(final Link link) {
        final NodeId sourceId = link.from().node();
        final NodeId targetId = link.to().node();
        if (!nodes.containsKey(sourceId) || !nodes.containsKey(targetId)) {
            return LinkRejection.UNKNOWN_NODE;
        }
        if (sourceId.equals(targetId)) {
            return LinkRejection.SELF_LINK;
        }
        final Optional<ConnectorDefinition> source = definition(sourceId);
        final Optional<ConnectorDefinition> target = definition(targetId);
        if (source.isEmpty() || target.isEmpty()) {
            return LinkRejection.NODE_NOT_RESOLVED;
        }
        if (source.get().outputFlows().isEmpty()) {
            return LinkRejection.SOURCE_HAS_NO_OUTPUT_PORT;
        }
        if (target.get().inputFlows().isEmpty()) {
            return LinkRejection.TARGET_HAS_NO_INPUT_PORT;
        }
        if (!source.get().hasOutputFlow(link.from().flow())) {
            return LinkRejection.UNKNOWN_OUTPUT_FLOW;
        }
        if (!target.get().hasInputFlow(link.to().flow())) {
            return LinkRejection.UNKNOWN_INPUT_FLOW;
        }
        if (links.contains(link)) {
            return LinkRejection.DUPLICATE_LINK;
        }
        if (links.stream().anyMatch(existing -> existing.to().equals(link.to()))) {
            return LinkRejection.INPUT_PORT_TAKEN;
        }
        if (reaches(targetId, sourceId)) {
            return LinkRejection.CYCLE;
        }
        return null;
    }

    private boolean reaches(final NodeId start, final NodeId goal) {
        final Set<NodeId> visited = new HashSet<>();
        final Deque<NodeId> pending = new ArrayDeque<>();
        pending.push(start);
        while (!pending.isEmpty()) {
            final NodeId current = pending.pop();
            if (current.equals(goal)) {
                return true;
            }
            if (visited.add(current)) {
                for (final Link link : links) {
                    if (link.from().node().equals(current)) {
                        pending.push(link.to().node());
                    }
                }
            }
        }
        return false;
    }

    private static boolean touches(final Link link, final NodeId id) {
        return link.from().node().equals(id) || link.to().node().equals(id);
    }

    private Node requireNode(final NodeId id) {
        final Node node = nodes.get(id);
        if (node == null) {
            throw new NoSuchElementException("Unknown node: " + id);
        }
        return node;
    }

    private NodeId nextNodeId() {
        NodeId id;
        do {
            id = new NodeId("n-" + nextNodeNumber++);
        } while (nodes.containsKey(id));
        return id;
    }

    private void insert(final Node node, final Resolution resolution) {
        nodes.put(node.id(), node);
        resolutions.put(node.id(), resolution);
        changed();
        fire(new PipelineEvent.NodeAdded(node));
    }

    private void replace(final Node node) {
        nodes.put(node.id(), node);
        changed();
    }

    private void resolve(final NodeId id, final Resolution resolution) {
        if (!resolution.equals(resolutions.put(id, resolution))) {
            fire(new PipelineEvent.NodeResolutionChanged(id, resolution));
        }
    }

    private void changed() {
        revision++;
        snapshot = null;
    }

    private void fire(final PipelineEvent event) {
        for (final PipelineListener listener : listeners) {
            try {
                listener.onEvent(event);
            } catch (final RuntimeException e) {
                LOGGER.log(System.Logger.Level.ERROR, "Pipeline listener failed on " + event, e);
            }
        }
    }
}
