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
import java.util.Collections;
import java.util.List;

import org.talend.core.model.process.EComponentCategory;
import org.talend.core.model.process.INode;
import org.talend.designer.core.model.components.ElementParameter;
import org.talend.sdk.component.server.front.model.SimplePropertyDefinition;

/**
 * Creates properties from leafs
 */
public class SettingsCreator implements PropertyVisitor {

    private int lastRowNumber = 1;

    private final INode iNode;

    private List<ElementParameter> settings = new ArrayList<>();

    public SettingsCreator(final INode iNode) {
        this.iNode = iNode;
    }

    /**
     * Creates ElementParameters only for leafs
     */
    @Override
    public void visit(final PropertyNode node) {
        if (node.isLeaf()) {
            createParameter(node);
        }
    }

    public List<ElementParameter> getSettings() {
        return Collections.unmodifiableList(settings);
    }

    /**
     * @param node
     */
    private void createParameter(final PropertyNode node) {
        SimplePropertyDefinition definition = node.getProperty();
        ElementParameter parameter = new ElementParameter(iNode);

        // Set common state
        // TODO
        parameter.setCategory(EComponentCategory.BASIC);
        parameter.setCurrentRow(0);
        parameter.setDisplayName(definition.getDisplayName());
        parameter.setFieldType(node.getFieldType());
        parameter.setName(definition.getPath());
        parameter.setNumRow(lastRowNumber++);
        parameter.setShow(true);
        parameter.setContextMode(false);
        parameter.setValue("default value will be here");

        // Set specific state. TODO refactor if possible
        switch (parameter.getFieldType()) {
        case CLOSED_LIST:
            setupTableParameter(parameter);
            break;
        default:
            // do nothing
        }
        settings.add(parameter);
    }

    private void setupTableParameter(final ElementParameter parameter) {
        parameter.setListItemsValue(new String[] { "Item1T", "Item2T", "Item3T" });
        parameter.setListItemsDisplayName(new String[] { "Item1D", "Item2D", "Item3D" });
        parameter.setListItemsDisplayCodeName(new String[] { "Item1C", "Item2C", "Item3C" });
        parameter.setListItemsReadOnlyIf(new String[3]);
        parameter.setListItemsNotReadOnlyIf(new String[3]);
        parameter.setListItemsShowIf(new String[3]);
        parameter.setListItemsNotShowIf(new String[3]);
        parameter.setDefaultClosedListValue("Item1T");
        parameter.setDefaultValue("Another default");
        parameter.setValue("Item1D");
    }

}
