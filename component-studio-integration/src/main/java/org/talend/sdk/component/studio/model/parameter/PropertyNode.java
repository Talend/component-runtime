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

import static org.talend.sdk.component.studio.model.parameter.Metadatas.UI_GRIDLAYOUT_ADVANCED;
import static org.talend.sdk.component.studio.model.parameter.Metadatas.UI_GRIDLAYOUT_MAIN;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.talend.core.model.process.EParameterFieldType;
import org.talend.sdk.component.server.front.model.SimplePropertyDefinition;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

@Data
public class PropertyNode {

    private static final String PATH_SEPARATOR = ".";

    private static final String NO_PARENT_ID = "";

    /**
     * Suffix used in id ({@link SimplePropertyDefinition#getPath()}), which denotes Array typed property
     * (which is Table property in Studio)
     */
    private static final String ARRAY_PATH = "[]";

    @Setter(AccessLevel.NONE)
    private List<PropertyNode> children = new ArrayList<>();

    private final SimplePropertyDefinition property;

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
        String id = getId();
        if (!id.contains(PATH_SEPARATOR)) {
            return NO_PARENT_ID;
        }
        String parentId = id.substring(0, id.lastIndexOf("."));
        // following is true, when parent is Table property
        if (parentId.endsWith(ARRAY_PATH)) {
            parentId = parentId.substring(0, parentId.lastIndexOf("[]"));
        }
        return parentId;
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
        return children.stream().filter(node -> mainChildrenNames.contains(node.property.getName())).collect(Collectors.toList());
    }
    
    private Set<String> getMainChildrenNames() {
        if (SimplePropertyDefinitionUtils.hasMainGridLayout(property)) {
            String gridLayout = property.getMetadata().get(UI_GRIDLAYOUT_MAIN);
            String[] names = gridLayout.split(",|\\|");
            return new HashSet<>(Arrays.asList(names));
        }
        // has advanced, but has no main gridlayout
        if (SimplePropertyDefinitionUtils.hasAdvancedGridLayout(property)) {
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
        return children.stream().filter(node -> advancedChildrenNames.contains(node.property.getName())).collect(Collectors.toList());
    }

    private Set<String> getAdvancedChildrenNames() {
        if (SimplePropertyDefinitionUtils.hasAdvancedGridLayout(property)) {
            String gridLayout = property.getMetadata().get(UI_GRIDLAYOUT_ADVANCED);
            String[] names = gridLayout.split(",|\\|");
            return new HashSet<>(Arrays.asList(names));
        }
        return Collections.emptySet();
    }
}
