/**
 * Copyright (C) 2006-2017 Talend Inc. - www.talend.com
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

import static org.talend.sdk.component.studio.model.parameter.Metadatas.ORDER_SEPARATOR;
import static org.talend.sdk.component.studio.model.parameter.Metadatas.UI_GRIDLAYOUT_ADVANCED;
import static org.talend.sdk.component.studio.model.parameter.Metadatas.UI_GRIDLAYOUT_MAIN;
import static org.talend.sdk.component.studio.model.parameter.Metadatas.UI_OPTIONS_ORDER;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.talend.core.model.process.EParameterFieldType;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

@Data
public class PropertyNode {

    @Setter(AccessLevel.NONE)
    private List<PropertyNode> children = new ArrayList<>();

    private final PropertyDefinitionDecorator property;

    private final EParameterFieldType fieldType;

    /**
     * Denotes whether this node is root in current tree
     */
    private final boolean root;

    public void addChild(final PropertyNode child) {
        children.add(child);
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
     * Traverses all nodes
     * 
     * @param visitor
     */
    public void accept(final PropertyVisitor visitor) {
        visitor.visit(this);
        children.forEach(child -> child.accept(visitor));
    }

    /**
     * Traverses Main form nodes in sorted according metadata order
     * 
     * @param visitor
     */
    public void acceptMain(final PropertyVisitor visitor) {
        visitor.visit(this);
        getMainChildren().forEach(child -> child.acceptMain(visitor));
    }

    /**
     * Returns only those children, which should be present on Main form
     * Result is sorted according order specified in metadata
     * 
     * @return main children
     */
    private List<PropertyNode> getMainChildren() {
        Set<String> mainChildrenNames = getMainChildrenNames();
        List<PropertyNode> mainChildren =
                children.stream().filter(node -> mainChildrenNames.contains(node.property.getName())).collect(
                        Collectors.toList());
        sortMain(mainChildren);
        return mainChildren;
    }

    private Set<String> getMainChildrenNames() {
        if (property.hasMainGridLayout()) {
            String gridLayout = property.getMetadata().get(UI_GRIDLAYOUT_MAIN);
            String[] names = gridLayout.split(",|\\|");
            return new HashSet<>(Arrays.asList(names));
        }
        // has advanced, but has no main gridlayout
        if (property.hasAdvancedGridLayout()) {
            return Collections.emptySet();
        }
        // else has no gridlayout at all, thus all children go to Main form
        Set<String> names = new HashSet<>();
        children.forEach(node -> names.add(node.getProperty().getName()));
        return names;
    }

    /**
     * Traverses Advanced form nodes in sorted according metadata order
     * 
     * @param visitor
     */
    public void acceptAdvanced(final PropertyVisitor visitor) {
        visitor.visit(this);
        getAdvancedChildren().forEach(child -> child.acceptAdvanced(visitor));
    }

    /**
     * Returns only those children, which should be present on Advanced form
     * Result is sorted according order specified in metadata
     * 
     * @return advanced children
     */
    private List<PropertyNode> getAdvancedChildren() {
        Set<String> advancedChildrenNames = getAdvancedChildrenNames();
        List<PropertyNode> advancedChildren =
                children.stream().filter(node -> advancedChildrenNames.contains(node.property.getName())).collect(
                        Collectors.toList());
        sortAdvanced(advancedChildren);
        return advancedChildren;
    }

    private Set<String> getAdvancedChildrenNames() {
        if (property.hasAdvancedGridLayout()) {
            String gridLayout = property.getMetadata().get(UI_GRIDLAYOUT_ADVANCED);
            String[] names = gridLayout.split(",|\\|");
            return new HashSet<>(Arrays.asList(names));
        }
        return Collections.emptySet();
    }

    /**
     * Sorts main children sublist
     * 
     * @param nodes
     */
    private void sortMain(final List<PropertyNode> nodes) {
        String gridLayout = property.getMetadata().get(UI_GRIDLAYOUT_MAIN);
        if (gridLayout != null) {
            sort(nodes, getGridLayoutOrder(gridLayout));
            return;
        }
        String optionsOrder = property.getMetadata().get(UI_OPTIONS_ORDER);
        if (optionsOrder != null) {
            sort(nodes, getOptionsOrder(optionsOrder));
            return;
        }
        // else do not sort
        return;
    }

    private void sortAdvanced(final List<PropertyNode> nodes) {
        String gridLayout = property.getMetadata().get(UI_GRIDLAYOUT_ADVANCED);
        if (gridLayout != null) {
            sort(nodes, getGridLayoutOrder(gridLayout));
        }
        // else do nothing
    }

    /**
     * Computes order for comparator accoding gridlayout
     * 
     * @param optionsOrder metadata value for ui::optionsorder::value
     * @return order
     */
    private HashMap<String, Integer> getGridLayoutOrder(final String gridLayout) {
        String[] values = gridLayout.split(",|\\|");
        HashMap<String, Integer> order = new HashMap<>();
        for (int i = 0; i < values.length; i++) {
            order.put(values[i], i);
        }
        return order;
    }

    /**
     * Sorts according specified order
     * 
     * @param nodes
     * @param order
     */
    private void sort(final List<PropertyNode> nodes, final HashMap<String, Integer> order) {
        nodes.sort((node1, node2) -> {
            Integer i1 = order.get(node1.getProperty().getName());
            Integer i2 = order.get(node2.getProperty().getName());
            return i1.compareTo(i2);
        });
    }

    /**
     * Computes order for comparator according optionsorder
     * 
     * @param optionsOrder metadata value for ui::optionsorder::value
     * @return order
     */
    private HashMap<String, Integer> getOptionsOrder(final String optionsOrder) {
        String[] values = optionsOrder.split(ORDER_SEPARATOR);
        HashMap<String, Integer> order = new HashMap<>();
        for (int i = 0; i < values.length; i++) {
            order.put(values[i], i);
        }
        return order;
    }
}
