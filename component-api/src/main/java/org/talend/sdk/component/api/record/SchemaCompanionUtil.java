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

import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.talend.sdk.component.api.record.Schema.Entry;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SchemaCompanionUtil {

    /**
     * Sanitize name to be avro compatible.
     *
     * @param name : original name.
     *
     * @return avro compatible name.
     */
    public static String sanitizeName(final String name) {
        if (Schema.SKIP_SANITIZE || name == null || name.isEmpty()) {
            return name;
        }

        final CharsetEncoder ascii = StandardCharsets.US_ASCII.newEncoder();
        final StringBuilder sanitizedBuilder = new StringBuilder();
        final char firstLetter = sanitizeFirstLetter(name, ascii);
        if (firstLetter != (char) -1) {
            sanitizedBuilder.append(firstLetter);
        }

        for (int i = 1; i < name.length(); i++) {
            char current = name.charAt(i);
            if (ascii.canEncode(current)) {
                sanitizedBuilder.append(Character.isLetterOrDigit(current) ? current : '_');
            } else {
                if (Character.isLowerCase(current) || Character.isUpperCase(current)) {
                    sanitizedBuilder.append('_');
                } else {
                    final byte[] encoded = base64(name.substring(i, i + 1));
                    final String enc = new String(encoded, StandardCharsets.UTF_8);
                    if (sanitizedBuilder.length() == 0 && Character.isDigit(enc.charAt(0))) {
                        sanitizedBuilder.append('_');
                    }

                    for (int iter = 0; iter < enc.length(); iter++) {
                        final char encodedCurrentChar = enc.charAt(iter);
                        final char sanitizedLetter = Character.isLetterOrDigit(encodedCurrentChar)
                                ? encodedCurrentChar
                                : '_';
                        sanitizedBuilder.append(sanitizedLetter);
                    }
                }
            }

        }
        return sanitizedBuilder.toString();
    }

    private static byte[] base64(final String value) {
        return Base64.getEncoder().encode(value.getBytes(StandardCharsets.UTF_8));
    }

    private static char sanitizeFirstLetter(final String name, final CharsetEncoder ascii) {
        char current = name.charAt(0);
        final boolean skipFirstChar = !(ascii.canEncode(current) && validFirstLetter(current))
                && name.length() > 1 && !Character.isDigit(name.charAt(1));

        // indicates that first letter is not valid, so it has to be skipped.
        // and because the next letter is valid (or can be sanitized) we can use it as first letter.
        if (skipFirstChar) {
            return (char) -1;
        }

        if (validFirstLetter(current) && ascii.canEncode(current)) {
            return current;
        } else {
            return '_';
        }
    }

    private static boolean validFirstLetter(final char value) {
        return Character.isLetter(value) || value == '_';
    }

    /**
     * May return a different entry with different name.
     */
    public static Schema.Entry avoidCollision(final Schema.Entry newEntry,
            final Function<String, Entry> entryGetter,
            final BiConsumer<String, Entry> replaceFunction) {
        if (Schema.SKIP_SANITIZE) {
            return newEntry;
        }

        final Entry alreadyExistedEntry = findCollidedEntry(newEntry, entryGetter);
        if (alreadyExistedEntry == null) {
            // No collision, return new entry.
            return newEntry;
        }

        final boolean matchedToChange = !isEmpty(alreadyExistedEntry.getRawName());
        if (matchedToChange) {
            // the rename has to be applied on entry already inside schema, so replace. (dunno why)
            // replace existed entry with a new name
            final String newSanitizedName = newNotCollidedName(entryGetter, alreadyExistedEntry.getRawName());
            final Entry updatedExistedEntry = alreadyExistedEntry.toBuilder()
                    .withName(newSanitizedName)
                    .build();
            replaceFunction.accept(alreadyExistedEntry.getName(), updatedExistedEntry);
            return newEntry;
        } else if (isEmpty(newEntry.getRawName())) {
            // try to add exactly same raw, skip the add here.
            return null;
        } else {
            // raw name isn't empty, so we need to create a new entry with a new name (sanitized).
            final String newSanitizedName = newNotCollidedName(entryGetter, newEntry.getRawName());
            return newEntry.toBuilder()
                    .withName(newSanitizedName)
                    .build();
        }
    }

    private static Entry findCollidedEntry(final Entry newEntry, final Function<String, Entry> entryGetter) {
        return Optional.ofNullable(entryGetter.apply(newEntry.getName()))
                .filter(retrievedEntry -> !Objects.equals(retrievedEntry, newEntry))
                .orElse(null);
    }

    private static String newNotCollidedName(final Function<String, Entry> entryGetter, final String rawName) {
        final String baseName = sanitizeName(rawName);
        int indexForAnticollision = 1;
        String newName = baseName + "_" + indexForAnticollision;
        while (entryGetter.apply(newName) != null) {
            indexForAnticollision++;
            newName = baseName + "_" + indexForAnticollision;
        }
        return newName;
    }

    private static boolean isEmpty(final String value) {
        return value == null || value.isEmpty();
    }
}
