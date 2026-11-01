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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import org.talend.sdk.component.tcketl.domain.action.ActionOutcome;
import org.talend.sdk.component.tcketl.domain.action.ActionRequest;
import org.talend.sdk.component.tcketl.domain.form.ActionBodies;
import org.talend.sdk.component.tcketl.domain.property.ActionBinding;

/**
 * Runs the services of one open form. Each call has a key (a path and a type): only the answer to the latest call
 * of a key is delivered, so a slow answer to an old value never overwrites a newer one. Answers that arrive after
 * {@link #close()} are dropped. It belongs to the JavaFX application thread.
 */
final class ActionService {

    private final FormViewModel model;

    private final ActionContext context;

    private final Map<String, Long> sequences = new HashMap<>();

    private final Map<ActionRequest, ActionOutcome.Choices> choices = new HashMap<>();

    private boolean closed;

    ActionService(final FormViewModel model, final ActionContext context) {
        this.model = model;
        this.context = context;
    }

    /** Whether the connector describes the action the binding names. */
    boolean supports(final ActionBinding binding) {
        return request(binding, "").isPresent();
    }

    /** The request for the binding of the property at {@code ownerPath}, from the current values. */
    Optional<ActionRequest> request(final ActionBinding binding, final String ownerPath) {
        return ActionBodies.request(context.family(), binding, context.actions(), ownerPath, model.values());
    }

    void run(final String key, final ActionRequest request, final Consumer<ActionOutcome> sink) {
        final long sequence = sequences.merge(key, 1L, Long::sum);
        context.execute(request, outcome -> {
            if (!closed && sequences.get(key) == sequence) {
                sink.accept(outcome);
            }
        });
    }

    /** Like {@link #run} for a list of choices, which are kept when {@code keep} or when the server says so. */
    void choices(final String key, final ActionRequest request, final boolean keep,
            final Consumer<ActionOutcome.Choices> sink) {
        final ActionOutcome.Choices known = choices.get(request);
        if (known != null) {
            sink.accept(known);
            return;
        }
        run(key, request, outcome -> {
            if (outcome instanceof ActionOutcome.Choices found) {
                if (keep || found.cacheable()) {
                    choices.put(request, found);
                }
                sink.accept(found);
            }
        });
    }

    void close() {
        closed = true;
    }
}
