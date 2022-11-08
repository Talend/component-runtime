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
package org.talend.sdk.component.runtime.record;

import static java.util.Collections.unmodifiableList;
import static org.talend.sdk.component.api.record.Schema.sanitizeConnectionName;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.json.bind.annotation.JsonbTransient;

import org.talend.sdk.component.api.record.Schema;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@EqualsAndHashCode
@ToString
public class SchemaImpl implements Schema {

    @Getter
    private final Type type;

    @Getter
    private final Schema elementSchema;

    @Getter
    private final List<Entry> entries;

    @JsonbTransient
    private final List<Entry> metadataEntries;

    @Getter
    private final Map<String, String> props;

    SchemaImpl(final SchemaImpl.BuilderImpl builder) {
        this.type = builder.type;
        this.elementSchema = builder.elementSchema;
        this.entries = unmodifiableList(builder.entries);
        this.metadataEntries = unmodifiableList(builder.metadataEntries);
        this.props = builder.props;
    }

    @Override
    public String getProp(final String property) {
        return props.get(property);
    }

    @Override
    public List<Entry> getMetadata() {
        return this.metadataEntries;
    }

    @Override
    @JsonbTransient
    public Stream<Entry> getAllEntries() {
        return Stream.concat(this.metadataEntries.stream(), this.entries.stream());
    }

    public static class BuilderImpl implements Builder {

        private Type type;

        private Schema elementSchema;

        private final List<Entry> entries = new ArrayList<>();

        private final List<Entry> metadataEntries = new ArrayList<>();

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
            if (entry.isMetadata()) {
                this.metadataEntries.add(entry);
            } else {
                entries.add(entry);
            }
            return this;
        }

        @Override
        public Builder withProp(final String key, final String value) {
            props.put(key, value);
            return this;
        }

        @Override
        public Builder withProps(final Map<String, String> props) {
            if (props != null) {
                this.props = props;
            }
            return this;
        }

        @Override
        public Schema build() {
            return new SchemaImpl(this);
        }
    }

    @Data
    public static class EntryImpl implements org.talend.sdk.component.api.record.Schema.Entry {

        EntryImpl(final EntryImpl.BuilderImpl builder) {
            this.name = builder.name;
            this.rawName = builder.rawName;
            this.type = builder.type;
            this.nullable = builder.nullable;
            this.metadata = builder.metadata;
            this.defaultValue = builder.defaultValue;
            this.elementSchema = builder.elementSchema;
            this.comment = builder.comment;

            if (builder.props != null) {
                this.props.putAll(builder.props);
            }
        }

        /**
         * if raw name is changed as follow name rule, use label to store raw name
         * if not changed, not set label to save space
         * 
         * @param name incoming entry name
         */
        private void initNames(final String name) {
            this.name = sanitizeConnectionName(name);
            if (!name.equals(this.name)) {
                rawName = name;
            }
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

        private final boolean metadata;

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

        public static class BuilderImpl implements Builder {

            private String name;

            private String rawName;

            private Schema.Type type;

            private boolean nullable;

            private boolean metadata = false;

            private Object defaultValue;

            private Schema elementSchema;

            private String comment;

            private final Map<String, String> props = new LinkedHashMap<>(0);

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
            public Builder withMetadata(final boolean metadata) {
                this.metadata = metadata;
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
                return new EntryImpl(this);
            }
        }
    }
}
