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

import java.util.ArrayList;
import java.util.List;

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

    public void accept(final PropertyVisitor visitor) {
        visitor.visit(this);
        children.forEach(child -> child.accept(visitor));
    }

}
