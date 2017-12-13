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
import java.util.Map;

import org.talend.core.model.process.EComponentCategory;
import org.talend.core.model.process.IElement;
import org.talend.designer.core.model.FakeElement;
import org.talend.designer.core.model.components.ElementParameter;
import org.talend.sdk.component.server.front.model.PropertyValidation;
import org.talend.sdk.component.studio.metadata.TaCoKitElementParameter;

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
    private int lastRowNumber = 2;

    /**
     * Element(Node) for which parameters are created. It is required to set {@link ElementParameter} constructor
     */
    private final IElement iNode;

    /**
     * Defines {@link EComponentCategory} to be set in created {@link ElementParameter}
     * It may be {@link EComponentCategory#BASIC} or {@link EComponentCategory#ADVANCED}
     * for Basic and Advanced view correspondingly
     */
    private final EComponentCategory category;

    /**
     * Defines a Form name, for which properties are built. E.g. "Main" or "Advanced"
     */
    private final String formName;

    /**
     * Stores created component parameters
     */
    private List<ElementParameter> settings = new ArrayList<>();

    public SettingsCreator(final IElement iNode, final EComponentCategory category) {
        this.iNode = iNode;
        this.category = category;
        formName = (category == EComponentCategory.ADVANCED) ? Metadatas.ADVANCED_FORM : Metadatas.MAIN_FORM;
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
            ElementParameter parameter = null;
            switch (node.getFieldType()) {
            case CHECK:
                parameter = visitCheck(node);
                break;
            case CLOSED_LIST:
                parameter = visitClosedList(node);
                break;
            case TABLE:
                parameter = visitTable((TablePropertyNode) node);
                break;
            default:
                parameter = createParameter(node);
            }
            settings.add(parameter);
        }
    }

    IElement getNode() {
        return this.iNode;
    }

    void addSetting(final ElementParameter setting) {
        settings.add(setting);
    }

    /**
     * Creates {@link ElementParameter} for Check field type
     * Converts default value from String to Boolean and sets it
     */
    private ElementParameter visitCheck(final PropertyNode node) {
        ElementParameter parameter = createParameter(node);
        String defaultValue = node.getProperty().getDefaultValue();
        if (defaultValue == null) {
            parameter.setValue(false);
        } else {
            parameter.setValue(Boolean.parseBoolean(defaultValue));
        }
        return parameter;
    }

    /**
     * Creates {@link ElementParameter} for Closed List field type
     * Sets Closed List possible values and sets 1st element as default
     */
    private ElementParameter visitClosedList(final PropertyNode node) {
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
        parameter.setDefaultClosedListValue(node.getProperty().getDefaultValue());
        parameter.setDefaultValue(node.getProperty().getDefaultValue());
        return parameter;
    }

    /**
     * Creates {@link ElementParameter} for Table field type
     * Sets special fields specific for Table parameter
     * Based on schema field controls whether table toolbox (buttons under table) is shown
     */
    private ElementParameter visitTable(final TablePropertyNode tableNode) {
        ElementParameter parameter = createParameter(tableNode);

        List<ElementParameter> tableParameters = createTableParameters(tableNode);
        List<String> codeNames = new ArrayList<>(tableParameters.size());
        List<String> displayNames = new ArrayList<>(tableParameters.size());
        for (ElementParameter param : tableParameters) {
            codeNames.add(param.getName());
            displayNames.add(param.getDisplayName());
        }
        parameter.setListItemsDisplayName(displayNames.toArray(new String[0]));
        parameter.setListItemsDisplayCodeName(codeNames.toArray(new String[0]));
        parameter.setListItemsValue(tableParameters.toArray(new ElementParameter[0]));
        parameter.setListItemsShowIf(new String[tableParameters.size()]);
        parameter.setListItemsNotShowIf(new String[tableParameters.size()]);

        parameter.setValue(new ArrayList<Map<String, Object>>());
        // TODO change to real value
        parameter.setBasedOnSchema(false);
        return parameter;
    }

    /**
     * Creates {@link ElementParameter} and sets common state for different types of parameters
     */
    private ElementParameter createParameter(final PropertyNode node) {
        ElementParameter parameter = new TaCoKitElementParameter(iNode);
        parameter.setCategory(category);
        parameter.setDisplayName(node.getProperty().getDisplayName());
        parameter.setFieldType(node.getFieldType());
        parameter.setName(node.getProperty().getPath());
        parameter.setRepositoryValue(node.getProperty().getPath());
        if (!node.isColumn(formName)) {
            lastRowNumber++;
        } // else property should be shown on the same row
        parameter.setNumRow(lastRowNumber);
        parameter.setShow(true);
        parameter.setValue(node.getProperty().getDefaultValue());
        return parameter;
    }

    /**
     * Creates table parameters (columns) for Table property
     * 
     * @param tableNode {@link TablePropertyNode}
     * @return list of table parameters
     */
    private List<ElementParameter> createTableParameters(final TablePropertyNode tableNode) {
        List<PropertyNode> columns = tableNode.getColumns();
        SettingsCreator creator = new SettingsCreator(new FakeElement("table"), category);
        columns.forEach(column -> creator.visit(column));
        return creator.getSettings();
    }
}
