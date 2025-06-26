/**
 * Copyright (C) 2006-2025 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.runtime.record;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.api.record.Schema.Entry;
import org.talend.sdk.component.api.record.Schema.Type;
import org.talend.sdk.component.api.record.SchemaCompanionUtil;

import lombok.RequiredArgsConstructor;

class SchemaCompanionUtilTest {

    @RequiredArgsConstructor
    private static final class ReplaceFunction implements BiConsumer<String, Entry> {

        private final Map<String, Entry> entries;

        @Override
        public void accept(final String s, final Entry entry) {
            entries.remove(s);
            entries.put(entry.getName(), entry);
        }
    }

    @Test
    void testAvoidCollision() {
        final Map<String, Entry> entries = new HashMap<>();
        for (int index = 1; index < 8; index++) {
            addNewEntry(newEntry(index + "name_b", Type.STRING), entries);
        }

        // this one should collide with the previous ones
        addNewEntry(newEntry("name_b_5", Type.STRING), entries);

        // in total 7 + 1 = 8 entries
        Assertions.assertEquals(8, entries.size());
        Assertions.assertEquals("name_b", entries.get("name_b").getName());
        // 7 names with suffixes
        Assertions
                .assertTrue(IntStream
                        .range(1, 8)
                        .mapToObj((int i) -> "name_b_" + i)
                        .allMatch((String name) -> entries.get(name).getName().equals(name)));
    }

    @Test
    void collisionDuplicatedNormalName() {
        final Map<String, Entry> entriesDuplicate = new HashMap<>();
        final Schema.Entry e1 = newEntry("goodName", Type.STRING);
        final Schema.Entry realEntry1 = addNewEntry(e1, entriesDuplicate);
        Assertions.assertSame(e1, realEntry1);

        final Schema.Entry e2 = newEntry("goodName", Type.STRING);
        final Schema.Entry realEntry2 = addNewEntry(e2, entriesDuplicate);

        Assertions.assertSame(realEntry2, e2);
    }

    /**
     * Similar to, but with the real EntryImpl that has build-in sanitization.
     * org.talend.sdk.component.api.record.SchemaCompanionUtilTest#sanitizeEqualLengthCyrillicNames()
     */
    @Test
    void avoidCollisionEqualLengthCyrillicNames() {
        final String firstRawName = "Світло";
        final String secondRawName = "Мріяти";

        final Map<String, Entry> entries = new HashMap<>();

        addNewEntry(newEntry(firstRawName, Type.STRING), entries);
        addNewEntry(newEntry(secondRawName, Type.STRING), entries);

        Assertions.assertEquals(2, entries.size());

        // Check that the sanitized names are different
        // it was a strange behavior when we replace the existed entry with the same name
        Assertions.assertEquals(secondRawName, entries.get("_____").getRawName());
        Assertions.assertEquals(firstRawName, entries.get("______1").getRawName());
    }

    private static Entry addNewEntry(final Entry entry, final Map<String, Entry> entries) {
        final ReplaceFunction replaceFunction = new ReplaceFunction(entries);
        final Entry sanitized = SchemaCompanionUtil.avoidCollision(entry, entries::get, replaceFunction);
        entries.put(sanitized.getName(), sanitized);
        return sanitized;
    }

    private static Schema.Entry newEntry(final String name, final Schema.Type type) {
        return new SchemaImpl.EntryImpl.BuilderImpl().withName(name).withType(type).build();
    }
}
