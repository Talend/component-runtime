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

import static org.talend.sdk.component.studio.model.parameter.Metadatas.MAIN_FORM;

import java.util.ArrayList;
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
import lombok.ToString;

@Data
@ToString(exclude = "parent")
public class PropertyNode {

    @Setter(AccessLevel.PRIVATE)
    private PropertyNode parent;

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
        PropertyDefinitionDecorator parentProperty = getParent().getProperty();
        if (!parentProperty.hasGridLayout(form)) {
            return false;
        }
        return parentProperty.isColumn(property.getName(), form);
    }

    /**
     * Traverses all nodes
     * 
     * @param visitor the property visitor to use to traverse the nodes.
     */
    public void accept(final PropertyVisitor visitor) {
        visitor.visit(this);
        children.forEach(child -> child.accept(visitor));
    }

    /**
     * Traverses nodes of specified <code>form</code> in sorted according metadata order
     * 
     * @param visitor the property visitor to use to traverse the nodes.
     * @param form Name of form
     */
    public void accept(final PropertyVisitor visitor, final String form) {
        visitor.visit(this);
        List<PropertyNode> children = getChildren(form);
        sortChildren(children, form);
        children.forEach(child -> child.accept(visitor, form));
    }

    /**
     * Returns children, which belongs to specified <code>form</code>
     * 
     * @param form Name of form
     * @return children of specified form
     */
    List<PropertyNode> getChildren(final String form) {
        Set<String> childrenNames = getChildrenNames(form);
        List<PropertyNode> formChildren =
                children.stream().filter(node -> childrenNames.contains(node.property.getName())).collect(
                        Collectors.toList());
        return formChildren;
    }

    /**
     * Sorts children according order specified in metadata or do nothing if order is not specified
     * 
     * @param children children node, which belongs specified form
     * @param form Name or form
     */
    void sortChildren(final List<PropertyNode> children, final String form) {
        HashMap<String, Integer> order = property.getChildrenOrder(form);
        if (order != null) {
            children.sort((node1, node2) -> {
                Integer i1 = order.get(node1.getProperty().getName());
                Integer i2 = order.get(node2.getProperty().getName());
                return i1.compareTo(i2);
            });
        }
        // else do not sort
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

}
