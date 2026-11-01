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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.tcketl.domain.connector.ConnectorDefinition;
import org.talend.sdk.component.tcketl.domain.connector.ConnectorId;
import org.talend.sdk.component.tcketl.domain.connector.ConnectorRef;
import org.talend.sdk.component.tcketl.domain.connector.ConnectorRole;
import org.talend.sdk.component.tcketl.domain.connector.ConnectorSummary;
import org.talend.sdk.component.tcketl.domain.connector.Port;
import org.talend.sdk.component.tcketl.domain.property.ActionBinding;
import org.talend.sdk.component.tcketl.domain.property.ActionDefinition;
import org.talend.sdk.component.tcketl.domain.property.ConditionClause;
import org.talend.sdk.component.tcketl.domain.property.ConfigurationTypeRef;
import org.talend.sdk.component.tcketl.domain.property.Constraints;
import org.talend.sdk.component.tcketl.domain.property.EvaluationStrategy;
import org.talend.sdk.component.tcketl.domain.property.GridLayout;
import org.talend.sdk.component.tcketl.domain.property.PropertyDefinition;
import org.talend.sdk.component.tcketl.domain.property.PropertyType;
import org.talend.sdk.component.tcketl.domain.property.UiHints;
import org.talend.sdk.component.tcketl.domain.property.VisibilityCondition;

class ConnectorMapperTest {

    private static final String SAMPLE_INPUT = "sample-component-details-sample-form-input.json";

    private static final String SAMPLE_INPUT_ID = "Y29uZmlndXJhdGlvbkZvcm1TYW1wbGUjc2FtcGxlRm9ybSNJbnB1dA";

    private static ConnectorDefinition definition(final String fixture) {
        return ConnectorMapper.definition(Fixtures.wire(fixture, Wire.ComponentDetails.class).details().get(0));
    }

    private static Map<String, PropertyDefinition> byPath(final ConnectorDefinition definition) {
        return definition
                .properties()
                .stream()
                .collect(Collectors
                        .toMap(PropertyDefinition::path, Function.identity(), (first, second) -> first,
                                LinkedHashMap::new));
    }

    private static ConditionClause clause(final String target, final EvaluationStrategy strategy,
            final Map<String, String> options, final boolean negate, final String... values) {
        return new ConditionClause(target, strategy, options, List.of(values), negate);
    }

    private static VisibilityCondition condition(final VisibilityCondition.Operator operator,
            final ConditionClause... clauses) {
        return new VisibilityCondition(operator, List.of(clauses));
    }

    @Test
    void everyIndexEntryBecomesASummaryWithItsProvisionalRole() {
        final List<ConnectorSummary> summaries = Fixtures
                .wire("sample-component-index.json", Wire.ComponentIndexes.class)
                .components()
                .stream()
                .map(ConnectorMapper::summary)
                .map(Optional::orElseThrow)
                .toList();

        assertEquals(List.of("Client", "Input", "AdlsGen2Output"),
                summaries.stream().map(ConnectorSummary::name).toList());
        assertEquals(List.of(ConnectorRole.PROCESSOR, ConnectorRole.INPUT, ConnectorRole.PROCESSOR),
                summaries.stream().map(ConnectorSummary::provisionalRole).toList());
        assertEquals(List.of(5, 1, 3), summaries.stream().map(summary -> summary.ref().version()).toList());

        final ConnectorSummary input = summaries.get(1);
        assertEquals(new ConnectorRef(new ConnectorId(SAMPLE_INPUT_ID), "sampleForm", "Input",
                "configurationFormSample", 1), input.ref());
        assertEquals("Y29uZmlndXJhdGlvbkZvcm1TYW1wbGUjc2FtcGxlRm9ybQ", input.familyId());
        assertEquals("sampleForm", input.familyDisplayName());
        assertEquals("Input", input.displayName());
        assertEquals(List.of("sample/sampleForm"), input.categories());
        assertFalse(input.streaming());
        assertEquals("A connector that returns only 1 record reflecting the configuration.", input.documentation());
        assertEquals("HTTP Client connector", summaries.get(0).displayName());
        assertEquals("Azure", summaries.get(2).familyDisplayName());
    }

    @Test
    void theIndexTypeOnlyGivesAProvisionalRole() {
        assertEquals(ConnectorRole.INPUT, ConnectorMapper.provisionalRole("input"));
        assertEquals(ConnectorRole.INPUT, ConnectorMapper.provisionalRole("INPUT"));
        assertEquals(ConnectorRole.STANDALONE, ConnectorMapper.provisionalRole("standalone"));
        assertEquals(ConnectorRole.PROCESSOR, ConnectorMapper.provisionalRole("processor"));
        assertEquals(ConnectorRole.PROCESSOR, ConnectorMapper.provisionalRole("brand-new-kind"));
        assertEquals(ConnectorRole.PROCESSOR, ConnectorMapper.provisionalRole(null));
    }

    @Test
    void aRecordedDetailsPayloadBecomesADefinitionWhoseRoleComesFromItsFlows() {
        final ConnectorDefinition definition = definition(SAMPLE_INPUT);

        assertEquals(new ConnectorRef(new ConnectorId(SAMPLE_INPUT_ID), "sampleForm", "Input",
                "configurationFormSample", 1), definition.ref());
        assertEquals("Input", definition.displayName());
        assertEquals(ConnectorRole.INPUT, definition.role());
        assertEquals(List.of(), definition.inputFlows());
        assertEquals(List.of(Port.DEFAULT_FLOW), definition.outputFlows());
        assertFalse(definition.streaming());
        assertFalse(definition.optionalRow());
        assertEquals("A connector that returns only 1 record reflecting the configuration.",
                definition.documentation());
        assertEquals(List.of("config", "config.allSupportedTypes", "config.allSupportedTypes.aBigDecimal"),
                List.copyOf(byPath(definition).keySet()));
    }

    @Test
    void theLayoutsConditionAndDocumentationOfRecordedPropertiesAreMapped() {
        final Map<String, PropertyDefinition> properties = byPath(definition(SAMPLE_INPUT));

        final PropertyDefinition config = properties.get("config");
        assertEquals(PropertyType.OBJECT, config.type());
        assertTrue(config.isParameter());
        assertEquals(0, config.parameterIndex());
        assertEquals(List.of("Advanced", "Main"), config.layout().grids().stream().map(GridLayout::name).toList());
        assertEquals(List.of(List.of("dataset")), config.layout().grid("Advanced").orElseThrow().rows());
        final List<List<String>> main = config.layout().grid("Main").orElseThrow().rows();
        assertEquals(13, main.size());
        assertEquals(List.of("dataset"), main.get(0));
        assertEquals(List.of("displayAllSupportedTypes"), main.get(1));
        assertEquals(List.of("dynamicElements"), main.get(12));

        final PropertyDefinition types = properties.get("config.allSupportedTypes");
        assertEquals("All supported types", types.displayName());
        assertEquals("A complex configuration with all supported types.", types.documentation());
        assertEquals(condition(VisibilityCondition.Operator.AND,
                clause("displayAllSupportedTypes", EvaluationStrategy.DEFAULT, Map.of(), false, "true")),
                types.visibility());
        assertEquals(12, types.layout().grid("Main").orElseThrow().cells().size());

        final PropertyDefinition decimal = properties.get("config.allSupportedTypes.aBigDecimal");
        assertEquals("aBigDecimal", decimal.name());
        assertEquals(PropertyType.STRING, decimal.type());
        assertEquals("A BigDecimal value.", decimal.documentation());
        assertEquals("", decimal.defaultValue());
        assertTrue(decimal.visibility().isAlways());
        assertFalse(decimal.isParameter());
    }

    @Test
    void theRecordedActionKeepsItsParametersInTheirDeclaredOrder() {
        final List<ActionDefinition> actions = definition(SAMPLE_INPUT).actions();

        assertEquals(1, actions.size());
        final ActionDefinition action = actions.get(0);
        assertEquals("update", action.type());
        assertEquals("UPDATABLE", action.name());
        assertEquals("sampleForm", action.family());
        assertEquals("Updatable", action.displayName());
        assertEquals(List.of("singleString", "suggestedElement"),
                action.parameters().stream().map(PropertyDefinition::name).toList());
    }

    @Test
    void aProcessorWithARejectFlowKeepsBothOutputsInServerOrder() {
        final ConnectorDefinition definition = definition("synthetic-details-widgets.json");

        assertEquals(ConnectorRole.PROCESSOR, definition.role());
        assertEquals(3, definition.ref().version());
        assertEquals(List.of(Port.DEFAULT_FLOW), definition.inputFlows());
        assertEquals(List.of(Port.DEFAULT_FLOW, Port.REJECT_FLOW), definition.outputFlows());
        assertEquals("Exercises every kind of widget.", definition.documentation());
    }

    @Test
    void theRootObjectKeepsItsPanesAndItsOptionsOrder() {
        final PropertyDefinition root = byPath(definition("synthetic-details-widgets.json")).get("configuration");

        assertEquals(List.of("Main", "Advanced"), root.layout().grids().stream().map(GridLayout::name).toList());
        assertEquals(
                List.of(List.of("mode"), List.of("login", "password"), List.of("script"), List.of("tags"),
                        List.of("fields"), List.of("startDate"), List.of("file")),
                root.layout().grid("Main").orElseThrow().rows());
        assertEquals(List.of(List.of("timeout"), List.of("$maxRecords")),
                root.layout().grid("Advanced").orElseThrow().rows());
        assertEquals(List.of("mode", "login", "password", "script", "tags", "fields", "startDate", "file", "timeout"),
                root.layout().optionsOrder());
        assertEquals(0, root.parameterIndex());
    }

    @Test
    void validationBecomesConstraints() {
        final Map<String, PropertyDefinition> properties = byPath(definition("synthetic-details-widgets.json"));

        assertEquals(Constraints.NONE.withRequired(true).withMinLength(3).withMaxLength(20).withPattern("^[a-z]+$"),
                properties.get("configuration.login").constraints());
        assertEquals(Constraints.NONE.withMinItems(1).withUniqueItems(true),
                properties.get("configuration.tags").constraints());
        assertEquals(Constraints.NONE.withMaxItems(10), properties.get("configuration.fields").constraints());
        assertEquals(Constraints.NONE.withMin(0).withMax(600), properties.get("configuration.timeout").constraints());
        assertTrue(properties.get("configuration.fields[].name").constraints().required());
        assertEquals(Constraints.NONE, properties.get("configuration.script").constraints());
    }

    @Test
    void enumsKeepTheServerOrderOfTheirValuesAndLabels() {
        final Map<String, PropertyDefinition> properties = byPath(definition("synthetic-details-widgets.json"));

        final PropertyDefinition kind = properties.get("configuration.fields[].kind");
        assertEquals(PropertyType.ENUM, kind.type());
        assertEquals(List.of("TEXT", "NUMBER"), kind.constraints().enumValues());
        assertEquals(List.of("TEXT", "NUMBER"), List.copyOf(kind.proposalLabels().keySet()));
        assertEquals("Text", kind.proposalLabels().get("TEXT"));

        final PropertyDefinition mode = properties.get("configuration.mode");
        assertEquals("FAST", mode.defaultValue());
        assertEquals(Map.of("FAST", "Fast", "SLOW", "Slow"), mode.proposalLabels());
    }

    @Test
    void arrayDefaultsStayJsonTextAndTheIndexPlaceholderIsLeftToTheRenderer() {
        final Map<String, PropertyDefinition> properties = byPath(definition("synthetic-details-widgets.json"));

        assertEquals("[\"a\",\"b\"]", properties.get("configuration.tags").defaultValue());
        final PropertyDefinition element = properties.get("configuration.tags[]");
        assertEquals("tags[]", element.name());
        assertEquals("Tag ${index}", element.displayName());
        assertEquals("Tag ${index}", element.placeholder());
    }

    @Test
    void widgetHintsAreReadFromTheMetadata() {
        final Map<String, PropertyDefinition> properties = byPath(definition("synthetic-details-widgets.json"));

        assertTrue(properties.get("configuration.password").ui().credential());
        assertEquals("sql", properties.get("configuration.script").ui().codeLanguage());
        assertEquals(UiHints.PathKind.FILE, properties.get("configuration.file").ui().path());
        assertTrue(properties.get("configuration.timeout").ui().readOnly());
        final UiHints date = properties.get("configuration.startDate").ui();
        assertEquals(UiHints.DateTimeKind.ZONED_DATE_TIME, date.dateTime());
        assertEquals("yyyy-MM-dd", date.dateFormat());
        assertFalse(date.useSeconds());
        assertTrue(date.useUtc());
        assertEquals(UiHints.NONE, properties.get("configuration.login").ui());
        assertEquals("Who connects.", properties.get("configuration.login").documentation());
    }

    @Test
    void technicalOptionsAreOrdinaryProperties() {
        final PropertyDefinition maxRecords =
                byPath(definition("synthetic-details-widgets.json")).get("configuration.$maxRecords");

        assertEquals("$maxRecords", maxRecords.name());
        assertEquals(PropertyType.NUMBER, maxRecords.type());
        assertEquals("-1", maxRecords.defaultValue());
    }

    @Test
    void aTypeAddedByANewerServerIsEditedAsTextAndItsMetadataIsKept() {
        final PropertyDefinition future =
                byPath(definition("synthetic-details-widgets.json")).get("configuration.future");

        assertEquals(PropertyType.STRING, future.type());
        assertEquals("Added by a newer server", future.displayName());
        assertEquals(Map.of("ui::somethingnew::value", "ignored"), future.metadata());
    }

    @Test
    void conditionsKeepTheirOperatorStrategiesAndNegation() {
        final ConnectorDefinition definition = definition("synthetic-details-conditions.json");
        final Map<String, PropertyDefinition> properties = byPath(definition);

        assertEquals(ConnectorRole.INPUT, definition.role());
        assertEquals(
                condition(VisibilityCondition.Operator.AND,
                        clause("str", EvaluationStrategy.DEFAULT, Map.of(), false, "value"),
                        clause("flag", EvaluationStrategy.DEFAULT, Map.of(), false, "true")),
                properties.get("conf.activeIfAnd").visibility());
        assertEquals(
                condition(VisibilityCondition.Operator.OR,
                        clause("../str", EvaluationStrategy.CONTAINS, Map.of("lowercase", "true"), false, "a", "b"),
                        clause("flag", EvaluationStrategy.DEFAULT, Map.of(), true, "true")),
                properties.get("conf.activeIfOr").visibility());
        assertEquals(
                condition(VisibilityCondition.Operator.AND,
                        clause("flag", EvaluationStrategy.DEFAULT, Map.of(), false, "true")),
                properties.get("conf.activeIfBool").visibility());
        assertEquals(
                condition(VisibilityCondition.Operator.AND,
                        clause("strLength", EvaluationStrategy.LENGTH, Map.of(), false, "7")),
                properties.get("conf.activeIfLength").visibility());
        assertEquals(
                condition(VisibilityCondition.Operator.AND,
                        clause("strNegate", EvaluationStrategy.DEFAULT, Map.of(), true, "value")),
                properties.get("conf.activeIfNegate").visibility());
        assertTrue(properties.get("conf.flag").visibility().isAlways());
    }

    @Test
    void actionsAreMappedAndTheOnesWithoutANameAreSkipped() {
        final ConnectorDefinition definition = definition("synthetic-details-actions.json");

        assertTrue(definition.streaming());
        assertTrue(definition.optionalRow());
        assertEquals(2, definition.ref().version());
        assertEquals(List.of("checkConnection", "guessMe", "listTables", "discover"),
                definition.actions().stream().map(ActionDefinition::name).toList());
        assertEquals(List.of("healthcheck", "update", "dynamic_values", "schema"),
                definition.actions().stream().map(ActionDefinition::type).toList());

        final ActionDefinition healthcheck = definition.actions().get(0);
        assertEquals("synthetic", healthcheck.family());
        assertEquals("Check the connection", healthcheck.displayName());
        assertEquals(List.of("datastore", "datastore.url"),
                healthcheck.properties().stream().map(PropertyDefinition::path).toList());
        assertEquals(List.of("datastore"),
                healthcheck.parameters().stream().map(PropertyDefinition::path).toList());
        assertEquals(List.of(), definition.actions().get(2).parameters());
    }

    @Test
    void actionBindingsAndTheirContextAreMappedOntoTheProperties() {
        final Map<String, PropertyDefinition> properties = byPath(definition("synthetic-details-actions.json"));

        assertEquals(List.of(new ActionBinding("schema", "discover")), properties.get("root").actions());

        final PropertyDefinition datastore = properties.get("root.datastore");
        assertEquals(List.of(new ActionBinding("healthcheck", "checkConnection")), datastore.actions());
        assertEquals(new ConfigurationTypeRef("datastore", "synthetic"), datastore.configurationType());

        assertEquals(
                List.of(new ActionBinding("dynamic_values", "listTables"),
                        new ActionBinding("validation", "checkDriver", List.of("."), Map.of())),
                properties.get("root.datastore.driver").actions());
        assertEquals(
                List.of(new ActionBinding("suggestions", "suggestUrls", List.of("../driver", "."),
                        Map.of("cacheable", "true"))),
                properties.get("root.datastore.url").actions());
        assertEquals(List.of(new ActionBinding("update", "guessMe", List.of("."),
                Map.of("after", "id", "activeIf", "target = setBoolean, value = { true }"))),
                properties.get("root.updatable").actions());

        final UiHints columns = properties.get("root.columns").ui();
        assertEquals(UiHints.StructureDirection.OUT, columns.structure());
        assertEquals("root.columns", columns.structureConnection());
        assertEquals("discover", columns.discoverSchema());
    }

    @Test
    void anIndexEntryThatCannotBeIdentifiedIsNotAConnector() {
        final List<Wire.ComponentIndex> entries =
                Fixtures.wireJson("""
                                  {"components": [
                                    {"displayName": "no id"},
                                    {"id": {"family": "f", "id": "i"}, "displayName": "no name"},
                                    {"id": {"family": "f", "id": "i", "name": "n"}, "version": -1,
                                     "metadata": {"mapper::infinite": "true", "documentation::value": null}}
                                  ]}""", Wire.ComponentIndexes.class).components();

        assertTrue(ConnectorMapper.summary(null).isEmpty());
        assertTrue(ConnectorMapper.summary(entries.get(0)).isEmpty());
        assertTrue(ConnectorMapper.summary(entries.get(1)).isEmpty());

        final ConnectorSummary summary = ConnectorMapper.summary(entries.get(2)).orElseThrow();
        assertEquals(0, summary.ref().version());
        assertTrue(summary.streaming());
        assertEquals("", summary.documentation());
        assertEquals("f", summary.familyDisplayName());
        assertEquals("n", summary.displayName());
        assertEquals(ConnectorRole.PROCESSOR, summary.provisionalRole());
    }

    @Test
    void aDefinitionNeedsAnIdentity() {
        final Wire.ComponentDetail detail = Fixtures
                .wireJson("{\"details\": [{\"id\": {\"family\": \"f\", \"id\": \"i\"}}]}", Wire.ComponentDetails.class)
                .details()
                .get(0);

        final IllegalArgumentException error =
                assertThrows(IllegalArgumentException.class, () -> ConnectorMapper.definition(detail));

        assertEquals("the component identity is incomplete", error.getMessage());
    }

    @Test
    void incompletePartsOfADefinitionAreDroppedInsteadOfFailingTheWholeConnector() {
        final Wire.ComponentDetail detail = Fixtures.wireJson(
                """
                {"details": [{
                  "id": {"family": "f", "id": "i", "name": "n", "plugin": "p"},
                  "version": -3,
                  "inputFlows": ["", null, "in"],
                  "properties": [
                    {"name": "no path", "type": "STRING"},
                    {"path": "a.b.c", "name": " ", "type": "number", "metadata": {"keep": "1", "dropped": null}}
                  ],
                  "actions": [null, {"family": "f", "type": "user"}, {"family": "f", "name": "x", "type": " "}]
                }]}""", Wire.ComponentDetails.class).details().get(0);

        final ConnectorDefinition definition = ConnectorMapper.definition(detail);

        assertEquals(0, definition.ref().version());
        assertEquals("n", definition.displayName());
        assertEquals(List.of("in"), definition.inputFlows());
        assertEquals(List.of(), definition.outputFlows());
        assertEquals(List.of(), definition.actions());
        assertEquals(1, definition.properties().size());
        final PropertyDefinition property = definition.properties().get(0);
        assertEquals("a.b.c", property.path());
        assertEquals("c", property.name());
        assertEquals(PropertyType.NUMBER, property.type());
        assertEquals(Map.of("keep", "1"), property.metadata());
    }
}
