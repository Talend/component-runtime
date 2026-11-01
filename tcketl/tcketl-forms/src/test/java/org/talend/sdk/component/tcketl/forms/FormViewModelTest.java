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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.talend.sdk.component.tcketl.forms.Props.array;
import static org.talend.sdk.component.tcketl.forms.Props.definitions;
import static org.talend.sdk.component.tcketl.forms.Props.map;
import static org.talend.sdk.component.tcketl.forms.Props.number;
import static org.talend.sdk.component.tcketl.forms.Props.object;
import static org.talend.sdk.component.tcketl.forms.Props.string;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.tcketl.domain.form.FormValues;
import org.talend.sdk.component.tcketl.domain.form.ValidationIssue;
import org.talend.sdk.component.tcketl.domain.property.ConditionClause;
import org.talend.sdk.component.tcketl.domain.property.Constraints;
import org.talend.sdk.component.tcketl.domain.property.EvaluationStrategy;
import org.talend.sdk.component.tcketl.domain.property.PropertyDefinition;
import org.talend.sdk.component.tcketl.domain.property.VisibilityCondition;

/** What the inspector relies on: edits are published once and in order, and nothing happens on open. */
class FormViewModelTest {

    private static final List<PropertyDefinition> PROPERTIES = definitions(object("cfg"),
            string("cfg.name").constraints(Constraints.NONE.withRequired(true)),
            number("cfg.count").defaultValue("1").constraints(Constraints.NONE.withMin(1)),
            string("cfg.label").defaultValue("L"), array("cfg.tags").constraints(Constraints.NONE.withMinItems(1)),
            string("cfg.tags[]"));

    @Test
    void aHiddenFieldIsNotValidatedAndComesBackWithItsCondition() {
        final FormViewModel model = FormViewModel.of(definitions(object("cfg"), string("cfg.mode"),
                string("cfg.secret").constraints(Constraints.NONE.withRequired(true))
                        .visibility(new VisibilityCondition(VisibilityCondition.Operator.AND, List.of(
                                new ConditionClause("mode", EvaluationStrategy.DEFAULT, Map.of(), List.of("ON"),
                                        false))))),
                map());

        assertFalse(model.isVisible("cfg.secret"));
        assertTrue(model.isValid());

        model.set("cfg.mode", "ON");

        assertTrue(model.isVisible("cfg.secret"));
        assertEquals(List.of("cfg.secret"), model.issues().stream().map(ValidationIssue::path).toList());
    }

    @Test
    void openingAFormChangesNothingAndShowsNoMessageYet() {
        final FormViewModel model = FormViewModel.of(PROPERTIES, map());

        assertEquals(0L, model.revisionProperty().get());
        assertEquals("L", model.get("cfg.label"));
        assertFalse(model.isValid());
        assertEquals(List.of("cfg.name", "cfg.tags"), model.issues().stream().map(ValidationIssue::path).toList());
        assertTrue(model.message("cfg.name").isEmpty());
        assertTrue(model.message("cfg.tags").isEmpty());
    }

    @Test
    void anEditIsPublishedOnceTheValuesAndTheIssuesAgree() {
        final FormViewModel model = FormViewModel.of(PROPERTIES, map());
        final List<Boolean> nameStillReported = new ArrayList<>();
        final List<Long> revisions = new ArrayList<>();
        model.valuesProperty().addListener((observable, was, now) -> {
            nameStillReported.add(model.issues().stream().anyMatch(issue -> issue.path().equals("cfg.name")));
            revisions.add(model.revisionProperty().get());
        });

        model.set("cfg.name", "x");

        assertEquals(List.of(false), nameStillReported);
        assertEquals(List.of(0L), revisions);
        assertEquals(1L, model.revisionProperty().get());
        assertEquals("x", model.values().toConfiguration().get("cfg.name"));
    }

    @Test
    void settingTheValueAFieldAlreadyHasIsNotAnEdit() {
        final FormViewModel model = FormViewModel.of(PROPERTIES, map());
        final FormValues before = model.values();

        model.set("cfg.label", "L");
        model.set("cfg.name", "");

        assertEquals(0L, model.revisionProperty().get());
        assertSame(before, model.values());
        assertFalse(model.values().has("cfg.name"));
    }

    @Test
    void aWrongValueIsShownAtOnceAndAMissingOneOnceTheFieldWasLeft() {
        final FormViewModel model = FormViewModel.of(PROPERTIES, map());

        model.set("cfg.count", "abc");
        assertTrue(model.message("cfg.count").isPresent());
        assertTrue(model.message("cfg.name").isEmpty());

        final long revision = model.revisionProperty().get();
        model.touch("cfg.name");
        assertTrue(model.message("cfg.name").isPresent());
        assertEquals(revision + 1, model.revisionProperty().get());

        model.touch("cfg.name");
        model.touch("cfg.label");
        assertEquals(revision + 1, model.revisionProperty().get());
    }

    @Test
    void theEntriesOfAListAreEditedThroughTheModelAndItsMessageAppearsOnceItWasWorkedOn() {
        final FormViewModel model = FormViewModel.of(PROPERTIES, map("cfg.name", "n"));
        assertEquals(0, model.count("cfg.tags"));
        assertTrue(model.message("cfg.tags").isEmpty());

        model.append("cfg.tags");
        model.set("cfg.tags[0]", "a");
        model.append("cfg.tags");
        assertEquals(2, model.count("cfg.tags"));
        assertTrue(model.isValid());

        model.move("cfg.tags", 0, 1);
        assertEquals("", model.get("cfg.tags[0]"));
        assertEquals("a", model.get("cfg.tags[1]"));

        model.remove("cfg.tags", 0);
        model.remove("cfg.tags", 0);
        assertEquals(0, model.count("cfg.tags"));
        assertTrue(model.message("cfg.tags").isPresent());

        final long revision = model.revisionProperty().get();
        model.remove("cfg.tags", 3);
        assertEquals(revision, model.revisionProperty().get());
    }
}
