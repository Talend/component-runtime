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

import org.talend.sdk.component.tcketl.domain.Preconditions;
import org.talend.sdk.component.tcketl.domain.form.PropertyNode;
import org.talend.sdk.component.tcketl.domain.property.ActionTypes;
import org.talend.sdk.component.tcketl.domain.property.PropertyDefinition;
import org.talend.sdk.component.tcketl.domain.property.UiHints;

/**
 * Chooses the control of a property from its type and its hints. For a text, the first hint that applies wins, in
 * the order of the widget table of the specification: password, code, suggestions, dynamic values, text area, date or
 * time, path.
 */
public final class WidgetSelector {

    private WidgetSelector() {
        // static helpers only
    }

    public static WidgetKind select(final PropertyNode node) {
        Preconditions.requireNonNull(node, "node");
        final PropertyDefinition definition = node.definition();
        switch (node.kind()) {
            case OBJECT:
                return WidgetKind.GROUP;
            case MAP:
                return WidgetKind.MAP;
            case ARRAY_OF_OBJECT:
                return WidgetKind.LIST_OF_OBJECTS;
            case ARRAY_OF_PRIMITIVE:
                return WidgetKind.LIST_OF_VALUES;
            case BOOLEAN:
                return WidgetKind.CHECKBOX;
            case NUMBER:
                return WidgetKind.NUMBER;
            case ENUM:
                // a choice without constants could not be answered, so it is typed in
                return definition.constraints().enumValues().isEmpty() ? WidgetKind.TEXT : WidgetKind.CHOICE;
            case STRING:
                return text(definition);
            default:
                throw new IllegalStateException("Unexpected kind " + node.kind());
        }
    }

    private static WidgetKind text(final PropertyDefinition definition) {
        final UiHints ui = definition.ui();
        if (ui.credential()) {
            return WidgetKind.PASSWORD;
        }
        if (!ui.codeLanguage().isEmpty()) {
            return WidgetKind.CODE;
        }
        if (definition.action(ActionTypes.SUGGESTIONS).isPresent()
                || definition.action(ActionTypes.BUILT_IN_SUGGESTABLE).isPresent()) {
            return WidgetKind.SUGGESTION;
        }
        if (definition.action(ActionTypes.DYNAMIC_VALUES).isPresent()) {
            return WidgetKind.DYNAMIC_CHOICE;
        }
        if (ui.textArea()) {
            return WidgetKind.TEXT_AREA;
        }
        if (ui.dateTime() != UiHints.DateTimeKind.NONE) {
            return WidgetKind.DATE_TIME;
        }
        return ui.path() == UiHints.PathKind.NONE ? WidgetKind.TEXT : WidgetKind.PATH;
    }
}
