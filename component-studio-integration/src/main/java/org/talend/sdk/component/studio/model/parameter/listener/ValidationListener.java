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
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.talend.sdk.component.studio.Lookups;
import org.talend.sdk.component.studio.model.parameter.ValidationLabel;
import org.talend.sdk.component.studio.websocket.WebSocketClient.V1Action;

/**
 *
 */
public class ValidationListener implements PropertyChangeListener {

    private static final String VALIDATION = "validation";

    private final String family;

    private final String actionName;

    private final ValidationLabel label;

    /**
     * Maps ElementParameter path to parameter alias used during call to validation callback
     */
    private final Map<String, String> parameters = new HashMap<>();

    private final Map<String, String> payload = new HashMap<>();

    public ValidationListener(final ValidationLabel label, final String family, final String actionName) {
        this.label = label;
        this.family = family;
        this.actionName = actionName;
    }

    public void addParameter(final String path, final String parameterName, final String initialValue) {
        Objects.nonNull(path);
        Objects.nonNull(parameterName);
        parameters.put(path, parameterName);
        payload.put(parameterName, initialValue);
    }

    @Override
    public void propertyChange(final PropertyChangeEvent event) {
        updatePayload(event);
        final Map<String, String> result = validationCallback(payload);
        if ("OK".equals(result.get("status"))) {
            label.hideValidation();
        } else {
            label.showValidation(result.get("comment"));
        }
    }

    private void updatePayload(final PropertyChangeEvent event) {
        final String parameter = parameters.get(event.getPropertyName());
        final String newValue = (String) event.getNewValue();
        payload.put(parameter, newValue);
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> validationCallback(final Map<String, String> payload) {
        final V1Action action = Lookups.client().v1().action();
        return action.execute(Map.class, family, VALIDATION, actionName, payload);
    }
}
