/**
 * Copyright (C) 2006-2022 Talend Inc. - www.talend.com
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

import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.time.temporal.Temporal;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonValue;

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
     * @return the data entries for records (not contains meta data entries).
     */
    List<Entry> getEntries();

    /**
     * @return the metadata entries for records (not contains ordinary data entries).
     */
    List<Entry> getMetadata();

    /**
     * @return All entries, including data and metadata, of this schema.
     */
    Stream<Entry> getAllEntries();

    default Entry getEntry(final String name) {
        return Optional
                .ofNullable(getEntries()) //
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
     * @param property : property name.
     * @return the requested metadata prop
     */
    String getProp(String property);

    /**
     * Get a property values from schema with its name.
     * 
     * @param name : property's name.
     * @return property's value.
     */
    default JsonValue getJsonProp(final String name) {
        final String prop = this.getProp(name);
        if (prop == null) {
            return null;
        }
        try {
            return Json.createParser(new StringReader(prop)).getValue();
        } catch (RuntimeException ex) {
            return Json.createValue(prop);
        }
    }

    enum Type {
        RECORD(new Class<?>[] { Record.class }),
        ARRAY(new Class<?>[] { Collection.class }),
        STRING(new Class<?>[] { String.class }),
        BYTES(new Class<?>[] { byte[].class, Byte[].class }),
        INT(new Class<?>[] { Integer.class }),
        LONG(new Class<?>[] { Long.class }),
        FLOAT(new Class<?>[] { Float.class }),
        DOUBLE(new Class<?>[] { Double.class }),
        BOOLEAN(new Class<?>[] { Boolean.class }),
        DATETIME(new Class<?>[] { Long.class, Date.class, Temporal.class });

        /** All compatibles Java classes */
        private final Class<?>[] classes;

        Type(final Class<?>[] classes) {
            this.classes = classes;
        }

        /**
         * Check if input can be affected to an entry of this type.
         * 
         * @param input : object.
         * @return true if input is null or ok.
         */
        public boolean isCompatible(final Object input) {
            if (input == null) {
                return true;
            }
            for (final Class<?> clazz : classes) {
                if (clazz.isInstance(input)) {
                    return true;
                }
            }
            return false;
        }
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
         * @return true if this entry is for metadata; false for ordinary data.
         */
        boolean isMetadata();

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
         * @param property : property name.
         * @return the requested metadata prop
         */
        String getProp(String property);

        /**
         * Get a property values from entry with its name.
         * 
         * @param name : property's name.
         * @return property's value.
         */
        default JsonValue getJsonProp(final String name) {
            final String prop = this.getProp(name);
            if (prop == null) {
                return null;
            }
            try {
                return Json.createParser(new StringReader(prop)).getValue();
            } catch (RuntimeException ex) {
                return Json.createValue(prop);
            }
        }

        // Map<String, Object> metadata <-- DON'T DO THAT, ENSURE ANY META IS TYPED!

        /**
         * Plain builder matching {@link Entry} structure.
         */
        interface Builder {

            Builder withName(String name);

            Builder withRawName(String rawName);

            Builder withType(Type type);

            Builder withNullable(boolean nullable);

            Builder withMetadata(boolean metadata);

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

    /**
     * Sanitize name to be avro compatible.
     * 
     * @param name : original name.
     * @return avro compatible name.
     */
    static String sanitizeConnectionName(final String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }

        char current = name.charAt(0);
        final CharsetEncoder ascii = Charset.forName(StandardCharsets.US_ASCII.name()).newEncoder();
        final boolean skipFirstChar = ((!ascii.canEncode(current)) || (!Character.isLetter(current) && current != '_'))
                && name.length() > 1 && (!Character.isDigit(name.charAt(1)));

        final StringBuilder sanitizedBuilder = new StringBuilder();

        if (!skipFirstChar) {
            if (((!Character.isLetter(current)) && current != '_') || (!ascii.canEncode(current))) {
                sanitizedBuilder.append('_');
            } else {
                sanitizedBuilder.append(current);
            }
        }
        for (int i = 1; i < name.length(); i++) {
            current = name.charAt(i);
            if (!ascii.canEncode(current)) {
                if (Character.isLowerCase(current) || Character.isUpperCase(current)) {
                    sanitizedBuilder.append('_');
                } else {
                    final byte[] encoded =
                            Base64.getEncoder().encode(name.substring(i, i + 1).getBytes(StandardCharsets.UTF_8));
                    final String enc = new String(encoded);
                    if (sanitizedBuilder.length() == 0 && Character.isDigit(enc.charAt(0))) {
                        sanitizedBuilder.append('_');
                    }
                    for (int iter = 0; iter < enc.length(); iter++) {
                        if (Character.isLetterOrDigit(enc.charAt(iter))) {
                            sanitizedBuilder.append(enc.charAt(iter));
                        } else {
                            sanitizedBuilder.append('_');
                        }
                    }
                }
            } else if (Character.isLetterOrDigit(current)) {
                sanitizedBuilder.append(current);
            } else {
                sanitizedBuilder.append('_');
            }

        }
        return sanitizedBuilder.toString();
    }

}
