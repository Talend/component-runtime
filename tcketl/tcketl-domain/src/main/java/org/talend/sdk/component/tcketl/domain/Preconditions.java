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
package org.talend.sdk.component.tcketl.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Argument checks and defensive copies shared by the immutable model types. */
public final class Preconditions {

    private Preconditions() {
        // static helpers only
    }

    public static <T> T requireNonNull(final T value, final String name) {
        if (value == null) {
            throw new NullPointerException(name + " must not be null");
        }
        return value;
    }

    public static String requireNonBlank(final String value, final String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }

    public static String nullToEmpty(final String value) {
        return value == null ? "" : value;
    }

    public static String orDefault(final String value, final String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    public static int requireNonNegative(final int value, final String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " must not be negative: " + value);
        }
        return value;
    }

    /** Unmodifiable copy, element order preserved, nulls rejected. */
    public static <T> List<T> immutableCopy(final Collection<? extends T> values, final String name) {
        requireNonNull(values, name);
        final List<T> copy = new ArrayList<>(values.size());
        for (final T value : values) {
            copy.add(requireNonNull(value, name + " element"));
        }
        return Collections.unmodifiableList(copy);
    }

    /** Unmodifiable copy without duplicates, first occurrence wins, blank entries rejected. */
    public static List<String> distinctNonBlank(final Collection<String> values, final String name) {
        requireNonNull(values, name);
        final LinkedHashSet<String> distinct = new LinkedHashSet<>();
        for (final String value : values) {
            distinct.add(requireNonBlank(value, name + " element"));
        }
        return Collections.unmodifiableList(new ArrayList<>(distinct));
    }

    /** Unmodifiable copy iterating in the source order, for maps whose order is meaningful. */
    public static Map<String, String> orderedCopy(final Map<String, String> values, final String name) {
        requireNonNull(values, name);
        final LinkedHashMap<String, String> copy = new LinkedHashMap<>();
        for (final Map.Entry<String, String> entry : values.entrySet()) {
            copy
                    .put(requireNonNull(entry.getKey(), name + " key"),
                            requireNonNull(entry.getValue(), name + " value of " + entry.getKey()));
        }
        return Collections.unmodifiableMap(copy);
    }

    /** Unmodifiable copy iterating in key order, which keeps persisted files deterministic. */
    public static Map<String, String> sortedCopy(final Map<String, String> values, final String name) {
        requireNonNull(values, name);
        final TreeMap<String, String> copy = new TreeMap<>();
        for (final Map.Entry<String, String> entry : values.entrySet()) {
            copy
                    .put(requireNonNull(entry.getKey(), name + " key"),
                            requireNonNull(entry.getValue(), name + " value of " + entry.getKey()));
        }
        return Collections.unmodifiableMap(copy);
    }
}
