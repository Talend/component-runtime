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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import org.talend.sdk.component.tcketl.domain.Preconditions;
import org.talend.sdk.component.tcketl.domain.property.PropertyDefinition;

/**
 * The flat property list of a connector, arranged as a tree: a property is the child of the property whose path is its
 * own path without the last segment.
 *
 * <p>
 * The server does not describe every level, so missing ones are made up (see {@link PropertyNode#isImplicit()}): the
 * parent of an array element, the value of a map of objects, or an ancestor the payload skipped.
 */
public final class PropertyTree {

    private static final Pattern INDEX = Pattern.compile("\\[\\d+]");

    private final List<PropertyNode> roots;

    private final Map<String, PropertyNode> byPath;

    private final List<String> warnings;

    PropertyTree(final List<PropertyNode> roots, final Map<String, PropertyNode> byPath,
            final List<String> warnings) {
        this.roots = Collections.unmodifiableList(roots);
        this.byPath = Collections.unmodifiableMap(byPath);
        this.warnings = Collections.unmodifiableList(warnings);
    }

    public static PropertyTree of(final Collection<PropertyDefinition> definitions) {
        return new PropertyTreeBuilder(Preconditions.requireNonNull(definitions, "definitions")).build();
    }

    /** Turns {@code a.list[3].b} into {@code a.list[].b}, the way the server writes element definitions. */
    public static String definitionPath(final String concretePath) {
        return INDEX.matcher(concretePath).replaceAll("[]");
    }

    /** The top-level properties, normally the single configuration root. */
    public List<PropertyNode> roots() {
        return roots;
    }

    /** Finds a node from either its definition path or a concrete path ({@code list[2].b}). */
    public Optional<PropertyNode> find(final String path) {
        return Optional.ofNullable(byPath.get(definitionPath(path)));
    }

    /** Every node, in the order the tree met them. */
    public Collection<PropertyNode> nodes() {
        return byPath.values();
    }

    /** Anomalies met while building: duplicate or malformed paths, which were ignored. */
    public List<String> warnings() {
        return warnings;
    }

    static Map<String, PropertyNode> newIndex() {
        return new LinkedHashMap<>();
    }

    static List<PropertyNode> newRoots() {
        return new ArrayList<>();
    }
}
