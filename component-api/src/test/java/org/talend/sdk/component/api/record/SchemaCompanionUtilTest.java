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
package org.talend.sdk.component.api.record;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.talend.sdk.component.api.record.Schema.Entry;
import org.talend.sdk.component.api.record.Schema.Type;
import org.talend.sdk.component.api.test.MockEntry;

import lombok.RequiredArgsConstructor;

class SchemaCompanionUtilTest {

    @Test
    void sanitizationPatternBasedCheck() {
        final Pattern checkPattern = Pattern.compile("^[A-Za-z_][A-Za-z0-9_]*$");
        final String nonAscii1 = SchemaCompanionUtil.sanitizeConnectionName("30_39歳");
        Assertions.assertTrue(checkPattern.matcher(nonAscii1).matches(), "'" + nonAscii1 + "' don't match");

        final String ch1 = SchemaCompanionUtil.sanitizeConnectionName("世帯数分布");
        final String ch2 = SchemaCompanionUtil.sanitizeConnectionName("抽出率調整");
        Assertions.assertTrue(checkPattern.matcher(ch1).matches(), "'" + ch1 + "' don't match");
        Assertions.assertTrue(checkPattern.matcher(ch2).matches(), "'" + ch2 + "' don't match");
        Assertions.assertNotEquals(ch1, ch2);

        final Random rnd = new Random();
        final byte[] array = new byte[20]; // length is bounded by 7
        for (int i = 0; i < 150; i++) {
            rnd.nextBytes(array);
            final String randomString = new String(array, StandardCharsets.UTF_8);
            final String sanitize = SchemaCompanionUtil.sanitizeConnectionName(randomString);
            Assertions.assertTrue(checkPattern.matcher(sanitize).matches(), "'" + sanitize + "' don't match");

            final String sanitize2 = SchemaCompanionUtil.sanitizeConnectionName(sanitize);
            Assertions.assertEquals(sanitize, sanitize2);
        }
    }

    @Test
    void sanitizeNull() {
        Assertions.assertNull(SchemaCompanionUtil.sanitizeConnectionName(null));
    }

    @MethodSource("sanitizeCasesSource")
    @ParameterizedTest
    void sanitizeCases(final String expected, final String rawName) {
        Assertions.assertEquals(expected, SchemaCompanionUtil.sanitizeConnectionName(rawName));
    }

    public static Stream<Arguments> sanitizeCasesSource() {
        return Stream.of(
                Arguments.of("", ""),
                Arguments.of("_", "$"),
                Arguments.of("_", "1"),
                Arguments.of("_", "é"),
                Arguments.of("H", "éH"),
                Arguments.of("_1", "é1"),
                Arguments.of("H_lloWorld", "HélloWorld"),
                Arguments.of("oid", "$oid"),
                Arguments.of("Hello_World_", " Hello World "),
                Arguments.of("_23HelloWorld", "123HelloWorld"),

                Arguments.of("Hello_World_", "Hello-World$"),
                Arguments.of("_656", "5656"),
                Arguments.of("_____", "Істина"),
                // not very good test, because it depends on base64 encoding
                // (but I wanted to check that part in coverage by this test)
                Arguments.of("_5q2z", "9歳"));
    }

    @Test
    void noCollisionDuplicatedEntry() {
        final String name = "name_b";

        final Entry entry1 = newEntry(name, Type.STRING);
        final Entry entry2 = newEntry(name, Type.STRING);

        final Map<String, Entry> entries = new HashMap<>();
        addNewEntry(entry1, entries);
        addNewEntry(entry2, entries);

        // second entry with the same name was ignored (can't be two same raw names)
        Assertions.assertEquals(1, entries.size());

        Assertions.assertNull(entries.get(name).getRawName());
        Assertions.assertEquals(name, entries.get("name_b").getName());
    }

    @Test
    void avoidCollisionWithSanitization() {
        final String name = "name_b";

        final Entry entry1 = newEntry(name, Type.STRING);
        final Entry entry2 = newEntry(name, Type.INT);

        final Map<String, Entry> entries = new HashMap<>();
        addNewEntry(entry1, entries);
        addNewEntry(entry2, entries);

        // second entry with the same name was ignored (can't be two same raw names)
        Assertions.assertEquals(1, entries.size());

        Assertions.assertNull(entries.get(name).getRawName());
        Assertions.assertEquals(name, entries.get("name_b").getName());
        // we remain the first entry.
        Assertions.assertEquals(Type.STRING, entries.get("name_b").getType());
    }

    @Test
    void avoidCollisionEqualLengthCyrillicNames() {
        final String firstRawName = "Світло";
        final String secondRawName = "Мріяти";
        final String thirdRawName = "Копати";

        final Entry entry1 = newEntry(firstRawName, Type.STRING);
        final Entry entry2 = newEntry(secondRawName, Type.STRING);
        final Entry entry3 = newEntry(thirdRawName, Type.STRING);

        final Map<String, Entry> entries = new HashMap<>();
        addNewEntry(entry1, entries);
        addNewEntry(entry2, entries);
        addNewEntry(entry3, entries);

        Assertions.assertEquals(3, entries.size());

        // Check that the sanitized names are different
        // it was a strange behavior when we replace the existed entry with the same name
        Assertions.assertEquals(thirdRawName, entries.get("_____").getRawName());
        Assertions.assertEquals(secondRawName, entries.get("______2").getRawName());
        Assertions.assertEquals(firstRawName, entries.get("______1").getRawName());
    }

    @Test
    void avoidCollisionNormalNameFirst() {
        final String firstRawName = "name_b";
        final String secondRawName = "1name_b";

        final Entry entry1 = newEntry(firstRawName, Type.STRING);
        final Entry entry2 = newEntry(secondRawName, Type.STRING);

        final Map<String, Entry> entries = new HashMap<>();
        addNewEntry(entry1, entries);
        addNewEntry(entry2, entries);

        Assertions.assertEquals(2, entries.size());

        // Check that the sanitized names are different
        // it was a strange behavior when we replace the existed entry with the same name
        Assertions.assertNull(entries.get("name_b").getRawName());
        Assertions.assertEquals(firstRawName, entries.get("name_b").getName());
        Assertions.assertEquals(secondRawName, entries.get("name_b_1").getRawName());
    }

    @Test
    void avoidCollisionNormalNameLast() {
        final String firstRawName = "1name_b";
        final String secondRawName = "name_b";

        final Entry entry1 = newEntry(firstRawName, Type.STRING);
        final Entry entry2 = newEntry(secondRawName, Type.STRING);

        final Map<String, Entry> entries = new HashMap<>();
        addNewEntry(entry1, entries);
        addNewEntry(entry2, entries);

        Assertions.assertEquals(2, entries.size());

        // Check that the sanitized names are different
        // it was a strange behavior when we replace the existed entry with the same name
        Assertions.assertEquals(firstRawName, entries.get("name_b_1").getRawName());
        Assertions.assertNull(entries.get("name_b").getRawName());
        Assertions.assertEquals(secondRawName, entries.get("name_b").getName());
    }

    private static Schema.Entry newEntry(final String name, final Schema.Type type) {
        final String sanitizedName = SchemaCompanionUtil.sanitizeConnectionName(name);
        return MockEntry.internalBuilder()
                .withName(sanitizedName)
                .withRawName(name.equals(sanitizedName) ? null : name)
                .withType(type)
                .build();
    }

    private static void addNewEntry(final Entry entry, final Map<String, Entry> entries) {
        final ReplaceFunction replaceFunction = new ReplaceFunction(entries);
        final Entry sanitized = SchemaCompanionUtil.avoidCollision(entry, entries::get, replaceFunction);
        if (sanitized != null) {
            entries.put(sanitized.getName(), sanitized);
        }
    }

    @RequiredArgsConstructor
    private static final class ReplaceFunction implements BiConsumer<String, Entry> {

        private final Map<String, Entry> entries;

        @Override
        public void accept(final String s, final Entry entry) {
            entries.remove(s);
            entries.put(entry.getName(), entry);
        }
    }
}
