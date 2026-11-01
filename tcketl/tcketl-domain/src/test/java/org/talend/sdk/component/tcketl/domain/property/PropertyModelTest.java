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
package org.talend.sdk.component.tcketl.domain.property;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.OptionalInt;

import org.junit.jupiter.api.Test;

/** The small value types of the property model. */
class PropertyModelTest {

    @Test
    void gridLayoutListsItsCellsRowAfterRow() {
        final GridLayout layout = new GridLayout("Main", List.of(List.of("login", "password"), List.of("ok")));

        assertEquals(List.of("login", "password", "ok"), layout.cells());
        assertThrows(UnsupportedOperationException.class, () -> layout.rows().get(0).add("x"));
    }

    @Test
    void gridLayoutRefusesAnEmptyRow() {
        assertThrows(IllegalArgumentException.class, () -> new GridLayout("Main", List.of(List.of())));
    }

    @Test
    void panesAreFoundIgnoringCase() {
        final LayoutHints hints = new LayoutHints(List
                .of(new GridLayout("Main", List.of(List.of("a"))), new GridLayout("Advanced", List.of(List.of("b")))),
                List.of());

        assertEquals("Advanced", hints.grid("advanced").orElseThrow().name());
        assertTrue(hints.grid("Other").isEmpty());
        assertFalse(hints.isEmpty());
        assertTrue(LayoutHints.NONE.isEmpty());
    }

    @Test
    void aConditionNeedsAnExpectedValueButItMayBeEmpty() {
        assertThrows(IllegalArgumentException.class, () -> clause(List.of()));
        assertEquals(List.of(""), clause(List.of("")).values());
    }

    @Test
    void noClauseMeansAlwaysVisible() {
        assertTrue(VisibilityCondition.ALWAYS.isAlways());
        assertFalse(new VisibilityCondition(VisibilityCondition.Operator.OR, List.of(clause(List.of("true"))))
                .isAlways());
    }

    @Test
    void bindingAttributesDefaultToEmpty() {
        final ActionBinding binding =
                new ActionBinding(ActionTypes.UPDATE, "refresh", List.of("a", "b"), Map.of("after", "name"));

        assertEquals("name", binding.after());
        assertEquals("", binding.activeIf());
        assertEquals(List.of("a", "b"), binding.parameters());
    }

    @Test
    void constraintsAreImmutableValues() {
        final Constraints constraints =
                Constraints.NONE.withRequired(true).withMin(-1).withEnumValues(List.of("A", "B"));

        assertFalse(Constraints.NONE.required());
        assertEquals(OptionalInt.empty(), Constraints.NONE.min());
        assertTrue(constraints.required());
        assertEquals(OptionalInt.of(-1), constraints.min());
        assertEquals(OptionalInt.empty(), constraints.max());
        assertEquals(List.of("A", "B"), constraints.enumValues());
    }

    @Test
    void configurationTypeIsPresentOnlyWhenTyped() {
        assertFalse(ConfigurationTypeRef.NONE.isPresent());
        assertTrue(new ConfigurationTypeRef("datastore", "default").isType("datastore"));
        assertFalse(new ConfigurationTypeRef("datastore", "default").isType("dataset"));
    }

    private static ConditionClause clause(final List<String> values) {
        return new ConditionClause("flag", EvaluationStrategy.DEFAULT, Map.of(), values, false);
    }
}
