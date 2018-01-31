/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.studio.model.parameter.listener;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Map;

import org.talend.sdk.component.studio.Lookups;
import org.talend.sdk.component.studio.model.parameter.ValidationLabel;
import org.talend.sdk.component.studio.websocket.WebSocketClient.V1Action;

/**
 *
 */
public class ValidationListener implements PropertyChangeListener {

    private static final String VALIDATION = "validation";

    private static final String STATUS = "status";

    private static final String OK = "OK";

    private static final String MESSAGE = "comment";

    private final String family;

    private final String actionName;

    private final ValidationLabel label;

    private final ActionParameters parameters = new ActionParameters();

    public ValidationListener(final ValidationLabel label, final String family, final String actionName) {
        this.label = label;
        this.family = family;
        this.actionName = actionName;
    }

    @Override
    public void propertyChange(final PropertyChangeEvent event) {
        parameters.setValue(event.getPropertyName(), (String) event.getNewValue());
        if (!parameters.areSet()) {
            return;
        }
        final Map<String, String> validation = callback(parameters.payload());
        if (OK.equals(validation.get(STATUS))) {
            label.hideValidation();
        } else {
            label.showValidation(validation.get(MESSAGE));
        }
    }

    public void addParameter(final ActionParameter parameter) {
        parameters.add(parameter);
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> callback(final Map<String, String> payload) {
        final V1Action action = Lookups.client().v1().action();
        return action.execute(Map.class, family, VALIDATION, actionName, payload);
    }
}
