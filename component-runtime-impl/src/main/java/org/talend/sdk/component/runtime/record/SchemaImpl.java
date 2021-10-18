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

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static org.talend.sdk.component.api.record.Schema.sanitizeConnectionName;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.json.bind.annotation.JsonbTransient;

import org.talend.sdk.component.api.record.Schema;

import lombok.AllArgsConstructor;
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

    @JsonbTransient
    private final transient EntriesOrder entriesOrder;

    public static final String ENTRIES_ORDER_PROP = "talend.fields.order";

    SchemaImpl(final SchemaImpl.BuilderImpl builder) {
        this.type = builder.type;
        this.elementSchema = builder.elementSchema;
        this.entries = unmodifiableList(builder.entries);
        this.metadataEntries = unmodifiableList(builder.metadataEntries);
        this.props = builder.props;
        entriesOrder = EntriesOrderImpl.of(getFieldsOrder());
    }

    @Override
    public Builder toBuilder() {
        Builder builder = new BuilderImpl();
        builder.withType(type).withProps(props).withElementSchema(elementSchema);
        getEntriesOrdered().forEach(entry -> builder.withEntry(entry));
        return builder;
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

    @Override
    @JsonbTransient
    public List<Entry> getEntriesOrdered() {
        return getAllEntries().sorted(entriesOrder).collect(Collectors.toList());
    }

    @Override
    @JsonbTransient
    public List<Entry> getEntriesOrdered(final Comparator<Entry> comparator) {
        return getAllEntries().sorted(comparator).collect(Collectors.toList());
    }

    @Override
    @JsonbTransient
    public List<Entry> getEntriesOrdered(final EntriesOrder entriesOrder) {
        return getAllEntries().sorted(entriesOrder).collect(Collectors.toList());
    }

    @Override
    @JsonbTransient
    public EntriesOrder naturalOrder() {
        return entriesOrder;
    }

    private String getFieldsOrder() {
        String fields = getProp(ENTRIES_ORDER_PROP);
        if (fields == null || fields.isEmpty()) {
            fields = getAllEntries().map(entry -> entry.getName()).collect(Collectors.joining(","));
            props.put(ENTRIES_ORDER_PROP, fields);
        }
        return fields;
    }

    @AllArgsConstructor
    @ToString
    @EqualsAndHashCode
    public static class EntriesOrderImpl implements EntriesOrder {

        @Getter
        private final List<String> fieldsOrder;

        /**
         * Build an EntriesOrder according fields.
         *
         * @param fields the fields ordering
         * @return the order EntriesOrder
         */
        public static EntriesOrder of(final String fields) {
            return new EntriesOrderImpl(fields);
        }

        public EntriesOrderImpl(final String fields) {
            if (fields == null) {
                fieldsOrder = emptyList();
            } else {
                fieldsOrder = Arrays.stream(fields.split(",")).collect(Collectors.toList());
            }
        }

        @Override
        public EntriesOrder moveAfter(final String after, final String name) {
            if (getFieldsOrder().indexOf(after) == -1) {
                throw new IllegalArgumentException(String.format("%s not in schema", after));
            }
            getFieldsOrder().remove(name);
            int destination = getFieldsOrder().indexOf(after);
            if (!(destination + 1 == getFieldsOrder().size())) {
                destination += 1;
            }
            getFieldsOrder().add(destination, name);
            return this;
        }

        @Override
        public EntriesOrder moveBefore(final String before, final String name) {
            if (getFieldsOrder().indexOf(before) == -1) {
                throw new IllegalArgumentException(String.format("%s not in schema", before));
            }
            getFieldsOrder().remove(name);
            getFieldsOrder().add(getFieldsOrder().indexOf(before), name);
            return this;
        }

        @Override
        public EntriesOrder swap(final String name, final String with) {
            Collections.swap(getFieldsOrder(), getFieldsOrder().indexOf(name), getFieldsOrder().indexOf(with));
            return this;
        }
    }

    public static class BuilderImpl implements Builder {

        private Type type;

        private Schema elementSchema;

        private final List<Entry> entries = new ArrayList<>();

        private final List<Entry> metadataEntries = new ArrayList<>();

        private Map<String, String> props = new LinkedHashMap<>(0);

        private List<String> entriesOrder = new ArrayList<>();

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
            entriesOrder.add(entry.getName());
            return this;
        }

        @Override
        public Builder withEntryAfter(final String after, final Entry entry) {
            if (entriesOrder.indexOf(after) == -1) {
                throw new IllegalArgumentException(String.format("%s not in schema", after));
            }
            withEntry(entry);
            entriesOrder.remove(entry.getName());
            int destination = entriesOrder.indexOf(after);
            if (!(destination + 1 == entriesOrder.size())) {
                destination += 1;
            }
            entriesOrder.add(destination, entry.getName());
            return this;
        }

        @Override
        public Builder withEntryBefore(final String before, final Entry entry) {
            if (entriesOrder.indexOf(before) == -1) {
                throw new IllegalArgumentException(String.format("%s not in schema", before));
            }
            withEntry(entry);
            entriesOrder.remove(entry.getName());
            entriesOrder.add(entriesOrder.indexOf(before), entry.getName());
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
        public Builder remove(final Entry entry) {
            if (entry.isMetadata()) {
                metadataEntries.remove(entry);
            } else {
                entries.remove(entry);
            }
            entriesOrder.remove(entry.getName());
            return this;
        }

        @Override
        public Builder moveAfter(final String after, final String name) {
            if (entriesOrder.indexOf(after) == -1) {
                throw new IllegalArgumentException(String.format("%s not in schema", after));
            }
            entriesOrder.remove(name);
            int destination = entriesOrder.indexOf(after);
            if (!(destination + 1 == entriesOrder.size())) {
                destination += 1;
            }
            entriesOrder.add(destination, name);
            return this;
        }

        @Override
        public Builder moveBefore(final String before, final String name) {
            if (entriesOrder.indexOf(before) == -1) {
                throw new IllegalArgumentException(String.format("%s not in schema", before));
            }
            entriesOrder.remove(name);
            entriesOrder.add(entriesOrder.indexOf(before), name);
            return this;
        }

        @Override
        public Builder swap(final String name, final String with) {
            Collections.swap(entriesOrder, entriesOrder.indexOf(name), entriesOrder.indexOf(with));
            return this;
        }

        @Override
        public Builder remove(final String name) {
            final Entry entry = Stream
                    .concat(entries.stream(), metadataEntries.stream())
                    .filter(e -> name.equals(e.getName()))
                    .findFirst()
                    .get();
            if (entry == null) {
                throw new IllegalArgumentException(String.format("%s not in schema", name));
            }
            return remove(entry);
        }

        @Override
        public Schema build() {
            this.props.put(ENTRIES_ORDER_PROP, entriesOrder.stream().collect(Collectors.joining(",")));
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
