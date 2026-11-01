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
import static org.talend.sdk.component.tcketl.domain.form.Props.map;
import static org.talend.sdk.component.tcketl.domain.form.Props.number;
import static org.talend.sdk.component.tcketl.domain.form.Props.object;
import static org.talend.sdk.component.tcketl.domain.form.Props.string;
import static org.talend.sdk.component.tcketl.domain.form.Props.tree;

import java.util.Map;

import org.junit.jupiter.api.Test;

/** Which values the defaults of the server fill in. */
class DefaultsTest {

    @Test
    void anAbsentValueGetsItsDefaultButAStoredOneIsNeverTouched() {
        final PropertyTree tree = tree(object("cfg"), string("cfg.name").defaultValue("anon"),
                string("cfg.cleared").defaultValue("anon"), number("cfg.size").defaultValue("30"),
                bool("cfg.flag").defaultValue("false"), string("cfg.plain"));

        final Map<String, String> filled =
                Defaults.materialize(tree, ConfigurationValues.of(map("cfg.cleared", "", "cfg.size", "5"))).asMap();

        assertEquals(map("cfg.cleared", "", "cfg.flag", "false", "cfg.name", "anon", "cfg.size", "5"), filled);
    }

    @Test
    void aListDefaultIsExpandedOnlyWhileTheListHasNoEntryNotEvenItsLengthBound() {
        final PropertyTree tree = tree(object("cfg"), array("cfg.tags").defaultValue("[\"a\", \"b\"]"),
                string("cfg.tags[]"));

        assertEquals(map("cfg.tags[0]", "a", "cfg.tags[1]", "b"), fill(tree, map()));
        assertEquals(map("cfg.tags[0]", "x"), fill(tree, map("cfg.tags[0]", "x")));
        assertEquals(map("cfg.tags[length]", "0"), fill(tree, map("cfg.tags[length]", "0")));
    }

    @Test
    void aListOfObjectsDefaultSpreadsIntoFieldsAndEveryElementGetsItsFieldDefaults() {
        final PropertyTree tree = tree(object("cfg"),
                array("cfg.items").defaultValue("[{\"name\": \"x\"}, {\"name\": \"y\", \"qty\": 7}]"),
                string("cfg.items[].name"), number("cfg.items[].qty").defaultValue("1"));

        assertEquals(map("cfg.items[0].name", "x", "cfg.items[0].qty", "1", "cfg.items[1].name", "y",
                "cfg.items[1].qty", "7"), fill(tree, map()));
    }

    @Test
    void aMapDefaultFillsTheKeyAndValueLists() {
        final PropertyTree tree =
                tree(object("cfg"), object("cfg.headers").defaultValue("{\"a\": \"1\", \"b\": \"2\"}"),
                        string("cfg.headers.key[]"), string("cfg.headers.value[]"));

        assertEquals(map("cfg.headers.key[0]", "a", "cfg.headers.value[0]", "1", "cfg.headers.key[1]", "b",
                "cfg.headers.value[1]", "2"), fill(tree, map()));
    }

    @Test
    void aDefaultThatIsNotValidJsonIsIgnored() {
        final PropertyTree tree =
                tree(object("cfg"), array("cfg.tags").defaultValue("[\"a\", "), string("cfg.tags[]"));

        assertEquals(map(), fill(tree, map()));
    }

    private static Map<String, String> fill(final PropertyTree tree, final Map<String, String> stored) {
        return Defaults.materialize(tree, ConfigurationValues.of(stored)).asMap();
    }
}
