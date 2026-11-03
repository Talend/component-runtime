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

import java.util.LinkedHashMap;
import java.util.Map;

import org.talend.sdk.component.tcketl.domain.action.ActionOutcome;

/**
 * What a form button shows next to itself, and its state.
 *
 * @param kind the state of the call.
 * @param text the message, empty when there is none.
 */
public record Feedback(Kind kind, String text) {

    /** The state of the call behind a button. */
    public enum Kind {
        NONE,
        RUNNING,
        OK,
        KO
    }

    public static final Feedback IDLE = new Feedback(Kind.NONE, "");

    public static final Feedback RUNNING = new Feedback(Kind.RUNNING, "Running\u2026");

    /**
     * Applies an outcome to the model when it is a change of values, and describes it.
     *
     * @param outcome the answer of the server.
     * @param path the concrete path of the object the button belongs to.
     * @param model the model the answer applies to.
     * @return what to show.
     */
    public static Feedback apply(final ActionOutcome outcome, final String path, final FormViewModel model) {
        if (outcome instanceof ActionOutcome.Checked checked) {
            return checked.ok()
                    ? new Feedback(Kind.OK, "\u2713 " + (checked.comment().isEmpty() ? "OK" : checked.comment()))
                    : new Feedback(Kind.KO, "\u2717 " + (checked.comment().isEmpty() ? "Failed" : checked.comment()));
        } else if (outcome instanceof ActionOutcome.Replacement replacement) {
            final long known = replacement.values()
                    .keySet()
                    .stream()
                    .filter(suffix -> model.tree().find(path + suffix).isPresent())
                    .count();
            if (replacement.values().isEmpty()) {
                return new Feedback(Kind.KO, "\u2717 The service returned no value, nothing was changed");
            } else if (known == 0) {
                return new Feedback(Kind.KO, "\u2717 None of the returned values is a field of this form: "
                        + replacement.values().keySet().stream().limit(5).toList());
            }
            model.replaceSubtree(path, replacement.values());
            return new Feedback(Kind.OK, "Updated (" + known + " values)");
        } else if (outcome instanceof ActionOutcome.Columns columns) {
            final Map<String, String> entries = new LinkedHashMap<>();
            for (int i = 0; i < columns.names().size(); i++) {
                entries.put("[" + i + "]", columns.names().get(i));
            }
            model.replaceSubtree(path, entries);
            return new Feedback(Kind.OK, columns.names().size() + " columns");
        } else if (outcome instanceof ActionOutcome.Failed failed) {
            return new Feedback(Kind.KO,
                    "\u2717 " + (failed.message().isEmpty() ? "The call failed" : failed.message()));
        }
        return IDLE;
    }
}
