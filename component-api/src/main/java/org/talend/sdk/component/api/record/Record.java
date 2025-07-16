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
package org.talend.sdk.component.api.record;

import static java.util.Optional.ofNullable;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.function.Function;

import org.talend.sdk.component.api.record.Schema.Entry;

public interface Record {

    String RECORD_ERROR_SUPPORT = "talend.component.record.error.support";

    /**
     * @return the schema of this record.
     */
    Schema getSchema();

    /**
     * Create a Builder with values of the record present in {@link Schema}.
     * 
     * @param schema new schema
     * @return a {@link Record.Builder}
     */
    default Builder withNewSchema(Schema schema) {
        throw new UnsupportedOperationException("#withNewSchema is not implemented");
    }

    /**
     * Access a record field value.
     *
     * IMPORTANT: it is always better to use the typed accessors and the optional flavor when the entry is nullable.
     *
     * @param expectedType the expected type for the column.
     * @param name the name of the column.
     * @param <T> the type of expectedType.
     * @return the column value.
     */
    <T> T get(Class<T> expectedType, String name);

    default <T> T get(Class<T> expectedType, Schema.Entry entry) {
        if (entry == null) {
            return null;
        }
        return this.get(expectedType, entry.getName());
    }

    /**
     * See {@link Record#get(Class, String)}.
     * 
     * @param name entry name.
     * @return the value of the entry in this record.
     */
    default String getString(final String name) {
        return get(String.class, name);
    }

    /**
     * See {@link Record#get(Class, String)}.
     * 
     * @param name entry name.
     * @return the value of the entry in this record.
     */
    default int getInt(final String name) {
        return get(Integer.class, name);
    }

    /**
     * See {@link Record#get(Class, String)}.
     * 
     * @param name entry name.
     * @return the value of the entry in this record.
     */
    default long getLong(final String name) {
        return get(Long.class, name);
    }

    /**
     * See {@link Record#get(Class, String)}.
     * 
     * @param name entry name.
     * @return the value of the entry in this record.
     */
    default double getDouble(final String name) {
        return get(Double.class, name);
    }

    /**
     * See {@link Record#get(Class, String)}.
     * 
     * @param name entry name.
     * @return the value of the entry in this record.
     */
    default float getFloat(final String name) {
        return get(Float.class, name);
    }

    /**
     * See {@link Record#get(Class, String)}.
     * 
     * @param name entry name.
     * @return the value of the entry in this record.
     */
    default boolean getBoolean(final String name) {
        return get(Boolean.class, name);
    }

    /**
     * See {@link Record#get(Class, String)}.
     * 
     * @param name entry name.
     * @return the value of the entry in this record.
     */
    default byte[] getBytes(final String name) {
        return get(byte[].class, name);
    }

    /**
     * See {@link Record#get(Class, String)}.
     * 
     * @param name entry name.
     * @return the value of the entry in this record.
     */
    default Record getRecord(final String name) {
        return get(Record.class, name);
    }

    /**
     * See {@link Record#get(Class, String)}.
     * 
     * @param type type of the elements of the collection.
     * @param name entry name.
     * @param <T> type of the collection elements.
     * @return the value of the entry in this record.
     */
    default <T> Collection<T> getArray(final Class<T> type, final String name) {
        return get(Collection.class, name);
    }

    /**
     * See {@link Record#get(Class, String)}.
     * 
     * @param name entry name.
     * @return the value of the entry in this record.
     */
    default ZonedDateTime getDateTime(final String name) {
        return get(ZonedDateTime.class, name);
    }

    /**
     * See {@link Record#get(Class, String)}.
     *
     * @param name entry name.
     * @return the value of the entry in this record.
     */
    default Instant getInstant(final String name) {
        return get(Instant.class, name);
    }

    default BigDecimal getDecimal(final String name) {
        return get(BigDecimal.class, name);
    }

    /**
     * See {@link Record#get(Class, String)}.
     * 
     * @param type type of the elements of the collection.
     * @param name entry name.
     * @param <T> type of the collection elements.
     * @return the value of the entry in this record.
     */
    default <T> Optional<Collection<T>> getOptionalArray(final Class<T> type, final String name) {
        final Collection<T> value = get(Collection.class, name);
        return ofNullable(value);
    }

    /**
     * See {@link Record#get(Class, String)}.
     * 
     * @param name entry name.
     * @return the value of the entry in this record.
     */
    default Optional<ZonedDateTime> getOptionalDateTime(final String name) {
        return ofNullable(get(ZonedDateTime.class, name));
    }

    /**
     * See {@link Record#get(Class, String)}.
     *
     * @param name entry name.
     * @return the value of the entry in this record.
     */
    default Optional<Instant> getOptionalInstant(final String name) {
        return ofNullable(get(Instant.class, name));
    }

    /**
     * See {@link Record#get(Class, String)}.
     *
     * @param name entry name.
     * @return the value of the entry in this record.
     */
    default Optional<BigDecimal> getOptionalDecimal(final String name) {
        return ofNullable(get(BigDecimal.class, name));
    }

    /**
     * See {@link Record#get(Class, String)}.
     * 
     * @param name entry name.
     * @return the value of the entry in this record.
     */
    default Optional<String> getOptionalString(final String name) {
        return ofNullable(get(String.class, name));
    }

    /**
     * See {@link Record#get(Class, String)}.
     * 
     * @param name entry name.
     * @return the value of the entry in this record.
     */
    default OptionalInt getOptionalInt(final String name) {
        final Integer value = get(Integer.class, name);
        return value == null ? OptionalInt.empty() : OptionalInt.of(value);
    }

    /**
     * See {@link Record#get(Class, String)}.
     * 
     * @param name entry name.
     * @return the value of the entry in this record.
     */
    default OptionalLong getOptionalLong(final String name) {
        final Long value = get(Long.class, name);
        return value == null ? OptionalLong.empty() : OptionalLong.of(value);
    }

    /**
     * See {@link Record#get(Class, String)}.
     * 
     * @param name entry name.
     * @return the value of the entry in this record.
     */
    default OptionalDouble getOptionalDouble(final String name) {
        final Double value = get(Double.class, name);
        return value == null ? OptionalDouble.empty() : OptionalDouble.of(value);
    }

    /**
     * See {@link Record#get(Class, String)}.
     * 
     * @param name entry name.
     * @return the value of the entry in this record.
     */
    default OptionalDouble getOptionalFloat(final String name) {
        final Float value = get(Float.class, name);
        return value == null ? OptionalDouble.empty() : OptionalDouble.of(value);
    }

    /**
     * See {@link Record#get(Class, String)}.
     * 
     * @param name entry name.
     * @return the value of the entry in this record.
     */
    default Optional<Boolean> getOptionalBoolean(final String name) {
        return ofNullable(get(Boolean.class, name));
    }

    /**
     * See {@link Record#get(Class, String)}.
     * 
     * @param name entry name.
     * @return the value of the entry in this record.
     */
    default Optional<byte[]> getOptionalBytes(final String name) {
        return ofNullable(get(byte[].class, name));
    }

    /**
     * See {@link Record#get(Class, String)}.
     * 
     * @param name entry name.
     * @return the value of the entry in this record.
     */
    default Optional<Record> getOptionalRecord(final String name) {
        return ofNullable(get(Record.class, name));
    }

    default boolean isValid() {
        return !getSchema().getAllEntries()
                .anyMatch(entry -> !entry.isValid());
    }

    /**
     * Allows to create a record with a fluent API. This is the unique recommended way to create a record.
     */

    interface Builder {

        Record build();

        Object getValue(String name);

        List<Entry> getCurrentEntries();

        default Entry getEntry(final String name) {
            return this.getCurrentEntries()
                    .stream()
                    .filter((Entry e) -> name.equals(e.getName()))
                    .findFirst()
                    .orElse(null);
        }

        /**
         * Mark that next entry created {@code withXXXX()} will be before {@code entryName} in schema order.
         *
         * @see
         * <ul>
         * <li>{@link Schema#naturalOrder()}</li>
         * <li>{@link Schema#getEntriesOrdered()}</li>
         * <li>{@link Schema#getEntriesOrdered(Comparator)}</li>
         * </ul>
         *
         * @param entryName target entry name. This entry <b>must</b> exist!
         *
         * @return this Builder
         */
        default Builder before(String entryName) {
            throw new UnsupportedOperationException("#before is not implemented");
        }

        /**
         * Mark that next entry created {@code withXXXX()} will be after {@code entryName} in schema order.
         *
         * @see
         * <ul>
         * <li>{@link Schema#naturalOrder()}</li>
         * <li>{@link Schema#getEntriesOrdered()}</li>
         * <li>{@link Schema#getEntriesOrdered(Comparator)}</li>
         * </ul>
         *
         * @param entryName target entry name. This entry <b>must</b> exist!
         *
         * @return this Builder
         */
        default Builder after(String entryName) {
            throw new UnsupportedOperationException("#after");
        }

        Builder removeEntry(Schema.Entry schemaEntry);

        Builder updateEntryByName(String name, Schema.Entry schemaEntry);

        default Builder updateEntryByName(String name,
                Schema.Entry schemaEntry,
                Function<Object, Object> valueCastFunction) {
            throw new UnsupportedOperationException("#updateEntryByName");
        }

        Builder with(Schema.Entry entry, Object value);

        Builder withString(String name, String value);

        Builder withString(Schema.Entry entry, String value);

        Builder withBytes(String name, byte[] value);

        Builder withBytes(Schema.Entry entry, byte[] value);

        Builder withDateTime(String name, Date value);

        Builder withDateTime(Schema.Entry entry, Date value);

        Builder withDateTime(String name, ZonedDateTime value);

        Builder withDateTime(Schema.Entry entry, ZonedDateTime value);

        default Builder withDecimal(String name, BigDecimal value) {
            throw new UnsupportedOperationException("#withDecimal");
        }

        default Builder withDecimal(Schema.Entry entry, BigDecimal value) {
            throw new UnsupportedOperationException("#withDecimal");
        }

        Builder withTimestamp(String name, long value);

        Builder withTimestamp(Schema.Entry entry, long value);

        default Builder withInstant(String name, Instant value) {
            throw new UnsupportedOperationException("#withInstant");
        }

        default Builder withInstant(Schema.Entry entry, Instant value) {
            throw new UnsupportedOperationException("#withInstant");
        }

        Builder withInt(String name, int value);

        Builder withInt(Schema.Entry entry, int value);

        Builder withLong(String name, long value);

        Builder withLong(Schema.Entry entry, long value);

        Builder withFloat(String name, float value);

        Builder withFloat(Schema.Entry entry, float value);

        Builder withDouble(String name, double value);

        Builder withDouble(Schema.Entry entry, double value);

        Builder withBoolean(String name, boolean value);

        Builder withBoolean(Schema.Entry entry, boolean value);

        Builder withRecord(Schema.Entry entry, Record value);

        /**
         * @since 1.1.6
         *
         * @param name entry name.
         * @param value record value.
         * @return this builder.
         */
        Builder withRecord(String name, Record value);

        <T> Builder withArray(Schema.Entry entry, Collection<T> values);
    }
}
