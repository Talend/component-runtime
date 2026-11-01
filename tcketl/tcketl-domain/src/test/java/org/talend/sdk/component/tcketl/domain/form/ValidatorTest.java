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
import static org.talend.sdk.component.tcketl.domain.form.Props.array;
import static org.talend.sdk.component.tcketl.domain.form.Props.bool;
import static org.talend.sdk.component.tcketl.domain.form.Props.choice;
import static org.talend.sdk.component.tcketl.domain.form.Props.map;
import static org.talend.sdk.component.tcketl.domain.form.Props.number;
import static org.talend.sdk.component.tcketl.domain.form.Props.object;
import static org.talend.sdk.component.tcketl.domain.form.Props.string;
import static org.talend.sdk.component.tcketl.domain.form.Props.tree;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.talend.sdk.component.tcketl.domain.property.Constraints;
import org.talend.sdk.component.tcketl.domain.property.UiHints;

/** The constraints of the connector, checked on what the user typed. Issues are written {@code path:KIND}. */
class ValidatorTest {

    private static final Constraints REQUIRED = Constraints.NONE.withRequired(true);

    @Test
    void aRequiredValueIsNotEmptyExceptABooleanAndZeroIsAValue() {
        final PropertyTree tree = tree(object("cfg"), bool("cfg.flag").constraints(REQUIRED),
                choice("cfg.mode", "A", "B").constraints(REQUIRED.withEnumValues(List.of("A", "B"))),
                number("cfg.number").constraints(REQUIRED), string("cfg.text").constraints(REQUIRED),
                number("cfg.zero").constraints(REQUIRED));

        assertEquals(List.of("cfg.mode:REQUIRED", "cfg.number:REQUIRED", "cfg.text:REQUIRED"),
                issues(FormValues.of(tree, map("cfg.zero", "0"))));
    }

    @Test
    void aDefaultSatisfiesRequiredUntilTheUserClearsIt() {
        final PropertyTree tree = tree(object("cfg"), string("cfg.name").defaultValue("anon").constraints(REQUIRED));
        final FormValues opened = FormValues.of(tree, map());

        assertEquals(List.of(), issues(opened));
        assertEquals(List.of("cfg.name:REQUIRED"), issues(opened.set("cfg.name", "")));
    }

    @ParameterizedTest
    @CsvSource({ "5,NONE", "10,NONE", "7.5,NONE", "4,MIN", "11,MAX", "-3,MIN", "1e3,MAX", "abc,NUMBER_FORMAT" })
    void aNumberParsesAndStaysWithinItsBounds(final String value, final String expected) {
        final PropertyTree tree =
                tree(object("cfg"), number("cfg.n").constraints(Constraints.NONE.withMin(5).withMax(10)));

        assertEquals("NONE".equals(expected) ? List.of() : List.of("cfg.n:" + expected),
                issues(FormValues.of(tree, map("cfg.n", value))));
    }

    @ParameterizedTest
    @CsvSource({ "abc,NONE", "abcde,NONE", "ab,MIN_LENGTH", "abcdef,MAX_LENGTH" })
    void aTextHasToBeLongEnoughAndNotTooLong(final String value, final String expected) {
        final PropertyTree tree =
                tree(object("cfg"), string("cfg.t").constraints(Constraints.NONE.withMinLength(3).withMaxLength(5)));

        assertEquals("NONE".equals(expected) ? List.of() : List.of("cfg.t:" + expected),
                issues(FormValues.of(tree, map("cfg.t", value))));
    }

    @ParameterizedTest
    @CsvSource({ "[0-9]+,abc123def,NONE", "^[0-9]+$,abc123def,PATTERN", "^[0-9]+$,123,NONE",
            "'^start[abcdef]{0,5}END$',startabcEND,NONE", "'^start[abcdef]{0,5}END$',startxEND,PATTERN",
            "'^start[abcdef]{0,5}END$',xstartabcEND,PATTERN", "'([',anything,NONE" })
    void aPatternMatchesSomewhereInTheTextUnlessAnchoredAndAnInvalidOneIsNotEnforced(final String pattern,
            final String value, final String expected) {
        final PropertyTree tree =
                tree(object("cfg"), string("cfg.t").constraints(Constraints.NONE.withPattern(pattern)));

        assertEquals("NONE".equals(expected) ? List.of() : List.of("cfg.t:" + expected),
                issues(FormValues.of(tree, map("cfg.t", value))));
    }

    @Test
    void aChoiceIsOneOfItsValues() {
        final PropertyTree tree = tree(object("cfg"), choice("cfg.mode", "FAST", "SLOW"));

        assertEquals(List.of(), issues(FormValues.of(tree, map("cfg.mode", "FAST"))));
        assertEquals(List.of("cfg.mode:ENUM_VALUE"), issues(FormValues.of(tree, map("cfg.mode", "TURBO"))));
    }

    @Test
    void anIssueExplainsItselfInEnglish() {
        final PropertyTree tree = tree(object("cfg"), choice("cfg.mode", "FAST", "SLOW"),
                number("cfg.n").constraints(Constraints.NONE.withMin(5)),
                number("cfg.x").constraints(Constraints.NONE.withMin(5)), string("cfg.t").constraints(REQUIRED));

        final List<String> messages = Validator
                .validate(FormValues.of(tree, map("cfg.mode", "TURBO", "cfg.n", "4", "cfg.x", "abc")))
                .stream()
                .map(ValidationIssue::message)
                .toList();

        assertEquals(List.of("'TURBO' is not one of FAST, SLOW.", "Must be at least 5.", "'abc' is not a number.",
                "A value is required."), messages);
    }

    @Test
    void aListHasItsSizeChecked() {
        final PropertyTree tree = tree(object("cfg"), array("cfg.few").constraints(Constraints.NONE.withMinItems(2)),
                string("cfg.few[]"), array("cfg.many").constraints(Constraints.NONE.withMaxItems(1)),
                string("cfg.many[]"), array("cfg.tags").constraints(REQUIRED), string("cfg.tags[]"));

        assertEquals(List.of("cfg.few:MIN_ITEMS", "cfg.many:MAX_ITEMS", "cfg.tags:REQUIRED"),
                issues(FormValues.of(tree, map("cfg.few[0]", "a", "cfg.many[0]", "a", "cfg.many[1]", "b"))));
    }

    @Test
    void uniqueItemsComparesTheValuesOfAPlainListAndTheContentOfTheObjectsOfAList() {
        final Constraints unique = Constraints.NONE.withUniqueItems(true);
        final PropertyTree tree = tree(object("cfg"), array("cfg.distinct").constraints(unique),
                string("cfg.distinct[]"), array("cfg.objects").constraints(unique), string("cfg.objects[].name"),
                number("cfg.objects[].qty"), array("cfg.plain").constraints(unique), string("cfg.plain[]"));

        assertEquals(List.of("cfg.objects:UNIQUE_ITEMS", "cfg.plain:UNIQUE_ITEMS"),
                issues(FormValues.of(tree,
                        map("cfg.distinct[0]", "a", "cfg.distinct[1]", "b", "cfg.plain[0]", "a", "cfg.plain[1]", "b",
                                "cfg.plain[2]", "a", "cfg.objects[0].name", "x", "cfg.objects[0].qty", "1",
                                "cfg.objects[1].name", "x", "cfg.objects[1].qty", "2", "cfg.objects[2].name", "x",
                                "cfg.objects[2].qty", "1"))));
        assertEquals(List.of(),
                issues(FormValues.of(tree,
                        map("cfg.objects[0].name", "x", "cfg.objects[0].qty", "1", "cfg.objects[1].name", "x",
                                "cfg.objects[1].qty", "2"))));
    }

    @Test
    void whatIsNotShownIsNotChecked() {
        final PropertyTree tree = tree(object("cfg"), object("cfg.$checkpoint"),
                string("cfg.$checkpoint.id").constraints(REQUIRED), string("cfg.always").constraints(REQUIRED),
                string("cfg.hidden").constraints(REQUIRED).ui(UiHints.builder().hidden(true).build()),
                string("cfg.other").constraints(REQUIRED), object("cfg.proxy"),
                string("cfg.proxy.host").constraints(REQUIRED));
        final FormValues values = FormValues.of(tree, map());

        assertEquals(List.of("cfg.always:REQUIRED", "cfg.other:REQUIRED", "cfg.proxy.host:REQUIRED"), issues(values));
        assertEquals(List.of("cfg.always:REQUIRED", "cfg.other:REQUIRED"),
                issues(values, path -> !path.startsWith("cfg.proxy")));
    }

    @Test
    void theIssuesOfListEntriesNameTheirConcretePathAndAVisibilityCanHideOneEntry() {
        final PropertyTree tree = tree(object("cfg"), array("cfg.items"),
                string("cfg.items[].name").constraints(REQUIRED), number("cfg.items[].qty"));
        final FormValues values = FormValues.of(tree, map("cfg.items[0].qty", "1", "cfg.items[1].qty", "2"));

        assertEquals(List.of("cfg.items[0].name:REQUIRED", "cfg.items[1].name:REQUIRED"), issues(values));
        assertEquals(List.of("cfg.items[0].name:REQUIRED"), issues(values, path -> !path.startsWith("cfg.items[1]")));
    }

    private static List<String> issues(final FormValues values) {
        return issues(values, Visibility.ALL);
    }

    private static List<String> issues(final FormValues values, final Visibility visibility) {
        return Validator
                .validate(values, visibility)
                .stream()
                .map(issue -> issue.path() + ":" + issue.kind())
                .toList();
    }
}
