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
import static org.talend.sdk.component.tcketl.domain.form.Props.array;
import static org.talend.sdk.component.tcketl.domain.form.Props.bool;
import static org.talend.sdk.component.tcketl.domain.form.Props.layout;
import static org.talend.sdk.component.tcketl.domain.form.Props.number;
import static org.talend.sdk.component.tcketl.domain.form.Props.object;
import static org.talend.sdk.component.tcketl.domain.form.Props.string;
import static org.talend.sdk.component.tcketl.domain.form.Props.tree;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.talend.sdk.component.tcketl.domain.form.FormLayout.ButtonCell;
import org.talend.sdk.component.tcketl.domain.form.FormLayout.Cell;
import org.talend.sdk.component.tcketl.domain.form.FormLayout.CollectionCell;
import org.talend.sdk.component.tcketl.domain.form.FormLayout.FieldCell;
import org.talend.sdk.component.tcketl.domain.form.FormLayout.GroupCell;
import org.talend.sdk.component.tcketl.domain.form.FormLayout.Row;
import org.talend.sdk.component.tcketl.domain.form.FormLayout.Tab;
import org.talend.sdk.component.tcketl.domain.property.ActionBinding;
import org.talend.sdk.component.tcketl.domain.property.ActionTypes;
import org.talend.sdk.component.tcketl.domain.property.LayoutHints;
import org.talend.sdk.component.tcketl.domain.property.PropertyDefinition;
import org.talend.sdk.component.tcketl.domain.property.UiHints;
import org.talend.sdk.component.tcketl.domain.property.UiHints.StructureDirection;

/**
 * The tabs, rows and cells of a form. A tab is written as one string per row, cells separated by commas, a group as
 * {@code name{rows separated by |}}, a list as {@code name[rows of an entry]} and a button as {@code <KIND:action>}.
 */
class LayoutResolverTest {

    @Test
    void tabsAreThePaneNamesWithMainFirstWhateverTheOrderOfTheServer() {
        final FormLayout form = LayoutResolver
                .resolve(tree(object("cfg").layout(layout("Proxy:proxy", "Advanced:timeout", "Main:login,password|ok")),
                        string("cfg.login"), string("cfg.password"), bool("cfg.ok"), number("cfg.timeout"),
                        string("cfg.proxy")));

        assertEquals(List.of("Main", "Proxy", "Advanced"), names(form));
        assertTrue(form.isTabbed());
        assertEquals(List.of("login,password", "ok"), shape(form, "Main"));
        assertEquals(List.of("proxy"), shape(form, "Proxy"));
        assertEquals(List.of("timeout"), shape(form, "Advanced"));
    }

    @Test
    void aSinglePaneNeedsNoTabs() {
        final FormLayout form = LayoutResolver
                .resolve(tree(object("cfg").layout(layout("Main:a|b")), string("cfg.a"), string("cfg.b")));

        assertEquals(List.of("Main"), names(form));
        assertFalse(form.isTabbed());
    }

    @Test
    void aPropertyAppearsOnceInTheFirstTabThatShowsIt() {
        final FormLayout form = LayoutResolver
                .resolve(tree(object("cfg").layout(layout("Main:a,b", "Advanced:b,c")), string("cfg.a"),
                        string("cfg.b"), string("cfg.c")));

        assertEquals(List.of("a,b"), shape(form, "Main"));
        assertEquals(List.of("c"), shape(form, "Advanced"));
    }

    @Test
    void aCellNamingNoPropertyIsSkippedAndARowThatEmptiesDisappears() {
        final FormLayout form = LayoutResolver
                .resolve(tree(object("cfg").layout(layout("Main:a,ghost|ghost|b")), string("cfg.a"), string("cfg.b")));

        assertEquals(List.of("a", "b"), shape(form, "Main"));
    }

    @Test
    void anUnlistedPropertyIsAppendedToTheFirstPaneUnlessTheOptionSaysOtherwise() {
        final PropertyTree tree = tree(object("cfg").layout(layout("Advanced:b", "Main:a")), string("cfg.a"),
                string("cfg.b"), string("cfg.c"));

        final FormLayout appended = LayoutResolver.resolve(tree);
        final FormLayout left = LayoutResolver.resolve(tree, new FormOptions(false));

        assertEquals(List.of("a", "c"), shape(appended, "Main"));
        assertEquals(List.of("b"), shape(appended, "Advanced"));
        assertEquals(List.of("a"), shape(left, "Main"));
    }

    @Test
    void technicalOptionsOfTheRootGoToAnAdvancedPaneThatIsCreatedWhenMissing() {
        final FormLayout created = LayoutResolver
                .resolve(tree(object("cfg").layout(layout("Main:a")), string("cfg.a"), number("cfg.$maxRecords")));
        final FormLayout existing = LayoutResolver
                .resolve(tree(object("cfg").layout(layout("Main:a", "Advanced:b")), string("cfg.a"), string("cfg.b"),
                        number("cfg.$maxRecords")));

        assertEquals(List.of("Main", "Advanced"), names(created));
        assertEquals(List.of("$maxRecords"), shape(created, "Advanced"));
        assertEquals(List.of("b", "$maxRecords"), shape(existing, "Advanced"));
    }

    @Test
    void anObjectWithoutGridFollowsItsOptionsOrderThenThePaths() {
        final FormLayout form = LayoutResolver
                .resolve(tree(object("cfg").layout(new LayoutHints(List.of(), List.of("z", "ghost", "a"))),
                        string("cfg.a"), string("cfg.b"), string("cfg.z"), number("cfg.$maxRecords")));

        assertEquals(List.of("Main", "Advanced"), names(form));
        assertEquals(List.of("z", "a", "b"), shape(form, "Main"));
        assertEquals(List.of("$maxRecords"), shape(form, "Advanced"));
    }

    @Test
    void nestedObjectsAreTitledGroupsThatShowThePaneOfTheTab() {
        final FormLayout form = LayoutResolver
                .resolve(tree(object("c").layout(layout("Main:message|dataset", "Advanced:dataset")),
                        string("c.message"),
                        object("c.dataset").layout(layout("Main:entity|datastore", "Advanced:readAll|datastore")),
                        string("c.dataset.entity"), bool("c.dataset.readAll"),
                        object("c.dataset.datastore").layout(layout("Main:url|auth", "Advanced:timeout|auth")),
                        string("c.dataset.datastore.url"), number("c.dataset.datastore.timeout"),
                        object("c.dataset.datastore.auth")
                                .layout(layout("Main:login,password", "Advanced:nbAuthRetry")),
                        string("c.dataset.datastore.auth.login"), string("c.dataset.datastore.auth.password"),
                        number("c.dataset.datastore.auth.nbAuthRetry")));

        assertEquals(List.of("Main", "Advanced"), names(form));
        assertEquals(List.of("message", "dataset{entity|datastore{url|auth{login,password}}}"), shape(form, "Main"));
        assertEquals(List.of("dataset{readAll|datastore{timeout|auth{nbAuthRetry}}}"), shape(form, "Advanced"));
    }

    @Test
    void anObjectLackingThePaneOfATabShowsItsOwnPanesAndAPaneNobodyShowsMakesNoTab() {
        final FormLayout form = LayoutResolver
                .resolve(tree(object("cfg").layout(layout("Main:name|proxy", "Advanced:timeout")), string("cfg.name"),
                        number("cfg.timeout"), object("cfg.proxy").layout(layout("Proxy:host|port")),
                        string("cfg.proxy.host"), number("cfg.proxy.port")));

        assertEquals(List.of("Main", "Advanced"), names(form));
        assertEquals(List.of("name", "proxy{host|port}"), shape(form, "Main"));
        assertEquals(List.of("timeout"), shape(form, "Advanced"));
    }

    @Test
    void hiddenPropertiesAndTheStateOfTheRuntimeAreNeverLaidOut() {
        final UiHints hidden = UiHints.builder().hidden(true).build();

        final FormLayout form = LayoutResolver
                .resolve(tree(
                        object("cfg").layout(layout("Main:a,secret,hiddenGroup,emptyGroup", "Checkpoint:$checkpoint")),
                        string("cfg.a"), string("cfg.secret").ui(hidden), object("cfg.hiddenGroup").ui(hidden),
                        string("cfg.hiddenGroup.x"), object("cfg.emptyGroup"), string("cfg.emptyGroup.y").ui(hidden),
                        object("cfg.$checkpoint"), string("cfg.$checkpoint.id")));

        assertEquals(List.of("Main"), names(form));
        assertEquals(List.of("a"), shape(form, "Main"));
    }

    @Test
    void aListKeepsTheLayoutOfItsEntriesAndAMapIsAKeyAndAValue() {
        final FormLayout form = LayoutResolver
                .resolve(tree(object("cfg").layout(layout("Main:items|headers|tags")),
                        array("cfg.items").layout(layout("Main:name,qty")), string("cfg.items[].name"),
                        number("cfg.items[].qty"), object("cfg.headers"), string("cfg.headers.key[]"),
                        string("cfg.headers.value[]"), array("cfg.tags"), string("cfg.tags[]")));

        assertEquals(List.of("items[name,qty]", "headers[key[],value[]]", "tags[]"), shape(form, "Main"));
    }

    @Test
    void theHealthcheckButtonEndsTheGroupOfItsObjectAndAppearsOnlyOnce() {
        final FormLayout form = LayoutResolver
                .resolve(tree(object("cfg").layout(layout("Main:datastore|name", "Advanced:datastore|timeout")),
                        string("cfg.name"), number("cfg.timeout"),
                        object("cfg.datastore")
                                .layout(layout("Main:url", "Advanced:retries"))
                                .action(new ActionBinding(ActionTypes.HEALTHCHECK, "check")),
                        string("cfg.datastore.url"), number("cfg.datastore.retries")));

        assertEquals(List.of("datastore{url|<HEALTHCHECK:check>}", "name"), shape(form, "Main"));
        assertEquals(List.of("datastore{retries}", "timeout"), shape(form, "Advanced"));
    }

    @Test
    void theUpdateButtonFollowsThePropertyItNamesOrEndsTheObject() {
        assertEquals(List.of("a", "b", "<UPDATE:refresh>", "c"), shapeWithUpdateAfter("b"));
        assertEquals(List.of("a", "b", "c", "<UPDATE:refresh>"), shapeWithUpdateAfter("unknown"));
        assertEquals(List.of("a", "b", "c", "<UPDATE:refresh>"), shapeWithUpdateAfter(""));
    }

    @ParameterizedTest
    @CsvSource({ "guess,discover,guess", "'',discover,discover", "'','',default" })
    void theSchemaButtonCallsTheDeclaredActionThenTheDiscoveryOfTheStructureThenDefault(final String declared,
            final String discovery, final String expected) {
        PropertyDefinition.Builder columns =
                array("cfg.columns").ui(UiHints.builder().structure(StructureDirection.OUT, "", discovery).build());
        if (!declared.isEmpty()) {
            columns = columns.action(new ActionBinding(ActionTypes.SCHEMA, declared));
        }

        final FormLayout form = LayoutResolver
                .resolve(tree(object("cfg").layout(layout("Main:name|columns")), string("cfg.name"), columns,
                        string("cfg.columns[]")));

        assertEquals(List.of("name", "columns[]", "<GUESS_SCHEMA:" + expected + "->columns>"), shape(form, "Main"));
    }

    @Test
    void aRootThatIsNotAnObjectGoesToTheMainTab() {
        final FormLayout form = LayoutResolver.resolve(tree(string("name")));

        assertEquals(List.of("Main"), names(form));
        assertEquals(List.of("name"), shape(form, "Main"));
    }

    private static List<String> shapeWithUpdateAfter(final String after) {
        final ActionBinding update =
                new ActionBinding(ActionTypes.UPDATE, "refresh", List.of(), Map.of("after", after));
        return shape(LayoutResolver
                .resolve(tree(object("cfg").layout(layout("Main:a|b|c")).action(update), string("cfg.a"),
                        string("cfg.b"), string("cfg.c"))),
                "Main");
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
                .map(row -> row.cells().stream().map(LayoutResolverTest::cell).collect(Collectors.joining(",")))
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
