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

import java.lang.System.Logger;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.Tooltip;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.input.ZoomEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.CubicCurve;
import javafx.scene.transform.Scale;
import javafx.util.Duration;

import org.talend.sdk.component.tcketl.app.icon.IconService;
import org.talend.sdk.component.tcketl.app.palette.PaletteTransfer;
import org.talend.sdk.component.tcketl.domain.connector.ConnectorId;
import org.talend.sdk.component.tcketl.domain.connector.ConnectorSummary;
import org.talend.sdk.component.tcketl.domain.connector.Port;
import org.talend.sdk.component.tcketl.domain.pipeline.Endpoint;
import org.talend.sdk.component.tcketl.domain.pipeline.Link;
import org.talend.sdk.component.tcketl.domain.pipeline.Node;
import org.talend.sdk.component.tcketl.domain.pipeline.NodeId;
import org.talend.sdk.component.tcketl.domain.pipeline.Pipeline;
import org.talend.sdk.component.tcketl.domain.pipeline.PipelineEditor;
import org.talend.sdk.component.tcketl.domain.pipeline.PipelineEvent;
import org.talend.sdk.component.tcketl.domain.pipeline.PipelineListener;
import org.talend.sdk.component.tcketl.domain.pipeline.Position;
import org.talend.sdk.component.tcketl.domain.pipeline.Resolution;

/**
 * Draws the pipeline of a {@link CanvasViewModel} and turns what the user does into calls to it: connectors dropped
 * from the palette, nodes moved, links drawn from an output to an input, the selection deleted. The pipeline is only
 * read here, the editor tells what changed and the views follow.
 *
 * <p>
 * The canvas scrolls and pans, and zooms with the buttons or Ctrl and the wheel.
 */
public final class CanvasView extends StackPane {

    private static final Logger LOGGER = System.getLogger(CanvasView.class.getName());

    // room kept after the last node, so that there is always some to drop or to move a node further, without the
    // scroll bars showing while everything is in view
    private static final double SURFACE_MARGIN = 100;

    // where the node lands relative to the pointer when it is dropped: the pointer is on its header
    private static final double DROP_OFFSET_X = 60;

    private static final double DROP_OFFSET_Y = 18;

    private static final double WHEEL_ZOOM = 1.1;

    private static final Duration FEEDBACK_DURATION = Duration.seconds(3);

    private final CanvasViewModel model;

    private final IconService icons;

    private final PipelineEditor editor;

    private final PipelineListener listener = this::onEvent;

    private final Map<NodeId, NodeView> nodes = new LinkedHashMap<>();

    private final Map<Link, LinkView> links = new LinkedHashMap<>();

    private final Surface surface = new Surface();

    private final Pane linkLayer = new Pane();

    private final Pane nodeLayer = new Pane();

    private final CubicCurve ghost = new CubicCurve();

    private final ScrollPane scroll = new ScrollPane();

    private final PauseTransition feedbackTimer = new PauseTransition(FEEDBACK_DURATION);

    private double viewportWidth;

    private double viewportHeight;

    // the pointer in the coordinates of the surface, while a link is drawn
    private Point2D pointer;

    private PortView hovered;

    public CanvasView(final CanvasViewModel model, final IconService icons) {
        this.model = model;
        this.icons = icons;
        this.editor = model.editor();
        getStyleClass().add("canvas");

        linkLayer.setPickOnBounds(false);
        nodeLayer.setPickOnBounds(false);
        ghost.getStyleClass().add("link-ghost");
        ghost.setFill(null);
        ghost.setManaged(false);
        ghost.setMouseTransparent(true);
        ghost.setVisible(false);
        surface.getStyleClass().add("canvas-surface");
        surface.setFocusTraversable(true);
        surface.getChildren().addAll(linkLayer, nodeLayer, ghost);

        final Scale scale = new Scale(1, 1, 0, 0);
        scale.xProperty().bind(model.zoom());
        scale.yProperty().bind(model.zoom());
        surface.getTransforms().add(scale);

        // the group is what makes the scroll pane take the zoom into account to know how far it scrolls
        scroll.setContent(new Group(surface));
        scroll.setPannable(true);
        scroll.getStyleClass().add("canvas-scroll");
        scroll.viewportBoundsProperty().addListener((observed, before, bounds) -> {
            viewportWidth = bounds.getWidth();
            viewportHeight = bounds.getHeight();
            surface.requestLayout();
        });
        model.zoom().addListener((observed, before, zoom) -> surface.requestLayout());

        getChildren().addAll(scroll, hint(), feedback(), zoomControls());
        surfaceHandlers();
        keyHandlers();

        model.selection().addListener((observed, before, selection) -> restyle());
        model.linkSource().addListener((observed, before, source) -> onLinkSourceChanged(source));
        // the icons are asked again whenever the server changes, and the old ones are not updated anymore
        model.serverRevision().addListener((observed, before, revision) -> nodes.keySet().forEach(this::bindIcon));

        rebuild(editor.pipeline());
        editor.addListener(listener);
    }

    /** Adds the connector to the canvas, in the visible part of it: what a double click in the palette does. */
    public void addConnectorInView(final ConnectorSummary connector) {
        final Point2D origin = surface.sceneToLocal(scroll.localToScene(0, 0));
        final double margin = 2 * CanvasViewModel.GRID;
        model.addConnector(connector,
                model.nextFreePosition(new Position(origin.getX() + margin, origin.getY() + margin)));
    }

    public void close() {
        editor.removeListener(listener);
        feedbackTimer.stop();
        links.values().forEach(LinkView::dispose);
    }

    private Label hint() {
        final Label hint = new Label("Drag a connector from the palette, or double-click it, to start a pipeline");
        hint.getStyleClass().add("canvas-hint");
        hint.setMouseTransparent(true);
        hint.visibleProperty().bind(model.empty());
        return hint;
    }

    private Label feedback() {
        final Label feedback = new Label();
        feedback.getStyleClass().add("canvas-feedback");
        feedback.setMouseTransparent(true);
        feedback.textProperty().bind(model.feedback());
        feedback.visibleProperty().bind(model.feedback().isNotEmpty());
        StackPane.setAlignment(feedback, Pos.BOTTOM_CENTER);
        StackPane.setMargin(feedback, new Insets(0, 0, 16, 0));
        feedbackTimer.setOnFinished(event -> {
            // what is said while a link is drawn is about the link, and is for as long as it is drawn
            if (model.linkSource().get() == null) {
                model.clearFeedback();
            }
        });
        model.feedback().addListener((observed, before, text) -> {
            feedbackTimer.stop();
            if (!text.isEmpty()) {
                feedbackTimer.playFromStart();
            }
        });
        return feedback;
    }

    private HBox zoomControls() {
        final Button out = zoomButton("\u2212", "Zoom out", model::zoomOut);
        final Button reset = zoomButton("100%", "Reset the zoom", model::resetZoom);
        reset.textProperty().bind(model.zoom().multiply(100).asString("%.0f%%"));
        final Button in = zoomButton("+", "Zoom in", model::zoomIn);
        final HBox controls = new HBox(out, reset, in);
        controls.getStyleClass().add("zoom-controls");
        controls.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        StackPane.setAlignment(controls, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(controls, new Insets(0, 24, 16, 0));
        return controls;
    }

    private static Button zoomButton(final String text, final String help, final Runnable action) {
        final Button button = new Button(text);
        button.getStyleClass().add("zoom-button");
        button.setFocusTraversable(false);
        button.setTooltip(new Tooltip(help));
        button.setOnAction(event -> action.run());
        return button;
    }

    private void surfaceHandlers() {
        // the keys of the canvas only work when it has the focus, which a click anywhere on it gives, unless the
        // click is in a field being edited
        surface.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            if (!insideTextInput(event.getTarget())) {
                surface.requestFocus();
            }
        });
        surface.setOnMousePressed(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getTarget() == surface) {
                model.clearSelection();
            }
        });

        surface.addEventFilter(MouseEvent.MOUSE_DRAGGED, event -> {
            if (model.linkSource().get() != null) {
                pointer = surface.sceneToLocal(event.getSceneX(), event.getSceneY());
                track(inputPortAt(event.getSceneX(), event.getSceneY()));
                updateGhost();
                event.consume();
            }
        });
        surface.addEventFilter(MouseEvent.MOUSE_RELEASED, event -> {
            if (model.linkSource().get() != null) {
                final PortView over = inputPortAt(event.getSceneX(), event.getSceneY());
                model.completeLink(over == null ? null : over.endpoint());
                event.consume();
            }
        });

        surface.setOnDragOver(event -> {
            if (event.getDragboard().hasContent(PaletteTransfer.CONNECTOR)) {
                event.acceptTransferModes(TransferMode.COPY);
                event.consume();
            }
        });
        surface.setOnDragDropped(event -> {
            final Dragboard board = event.getDragboard();
            boolean done = false;
            if (board.hasContent(PaletteTransfer.CONNECTOR)
                    && board.getContent(PaletteTransfer.CONNECTOR) instanceof String id && !id.isBlank()) {
                final Point2D at = surface.sceneToLocal(event.getSceneX(), event.getSceneY());
                done = model
                        .dropConnector(new ConnectorId(id),
                                new Position(at.getX() - DROP_OFFSET_X, at.getY() - DROP_OFFSET_Y))
                        .isPresent();
            }
            event.setDropCompleted(done);
            event.consume();
        });
    }

    private void keyHandlers() {
        addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (insideTextInput(event.getTarget())) {
                return;
            }
            switch (event.getCode()) {
                case DELETE:
                case BACK_SPACE:
                    model.deleteSelection();
                    event.consume();
                    break;
                case ESCAPE:
                    if (model.linkSource().get() != null) {
                        model.cancelLink();
                        event.consume();
                    }
                    break;
                default:
                    break;
            }
        });
        addEventFilter(ScrollEvent.SCROLL, event -> {
            if (event.isControlDown() && event.getDeltaY() != 0) {
                model.zoomBy(event.getDeltaY() > 0 ? WHEEL_ZOOM : 1 / WHEEL_ZOOM);
                event.consume();
            }
        });
        addEventHandler(ZoomEvent.ZOOM, event -> {
            model.zoomBy(event.getZoomFactor());
            event.consume();
        });
    }

    private static boolean insideTextInput(final Object target) {
        for (javafx.scene.Node current =
                target instanceof javafx.scene.Node node ? node : null; current != null; current =
                        current.getParent()) {
            if (current instanceof TextInputControl) {
                return true;
            }
        }
        return false;
    }

    // ---- what the editor tells

    private void onEvent(final PipelineEvent event) {
        if (event instanceof PipelineEvent.Reset reset) {
            rebuild(reset.pipeline());
        } else if (event instanceof PipelineEvent.NodeAdded added) {
            addNodeView(added.node());
        } else if (event instanceof PipelineEvent.NodeRemoved removed) {
            removed.removedLinks().forEach(this::removeLinkView);
            removeNodeView(removed.node().id());
            removed.removedLinks().forEach(this::refreshEnds);
        } else if (event instanceof PipelineEvent.NodeMoved moved) {
            view(moved.node()).ifPresent(view -> view.moveTo(moved.position()));
        } else if (event instanceof PipelineEvent.NodeRelabeled relabeled) {
            view(relabeled.node()).ifPresent(view -> view.showLabel(relabeled.label()));
        } else if (event instanceof PipelineEvent.NodeResolutionChanged changed) {
            refreshNode(changed.node());
        } else if (event instanceof PipelineEvent.LinkAdded added) {
            // the ports first: a link may use a flow that its node does not declare
            refreshEnds(added.link());
            addLinkView(added.link());
        } else if (event instanceof PipelineEvent.LinkRemoved removed) {
            removeLinkView(removed.link());
            refreshEnds(removed.link());
        }
        surface.requestLayout();
        restyle();
    }

    private void rebuild(final Pipeline pipeline) {
        links.values().forEach(LinkView::dispose);
        links.clear();
        nodes.clear();
        linkLayer.getChildren().clear();
        nodeLayer.getChildren().clear();
        pipeline.nodes().forEach(this::addNodeView);
        pipeline.links().forEach(this::addLinkView);
        scroll.setHvalue(0);
        scroll.setVvalue(0);
        restyle();
    }

    private void addNodeView(final Node node) {
        final NodeView view = new NodeView(model, node.id(), this::startLink);
        view.showNode(node, editor.resolution(node.id()));
        view.moveTo(node.position());
        nodes.put(node.id(), view);
        nodeLayer.getChildren().add(view);
        bindIcon(node.id());
        refreshPorts(node.id());
    }

    private void removeNodeView(final NodeId id) {
        final NodeView view = nodes.remove(id);
        if (view != null) {
            nodeLayer.getChildren().remove(view);
        }
    }

    private void addLinkView(final Link link) {
        final Optional<PortView> from = port(link.from(), Port.Direction.OUTPUT);
        final Optional<PortView> to = port(link.to(), Port.Direction.INPUT);
        if (from.isEmpty() || to.isEmpty()) {
            LOGGER.log(Logger.Level.WARNING, "No ports to draw the link " + link);
            return;
        }
        if (!links.containsKey(link)) {
            final LinkView view = new LinkView(model, link, from.get(), to.get(), surface);
            links.put(link, view);
            linkLayer.getChildren().add(view);
        }
    }

    private void removeLinkView(final Link link) {
        final LinkView view = links.remove(link);
        if (view != null) {
            view.dispose();
            linkLayer.getChildren().remove(view);
        }
    }

    private void refreshNode(final NodeId id) {
        final Optional<Node> node = editor.node(id);
        final NodeView view = nodes.get(id);
        if (node.isPresent() && view != null) {
            view.showNode(node.get(), editor.resolution(id));
            refreshPorts(id);
        }
    }

    private void refreshEnds(final Link link) {
        refreshPorts(link.from().node());
        refreshPorts(link.to().node());
    }

    private void refreshPorts(final NodeId id) {
        final NodeView view = nodes.get(id);
        if (view != null) {
            view.showPorts(model.ports(id), editor.resolution(id) instanceof Resolution.Resolved,
                    editor.pipeline().links());
        }
    }

    private void bindIcon(final NodeId id) {
        final NodeView view = nodes.get(id);
        final Optional<Node> node = editor.node(id);
        if (view != null && node.isPresent()) {
            view.bindIcon(icons.component(node.get().connector().id()));
        }
    }

    private void restyle() {
        final Selection current = model.selection().get();
        nodes.forEach((id, view) -> view.setSelected(current.selects(id)));
        links.forEach((link, view) -> view.refresh(current.selects(link)));
    }

    private Optional<NodeView> view(final NodeId id) {
        return Optional.ofNullable(nodes.get(id));
    }

    private Optional<PortView> port(final Endpoint endpoint, final Port.Direction direction) {
        return view(endpoint.node()).flatMap(view -> view.port(direction, endpoint.flow()));
    }

    // ---- drawing a link

    private void startLink(final PortView port, final MouseEvent event) {
        pointer = surface.sceneToLocal(event.getSceneX(), event.getSceneY());
        model.beginLink(port.endpoint());
    }

    private void onLinkSourceChanged(final Endpoint source) {
        hovered = null;
        for (final NodeView view : nodes.values()) {
            for (final PortView port : view.allPorts()) {
                final boolean target = source != null && port.port().direction() == Port.Direction.INPUT;
                port.setMode(target ? modeOf(port, false) : PortView.Mode.IDLE);
            }
        }
        updateGhost();
    }

    // an input a link could end on, or one it could not
    private PortView.Mode modeOf(final PortView port, final boolean pointed) {
        final boolean accepted = model.check(port.endpoint()).isEmpty();
        if (pointed) {
            return accepted ? PortView.Mode.HOT : PortView.Mode.REFUSED;
        }
        return accepted ? PortView.Mode.COMPATIBLE : PortView.Mode.DIMMED;
    }

    private void track(final PortView over) {
        if (over == hovered) {
            return;
        }
        if (hovered != null) {
            hovered.setMode(modeOf(hovered, false));
        }
        hovered = over;
        if (over != null) {
            over.setMode(modeOf(over, true));
        }
        model.hover(over == null ? null : over.endpoint());
    }

    private void updateGhost() {
        final Endpoint source = model.linkSource().get();
        final Optional<PortView> from = source == null ? Optional.empty() : port(source, Port.Direction.OUTPUT);
        if (from.isEmpty() || pointer == null) {
            ghost.setVisible(false);
            return;
        }
        // the end sticks to the input the link would be made with
        final Point2D end =
                hovered != null && hovered.mode() == PortView.Mode.HOT ? hovered.anchorIn(surface) : pointer;
        LinkView.shape(ghost, from.get().anchorIn(surface), end);
        ghost.setVisible(true);
    }

    // the topmost node under the point decides: a node hidden by another one has no port to drop a link on
    private PortView inputPortAt(final double sceneX, final double sceneY) {
        final List<javafx.scene.Node> children = nodeLayer.getChildren();
        for (int i = children.size() - 1; i >= 0; i--) {
            if (children.get(i) instanceof NodeView view && view.hits(sceneX, sceneY)) {
                for (final PortView port : view.inputPorts()) {
                    if (port.hits(sceneX, sceneY)) {
                        return port;
                    }
                }
                return null;
            }
        }
        return null;
    }

    /**
     * The area the nodes and the links are drawn on. It is as big as the nodes need, and never smaller than what is
     * visible, so that the grid fills the window and every click lands on it.
     */
    private final class Surface extends Pane {

        @Override
        protected double computePrefWidth(final double height) {
            double extent = 0;
            for (final NodeView view : nodes.values()) {
                extent = Math.max(extent, view.getLayoutX() + view.prefWidth(-1));
            }
            return Math.max(extent + SURFACE_MARGIN, viewportWidth / model.zoom().get());
        }

        @Override
        protected double computePrefHeight(final double width) {
            double extent = 0;
            for (final NodeView view : nodes.values()) {
                extent = Math.max(extent, view.getLayoutY() + view.prefHeight(-1));
            }
            return Math.max(extent + SURFACE_MARGIN, viewportHeight / model.zoom().get());
        }
    }
}
