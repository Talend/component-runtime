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
package org.talend.sdk.component.tcketl.forms.vaadin;

import com.vaadin.flow.component.Component;

import org.talend.sdk.component.tcketl.domain.Preconditions;
import org.talend.sdk.component.tcketl.forms.ActionContext;
import org.talend.sdk.component.tcketl.forms.ActionService;
import org.talend.sdk.component.tcketl.forms.FormAssembler;
import org.talend.sdk.component.tcketl.forms.FormBindings;
import org.talend.sdk.component.tcketl.forms.FormViewModel;

/**
 * The form of one connector, rendered with Vaadin components: the tabs of its layout, with a control per property,
 * editing a {@link FormViewModel}. It belongs to the UI that shows it; {@link #dispose()} it when it is replaced.
 */
public final class VaadinFormView {

    private final FormViewModel model;

    private final FormBindings bindings;

    private final ActionService service;

    private final Component component;

    private VaadinFormView(final FormViewModel model, final ActionContext actions) {
        this.model = model;
        this.bindings = new FormBindings(model);
        this.service = new ActionService(model, actions);
        this.component = new FormAssembler<>(model, bindings, service, new VaadinFormToolkit(model, service)).build();
        component.getElement().getClassList().add("form-view");
    }

    public static VaadinFormView of(final FormViewModel model) {
        return of(model, ActionContext.NONE);
    }

    public static VaadinFormView of(final FormViewModel model, final ActionContext actions) {
        return new VaadinFormView(Preconditions.requireNonNull(model, "model"),
                Preconditions.requireNonNull(actions, "actions"));
    }

    public FormViewModel model() {
        return model;
    }

    public Component component() {
        return component;
    }

    /** Stops the controls from following the model. */
    public void dispose() {
        service.close();
        bindings.close();
    }
}
