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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.talend.sdk.component.tcketl.forms.Props.array;
import static org.talend.sdk.component.tcketl.forms.Props.bool;
import static org.talend.sdk.component.tcketl.forms.Props.choice;
import static org.talend.sdk.component.tcketl.forms.Props.definitions;
import static org.talend.sdk.component.tcketl.forms.Props.number;
import static org.talend.sdk.component.tcketl.forms.Props.object;
import static org.talend.sdk.component.tcketl.forms.Props.string;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.talend.sdk.component.tcketl.domain.form.PropertyTree;
import org.talend.sdk.component.tcketl.domain.property.ActionBinding;
import org.talend.sdk.component.tcketl.domain.property.ActionTypes;
import org.talend.sdk.component.tcketl.domain.property.PropertyDefinition;
import org.talend.sdk.component.tcketl.domain.property.PropertyType;
import org.talend.sdk.component.tcketl.domain.property.UiHints;
import org.talend.sdk.component.tcketl.domain.property.UiHints.DateTimeKind;
import org.talend.sdk.component.tcketl.domain.property.UiHints.PathKind;

/** The widget table: every type and hint gets its control, and the first hint that applies wins. */
class WidgetSelectorTest {

    private static final PropertyTree TREE = PropertyTree
            .of(definitions(object("cfg"), bool("cfg.flag"), number("cfg.count"), choice("cfg.mode", "FAST", "SAFE"),
                    PropertyDefinition.builder("cfg.noConstants", PropertyType.ENUM), array("cfg.tags"),
                    string("cfg.tags[]"), array("cfg.items"), string("cfg.items[].name"), object("cfg.headers"),
                    string("cfg.headers.key[]"), string("cfg.headers.value[]"), string("cfg.plain"),
                    string("cfg.secret").ui(UiHints.builder().credential(true).build()),
                    string("cfg.query").ui(UiHints.builder().codeLanguage("sql").build()),
                    string("cfg.suggested").action(new ActionBinding(ActionTypes.SUGGESTIONS, "names")),
                    string("cfg.builtIn").action(new ActionBinding(ActionTypes.BUILT_IN_SUGGESTABLE, "columns")),
                    string("cfg.dynamic").action(new ActionBinding(ActionTypes.DYNAMIC_VALUES, "kinds")),
                    string("cfg.notes").ui(UiHints.builder().textArea(true).build()),
                    string("cfg.since").ui(UiHints.builder().dateTime(DateTimeKind.DATE_TIME).build()),
                    string("cfg.file").ui(UiHints.builder().path(PathKind.FILE).build()),
                    string("cfg.secretQuery")
                            .ui(UiHints.builder().credential(true).codeLanguage("sql").build()),
                    string("cfg.codeSuggested")
                            .ui(UiHints.builder().codeLanguage("sql").build())
                            .action(new ActionBinding(ActionTypes.SUGGESTIONS, "names")),
                    string("cfg.suggestedDynamic")
                            .action(new ActionBinding(ActionTypes.SUGGESTIONS, "names"))
                            .action(new ActionBinding(ActionTypes.DYNAMIC_VALUES, "kinds")),
                    string("cfg.dynamicNotes")
                            .ui(UiHints.builder().textArea(true).build())
                            .action(new ActionBinding(ActionTypes.DYNAMIC_VALUES, "kinds")),
                    string("cfg.notesSince").ui(UiHints.builder().textArea(true).dateTime(DateTimeKind.DATE).build()),
                    string("cfg.sinceFile")
                            .ui(UiHints.builder().dateTime(DateTimeKind.TIME).path(PathKind.DIRECTORY).build())));

    @ParameterizedTest(name = "{0} is a {1}")
    @CsvSource({ "cfg,GROUP", "cfg.flag,CHECKBOX", "cfg.count,NUMBER", "cfg.mode,CHOICE", "cfg.noConstants,TEXT",
            "cfg.tags,LIST_OF_VALUES", "cfg.items,LIST_OF_OBJECTS", "cfg.headers,MAP", "cfg.plain,TEXT",
            "cfg.secret,PASSWORD", "cfg.query,CODE", "cfg.suggested,SUGGESTION", "cfg.builtIn,SUGGESTION",
            "cfg.dynamic,DYNAMIC_CHOICE", "cfg.notes,TEXT_AREA", "cfg.since,DATE_TIME", "cfg.file,PATH" })
    void everyTypeAndHintGetsItsControl(final String path, final WidgetKind expected) {
        assertEquals(expected, WidgetSelector.select(TREE.find(path).orElseThrow()));
    }

    @ParameterizedTest(name = "{0} wins as a {1}")
    @CsvSource({ "cfg.secretQuery,PASSWORD", "cfg.codeSuggested,CODE", "cfg.suggestedDynamic,SUGGESTION",
            "cfg.dynamicNotes,DYNAMIC_CHOICE", "cfg.notesSince,TEXT_AREA", "cfg.sinceFile,DATE_TIME" })
    void theFirstHintOfTheTableWins(final String path, final WidgetKind expected) {
        assertEquals(expected, WidgetSelector.select(TREE.find(path).orElseThrow()));
    }

    @Test
    void theEntriesOfAListAndTheKeysOfAMapAreChosenLikeAnyValue() {
        assertEquals(WidgetKind.TEXT,
                WidgetSelector.select(TREE.find("cfg.tags").orElseThrow().element().orElseThrow()));
        assertEquals(WidgetKind.TEXT,
                WidgetSelector.select(TREE.find("cfg.headers").orElseThrow().child("key[]").orElseThrow()));
    }
}
