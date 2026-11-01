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
import java.util.List;
import java.util.Optional;

import org.talend.sdk.component.tcketl.domain.Preconditions;
import org.talend.sdk.component.tcketl.domain.property.ActionBinding;

/**
 * What a form shows and where: tabs, each made of rows of cells. It is a description, not a view: a renderer turns it
 * into controls and the engine computes it once per connector.
 *
 * <p>
 * Every field and collection of the connector appears at most once in the whole layout, in the first tab that shows
 * it. Nodes that are hidden ({@code ui::hidden}) or that belong to the runtime ({@code $checkpoint}) never appear.
 *
 * @param tabs the tabs in display order; a single tab is meant to be shown without a tab pane.
 */
public record FormLayout(List<Tab> tabs) {

    public static final FormLayout EMPTY = new FormLayout(List.of());

    public FormLayout {
        tabs = Preconditions.immutableCopy(tabs, "tabs");
    }

    /** True when there is more than one tab, so a tab pane is needed. */
    public boolean isTabbed() {
        return tabs.size() > 1;
    }

    public Optional<Tab> tab(final String name) {
        return tabs.stream().filter(tab -> tab.name().equalsIgnoreCase(name)).findFirst();
    }

    public record Tab(String name, List<Row> rows) {

        public Tab {
            Preconditions.requireNonBlank(name, "name");
            rows = Preconditions.immutableCopy(rows, "rows");
        }

        /** The fields and collections of the tab in display order, groups flattened. */
        public List<PropertyNode> nodes() {
            final List<PropertyNode> nodes = new ArrayList<>();
            collect(rows, nodes);
            return nodes;
        }

        private static void collect(final List<Row> rows, final List<PropertyNode> nodes) {
            for (final Row row : rows) {
                for (final Cell cell : row.cells()) {
                    if (cell instanceof FieldCell field) {
                        nodes.add(field.node());
                    } else if (cell instanceof CollectionCell collection) {
                        nodes.add(collection.node());
                    } else if (cell instanceof GroupCell group) {
                        collect(group.rows(), nodes);
                    }
                }
            }
        }
    }

    /** Cells laid side by side, from left to right. */
    public record Row(List<Cell> cells) {

        public Row {
            cells = Preconditions.immutableCopy(cells, "cells");
            if (cells.isEmpty()) {
                throw new IllegalArgumentException("a row must have at least one cell");
            }
        }
    }

    public sealed interface Cell {
    }

    /** A single value: text, number, boolean or choice. */
    public record FieldCell(PropertyNode node) implements Cell {

        public FieldCell {
            Preconditions.requireNonNull(node, "node");
        }
    }

    /**
     * A list or a map. Its entries are added and removed by the user, and each one is shown as the {@code elementRows}
     * below; they are empty for a list of plain values, which needs no layout, and for a map they hold the single row
     * of a key and a value.
     */
    public record CollectionCell(PropertyNode node, List<Row> elementRows) implements Cell {

        public CollectionCell {
            Preconditions.requireNonNull(node, "node");
            elementRows = Preconditions.immutableCopy(elementRows, "elementRows");
        }
    }

    /** A nested object, shown as a titled group. */
    public record GroupCell(PropertyNode node, List<Row> rows) implements Cell {

        public GroupCell {
            Preconditions.requireNonNull(node, "node");
            rows = Preconditions.immutableCopy(rows, "rows");
        }
    }

    /**
     * A button calling a service of the server.
     *
     * @param owner the object whose group hosts the button.
     * @param kind what the button does.
     * @param target the property the action reads and writes: the {@code owner} itself, except for a schema discovery,
     * where it is the property that holds the schema.
     * @param binding the action to call. A schema discovery is offered for every output structure, with the action
     * the property names or else {@code default}: the host hides the button when the server has no such action.
     */
    public record ButtonCell(PropertyNode owner, ButtonKind kind, PropertyNode target, ActionBinding binding)
            implements Cell {

        public ButtonCell {
            Preconditions.requireNonNull(owner, "owner");
            Preconditions.requireNonNull(kind, "kind");
            Preconditions.requireNonNull(target, "target");
            Preconditions.requireNonNull(binding, "binding");
        }
    }

    public enum ButtonKind {
        HEALTHCHECK,
        UPDATE,
        GUESS_SCHEMA
    }
}
