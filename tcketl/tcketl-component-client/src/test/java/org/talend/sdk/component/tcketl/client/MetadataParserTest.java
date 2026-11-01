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
package org.talend.sdk.component.tcketl.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.tcketl.domain.property.ActionBinding;
import org.talend.sdk.component.tcketl.domain.property.ConditionClause;
import org.talend.sdk.component.tcketl.domain.property.ConfigurationTypeRef;
import org.talend.sdk.component.tcketl.domain.property.EvaluationStrategy;
import org.talend.sdk.component.tcketl.domain.property.GridLayout;
import org.talend.sdk.component.tcketl.domain.property.LayoutHints;
import org.talend.sdk.component.tcketl.domain.property.PropertyDefinition;
import org.talend.sdk.component.tcketl.domain.property.UiHints;
import org.talend.sdk.component.tcketl.domain.property.VisibilityCondition;

class MetadataParserTest {

    private static Map<String, String> meta(final String... keysAndValues) {
        final Map<String, String> metadata = new LinkedHashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            metadata.put(keysAndValues[i], keysAndValues[i + 1]);
        }
        return metadata;
    }

    private static ConditionClause onlyClause(final Map<String, String> metadata) {
        final List<ConditionClause> clauses = MetadataParser.visibility(metadata).clauses();
        assertEquals(1, clauses.size());
        return clauses.get(0);
    }

    private static List<String> targets(final VisibilityCondition condition) {
        return condition.clauses().stream().map(ConditionClause::target).toList();
    }

    @Test
    void panesKeepTheServerOrderAndTheirRowsAreNormalised() {
        final LayoutHints layout = MetadataParser
                .layout(meta("ui::gridlayout::Main::value", " a , b |c||d,,a,d ", "ui::gridlayout::Advanced::value",
                        "e", "ui::gridlayout::Empty::value", " | ", "ui::gridlayout:: ::value", "ignored",
                        "ui::gridlayout::::value", "ignored", "ui::gridlayout::Main::other", "ignored",
                        "ui::optionsorder::value", "z, a,,z,b"));

        assertEquals(List.of("Main", "Advanced"), layout.grids().stream().map(GridLayout::name).toList());
        assertEquals(List.of(List.of("a", "b"), List.of("c"), List.of("d", "a")),
                layout.grid("main").orElseThrow().rows());
        assertEquals(List.of(List.of("e")), layout.grid("Advanced").orElseThrow().rows());
        assertEquals(List.of("z", "a", "b"), layout.optionsOrder());
    }

    @Test
    void noLayoutKeyMeansNoLayout() {
        assertTrue(MetadataParser.layout(meta("ui::hidden", "true")).isEmpty());
    }

    @Test
    void theDefaultDeclaredInTheMetadataWinsEvenWhenBlank() {
        assertEquals("declared", MetadataParser.defaultValue(meta("ui::defaultvalue::value", "declared"), "reported"));
        assertEquals("", MetadataParser.defaultValue(meta("ui::defaultvalue::value", ""), "reported"));
        assertEquals("reported", MetadataParser.defaultValue(meta(), "reported"));
        assertEquals("", MetadataParser.defaultValue(meta(), null));
    }

    @Test
    void theParameterIndexIsANumberOrThePropertyIsNotAParameter() {
        assertEquals(2, MetadataParser.parameterIndex(meta("definition::parameter::index", " 2 ")));
        assertEquals(PropertyDefinition.NOT_A_PARAMETER,
                MetadataParser.parameterIndex(meta("definition::parameter::index", "x")));
        assertEquals(PropertyDefinition.NOT_A_PARAMETER, MetadataParser.parameterIndex(meta()));
    }

    @Test
    void aConfigurationTypeNeedsItsType() {
        assertEquals(new ConfigurationTypeRef("datastore", "sample"),
                MetadataParser
                        .configurationType(
                                meta("configurationtype::type", "datastore", "configurationtype::name", " sample ")));
        assertEquals(new ConfigurationTypeRef("dataset", ""),
                MetadataParser.configurationType(meta("configurationtype::type", "dataset")));
        assertEquals(ConfigurationTypeRef.NONE,
                MetadataParser.configurationType(meta("configurationtype::name", "orphan")));
    }

    @Test
    void flagsAndPathsOfTheUiAreRead() {
        final UiHints hints = MetadataParser
                .ui(meta("ui::hidden", "true", "ui::readonly", "TRUE", "ui::credential", "true", "ui::textarea",
                        "false",
                        "ui::code::value", " sql ", "ui::path::value", "directory"));

        assertTrue(hints.hidden());
        assertTrue(hints.readOnly());
        assertTrue(hints.credential());
        assertFalse(hints.textArea());
        assertEquals("sql", hints.codeLanguage());
        assertEquals(UiHints.PathKind.DIRECTORY, hints.path());
        assertEquals(UiHints.NONE, MetadataParser.ui(meta()));
    }

    @Test
    void codeWithoutALanguageIsPlainText() {
        assertEquals("text", MetadataParser.ui(meta("ui::code::value", " ")).codeLanguage());
        assertEquals("", MetadataParser.ui(meta()).codeLanguage());
    }

    @Test
    void secondsAndUtcAreOnForDateTimesUnlessSwitchedOff() {
        final UiHints defaults = MetadataParser.ui(meta("ui::datetime", "zoneddatetime"));
        assertEquals(UiHints.DateTimeKind.ZONED_DATE_TIME, defaults.dateTime());
        assertTrue(defaults.useSeconds());
        assertTrue(defaults.useUtc());

        final UiHints custom = MetadataParser
                .ui(meta("ui::datetime", "date", "ui::datetime::dateFormat", "yyyy-MM-dd", "ui::datetime::useSeconds",
                        "false", "ui::datetime::useUTC", "FALSE"));
        assertEquals(UiHints.DateTimeKind.DATE, custom.dateTime());
        assertEquals("yyyy-MM-dd", custom.dateFormat());
        assertFalse(custom.useSeconds());
        assertFalse(custom.useUtc());

        assertEquals(UiHints.DateTimeKind.NONE, MetadataParser.ui(meta("ui::datetime", "unknown")).dateTime());
    }

    @Test
    void aStructureIsIncomingUnlessItIsDeclaredOutgoing() {
        final UiHints incoming = MetadataParser.ui(meta("ui::structure::value", " conf.columns "));
        assertEquals(UiHints.StructureDirection.IN, incoming.structure());
        assertEquals("conf.columns", incoming.structureConnection());

        final UiHints outgoing =
                MetadataParser.ui(meta("ui::structure::type", "out", "ui::structure::discoverSchema", "guess"));
        assertEquals(UiHints.StructureDirection.OUT, outgoing.structure());
        assertEquals("", outgoing.structureConnection());
        assertEquals("guess", outgoing.discoverSchema());

        assertEquals(UiHints.StructureDirection.NONE, MetadataParser.ui(meta()).structure());
    }

    @Test
    void withoutConditionKeysThePropertyIsAlwaysVisible() {
        assertTrue(MetadataParser.visibility(meta("ui::hidden", "true")).isAlways());
        assertTrue(MetadataParser
                .visibility(meta("condition::if::value", "x", "condition::if::negate", "true"))
                .isAlways());
    }

    @Test
    void aSingleConditionExpectsTrueByDefault() {
        final VisibilityCondition condition = MetadataParser.visibility(meta("condition::if::target", " ../flag "));

        assertEquals(VisibilityCondition.Operator.AND, condition.operator());
        assertEquals(List.of(new ConditionClause("../flag", EvaluationStrategy.DEFAULT, Map.of(), List.of("true"),
                false)), condition.clauses());
    }

    @Test
    void numberedConditionsFollowTheUnnumberedOneInNumericOrder() {
        final VisibilityCondition condition = MetadataParser
                .visibility(meta("condition::if::target::10", "ten", "condition::if::value::10", "10",
                        "condition::if::target::2", "two", "condition::if::value::2", "2", "condition::if::target",
                        "single", "condition::if::target::x", "ignored", "condition::if::target::1::2", "ignored",
                        "condition::ifs::operator", " or "));

        assertEquals(VisibilityCondition.Operator.OR, condition.operator());
        assertEquals(List.of("single", "two", "ten"), targets(condition));
    }

    @Test
    void aConditionWithoutTargetIsSkipped() {
        final VisibilityCondition condition = MetadataParser
                .visibility(meta("condition::if::target::0", " ", "condition::if::value::0", "x",
                        "condition::if::target::1", "kept"));

        assertEquals(List.of("kept"), targets(condition));
    }

    @Test
    void strategyOptionsAreLowerCasedAndMalformedOnesAreIgnored() {
        final ConditionClause clause = onlyClause(meta("condition::if::target", "t", "condition::if::value", "a,b",
                "condition::if::negate", "true", "condition::if::evaluationStrategy",
                "CONTAINS(lowercase=true, Other = x ,bad,=no)"));

        assertEquals(EvaluationStrategy.CONTAINS, clause.strategy());
        assertEquals(Map.of("lowercase", "true", "other", "x"), clause.strategyOptions());
        assertEquals(List.of("a", "b"), clause.values());
        assertTrue(clause.negate());
    }

    @Test
    void strategyNamesAreCaseInsensitiveAndAnUnknownOneIsUnsupported() {
        assertEquals(EvaluationStrategy.LENGTH,
                onlyClause(meta("condition::if::target", "t", "condition::if::evaluationStrategy", " length "))
                        .strategy());
        assertEquals(EvaluationStrategy.UNSUPPORTED,
                onlyClause(meta("condition::if::target", "t", "condition::if::evaluationStrategy", "REGEX"))
                        .strategy());
        assertEquals(EvaluationStrategy.DEFAULT,
                onlyClause(meta("condition::if::target", "t", "condition::if::evaluationStrategy", "")).strategy());
    }

    @Test
    void anEmptyExpectedValueIsAnEmptyString() {
        assertEquals(List.of(""),
                onlyClause(meta("condition::if::target", "t", "condition::if::value", "")).values());
        assertEquals(List.of(""),
                onlyClause(meta("condition::if::target", "t", "condition::if::value", ",")).values());
    }

    @Test
    void anActionIsBoundByNameWithItsParametersAndAttributes() {
        final List<ActionBinding> bindings = MetadataParser
                .actions(meta("action::update", "guessMe", "action::update::parameters", "., ../driver,,.",
                        "action::update::after", "id", "action::update::activeIf",
                        "target = setBoolean, value = { true }", "action::suggestions", "suggestUrls",
                        "action::suggestions::cacheable", "true", "action::orphan::after", "nowhere", "action::",
                        "ignored", "action::x::", "ignored", "ui::hidden", "true"));

        assertEquals(
                List.of(new ActionBinding("update", "guessMe", List.of(".", "../driver", "."),
                        Map.of("after", "id", "activeIf", "target = setBoolean, value = { true }")),
                        new ActionBinding("suggestions", "suggestUrls", List.of(), Map.of("cacheable", "true"))),
                bindings);
        assertEquals("id", bindings.get(0).after());
    }

    @Test
    void noActionKeyMeansNoAction() {
        assertTrue(MetadataParser.actions(meta("ui::hidden", "true")).isEmpty());
    }
}
