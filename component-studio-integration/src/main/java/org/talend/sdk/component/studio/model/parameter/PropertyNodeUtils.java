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
import static org.talend.sdk.component.studio.model.parameter.Metadatas.UI_OPTIONS_ORDER;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.talend.core.model.process.EParameterFieldType;
import org.talend.sdk.component.server.front.model.SimplePropertyDefinition;

import lombok.NoArgsConstructor;

/**
 * Provides methods for handling {@link PropertyNode} tree
 */
@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class PropertyNodeUtils {

    /**
     * Creates tree representation of {@link SimplePropertyDefinition}.
     * Not all definitions represent Component property, which may store User setting.
     * Some of them are holders for other definitions (like Forms or Properties in v0 integration)
     * ElementParameters should be created only from leaf nodes in this tree
     * Internal nodes store useful metadata information like ordering </br>
     * 
     * There may be different types of node (different {@link PropertyNode} implementations)
     * Node type is defined by {@link SimplePropertyDefinition}, so it should be known during
     * node creation </br>
     * 
     * Tree is created according following algorithm:
     * <ol>
     * <li>Find root {@link SimplePropertyDefinition}</li>
     * <li>Create root node</li>
     * <li>Create other nodes, but not root</li>
     * <li>Create links between nodes</li>
     * </ol>
     * Note, there are 3 traversals through Collection
     * 
     * @param properties a collections of {@link SimplePropertyDefinition} retrieved from ComponentModel
     * @return root node of created tree
     */
    public static PropertyNode createPropertyTree(final Collection<SimplePropertyDefinition> properties) {
        if (properties == null) {
            throw new NullPointerException("properties should not be null");
        }
        if (properties.isEmpty()) {
            throw new IllegalArgumentException("properties should not be empty");
        }
        PropertyNode root = createRootNode(properties);
        HashMap<String, PropertyNode> nodes = new HashMap<>();
        nodes.put(root.getId(), root);

        createRemainingNodes(properties, nodes);
        linkNodes(properties, nodes);
        return root;
    }

    /**
     * Creates all nodes and put them into <code>nodes</code> except root node, as it is already there
     * 
     * @param properties all {@link SimplePropertyDefinition}
     * @param nodes stores all created {@link PropertyNode}
     */
    static void createRemainingNodes(final Collection<SimplePropertyDefinition> properties,
            final Map<String, PropertyNode> nodes) {
        properties.forEach(property -> nodes.putIfAbsent(property.getPath(), createNode(property, false)));
    }

    /**
     * Links child nodes with their parent nodes. Only root node has no parent node, so it is skipped
     * 
     * @param properties all {@link SimplePropertyDefinition}
     * @param nodes all {@link PropertyNode}
     */
    static void linkNodes(final Collection<SimplePropertyDefinition> properties,
            final Map<String, PropertyNode> nodes) {
        properties.stream().map(property -> property.getPath()).forEach(id -> {
            PropertyNode current = nodes.get(id);
            if (!current.isRoot()) {
                String parentId = current.getParentId();
                PropertyNode parent = nodes.get(parentId);
                parent.addChild(current);
            }
        });
    }

    /**
     * Factory method, which creates specific {@link PropertyNode} implementation according Property type
     * Method also sets {@link SimplePropertyDefinition}
     * 
     * @param property Property Definition
     * @param isRoot specifies whether this Node is root Node
     * @return {@link PropertyNode} implementation
     */
    static PropertyNode createNode(final SimplePropertyDefinition property, final boolean isRoot) {
        EParameterFieldType fieldType = new WidgetTypeMapper(property).getFieldType();
        PropertyNode node = null;
        switch (fieldType) {
        case TABLE:
            node = new TablePropertyNode(property, fieldType, isRoot);
            break;
        default:
            node = new PropertyNode(property, fieldType, isRoot);
        }
        return node;
    }

    /**
     * Creates and returns root PropertyNode
     * 
     * @param properties all SimplePropertyDefinitions
     * @return root PropertyNode
     */
    static PropertyNode createRootNode(final Collection<SimplePropertyDefinition> properties) {
        return createNode(findRootProperty(properties), true);
    }

    /**
     * Finds a root of {@link SimplePropertyDefinition} subtree represented by <code>properties</code> Collection.
     * Root is such {@link SimplePropertyDefinition}, which <code>path</code> is "shortest" (minimal) in following
     * meaning:
     * path1 is less than path2, when path2 contains path1. </br>
     * E.g. path1 = "p0.p1"; path2 = "p0.p1.p2"; // path1 is less than path2</br>
     * It is assumed input <code>properties</code> is not null and not empty.
     * Also it is assumed Collection contains only single root element.
     * Note, not any 2 arbitrary SimplePropertyDefinition may be compared. They may belong to different branches in a
     * tree.
     * Such SimplePropertyDefinition assumed to be equal
     * 
     * @param properties Collection of {@link SimplePropertyDefinition}
     * @return root {@link SimplePropertyDefinition}
     */
    static SimplePropertyDefinition findRootProperty(final Collection<SimplePropertyDefinition> properties) {
        SimplePropertyDefinition rootProperty = Collections.min(properties, (p1, p2) -> {
            String path1 = p1.getPath();
            String path2 = p2.getPath();
            if (path2.contains(path1)) {
                return -1;
            }
            if (path1.contains(path2)) {
                return 1;
            }
            return 0;
        });
        return rootProperty;
    }

    /**
     * Sorts siblings according that how they should be shown on UI
     */
    public static void sortPropertyTree(final PropertyNode root) {
        root.accept(new PropertyVisitor() {

            @Override
            public void visit(final PropertyNode node) {
                SimplePropertyDefinition property = node.getProperty();
                String optionsOrder = property.getMetadata().get(UI_OPTIONS_ORDER);
                if (optionsOrder != null) {
                    optionsOrderSort(node, optionsOrder);
                }
                // TODO implement sorting according GridLayout
            }

            /**
             * Sorts node children according order specified in OptionsOrder or GridLayout
             * 
             * @param node current node
             * @param optionsOrder metadata value for ui::optionsorder::value
             */
            private void optionsOrderSort(final PropertyNode node, final String optionsOrder) {
                HashMap<String, Integer> order = getOrder(optionsOrder);

                node.getChildren().sort((node1, node2) -> {
                    Integer i1 = order.get(node1.getProperty().getName());
                    Integer i2 = order.get(node2.getProperty().getName());
                    return i1.compareTo(i2);
                });
            }

            /**
             * Computes order for comparator
             * 
             * @param optionsOrder metadata value for ui::optionsorder::value
             * @return order
             */
            private HashMap<String, Integer> getOrder(final String optionsOrder) {
                String[] values = optionsOrder.split(ORDER_SEPARATOR);
                HashMap<String, Integer> order = new HashMap<>();
                for (int i = 0; i < values.length; i++) {
                    order.put(values[i], i);
                }
                return order;
            }
        });
    }
}
