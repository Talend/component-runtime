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
package org.talend.sdk.component.tcketl.app.canvas;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.property.ReadOnlyLongProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.talend.sdk.component.tcketl.app.session.ServerSession;
import org.talend.sdk.component.tcketl.domain.connector.ConnectorDefinition;
import org.talend.sdk.component.tcketl.domain.connector.ConnectorId;
import org.talend.sdk.component.tcketl.domain.connector.ConnectorSummary;
import org.talend.sdk.component.tcketl.domain.pipeline.Endpoint;
import org.talend.sdk.component.tcketl.domain.pipeline.Link;
import org.talend.sdk.component.tcketl.domain.pipeline.LinkRejection;
import org.talend.sdk.component.tcketl.domain.pipeline.Node;
import org.talend.sdk.component.tcketl.domain.pipeline.NodeId;
import org.talend.sdk.component.tcketl.domain.pipeline.PipelineEditor;
import org.talend.sdk.component.tcketl.domain.pipeline.PipelineEvent;
import org.talend.sdk.component.tcketl.domain.pipeline.PipelineListener;
import org.talend.sdk.component.tcketl.domain.pipeline.PipelineProblem;
import org.talend.sdk.component.tcketl.domain.pipeline.Position;

/**
 * What the canvas does, without any drawing: it turns the gestures of the user (drop a connector, move or delete a
 * node, draw a link) into edits of the pipeline, and keeps the state the views display next to the pipeline itself:
 * the selection, the zoom, the findings about the pipeline and the sentence explaining a refusal.
 *
 * <p>
 * The {@link PipelineEditor} stays the owner of the rules, so a link is refused for the same reason whatever way it
 * is made. The views read the pipeline from {@link #editor()} and MUST NOT change it, so that the selection stays
 * consistent with it. This view model listens to the editor before the views do, which is what lets a view find the
 * selection up to date when it is told about a change.
 *
 * <p>
 * Everything MUST be used on the UI thread.
 */
public final class CanvasViewModel implements AutoCloseable {

    /** Distance between the lines of the grid the nodes snap to. */
    public static final double GRID = 20;

    public static final double MIN_ZOOM = 0.25;

    public static final double MAX_ZOOM = 2;

    public static final Position FIRST_POSITION = new Position(2 * GRID, 2 * GRID);

    private static final double ZOOM_STEP = 1.25;

    // far enough for any pipeline, close enough to keep the scrollable area sane
    private static final double MAX_COORDINATE = 50_000;

    private static final double CASCADE = 2 * GRID;

    private static final double OVERLAP = 1.5 * GRID;

    private final PipelineEditor editor;

    private final ServerSession session;

    private final PipelineListener listener = this::onEvent;

    private final ReadOnlyObjectWrapper<Selection> selection = new ReadOnlyObjectWrapper<>(Selection.NONE);

    private final ObservableList<PipelineProblem> problems = FXCollections.observableArrayList();

    private final ObservableList<PipelineProblem> readOnlyProblems =
            FXCollections.unmodifiableObservableList(problems);

    private final ReadOnlyStringWrapper feedback = new ReadOnlyStringWrapper("");

    private final ReadOnlyObjectWrapper<Endpoint> linkSource = new ReadOnlyObjectWrapper<>();

    private final ReadOnlyDoubleWrapper zoom = new ReadOnlyDoubleWrapper(1);

    private final ReadOnlyBooleanWrapper empty = new ReadOnlyBooleanWrapper();

    public CanvasViewModel(final PipelineEditor editor, final ServerSession session) {
        this.editor = editor;
        this.session = session;
        refresh();
        editor.addListener(listener);
    }

    /** The pipeline, to read and to listen to: the changes go through this view model. */
    public PipelineEditor editor() {
        return editor;
    }

    public ReadOnlyObjectProperty<Selection> selection() {
        return selection.getReadOnlyProperty();
    }

    /** The findings about the pipeline, kept up to date after each change. */
    public ObservableList<PipelineProblem> problems() {
        return readOnlyProblems;
    }

    /** Why the last gesture was refused, or what the one being made would do: blank when there is nothing to say. */
    public ReadOnlyStringProperty feedback() {
        return feedback.getReadOnlyProperty();
    }

    /** The output a link is being drawn from, {@code null} when the user is not drawing one. */
    public ReadOnlyObjectProperty<Endpoint> linkSource() {
        return linkSource.getReadOnlyProperty();
    }

    public ReadOnlyDoubleProperty zoom() {
        return zoom.getReadOnlyProperty();
    }

    /** True while the pipeline has no node. */
    public ReadOnlyBooleanProperty empty() {
        return empty.getReadOnlyProperty();
    }

    /** Changes each time the server or its connectors change, which is when the icons have to be asked again. */
    public ReadOnlyLongProperty serverRevision() {
        return session.revision();
    }

    /** The ports to show on a node, none for a node that is not in the pipeline. */
    public NodePorts ports(final NodeId id) {
        return editor.node(id).isPresent()
                ? NodePorts.of(id, editor.resolution(id), editor.pipeline().links())
                : new NodePorts(List.of(), List.of());
    }

    /** Snaps a position to the grid, and keeps it inside the area the canvas scrolls over. */
    public static Position snap(final Position position) {
        return new Position(snap(position.x()), snap(position.y()));
    }

    private static double snap(final double coordinate) {
        return Math.min(MAX_COORDINATE, Math.max(GRID, Math.round(coordinate / GRID) * GRID));
    }

    /**
     * Adds a node for a connector of the palette and selects it. Its label is the display name of the connector, made
     * unique among the nodes.
     */
    public NodeId addConnector(final ConnectorSummary connector, final Position position) {
        final NodeId id = editor.addNode(connector.ref(), uniqueLabel(connector.displayName()), snap(position));
        select(id);
        return id;
    }

    /** Adds a node on the first free spot from the top left corner. */
    public NodeId addConnector(final ConnectorSummary connector) {
        return addConnector(connector, nextFreePosition(FIRST_POSITION));
    }

    /**
     * Adds the node of a connector that was dragged from the palette, which is identified by its id.
     *
     * @return the node, or empty when the server does not list this connector (anymore)
     */
    public Optional<NodeId> dropConnector(final ConnectorId id, final Position position) {
        final Optional<ConnectorSummary> connector =
                session.connectors().get().stream().filter(candidate -> candidate.id().equals(id)).findFirst();
        if (connector.isEmpty()) {
            feedback.set("This connector is not in the palette anymore");
            return Optional.empty();
        }
        return Optional.of(addConnector(connector.get(), position));
    }

    /** The first spot from {@code from} that does not hide a node, going down and right. */
    public Position nextFreePosition(final Position from) {
        final List<Node> nodes = editor.pipeline().nodes();
        Position candidate = snap(from);
        while (isTaken(nodes, candidate)) {
            candidate = new Position(candidate.x() + CASCADE, candidate.y() + CASCADE);
        }
        return candidate;
    }

    public void moveNode(final NodeId id, final Position position) {
        if (editor.node(id).isPresent()) {
            editor.moveNode(id, snap(position));
        }
    }

    /** @return false when the label is blank or the node unknown, in which case nothing changed */
    public boolean renameNode(final NodeId id, final String label) {
        if (label == null || label.isBlank() || editor.node(id).isEmpty()) {
            return false;
        }
        editor.setLabel(id, label);
        return true;
    }

    public void deleteNode(final NodeId id) {
        if (editor.node(id).isPresent()) {
            editor.removeNode(id);
        }
    }

    public void deleteLink(final Link link) {
        editor.disconnect(link);
    }

    /** Deletes the selected node, with its links, or the selected link. */
    public void deleteSelection() {
        final Selection current = selection.get();
        if (current instanceof Selection.OfNode node) {
            deleteNode(node.id());
        } else if (current instanceof Selection.OfLink selected) {
            deleteLink(selected.link());
        }
    }

    public void select(final NodeId id) {
        if (editor.node(id).isPresent()) {
            selection.set(new Selection.OfNode(id));
            feedback.set("");
        }
    }

    public void select(final Link link) {
        if (editor.pipeline().links().contains(link)) {
            selection.set(new Selection.OfLink(link));
            feedback.set("");
        }
    }

    public void clearSelection() {
        selection.set(Selection.NONE);
        feedback.set("");
    }

    public void clearFeedback() {
        feedback.set("");
    }

    /**
     * Starts drawing a link from an output. Only an output the connector declares can start one.
     *
     * @return false, with the reason in {@link #feedback()}, when the link cannot start from there
     */
    public boolean beginLink(final Endpoint source) {
        cancelLink();
        final Optional<ConnectorDefinition> definition = editor.definition(source.node());
        final LinkRejection refusal;
        if (editor.node(source.node()).isEmpty()) {
            refusal = LinkRejection.UNKNOWN_NODE;
        } else if (definition.isEmpty()) {
            refusal = LinkRejection.NODE_NOT_RESOLVED;
        } else if (!definition.get().hasOutputFlow(source.flow())) {
            refusal = LinkRejection.UNKNOWN_OUTPUT_FLOW;
        } else {
            refusal = null;
        }
        if (refusal != null) {
            feedback.set(refusal.message());
            return false;
        }
        feedback.set("");
        linkSource.set(source);
        return true;
    }

    /** Why the link being drawn cannot end on {@code target}, empty when it can. Without a link being drawn, no. */
    public Optional<LinkRejection> check(final Endpoint target) {
        final Endpoint source = linkSource.get();
        return source == null ? Optional.of(LinkRejection.UNKNOWN_NODE) : editor.checkLink(new Link(source, target));
    }

    /** The pointer is over {@code target} ({@code null} for no port): tells what would happen on release. */
    public void hover(final Endpoint target) {
        feedback.set(
                linkSource.get() == null || target == null ? "" : check(target).map(LinkRejection::message).orElse(""));
    }

    /**
     * Ends the link being drawn on {@code target}, or without target for {@code null}. A link that breaks a rule is
     * not made and the rule is told by {@link #feedback()}.
     *
     * @return true when the link was made
     */
    public boolean completeLink(final Endpoint target) {
        final Endpoint source = linkSource.get();
        if (source == null) {
            return false;
        }
        linkSource.set(null);
        if (target == null) {
            feedback.set("");
            return false;
        }
        final Link link = new Link(source, target);
        final Optional<LinkRejection> refusal = editor.checkLink(link);
        if (refusal.isPresent()) {
            feedback.set(refusal.get().message());
            return false;
        }
        feedback.set("");
        editor.connect(link);
        return true;
    }

    public void cancelLink() {
        if (linkSource.get() != null) {
            linkSource.set(null);
            feedback.set("");
        }
    }

    public void zoomIn() {
        setZoom(zoom.get() * ZOOM_STEP);
    }

    public void zoomOut() {
        setZoom(zoom.get() / ZOOM_STEP);
    }

    /** @param factor what the zoom is multiplied by, for example 1.1 to zoom in by a tenth */
    public void zoomBy(final double factor) {
        setZoom(zoom.get() * factor);
    }

    public void resetZoom() {
        setZoom(1);
    }

    @Override
    public void close() {
        editor.removeListener(listener);
    }

    private void setZoom(final double value) {
        if (Double.isFinite(value)) {
            zoom.set(Math.round(Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, value)) * 100) / 100d);
        }
    }

    private void onEvent(final PipelineEvent event) {
        if (event instanceof PipelineEvent.NodeMoved) {
            return;
        }
        if (event instanceof PipelineEvent.Reset) {
            selection.set(Selection.NONE);
            linkSource.set(null);
            feedback.set("");
        } else if (event instanceof PipelineEvent.NodeRemoved removed) {
            final Selection current = selection.get();
            if (current.selects(removed.node().id()) || removed.removedLinks().stream().anyMatch(current::selects)) {
                selection.set(Selection.NONE);
            }
            dropLinkFrom(removed.node().id());
        } else if (event instanceof PipelineEvent.LinkRemoved removed && selection.get().selects(removed.link())) {
            selection.set(Selection.NONE);
        } else if (event instanceof PipelineEvent.NodeResolutionChanged changed) {
            dropLinkFrom(changed.node());
        }
        refresh();
    }

    // the output the link was drawn from does not exist anymore, or may not be the same
    private void dropLinkFrom(final NodeId id) {
        final Endpoint source = linkSource.get();
        if (source != null && source.node().equals(id)) {
            cancelLink();
        }
    }

    private void refresh() {
        final List<PipelineProblem> current = editor.problems();
        if (!current.equals(problems)) {
            problems.setAll(current);
        }
        empty.set(editor.pipeline().nodes().isEmpty());
    }

    private String uniqueLabel(final String name) {
        final Set<String> used = new HashSet<>();
        editor.pipeline().nodes().forEach(node -> used.add(node.label()));
        String label = name;
        for (int number = 2; used.contains(label); number++) {
            label = name + " " + number;
        }
        return label;
    }

    private static boolean isTaken(final List<Node> nodes, final Position candidate) {
        return nodes
                .stream()
                .anyMatch(node -> Math.abs(node.position().x() - candidate.x()) < OVERLAP
                        && Math.abs(node.position().y() - candidate.y()) < OVERLAP);
    }
}
