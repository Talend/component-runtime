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

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.util.StringConverter;

import org.talend.sdk.component.tcketl.domain.action.ActionOutcome;
import org.talend.sdk.component.tcketl.domain.form.PropertyNode;
import org.talend.sdk.component.tcketl.domain.property.ActionBinding;
import org.talend.sdk.component.tcketl.domain.property.ActionTypes;
import org.talend.sdk.component.tcketl.domain.property.PropertyDefinition;
import org.talend.sdk.component.tcketl.domain.property.UiHints;
import org.talend.sdk.component.tcketl.domain.property.UiHints.DateTimeKind;
import org.talend.sdk.component.tcketl.domain.property.UiHints.PathKind;

/**
 * Creates the control editing one scalar value. A control writes to the model when the user changes it and reads the
 * model back whenever its revision changes; reading never writes, so that opening a form does not edit anything.
 */
final class Editors {

    private static final String DEFAULT_DATE_FORMAT = "YYYY/MM/DD";

    private static final int CODE_ROWS = 6;

    private static final int TEXT_AREA_ROWS = 4;

    private Editors() {
        // static factory
    }

    /** A control, and the node that takes the focus when the user enters it. */
    record Editor(Node node, Node focusOwner) {
    }

    /**
     * What a control edits.
     *
     * @param node the property.
     * @param path its concrete path.
     * @param index the position of the entry when it is in a list, else -1.
     * @param prompt the text shown in an empty control.
     */
    record Spec(PropertyNode node, String path, int index, String prompt) {

        boolean readOnly() {
            return node.definition().ui().readOnly();
        }
    }

    static Editor create(final WidgetKind kind, final Spec spec, final FormViewModel model,
            final FormBindings.Scope scope, final ActionService service) {
        return switch (kind) {
            case CHECKBOX -> checkBox(spec, model, scope);
            case CHOICE -> choice(spec, model, scope);
            case SUGGESTION -> editableCombo(spec, model, scope, service);
            case DYNAMIC_CHOICE -> dynamicCombo(spec, model, scope, service);
            case PASSWORD -> text(new PasswordField(), spec, model, scope);
            case CODE -> area(spec, model, scope, CODE_ROWS, false, "form-code");
            case TEXT_AREA -> area(spec, model, scope, TEXT_AREA_ROWS, true, "form-area");
            case PATH -> path(spec, model, scope);
            case TEXT, NUMBER, DATE_TIME -> text(new TextField(), spec, model, scope);
            default -> throw new IllegalArgumentException("Not a scalar control: " + kind);
        };
    }

    /** The hint shown in an empty date or time field. */
    static String dateHint(final UiHints ui) {
        final String date = ui.dateFormat().isEmpty() ? DEFAULT_DATE_FORMAT : ui.dateFormat();
        final String time = ui.useSeconds() ? "HH:mm:ss" : "HH:mm";
        return switch (ui.dateTime()) {
            case DATE -> date;
            case TIME -> time;
            case DATE_TIME, ZONED_DATE_TIME -> date + ' ' + time;
            default -> "";
        };
    }

    static String promptOf(final PropertyNode node, final int index) {
        final String placeholder = Texts.placeholder(node, index);
        if (!placeholder.isEmpty()) {
            return placeholder;
        }
        final UiHints ui = node.definition().ui();
        return ui.dateTime() == DateTimeKind.NONE ? "" : dateHint(ui);
    }

    private static Editor text(final TextInputControl field, final Spec spec, final FormViewModel model,
            final FormBindings.Scope scope) {
        final Guard guard = new Guard();
        field.setPromptText(spec.prompt());
        field.setEditable(!spec.readOnly());
        field.textProperty().addListener((value, before, after) -> guard.push(() -> model.set(spec.path(), after)));
        scope.onRefresh(() -> pullText(field, model.get(spec.path()), guard));
        return new Editor(field, field);
    }

    private static Editor area(final Spec spec, final FormViewModel model, final FormBindings.Scope scope,
            final int rows, final boolean wrap, final String style) {
        final TextArea area = new TextArea();
        area.setPrefRowCount(rows);
        area.setWrapText(wrap);
        area.getStyleClass().add(style);
        return text(area, spec, model, scope);
    }

    private static void pullText(final TextInputControl field, final String value, final Guard guard) {
        if (!value.equals(field.getText())) {
            guard.pull(() -> field.setText(value));
        }
    }

    private static Editor checkBox(final Spec spec, final FormViewModel model, final FormBindings.Scope scope) {
        final Guard guard = new Guard();
        final CheckBox box = new CheckBox(Texts.title(spec.node(), spec.index()));
        box.setDisable(spec.readOnly());
        box.selectedProperty()
                .addListener((value, before, after) -> guard.push(() -> model.set(spec.path(), after.toString())));
        scope.onRefresh(() -> {
            final boolean selected = Boolean.parseBoolean(model.get(spec.path()));
            if (selected != box.isSelected()) {
                guard.pull(() -> box.setSelected(selected));
            }
        });
        return new Editor(box, box);
    }

    private static Editor choice(final Spec spec, final FormViewModel model, final FormBindings.Scope scope) {
        final Guard guard = new Guard();
        final PropertyDefinition definition = spec.node().definition();
        final Map<String, String> labels = definition.proposalLabels();
        final ComboBox<String> combo = new ComboBox<>();
        combo.getItems().setAll(definition.constraints().enumValues());
        combo.setConverter(new StringConverter<>() {

            @Override
            public String toString(final String value) {
                return value == null ? "" : labels.getOrDefault(value, value);
            }

            @Override
            public String fromString(final String label) {
                return labels.entrySet()
                        .stream()
                        .filter(entry -> entry.getValue().equals(label))
                        .map(Map.Entry::getKey)
                        .findFirst()
                        .orElse(label);
            }
        });
        combo.setPromptText(spec.prompt());
        combo.setDisable(spec.readOnly());
        combo.valueProperty()
                .addListener(
                        (value, before, after) -> guard.push(() -> model.set(spec.path(), after == null ? "" : after)));
        scope.onRefresh(() -> {
            final String stored = model.get(spec.path());
            final String shown = stored.isEmpty() ? null : stored;
            if (shown == null ? combo.getValue() != null : !shown.equals(combo.getValue())) {
                guard.pull(() -> combo.setValue(shown));
            }
        });
        return new Editor(combo, combo);
    }

    /** An editable combo: what the user types is stored, a suggestion stores its id and shows its label. */
    private static Editor editableCombo(final Spec spec, final FormViewModel model, final FormBindings.Scope scope,
            final ActionService service) {
        final Guard guard = new Guard();
        final Map<String, String> ids = new HashMap<>();
        final Map<String, String> labels = new HashMap<>();
        final ComboBox<String> combo = new ComboBox<>();
        combo.setEditable(true);
        combo.setPromptText(spec.prompt());
        combo.setDisable(spec.readOnly());
        final TextField editor = combo.getEditor();
        editor.textProperty()
                .addListener((value, before, after) -> guard
                        .push(() -> model.set(spec.path(), ids.getOrDefault(after, after))));
        scope.onRefresh(() -> {
            final String stored = model.get(spec.path());
            pullText(editor, labels.getOrDefault(stored, stored), guard);
        });
        final Optional<ActionBinding> binding = spec.node().definition().action(ActionTypes.SUGGESTIONS);
        if (binding.isPresent() && service.supports(binding.get())) {
            final Runnable load = () -> service
                    .request(binding.get(), spec.path())
                    .ifPresent(request -> service.choices(spec.path() + '#' + ActionTypes.SUGGESTIONS, request, false,
                            found -> {
                                ids.clear();
                                labels.clear();
                                found.items().forEach(choice -> {
                                    ids.put(choice.label(), choice.id());
                                    labels.put(choice.id(), choice.label());
                                });
                                guard.pull(() -> combo.getItems()
                                        .setAll(found.items().stream().map(ActionOutcome.Choice::label).toList()));
                                final String stored = model.get(spec.path());
                                pullText(editor, labels.getOrDefault(stored, stored), guard);
                            }));
            combo.showingProperty().addListener((value, before, after) -> {
                if (after) {
                    load.run();
                }
            });
            editor.focusedProperty().addListener((value, before, after) -> {
                if (after) {
                    load.run();
                }
            });
        }
        return new Editor(combo, editor);
    }

    /** A restricted combo whose options the server gives once, when the form is built; the stored value is the id. */
    private static Editor dynamicCombo(final Spec spec, final FormViewModel model, final FormBindings.Scope scope,
            final ActionService service) {
        final Guard guard = new Guard();
        final Map<String, String> labels = new HashMap<>();
        final ComboBox<String> combo = new ComboBox<>();
        combo.setPromptText(spec.prompt());
        combo.setDisable(spec.readOnly());
        combo.setConverter(new StringConverter<>() {

            @Override
            public String toString(final String value) {
                return value == null ? "" : labels.getOrDefault(value, value);
            }

            @Override
            public String fromString(final String label) {
                return label;
            }
        });
        combo.valueProperty()
                .addListener(
                        (value, before, after) -> guard.push(() -> model.set(spec.path(), after == null ? "" : after)));
        scope.onRefresh(() -> {
            final String stored = model.get(spec.path());
            final String shown = stored.isEmpty() ? null : stored;
            if (shown == null ? combo.getValue() != null : !shown.equals(combo.getValue())) {
                guard.pull(() -> combo.setValue(shown));
            }
        });
        spec.node()
                .definition()
                .action(ActionTypes.DYNAMIC_VALUES)
                .flatMap(binding -> service.request(binding, spec.path()))
                .ifPresent(request -> service.choices(spec.path() + '#' + ActionTypes.DYNAMIC_VALUES, request, true,
                        found -> {
                            found.items().forEach(choice -> labels.put(choice.id(), choice.label()));
                            final String stored = model.get(spec.path());
                            guard.pull(() -> {
                                combo.getItems().setAll(found.items().stream().map(ActionOutcome.Choice::id).toList());
                                combo.setValue(stored.isEmpty() ? null : stored);
                            });
                        }));
        return new Editor(combo, combo);
    }

    private static Editor path(final Spec spec, final FormViewModel model, final FormBindings.Scope scope) {
        final Editor inner = text(new TextField(), spec, model, scope);
        final TextField field = (TextField) inner.node();
        final Button browse = new Button("\u2026");
        browse.setDisable(spec.readOnly());
        browse.setAccessibleText("Browse");
        browse.setOnAction(event -> {
            final Scene scene = browse.getScene();
            final Window window = scene == null ? null : scene.getWindow();
            final File chosen = spec.node().definition().ui().path() == PathKind.DIRECTORY
                    ? new DirectoryChooser().showDialog(window)
                    : new FileChooser().showOpenDialog(window);
            if (chosen != null) {
                field.setText(chosen.getPath());
            }
        });
        HBox.setHgrow(field, Priority.ALWAYS);
        final HBox box = new HBox(4, field, browse);
        box.getStyleClass().add("form-path");
        return new Editor(box, field);
    }

    /** Tells a change made by the user from one made by the refresh. */
    private static final class Guard {

        private boolean pulling;

        void pull(final Runnable action) {
            final boolean before = pulling;
            pulling = true;
            try {
                action.run();
            } finally {
                pulling = before;
            }
        }

        void push(final Runnable action) {
            if (!pulling) {
                action.run();
            }
        }
    }
}
