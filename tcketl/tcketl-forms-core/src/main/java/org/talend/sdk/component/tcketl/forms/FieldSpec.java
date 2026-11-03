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
import org.talend.sdk.component.tcketl.domain.property.UiHints;
import org.talend.sdk.component.tcketl.domain.property.UiHints.DateTimeKind;

/**
 * What a control edits.
 *
 * @param node the property.
 * @param path its concrete path.
 * @param index the position of the entry when it is in a list, else -1.
 * @param prompt the text shown in an empty control.
 */
public record FieldSpec(PropertyNode node, String path, int index, String prompt) {

    private static final String DEFAULT_DATE_FORMAT = "YYYY/MM/DD";

    public static FieldSpec of(final PropertyNode node, final String path, final int index) {
        return new FieldSpec(node, path, index, promptOf(node, index));
    }

    public boolean readOnly() {
        return node.definition().ui().readOnly();
    }

    /** The hint shown in an empty date or time field. */
    public static String dateHint(final UiHints ui) {
        final String date = ui.dateFormat().isEmpty() ? DEFAULT_DATE_FORMAT : ui.dateFormat();
        final String time = ui.useSeconds() ? "HH:mm:ss" : "HH:mm";
        return switch (ui.dateTime()) {
            case DATE -> date;
            case TIME -> time;
            case DATE_TIME, ZONED_DATE_TIME -> date + ' ' + time;
            default -> "";
        };
    }

    private static String promptOf(final PropertyNode node, final int index) {
        final String placeholder = Texts.placeholder(node, index);
        if (!placeholder.isEmpty()) {
            return placeholder;
        }
        final UiHints ui = node.definition().ui();
        return ui.dateTime() == DateTimeKind.NONE ? "" : dateHint(ui);
    }
}
