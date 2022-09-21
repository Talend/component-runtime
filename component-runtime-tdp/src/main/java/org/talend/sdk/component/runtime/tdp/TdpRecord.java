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
package org.talend.sdk.component.runtime.tdp;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.record.Schema;

public class TdpRecord implements Record {

    private Map<String, Object> values;

    private final TdpSchema tdpSchema;

    private TdpRecord(final TdpSchema tdpSchema) {
        this.tdpSchema = tdpSchema;
        this.values = new HashMap<>();
    }

    public Map<String, Object> getValues() {
        return values;
    }

    public TdpSchema getTdpSchema() {
        return tdpSchema;
    }

    public void setValues(Map<String, Object> values) {
        this.values = values;
    }

    @Override
    public Schema getSchema() {
        return tdpSchema;
    }

    @Override
    public <T> T get(final Class<T> expectedType, final String name) {
        // noinspection unchecked YOLO
        return (T) values.get(name);
    }

    private void set(final String name, final Object value) {
        values.put(name, value);
    }

    /**
     * Warning, will mutate existing record
     */
    @Override
    public Builder toBuilder() {
        return new Builder(this);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(Schema schema) {
        return new Builder(schema);
    }

    public static Builder builder(Schema schema, Record record) {
        return new Builder(schema, record);
    }

    /**
     * Ugly builder which can either be used to create a new Record instance from scratch or
     * mutate an existing instance under-the-hood.
     * <p>
     * Note: usually both {@link Schema} and {@link Record} instances are immutable, so {@link Record.Builder}
     * implementations deal with this, but here one can leverage setters in our TDP implementations...
     */
    public static class Builder implements Record.Builder {

        private final TdpRecord tdpRecord;

        Builder() {
            TdpSchema mutableSchema = (TdpSchema) TdpSchema.builder().build();
            tdpRecord = new TdpRecord(mutableSchema);
        }

        /**
         * Instanciate a builder which will mutate the provided {@code tdpRecord}
         */
        private Builder(final TdpRecord tdpRecord) {
            this.tdpRecord = tdpRecord;
        }

        private Builder(final Schema schema, final Record record) {
            boolean isTdpRecord = record instanceof TdpRecord;

            tdpRecord = isTdpRecord
                    ? (TdpRecord) record
                    : initializeFromRawRecord(record);

            appendNewSchemaEntries(schema);
        }

        private Builder(final Schema schema) {
            final TdpSchema mutableSchema = TdpSchema.fromExistingSchema(schema);
            tdpRecord = new TdpRecord(mutableSchema);
        }

        private TdpRecord initializeFromRawRecord(Record record) {
            TdpSchema mutableSchema = TdpSchema.fromExistingSchema(record.getSchema());

            TdpRecord tdpRecord = new TdpRecord(mutableSchema);
            record.getSchema().getEntries().forEach(entry -> {
                final Object value = record.get(Object.class, entry);
                tdpRecord.set(entry.getName(), value);
            });

            return tdpRecord;
        }

        private void appendNewSchemaEntries(final Schema newSchema) {
            newSchema.getAllEntries()
                    .filter(entry -> !tdpRecord.tdpSchema.hasEntry(entry.getName()))
                    .forEach(newEntry -> {
                        tdpRecord.tdpSchema.addEntry(newEntry);
                        tdpRecord.set(newEntry.getName(), null);
                    });
        }

        private Schema.Entry findOrBuildEntry(final String name, final Schema.Type type, final boolean nullable) {
            final TdpSchema tdpSchema = (TdpSchema) tdpRecord.getSchema();
            final Schema.Entry entry = tdpSchema.getEntry(name);

            if (entry == null) {
                return tdpSchema.createEntry(name, type, nullable);
            } else {
                return entry;
            }
        }

        @Override
        public Record build() {
            return tdpRecord;
        }

        @Override
        public Object getValue(final String name) {
            return tdpRecord.get(Object.class, name);
        }

        @Override
        public List<Schema.Entry> getCurrentEntries() {
            return null;
        }

        @Override
        public Record.Builder removeEntry(final Schema.Entry schemaEntry) {
            return null;
        }

        @Override
        public Record.Builder updateEntryByName(final String name, final Schema.Entry schemaEntry) {
            return null;
        }

        @Override
        public Record.Builder with(final Schema.Entry entry, final Object value) {
            tdpRecord.set(entry.getName(), value);
            return this;
        }

        @Override
        public Record.Builder withString(final String name, final String value) {
            final Schema.Entry entry = findOrBuildEntry(name, Schema.Type.STRING, true);
            tdpRecord.set(entry.getName(), value);
            return this;
        }

        @Override
        public Record.Builder withString(final Schema.Entry entry, final String value) {
            tdpRecord.set(entry.getName(), value);
            return this;
        }

        @Override
        public Record.Builder withBytes(final String name, final byte[] value) {
            final Schema.Entry entry = findOrBuildEntry(name, Schema.Type.BYTES, true);
            tdpRecord.set(entry.getName(), value);
            return this;
        }

        @Override
        public Record.Builder withBytes(final Schema.Entry entry, final byte[] value) {
            tdpRecord.set(entry.getName(), value);
            return this;
        }

        @Override
        public Record.Builder withDateTime(final String name, final Date value) {
            final Schema.Entry entry = findOrBuildEntry(name, Schema.Type.DATETIME, true);
            tdpRecord.set(entry.getName(), value.getTime());
            return this;
        }

        @Override
        public Record.Builder withDateTime(final Schema.Entry entry, final Date value) {
            tdpRecord.set(entry.getName(), value.getTime());
            return this;
        }

        @Override
        public Record.Builder withDateTime(final String name, final ZonedDateTime value) {
            final Schema.Entry entry = findOrBuildEntry(name, Schema.Type.DATETIME, true);
            tdpRecord.set(entry.getName(), value.toInstant().toEpochMilli());
            return this;
        }

        @Override
        public Record.Builder withDateTime(final Schema.Entry entry, final ZonedDateTime value) {
            tdpRecord.set(entry.getName(), value.toInstant().toEpochMilli());
            return this;
        }

        @Override
        public Record.Builder withTimestamp(final String name, final long value) {
            final Schema.Entry entry = findOrBuildEntry(name, Schema.Type.DATETIME, true);
            tdpRecord.set(entry.getName(), value);
            return this;
        }

        @Override
        public Record.Builder withTimestamp(final Schema.Entry entry, final long value) {
            tdpRecord.set(entry.getName(), value);
            return this;
        }

        @Override
        public Record.Builder withInt(final String name, final int value) {
            final Schema.Entry entry = findOrBuildEntry(name, Schema.Type.INT, true);
            tdpRecord.set(entry.getName(), value);
            return this;
        }

        @Override
        public Record.Builder withInt(final Schema.Entry entry, final int value) {
            tdpRecord.set(entry.getName(), value);
            return this;
        }

        @Override
        public Record.Builder withLong(final String name, final long value) {
            final Schema.Entry entry = findOrBuildEntry(name, Schema.Type.LONG, true);
            tdpRecord.set(entry.getName(), value);
            return this;
        }

        @Override
        public Record.Builder withLong(final Schema.Entry entry, final long value) {
            tdpRecord.set(entry.getName(), value);
            return this;
        }

        @Override
        public Record.Builder withFloat(final String name, final float value) {
            final Schema.Entry entry = findOrBuildEntry(name, Schema.Type.FLOAT, true);
            tdpRecord.set(entry.getName(), value);
            return this;
        }

        @Override
        public Record.Builder withFloat(final Schema.Entry entry, final float value) {
            tdpRecord.set(entry.getName(), value);
            return this;
        }

        @Override
        public Record.Builder withDouble(final String name, final double value) {
            final Schema.Entry entry = findOrBuildEntry(name, Schema.Type.DOUBLE, true);
            tdpRecord.set(entry.getName(), value);
            return this;
        }

        @Override
        public Record.Builder withDouble(final Schema.Entry entry, final double value) {
            tdpRecord.set(entry.getName(), value);
            return this;
        }

        @Override
        public Record.Builder withBoolean(final String name, final boolean value) {
            final Schema.Entry entry = findOrBuildEntry(name, Schema.Type.BOOLEAN, true);
            tdpRecord.set(entry.getName(), value);
            return this;
        }

        @Override
        public Record.Builder withBoolean(final Schema.Entry entry, final boolean value) {
            tdpRecord.set(entry.getName(), value);
            return this;
        }

        @Override
        public Record.Builder withRecord(final Schema.Entry entry, final Record value) {
            throw new UnsupportedOperationException("#withRecord is not supported");
        }

        @Override
        public Record.Builder withRecord(final String name, final Record value) {
            throw new UnsupportedOperationException("#withRecord is not supported");
        }

        @Override
        public <T> Record.Builder withArray(final Schema.Entry entry, final Collection<T> values) {
            throw new UnsupportedOperationException("#withArray is not supported");
        }
    }
}
