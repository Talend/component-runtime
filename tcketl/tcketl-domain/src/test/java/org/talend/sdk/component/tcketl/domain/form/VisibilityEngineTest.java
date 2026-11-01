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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.talend.sdk.component.tcketl.domain.form.Props.bool;
import static org.talend.sdk.component.tcketl.domain.form.Props.map;
import static org.talend.sdk.component.tcketl.domain.form.Props.object;
import static org.talend.sdk.component.tcketl.domain.form.Props.string;
import static org.talend.sdk.component.tcketl.domain.form.Props.tree;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.tcketl.domain.property.ConditionClause;
import org.talend.sdk.component.tcketl.domain.property.EvaluationStrategy;
import org.talend.sdk.component.tcketl.domain.property.VisibilityCondition;

/** The {@code @ActiveIf} conditions, evaluated on the values of a form. */
class VisibilityEngineTest {

    private static ConditionClause clause(final String target, final EvaluationStrategy strategy,
            final boolean negate, final String... values) {
        return new ConditionClause(target, strategy, Map.of(), List.of(values), negate);
    }

    private static VisibilityCondition when(final VisibilityCondition.Operator operator,
            final ConditionClause... clauses) {
        return new VisibilityCondition(operator, List.of(clauses));
    }

    private static VisibilityEngine engine(final PropertyTree tree, final String... keysAndValues) {
        return new VisibilityEngine(tree, FormValues.of(tree, map(keysAndValues)).effective());
    }

    @Test
    void aSiblingFlagShowsAndHidesAField() {
        final PropertyTree tree = tree(object("cfg"), bool("cfg.flag"), string("cfg.name")
                .visibility(when(VisibilityCondition.Operator.AND,
                        clause("flag", EvaluationStrategy.DEFAULT, false, "true"))));

        assertFalse(engine(tree).isVisible("cfg.name"));
        assertTrue(engine(tree, "cfg.flag", "true").isVisible("cfg.name"));
        assertTrue(engine(tree).isVisible("cfg.flag"));
    }

    @Test
    void negateInvertsTheClauseAndOrNeedsOnlyOneClause() {
        final PropertyTree tree = tree(object("cfg"), string("cfg.kind"), string("cfg.other"),
                string("cfg.a").visibility(
                        when(VisibilityCondition.Operator.AND, clause("kind", EvaluationStrategy.DEFAULT, true, "X"))),
                string("cfg.b").visibility(when(VisibilityCondition.Operator.OR,
                        clause("kind", EvaluationStrategy.DEFAULT, false, "X"),
                        clause("other", EvaluationStrategy.DEFAULT, false, "Y"))));

        assertTrue(engine(tree).isVisible("cfg.a"));
        assertFalse(engine(tree, "cfg.kind", "X").isVisible("cfg.a"));
        assertFalse(engine(tree).isVisible("cfg.b"));
        assertTrue(engine(tree, "cfg.other", "Y").isVisible("cfg.b"));
    }

    @Test
    void lengthAndContainsStrategies() {
        final PropertyTree tree = tree(object("cfg"), string("cfg.text"), string("cfg.empty").visibility(
                when(VisibilityCondition.Operator.AND, clause("text", EvaluationStrategy.LENGTH, false, "0"))),
                string("cfg.has").visibility(when(VisibilityCondition.Operator.AND,
                        clause("text", EvaluationStrategy.CONTAINS, false, "ab"))));

        assertTrue(engine(tree).isVisible("cfg.empty"));
        assertFalse(engine(tree, "cfg.text", "xabx").isVisible("cfg.empty"));
        assertTrue(engine(tree, "cfg.text", "xabx").isVisible("cfg.has"));
        assertFalse(engine(tree, "cfg.text", "xx").isVisible("cfg.has"));
    }

    @Test
    void anHiddenObjectHidesItsWholeSubtree() {
        final PropertyTree tree = tree(object("cfg"), bool("cfg.on"), object("cfg.group").visibility(
                when(VisibilityCondition.Operator.AND, clause("../on", EvaluationStrategy.DEFAULT, false, "true"))),
                string("cfg.group.leaf"));

        assertFalse(engine(tree).isVisible("cfg.group.leaf"));
        assertTrue(engine(tree, "cfg.on", "true").isVisible("cfg.group.leaf"));
    }

    @Test
    void anUnsupportedStrategyNeverHides() {
        final PropertyTree tree = tree(object("cfg"), string("cfg.x"), string("cfg.y").visibility(
                when(VisibilityCondition.Operator.AND, clause("x", EvaluationStrategy.UNSUPPORTED, false, "1"))));

        assertEquals(true, engine(tree).isVisible("cfg.y"));
    }
}
