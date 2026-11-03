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

import java.util.List;
import java.util.function.Consumer;

import org.talend.sdk.component.tcketl.domain.action.ActionOutcome;
import org.talend.sdk.component.tcketl.domain.action.ActionRequest;
import org.talend.sdk.component.tcketl.domain.property.ActionDefinition;

/**
 * What a form needs to call the services of its connector on the server: the technical family, the actions the
 * connector describes, and a way to run one without blocking the caller.
 */
public interface ActionContext {

    /** No service is available: the buttons that need one stay disabled. */
    ActionContext NONE = new ActionContext() {

        @Override
        public String family() {
            return "";
        }

        @Override
        public List<ActionDefinition> actions() {
            return List.of();
        }

        @Override
        public void execute(final ActionRequest request, final Consumer<ActionOutcome> onOutcome) {
            onOutcome.accept(new ActionOutcome.Failed("No server"));
        }
    };

    /** The technical family of the connector, the {@code family} parameter of the call. */
    String family();

    /** The actions the connector describes. */
    List<ActionDefinition> actions();

    /**
     * Runs the request away from the calling thread. The outcome is always delivered, on the JavaFX application
     * thread: a failure of the call is an {@link ActionOutcome.Failed}.
     */
    void execute(ActionRequest request, Consumer<ActionOutcome> onOutcome);
}
