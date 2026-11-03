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

import org.talend.sdk.component.tcketl.domain.form.PropertyNode;

/** Texts shown by the controls. */
public final class Texts {

    private static final String INDEX = "${index}";

    private Texts() {
        // static helpers only
    }

    /**
     * Replaces the {@code ${index}} placeholder the server leaves in the titles of list entries by the position of the
     * entry, counted from 1 (inferred: the server documents no base).
     */
    public static String numbered(final String text, final int index) {
        return index < 0 ? text : text.replace(INDEX, Integer.toString(index + 1));
    }

    public static String title(final PropertyNode node, final int index) {
        return numbered(node.definition().displayName(), index);
    }

    public static String placeholder(final PropertyNode node, final int index) {
        return numbered(node.definition().placeholder(), index);
    }
}
