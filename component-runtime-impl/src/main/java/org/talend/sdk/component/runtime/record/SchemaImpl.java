/**
 * Copyright (C) 2006-2020 Talend Inc. - www.talend.com
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

import static java.util.Collections.unmodifiableList;

import java.util.ArrayList;
import java.util.List;

import org.talend.sdk.component.api.record.Schema;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SchemaImpl implements Schema {

    private Type type;

    private Schema elementSchema;

    private List<Entry> entries;

    public static class BuilderImpl implements Builder {

        private Type type;

        private Schema elementSchema;

        private List<Entry> entries = new ArrayList<>();

        @Override
        public Builder withElementSchema(final Schema schema) {
            if (type != Type.ARRAY && schema != null) {
                throw new IllegalArgumentException("elementSchema is only valid for ARRAY type of schema");
            }
            this.elementSchema = schema;
            return this;
        }

        @Override
        public Builder withType(final Type type) {
            this.type = type;
            return this;
        }

        @Override
        public Builder withEntry(final Entry entry) {
            if (type != Type.RECORD) {
                throw new IllegalArgumentException("entry is only valid for RECORD type of schema");
            }
            if (entries == null) {
                entries = new ArrayList<>();
            }
            entries.add(entry);
            return this;
        }

        @Override
        public Schema build() {
            return new SchemaImpl(type, elementSchema, entries == null ? null : unmodifiableList(entries));
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EntryImpl implements org.talend.sdk.component.api.record.Schema.Entry {

        // add this for some old code which refer this construct
        public EntryImpl(final String name, final Schema.Type type, final boolean nullable, final Object defaultValue,
                final Schema elementSchema, final String comment) {
            this.name = name;
            this.type = type;
            this.nullable = nullable;
            this.defaultValue = defaultValue;
            this.elementSchema = elementSchema;
            this.comment = comment;
        }

        public String getLabel() {
            return rawName != null ? rawName : name;
        }

        /**
         * The name of this entry.
         */
        private String name;

        /**
         * The label of this entry.
         */
        private String rawName;

        /**
         * Type of the entry, this determine which other fields are populated.
         */
        private Schema.Type type;

        /**
         * Is this entry nullable or always valued.
         */
        private boolean nullable;

        /**
         * Default value for this entry.
         */
        private Object defaultValue;

        /**
         * For type == record, the element type.
         */
        private Schema elementSchema;

        /**
         * Allows to associate to this field a comment - for doc purposes, no use in the runtime.
         */
        private String comment;

        // Map<String, Object> metadata <-- DON'T DO THAT, ENSURE ANY META IS TYPED!

        public static class BuilderImpl implements Builder {

            private String name;

            private String rawName;

            private Schema.Type type;

            private boolean nullable;

            private Object defaultValue;

            private Schema elementSchema;

            private String comment;

            private static String sanitizeConnectionName(final String name) {
                if (name.isEmpty()) {
                    return name;
                }
                final char[] original = name.toCharArray();
                final char[] sanitized = new char[original.length];
                for (int i = 0; i < original.length; i++) {
                    char c = original[i];

                    if (i == 0) {
                        if (Character.isLetter(c) || c == '_') {
                            sanitized[i] = c;
                        } else {
                            sanitized[i] = '_';
                        }

                        continue;
                    }

                    if (Character.isLetterOrDigit(c) || c == '_') {
                        sanitized[i] = c;
                    } else {
                        sanitized[i] = '_';
                    }
                }
                return new String(sanitized);
            }

            @Override
            public Builder withName(final String name) {
                this.name = sanitizeConnectionName(name);
                // if raw name is changed as follow name rule, use label to store raw name
                // if not changed, not set label to save space
                if (!name.equals(this.name)) {
                    withRawName(name);
                }
                return this;
            }

            @Override
            public Builder withRawName(final String rawName) {
                this.rawName = rawName;
                return this;
            }

            @Override
            public Builder withType(final Type type) {
                this.type = type;
                return this;
            }

            @Override
            public Builder withNullable(final boolean nullable) {
                this.nullable = nullable;
                return this;
            }

            @Override
            public <T> Builder withDefaultValue(final T value) {
                this.defaultValue = value;
                return this;
            }

            @Override
            public Builder withElementSchema(final Schema schema) {
                this.elementSchema = schema;
                return this;
            }

            @Override
            public Builder withComment(final String comment) {
                this.comment = comment;
                return this;
            }

            @Override
            public Entry build() {
                return new EntryImpl(name, rawName, type, nullable, defaultValue, elementSchema, comment);
            }
        }
    }
}
