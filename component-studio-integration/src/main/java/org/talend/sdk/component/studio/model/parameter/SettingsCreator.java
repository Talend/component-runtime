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
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.talend.core.model.process.EComponentCategory;
import org.talend.core.model.process.INode;
import org.talend.designer.core.model.components.ElementParameter;
import org.talend.sdk.component.server.front.model.PropertyValidation;

/**
 * Creates properties from leafs
 */
public class SettingsCreator implements PropertyVisitor {

    /**
     * {@link ElementParameter} has numRow field which stores widget relative position (row number on which it
     * appears)
     * If numRow = 10, this does not necessarily mean that widget will be shown on 10th line,
     * but when 1 parameter has numRow = 8, and 2 has numRow = 10, then 2 will shown under 1
     */
    private int lastRowNumber = 1;

    /**
     * Node for which parameters are created. It is required to set {@link ElementParameter} constructor
     */
    private final INode iNode;

    /**
     * Stores configuration parameters for {@link INode}
     */
    private List<ElementParameter> settings = new ArrayList<>();

    public SettingsCreator(final INode iNode) {
        this.iNode = iNode;
    }

    public List<ElementParameter> getSettings() {
        return Collections.unmodifiableList(settings);
    }

    /**
     * Creates ElementParameters only from leafs
     * 
     */
    @Override
    public void visit(final PropertyNode node) {
        if (node.isLeaf()) {
            switch (node.getFieldType()) {
            case CLOSED_LIST:
                visitClosedList(node);
                break;
            default:
                visitDefault(node);
            }
        }
    }

    /**
     * Creates {@link ElementParameter} for Closed List field type and adds it to settings
     * Sets Closed List possible values and sets 1st element as default
     */
    private void visitClosedList(final PropertyNode node) {
        ElementParameter parameter = createParameter(node);
        PropertyValidation validation = node.getProperty().getValidation();
        if (validation == null) {
            throw new IllegalArgumentException("validation should not be null");
        }
        Collection<String> possibleValues = validation.getEnumValues();
        if (possibleValues == null) {
            throw new IllegalArgumentException("validation enum values should not be null");
        }
        parameter.setListItemsValue(possibleValues.toArray());
        parameter.setListItemsDisplayName(possibleValues.toArray(new String[0]));
        parameter.setListItemsDisplayCodeName(possibleValues.toArray(new String[0]));
        parameter.setListItemsReadOnlyIf(new String[possibleValues.size()]);
        parameter.setListItemsNotReadOnlyIf(new String[possibleValues.size()]);
        parameter.setListItemsShowIf(new String[possibleValues.size()]);
        parameter.setListItemsNotShowIf(new String[possibleValues.size()]);
        settings.add(parameter);
    }

    /**
     * Creates {@link ElementParameter} for default parameter type and adds it to settings
     */
    private void visitDefault(final PropertyNode node) {
        settings.add(createParameter(node));
    }

    /**
     * Creates {@link ElementParameter} and sets common state for different types of parameters
     */
    private ElementParameter createParameter(final PropertyNode node) {
        ElementParameter parameter = new ElementParameter(iNode);
        // TODO implement category computing
        parameter.setCategory(EComponentCategory.BASIC);
        parameter.setDisplayName(node.getProperty().getDisplayName());
        parameter.setFieldType(node.getFieldType());
        parameter.setName(node.getProperty().getPath());
        parameter.setNumRow(lastRowNumber++);
        parameter.setShow(true);
        parameter.setValue(node.getProperty().getDefaultValue());
        return parameter;
    }

}
