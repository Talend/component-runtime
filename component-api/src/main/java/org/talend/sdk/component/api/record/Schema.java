/**
 * Copyright (C) 2006-2023 Talend Inc. - www.talend.com
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

import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.time.temporal.Temporal;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonValue;
import javax.json.bind.annotation.JsonbTransient;
import javax.json.stream.JsonParser;

import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

public interface Schema {

    /**
     * @return the type of this schema.
     */
    Type getType();

    /**
     * @return the nested element schema for arrays.
     */
    Schema getElementSchema();

    /**
     * @return the data entries for records (not contains meta data entries).
     */
    List<Entry> getEntries();

    /**
     * @return the metadata entries for records (not contains ordinary data entries).
     */
    List<Entry> getMetadata();

    /**
     * @return All entries, including data and metadata, of this schema.
     */
    Stream<Entry> getAllEntries();

    /**
     * Get a Builder from the current schema.
     *
     * @return a {@link Schema.Builder}
     */
    default Schema.Builder toBuilder() {
        throw new UnsupportedOperationException("#toBuilder is not implemented");
    }

    /**
     * Get all entries sorted by schema designed order.
     *
     * @return all entries ordered
     */
    default List<Entry> getEntriesOrdered() {
        return getEntriesOrdered(naturalOrder());
    }

    /**
     * Get all entries sorted using a custom comparator.
     *
     * @param comparator the comparator
     *
     * @return all entries ordered with provided comparator
     */
    @JsonbTransient
    default List<Entry> getEntriesOrdered(final Comparator<Entry> comparator) {
        return getAllEntries().sorted(comparator).collect(Collectors.toList());
    }

    /**
     * Get the EntriesOrder defined with Builder.
     *
     * @return the EntriesOrder
     */

    default EntriesOrder naturalOrder() {
        throw new UnsupportedOperationException("#naturalOrder is not implemented");
    }

    default Entry getEntry(final String name) {
        return getAllEntries() //
                .filter((Entry e) -> Objects.equals(e.getName(), name)) //
                .findFirst() //
                .orElse(null);
    }

    /**
     * @return the metadata props
     */
    Map<String, String> getProps();

    /**
     * @param property : property name.
     *
     * @return the requested metadata prop
     */
    String getProp(String property);

    /**
     * Get a property values from schema with its name.
     *
     * @param name : property's name.
     *
     * @return property's value.
     */
    default JsonValue getJsonProp(final String name) {
        final String prop = this.getProp(name);
        if (prop == null) {
            return null;
        }
        try (final StringReader reader = new StringReader(prop);
                final JsonParser parser = Json.createParser(reader)) {
            return parser.getValue();
        } catch (RuntimeException ex) {
            return Json.createValue(prop);
        }
    }

    enum Type {

        RECORD(new Class<?>[] { Record.class }),
        ARRAY(new Class<?>[] { Collection.class }),
        STRING(new Class<?>[] { String.class, Object.class }),
        BYTES(new Class<?>[] { byte[].class, Byte[].class }),
        INT(new Class<?>[] { Integer.class }),
        LONG(new Class<?>[] { Long.class }),
        FLOAT(new Class<?>[] { Float.class }),
        DOUBLE(new Class<?>[] { Double.class }),
        BOOLEAN(new Class<?>[] { Boolean.class }),
        DATETIME(new Class<?>[] { Long.class, Date.class, Temporal.class }),
        DECIMAL(new Class<?>[] { BigDecimal.class });

        /**
         * All compatibles Java classes
         */
        private final Class<?>[] classes;

        Type(final Class<?>[] classes) {
            this.classes = classes;
        }

        /**
         * Check if input can be affected to an entry of this type.
         *
         * @param input : object.
         *
         * @return true if input is null or ok.
         */
        public boolean isCompatible(final Object input) {
            if (input == null) {
                return true;
            }
            for (final Class<?> clazz : classes) {
                if (clazz.isInstance(input)) {
                    return true;
                }
            }
            return false;
        }
    }

    interface Entry {

        /**
         * @return The name of this entry.
         */
        String getName();

        /**
         * @return The raw name of this entry.
         */
        String getRawName();

        /**
         * @return the raw name of this entry if exists, else return name.
         */
        String getOriginalFieldName();

        /**
         * @return Type of the entry, this determine which other fields are populated.
         */
        Type getType();

        /**
         * @return Is this entry nullable or always valued.
         */
        boolean isNullable();

        /**
         * @return true if this entry is for metadata; false for ordinary data.
         */
        boolean isMetadata();

        /**
         * @param <T> the default value type.
         *
         * @return Default value for this entry.
         */
        <T> T getDefaultValue();

        /**
         * @return For type == record, the element type.
         */
        Schema getElementSchema();

        /**
         * @return Allows to associate to this field a comment - for doc purposes, no use in the runtime.
         */
        String getComment();

        /**
         * @return the metadata props
         */
        Map<String, String> getProps();

        /**
         * @param property : property name.
         *
         * @return the requested metadata prop
         */
        String getProp(String property);

        /**
         * Get a property values from entry with its name.
         *
         * @param name : property's name.
         *
         * @return property's value.
         */
        default JsonValue getJsonProp(final String name) {
            final String prop = this.getProp(name);
            if (prop == null) {
                return null;
            }
            try (final StringReader reader = new StringReader(prop);
                    final JsonParser parser = Json.createParser(reader)) {
                return parser.getValue();
            } catch (RuntimeException ex) {
                return Json.createValue(prop);
            }
        }

        /**
         * @return an {@link Entry.Builder} from this entry.
         */
        default Entry.Builder toBuilder() {
            throw new UnsupportedOperationException("#toBuilder is not implemented");
        }

        /**
         * Plain builder matching {@link Entry} structure.
         */
        interface Builder {

            Builder withName(String name);

            Builder withRawName(String rawName);

            Builder withType(Type type);

            Builder withNullable(boolean nullable);

            Builder withMetadata(boolean metadata);

            <T> Builder withDefaultValue(T value);

            Builder withElementSchema(Schema schema);

            Builder withComment(String comment);

            Builder withProps(Map<String, String> props);

            Builder withProp(String key, String value);

            Entry build();
        }
    }

    /**
     * Allows to build a {@link Schema}.
     */
    interface Builder {

        /**
         * @param type schema type.
         *
         * @return this builder.
         */
        Builder withType(Type type);

        /**
         * @param entry element for either an array or record type.
         *
         * @return this builder.
         */
        Builder withEntry(Entry entry);

        /**
         * Insert the entry after the specified entry.
         *
         * @param after the entry name reference
         * @param entry the entry name
         *
         * @return this builder
         */
        default Builder withEntryAfter(String after, Entry entry) {
            throw new UnsupportedOperationException("#withEntryAfter is not implemented");
        }

        /**
         * Insert the entry before the specified entry.
         *
         * @param before the entry name reference
         * @param entry the entry name
         *
         * @return this builder
         */
        default Builder withEntryBefore(String before, Entry entry) {
            throw new UnsupportedOperationException("#withEntryBefore is not implemented");
        }

        /**
         * Remove entry from builder.
         *
         * @param name the entry name
         *
         * @return this builder
         */
        default Builder remove(String name) {
            throw new UnsupportedOperationException("#remove is not implemented");
        }

        /**
         * Remove entry from builder.
         *
         * @param entry the entry
         *
         * @return this builder
         */
        default Builder remove(Entry entry) {
            throw new UnsupportedOperationException("#remove is not implemented");
        }

        /**
         * Move an entry after another one.
         *
         * @param after the entry name reference
         * @param name the entry name
         */
        default Builder moveAfter(final String after, final String name) {
            throw new UnsupportedOperationException("#moveAfter is not implemented");
        }

        /**
         * Move an entry before another one.
         *
         * @param before the entry name reference
         * @param name the entry name
         */
        default Builder moveBefore(final String before, final String name) {
            throw new UnsupportedOperationException("#moveBefore is not implemented");
        }

        /**
         * Swap two entries.
         *
         * @param name the entry name
         * @param with the other entry name
         */
        default Builder swap(final String name, final String with) {
            throw new UnsupportedOperationException("#swap is not implemented");
        }

        /**
         * @param schema nested element schema.
         *
         * @return this builder.
         */
        Builder withElementSchema(Schema schema);

        /**
         * @param props schema properties
         *
         * @return this builder
         */
        Builder withProps(Map<String, String> props);

        /**
         * @param key the prop key name
         * @param value the prop value
         *
         * @return this builder
         */
        Builder withProp(String key, String value);

        /**
         * @return the described schema.
         */
        Schema build();

        /**
         * Same as {@link Builder#build()} but entries order is specified by {@code order}. This takes precedence on any
         * previous defined order with builder and may void it.
         *
         * @param order the wanted order for entries.
         * @return the described schema.
         */
        default Schema build(Comparator<Entry> order) {
            throw new UnsupportedOperationException("#build(EntriesOrder) is not implemented");
        }
    }

    /**
     * Sanitize name to be avro compatible.
     *
     * @param name : original name.
     *
     * @return avro compatible name.
     */
    static String sanitizeConnectionName(final String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }

        char current = name.charAt(0);
        final CharsetEncoder ascii = Charset.forName(StandardCharsets.US_ASCII.name()).newEncoder();
        final boolean skipFirstChar = ((!ascii.canEncode(current)) || (!Character.isLetter(current) && current != '_'))
                && name.length() > 1 && (!Character.isDigit(name.charAt(1)));

        final StringBuilder sanitizedBuilder = new StringBuilder();

        if (!skipFirstChar) {
            if (((!Character.isLetter(current)) && current != '_') || (!ascii.canEncode(current))) {
                sanitizedBuilder.append('_');
            } else {
                sanitizedBuilder.append(current);
            }
        }
        for (int i = 1; i < name.length(); i++) {
            current = name.charAt(i);
            if (!ascii.canEncode(current)) {
                if (Character.isLowerCase(current) || Character.isUpperCase(current)) {
                    sanitizedBuilder.append('_');
                } else {
                    final byte[] encoded =
                            Base64.getEncoder().encode(name.substring(i, i + 1).getBytes(StandardCharsets.UTF_8));
                    final String enc = new String(encoded);
                    if (sanitizedBuilder.length() == 0 && Character.isDigit(enc.charAt(0))) {
                        sanitizedBuilder.append('_');
                    }
                    for (int iter = 0; iter < enc.length(); iter++) {
                        if (Character.isLetterOrDigit(enc.charAt(iter))) {
                            sanitizedBuilder.append(enc.charAt(iter));
                        } else {
                            sanitizedBuilder.append('_');
                        }
                    }
                }
            } else if (Character.isLetterOrDigit(current)) {
                sanitizedBuilder.append(current);
            } else {
                sanitizedBuilder.append('_');
            }

        }
        return sanitizedBuilder.toString();
    }

    @RequiredArgsConstructor
    @ToString
    @EqualsAndHashCode
    class EntriesOrder implements Comparator<Entry> {

        private final OrderedMap<String> fieldsOrder;

        // Keep comparator while no change occurs in fieldsOrder.
        private Comparator<Entry> currentComparator = null;

        /**
         * Build an EntriesOrder according fields.
         *
         * @param fields the fields ordering. Each field should be {@code ,} separated.
         *
         * @return the order EntriesOrder
         */
        public static EntriesOrder of(final String fields) {
            return new EntriesOrder(fields);
        }

        /**
         * Build an EntriesOrder according fields.
         *
         * @param fields the fields ordering.
         *
         * @return the order EntriesOrder
         */
        public static EntriesOrder of(final Iterable<String> fields) {
            final OrderedMap<String> orders = new OrderedMap<>(Function.identity(), fields);
            return new EntriesOrder(orders);
        }

        public EntriesOrder(final String fields) {
            if (fields == null || fields.isEmpty()) {
                fieldsOrder = new OrderedMap<>(Function.identity());
            } else {
                final List<String> fieldList = Arrays.stream(fields.split(",")).collect(Collectors.toList());
                fieldsOrder = new OrderedMap<>(Function.identity(), fieldList);
            }
        }

        public EntriesOrder(final Iterable<String> fields) {
            this(new OrderedMap<>(Function.identity(), fields));
        }

        public Stream<String> getFieldsOrder() {
            return this.fieldsOrder.streams();
        }

        /**
         * Move a field after another one.
         *
         * @param after the field name reference
         * @param name the field name
         *
         * @return this EntriesOrder
         */
        public EntriesOrder moveAfter(final String after, final String name) {
            this.currentComparator = null;
            this.fieldsOrder.moveAfter(after, name);
            return this;
        }

        /**
         * Move a field before another one.
         *
         * @param before the field name reference
         * @param name the field name
         *
         * @return this EntriesOrder
         */
        public EntriesOrder moveBefore(final String before, final String name) {
            this.currentComparator = null;
            this.fieldsOrder.moveBefore(before, name);
            return this;
        }

        /**
         * Swap two fields.
         *
         * @param name the field name
         * @param with the other field
         *
         * @return this EntriesOrder
         */
        public EntriesOrder swap(final String name, final String with) {
            this.currentComparator = null;
            this.fieldsOrder.swap(name, with);
            return this;
        }

        public String toFields() {
            return this.fieldsOrder.streams().collect(Collectors.joining(","));
        }

        public Comparator<Entry> getComparator() {
            if (this.currentComparator == null) {
                final Map<String, Integer> entryPositions = new HashMap<>();
                final AtomicInteger index = new AtomicInteger(1);
                this.fieldsOrder.streams()
                        .forEach(
                                (final String name) -> entryPositions.put(name, index.getAndIncrement()));
                this.currentComparator = new EntryComparator(entryPositions);
            }
            return this.currentComparator;
        }

        @Override
        public int compare(final Entry e1, final Entry e2) {
            return this.getComparator().compare(e1, e2);
        }

        @RequiredArgsConstructor
        static class EntryComparator implements Comparator<Entry> {

            private final Map<String, Integer> entryPositions;

            @Override
            public int compare(final Entry e1, final Entry e2) {
                final int index1 = this.entryPositions.getOrDefault(e1.getName(), Integer.MAX_VALUE);
                final int index2 = this.entryPositions.getOrDefault(e2.getName(), Integer.MAX_VALUE);
                if (index1 >= 0 && index2 >= 0) {
                    return index1 - index2;
                }
                if (index1 >= 0) {
                    return -1;
                }
                if (index2 >= 0) {
                    return 1;
                }
                return 0;
            }
        }
    }

    // use new avoid collision with entry getter.
    @Deprecated
    static Schema.Entry avoidCollision(final Schema.Entry newEntry,
            final Supplier<Stream<Schema.Entry>> allEntriesSupplier,
            final BiConsumer<String, Entry> replaceFunction) {
        final Function<String, Entry> entryGetter = (String name) -> allEntriesSupplier //
                .get() //
                .filter((final Entry field) -> field.getName().equals(name))
                .findFirst()
                .orElse(null);
        return avoidCollision(newEntry, entryGetter, replaceFunction);
    }

    static Schema.Entry avoidCollision(final Schema.Entry newEntry,
            final Function<String, Entry> entryGetter,
            final BiConsumer<String, Entry> replaceFunction) {
        final Optional<Entry> collisionedEntry = Optional.ofNullable(entryGetter //
                .apply(newEntry.getName())) //
                .filter((final Entry field) -> !Objects.equals(field, newEntry));
        if (!collisionedEntry.isPresent()) {
            // No collision, return new entry.
            return newEntry;
        }
        final Entry matchedEntry = collisionedEntry.get();
        final boolean matchedToChange = matchedEntry.getRawName() != null && !(matchedEntry.getRawName().isEmpty());
        if (matchedToChange) {
            // the rename has to be applied on entry already inside schema, so replace.
            replaceFunction.accept(matchedEntry.getName(), newEntry);
        } else if (newEntry.getRawName() == null || newEntry.getRawName().isEmpty()) {
            // try to add exactly same raw, skip the add here.
            return null;
        }
        final Entry fieldToChange = matchedToChange ? matchedEntry : newEntry;
        int indexForAnticollision = 1;
        final String baseName = Schema.sanitizeConnectionName(fieldToChange.getRawName()); // recalc primiti name.

        String newName = baseName + "_" + indexForAnticollision;
        while (entryGetter.apply(newName) != null) {
            indexForAnticollision++;
            newName = baseName + "_" + indexForAnticollision;
        }
        final Entry newFieldToAdd = fieldToChange.toBuilder().withName(newName).build();

        return newFieldToAdd; // matchedToChange ? newFieldToAdd : newEntry;
    }
}
