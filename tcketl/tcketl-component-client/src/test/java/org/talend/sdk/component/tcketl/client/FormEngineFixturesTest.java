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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.talend.sdk.component.tcketl.domain.form.FormLayout;
import org.talend.sdk.component.tcketl.domain.form.FormLayout.ButtonCell;
import org.talend.sdk.component.tcketl.domain.form.FormLayout.Cell;
import org.talend.sdk.component.tcketl.domain.form.FormLayout.CollectionCell;
import org.talend.sdk.component.tcketl.domain.form.FormLayout.FieldCell;
import org.talend.sdk.component.tcketl.domain.form.FormLayout.GroupCell;
import org.talend.sdk.component.tcketl.domain.form.FormLayout.Row;
import org.talend.sdk.component.tcketl.domain.form.FormLayout.Tab;
import org.talend.sdk.component.tcketl.domain.form.FormValues;
import org.talend.sdk.component.tcketl.domain.form.LayoutResolver;
import org.talend.sdk.component.tcketl.domain.form.PropertyKind;
import org.talend.sdk.component.tcketl.domain.form.PropertyNode;
import org.talend.sdk.component.tcketl.domain.form.PropertyTree;
import org.talend.sdk.component.tcketl.domain.form.Validator;
import org.talend.sdk.component.tcketl.domain.form.Visibility;

/**
 * Runs the form engine on server payloads, mapped the way the client maps them. A tab is written as one string per
 * row, cells separated by commas, a group as {@code name{rows separated by |}}, a list as {@code name[rows of an
 * entry]} and a button as {@code <KIND:action[->property]>}.
 */
class FormEngineFixturesTest {

    private static final String SAMPLE_FORM = "real-details-sample-form-input.json";

    private static final String THE_FAMILY_OUTPUT = "real-details-the-family-output.json";

    private static final String FIXED_SCHEMA = "real-details-fixed-schema-processor.json";

    private static final String WIDGETS = "synthetic-details-widgets.json";

    private static final String CONDITIONS = "synthetic-details-conditions.json";

    private static final String ACTIONS = "synthetic-details-actions.json";

    private static final String CONSTRAINTS = "config.elementsWithConstraints.";

    @Test
    void theSampleFormInputIsLaidOutInAMainAndAnAdvancedTab() {
        final FormLayout form = LayoutResolver.resolve(tree(SAMPLE_FORM));

        assertEquals(List.of("Main", "Advanced"), names(form));
        assertEquals(List.of(
                "dataset{datastore{login,password|healthcheckOk|<HEALTHCHECK:HEALTHCHECK>}|entity|schema[label|"
                        + "originalDbColumnName|key|type|talendType|nullable|pattern|length|precision|defaultValue|"
                        + "comment]|<GUESS_SCHEMA:DISCOVERSCHEMA->schema>}",
                "displayAllSupportedTypes",
                "allSupportedTypes{aString|aBoolean|aBigDecimal|aFloat|aBigInteger|anInteger|aFile|aInetAddress|aURI|"
                        + "aURL|aLocalDateTime|aZonedDateTime}",
                "displayConfiguredWidget",
                "configuredWidgets{stringWithDefault|multiline|sqlInput|jsonInput|aToken|folder|file|readOnly}",
                "displaySomeLists",
                "someLists{aListOfStrings[]|aListOfInts[]|aListOfBooleans[]|listOfAllSupportedTypes[aString|aBoolean|"
                        + "aBigDecimal|aFloat|aBigInteger|anInteger|aFile|aInetAddress|aURI|aURL|aLocalDateTime|"
                        + "aZonedDateTime]}",
                "displayElementsWithConstraints",
                "elementsWithConstraints{stringWithMaxLength|stringWithMinLength|stringWithPattern|intWithMin|"
                        + "intWithMax|aRequiredString|listWithMaxItems[key|value]|listWithMinItems[key|value]|"
                        + "listWithUniqueItems[key|value]}",
                "displayConditionalDisplay",
                "conditionalDisplay{aBoolean|aString|conditionString|secondConditionString|thirdConditionString|"
                        + "fourthConditionString|fifthConditionString|sixthConditionString}",
                "displayDynamicElements",
                "dynamicElements{singleString|someComplexConfig{aString|aBoolean|anInteger|<UPDATE:UPDATABLE>}|"
                        + "asyncValidation|suggestedElement}"),
                shape(form, "Main"));
        assertEquals(List.of("dataset{datastore{timeout}}"), shape(form, "Advanced"));
    }

    @Test
    void theNestedPanesOfTheFamilyOutputAreFollowedLevelByLevel() {
        final FormLayout form = LayoutResolver.resolve(tree(THE_FAMILY_OUTPUT));

        assertEquals(List.of("Main", "Advanced"), names(form));
        assertEquals(List.of("message",
                "dataset{entity|nestedConfigSrc{stringOption1|stringOption2}|"
                        + "nestedConfigDest{stringOption1|stringOption2|<UPDATE:action_UPDATE>}|"
                        + "datastore{url|auth{login|password}}}"),
                shape(form, "Main"));
        assertEquals(List.of("dataset{readAll|datastore{timeout|auth{nbAuthRetry}}}"), shape(form, "Advanced"));
    }

    @Test
    void aProcessorWithASinglePaneNeedsNoTabs() {
        final FormLayout form = LayoutResolver.resolve(tree(FIXED_SCHEMA));

        assertEquals(List.of("Main"), names(form));
        assertFalse(form.isTabbed());
        assertEquals(List.of("dse{dso{input}|anotherInput}", "aBoolean"), shape(form, "Main"));
    }

    @Test
    void aPaneNobodyKnowsBecomesATabAfterTheKnownOnes() {
        final JsonNode payload = Fixtures.readJson(THE_FAMILY_OUTPUT);
        final ArrayNode properties = (ArrayNode) payload.get("details").get(0).get("properties");
        properties.forEach(property -> {
            if ("configout".equals(property.get("path").asText())) {
                ((ObjectNode) property.get("metadata")).put("ui::gridlayout::Proxy::value", "proxyHost");
            }
        });
        final ObjectNode added = properties.addObject();
        added.put("path", "configout.proxyHost").put("name", "proxyHost").put("type", "STRING");
        added.putObject("metadata");

        final FormLayout form = LayoutResolver
                .resolve(PropertyTree
                        .of(ConnectorMapper
                                .definition(Fixtures.wireJson(payload.toString(), Wire.ComponentDetails.class)
                                        .details()
                                        .get(0))
                                .properties()));

        assertEquals(List.of("Main", "Advanced", "Proxy"), names(form));
        assertEquals(List.of("proxyHost"), shape(form, "Proxy"));
    }

    @ParameterizedTest
    @ValueSource(strings = { SAMPLE_FORM, THE_FAMILY_OUTPUT, FIXED_SCHEMA, WIDGETS, CONDITIONS, ACTIONS })
    void everyDisplayedPropertyIsLaidOutExactlyOnce(final String fixture) {
        final PropertyTree tree = tree(fixture);
        final List<String> expected = new ArrayList<>();
        tree.roots().forEach(root -> displayedLeaves(root, expected));

        final List<String> laidOut = LayoutResolver
                .resolve(tree)
                .tabs()
                .stream()
                .flatMap(tab -> tab.nodes().stream())
                .map(PropertyNode::path)
                .sorted()
                .toList();

        assertTrue(tree.warnings().isEmpty());
        assertFalse(expected.isEmpty());
        assertEquals(expected.stream().sorted().toList(), laidOut);
    }

    @Test
    void theButtonsFollowTheActionsDeclaredByThePayload() {
        final FormLayout form = LayoutResolver.resolve(tree(ACTIONS));

        assertEquals(List.of("Main"), names(form));
        assertEquals(List.of("datastore{driver,url|<HEALTHCHECK:checkConnection>}",
                "updatable{id|<UPDATE:guessMe>|name}", "columns[]", "<GUESS_SCHEMA:discover->columns>"),
                shape(form, "Main"));
    }

    @Test
    void defaultsFillOnlyWhatTheUserDidNotSet() {
        final PropertyTree tree = tree(SAMPLE_FORM);

        final FormValues fresh = FormValues.of(tree, Map.of());
        assertEquals("0", fresh.get("config.dataset.datastore.timeout"));
        assertEquals("false", fresh.get("config.dataset.datastore.healthcheckOk"));
        assertEquals("0.0", fresh.get("config.allSupportedTypes.aFloat"));
        assertEquals("A default value.", fresh.get("config.configuredWidgets.stringWithDefault"));
        assertEquals("A default for the hidden value.", fresh.get("config.configuredWidgets.hiddenString"));
        assertEquals("", fresh.get("config.dataset.datastore.login"));
        assertEquals(0, fresh.count("config.someLists.aListOfStrings"));

        final FormValues edited = FormValues
                .of(tree,
                        map("config.dataset.datastore.timeout", "15", "config.configuredWidgets.stringWithDefault",
                                ""));
        assertEquals("15", edited.get("config.dataset.datastore.timeout"));
        assertEquals("", edited.get("config.configuredWidgets.stringWithDefault"));

        final FormValues withEntry = fresh.append("config.dataset.schema");
        assertEquals(1, withEntry.count("config.dataset.schema"));
        assertEquals("false", withEntry.get("config.dataset.schema[0].key"));
        assertEquals("0", withEntry.get("config.dataset.schema[0].length"));
    }

    @Test
    void theDefaultsOfEveryKindAreWrittenOnTheFirstEdit() {
        final FormValues values = FormValues.of(tree(WIDGETS), Map.of());

        assertEquals(
                Map.of("configuration.$maxRecords", "-1", "configuration.mode", "FAST", "configuration.tags[0]", "a",
                        "configuration.tags[1]", "b", "configuration.timeout", "30"),
                values.toConfiguration());
    }

    @Test
    void anEditedFormEncodesWhatTheRuntimeExpectsAndEncodesItTheSameWayTwice() {
        final PropertyTree tree = tree(SAMPLE_FORM);
        final Map<String, String> stored = map("config.dataset.datastore.login", "john",
                "config.dataset.datastore.password", "secret", "config.dataset.datastore.timeout", "15",
                "config.dataset.datastore.$login_name", "only for the ui", "config.dataset.datastore.__version", "1",
                "config.dataset.entity", "PRODUCT", "config.dataset.legacy", "old", "config.dataset.schema[0].label",
                "id", "config.dataset.schema[0].key", "true", "config.dataset.schema[1].label", "name");

        final Map<String, String> encoded = FormValues.of(tree, stored).toConfiguration();

        assertEquals("john", encoded.get("config.dataset.datastore.login"));
        assertEquals("15", encoded.get("config.dataset.datastore.timeout"));
        assertEquals("true", encoded.get("config.dataset.schema[0].key"));
        assertEquals("false", encoded.get("config.dataset.schema[1].key"));
        assertEquals("0", encoded.get("config.dataset.schema[1].length"));
        assertEquals("old", encoded.get("config.dataset.legacy"));
        assertEquals("1", encoded.get("config.dataset.datastore.__version"));
        assertFalse(encoded.containsKey("config.dataset.datastore.$login_name"));
        assertEquals(encoded, FormValues.of(tree, encoded).toConfiguration());
    }

    @Test
    void theConstraintsOfThePayloadAreChecked() {
        final Map<String, String> stored = map(CONSTRAINTS + "stringWithMaxLength", "12345678901",
                CONSTRAINTS + "stringWithMinLength", "ab", CONSTRAINTS + "stringWithPattern", "startxEND",
                CONSTRAINTS + "intWithMin", "99", CONSTRAINTS + "intWithMax", "101",
                CONSTRAINTS + "listWithUniqueItems[0].key", "a", CONSTRAINTS + "listWithUniqueItems[0].value", "1",
                CONSTRAINTS + "listWithUniqueItems[1].key", "a", CONSTRAINTS + "listWithUniqueItems[1].value", "1");
        for (int index = 0; index < 6; index++) {
            stored.put(CONSTRAINTS + "listWithMaxItems[" + index + "].key", "k" + index);
        }

        assertEquals(
                List.of(CONSTRAINTS + "aRequiredString:REQUIRED", CONSTRAINTS + "intWithMax:MAX",
                        CONSTRAINTS + "intWithMin:MIN", CONSTRAINTS + "listWithMaxItems:MAX_ITEMS",
                        CONSTRAINTS + "listWithMinItems:MIN_ITEMS", CONSTRAINTS + "listWithUniqueItems:UNIQUE_ITEMS",
                        CONSTRAINTS + "stringWithMaxLength:MAX_LENGTH", CONSTRAINTS + "stringWithMinLength:MIN_LENGTH",
                        CONSTRAINTS + "stringWithPattern:PATTERN"),
                issues(FormValues.of(tree(SAMPLE_FORM), stored), Visibility.ALL));
    }

    @Test
    void valuesAtTheLimitsOfTheConstraintsAreAccepted() {
        final Map<String, String> stored = map(CONSTRAINTS + "aRequiredString", "x",
                CONSTRAINTS + "stringWithMaxLength", "1234567890", CONSTRAINTS + "stringWithMinLength", "abc",
                CONSTRAINTS + "stringWithPattern", "startabcEND", CONSTRAINTS + "intWithMin", "100",
                CONSTRAINTS + "intWithMax", "100", CONSTRAINTS + "listWithMinItems[0].key", "a",
                CONSTRAINTS + "listWithMinItems[1].key", "b", CONSTRAINTS + "listWithUniqueItems[0].key", "a",
                CONSTRAINTS + "listWithUniqueItems[1].key", "b");
        for (int index = 0; index < 5; index++) {
            stored.put(CONSTRAINTS + "listWithMaxItems[" + index + "].key", "k" + index);
        }

        assertEquals(List.of(), issues(FormValues.of(tree(SAMPLE_FORM), stored), Visibility.ALL));
    }

    @Test
    void aHiddenGroupIsNotValidated() {
        final FormValues untouched = FormValues.of(tree(SAMPLE_FORM), Map.of());

        assertEquals(List.of(CONSTRAINTS + "aRequiredString:REQUIRED", CONSTRAINTS + "listWithMinItems:MIN_ITEMS"),
                issues(untouched, Visibility.ALL));
        assertEquals(List.of(), issues(untouched, path -> !path.startsWith(CONSTRAINTS)));
    }

    private static PropertyTree tree(final String fixture) {
        return PropertyTree
                .of(ConnectorMapper
                        .definition(Fixtures.wire(fixture, Wire.ComponentDetails.class).details().get(0))
                        .properties());
    }

    private static void displayedLeaves(final PropertyNode node, final List<String> paths) {
        if (!node.isDisplayed()) {
            return;
        }
        if (node.kind() == PropertyKind.OBJECT) {
            node.children().forEach(child -> displayedLeaves(child, paths));
        } else {
            paths.add(node.path());
        }
    }

    private static List<String> issues(final FormValues values, final Visibility visibility) {
        return Validator
                .validate(values, visibility)
                .stream()
                .map(issue -> issue.path() + ":" + issue.kind())
                .sorted()
                .toList();
    }

    private static Map<String, String> map(final String... keysAndValues) {
        final Map<String, String> map = new TreeMap<>();
        for (int index = 0; index < keysAndValues.length; index += 2) {
            map.put(keysAndValues[index], keysAndValues[index + 1]);
        }
        return map;
    }

    private static List<String> names(final FormLayout form) {
        return form.tabs().stream().map(Tab::name).toList();
    }

    private static List<String> shape(final FormLayout form, final String tab) {
        return rows(form.tab(tab).orElseThrow().rows());
    }

    private static List<String> rows(final List<Row> rows) {
        return rows
                .stream()
                .map(row -> row.cells().stream().map(FormEngineFixturesTest::cell).collect(Collectors.joining(",")))
                .toList();
    }

    private static String cell(final Cell cell) {
        if (cell instanceof FieldCell field) {
            return field.node().name();
        }
        if (cell instanceof CollectionCell collection) {
            return collection.node().name() + "[" + String.join("|", rows(collection.elementRows())) + "]";
        }
        if (cell instanceof GroupCell group) {
            return group.node().name() + "{" + String.join("|", rows(group.rows())) + "}";
        }
        final ButtonCell button = (ButtonCell) cell;
        final String target = button.target() == button.owner() ? "" : "->" + button.target().name();
        return "<" + button.kind() + ":" + button.binding().name() + target + ">";
    }
}
