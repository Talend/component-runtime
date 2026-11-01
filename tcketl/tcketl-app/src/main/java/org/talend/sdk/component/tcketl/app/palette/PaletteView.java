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
package org.talend.sdk.component.tcketl.app.palette;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import org.talend.sdk.component.tcketl.app.icon.IconService;
import org.talend.sdk.component.tcketl.app.ui.RoleGlyph;
import org.talend.sdk.component.tcketl.domain.connector.ConnectorRole;
import org.talend.sdk.component.tcketl.domain.connector.ConnectorSummary;

/**
 * The connectors of the server, grouped by family, with a search field above them. Each entry shows its icon, a
 * glyph for its role and, for an input that never ends on its own, a streaming badge; the tooltip tells the role and
 * quotes the documentation.
 */
public final class PaletteView extends VBox {

    private static final String FAMILY_STYLE = "family-name";

    private static final int TOOLTIP_WIDTH = 360;

    private static final Color DRAG_FILL = Color.rgb(255, 255, 255, 0.9);

    private final PaletteViewModel model;

    private final IconService icons;

    private final TextField search = new TextField();

    private final TreeView<Object> tree = new TreeView<>();

    // families the user folded, remembered while the search text is blank only: searching shows every match
    private final Set<String> collapsed = new HashSet<>();

    private Consumer<ConnectorSummary> onActivate = connector -> {
    };

    public PaletteView(final PaletteViewModel model, final IconService icons) {
        this.model = model;
        this.icons = icons;
        getStyleClass().add("palette");

        search.setPromptText("Search components");
        search.textProperty().bindBidirectional(model.query());
        search.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                search.clear();
                event.consume();
            }
        });
        final Button refresh = new Button("\u21BB");
        refresh.setTooltip(new Tooltip("Refresh the components (F5)"));
        refresh.setOnAction(event -> model.refresh());
        final HBox bar = new HBox(6, search, refresh);
        bar.getStyleClass().add("palette-bar");
        HBox.setHgrow(search, Priority.ALWAYS);

        tree.setShowRoot(false);
        tree.setCellFactory(view -> new PaletteCell());
        final Label placeholder = new Label();
        placeholder.getStyleClass().add("palette-placeholder");
        placeholder.setWrapText(true);
        placeholder.setMouseTransparent(true);
        placeholder.textProperty().bind(model.placeholderText());
        placeholder.visibleProperty().bind(placeholder.textProperty().isNotEmpty());
        final StackPane body = new StackPane(tree, placeholder);
        VBox.setVgrow(body, Priority.ALWAYS);

        final Label count = new Label();
        count.getStyleClass().add("palette-count");
        count.textProperty().bind(model.countText());

        getChildren().addAll(bar, body, count);
        model.visible().addListener((observed, before, after) -> rebuild());
        model.roles().addListener((observed, before, after) -> tree.refresh());
        rebuild();
    }

    /** Puts the cursor in the search field, with the previous search selected. */
    public void focusSearch() {
        search.requestFocus();
        search.selectAll();
    }

    /** Told when a connector is double-clicked, which is the quick way to add it to the pipeline. */
    public void setOnActivate(final Consumer<ConnectorSummary> onActivate) {
        this.onActivate = Objects.requireNonNull(onActivate, "onActivate");
    }

    private void rebuild() {
        final boolean searching = !model.query().get().isBlank();
        final TreeItem<Object> root = new TreeItem<>();
        for (final PaletteFamily family : model.visible().get().families()) {
            final TreeItem<Object> item = new TreeItem<>(family);
            item.setExpanded(searching || !collapsed.contains(family.id()));
            item.expandedProperty().addListener((observed, was, expanded) -> remember(family, expanded));
            family.connectors().forEach(connector -> item.getChildren().add(new TreeItem<>(connector)));
            root.getChildren().add(item);
        }
        tree.setRoot(root);
    }

    private void remember(final PaletteFamily family, final boolean expanded) {
        if (!model.query().get().isBlank()) {
            return;
        }
        if (expanded) {
            collapsed.remove(family.id());
        } else {
            collapsed.add(family.id());
        }
    }

    private static void style(final Node node, final String styleClass, final boolean on) {
        node.getStyleClass().remove(styleClass);
        if (on) {
            node.getStyleClass().add(styleClass);
        }
    }

    private final class PaletteCell extends TreeCell<Object> {

        private final ImageView icon = new ImageView();

        private final RoleGlyph glyph = new RoleGlyph();

        private final Label name = new Label();

        private final Label streaming = new Label("\u221E");

        private final HBox row = new HBox(6, icon, glyph, name, streaming);

        private final Tooltip tooltip = new Tooltip();

        private PaletteCell() {
            icon.setFitWidth(IconService.DISPLAY_SIZE);
            icon.setFitHeight(IconService.DISPLAY_SIZE);
            icon.setPreserveRatio(true);
            icon.setSmooth(true);
            streaming.getStyleClass().add("streaming-badge");
            row.setAlignment(Pos.CENTER_LEFT);
            tooltip.setWrapText(true);
            tooltip.setMaxWidth(TOOLTIP_WIDTH);
            setOnDragDetected(this::dragConnector);
            setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2
                        && getItem() instanceof ConnectorSummary connector) {
                    onActivate.accept(connector);
                    event.consume();
                }
            });
        }

        private void dragConnector(final MouseEvent event) {
            if (isEmpty() || !(getItem() instanceof ConnectorSummary connector)) {
                return;
            }
            final Dragboard board = startDragAndDrop(TransferMode.COPY);
            final ClipboardContent content = new ClipboardContent();
            content.put(PaletteTransfer.CONNECTOR, connector.id().value());
            board.setContent(content);
            final SnapshotParameters parameters = new SnapshotParameters();
            parameters.setFill(DRAG_FILL);
            board.setDragView(row.snapshot(parameters, null));
            event.consume();
        }

        @Override
        protected void updateItem(final Object item, final boolean empty) {
            super.updateItem(item, empty);
            icon.imageProperty().unbind();
            if (empty || item == null) {
                setGraphic(null);
                setTooltip(null);
                return;
            }
            if (item instanceof PaletteFamily) {
                showFamily((PaletteFamily) item);
            } else if (item instanceof ConnectorSummary) {
                showConnector((ConnectorSummary) item);
            }
            setGraphic(row);
        }

        private void showFamily(final PaletteFamily family) {
            icon.imageProperty().bind(icons.family(family.id()));
            name.setText(family.displayName());
            style(name, FAMILY_STYLE, true);
            setVisibleAndManaged(glyph, false);
            setVisibleAndManaged(streaming, false);
            setTooltip(null);
        }

        private void showConnector(final ConnectorSummary connector) {
            final ConnectorRole role = model.roleOf(connector);
            icon.imageProperty().bind(icons.component(connector.id()));
            glyph.show(role, !model.isResolved(connector));
            name.setText(connector.displayName());
            style(name, FAMILY_STYLE, false);
            setVisibleAndManaged(glyph, true);
            setVisibleAndManaged(streaming, connector.streaming());
            tooltip.setText(describe(connector, role));
            setTooltip(tooltip);
        }

        private String describe(final ConnectorSummary connector, final ConnectorRole role) {
            final String kind = RoleGlyph.label(role) + (connector.streaming() ? ", streaming" : "");
            return connector.documentation().isBlank() ? kind : kind + "\n\n" + connector.documentation();
        }

        private void setVisibleAndManaged(final Node node, final boolean shown) {
            node.setVisible(shown);
            node.setManaged(shown);
        }
    }
}
