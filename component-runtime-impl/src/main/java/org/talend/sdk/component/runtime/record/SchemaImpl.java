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

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.json.bind.annotation.JsonbTransient;

import org.talend.sdk.component.api.record.Schema;

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

    @Override
    public Builder toBuilder() {
        final Builder builder = new BuilderImpl()
                .withType(this.type)
                .withElementSchema(this.elementSchema)
                .withProps(this.props
                        .entrySet()
                        .stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
        this.getAllEntries().forEach(builder::withEntry);
        return builder;
    }

    public static class BuilderImpl implements Schema.Builder {

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
            final Entry entryToAdd = Schema.avoidCollision(entry, this::getAllEntries, this::replaceEntry);
            if (entryToAdd == null) {
                // mean try to add entry with same name.
                throw new IllegalArgumentException("Entry with name " + entry.getName() + " already exist in schema");
            }
            if (entry.isMetadata()) {
                this.metadataEntries.add(entryToAdd);
            } else {
                this.entries.add(entryToAdd);
            }

            return this;
        }

        private void replaceEntry(final String name, final Schema.Entry entry) {
            boolean fromEntries = this.replaceEntryFrom(this.entries, name, entry);
            if (!fromEntries) {
                this.replaceEntryFrom(this.metadataEntries, name, entry);
            }
        }

        private boolean replaceEntryFrom(final List<Entry> currentEntries, final String entryName,
                final Schema.Entry entry) {
            for (int index = 0; index < currentEntries.size(); index++) {
                if (Objects.equals(entryName, currentEntries.get(index).getName())) {
                    currentEntries.set(index, entry);
                    return true;
                }
            }
            return false;
        }

        private Stream<Entry> getAllEntries() {
            return Stream.concat(this.entries.stream(), this.metadataEntries.stream());
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

}
