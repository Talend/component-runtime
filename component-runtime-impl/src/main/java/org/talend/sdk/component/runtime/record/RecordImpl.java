/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.talend.sdk.component.runtime.record;

import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;
import static org.talend.sdk.component.api.record.Schema.Type.ARRAY;
import static org.talend.sdk.component.api.record.Schema.Type.BOOLEAN;
import static org.talend.sdk.component.api.record.Schema.Type.BYTES;
import static org.talend.sdk.component.api.record.Schema.Type.DATETIME;
import static org.talend.sdk.component.api.record.Schema.Type.DOUBLE;
import static org.talend.sdk.component.api.record.Schema.Type.FLOAT;
import static org.talend.sdk.component.api.record.Schema.Type.INT;
import static org.talend.sdk.component.api.record.Schema.Type.LONG;
import static org.talend.sdk.component.api.record.Schema.Type.RECORD;
import static org.talend.sdk.component.api.record.Schema.Type.STRING;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;
import javax.json.bind.annotation.JsonbTransient;
import javax.json.spi.JsonProvider;

import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.record.Schema;

import lombok.Getter;

public final class RecordImpl implements Record {

    private static final RecordConverters RECORD_CONVERTERS = new RecordConverters();

    private final Map<String, Object> values;

    @Getter
    @JsonbTransient
    private final Schema schema;

    private RecordImpl(final Map<String, Object> values, final Schema schema) {
        this.values = values;
        this.schema = schema;
    }

    @Override
    public <T> T get(final Class<T> expectedType, final String name) {
        final Object value = values.get(name);
        if (value == null || expectedType.isInstance(value)) {
            return expectedType.cast(value);
        }

        return RECORD_CONVERTERS.coerce(expectedType, value, name);
    }

    @Override // for debug purposes, don't use it for anything else
    public String toString() {
        try (final Jsonb jsonb = JsonbBuilder
                .create(new JsonbConfig().withFormatting(true).setProperty("johnzon.cdi.activated", false))) {
            return new RecordConverters()
                    .toType(this, JsonObject.class, () -> Json.createBuilderFactory(emptyMap()), JsonProvider::provider,
                            () -> jsonb)
                    .toString();
        } catch (final Exception e) {
            return super.toString();
        }
    }

    // todo: avoid to create the schema each time? this is not that costly but can be saved
    // using a "changeOnValidateFailure" kind of logic
    public static class BuilderImpl implements Builder {

        private final Map<String, Object> values = new HashMap<>(8);

        private final List<Schema.Entry> entries = new ArrayList<>(8);

        // here the game is to add an entry method for each kind of type + its companion with Entry provider

        public Record build() {
            return new RecordImpl(unmodifiableMap(values), new SchemaImpl(RECORD, null, unmodifiableList(entries)));
        }

        public Builder withString(final String name, final String value) {
            return withString(
                    new SchemaImpl.EntryImpl.BuilderImpl().withName(name).withType(STRING).withNullable(true).build(),
                    value);
        }

        public Builder withString(final Schema.Entry entry, final String value) {
            assertType(entry.getType(), STRING);
            return append(entry, value);
        }

        public Builder withBytes(final String name, final byte[] value) {
            return withBytes(
                    new SchemaImpl.EntryImpl.BuilderImpl().withName(name).withType(BYTES).withNullable(true).build(),
                    value);
        }

        public Builder withBytes(final Schema.Entry entry, final byte[] value) {
            assertType(entry.getType(), BYTES);
            return append(entry, value);
        }

        public Builder withDateTime(final String name, final Date value) {
            return withDateTime(
                    new SchemaImpl.EntryImpl.BuilderImpl().withName(name).withType(DATETIME).withNullable(true).build(),
                    value);
        }

        public Builder withDateTime(final Schema.Entry entry, final Date value) {
            if (value == null && !entry.isNullable()) {
                throw new IllegalArgumentException("date '" + entry.getName() + "' is not allowed to be null");
            }
            return withTimestamp(entry, value == null ? -1 : value.getTime());
        }

        public Builder withDateTime(final String name, final ZonedDateTime value) {
            return withDateTime(
                    new SchemaImpl.EntryImpl.BuilderImpl().withName(name).withType(DATETIME).withNullable(true).build(),
                    value);
        }

        public Builder withDateTime(final Schema.Entry entry, final ZonedDateTime value) {
            if (value == null && !entry.isNullable()) {
                throw new IllegalArgumentException("datetime '" + entry.getName() + "' is not allowed to be null");
            }
            return withTimestamp(entry, value == null ? -1 : value.toInstant().toEpochMilli());
        }

        public Builder withTimestamp(final String name, final long value) {
            return withTimestamp(new SchemaImpl.EntryImpl.BuilderImpl()
                    .withName(name)
                    .withType(DATETIME)
                    .withNullable(false)
                    .build(), value);
        }

        public Builder withTimestamp(final Schema.Entry entry, final long value) {
            assertType(entry.getType(), DATETIME);
            return append(entry, value);
        }

        public Builder withInt(final String name, final int value) {
            return withInt(
                    new SchemaImpl.EntryImpl.BuilderImpl().withName(name).withType(INT).withNullable(false).build(),
                    value);
        }

        public Builder withInt(final Schema.Entry entry, final int value) {
            assertType(entry.getType(), INT);
            return append(entry, value);
        }

        public Builder withLong(final String name, final long value) {
            return withLong(
                    new SchemaImpl.EntryImpl.BuilderImpl().withName(name).withType(LONG).withNullable(false).build(),
                    value);
        }

        public Builder withLong(final Schema.Entry entry, final long value) {
            assertType(entry.getType(), LONG);
            return append(entry, value);
        }

        public Builder withFloat(final String name, final float value) {
            return withFloat(
                    new SchemaImpl.EntryImpl.BuilderImpl().withName(name).withType(FLOAT).withNullable(false).build(),
                    value);
        }

        public Builder withFloat(final Schema.Entry entry, final float value) {
            assertType(entry.getType(), FLOAT);
            return append(entry, value);
        }

        public Builder withDouble(final String name, final double value) {
            return withDouble(
                    new SchemaImpl.EntryImpl.BuilderImpl().withName(name).withType(DOUBLE).withNullable(false).build(),
                    value);
        }

        public Builder withDouble(final Schema.Entry entry, final double value) {
            assertType(entry.getType(), DOUBLE);
            return append(entry, value);
        }

        public Builder withBoolean(final String name, final boolean value) {
            return withBoolean(
                    new SchemaImpl.EntryImpl.BuilderImpl().withName(name).withType(BOOLEAN).withNullable(false).build(),
                    value);
        }

        public Builder withBoolean(final Schema.Entry entry, final boolean value) {
            assertType(entry.getType(), BOOLEAN);
            return append(entry, value);
        }

        public Builder withRecord(final Schema.Entry entry, final Record value) {
            assertType(entry.getType(), RECORD);
            if (entry.getElementSchema() == null) {
                throw new IllegalArgumentException("No schema for the nested record");
            }
            return append(entry, value);
        }

        public <T> Builder withArray(final Schema.Entry entry, final Collection<T> values) {
            assertType(entry.getType(), ARRAY);
            if (entry.getElementSchema() == null) {
                throw new IllegalArgumentException("No schema for the collection items");
            }
            // todo: check item type?
            return append(entry, values);
        }

        private void assertType(final Schema.Type actual, final Schema.Type expected) {
            if (actual != expected) {
                throw new IllegalArgumentException("Expected entry type: " + expected + ", got: " + actual);
            }
        }

        private <T> Builder append(final Schema.Entry entry, final T value) {
            if (value != null) {
                values.put(entry.getName(), value);
            } else if (!entry.isNullable()) {
                throw new IllegalArgumentException(entry.getName() + " is not nullable but got a null value");
            }
            entries.add(entry);
            return this;
        }
    }
}
