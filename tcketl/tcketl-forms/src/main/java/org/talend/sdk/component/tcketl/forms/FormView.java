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

import javafx.scene.layout.Region;

import org.talend.sdk.component.tcketl.domain.Preconditions;

/**
 * The form of one connector: the tabs of its layout, with a control per property, editing a {@link FormViewModel}.
 * It belongs to the JavaFX application thread; {@link #dispose()} it when it is replaced.
 */
public final class FormView {

    private static final String STYLESHEET = "forms.css";

    private final FormViewModel model;

    private final FormBindings bindings;

    private final ActionService service;

    private final Region node;

    private FormView(final FormViewModel model, final ActionContext actions) {
        this.model = model;
        this.bindings = new FormBindings(model);
        this.service = new ActionService(model, actions);
        this.node = new FormBuilder(model, bindings, service).build();
        node.getStyleClass().add("form-view");
        node.getStylesheets().add(FormView.class.getResource(STYLESHEET).toExternalForm());
    }

    public static FormView of(final FormViewModel model) {
        return of(model, ActionContext.NONE);
    }

    public static FormView of(final FormViewModel model, final ActionContext actions) {
        return new FormView(Preconditions.requireNonNull(model, "model"),
                Preconditions.requireNonNull(actions, "actions"));
    }

    public FormViewModel model() {
        return model;
    }

    public Region node() {
        return node;
    }

    /** Stops the controls from following the model. */
    public void dispose() {
        service.close();
        bindings.close();
    }
}
