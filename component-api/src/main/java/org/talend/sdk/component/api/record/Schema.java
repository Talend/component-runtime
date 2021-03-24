/**
 * Copyright (C) 2006-2021 Talend Inc. - www.talend.com
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

import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public interface Schema {

    /**
     * @return the type of this schema.
     */
    Type getType();

    /**
     * @return the nested element schema for arrays.
     */
    Schema getElementSchema();

    /**
     * @return the entries for records.
     */
    List<Entry> getEntries();

    default Entry getEntry(final String name) {
        return Optional
                .ofNullable(this.getEntries()) //
                .orElse(Collections.emptyList()) //
                .stream() //
                .filter((Entry e) -> Objects.equals(e.getName(), name)) //
                .findFirst() //
                .orElse(null);
    }

    /**
     * @return the metadata props
     */
    Map<String, String> getProps();

    /**
     * @param property
     * @return the requested metadata prop
     */
    String getProp(String property);

    enum Type {
        RECORD,
        ARRAY,
        STRING,
        BYTES,
        INT,
        LONG,
        FLOAT,
        DOUBLE,
        BOOLEAN,
        DATETIME
    }

    interface Entry {

        /**
         * @return The name of this entry.
         */
        String getName();

        /**
         * @return The raw name of this entry.
         */
        String getRawName();

        /**
         * @return the raw name of this entry if exists, else return name.
         */
        String getOriginalFieldName();

        /**
         * @return Type of the entry, this determine which other fields are populated.
         */
        Type getType();

        /**
         * @return Is this entry nullable or always valued.
         */
        boolean isNullable();

        /**
         * @param <T> the default value type.
         * @return Default value for this entry.
         */
        <T> T getDefaultValue();

        /**
         * @return For type == record, the element type.
         */
        Schema getElementSchema();

        /**
         * @return Allows to associate to this field a comment - for doc purposes, no use in the runtime.
         */
        String getComment();

        /**
         * @return the metadata props
         */
        Map<String, String> getProps();

        /**
         * @param property
         * @return the requested metadata prop
         */
        String getProp(String property);

        // Map<String, Object> metadata <-- DON'T DO THAT, ENSURE ANY META IS TYPED!

        /**
         * Plain builder matching {@link Entry} structure.
         */
        interface Builder {

            Builder withName(String name);

            Builder withRawName(String rawName);

            Builder withType(Type type);

            Builder withNullable(boolean nullable);

            <T> Builder withDefaultValue(T value);

            Builder withElementSchema(Schema schema);

            Builder withComment(String comment);

            Builder withProps(Map<String, String> props);

            Builder withProp(String key, String value);

            Entry build();

        }
    }

    /**
     * Allows to build a schema.
     */
    interface Builder {

        /**
         * @param type schema type.
         * @return this builder.
         */
        Builder withType(Type type);

        /**
         * @param entry element for either an array or record type.
         * @return this builder.
         */
        Builder withEntry(Entry entry);

        /**
         * @param schema nested element schema.
         * @return this builder.
         */
        Builder withElementSchema(Schema schema);

        /**
         * @param props schema properties
         * @return this builder
         */
        Builder withProps(Map<String, String> props);

        /**
         *
         * @param key the prop key name
         * @param value the prop value
         * @return this builder
         */
        Builder withProp(String key, String value);

        /**
         * @return the described schema.
         */
        Schema build();
    }

    static String sanitizeConnectionName(final String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }

        final char[] original = name.toCharArray();
        final char[] sanitized = new char[original.length];

        final CharsetEncoder ascii = Charset.forName(StandardCharsets.US_ASCII.name()).newEncoder();

        final boolean firstCharIsOK =
                ascii.canEncode(original[0]) && (Character.isLetter(original[0]) || original[0] == '_');

        sanitized[0] = firstCharIsOK ? original[0] : '_';

        for (int i = 1; i < original.length; i++) {
            char current = original[i];
            if ((!ascii.canEncode(current)) || (!Character.isLetterOrDigit(current) && current != '_')) {
                sanitized[i] = '_';
            } else {
                sanitized[i] = current;
            }
        }
        return new String(sanitized);
    }
}
