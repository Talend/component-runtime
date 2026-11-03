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
package org.talend.sdk.component.tcketl.app.palette;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.talend.sdk.component.tcketl.domain.connector.ConnectorSummary;

/**
 * The connectors of a server grouped by family. Families and the connectors of a family are sorted case
 * insensitively by display name. Immutable.
 */
public final class Palette {

    // declared before EMPTY, whose construction already sorts with it
    private static final Comparator<String> BY_TEXT = textOrder();

    public static final Palette EMPTY = new Palette(List.of());

    private final List<Entry> entries;

    private final List<PaletteFamily> families;

    private final int size;

    private Palette(final List<Entry> entries) {
        this.entries = entries;
        final Map<String, List<Entry>> byFamily = new LinkedHashMap<>();
        entries.forEach(entry -> byFamily.computeIfAbsent(entry.connector().familyId(), id -> new ArrayList<>())
                .add(entry));
        this.families = byFamily.entrySet().stream().map(family -> {
            final List<Entry> members = family.getValue();
            return new PaletteFamily(family.getKey(), members.get(0).connector().familyDisplayName(),
                    members.stream().map(Entry::connector).toList());
        }).sorted(Comparator.comparing(PaletteFamily::displayName, BY_TEXT).thenComparing(PaletteFamily::id)).toList();
        this.size = entries.size();
    }

    public static Palette of(final Collection<ConnectorSummary> connectors) {
        if (connectors.isEmpty()) {
            return EMPTY;
        }
        return new Palette(connectors.stream()
                .map(Entry::new)
                .sorted(Comparator.comparing((Entry entry) -> entry.connector().displayName(), BY_TEXT)
                        .thenComparing(entry -> entry.connector().id().value()))
                .toList());
    }

    public List<PaletteFamily> families() {
        return families;
    }

    /** Number of connectors, whatever their family. */
    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * The connectors that match every word of the query, a word matching when it is part of the display name, the
     * family name or one of the categories, whatever the case. A blank query matches everything.
     */
    public Palette filter(final String query) {
        final List<String> words = words(query);
        if (words.isEmpty() || isEmpty()) {
            return this;
        }
        final List<Entry> matching = entries.stream().filter(entry -> entry.matchesAll(words)).toList();
        return matching.size() == size ? this : new Palette(matching);
    }

    private static List<String> words(final String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        return List.of(query.strip().toLowerCase(Locale.ROOT).split("\\s+"));
    }

    private static Comparator<String> textOrder() {
        final Collator collator = Collator.getInstance(Locale.ROOT);
        return collator::compare;
    }

    private static final class Entry {

        private final ConnectorSummary connector;

        private final List<String> searchable;

        private Entry(final ConnectorSummary connector) {
            this.connector = connector;
            final List<String> fields = new ArrayList<>();
            fields.add(connector.displayName());
            fields.add(connector.familyDisplayName());
            fields.addAll(connector.categories());
            this.searchable = fields.stream().map(field -> field.toLowerCase(Locale.ROOT)).toList();
        }

        private ConnectorSummary connector() {
            return connector;
        }

        private boolean matchesAll(final List<String> words) {
            return words.stream().allMatch(word -> searchable.stream().anyMatch(field -> field.contains(word)));
        }
    }
}
