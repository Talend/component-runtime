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
package org.talend.sdk.component.runtime.record;

import static java.util.Collections.unmodifiableList;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.json.bind.annotation.JsonbTransient;

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

    private Map<String, String> props = new LinkedHashMap<>(0);

    public SchemaImpl(final Type type, final Schema schema, final List<Entry> entries) {
        this.type = type;
        elementSchema = schema;
        this.entries = entries;
    }

    @Override
    public String getProp(final String property) {
        return props.get(property);
    }

    public static class BuilderImpl implements Builder {

        private Type type;

        private Schema elementSchema;

        private List<Entry> entries = new ArrayList<>();

        private Map<String, String> props = new LinkedHashMap<>(0);

        @Override
        public Builder withElementSchema(final Schema schema) {
            if (type != Type.ARRAY && schema != null) {
                throw new IllegalArgumentException("elementSchema is only valid for ARRAY type of schema");
            }
            elementSchema = schema;
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
        public Builder withProp(final String key, final String value) {
            props.put(key, value);
            return this;
        }

        @Override
        public Builder withProps(final Map props) {
            if (props != null) {
                this.props = props;
            }
            return this;
        }

        @Override
        public Schema build() {
            return new SchemaImpl(type, elementSchema, entries == null ? null : unmodifiableList(entries), props);
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EntryImpl implements org.talend.sdk.component.api.record.Schema.Entry {

        // add this for some old code which refer this construct
        public EntryImpl(final String name, final Schema.Type type, final boolean nullable, final Object defaultValue,
                final Schema elementSchema, final String comment, final Map<String, String> props) {
            this.name = name;
            this.type = type;
            this.nullable = nullable;
            this.defaultValue = defaultValue;
            this.elementSchema = elementSchema;
            this.comment = comment;
            this.props = props;
        }

        public EntryImpl(final String name, final String rawName, final Schema.Type type, final boolean nullable,
                final Object defaultValue, final Schema elementSchema, final String comment) {
            this.name = name;
            this.type = type;
            this.nullable = nullable;
            this.defaultValue = defaultValue;
            this.elementSchema = elementSchema;
            this.comment = comment;
        }

        @JsonbTransient
        public String getOriginalFieldName() {
            return rawName != null ? rawName : name;
        }

        /**
         * The name of this entry.
         */
        private String name;

        /**
         * The raw name of this entry.
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

        /**
         * metadata
         */
        private Map<String, String> props = new LinkedHashMap<>(0);

        @Override
        public String getProp(final String property) {
            return props.get(property);
        }

        // Map<String, Object> metadata <-- DON'T DO THAT, ENSURE ANY META IS TYPED!

        public static class BuilderImpl implements Builder {

            private String name;

            private String rawName;

            private Schema.Type type;

            private boolean nullable;

            private Object defaultValue;

            private Schema elementSchema;

            private String comment;

            private final Map<String, String> props = new LinkedHashMap<>(0);

            private static String sanitizeConnectionName(final String name) {
                if (name.isEmpty()) {
                    return name;
                }
                final char[] original = name.toCharArray();
                final boolean skipFirstChar = !Character.isLetter(original[0]) && original[0] != '_';
                final int offset = skipFirstChar ? 1 : 0;
                final char[] sanitized = skipFirstChar ? new char[original.length - offset] : new char[original.length];
                if (!skipFirstChar) {
                    sanitized[0] = original[0];
                }
                for (int i = 1; i < original.length; i++) {
                    if (!Character.isLetterOrDigit(original[i]) && original[i] != '_') {
                        sanitized[i - offset] = '_';
                    } else {
                        sanitized[i - offset] = original[i];
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
                defaultValue = value;
                return this;
            }

            @Override
            public Builder withElementSchema(final Schema schema) {
                elementSchema = schema;
                return this;
            }

            @Override
            public Builder withComment(final String comment) {
                this.comment = comment;
                return this;
            }

            @Override
            public Builder withProp(final String key, final String value) {
                props.put(key, value);
                return this;
            }

            @Override
            public Builder withProps(final Map props) {
                if (props == null) {
                    return this;
                }
                this.props.putAll(props);
                return this;
            }

            @Override
            public Entry build() {
                return new EntryImpl(name, rawName, type, nullable, defaultValue, elementSchema, comment, props);
            }
        }
    }
}
