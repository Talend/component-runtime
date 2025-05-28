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

import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import javax.json.bind.annotation.JsonbTransient;

import org.talend.sdk.component.api.record.OrderedMap;
import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.api.record.SchemaCompanionUtil;
import org.talend.sdk.component.api.record.SchemaProperty;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

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
    private final EntriesOrder entriesOrder;

    @Getter
    @JsonbTransient
    private Map<String, Entry> entryMap = new HashMap<>();

    public static final String ENTRIES_ORDER_PROP = "talend.fields.order";

    SchemaImpl(final SchemaImpl.BuilderImpl builder) {
        this.type = builder.type;
        this.elementSchema = builder.elementSchema;
        this.entries = unmodifiableList(builder.entries.streams().collect(toList()));
        this.metadataEntries = unmodifiableList(builder.metadataEntries.streams().collect(toList()));
        this.props = builder.props;
        entriesOrder = EntriesOrder.of(getFieldsOrder());
        getAllEntries().forEach(e -> entryMap.put(e.getName(), e));
    }

    /**
     * Optimized hashcode method (do not enter inside field hashcode, just getName, ignore props fields).
     *
     * @return hashcode.
     */
    @Override
    public int hashCode() {
        final String e1 =
                this.entries != null ? this.entries.stream().map(Entry::getName).collect(joining(",")) : "";
        final String m1 = this.metadataEntries != null
                ? this.metadataEntries.stream().map(Entry::getName).collect(joining(","))
                : "";

        return Objects.hash(this.type, this.elementSchema, e1, m1);
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof SchemaImpl)) {
            return false;
        }
        final SchemaImpl other = (SchemaImpl) obj;
        if (!other.canEqual(this)) {
            return false;
        }
        return Objects.equals(this.type, other.type)
                && Objects.equals(this.elementSchema, other.elementSchema)
                && Objects.equals(this.entries, other.entries)
                && Objects.equals(this.metadataEntries, other.metadataEntries)
                && Objects.equals(this.props, other.props);
    }

    protected boolean canEqual(final SchemaImpl other) {
        return true;
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
    public Builder toBuilder() {
        final Builder builder = new BuilderImpl()
                .withType(this.type)
                .withElementSchema(this.elementSchema)
                .withProps(this.props
                        .entrySet()
                        .stream()
                        .collect(toMap(Map.Entry::getKey, Map.Entry::getValue)));
        getEntriesOrdered().forEach(builder::withEntry);
        return builder;
    }

    @Override
    @JsonbTransient
    public List<Entry> getEntriesOrdered() {
        return getAllEntries().sorted(entriesOrder).collect(toList());
    }

    @Override
    @JsonbTransient
    public EntriesOrder naturalOrder() {
        return entriesOrder;
    }

    private String getFieldsOrder() {
        String fields = getProp(ENTRIES_ORDER_PROP);
        if (fields == null || fields.isEmpty()) {
            fields = getAllEntries().map(Entry::getName).collect(joining(","));
            props.put(ENTRIES_ORDER_PROP, fields);
        }
        return fields;
    }

    public static class BuilderImpl implements Builder {

        private Type type;

        private Schema elementSchema;

        private final OrderedMap<Schema.Entry> entries = new OrderedMap<>(Schema.Entry::getName);

        private final OrderedMap<Schema.Entry> metadataEntries = new OrderedMap<>(Schema.Entry::getName);

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
            final Entry entryToAdd = SchemaCompanionUtil.avoidCollision(entry,
                    this::getEntry,
                    this::replaceEntry);
            if (entryToAdd == null) {
                // mean try to add entry with same name.
                throw new IllegalArgumentException("Entry with name " + entry.getName() + " already exist in schema");
            }
            if (entryToAdd.isMetadata()) {
                this.metadataEntries.addValue(entryToAdd);
            } else {
                this.entries.addValue(entryToAdd);
            }

            entriesOrder.add(entryToAdd.getName());
            return this;
        }

        @Override
        public Builder withEntryAfter(final String after, final Entry entry) {
            withEntry(entry);
            return moveAfter(after, entry.getName());
        }

        @Override
        public Builder withEntryBefore(final String before, final Entry entry) {
            withEntry(entry);
            return moveBefore(before, entry.getName());
        }

        private void replaceEntry(final String name, final Schema.Entry entry) {
            if (this.entries.getValue(name) != null) {
                this.entries.replace(name, entry);
            } else if (this.metadataEntries.getValue(name) != null) {
                this.metadataEntries.replace(name, entry);
            }
        }

        private Stream<Entry> getAllEntries() {
            return Stream.concat(this.entries.streams(), this.metadataEntries.streams());
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
        public Builder remove(final String name) {
            final Entry entry = this.getEntry(name);
            if (entry == null) {
                throw new IllegalArgumentException(String.format("%s not in schema", name));
            }
            return this.remove(entry);
        }

        @Override
        public Builder remove(final Entry entry) {
            if (entry != null) {
                if (entry.isMetadata()) {
                    if (this.metadataEntries.getValue(entry.getName()) != null) {
                        metadataEntries.removeValue(entry);
                    }
                } else if (this.entries.getValue(entry.getName()) != null) {
                    entries.removeValue(entry);
                }
                entriesOrder.remove(entry.getName());
            }
            return this;
        }

        @Override
        public Builder moveAfter(final String after, final String name) {
            if (entriesOrder.indexOf(after) == -1) {
                throw new IllegalArgumentException(String.format("%s not in schema", after));
            }
            entriesOrder.remove(name);
            int destination = entriesOrder.indexOf(after) + 1;
            if (destination < entriesOrder.size()) {
                entriesOrder.add(destination, name);
            } else {
                entriesOrder.add(name);
            }
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
        public Schema build() {
            if (this.entriesOrder != null && !this.entriesOrder.isEmpty()) {
                this.props.put(ENTRIES_ORDER_PROP, entriesOrder.stream().collect(joining(",")));
            }
            return new SchemaImpl(this);
        }

        @Override
        public Schema build(final Comparator<Entry> order) {
            final String entriesOrderProp =
                    this.getAllEntries().sorted(order).map(Entry::getName).collect(joining(","));
            this.props.put(ENTRIES_ORDER_PROP, entriesOrderProp);

            return new SchemaImpl(this);
        }

        private Schema.Entry getEntry(final String name) {
            Entry entry = this.entries.getValue(name);
            if (entry == null) {
                entry = this.metadataEntries.getValue(name);
            }
            return entry;
        }
    }

    /**
     * {@link org.talend.sdk.component.api.record.Schema.Entry} implementation.
     */
    @EqualsAndHashCode
    @ToString
    public static class EntryImpl implements org.talend.sdk.component.api.record.Schema.Entry {

        private EntryImpl(final EntryImpl.BuilderImpl builder) {
            this.name = builder.name;
            this.rawName = builder.rawName;
            if (builder.type == null && builder.logicalType != null) {
                this.type = builder.logicalType.storageType();
            } else {
                this.type = builder.type;
            }
            this.nullable = builder.nullable;
            this.errorCapable = builder.errorCapable;
            this.metadata = builder.metadata;
            this.defaultValue = builder.defaultValue;
            this.elementSchema = builder.elementSchema;
            this.comment = builder.comment;
            this.props.putAll(builder.props);
        }

        /**
         * The name of this entry.
         */
        private final String name;

        /**
         * The raw name of this entry.
         */
        private final String rawName;

        /**
         * Type of the entry, this determine which other fields are populated.
         */
        private final Schema.Type type;

        /**
         * Is this entry nullable or always valued.
         */
        private final boolean nullable;

        /**
         * Is this entry can be in error.
         */
        private final boolean errorCapable;

        /**
         * Is this entry a metadata entry.
         */
        private final boolean metadata;

        /**
         * Default value for this entry.
         */
        private final Object defaultValue;

        /**
         * For type == record, the element type.
         */
        private final Schema elementSchema;

        /**
         * Allows to associate to this field a comment - for doc purposes, no use in the runtime.
         */
        private final String comment;

        /**
         * metadata
         */
        private final Map<String, String> props = new LinkedHashMap<>(0);

        @Override
        @JsonbTransient
        public String getOriginalFieldName() {
            return rawName != null ? rawName : name;
        }

        @Override
        public String getProp(final String property) {
            return this.props.get(property);
        }

        @Override
        public Entry.Builder toBuilder() {
            return new EntryImpl.BuilderImpl(this);
        }

        @Override
        public String getName() {
            return this.name;
        }

        @Override
        public String getRawName() {
            return this.rawName;
        }

        @Override
        public Type getType() {
            return this.type;
        }

        @Override
        public boolean isNullable() {
            return this.nullable;
        }

        @Override
        public boolean isErrorCapable() {
            return this.errorCapable;
        }

        @Override
        public boolean isMetadata() {
            return this.metadata;
        }

        @Override
        public Object getDefaultValue() {
            return this.defaultValue;
        }

        @Override
        public Schema getElementSchema() {
            return this.elementSchema;
        }

        @Override
        public String getComment() {
            return this.comment;
        }

        @Override
        public Map<String, String> getProps() {
            return this.props;
        }

        @Override
        public boolean isValid() {
            String property = this.getProp(SchemaProperty.ENTRY_IS_ON_ERROR);
            if (property == null) {
                return true;
            }
            return !Boolean.parseBoolean(property);
        }

        /**
         * Plain builder matching {@link Entry} structure.
         */
        public static class BuilderImpl implements Entry.Builder {

            private String name;

            private String rawName;

            private Schema.Type type;

            private boolean nullable;

            private boolean errorCapable;

            private boolean metadata = false;

            private Object defaultValue;

            private Schema elementSchema;

            private String comment;

            private SchemaProperty.LogicalType logicalType;

            private final Map<String, String> props = new LinkedHashMap<>(0);

            public BuilderImpl() {
            }

            private BuilderImpl(final Entry entry) {
                this.name = entry.getName();
                this.rawName = entry.getRawName();
                this.nullable = entry.isNullable();
                this.errorCapable = entry.isErrorCapable();
                this.type = entry.getType();
                this.comment = entry.getComment();
                this.elementSchema = entry.getElementSchema();
                this.defaultValue = entry.getDefaultValue();
                this.metadata = entry.isMetadata();
                this.props.putAll(entry.getProps());
            }

            public Builder withName(final String name) {
                this.name = SchemaCompanionUtil.sanitizeConnectionName(name);
                // if raw name is changed as follow name rule, use label to store raw name
                // if not changed, not set label to save space
                if (!name.equals(this.name)) {
                    this.rawName = name;
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
            public Builder withLogicalType(final SchemaProperty.LogicalType logicalType) {
                this.logicalType = logicalType;
                this.props.put(SchemaProperty.LOGICAL_TYPE, logicalType.key());
                return this;
            }

            @Override
            public Builder withNullable(final boolean nullable) {
                this.nullable = nullable;
                return this;
            }

            @Override
            public Builder withErrorCapable(final boolean errorCapable) {
                this.errorCapable = errorCapable;
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

            public Entry build() {
                return new EntryImpl(this);
            }

        }
    }

}
