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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.tcketl.domain.connector.ConnectorDefinition;
import org.talend.sdk.component.tcketl.domain.connector.ConnectorRole;
import org.talend.sdk.component.tcketl.domain.connector.ConnectorSummary;
import org.talend.sdk.component.tcketl.domain.connector.Port;
import org.talend.sdk.component.tcketl.domain.property.ConditionClause;
import org.talend.sdk.component.tcketl.domain.property.EvaluationStrategy;
import org.talend.sdk.component.tcketl.domain.property.GridLayout;
import org.talend.sdk.component.tcketl.domain.property.PropertyDefinition;
import org.talend.sdk.component.tcketl.domain.property.VisibilityCondition;

/** Checks the mapping on answers recorded from a running component server (the {@code real-} fixtures). */
class RealServerPayloadsTest {

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

    private static List<List<String>> rows(final PropertyDefinition property, final String pane) {
        return property.layout().grid(pane).map(GridLayout::rows).orElseThrow();
    }

    @Test
    void aSinkIsReportedAsAProcessorByTheIndexAndResolvedFromItsFlows() {
        final List<ConnectorSummary> summaries = Fixtures
                .wire("real-component-index.json", Wire.ComponentIndexes.class)
                .components()
                .stream()
                .map(ConnectorMapper::summary)
                .map(Optional::orElseThrow)
                .collect(Collectors.toList());
        assertEquals(6, summaries.size());
        assertEquals(Set.of("the_family", "sampleForm", "sampleFixedSchema"),
                summaries.stream().map(ConnectorSummary::family).collect(Collectors.toSet()));

        final ConnectorSummary sink = summaries
                .stream()
                .filter(summary -> summary.displayName().contains("Output 1"))
                .findFirst()
                .orElseThrow();
        assertEquals(ConnectorRole.PROCESSOR, sink.provisionalRole());

        final ConnectorDefinition definition = definition("real-details-the-family-output.json");
        assertEquals(sink.id(), definition.id());
        assertEquals(ConnectorRole.OUTPUT, definition.role());
        assertEquals(List.of(Port.input(Port.DEFAULT_FLOW)), definition.inputPorts());
        assertTrue(definition.outputPorts().isEmpty());
    }

    @Test
    void aProcessorOffersOnePortPerNamedFlow() {
        final ConnectorDefinition definition = definition("real-details-fixed-schema-processor.json");

        assertEquals(ConnectorRole.PROCESSOR, definition.role());
        assertEquals(List.of("main"), definition.inputPorts().stream().map(Port::label).collect(Collectors.toList()));
        assertEquals(List.of("main", "second", "third"),
                definition.outputPorts().stream().map(Port::label).collect(Collectors.toList()));
    }

    @Test
    void theFullSampleFormKeepsItsNestedPanesConditionsAndActions() {
        final ConnectorDefinition definition = definition("real-details-sample-form-input.json");
        final Map<String, PropertyDefinition> properties = byPath(definition);

        assertEquals(ConnectorRole.INPUT, definition.role());
        assertEquals(102, properties.size());

        // the same pane names are declared on every level of the datastore chain
        assertEquals(Set.of("Main", "Advanced"),
                properties
                        .get("config")
                        .layout()
                        .grids()
                        .stream()
                        .map(GridLayout::name)
                        .collect(Collectors.toSet()));
        assertEquals(List.of(List.of("datastore"), List.of("entity"), List.of("schema")),
                rows(properties.get("config.dataset"), "Main"));
        assertEquals(List.of(List.of("datastore")), rows(properties.get("config.dataset"), "Advanced"));
        assertEquals(List.of(List.of("login", "password"), List.of("healthcheckOk")),
                rows(properties.get("config.dataset.datastore"), "Main"));
        assertEquals(List.of(List.of("timeout")), rows(properties.get("config.dataset.datastore"), "Advanced"));

        assertEquals(12,
                properties.values().stream().filter(property -> !property.visibility().isAlways()).count());
        final VisibilityCondition both = properties.get("config.conditionalDisplay.fifthConditionString").visibility();
        assertEquals(VisibilityCondition.Operator.AND, both.operator());
        assertEquals(List.of(EvaluationStrategy.DEFAULT, EvaluationStrategy.CONTAINS),
                both.clauses().stream().map(ConditionClause::strategy).collect(Collectors.toList()));
        assertEquals(VisibilityCondition.Operator.OR,
                properties.get("config.conditionalDisplay.sixthConditionString").visibility().operator());

        assertTrue(properties.get("config.dataset.datastore").action("healthcheck").isPresent());
        assertEquals(Set.of("update/UPDATABLE", "schema/DISCOVERSCHEMA", "suggestions/SUGGESTABLE",
                "healthcheck/HEALTHCHECK", "validation/ASYNC_VALIDATION", "validation/ASYNC_VALIDATION2"),
                definition
                        .actions()
                        .stream()
                        .map(action -> action.type() + "/" + action.name())
                        .collect(Collectors.toSet()));
    }
}
