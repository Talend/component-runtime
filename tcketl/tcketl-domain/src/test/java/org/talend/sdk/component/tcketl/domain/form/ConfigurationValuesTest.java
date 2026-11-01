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
import static org.talend.sdk.component.tcketl.domain.form.Props.map;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

/** The flat map operations behind lists and maps. */
class ConfigurationValuesTest {

    @Test
    void operationsReturnNewValuesAndLeaveTheOriginalUntouched() {
        final ConfigurationValues original = ConfigurationValues.of(map("a", "1"));

        final ConfigurationValues changed = original.with("b", "2").with("a", "3");

        assertEquals(map("a", "1"), original.asMap());
        assertEquals(map("a", "3", "b", "2"), changed.asMap());
        assertSame(changed, changed.with("a", "3"));
        assertEquals("", original.get("missing"));
        assertFalse(original.contains("missing"));
    }

    @Test
    void aListEndsAtTheFirstMissingIndex() {
        final ConfigurationValues values =
                ConfigurationValues.of(map("l[0]", "a", "l[1]", "b", "l[3]", "d", "l[length]", "5", "l2[0]", "x"));

        assertEquals(2, values.count("l"));
        assertEquals(1, values.count("l2"));
    }

    @Test
    void anElementExistsThroughItsOwnKeyEvenEmptyOrThroughItsFields() {
        final ConfigurationValues values = ConfigurationValues.of(map("l[0].x", "1", "l[1]", "", "l[2][0]", "n"));

        assertEquals(3, values.count("l"));
    }

    @Test
    void aMapEntryExistsAsSoonAsItsKeyOrItsValueDoes() {
        final ConfigurationValues values =
                ConfigurationValues.of(map("m.key[0]", "a", "m.value[1]", "b", "m.key[2]", "c"));

        assertEquals(3, values.count("m.key", "m.value"));
        assertEquals(1, values.count("m.key"));
    }

    @Test
    void removingAnElementRenumbersTheFollowingOnesWithTheirNestedKeys() {
        final ConfigurationValues values = ConfigurationValues
                .of(map("l[0].a", "a0", "l[1].a", "a1", "l[1].tags[0]", "t", "l[2].a", "a2", "l[length]", "5", "other",
                        "x"));

        final ConfigurationValues removed = values.removeElement("l", 1);

        assertEquals(map("l[0].a", "a0", "l[1].a", "a2", "other", "x"), removed.asMap());
        assertSame(values, values.removeElement("l", 3));
        assertSame(values, values.removeElement("l", -1));
    }

    @Test
    void movingAnElementShiftsTheOnesInBetween() {
        final ConfigurationValues values =
                ConfigurationValues.of(map("l[0]", "a", "l[1]", "b", "l[2]", "c", "l[length]", "9"));

        assertEquals(map("l[0]", "b", "l[1]", "c", "l[2]", "a"), values.moveElement("l", 0, 2).asMap());
        assertEquals(map("l[0]", "c", "l[1]", "a", "l[2]", "b"), values.moveElement("l", 2, 0).asMap());
        assertSame(values, values.moveElement("l", 1, 1));
        assertSame(values, values.moveElement("l", 0, 3));
    }

    @Test
    void aSubtreeIsTheKeyItsFieldsAndItsElementsButNotItsNeighbours() {
        final ConfigurationValues values =
                ConfigurationValues.of(map("a", "1", "a.b", "2", "a[0]", "3", "a[0].c", "4", "ab", "5", "a2", "6"));

        assertEquals(Set.of("a", "a.b", "a[0]", "a[0].c"), values.subtree("a").keySet());
        assertEquals(map("a2", "6", "ab", "5"), values.withoutSubtree("a").asMap());
        assertEquals(map("a", "new", "a2", "6", "ab", "5"),
                values.replaceSubtree("a", Map.of("a", "new")).asMap());
        assertSame(values, values.withoutSubtree("zzz"));
    }

    @Test
    void missingEntriesAreAddedWithoutOverwritingTheOnesThatExist() {
        final ConfigurationValues values = ConfigurationValues.of(map("a", "1"));

        assertEquals(map("a", "1", "b", "2"), values.withMissing(map("a", "other", "b", "2")).asMap());
        assertSame(values, values.withMissing(map("a", "other")));
    }
}
