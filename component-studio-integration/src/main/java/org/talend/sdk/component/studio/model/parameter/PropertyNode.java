/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.studio.model.parameter;

import static org.talend.sdk.component.studio.model.parameter.Metadatas.MAIN_FORM;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.talend.core.model.process.EParameterFieldType;

import lombok.AccessLevel;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Data
@ToString(exclude = "parent")
public class PropertyNode {

    @Setter(AccessLevel.PROTECTED)
    private PropertyNode parent;

    @Setter(AccessLevel.NONE)
    private final List<PropertyNode> children = new ArrayList<>();

    private final Map<String, Layout> layouts = new HashMap<>();

    private final PropertyDefinitionDecorator property;

    private final EParameterFieldType fieldType;

    /**
     * Denotes whether this node is root in current tree
     */
    private final boolean root;

    public void addChild(final PropertyNode child) {
        children.add(child);
        child.setParent(this);
    }

    public String getId() {
        return property.getPath();
    }

    public String getParentId() {
        return property.getParentPath();
    }

    public boolean isLeaf() {
        return children.isEmpty();
    }

    /**
     * Checks whether it is column according ui::gridlayout for specified <code>form</code>
     * 
     * @param form Name of form
     * @return true, if it column; false - otherwise
     */
    public boolean isColumn(final String form) {
        if (isRoot()) {
            return false;
        }
        final PropertyDefinitionDecorator parentProperty = getParent().getProperty();
        if (!parentProperty.hasGridLayout(form)) {
            return getParent().isColumn(form);
        }
        return parentProperty.isColumn(property.getName(), form);
    }

    /**
     * Traverses all nodes
     * 
     * @param visitor the property visitor to use to traverse the nodes.
     */
    public void accept(final PropertyVisitor visitor) {
        children.forEach(child -> child.accept(visitor));
        visitor.visit(this);
    }

    /**
     * Traverses nodes of specified <code>form</code> in sorted according metadata order
     * 
     * @param visitor the property visitor to use to traverse the nodes.
     * @param form Name of form
     */
    public void accept(final PropertyVisitor visitor, final String form) {
        final List<PropertyNode> children = sortChildren(getChildren(form), form);
        children.forEach(child -> child.accept(visitor, form));
        visitor.visit(this);
    }

    private void acceptParentFirst(final PropertyVisitor visitor, final String form) {
        visitor.visit(this);
        final List<PropertyNode> children = sortChildren(getChildren(form), form);
        children.forEach(child -> child.acceptParentFirst(visitor, form));
    }

    /**
     * Returns children, which belongs to specified <code>form</code>
     * 
     * @param form Name of form
     * @return children of specified form
     */
    public List<PropertyNode> getChildren(final String form) {
        final Set<String> childrenNames = getChildrenNames(form);
        return children.stream().filter(node -> childrenNames.contains(node.property.getName())).collect(
                Collectors.toList());
    }

    // TODO make it package-private
    public PropertyNode getChild(final String name, final String form) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(form);
        return getChildren(form).stream().filter(p -> name.equals(p.getProperty().getName())).findFirst().orElseThrow(
                () -> new IllegalArgumentException("no child with name " + name));
    }

    /**
     * Sorts children according order specified in metadata or do nothing if order is not specified
     * 
     * @param children children node, which belongs specified form
     * @param form Name or form
     */
    // TODO change it to return sorted children
    public List<PropertyNode> sortChildren(final List<PropertyNode> children, final String form) {
        HashMap<String, Integer> order = property.getChildrenOrder(form);
        if (order != null) {
            children.sort((node1, node2) -> {
                Integer i1 = order.get(node1.getProperty().getName());
                Integer i2 = order.get(node2.getProperty().getName());
                return i1.compareTo(i2);
            });
        }
        // else do not sort
        return children;
    }

    /**
     * Returns children names for specified <code>form</code>.
     * If <code>form</code> is Main form its children may be specified by ui::gridlayout or ui:optionsorder.
     * If it has no both metadata, then all children are considered as Main children.
     * For other <code>form</code> children may be specified only by ui::gridlayout.
     * 
     * @param form Name of form
     * @return children names of specified <code>form</code>
     */
    private Set<String> getChildrenNames(final String form) {
        if (MAIN_FORM.equals(form)) {
            return getMainChildrenNames();
        } else {
            return property.getChildrenNames(form);
        }
    }

    /**
     * Returns children names for Main form
     * If it has ui:gridlayout metadata value for Main form, then names are retrieved from there
     * If it has ui:gridlayout for other forms, then it is considered that Main form is empty
     * If it has ui:optionsorder (and has no any ui:gridlayout), then names are retrieved from there
     * If it has no both metadatas, then all children belong to Main form
     * 
     * @return children names for Main form
     */
    private Set<String> getMainChildrenNames() {
        if (property.hasGridLayout(MAIN_FORM)) {
            return property.getChildrenNames(MAIN_FORM);
        }
        if (property.hasGridLayouts()) {
            return Collections.emptySet();
        }
        if (property.hasOptionsOrder()) {
            return property.getOptionsOrderNames();
        }
        Set<String> names = new HashSet<>();
        children.forEach(node -> names.add(node.getProperty().getName()));
        return names;
    }

    public Layout getLayout(final String name) {
        if (!layouts.containsKey(name)) {
            throw new IllegalArgumentException("No layout " + name);
        }
        return layouts.get(name);
    }

    /**
     * Creates layout for specified {@code form} and computes position for all children nodes.
     * It traverse a tree in-depth. Children nodes are visited before parent
     * 
     * @param form Layout form for which node position is computed
     */
    void computePosition(final String form) {
        accept(new LayoutHeightVisitor(form), form);
        acceptParentFirst(new LayoutPositionVisitor(form), form);
    }

    void addLayout(final String name, final Layout layout) {
        layouts.putIfAbsent(name, layout);
    }

    @RequiredArgsConstructor
    private static class LayoutHeightVisitor implements PropertyVisitor {

        private final String form;

        private PropertyNode current;

        @Override
        public void visit(final PropertyNode node) {
            this.current = node;
            createLayout();
            computeHeight();
        }

        private void createLayout() {
            final Layout layout = new Layout(current.getProperty().getPath());
            if (!current.isLeaf()) {
                if (current.getProperty().hasGridLayout(form)) {
                    fillGridLayout(layout);
                } else {
                    fillSimpleLayout(layout);
                }
            }
            current.addLayout(form, layout);
        }

        private void fillGridLayout(final Layout layout) {
            final String gridLayout = current.getProperty().getGridLayout(form);
            final String[] rows = gridLayout.split("\\|");
            // create Level for each row
            for (final String row : rows) {
                final Level level = new Level();
                layout.getLevels().add(level);
                for (final String column : row.split(",")) {
                    final PropertyNode child = current.getChild(column, form);
                    level.getColumns().add(child.getLayout(form));
                }
            }
        }

        private void fillSimpleLayout(final Layout layout) {
            final List<PropertyNode> children = current.sortChildren(current.getChildren(form), form);
            children.forEach(child -> {
                final Level level = new Level();
                layout.getLevels().add(level);
                // only one column in each level when there is no GridLayout
                level.getColumns().add(child.getLayout(form));
            });
        }

        private void computeHeight() {
            final Layout layout = current.getLayout(form);
            int height = 0;
            if (current.isLeaf()) {
                height = 1;
                // TODO try to refactor this
                if (current.getProperty().hasConstraint() || current.getProperty().hasValidation()) {
                    height++;
                }
            } else {
                layout.getLevels().forEach(level -> {
                    final int levelHeight = level.getColumns().stream().mapToInt(Layout::getHeight).max().getAsInt();
                    level.setHeight(levelHeight);
                });
                height = layout.getLevels().stream().mapToInt(Level::getHeight).sum();
            }
            layout.setHeight(height);
        }
    }

    @RequiredArgsConstructor
    private static class LayoutPositionVisitor implements PropertyVisitor {

        /**
         * First 2 position are occupied by schema and property type
         */
        private static final int INITIAL_POSITION = 3;

        private final String form;

        @Override
        public void visit(final PropertyNode node) {
            if (!node.isLeaf()) {
                final Layout layout = node.getLayout(form);
                if (node.isRoot()) {
                    layout.setPosition(INITIAL_POSITION);
                }
                int position = layout.getPosition();
                for (final Level level : layout.getLevels()) {
                    level.setPosition(position);
                    for (final Layout column : level.getColumns()) {
                        column.setPosition(position);
                    }
                    position = position + level.getHeight();
                }
            } // else no-op as position is set during visiting only parent node
        }

    }

}
