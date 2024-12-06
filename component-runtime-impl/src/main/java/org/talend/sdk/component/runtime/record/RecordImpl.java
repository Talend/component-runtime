/**
 * Copyright (C) 2006-2024 Talend Inc. - www.talend.com
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

import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableMap;
import static java.util.stream.Collectors.joining;
import static org.talend.sdk.component.api.record.Schema.Type.ARRAY;
import static org.talend.sdk.component.api.record.Schema.Type.BOOLEAN;
import static org.talend.sdk.component.api.record.Schema.Type.BYTES;
import static org.talend.sdk.component.api.record.Schema.Type.DATETIME;
import static org.talend.sdk.component.api.record.Schema.Type.DECIMAL;
import static org.talend.sdk.component.api.record.Schema.Type.DOUBLE;
import static org.talend.sdk.component.api.record.Schema.Type.FLOAT;
import static org.talend.sdk.component.api.record.Schema.Type.INT;
import static org.talend.sdk.component.api.record.Schema.Type.LONG;
import static org.talend.sdk.component.api.record.Schema.Type.RECORD;
import static org.talend.sdk.component.api.record.Schema.Type.STRING;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.Temporal;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;
import javax.json.bind.annotation.JsonbTransient;
import javax.json.bind.config.PropertyOrderStrategy;
import javax.json.spi.JsonProvider;

import org.talend.sdk.component.api.record.OrderedMap;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.api.record.Schema.EntriesOrder;
import org.talend.sdk.component.api.record.Schema.Entry;

import lombok.EqualsAndHashCode;
import lombok.Getter;

@EqualsAndHashCode
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
        final String entryName = Schema.sanitizeConnectionName(name);
        final Object value = values.get(entryName);
        // here mean get(Object.class, name) return origin store type, like DATETIME return long, is expected?
        if (value == null || expectedType.isInstance(value)) {
            return expectedType.cast(value);
        }

        return RECORD_CONVERTERS.coerce(expectedType, value, entryName);
    }

    @Override // for debug purposes, don't use it for anything else
    public String toString() {
        try (final Jsonb jsonb = JsonbBuilder
                .create(new JsonbConfig()
                        .withFormatting(true)
                        .withPropertyOrderStrategy(PropertyOrderStrategy.LEXICOGRAPHICAL)
                        .setProperty("johnzon.cdi.activated", false))) {
            return new RecordConverters()
                    .toType(new RecordConverters.MappingMetaRegistry(), this, JsonObject.class,
                            () -> Json.createBuilderFactory(emptyMap()), JsonProvider::provider, () -> jsonb,
                            () -> new RecordBuilderFactoryImpl("tostring"))
                    .toString();
        } catch (final Exception e) {
            return super.toString();
        }
    }

    @Override
    public Builder withNewSchema(final Schema newSchema) {
        final BuilderImpl builder = new BuilderImpl(newSchema);
        newSchema.getAllEntries()
                .filter(e -> Objects.equals(schema.getEntry(e.getName()), e))
                .forEach(e -> builder.with(e, values.get(e.getName())));
        return builder;
    }

    // Entry creation can be optimized a bit but recent GC should not see it as a big deal
    public static class BuilderImpl implements Builder {

        private final Map<String, Object> values = new HashMap<>(8);

        private final OrderedMap<Schema.Entry> entries;

        private final Schema providedSchema;

        private OrderState orderState;

        public BuilderImpl() {
            this(null);
        }

        public BuilderImpl(final Schema providedSchema) {
            this.providedSchema = providedSchema;
            if (this.providedSchema == null) {
                this.entries = new OrderedMap<>(Schema.Entry::getName, Collections.emptyList());
                this.orderState = new OrderState(Collections.emptyList());
            } else {
                this.entries = null;
            }
        }

        private void initOrderState() {
            if (orderState == null) {
                if (this.providedSchema == null) {
                    this.orderState = new OrderState(Collections.emptyList());
                } else {
                    final List<Entry> fields = this.providedSchema.naturalOrder()
                            .getFieldsOrder()
                            .map(this.providedSchema::getEntry)
                            .collect(Collectors.toList());
                    this.orderState = new OrderState(fields);
                }
            }
        }

        private BuilderImpl(final List<Schema.Entry> entries, final Map<String, Object> values) {
            this.providedSchema = null;
            this.entries = new OrderedMap<>(Schema.Entry::getName, entries);
            this.values.putAll(values);
            this.orderState = null;
        }

        @Override
        public Object getValue(final String name) {
            return this.values.get(Schema.sanitizeConnectionName(name));
        }

        @Override
        public Builder with(final Entry entry, final Object value) {
            validateTypeAgainstProvidedSchema(entry.getName(), entry.getType(), value);
            if (!entry.getType().isCompatible(value)) {
                throw new IllegalArgumentException(String
                        .format("Entry '%s' of type %s is not compatible with value of type '%s'", entry.getName(),
                                entry.getType(), value.getClass().getName()));
            }

            if (entry.getType() == Schema.Type.DATETIME) {
                if (value == null) {
                    withDateTime(entry, (ZonedDateTime) value);
                } else if (value instanceof Long) {
                    withTimestamp(entry, (Long) value);
                } else if (value instanceof Date) {
                    withDateTime(entry, (Date) value);
                } else if (value instanceof ZonedDateTime) {
                    withDateTime(entry, (ZonedDateTime) value);
                } else if (value instanceof Instant) {
                    withInstant(entry, (Instant) value);
                } else if (value instanceof Temporal) {
                    withTimestamp(entry, ((Temporal) value).get(ChronoField.INSTANT_SECONDS) * 1000L);
                }
                return this;
            } else {
                return append(entry, value);
            }
        }

        @Override
        public Entry getEntry(final String name) {
            final String entryName = Schema.sanitizeConnectionName(name);
            if (this.providedSchema != null) {
                return this.providedSchema.getEntry(entryName);
            } else {
                return this.entries.getValue(entryName);
            }
        }

        @Override
        public List<Entry> getCurrentEntries() {
            if (this.providedSchema != null) {
                return Collections.unmodifiableList(this.providedSchema.getAllEntries().collect(Collectors.toList()));
            }
            return this.entries.streams().collect(Collectors.toList());
        }

        @Override
        public Builder removeEntry(final Schema.Entry schemaEntry) {
            if (this.providedSchema == null) {
                this.entries.removeValue(schemaEntry);
                this.values.remove(schemaEntry.getName());
                return this;
            }

            final BuilderImpl builder =
                    new BuilderImpl(this.providedSchema.getAllEntries().collect(Collectors.toList()), this.values);
            return builder.removeEntry(schemaEntry);
        }

        @Override
        public Builder updateEntryByName(final String name, final Schema.Entry schemaEntry) {
            final String entryName = Schema.sanitizeConnectionName(name);
            if (this.providedSchema == null) {
                if (this.entries.getValue(entryName) == null) {
                    throw new IllegalArgumentException(
                            "No entry '" + schemaEntry.getName() + "' expected in entries");
                }

                final Object value = this.values.get(entryName);
                if (!schemaEntry.getType().isCompatible(value)) {
                    throw new IllegalArgumentException(String
                            .format("Entry '%s' of type %s is not compatible with value of type '%s'",
                                    schemaEntry.getName(), schemaEntry.getType(), value.getClass()
                                            .getName()));
                }
                this.entries.replace(entryName, schemaEntry);

                if (this.orderState != null) {
                    this.orderState.orderedEntries.replace(entryName, schemaEntry);
                }

                this.values.remove(entryName);
                this.values.put(schemaEntry.getName(), value);
                return this;
            }

            final BuilderImpl builder =
                    new BuilderImpl(this.providedSchema.getAllEntries().collect(Collectors.toList()),
                            this.values);
            return builder.updateEntryByName(entryName, schemaEntry);
        }

        @Override
        public Builder updateEntryByName(final String name, final Entry schemaEntry,
                final Function<Object, Object> valueCastFunction) {
            final String entryName = Schema.sanitizeConnectionName(name);
            Object currentValue = this.values.get(entryName);
            this.values.put(entryName, valueCastFunction.apply(currentValue));
            return updateEntryByName(entryName, schemaEntry);
        }

        @Override
        public Builder before(final String entryName) {
            initOrderState();
            orderState.before(entryName);
            return this;
        }

        @Override
        public Builder after(final String entryName) {
            initOrderState();
            orderState.after(entryName);
            return this;
        }

        private Schema.Entry findExistingEntry(final String name) {
            final Schema.Entry entry;
            final String entryName = Schema.sanitizeConnectionName(name);
            if (this.providedSchema != null) {
                entry = this.providedSchema.getEntry(entryName);
            } else {
                entry = this.entries.getValue(entryName);
            }
            if (entry == null) {
                throw new IllegalArgumentException(
                        "No entry '" + name + "' expected in provided schema");
            }
            return entry;
        }

        private Schema.Entry findOrBuildEntry(final String name, final Schema.Type type, final boolean nullable) {
            if (this.providedSchema == null) {
                return new SchemaImpl.EntryImpl.BuilderImpl().withName(name)
                        .withType(type)
                        .withNullable(nullable)
                        .build();
            }
            return this.findExistingEntry(name);
        }

        private Schema.Entry validateTypeAgainstProvidedSchema(final String name, final Schema.Type type,
                final Object value) {
            if (this.providedSchema == null) {
                return null;
            }

            final Schema.Entry entry = this.findExistingEntry(name);
            if (entry.getType() != type) {
                throw new IllegalArgumentException(
                        "Entry '" + name + "' expected to be a " + entry.getType() + ", got a " + type);
            }
            if (value == null && !entry.isNullable()) {
                throw new IllegalArgumentException("Entry '" + name + "' is not nullable");
            }
            return entry;
        }

        public Record build() {
            final Schema currentSchema;
            if (this.providedSchema != null) {
                final String missing = this.providedSchema
                        .getAllEntries()
                        .filter(it -> !it.isNullable() && !values.containsKey(it.getName()))
                        .map(Schema.Entry::getName)
                        .collect(joining(", "));
                if (!missing.isEmpty()) {
                    throw new IllegalArgumentException("Missing entries: " + missing);
                }
                if (orderState != null && orderState.isOverride()) {
                    currentSchema = this.providedSchema.toBuilder().build(this.orderState.buildComparator());
                } else {
                    currentSchema = this.providedSchema;
                }
            } else {
                final Schema.Builder builder = new SchemaImpl.BuilderImpl().withType(RECORD);
                this.entries.forEachValue(builder::withEntry);
                initOrderState();
                currentSchema = builder.build(orderState.buildComparator());
            }
            return new RecordImpl(unmodifiableMap(values), currentSchema);
        }

        // here the game is to add an entry method for each kind of type + its companion with Entry provider

        public Builder withString(final String name, final String value) {
            final Schema.Entry entry = this.findOrBuildEntry(name, STRING, true);
            return withString(entry, value);
        }

        public Builder withString(final Schema.Entry entry, final String value) {
            assertType(entry.getType(), STRING);
            validateTypeAgainstProvidedSchema(entry.getName(), STRING, value);
            return append(entry, value);
        }

        public Builder withBytes(final String name, final byte[] value) {
            final Schema.Entry entry = this.findOrBuildEntry(name, BYTES, true);
            return withBytes(entry, value);
        }

        public Builder withBytes(final Schema.Entry entry, final byte[] value) {
            assertType(entry.getType(), BYTES);
            validateTypeAgainstProvidedSchema(entry.getName(), BYTES, value);
            return append(entry, value);
        }

        public Builder withDateTime(final String name, final Date value) {
            final Schema.Entry entry = this.findOrBuildEntry(name, DATETIME, true);
            return withDateTime(entry, value);
        }

        public Builder withDateTime(final Schema.Entry entry, final Date value) {
            validateTypeAgainstProvidedSchema(entry.getName(), DATETIME, value);
            return append(entry, value == null ? null : value.getTime());
        }

        public Builder withDateTime(final String name, final ZonedDateTime value) {
            final Schema.Entry entry = this.findOrBuildEntry(name, DATETIME, true);
            return withDateTime(entry, value);
        }

        public Builder withDateTime(final Schema.Entry entry, final ZonedDateTime value) {
            validateTypeAgainstProvidedSchema(entry.getName(), DATETIME, value);
            return append(entry, value == null ? null : value.toInstant().toEpochMilli());
        }

        @Override
        public Builder withDecimal(final String name, final BigDecimal value) {
            final Schema.Entry entry = this.findOrBuildEntry(name, DECIMAL, true);
            return withDecimal(entry, value);
        }

        @Override
        public Builder withDecimal(final Entry entry, final BigDecimal value) {
            assertType(entry.getType(), DECIMAL);
            validateTypeAgainstProvidedSchema(entry.getName(), DECIMAL, value);
            return append(entry, value);
        }

        public Builder withTimestamp(final String name, final long value) {
            final Schema.Entry entry = this.findOrBuildEntry(name, DATETIME, false);
            return withTimestamp(entry, value);
        }

        public Builder withTimestamp(final Schema.Entry entry, final long value) {
            assertType(entry.getType(), DATETIME);
            validateTypeAgainstProvidedSchema(entry.getName(), DATETIME, value);
            return append(entry, value);
        }

        public Builder withInstant(final String name, final Instant value) {
            final Schema.Entry entry = this.findOrBuildEntry(name, DATETIME, false);
            return withInstant(entry, value);
        }

        public Builder withInstant(final Schema.Entry entry, final Instant value) {
            assertType(entry.getType(), DATETIME);
            validateTypeAgainstProvidedSchema(entry.getName(), DATETIME, value);
            return append(entry, value);
        }

        public Builder withInt(final String name, final int value) {
            final Schema.Entry entry = this.findOrBuildEntry(name, INT, false);
            return withInt(entry, value);
        }

        public Builder withInt(final Schema.Entry entry, final int value) {
            assertType(entry.getType(), INT);
            validateTypeAgainstProvidedSchema(entry.getName(), INT, value);
            return append(entry, value);
        }

        public Builder withLong(final String name, final long value) {
            final Schema.Entry entry = this.findOrBuildEntry(name, LONG, false);
            return withLong(entry, value);
        }

        public Builder withLong(final Schema.Entry entry, final long value) {
            assertType(entry.getType(), LONG);
            validateTypeAgainstProvidedSchema(entry.getName(), LONG, value);
            return append(entry, value);
        }

        public Builder withFloat(final String name, final float value) {
            final Schema.Entry entry = this.findOrBuildEntry(name, FLOAT, false);
            return withFloat(entry, value);
        }

        public Builder withFloat(final Schema.Entry entry, final float value) {
            assertType(entry.getType(), FLOAT);
            validateTypeAgainstProvidedSchema(entry.getName(), FLOAT, value);
            return append(entry, value);
        }

        public Builder withDouble(final String name, final double value) {
            final Schema.Entry entry = this.findOrBuildEntry(name, DOUBLE, false);
            return withDouble(entry, value);
        }

        public Builder withDouble(final Schema.Entry entry, final double value) {
            assertType(entry.getType(), DOUBLE);
            validateTypeAgainstProvidedSchema(entry.getName(), DOUBLE, value);
            return append(entry, value);
        }

        public Builder withBoolean(final String name, final boolean value) {
            final Schema.Entry entry = this.findOrBuildEntry(name, BOOLEAN, false);
            return withBoolean(entry, value);
        }

        public Builder withBoolean(final Schema.Entry entry, final boolean value) {
            assertType(entry.getType(), BOOLEAN);
            validateTypeAgainstProvidedSchema(entry.getName(), BOOLEAN, value);
            return append(entry, value);
        }

        public Builder withRecord(final Schema.Entry entry, final Record value) {
            assertType(entry.getType(), RECORD);
            if (entry.getElementSchema() == null) {
                throw new IllegalArgumentException("No schema for the nested record");
            }
            validateTypeAgainstProvidedSchema(entry.getName(), RECORD, value);
            return append(entry, value);
        }

        public Builder withRecord(final String name, final Record value) {
            if (value == null) {
                throw new IllegalArgumentException("No schema for the nested record due to null record value");
            }
            return withRecord(new SchemaImpl.EntryImpl.BuilderImpl()
                    .withName(name)
                    .withElementSchema(value.getSchema())
                    .withType(RECORD)
                    .withNullable(true)
                    .build(), value);
        }

        public <T> Builder withArray(final Schema.Entry entry, final Collection<T> values) {
            assertType(entry.getType(), ARRAY);
            if (entry.getElementSchema() == null) {
                throw new IllegalArgumentException("No schema for the collection items");
            }
            validateTypeAgainstProvidedSchema(entry.getName(), ARRAY, values);
            // todo: check item type?
            return append(entry, values);
        }

        private void assertType(final Schema.Type actual, final Schema.Type expected) {
            if (actual != expected) {
                throw new IllegalArgumentException("Expected entry type: " + expected + ", got: " + actual);
            }
        }

        private <T> Builder append(final Schema.Entry entry, final T value) {

            final Schema.Entry realEntry;
            if (this.entries != null) {
                realEntry = Optional
                        .ofNullable(Schema.avoidCollision(entry,
                                this.entries::getValue,
                                this.entries::replace))
                        .orElse(entry);
            } else {
                realEntry = entry;
            }
            if (value != null) {
                values.put(realEntry.getName(), value);
            } else if (!realEntry.isNullable()) {
                throw new IllegalArgumentException(realEntry.getName() + " is not nullable but got a null value");
            }

            if (this.entries != null) {
                this.entries.addValue(realEntry);
            }
            if (orderState == null) {
                if (this.providedSchema != null && this.providedSchema.getEntryMap().containsKey(realEntry.getName())) {
                    // no need orderState, delay init it for performance, this is 99% cases for
                    // RecordBuilderFactoryImpl.newRecordBuilder(schema) usage
                } else {
                    initOrderState();
                    orderState.update(realEntry);
                }
            } else {
                orderState.update(realEntry);
            }
            return this;
        }

        private enum Order {
            BEFORE,
            AFTER,
            LAST;
        }

        static class OrderState {

            private Order state = Order.LAST;

            private String target = "";

            @Getter()
            // flag if providedSchema's entriesOrder was altered
            private boolean override = false;

            private final OrderedMap<Schema.Entry> orderedEntries;

            public OrderState(final Iterable<Schema.Entry> orderedEntries) {
                this.orderedEntries = new OrderedMap<>(Schema.Entry::getName, orderedEntries);
            }

            public void before(final String entryName) {
                setState(Order.BEFORE, entryName);
            }

            public void after(final String entryName) {
                setState(Order.AFTER, entryName);
            }

            private void setState(final Order order, final String target) {
                state = order; //
                this.target = target;
                override = true;
            }

            private void resetState() {
                target = "";
                state = Order.LAST;
            }

            public void update(final Schema.Entry entry) {
                final Schema.Entry existingEntry = this.orderedEntries.getValue(entry.getName());
                if (state == Order.LAST) {
                    // if entry is already present, we keep its position otherwise put it all the end.
                    if (existingEntry == null) {
                        orderedEntries.addValue(entry);
                    }
                } else {
                    final Schema.Entry targetIndex = orderedEntries.getValue(target);
                    if (targetIndex == null) {
                        throw new IllegalArgumentException(String.format("'%s' not in schema.", target));
                    }
                    if (existingEntry == null) {
                        this.orderedEntries.addValue(entry);
                    }

                    if (state == Order.BEFORE) {
                        orderedEntries.moveBefore(target, entry);
                    } else {
                        orderedEntries.moveAfter(target, entry);
                    }
                }
                // reset default behavior
                resetState();
            }

            public Comparator<Entry> buildComparator() {
                final List<String> orderedFields =
                        this.orderedEntries.streams().map(Entry::getName).collect(Collectors.toList());
                return EntriesOrder.of(orderedFields);
            }
        }
    }

}
