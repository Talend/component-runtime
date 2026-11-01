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
package org.talend.sdk.component.tcketl.domain.action;

import java.util.List;
import java.util.Map;

import org.talend.sdk.component.tcketl.domain.Preconditions;

/**
 * What a service answered, decoded by the type of the request: a status for {@code healthcheck} and
 * {@code validation}, choices for {@code suggestions} and {@code dynamic_values}, a replacement for {@code update} and
 * column names for {@code schema}. A failure of the call itself is reported as {@link Failed} by the callers that
 * cannot propagate an exception.
 */
public sealed interface ActionOutcome {

    /** {@code OK} or {@code KO} with the comment of the service. */
    record Checked(boolean ok, String comment) implements ActionOutcome {

        public Checked {
            comment = Preconditions.nullToEmpty(comment);
        }
    }

    /** A value (the {@code id}, which is what gets stored) and the text shown for it. */
    record Choice(String id, String label) {

        public Choice {
            Preconditions.requireNonBlank(id, "id");
            label = Preconditions.orDefault(label, id);
        }
    }

    record Choices(boolean cacheable, List<Choice> items) implements ActionOutcome {

        public Choices {
            items = Preconditions.immutableCopy(items, "items");
        }
    }

    /**
     * The new flat values of the sub-tree the update was run for, keyed by what follows its path: {@code .url},
     * {@code [0]}, or empty.
     */
    record Replacement(Map<String, String> values) implements ActionOutcome {

        public Replacement {
            values = Preconditions.sortedCopy(values, "values");
        }
    }

    record Columns(List<String> names) implements ActionOutcome {

        public Columns {
            names = Preconditions.immutableCopy(names, "names");
        }
    }

    record Failed(String message) implements ActionOutcome {

        public Failed {
            message = Preconditions.nullToEmpty(message);
        }
    }
}
