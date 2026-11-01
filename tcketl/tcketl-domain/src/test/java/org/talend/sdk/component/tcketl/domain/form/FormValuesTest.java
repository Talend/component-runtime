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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.talend.sdk.component.tcketl.domain.form.Props.array;
import static org.talend.sdk.component.tcketl.domain.form.Props.map;
import static org.talend.sdk.component.tcketl.domain.form.Props.number;
import static org.talend.sdk.component.tcketl.domain.form.Props.object;
import static org.talend.sdk.component.tcketl.domain.form.Props.string;
import static org.talend.sdk.component.tcketl.domain.form.Props.tree;

import org.junit.jupiter.api.Test;

/** The values of a form and the edits of its lists and maps. */
class FormValuesTest {

    private static final PropertyTree TREE = tree(object("cfg"), string("cfg.label").defaultValue("L"),
            array("cfg.tags"), string("cfg.tags[]"), array("cfg.items"), string("cfg.items[].name"),
            number("cfg.items[].qty").defaultValue("1"), array("cfg.rows"), string("cfg.rows[].a"),
            string("cfg.rows[].b"), object("cfg.headers"), string("cfg.headers.key[]"),
            string("cfg.headers.value[]"));

    @Test
    void aStoredValueWinsOverTheDefaultAndAnUnsetValueReadsEmpty() {
        final FormValues opened = FormValues.of(TREE, map());
        final FormValues stored = FormValues.of(TREE, map("cfg.label", "mine"));

        assertEquals("L", opened.get("cfg.label"));
        assertEquals("mine", stored.get("cfg.label"));
        assertTrue(opened.has("cfg.label"));
        assertEquals("", opened.get("cfg.tags[0]"));
        assertFalse(opened.has("cfg.tags[0]"));
    }

    @Test
    void anEntryAddedToAListOfValuesStartsEmpty() {
        final FormValues values = FormValues.of(TREE, map()).append("cfg.tags").append("cfg.tags");

        assertEquals(2, values.count("cfg.tags"));
        assertTrue(values.has("cfg.tags[1]"));
        assertEquals("a", values.set("cfg.tags[0]", "a").get("cfg.tags[0]"));
    }

    @Test
    void anObjectAddedToAListGetsTheDefaultsOfItsFields() {
        final FormValues values = FormValues.of(TREE, map()).append("cfg.items");

        assertEquals(1, values.count("cfg.items"));
        assertEquals("1", values.get("cfg.items[0].qty"));
        assertFalse(values.has("cfg.items[0].name"));
    }

    @Test
    void anAddedObjectWithoutAnyDefaultStillExistsThroughAMarker() {
        final FormValues values = FormValues.of(TREE, map()).append("cfg.rows").append("cfg.rows");

        assertEquals(2, values.count("cfg.rows"));
        assertTrue(values.has("cfg.rows[0].a"));
        assertTrue(values.has("cfg.rows[1].a"));
    }

    @Test
    void aMapEntryIsAddedMovedAndRemovedAsAKeyValuePair() {
        final FormValues values = FormValues
                .of(TREE, map())
                .append("cfg.headers")
                .append("cfg.headers")
                .set("cfg.headers.key[0]", "a")
                .set("cfg.headers.value[0]", "1")
                .set("cfg.headers.key[1]", "b")
                .set("cfg.headers.value[1]", "2");

        assertEquals(2, values.count("cfg.headers"));
        final FormValues moved = values.move("cfg.headers", 0, 1);
        assertEquals("b", moved.get("cfg.headers.key[0]"));
        assertEquals("2", moved.get("cfg.headers.value[0]"));
        assertEquals("a", moved.get("cfg.headers.key[1]"));
        assertEquals("1", moved.get("cfg.headers.value[1]"));
        final FormValues removed = values.remove("cfg.headers", 0);
        assertEquals(1, removed.count("cfg.headers"));
        assertEquals("b", removed.get("cfg.headers.key[0]"));
        assertEquals("2", removed.get("cfg.headers.value[0]"));
    }

    @Test
    void movingAndRemovingAnObjectKeepsItsFieldsTogether() {
        final FormValues values = FormValues
                .of(TREE, map())
                .append("cfg.items")
                .append("cfg.items")
                .set("cfg.items[0].name", "first")
                .set("cfg.items[1].name", "second")
                .set("cfg.items[1].qty", "9");

        final FormValues moved = values.move("cfg.items", 1, 0);
        assertEquals("second", moved.get("cfg.items[0].name"));
        assertEquals("9", moved.get("cfg.items[0].qty"));
        assertEquals("first", moved.get("cfg.items[1].name"));
        assertEquals("1", moved.get("cfg.items[1].qty"));
        final FormValues removed = values.remove("cfg.items", 0);
        assertEquals(1, removed.count("cfg.items"));
        assertEquals("second", removed.get("cfg.items[0].name"));
    }

    @Test
    void anEditThatChangesNothingOrNamesNoCollectionReturnsTheSameValues() {
        final FormValues values = FormValues.of(TREE, map());

        assertSame(values, values.set("cfg.label", "L"));
        assertSame(values, values.append("cfg.label"));
        assertSame(values, values.append("cfg.unknown"));
        assertSame(values, values.remove("cfg.label", 0));
        assertSame(values, values.move("cfg.label", 0, 1));
    }
}
