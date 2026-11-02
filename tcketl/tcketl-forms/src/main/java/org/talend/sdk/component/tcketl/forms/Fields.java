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

import java.util.Optional;

import javafx.css.PseudoClass;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import org.talend.sdk.component.tcketl.domain.form.PropertyKind;
import org.talend.sdk.component.tcketl.domain.property.PropertyDefinition;
import org.talend.sdk.component.tcketl.forms.Editors.Editor;
import org.talend.sdk.component.tcketl.forms.Editors.Spec;

/** Wraps a control with what surrounds it: its title, its documentation and the message about its value. */
final class Fields {

    private static final PseudoClass ERROR = PseudoClass.getPseudoClass("error");

    private static final double TOOLTIP_WIDTH = 420;

    private static final Duration TOOLTIP_DURATION = Duration.seconds(30);

    private static final Duration TOOLTIP_DELAY = Duration.millis(500);

    private Fields() {
        // static factory
    }

    /** A control with the message about its value below it and the touch of the field when it is left. */
    static Node withMessage(final Editor editor, final Spec spec, final FormViewModel model,
            final FormBindings.Scope scope) {
        final Label message = new Label();
        message.getStyleClass().add("form-error");
        message.setWrapText(true);
        message.setMinHeight(Label.USE_PREF_SIZE);
        editor.focusOwner().focusedProperty().addListener((value, before, after) -> {
            if (!after) {
                model.touch(spec.path());
            }
        });
        scope.onRefresh(() -> {
            final Optional<String> text = model.message(spec.path());
            message.setText(text.orElse(""));
            message.setVisible(text.isPresent());
            message.setManaged(text.isPresent());
            editor.node().pseudoClassStateChanged(ERROR, text.isPresent());
        });
        return new VBox(2, editor.node(), message);
    }

    /** A titled field: title, required mark, documentation, control and message. */
    static Node full(final Editor editor, final Spec spec, final FormViewModel model, final FormBindings.Scope scope) {
        final PropertyDefinition definition = spec.node().definition();
        final VBox box = new VBox(3);
        box.getStyleClass().add("form-field");
        final boolean checkBox = spec.node().kind() == PropertyKind.BOOLEAN;
        if (!checkBox) {
            final Label title = new Label(Texts.title(spec.node(), spec.index()));
            title.getStyleClass().add("form-title");
            title.setLabelFor(editor.focusOwner());
            final HBox row = new HBox(3, title);
            if (definition.constraints().required()) {
                final Label star = new Label("*");
                star.getStyleClass().add("form-required");
                row.getChildren().add(star);
            }
            box.getChildren().add(row);
        }
        final String documentation = Texts.numbered(definition.documentation(), spec.index());
        documentation(box, documentation);
        if (checkBox) {
            documentation(editor.node(), documentation);
        }
        box.getChildren().add(withMessage(editor, spec, model, scope));
        return box;
    }

    /** Shows the documentation as a tooltip once the pointer rests half a second on the node. */
    static void documentation(final Node node, final String documentation) {
        if (documentation.isEmpty()) {
            return;
        }
        final Tooltip tooltip = new Tooltip(documentation);
        tooltip.setWrapText(true);
        tooltip.setMaxWidth(TOOLTIP_WIDTH);
        tooltip.setShowDelay(TOOLTIP_DELAY);
        tooltip.setShowDuration(TOOLTIP_DURATION);
        Tooltip.install(node, tooltip);
    }
}
