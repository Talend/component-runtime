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
import java.util.List;
import java.util.Optional;

import org.talend.sdk.component.tcketl.domain.property.LayoutHints;
import org.talend.sdk.component.tcketl.domain.property.PropertyDefinition;

/**
 * A node of the {@link PropertyTree}: one property definition, or an implicit object standing for a level the server
 * did not describe. Its path is a <em>definition</em> path, where array elements are written {@code list[]}; the
 * stored values use <em>concrete</em> paths, {@code list[2]}, see {@link #concretePath(String, int)}.
 *
 * <p>
 * The tree is built once by {@link PropertyTree#of} and is not modified afterwards.
 */
public final class PropertyNode {

    private static final String CHECKPOINT_OPTION = "$checkpoint";

    private final PropertyDefinition definition;

    private final boolean implicit;

    private final List<PropertyNode> children = new ArrayList<>();

    private PropertyKind kind;

    private PropertyNode parent;

    private PropertyNode element;

    PropertyNode(final PropertyDefinition definition, final boolean implicit) {
        this.definition = definition;
        this.implicit = implicit;
    }

    public PropertyDefinition definition() {
        return definition;
    }

    public String path() {
        return definition.path();
    }

    public String name() {
        return definition.name();
    }

    public PropertyKind kind() {
        return kind;
    }

    /** True when the server sent no definition for this level and the tree made one up. */
    public boolean isImplicit() {
        return implicit;
    }

    public Optional<PropertyNode> parent() {
        return Optional.ofNullable(parent);
    }

    /** The properties nested in this one, in the order the server listed them. */
    public List<PropertyNode> children() {
        return Collections.unmodifiableList(children);
    }

    public Optional<PropertyNode> child(final String childName) {
        return children.stream().filter(child -> child.name().equals(childName)).findFirst();
    }

    /** For an array, the node describing one of its elements. */
    public Optional<PropertyNode> element() {
        return Optional.ofNullable(element);
    }

    /** True for the node describing the elements of an array. */
    public boolean isElement() {
        return parent != null && parent.element == this;
    }

    /** True when the node occurs once per entry of a list: an array element, or the key or value of a map. */
    public boolean repeats() {
        return isElement() || name().endsWith("[]");
    }

    /**
     * False for what the user neither sees nor edits: a hidden property ({@code ui::hidden}) and the runtime state
     * kept under {@code $checkpoint}. What lies below such a node is not displayed either.
     */
    public boolean isDisplayed() {
        return !definition.ui().hidden() && !name().startsWith(CHECKPOINT_OPTION);
    }

    /**
     * The layout arranging the children of this node. An array element is arranged by the layout of its array, since
     * that is where the server declares it.
     */
    public LayoutHints layout() {
        return isElement() && definition.layout().isEmpty() ? parent.definition.layout() : definition.layout();
    }

    /**
     * The path of this node for a concrete instance of its parent.
     *
     * @param parentConcretePath the concrete path of the parent (of the array for an element).
     * @param index the position of this node when it repeats: an array element, or an entry of a map; ignored
     * otherwise.
     */
    public String concretePath(final String parentConcretePath, final int index) {
        if (parent == null) {
            return path();
        }
        if (isElement()) {
            return parentConcretePath + '[' + index + ']';
        }
        final String name = name();
        if (repeats()) {
            return parentConcretePath + '.' + name.substring(0, name.length() - 2) + '[' + index + ']';
        }
        return parentConcretePath + '.' + name;
    }

    @Override
    public String toString() {
        return "PropertyNode[" + path() + ", " + kind + (implicit ? ", implicit" : "") + ']';
    }

    void setKind(final PropertyKind value) {
        this.kind = value;
    }

    void addChild(final PropertyNode child) {
        child.parent = this;
        children.add(child);
    }

    void setElement(final PropertyNode value) {
        value.parent = this;
        this.element = value;
    }
}
