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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.talend.sdk.component.tcketl.domain.form.Props.array;
import static org.talend.sdk.component.tcketl.domain.form.Props.bool;
import static org.talend.sdk.component.tcketl.domain.form.Props.choice;
import static org.talend.sdk.component.tcketl.domain.form.Props.map;
import static org.talend.sdk.component.tcketl.domain.form.Props.number;
import static org.talend.sdk.component.tcketl.domain.form.Props.object;
import static org.talend.sdk.component.tcketl.domain.form.Props.string;
import static org.talend.sdk.component.tcketl.domain.form.Props.tree;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.tcketl.domain.form.ConfigurationCodec.KeyClass;

/** The flat map given to the runtime, and the way back. */
class ConfigurationCodecTest {

    @Test
    void anEmptyValueIsLeftOutExceptATextWhoseDefaultIsNotEmpty() {
        final PropertyTree tree = tree(object("cfg"), string("cfg.name").defaultValue("anon"), string("cfg.note"),
                number("cfg.count"), choice("cfg.mode", "FAST", "SLOW"), bool("cfg.flag").defaultValue("false"));

        final Map<String, String> encoded = encode(tree, map("cfg.name", "", "cfg.note", "", "cfg.count", "",
                "cfg.mode", "", "cfg.flag", "false"));

        assertEquals(map("cfg.flag", "false", "cfg.name", ""), encoded);
    }

    @Test
    void theEntriesOfAListOfValuesAndTheKeysAndValuesOfAMapAreKeptEvenEmpty() {
        final PropertyTree tree = tree(object("cfg"), array("cfg.tags"), string("cfg.tags[]"), object("cfg.headers"),
                string("cfg.headers.key[]"), string("cfg.headers.value[]"));
        final Map<String, String> stored =
                map("cfg.tags[0]", "", "cfg.tags[1]", "x", "cfg.headers.key[0]", "", "cfg.headers.value[0]", "");

        assertEquals(stored, encode(tree, stored));
    }

    @Test
    void aListEmptiedOnPurposeIsMarkedWhenItsDefaultWouldComeBack() {
        final PropertyTree tree =
                tree(object("cfg"), array("cfg.tags").defaultValue("[\"a\"]"), string("cfg.tags[]"));

        assertEquals(map("cfg.tags[length]", "0"), encode(tree, map()));
        assertEquals(map("cfg.tags[0]", "a"), encode(tree, map("cfg.tags[0]", "a")));
        assertEquals(map("cfg.tags[0]", "a"), encode(tree, map("cfg.tags[length]", "0", "cfg.tags[0]", "a")));
        assertEquals(map("cfg.tags[length]", "5"), encode(tree, map("cfg.tags[length]", "5")));
    }

    @Test
    void anElementWhoseValuesAreAllEmptyIsKeptAliveByAMarker() {
        final PropertyTree texts = tree(object("cfg"), array("cfg.rows"), string("cfg.rows[].a"),
                number("cfg.rows[].n"));
        final PropertyTree numbers = tree(object("cfg"), array("cfg.rows"), number("cfg.rows[].n"));

        assertEquals(map("cfg.rows[0].a", "", "cfg.rows[1].a", "x"),
                encode(texts, map("cfg.rows[0].n", "", "cfg.rows[1].a", "x")));
        assertEquals(map("cfg.rows[0].n", "0"), encode(numbers, map("cfg.rows[0].n", "")));
    }

    @Test
    void keysTheTreeDoesNotDescribeAreKeptExceptTheOnesMadeForTheUi() {
        final PropertyTree tree = tree(object("cfg"), string("cfg.name"));

        final Map<String, String> encoded = encode(tree, map("cfg.name", "x", "cfg.legacy", "y", "cfg.__version", "2",
                "cfg.$maxRecords", "10", "cfg.$driver_name", "ui"));

        assertEquals(map("cfg.$maxRecords", "10", "cfg.__version", "2", "cfg.legacy", "y", "cfg.name", "x"), encoded);
    }

    @Test
    void everyKeyIsClassifiedAgainstTheTree() {
        final PropertyTree tree = tree(object("cfg"), string("cfg.name"), array("cfg.items"),
                string("cfg.items[].name"));

        assertEquals(KeyClass.KNOWN, ConfigurationCodec.classify(tree, "cfg.name"));
        assertEquals(KeyClass.KNOWN, ConfigurationCodec.classify(tree, "cfg.items[3].name"));
        assertEquals(KeyClass.TECHNICAL, ConfigurationCodec.classify(tree, "cfg.$maxRecords"));
        assertEquals(KeyClass.TECHNICAL, ConfigurationCodec.classify(tree, "cfg.items[length]"));
        assertEquals(KeyClass.TECHNICAL, ConfigurationCodec.classify(tree, "cfg.__version"));
        assertEquals(KeyClass.UNKNOWN, ConfigurationCodec.classify(tree, "cfg.legacy"));
        assertEquals(KeyClass.UI_COMPANION, ConfigurationCodec.classify(tree, "cfg.$driver_name"));
    }

    @Test
    void theResultIsSortedAndCannotBeModified() {
        final Map<String, String> encoded = encode(tree(object("cfg"), string("cfg.b"), string("cfg.a")),
                map("cfg.b", "2", "cfg.a", "1"));

        assertEquals("cfg.a", encoded.keySet().iterator().next());
        assertThrows(UnsupportedOperationException.class, () -> encoded.put("x", "y"));
    }

    @Test
    void whatTheFormWritesIsReadBackAsTheSameValues() {
        final PropertyTree tree = tree(object("cfg"), string("cfg.name").defaultValue("anon"), string("cfg.note"),
                number("cfg.count"), choice("cfg.mode", "FAST", "SLOW"), bool("cfg.flag").defaultValue("false"),
                array("cfg.tags").defaultValue("[\"a\", \"b\"]"), string("cfg.tags[]"), array("cfg.rows"),
                string("cfg.rows[].a"), number("cfg.rows[].n"), object("cfg.headers"), string("cfg.headers.key[]"),
                string("cfg.headers.value[]"));
        final FormValues edited = FormValues
                .of(tree, map("cfg.$maxRecords", "100"))
                .set("cfg.note", "hello")
                .set("cfg.count", "42")
                .set("cfg.mode", "SLOW")
                .remove("cfg.tags", 0)
                .append("cfg.rows")
                .append("cfg.rows")
                .set("cfg.rows[1].a", "second")
                .append("cfg.headers")
                .set("cfg.headers.key[0]", "k");

        final Map<String, String> written = edited.toConfiguration();
        final FormValues reopened = FormValues.of(tree, written);

        assertEquals(written, reopened.toConfiguration());
        assertEquals("hello", reopened.get("cfg.note"));
        assertEquals("42", reopened.get("cfg.count"));
        assertEquals("anon", reopened.get("cfg.name"));
        assertEquals(1, reopened.count("cfg.tags"));
        assertEquals("b", reopened.get("cfg.tags[0]"));
        assertEquals(2, reopened.count("cfg.rows"));
        assertEquals("second", reopened.get("cfg.rows[1].a"));
        assertEquals(1, reopened.count("cfg.headers"));
        assertEquals("k", reopened.get("cfg.headers.key[0]"));
        assertEquals("100", reopened.get("cfg.$maxRecords"));
    }

    private static Map<String, String> encode(final PropertyTree tree, final Map<String, String> values) {
        return ConfigurationCodec.encode(tree, ConfigurationValues.of(values));
    }
}
