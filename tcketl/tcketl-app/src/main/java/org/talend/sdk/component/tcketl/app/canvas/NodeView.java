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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import org.talend.sdk.component.tcketl.app.icon.IconService;
import org.talend.sdk.component.tcketl.app.ui.RoleGlyph;
import org.talend.sdk.component.tcketl.domain.connector.ConnectorDefinition;
import org.talend.sdk.component.tcketl.domain.connector.Port;
import org.talend.sdk.component.tcketl.domain.pipeline.Link;
import org.talend.sdk.component.tcketl.domain.pipeline.Node;
import org.talend.sdk.component.tcketl.domain.pipeline.NodeId;
import org.talend.sdk.component.tcketl.domain.pipeline.Position;
import org.talend.sdk.component.tcketl.domain.pipeline.Resolution;

/**
 * A node on the canvas: a card with the icon, the role and the name of the connector on top, and its input ports on
 * the left and its output ports on the right, straddling the edge of the card. The card is moved by dragging it,
 * renamed by a double click on its header and deleted from its context menu. It only shows the state of the pipeline
 * and tells the view model what the user does: the changes come back through the events of the editor.
 */
final class NodeView extends VBox {

    private static final double MAX_TITLE_WIDTH = 190;

    private static final double MIN_RENAME_WIDTH = 120;

    // keeps the inputs and the outputs apart whatever the width of the card
    private static final double MIN_PORT_GAP = 24;

    private static final double PORT_SPACING = 2;

    private static final double MAX_TOOLTIP_WIDTH = 360;

    private final CanvasViewModel model;

    private final NodeId id;

    private final BiConsumer<PortView, MouseEvent> onLinkStart;

    private final ImageView icon = new ImageView();

    private final RoleGlyph glyph = new RoleGlyph();

    private final Label title = new Label();

    private final Label streaming = new Label("\u221E");

    private final ProgressIndicator progress = new ProgressIndicator();

    private final Label warning = new Label("\u26A0");

    private final Label status = new Label();

    private final HBox header = new HBox();

    private final VBox inputs = new VBox(PORT_SPACING);

    private final VBox outputs = new VBox(PORT_SPACING);

    private final Tooltip tooltip = new Tooltip();

    private final Map<Port, PortView> ports = new HashMap<>();

    private final List<PortView> inputViews = new ArrayList<>();

    private String label = "";

    private boolean selected;

    private boolean unresolved;

    private TextField renamer;

    private boolean pressed;

    private boolean dragged;

    private double grabX;

    private double grabY;

    /**
     * @param onLinkStart told when the user presses an output port, to start drawing a link from it
     */
    NodeView(final CanvasViewModel model, final NodeId id, final BiConsumer<PortView, MouseEvent> onLinkStart) {
        this.model = model;
        this.id = id;
        this.onLinkStart = onLinkStart;

        icon.setFitWidth(IconService.DISPLAY_SIZE);
        icon.setFitHeight(IconService.DISPLAY_SIZE);
        icon.setPreserveRatio(true);
        icon.setSmooth(true);
        title.getStyleClass().add("node-title");
        title.setMaxWidth(MAX_TITLE_WIDTH);
        HBox.setHgrow(title, Priority.ALWAYS);
        streaming.getStyleClass().add("streaming-badge");
        progress.setPrefSize(14, 14);
        progress.setMaxSize(14, 14);
        warning.getStyleClass().add("node-warning");
        header.getStyleClass().add("node-header");
        header.setAlignment(Pos.CENTER_LEFT);
        Tooltip.install(header, tooltip);
        tooltip.setWrapText(true);
        tooltip.setMaxWidth(MAX_TOOLTIP_WIDTH);

        status.getStyleClass().add("node-status");
        status.setMaxWidth(MAX_TITLE_WIDTH);

        inputs.setAlignment(Pos.TOP_LEFT);
        outputs.setAlignment(Pos.TOP_RIGHT);
        final Region gap = new Region();
        gap.setMinWidth(MIN_PORT_GAP);
        HBox.setHgrow(gap, Priority.ALWAYS);
        final HBox body = new HBox(inputs, gap, outputs);
        body.getStyleClass().add("node-body");

        getChildren().addAll(header, status, body);
        // the drop shadow is part of the bounds of the card, but a click in it is a click on the canvas
        setPickOnBounds(false);
        restyle();

        setOnMousePressed(this::press);
        setOnMouseDragged(this::drag);
        setOnMouseReleased(this::release);
        header.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2 && event.isStillSincePress()) {
                beginRename();
                event.consume();
            }
        });
        setOnContextMenuRequested(event -> {
            model.select(id);
            final MenuItem rename = new MenuItem("Rename");
            rename.setOnAction(action -> beginRename());
            final MenuItem delete = new MenuItem("Delete");
            delete.setOnAction(action -> model.deleteNode(id));
            new ContextMenu(rename, delete).show(this, event.getScreenX(), event.getScreenY());
            event.consume();
        });
    }

    NodeId id() {
        return id;
    }

    /** What the node shows of its connector depends on how far the loading of the definition went. */
    void showNode(final Node node, final Resolution resolution) {
        showLabel(node.label());
        unresolved = resolution instanceof Resolution.Unresolved;
        final List<javafx.scene.Node> content = new ArrayList<>();
        content.add(icon);
        if (resolution instanceof Resolution.Resolved resolved) {
            final ConnectorDefinition definition = resolved.definition();
            glyph.show(definition.role(), false);
            content.add(glyph);
            content.add(titleSlot());
            if (definition.streaming()) {
                content.add(streaming);
            }
            showStatus("");
        } else if (unresolved) {
            content.add(titleSlot());
            content.add(warning);
            showStatus("Unavailable");
        } else {
            content.add(titleSlot());
            content.add(progress);
            showStatus("Loading\u2026");
        }
        header.getChildren().setAll(content);
        tooltip.setText(describe(resolution));
        restyle();
    }

    void showLabel(final String text) {
        label = text;
        title.setText(text);
    }

    void moveTo(final Position position) {
        setLayoutX(position.x());
        setLayoutY(position.y());
    }

    void bindIcon(final ReadOnlyObjectProperty<Image> image) {
        icon.imageProperty().unbind();
        icon.imageProperty().bind(image);
    }

    void setSelected(final boolean value) {
        if (selected != value) {
            selected = value;
            restyle();
        }
    }

    /**
     * Shows the ports and keeps the views of those that stay, so that the links attached to them keep following.
     *
     * @param resolved true when the connector is loaded: a flow it does not declare is then a defect to point out
     * @param links the links of the pipeline, to tell which ports are connected
     */
    void showPorts(final NodePorts wanted, final boolean resolved, final Collection<Link> links) {
        final Set<String> linkedInputs = new HashSet<>();
        final Set<String> linkedOutputs = new HashSet<>();
        for (final Link link : links) {
            if (link.to().node().equals(id)) {
                linkedInputs.add(link.to().flow());
            }
            if (link.from().node().equals(id)) {
                linkedOutputs.add(link.from().flow());
            }
        }
        final List<PortView> wantedInputs = views(wanted.inputs(), resolved, linkedInputs);
        final List<PortView> wantedOutputs = views(wanted.outputs(), resolved, linkedOutputs);
        inputs.getChildren().setAll(wantedInputs);
        outputs.getChildren().setAll(wantedOutputs);
        inputViews.clear();
        inputViews.addAll(wantedInputs);
        ports.values().removeIf(view -> !wantedInputs.contains(view) && !wantedOutputs.contains(view));
    }

    Optional<PortView> port(final Port.Direction direction, final String flow) {
        return Optional.ofNullable(ports.get(new Port(flow, direction)));
    }

    /** The input ports, from the top. */
    List<PortView> inputPorts() {
        return List.copyOf(inputViews);
    }

    /** All the ports, for the view that restyles them while a link is drawn. */
    Collection<PortView> allPorts() {
        return List.copyOf(ports.values());
    }

    /** True when the point, in scene coordinates, is on the card or on the handle of one of its ports. */
    boolean hits(final double sceneX, final double sceneY) {
        return localToScene(getBoundsInLocal()).contains(sceneX, sceneY);
    }

    /** Edits the name in place: the enter key or leaving the field confirms, the escape key gives up. */
    void beginRename() {
        if (renamer != null) {
            return;
        }
        final TextField field = new TextField(label);
        field.getStyleClass().add("node-rename");
        field.setPrefWidth(Math.max(MIN_RENAME_WIDTH, title.getWidth()));
        field.setOnAction(event -> finishRename(true, true));
        field.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                finishRename(false, true);
                event.consume();
            }
        });
        field.focusedProperty().addListener((observed, was, focused) -> {
            if (!focused) {
                // the focus went where the user clicked: it is not for the canvas to take it back
                finishRename(true, false);
            }
        });
        final int at = header.getChildren().indexOf(title);
        if (at < 0) {
            return;
        }
        renamer = field;
        header.getChildren().set(at, field);
        field.requestFocus();
        field.selectAll();
    }

    private List<PortView> views(final List<NodePorts.Entry> entries, final boolean resolved,
            final Set<String> linked) {
        final List<PortView> views = new ArrayList<>();
        for (final NodePorts.Entry entry : entries) {
            final PortView view = ports.computeIfAbsent(entry.port(), port -> new PortView(id, port, onLinkStart));
            view.show(entry.declared(), resolved, linked.contains(entry.port().flow()));
            views.add(view);
        }
        return views;
    }

    private void press(final MouseEvent event) {
        if (event.getButton() != MouseButton.PRIMARY || renamer != null || getParent() == null) {
            return;
        }
        model.select(id);
        // the layer of the nodes has no transform of its own, so these are the coordinates of the canvas
        final Point2D pointer = getParent().sceneToLocal(event.getSceneX(), event.getSceneY());
        grabX = pointer.getX() - getLayoutX();
        grabY = pointer.getY() - getLayoutY();
        pressed = true;
        dragged = false;
        // consumed, so that the scroll pane does not pan while a node is moved
        event.consume();
    }

    private void drag(final MouseEvent event) {
        if (!pressed || !event.isPrimaryButtonDown() || getParent() == null) {
            return;
        }
        final Point2D pointer = getParent().sceneToLocal(event.getSceneX(), event.getSceneY());
        moveTo(CanvasViewModel.snap(new Position(pointer.getX() - grabX, pointer.getY() - grabY)));
        dragged = true;
        event.consume();
    }

    private void release(final MouseEvent event) {
        if (!pressed) {
            return;
        }
        pressed = false;
        if (dragged) {
            dragged = false;
            model.moveNode(id, new Position(getLayoutX(), getLayoutY()));
            event.consume();
        }
    }

    private void finishRename(final boolean commit, final boolean giveFocusBack) {
        final TextField field = renamer;
        if (field == null) {
            return;
        }
        renamer = null;
        final int at = header.getChildren().indexOf(field);
        if (at >= 0) {
            header.getChildren().set(at, title);
        }
        final String text = field.getText().strip();
        if (commit && !text.isEmpty() && !text.equals(label)) {
            model.renameNode(id, text);
        }
        if (giveFocusBack) {
            focusCanvas();
        }
    }

    // the keys of the canvas (delete, escape) only work while it has the focus
    private void focusCanvas() {
        for (javafx.scene.Node ancestor = getParent(); ancestor != null; ancestor = ancestor.getParent()) {
            if (ancestor.isFocusTraversable()) {
                ancestor.requestFocus();
                return;
            }
        }
    }

    private javafx.scene.Node titleSlot() {
        return renamer != null ? renamer : title;
    }

    private void showStatus(final String text) {
        status.setText(text);
        status.setVisible(!text.isEmpty());
        status.setManaged(!text.isEmpty());
    }

    private String describe(final Resolution resolution) {
        if (resolution instanceof Resolution.Resolved resolved) {
            final ConnectorDefinition definition = resolved.definition();
            final StringBuilder text = new StringBuilder(definition.displayName())
                    .append('\n')
                    .append(RoleGlyph.label(definition.role()))
                    .append(definition.streaming() ? ", streaming" : "")
                    .append('\n')
                    .append(definition.ref().family())
                    .append(", version ")
                    .append(definition.ref().version());
            if (!definition.documentation().isBlank()) {
                text.append("\n\n").append(definition.documentation().strip());
            }
            return text.toString();
        }
        if (resolution instanceof Resolution.Unresolved failed) {
            return failed.reason().isBlank() ? "The definition of this connector is not available" : failed.reason();
        }
        return "Loading the definition of the connector\u2026";
    }

    private void restyle() {
        final List<String> styles = new ArrayList<>();
        styles.add("node-card");
        if (selected) {
            styles.add("selected");
        }
        if (unresolved) {
            styles.add("unresolved");
        }
        getStyleClass().setAll(styles);
    }
}
