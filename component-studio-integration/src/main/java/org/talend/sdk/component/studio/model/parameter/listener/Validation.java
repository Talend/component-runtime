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

import org.talend.sdk.component.studio.Lookups;
import org.talend.sdk.component.studio.websocket.WebSocketClient.V1Action;

/**
 *
 */
public class Validation implements PropertyChangeListener {

    private static final String VALIDATION = "validation";

    private final String family;

    private final String actionName;

    private final Map<String, String> payload = new HashMap<>();

    public Validation(final String family, final String actionName) {
        this.family = family;
        this.actionName = actionName;
    }

    public void addParameter(final String parameter, final String initialValue) {
        payload.put(parameter, initialValue);
    }

    @Override
    public void propertyChange(final PropertyChangeEvent event) {
        // collect values
        final Map<String, String> payload = makePayload();
        makeCall(payload);

    }

    private Map<String, String> makePayload() {
        Map<String, String> payload = new HashMap<>();

        return payload;
    }

    private String makeCall(final Map<String, String> payload) {
        final V1Action action = Lookups.client().v1().action();
        return action.execute(String.class, family, VALIDATION, actionName, payload);
    }
}
