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
import java.util.HashMap;

import org.talend.sdk.component.server.front.model.SimplePropertyDefinition;
import org.talend.sdk.component.studio.ComponentModel;

import lombok.NoArgsConstructor;

/**
 * Provides methods for handling {@link PropertyNode} tree
 */
@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class PropertyNodeUtils {

    /**
     * Creates tree representation of {@link SimplePropertyDefinition} .
     * Not all definitions represent Component property, which may store User setting.
     * Some of them are holders for other definitions (like Forms or Properties in v0 integration)
     * ElementParameters should be created only from leaf nodes in this tree
     * Internal nodes store useful metadata information like ordering
     * 
     * @param properties a collections of {@link SimplePropertyDefinition} retrieved from {@link ComponentModel}
     * @return root node of created tree
     */
    public static PropertyNode createPropertyTree(Collection<SimplePropertyDefinition> properties) {
        PropertyNode root = null;
        HashMap<String, PropertyNode> nodes = new HashMap<>();

        for (SimplePropertyDefinition definition : properties) {
            String id = definition.getPath();
            PropertyNode current = nodes.computeIfAbsent(id, key -> new PropertyNode());
            current.setProperty(definition);

            if (current.isRoot()) {
                root = current;
            } else {
                String parentId = current.getParentId();
                PropertyNode parent = nodes.computeIfAbsent(parentId, key -> new PropertyNode());
                parent.addChild(current);
            }
        }
        return root;
    }

    /**
     * Sorts siblings according that how they should be shown on UI
     */
    public static void sortPropertyTree(PropertyNode root) {
        root.accept(new PropertyVisitor() {

            @Override
            public void visit(PropertyNode node) {
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
            private void optionsOrderSort(PropertyNode node, String optionsOrder) {
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
            private HashMap<String, Integer> getOrder(String optionsOrder) {
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
