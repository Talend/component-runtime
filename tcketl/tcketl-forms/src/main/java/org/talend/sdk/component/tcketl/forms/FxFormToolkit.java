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
package org.talend.sdk.component.tcketl.forms;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntFunction;

import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import org.talend.sdk.component.tcketl.forms.Editors.Editor;

/** The JavaFX widgets of a form. */
final class FxFormToolkit implements FormToolkit<Node> {

    private static final double ROW_SPACING = 10;

    private static final double COLUMN_GAP = 12;

    private static final double PERCENT = 100;

    private final FormViewModel model;

    private final ActionService service;

    FxFormToolkit(final FormViewModel model, final ActionService service) {
        this.model = model;
        this.service = service;
    }

    @Override
    public Node empty(final String text) {
        final Label empty = new Label(text);
        empty.getStyleClass().add("form-empty");
        return new VBox(empty);
    }

    @Override
    public Node tabs(final List<String> names, final IntFunction<Node> content) {
        final TabPane pane = new TabPane();
        pane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        for (int i = 0; i < names.size(); i++) {
            final int index = i;
            final Tab tab = new Tab(names.get(i));
            pane.getTabs().add(tab);
            final Runnable fill = () -> {
                if (tab.getContent() == null) {
                    tab.setContent(content.apply(index));
                }
            };
            tab.selectedProperty().addListener((value, before, after) -> {
                if (after) {
                    fill.run();
                }
            });
            if (i == 0) {
                fill.run();
            }
        }
        return pane;
    }

    @Override
    public Node scroll(final Node content) {
        content.getStyleClass().add("form-rows");
        final ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        return scroll;
    }

    @Override
    public Node rows(final List<List<Node>> rows) {
        final VBox box = new VBox(ROW_SPACING);
        for (final List<Node> cells : rows) {
            box.getChildren().add(cells.size() == 1 ? cells.get(0) : grid(cells));
        }
        return box;
    }

    private static GridPane grid(final List<Node> cells) {
        final GridPane grid = new GridPane();
        grid.setHgap(COLUMN_GAP);
        for (int i = 0; i < cells.size(); i++) {
            final ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(PERCENT / cells.size());
            column.setHgrow(Priority.ALWAYS);
            column.setMinWidth(0);
            grid.getColumnConstraints().add(column);
            final Node cell = cells.get(i);
            GridPane.setValignment(cell, VPos.TOP);
            grid.add(cell, i, 0);
        }
        return grid;
    }

    @Override
    public void visible(final Node node, final boolean visible) {
        node.setVisible(visible);
        node.setManaged(visible);
    }

    @Override
    public Node group(final String title, final Node content) {
        if (title == null) {
            return new VBox(ROW_SPACING, content);
        }
        final TitledPane pane = new TitledPane(title, new VBox(ROW_SPACING, content));
        pane.setAnimated(false);
        return pane;
    }

    @Override
    public Node field(final FieldSpec spec, final WidgetKind kind, final boolean compact,
            final FormBindings.Scope scope, final Runnable onBlur) {
        final Editor editor = Editors.create(kind, spec, model, scope, service);
        editor.focusOwner().focusedProperty().addListener((value, before, after) -> {
            if (!after) {
                onBlur.run();
            }
        });
        return compact ? Fields.withMessage(editor, spec, model, scope) : Fields.full(editor, spec, model, scope);
    }

    @Override
    public Node button(final String text, final boolean visible, final String disabledReason,
            final Consumer<ButtonResult> onClick) {
        final Button button = new Button(text);
        button.getStyleClass().add("form-action");
        button.setMaxWidth(Region.USE_PREF_SIZE);
        final Label result = new Label();
        result.setWrapText(true);
        result.setMinHeight(Label.USE_PREF_SIZE);
        final HBox box = new HBox(8, button, result);
        box.setAlignment(Pos.CENTER_LEFT);
        if (!visible) {
            visible(box, false);
        }
        if (disabledReason != null) {
            button.setDisable(true);
            button.setTooltip(new Tooltip(disabledReason));
            return box;
        }
        final ButtonResult handle = feedback -> {
            button.setDisable(feedback.kind() == Feedback.Kind.RUNNING);
            result.getStyleClass().removeAll("form-ok", "form-ko");
            result.setText(feedback.text());
            if (feedback.kind() == Feedback.Kind.OK) {
                result.getStyleClass().add("form-ok");
            } else if (feedback.kind() == Feedback.Kind.KO) {
                result.getStyleClass().add("form-ko");
            }
        };
        button.setOnAction(event -> onClick.accept(handle));
        return box;
    }

    @Override
    public ListEditor<Node> collection(final String title, final String documentation, final boolean readOnly,
            final Runnable onAdd) {
        final Label name = new Label(title);
        name.getStyleClass().add("form-title");
        Fields.documentation(name, documentation);
        final VBox entries = new VBox(6);
        final Button add = new Button("Add");
        add.setDisable(readOnly);
        add.setOnAction(event -> onAdd.run());
        final Label message = new Label();
        message.getStyleClass().add("form-error");
        message.setWrapText(true);
        final VBox box = new VBox(4, name, entries, add, message);
        return new ListEditor<>() {

            @Override
            public Node node() {
                return box;
            }

            @Override
            public void message(final String textOrNull) {
                message.setText(textOrNull == null ? "" : textOrNull);
                visible(message, textOrNull != null);
            }

            @Override
            public void entries(final List<Entry<Node>> values) {
                final List<Node> children = new ArrayList<>();
                values.forEach(entry -> children.add(wrap(entry)));
                entries.getChildren().setAll(children);
            }
        };
    }

    private static Node wrap(final FormToolkit.Entry<Node> entry) {
        final Button up = iconButton("icon-up", "Move up", !entry.canUp(), entry.up());
        final Button down = iconButton("icon-down", "Move down", !entry.canDown(), entry.down());
        final Button remove = iconButton("icon-remove", "Remove", !entry.canRemove(), entry.remove());
        final Node body = entry.body();
        if (entry.header() != null) {
            final Label name = new Label(entry.header());
            name.getStyleClass().add("form-title");
            final Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            final HBox header = new HBox(4, name, spacer, up, down, remove);
            header.getStyleClass().add("form-entry-header");
            final VBox box = new VBox(6, header, body);
            box.getStyleClass().add("form-entry");
            return box;
        }
        final HBox box = entry.reorderable() ? new HBox(4, body, up, down, remove) : new HBox(4, body, remove);
        HBox.setHgrow(body, Priority.ALWAYS);
        box.setAlignment(Pos.TOP_LEFT);
        return box;
    }

    private static Button iconButton(final String icon, final String label, final boolean disabled,
            final Runnable action) {
        final Region graphic = new Region();
        graphic.getStyleClass().addAll("icon", icon);
        final Button button = new Button();
        button.setGraphic(graphic);
        button.getStyleClass().add("icon-button");
        button.setTooltip(new Tooltip(label));
        button.setAccessibleText(label);
        button.setDisable(disabled);
        button.setOnAction(event -> action.run());
        return button;
    }
}
