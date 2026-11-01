/**
 * Copyright (C) 2006-2026 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.tcketl.domain.form;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.IntStream;

import org.talend.sdk.component.tcketl.domain.Preconditions;

/**
 * The flat {@code key=value} map the runtime reads, with the few structural operations a form needs: lists are stored
 * as {@code list[0]}, {@code list[0].field}, {@code list[1]}... and a list ends at the first missing index.
 *
 * <p>
 * This type knows nothing about property definitions, so it never loses a key it does not understand. It is immutable:
 * every operation returns a new instance.
 */
public final class ConfigurationValues {

    public static final ConfigurationValues EMPTY = new ConfigurationValues(new TreeMap<>());

    private static final int MAX_INDEX_DIGITS = 9;

    private final TreeMap<String, String> values;

    private final Map<String, String> view;

    private ConfigurationValues(final TreeMap<String, String> values) {
        this.values = values;
        this.view = Collections.unmodifiableMap(values);
    }

    public static ConfigurationValues of(final Map<String, String> source) {
        final Map<String, String> sorted = Preconditions.sortedCopy(source, "values");
        return sorted.isEmpty() ? EMPTY : new ConfigurationValues(new TreeMap<>(sorted));
    }

    /** The entries in key order. */
    public Map<String, String> asMap() {
        return view;
    }

    public int size() {
        return values.size();
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    public boolean contains(final String key) {
        return values.containsKey(key);
    }

    /** The value of a key, or an empty string when there is none. */
    public String get(final String key) {
        return values.getOrDefault(key, "");
    }

    public ConfigurationValues with(final String key, final String value) {
        Preconditions.requireNonNull(key, "key");
        Preconditions.requireNonNull(value, "value");
        if (value.equals(values.get(key))) {
            return this;
        }
        final TreeMap<String, String> next = new TreeMap<>(values);
        next.put(key, value);
        return new ConfigurationValues(next);
    }

    public ConfigurationValues withAll(final Map<String, String> entries) {
        if (entries.isEmpty()) {
            return this;
        }
        final TreeMap<String, String> next = new TreeMap<>(values);
        next.putAll(Preconditions.sortedCopy(entries, "entries"));
        return new ConfigurationValues(next);
    }

    /** Adds the entries whose key is not already present. */
    public ConfigurationValues withMissing(final Map<String, String> entries) {
        final TreeMap<String, String> next = new TreeMap<>(values);
        Preconditions.sortedCopy(entries, "entries").forEach(next::putIfAbsent);
        return next.size() == values.size() ? this : new ConfigurationValues(next);
    }

    public ConfigurationValues without(final String key) {
        if (!values.containsKey(key)) {
            return this;
        }
        final TreeMap<String, String> next = new TreeMap<>(values);
        next.remove(key);
        return new ConfigurationValues(next);
    }

    /** True when at least one key looks like an element of the list {@code prefix}, or its length bound. */
    public boolean hasEntries(final String prefix) {
        return !family(prefix, '[').isEmpty();
    }

    /**
     * The number of elements of a list: the first missing index ends it. An element exists when its own key is
     * present, even with an empty value, or when a key nests below it. Several prefixes are for a map, whose entry
     * exists as soon as its key or its value does.
     */
    public int count(final String... prefixes) {
        final TreeSet<Integer> present = new TreeSet<>();
        for (final String prefix : prefixes) {
            for (final String key : family(prefix, '[').keySet()) {
                final int index = elementIndex(key, prefix);
                if (index >= 0) {
                    present.add(index);
                }
            }
        }
        int count = 0;
        while (present.contains(count)) {
            count++;
        }
        return count;
    }

    /**
     * Deletes one element of a list and renumbers the following ones. An index outside the list changes nothing. The
     * length bound {@code list[length]} no longer describes the list and is dropped.
     */
    public ConfigurationValues removeElement(final String prefix, final int index) {
        if (index < 0 || index >= count(prefix)) {
            return this;
        }
        final TreeMap<String, String> next = new TreeMap<>(values);
        next.remove(prefix + "[length]");
        final Map<String, String> renumbered = new HashMap<>();
        for (final Map.Entry<String, String> entry : family(prefix, '[').entrySet()) {
            final int current = elementIndex(entry.getKey(), prefix);
            if (current >= index) {
                next.remove(entry.getKey());
                if (current > index) {
                    renumbered.put(withIndex(entry.getKey(), prefix, current - 1), entry.getValue());
                }
            }
        }
        next.putAll(renumbered);
        return new ConfigurationValues(next);
    }

    /** Moves one element of a list to another position, the elements in between shift by one. */
    public ConfigurationValues moveElement(final String prefix, final int from, final int to) {
        final int size = count(prefix);
        if (from == to || from < 0 || to < 0 || from >= size || to >= size) {
            return this;
        }
        final List<Integer> order = new ArrayList<>(IntStream.range(0, size).boxed().toList());
        order.add(to, order.remove(from));
        final int[] newIndex = new int[size];
        for (int position = 0; position < size; position++) {
            newIndex[order.get(position)] = position;
        }
        final TreeMap<String, String> next = new TreeMap<>(values);
        next.remove(prefix + "[length]");
        final Map<String, String> moved = new HashMap<>();
        for (final Map.Entry<String, String> entry : family(prefix, '[').entrySet()) {
            final int current = elementIndex(entry.getKey(), prefix);
            if (current >= 0 && current < size) {
                next.remove(entry.getKey());
                moved.put(withIndex(entry.getKey(), prefix, newIndex[current]), entry.getValue());
            }
        }
        next.putAll(moved);
        return new ConfigurationValues(next);
    }

    /** Drops a key, everything nested below it ({@code key.x}) and its elements ({@code key[0]}). */
    public ConfigurationValues withoutSubtree(final String prefix) {
        final Map<String, String> subtree = subtree(prefix);
        if (subtree.isEmpty()) {
            return this;
        }
        final TreeMap<String, String> next = new TreeMap<>(values);
        next.keySet().removeAll(subtree.keySet());
        return new ConfigurationValues(next);
    }

    /** A key and everything nested below it, keys unchanged. */
    public Map<String, String> subtree(final String prefix) {
        final TreeMap<String, String> subtree = new TreeMap<>();
        if (values.containsKey(prefix)) {
            subtree.put(prefix, values.get(prefix));
        }
        subtree.putAll(family(prefix, '.'));
        subtree.putAll(family(prefix, '['));
        return subtree;
    }

    /** Replaces everything at and below a key by the given entries, whose keys are absolute. */
    public ConfigurationValues replaceSubtree(final String prefix, final Map<String, String> replacement) {
        return withoutSubtree(prefix).withAll(replacement);
    }

    @Override
    public boolean equals(final Object other) {
        return other instanceof ConfigurationValues that && values.equals(that.values);
    }

    @Override
    public int hashCode() {
        return values.hashCode();
    }

    @Override
    public String toString() {
        return "ConfigurationValues" + values;
    }

    /** The entries whose key starts with {@code prefix} followed by the separator. */
    private SortedMap<String, String> family(final String prefix, final char separator) {
        return values.subMap(prefix + separator, prefix + (char) (separator + 1));
    }

    /** The position of the element a key belongs to in the list {@code prefix}, or -1. */
    private static int elementIndex(final String key, final String prefix) {
        final int start = prefix.length() + 1;
        int end = start;
        while (end < key.length() && key.charAt(end) >= '0' && key.charAt(end) <= '9') {
            end++;
        }
        final boolean closed = end > start && end - start <= MAX_INDEX_DIGITS && end < key.length()
                && key.charAt(end) == ']';
        final boolean nested = !closed || end + 1 == key.length() || key.charAt(end + 1) == '.'
                || key.charAt(end + 1) == '[';
        return closed && nested ? Integer.parseInt(key.substring(start, end)) : -1;
    }

    private static String withIndex(final String key, final String prefix, final int index) {
        return prefix + '[' + index + key.substring(key.indexOf(']', prefix.length() + 1));
    }
}
