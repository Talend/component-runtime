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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

import org.talend.sdk.component.tcketl.domain.action.ActionOutcome;
import org.talend.sdk.component.tcketl.domain.form.FormLayout;
import org.talend.sdk.component.tcketl.domain.form.FormLayout.ButtonCell;
import org.talend.sdk.component.tcketl.domain.form.FormLayout.Cell;
import org.talend.sdk.component.tcketl.domain.form.FormLayout.CollectionCell;
import org.talend.sdk.component.tcketl.domain.form.FormLayout.FieldCell;
import org.talend.sdk.component.tcketl.domain.form.FormLayout.GroupCell;
import org.talend.sdk.component.tcketl.domain.form.FormLayout.Row;
import org.talend.sdk.component.tcketl.domain.form.PropertyKind;
import org.talend.sdk.component.tcketl.domain.form.PropertyNode;
import org.talend.sdk.component.tcketl.domain.property.ActionBinding;
import org.talend.sdk.component.tcketl.domain.property.ActionTypes;
import org.talend.sdk.component.tcketl.forms.Editors.Editor;
import org.talend.sdk.component.tcketl.forms.Editors.Spec;

/** Builds the controls of a form from its layout. */
final class FormBuilder {

    private static final double ROW_SPACING = 10;

    private static final double COLUMN_GAP = 12;

    private static final double PERCENT = 100;

    private final FormViewModel model;

    private final FormBindings bindings;

    private final ActionService service;

    FormBuilder(final FormViewModel model, final FormBindings bindings, final ActionService service) {
        this.model = model;
        this.bindings = bindings;
        this.service = service;
    }

    /**
     * Where a cell is built: the concrete path of the object around it, and the position of the entry when it is in a
     * list or a map.
     */
    private record Context(String parent, int index, boolean compact) {

        static final Context ROOT = new Context(null, -1, false);

        Context inside(final String parentPath) {
            return new Context(parentPath, index, false);
        }
    }

    Region build() {
        final List<FormLayout.Tab> tabs = model.layout().tabs();
        if (tabs.isEmpty() || tabs.stream().allMatch(tab -> tab.rows().isEmpty())) {
            final Label empty = new Label("Nothing to configure");
            empty.getStyleClass().add("form-empty");
            return new VBox(empty);
        }
        if (!model.layout().isTabbed()) {
            return scroll(rows(tabs.get(0).rows(), Context.ROOT, bindings.root()));
        }
        final TabPane pane = new TabPane();
        pane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        for (final FormLayout.Tab layoutTab : tabs) {
            final Tab tab = new Tab(layoutTab.name());
            pane.getTabs().add(tab);
            final Runnable fill = () -> {
                if (tab.getContent() == null) {
                    tab.setContent(scroll(rows(layoutTab.rows(), Context.ROOT, bindings.root())));
                }
            };
            tab.selectedProperty().addListener((value, before, after) -> {
                if (after) {
                    fill.run();
                }
            });
            if (pane.getTabs().size() == 1) {
                fill.run();
            }
        }
        return pane;
    }

    private static ScrollPane scroll(final Node content) {
        content.getStyleClass().add("form-rows");
        final ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        return scroll;
    }

    private VBox rows(final List<Row> rows, final Context context, final FormBindings.Scope scope) {
        final VBox box = new VBox(ROW_SPACING);
        for (final Row row : rows) {
            final List<Node> cells = new ArrayList<>();
            for (final Cell cell : row.cells()) {
                cells.add(cell(cell, context, scope));
            }
            if (cells.size() == 1) {
                box.getChildren().add(cells.get(0));
            } else if (!cells.isEmpty()) {
                box.getChildren().add(grid(cells));
            }
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

    private Node cell(final Cell cell, final Context context, final FormBindings.Scope scope) {
        if (cell instanceof FieldCell field) {
            return conditional(field(field.node(), context, scope), pathOf(field.node(), context), scope);
        } else if (cell instanceof GroupCell group) {
            return conditional(group(group, context, scope), pathOf(group.node(), context), scope);
        } else if (cell instanceof CollectionCell collection) {
            return conditional(collection(collection, context, scope), pathOf(collection.node(), context), scope);
        } else if (cell instanceof ButtonCell button) {
            return button(button, context);
        }
        throw new IllegalArgumentException("Unknown cell " + cell);
    }

    private Node conditional(final Node node, final String path, final FormBindings.Scope scope) {
        scope.onRefresh(() -> {
            final boolean visible = model.isVisible(path);
            node.setVisible(visible);
            node.setManaged(visible);
        });
        return node;
    }

    /** A button is laid out inside its owner, so the context already points at the owner's concrete path. */
    private static String buttonPath(final ButtonCell cell, final Context context) {
        final PropertyNode owner = cell.owner();
        final PropertyNode target = cell.target();
        if (context.parent() == null || !target.path().startsWith(owner.path())) {
            return pathOf(target, context);
        }
        return context.parent() + target.path().substring(owner.path().length());
    }

    private static String pathOf(final PropertyNode node, final Context context) {
        final String parent = context.parent() != null ? context.parent()
                : node.parent().map(PropertyNode::path).orElse(null);
        return node.concretePath(parent, context.index());
    }

    private Node field(final PropertyNode node, final Context context, final FormBindings.Scope scope) {
        final String path = pathOf(node, context);
        final Spec spec = new Spec(node, path, context.index(), Editors.promptOf(node, context.index()));
        final Editor editor = Editors.create(scalarKind(node), spec, model, scope, service);
        validateRemotely(node, path, editor);
        return context.compact() ? Fields.withMessage(editor, spec, model, scope)
                : Fields.full(editor, spec, model, scope);
    }

    /** An asynchronous validation runs when the user leaves the field; a KO shows the comment until the next edit. */
    private void validateRemotely(final PropertyNode node, final String path, final Editor editor) {
        final Optional<ActionBinding> binding = node.definition().action(ActionTypes.VALIDATION);
        if (binding.isEmpty() || !service.supports(binding.get())) {
            return;
        }
        editor.focusOwner().focusedProperty().addListener((value, before, after) -> {
            if (!after) {
                service
                        .request(binding.get(), path)
                        .ifPresent(request -> service.run(path + '#' + ActionTypes.VALIDATION, request,
                                outcome -> model.setRemoteMessage(path,
                                        outcome instanceof ActionOutcome.Checked checked && !checked.ok()
                                                ? (checked.comment().isEmpty() ? "Rejected by the server"
                                                        : checked.comment())
                                                : null)));
            }
        });
    }

    private static WidgetKind scalarKind(final PropertyNode node) {
        final WidgetKind kind = WidgetSelector.select(node);
        return switch (kind) {
            case GROUP, LIST_OF_OBJECTS, LIST_OF_VALUES, MAP -> WidgetKind.TEXT;
            default -> kind;
        };
    }

    private Node group(final GroupCell group, final Context context, final FormBindings.Scope scope) {
        final PropertyNode node = group.node();
        final String path = pathOf(node, context);
        final VBox content = new VBox(ROW_SPACING);
        final String documentation = Texts.numbered(node.definition().documentation(), context.index());
        if (!documentation.isEmpty()) {
            final Label help = new Label(documentation);
            help.getStyleClass().add("form-help");
            help.setWrapText(true);
            help.setMinHeight(Label.USE_PREF_SIZE);
            content.getChildren().add(help);
        }
        content.getChildren().add(rows(group.rows(), context.inside(path), scope));
        if (node.parent().isEmpty() || !node.definition().configurationType().isPresent()) {
            return content;
        }
        final TitledPane pane = new TitledPane(Texts.title(node, context.index()), content);
        pane.setAnimated(false);
        return pane;
    }

    private Node button(final ButtonCell cell, final Context context) {
        final String text = switch (cell.kind()) {
            case HEALTHCHECK -> "Check connection";
            case UPDATE -> "Update";
            case GUESS_SCHEMA -> "Guess schema";
        };
        final boolean described = service.supports(cell.binding());
        final Button button = new Button(text);
        button.getStyleClass().add("form-action");
        button.setMaxWidth(Region.USE_PREF_SIZE);
        final Label result = new Label();
        result.setWrapText(true);
        result.setMinHeight(Label.USE_PREF_SIZE);
        final HBox box = new HBox(8, button, result);
        box.setAlignment(Pos.CENTER_LEFT);
        if (!described) {
            if (cell.kind() == FormLayout.ButtonKind.GUESS_SCHEMA) {
                box.setVisible(false);
                box.setManaged(false);
            }
            button.setDisable(true);
            button.setTooltip(new Tooltip("The server has no such service for this connector"));
            return box;
        }
        final String path = buttonPath(cell, context);
        final String key = path + '#' + cell.binding().type();
        button.setOnAction(event -> service.request(cell.binding(), path).ifPresent(request -> {
            button.setDisable(true);
            result.getStyleClass().removeAll("form-ok", "form-ko");
            result.setText("Running\u2026");
            service.run(key, request, outcome -> {
                button.setDisable(false);
                applyOutcome(cell, path, outcome, result);
            });
        }));
        return box;
    }

    private void applyOutcome(final ButtonCell cell, final String path, final ActionOutcome outcome,
            final Label result) {
        result.getStyleClass().removeAll("form-ok", "form-ko");
        if (outcome instanceof ActionOutcome.Checked checked) {
            result.setText(checked.ok() ? "\u2713 " + (checked.comment().isEmpty() ? "OK" : checked.comment())
                    : "\u2717 " + (checked.comment().isEmpty() ? "Failed" : checked.comment()));
            result.getStyleClass().add(checked.ok() ? "form-ok" : "form-ko");
        } else if (outcome instanceof ActionOutcome.Replacement replacement) {
            final long known = replacement.values()
                    .keySet()
                    .stream()
                    .filter(suffix -> model.tree().find(path + suffix).isPresent())
                    .count();
            if (replacement.values().isEmpty()) {
                result.setText("\u2717 The service returned no value, nothing was changed");
                result.getStyleClass().add("form-ko");
            } else if (known == 0) {
                result.setText("\u2717 None of the returned values is a field of this form: "
                        + replacement.values().keySet().stream().limit(5).toList());
                result.getStyleClass().add("form-ko");
            } else {
                model.replaceSubtree(path, replacement.values());
                result.setText("Updated (" + known + " values)");
                result.getStyleClass().add("form-ok");
            }
        } else if (outcome instanceof ActionOutcome.Columns columns) {
            final Map<String, String> entries = new LinkedHashMap<>();
            for (int i = 0; i < columns.names().size(); i++) {
                entries.put("[" + i + "]", columns.names().get(i));
            }
            model.replaceSubtree(path, entries);
            result.setText(columns.names().size() + " columns");
            result.getStyleClass().add("form-ok");
        } else if (outcome instanceof ActionOutcome.Failed failed) {
            result.setText("\u2717 " + (failed.message().isEmpty() ? "The call failed" : failed.message()));
            result.getStyleClass().add("form-ko");
        } else {
            result.setText("");
        }
    }

    private Node collection(final CollectionCell cell, final Context context, final FormBindings.Scope scope) {
        final PropertyNode node = cell.node();
        final String path = pathOf(node, context);
        final boolean readOnly = node.definition().ui().readOnly();
        final PropertyKind kind = node.kind();

        final Label title = new Label(Texts.title(node, context.index()));
        title.getStyleClass().add("form-title");
        final VBox entries = new VBox(6);
        final Button add = new Button("Add");
        add.setDisable(readOnly);
        add.setOnAction(event -> model.append(path));
        final Label message = new Label();
        message.getStyleClass().add("form-error");
        message.setWrapText(true);
        final VBox box = new VBox(4, title);
        final String documentation = Texts.numbered(node.definition().documentation(), context.index());
        if (!documentation.isEmpty()) {
            final Label help = new Label(documentation);
            help.getStyleClass().add("form-help");
            help.setWrapText(true);
            help.setMinHeight(Label.USE_PREF_SIZE);
            box.getChildren().add(help);
        }
        box.getChildren().addAll(entries, add, message);

        final int[] shown = { -1 };
        final FormBindings.Scope[] current = { null };
        scope.onRefresh(() -> {
            final Optional<String> text = model.message(path);
            message.setText(text.orElse(""));
            message.setVisible(text.isPresent());
            message.setManaged(text.isPresent());
            final int count = model.count(path);
            if (count == shown[0]) {
                return;
            }
            shown[0] = count;
            if (current[0] != null) {
                current[0].close();
            }
            final FormBindings.Scope entryScope = scope.child();
            current[0] = entryScope;
            entries.getChildren().clear();
            final IntFunction<Node> factory = switch (kind) {
                case MAP -> index -> mapEntry(cell, path, index, entryScope);
                case ARRAY_OF_OBJECT -> index -> objectEntry(cell, path, index, entryScope);
                default -> index -> valueEntry(node, path, index, entryScope);
            };
            for (int i = 0; i < count; i++) {
                entries.getChildren().add(wrapEntry(kind, factory.apply(i), path, i, count, readOnly, node, context));
            }
        });
        return box;
    }

    private Node wrapEntry(final PropertyKind kind, final Node body, final String path, final int index,
            final int count, final boolean readOnly, final PropertyNode node, final Context context) {
        final Button up =
                iconButton("icon-up", "Move up", readOnly || index == 0, () -> model.move(path, index, index - 1));
        final Button down = iconButton("icon-down", "Move down", readOnly || index == count - 1,
                () -> model.move(path, index, index + 1));
        final Button remove = iconButton("icon-remove", "Remove", readOnly, () -> model.remove(path, index));
        if (kind == PropertyKind.ARRAY_OF_OBJECT) {
            final String display = node.definition().displayName();
            final Label name = new Label(display.contains("${index}") ? Texts.numbered(display, index)
                    : display + " #" + (index + 1));
            name.getStyleClass().add("form-title");
            final Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            final HBox header = new HBox(4, name, spacer, up, down, remove);
            header.getStyleClass().add("form-entry-header");
            final VBox entry = new VBox(6, header, body);
            entry.getStyleClass().add("form-entry");
            return entry;
        }
        final HBox entry = kind == PropertyKind.MAP ? new HBox(4, body, remove) : new HBox(4, body, up, down, remove);
        HBox.setHgrow(body, Priority.ALWAYS);
        entry.setAlignment(Pos.TOP_LEFT);
        return entry;
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

    private Node valueEntry(final PropertyNode list, final String path, final int index,
            final FormBindings.Scope scope) {
        final Optional<PropertyNode> element = list.element();
        if (element.isEmpty()) {
            return new Label("?");
        }
        final PropertyNode node = element.get();
        final String elementPath = node.concretePath(path, index);
        final Spec spec = new Spec(node, elementPath, index, Editors.promptOf(node, index));
        return Fields.withMessage(Editors.create(scalarKind(node), spec, model, scope, service), spec, model, scope);
    }

    private Node objectEntry(final CollectionCell cell, final String path, final int index,
            final FormBindings.Scope scope) {
        final PropertyNode element = cell.node().element().orElse(cell.node());
        final String elementPath = element.concretePath(path, index);
        return rows(cell.elementRows(), new Context(elementPath, index, false), scope);
    }

    private Node mapEntry(final CollectionCell cell, final String path, final int index,
            final FormBindings.Scope scope) {
        return rows(cell.elementRows(), new Context(path, index, true), scope);
    }
}
