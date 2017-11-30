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

    @Setter(AccessLevel.NONE)
    private List<PropertyNode> children = new ArrayList<>();

    @Setter(AccessLevel.NONE)
    private SimplePropertyDefinition property;

    @Setter(AccessLevel.NONE)
    private EParameterFieldType fieldType;

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
        return id.substring(0, id.lastIndexOf("."));
    }

    /**
     * Sets {@link SimplePropertyDefinition} and {@link EParameterFieldType}
     * These 2 fields should be set together
     * 
     * @param property Property Definition to set
     */
    public void setProperty(final SimplePropertyDefinition property) {
        this.property = property;
        this.fieldType = new WidgetTypeMapper(property).getFieldType();
    }

    public boolean isRoot() {
        return NO_PARENT_ID.equals(getParentId());
    }

    public boolean isLeaf() {
        return children.isEmpty();
    }

    public void accept(final PropertyVisitor visitor) {
        visitor.visit(this);
        children.forEach(child -> child.accept(visitor));
    }

}
