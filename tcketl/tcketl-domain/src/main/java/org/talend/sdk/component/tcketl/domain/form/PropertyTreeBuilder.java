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
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.talend.sdk.component.tcketl.domain.property.PropertyDefinition;
import org.talend.sdk.component.tcketl.domain.property.PropertyType;

/** Builds a {@link PropertyTree}; the shape rules live here. */
final class PropertyTreeBuilder {

    private static final String ARRAY_SUFFIX = "[]";

    private static final Set<String> MAP_CHILDREN = Set.of("key[]", "value[]");

    private final Map<String, PropertyDefinition> definitions = new LinkedHashMap<>();

    private final Map<String, PropertyNode> nodes = PropertyTree.newIndex();

    private final List<PropertyNode> roots = PropertyTree.newRoots();

    private final List<String> warnings = new ArrayList<>();

    PropertyTreeBuilder(final Collection<PropertyDefinition> input) {
        for (final PropertyDefinition definition : input) {
            final String path = definition.path();
            if (!isWellFormed(path)) {
                warnings.add("Ignored a property with a malformed path: '" + path + "'");
            } else if (definitions.putIfAbsent(path, definition) != null) {
                warnings.add("Ignored a duplicate definition of '" + path + "'");
            }
        }
    }

    PropertyTree build() {
        new ArrayList<>(definitions.keySet()).forEach(this::node);
        nodes.values().forEach(node -> node.setKind(initialKind(node)));
        nodes.values()
                .stream()
                .filter(PropertyTreeBuilder::looksLikeMap)
                .forEach(node -> node.setKind(PropertyKind.MAP));
        new ArrayList<>(nodes.values()).stream()
                .filter(node -> node.definition().type() == PropertyType.ARRAY)
                .forEach(this::resolveArray);
        return new PropertyTree(roots, nodes, warnings);
    }

    private PropertyNode node(final String path) {
        final PropertyNode known = nodes.get(path);
        if (known != null) {
            return known;
        }
        final PropertyDefinition definition = definitions.get(path);
        final PropertyNode created = definition != null ? new PropertyNode(definition, false)
                : new PropertyNode(PropertyDefinition.builder(path, PropertyType.OBJECT).build(), true);
        nodes.put(path, created);
        attach(created);
        return created;
    }

    /**
     * {@code p[]} is the element of the array {@code p} when the server describes one. Otherwise it is an entry of a
     * parallel list, like the {@code key[]} and {@code value[]} of a map, and a plain child of the enclosing object.
     */
    private void attach(final PropertyNode node) {
        final String path = node.path();
        if (!path.endsWith(ARRAY_SUFFIX)) {
            attachTo(parentPath(path), node);
            return;
        }
        final String arrayPath = path.substring(0, path.length() - ARRAY_SUFFIX.length());
        final PropertyDefinition array = definitions.get(arrayPath);
        if (array != null && array.type() == PropertyType.ARRAY) {
            node(arrayPath).setElement(node);
        } else {
            attachTo(parentPath(arrayPath), node);
        }
    }

    private void attachTo(final String parentPath, final PropertyNode node) {
        if (parentPath.isEmpty()) {
            roots.add(node);
        } else {
            node(parentPath).addChild(node);
        }
    }

    private void resolveArray(final PropertyNode array) {
        if (array.element().isEmpty()) {
            final PropertyNode element = new PropertyNode(
                    PropertyDefinition.builder(array.path() + ARRAY_SUFFIX, PropertyType.STRING).build(), true);
            element.setKind(PropertyKind.STRING);
            nodes.put(element.path(), element);
            array.setElement(element);
        }
        final PropertyKind elementKind = array.element().orElseThrow().kind();
        array.setKind(elementKind == PropertyKind.OBJECT || elementKind == PropertyKind.MAP
                ? PropertyKind.ARRAY_OF_OBJECT
                : PropertyKind.ARRAY_OF_PRIMITIVE);
    }

    private static PropertyKind initialKind(final PropertyNode node) {
        return switch (node.definition().type()) {
            case OBJECT -> PropertyKind.OBJECT;
            case ARRAY -> PropertyKind.ARRAY_OF_PRIMITIVE;
            case NUMBER -> PropertyKind.NUMBER;
            case BOOLEAN -> PropertyKind.BOOLEAN;
            case ENUM -> PropertyKind.ENUM;
            case STRING -> PropertyKind.STRING;
        };
    }

    private static boolean looksLikeMap(final PropertyNode node) {
        return node.kind() == PropertyKind.OBJECT && node.children().size() == MAP_CHILDREN.size()
                && node.children().stream().allMatch(child -> MAP_CHILDREN.contains(child.name()));
    }

    private static String parentPath(final String path) {
        final int dot = path.lastIndexOf('.');
        return dot < 0 ? "" : path.substring(0, dot);
    }

    private static boolean isWellFormed(final String path) {
        return !path.isEmpty() && !path.startsWith(".") && !path.endsWith(".") && !path.contains("..");
    }
}
