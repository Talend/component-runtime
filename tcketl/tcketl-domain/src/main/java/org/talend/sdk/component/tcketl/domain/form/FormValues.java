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

import java.util.Map;
import java.util.Optional;

import org.talend.sdk.component.tcketl.domain.Preconditions;

/**
 * The values shown by a form: the stored configuration completed with the defaults of the property tree, addressed by
 * concrete path ({@code config.items[1].name}). It is immutable, every change returns a new instance, and it knows
 * how to add, remove and move the entries of lists and maps.
 *
 * <p>
 * Opening a form creates a {@code FormValues} but writes nothing back: only {@link #toConfiguration()} produces what
 * is stored, and the caller does it when the user edits something.
 */
public final class FormValues {

    private final PropertyTree tree;

    private final ConfigurationValues values;

    private FormValues(final PropertyTree tree, final ConfigurationValues values) {
        this.tree = tree;
        this.values = values;
    }

    /** The stored configuration, completed with the defaults it lacks. */
    public static FormValues of(final PropertyTree tree, final Map<String, String> stored) {
        Preconditions.requireNonNull(tree, "tree");
        return new FormValues(tree, Defaults.materialize(tree, ConfigurationValues.of(stored)));
    }

    public PropertyTree tree() {
        return tree;
    }

    /** The value at a concrete path, or an empty string when there is none. */
    public String get(final String path) {
        return values.get(path);
    }

    public boolean has(final String path) {
        return values.contains(path);
    }

    public FormValues set(final String path, final String value) {
        final ConfigurationValues next = values.with(path, value);
        return next == values ? this : new FormValues(tree, next);
    }

    /**
     * Replaces everything at and below {@code path} with {@code suffixes}: each key is what follows the path ({@code
     * .url}, {@code [0]}, or empty for the value itself).
     */
    public FormValues replaceSubtree(final String path, final Map<String, String> suffixes) {
        final Map<String, String> absolute = new java.util.TreeMap<>();
        suffixes.forEach((suffix, value) -> absolute.put(path + suffix, value));
        return new FormValues(tree, values.replaceSubtree(path, absolute));
    }

    /** The number of entries of the list or map at a concrete path, 0 for anything else. */
    public int count(final String path) {
        return tree.find(path).map(node -> count(node, path)).orElse(0);
    }

    /** Adds an entry, filled with the defaults of its properties, at the end of the list or map at {@code path}. */
    public FormValues append(final String path) {
        final Optional<PropertyNode> found = tree.find(path);
        if (found.isEmpty() || !found.get().kind().isCollection()) {
            return this;
        }
        final PropertyNode node = found.get();
        final int position = count(node, path);
        if (node.kind() == PropertyKind.MAP) {
            return new FormValues(tree, addEntry(node, path, position));
        }
        final PropertyNode element = node.element().orElseThrow();
        return new FormValues(tree, addElement(element, element.concretePath(path, position)));
    }

    /** Removes an entry of the list or map at {@code path}; the entries after it move up. */
    public FormValues remove(final String path, final int index) {
        final Optional<PropertyNode> found = tree.find(path);
        if (found.isEmpty() || !found.get().kind().isCollection()) {
            return this;
        }
        final ConfigurationValues next = found.get().kind() == PropertyKind.MAP
                ? values.removeElement(path + ".key", index).removeElement(path + ".value", index)
                : values.removeElement(path, index);
        return new FormValues(tree, next);
    }

    /** Moves an entry of the list or map at {@code path} to another position. */
    public FormValues move(final String path, final int from, final int to) {
        final Optional<PropertyNode> found = tree.find(path);
        if (found.isEmpty() || !found.get().kind().isCollection()) {
            return this;
        }
        final ConfigurationValues next = found.get().kind() == PropertyKind.MAP
                ? values.moveElement(path + ".key", from, to).moveElement(path + ".value", from, to)
                : values.moveElement(path, from, to);
        return new FormValues(tree, next);
    }

    /** Every value, defaults included, in key order. */
    public ConfigurationValues effective() {
        return values;
    }

    /** The flat map to store and to give the runtime, see {@link ConfigurationCodec}. */
    public Map<String, String> toConfiguration() {
        return ConfigurationCodec.encode(tree, values);
    }

    @Override
    public boolean equals(final Object other) {
        return other instanceof FormValues that && tree == that.tree && values.equals(that.values);
    }

    @Override
    public int hashCode() {
        return values.hashCode();
    }

    @Override
    public String toString() {
        return "FormValues" + values.asMap();
    }

    private int count(final PropertyNode node, final String path) {
        return node.kind().isCollection() ? ValueWalker.countEntries(values, node, path) : 0;
    }

    private ConfigurationValues addElement(final PropertyNode element, final String elementPath) {
        if (element.kind().isScalar()) {
            return values.with(elementPath, element.definition().defaultValue());
        }
        if (element.kind() != PropertyKind.OBJECT) {
            return values.with(elementPath, "");
        }
        final ConfigurationValues filled = Defaults.materialize(element, elementPath, values);
        if (!filled.subtree(elementPath).isEmpty()) {
            return filled;
        }
        return ConfigurationCodec.marker(element, elementPath)
                .map(entry -> filled.with(entry.getKey(), entry.getValue()))
                .orElse(filled);
    }

    private ConfigurationValues addEntry(final PropertyNode map, final String path, final int position) {
        final PropertyNode key = map.child("key[]").orElseThrow();
        final PropertyNode value = map.child("value[]").orElseThrow();
        final ConfigurationValues withKey =
                values.with(key.concretePath(path, position), key.definition().defaultValue());
        final String valuePath = value.concretePath(path, position);
        if (value.kind() == PropertyKind.OBJECT) {
            return Defaults.materialize(value, valuePath, withKey);
        }
        return value.kind().isScalar() ? withKey.with(valuePath, value.definition().defaultValue()) : withKey;
    }
}
