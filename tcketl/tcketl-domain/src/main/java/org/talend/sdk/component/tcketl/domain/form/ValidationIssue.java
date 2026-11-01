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
package org.talend.sdk.component.tcketl.domain.form;

import org.talend.sdk.component.tcketl.domain.Preconditions;

/**
 * A value that breaks a rule of its property.
 *
 * @param path the concrete path of the value, or of the list for a rule on its size.
 * @param kind the rule that is broken.
 * @param message what to tell the user, in English.
 */
public record ValidationIssue(String path, Kind kind, String message) {

    public ValidationIssue {
        Preconditions.requireNonBlank(path, "path");
        Preconditions.requireNonNull(kind, "kind");
        message = Preconditions.nullToEmpty(message);
    }

    public enum Kind {
        REQUIRED,
        NUMBER_FORMAT,
        MIN,
        MAX,
        MIN_LENGTH,
        MAX_LENGTH,
        PATTERN,
        MIN_ITEMS,
        MAX_ITEMS,
        UNIQUE_ITEMS,
        ENUM_VALUE
    }
}
