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

import static org.talend.sdk.component.studio.metadata.ITaCoKitElementParameterEventProperties.EVENT_PROPERTY_VALUE_CHANGED;
import static org.talend.sdk.component.studio.model.parameter.Metadatas.DOT_PATH_SEPARATOR;
import static org.talend.sdk.component.studio.model.parameter.Metadatas.PARENT_NODE;
import static org.talend.sdk.component.studio.model.parameter.Metadatas.PATH_SEPARATOR;
import static org.talend.sdk.component.studio.model.parameter.Metadatas.UI_STRUCTURE_TYPE;
import static org.talend.sdk.component.studio.model.parameter.Metadatas.UI_STRUCTURE_VALUE;

import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.talend.core.model.process.EComponentCategory;
import org.talend.core.model.process.EConnectionType;
import org.talend.core.model.process.EParameterFieldType;
import org.talend.core.model.process.IElement;
import org.talend.designer.core.model.FakeElement;
import org.talend.designer.core.model.components.EParameterName;
import org.talend.designer.core.model.components.ElementParameter;
import org.talend.sdk.component.server.front.model.PropertyValidation;
import org.talend.sdk.component.studio.metadata.TaCoKitElementParameter;
import org.talend.sdk.component.studio.model.parameter.listener.ParameterActivator;

/**
 * Creates properties from leafs
 */
public class SettingsCreator implements PropertyVisitor {

    /**
     * {@link TaCoKitElementParameter} has numRow field which stores widget relative position (row number on which it
     * appears)
     * If numRow = 10, this does not necessarily mean that widget will be shown on 10th line,
     * but when 1 parameter has numRow = 8, and 2 has numRow = 10, then 2 will shown under 1
     */
    private int lastRowNumber = 2;

    /**
     * Element(Node) for which parameters are created. It is required to set {@link TaCoKitElementParameter} constructor
     */
    private final IElement iNode;

    /**
     * Defines {@link EComponentCategory} to be set in created {@link TaCoKitElementParameter}
     * It may be {@link EComponentCategory#BASIC} or {@link EComponentCategory#ADVANCED}
     * for Basic and Advanced view correspondingly
     */
    private final EComponentCategory category;

    /**
     * Defines a Form name, for which properties are built. E.g. "Main" or "Advanced"
     */
    private final String formName;

    /**
     * {@link ElementParameter} which defines whether UI should be redrawn
     */
    private final ElementParameter redrawParameter;

    /**
     * Stores created component parameters.
     * Key is parameter name (which is also its path)
     */
    protected final Map<String, TaCoKitElementParameter> settings = new LinkedHashMap<>();

    /**
     * Stores created {@link ParameterActivator} for further registering them into corresponding
     * {@link ElementParameter}
     */
    private final Map<String, List<ParameterActivator>> activators = new HashMap<>();

    public SettingsCreator(final IElement iNode, final EComponentCategory category,
            final ElementParameter redrawParameter) {
        this.iNode = iNode;
        this.category = category;
        this.redrawParameter = redrawParameter;
        formName = (category == EComponentCategory.ADVANCED) ? Metadatas.ADVANCED_FORM : Metadatas.MAIN_FORM;
    }

    /**
     * Registers created Listeners in {@link TaCoKitElementParameter} and returns list of created parameters.
     * Also setup initial visibility according initial value of target parameters
     * 
     * @return created parameters
     */
    public List<ElementParameter> getSettings() {

        activators.forEach((path, activators) -> {
            TaCoKitElementParameter targetParameter = settings.get(path);
            targetParameter.setRedrawParameter(redrawParameter);
            activators.forEach(activator -> {
                targetParameter.registerListener(EVENT_PROPERTY_VALUE_CHANGED, activator);
                initVisibility(targetParameter, activator);
            });
        });

        return Collections.unmodifiableList(new ArrayList<>(settings.values()));
    }

    /**
     * Sends initial event to listener to set initial visibility
     */
    private void initVisibility(final ElementParameter targetParameter, final ParameterActivator listener) {
        Object initialValue = targetParameter.getValue();
        PropertyChangeEvent event =
                new PropertyChangeEvent(targetParameter, EVENT_PROPERTY_VALUE_CHANGED, initialValue, initialValue);
        listener.propertyChange(event);
    }

    /**
     * Creates ElementParameters only from leafs
     * 
     */
    @Override
    public void visit(final PropertyNode node) {
        if (node.isLeaf()) {
            TaCoKitElementParameter parameter = null;
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
            case SCHEMA_TYPE:
                parameter = visitSchema(node);
                break;
            default:
                parameter = createParameter(node);
            }
            settings.put(parameter.getName(), parameter);
        }
    }

    IElement getNode() {
        return this.iNode;
    }

    void addSetting(final TaCoKitElementParameter setting) {
        settings.put(setting.getName(), setting);
    }

    /**
     * Creates {@link TaCoKitElementParameter} for Check field type
     * Converts default value from String to Boolean and sets it
     */
    private TaCoKitElementParameter visitCheck(final PropertyNode node) {
        TaCoKitElementParameter parameter = createParameter(node);
        String defaultValue = node.getProperty().getDefaultValue();
        if (defaultValue == null) {
            parameter.setValue(false);
        } else {
            parameter.setValue(Boolean.parseBoolean(defaultValue));
        }
        return parameter;
    }

    /**
     * Creates {@link TaCoKitElementParameter} for Closed List field type
     * Sets Closed List possible values and sets 1st element as default
     */
    private TaCoKitElementParameter visitClosedList(final PropertyNode node) {
        TaCoKitElementParameter parameter = createParameter(node);
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
     * Creates {@link TaCoKitElementParameter} for Table field type
     * Sets special fields specific for Table parameter
     * Based on schema field controls whether table toolbox (buttons under table) is shown
     */
    private TaCoKitElementParameter visitTable(final TablePropertyNode tableNode) {
        TaCoKitElementParameter parameter = createParameter(tableNode);

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

    private TaCoKitElementParameter visitSchema(final PropertyNode node) {
        TaCoKitElementParameter schema = new TaCoKitElementParameter(getNode());
        String connectionType = node.getProperty().getMetadata().get(UI_STRUCTURE_TYPE);
        String connectionName = node.getProperty().getMetadata().get(UI_STRUCTURE_VALUE);
        connectionName = connectionName.equals("__default__") ? EConnectionType.FLOW_MAIN.getName() : connectionName;
        schema.setName(connectionType + "$$" + node.getId());
        schema.setDisplayName("!!!SCHEMA.NAME!!!");
        schema.setCategory(EComponentCategory.BASIC);
        schema.setFieldType(EParameterFieldType.SCHEMA_TYPE);
        schema.setNumRow(1);
        schema.setShow(true);
        schema.setReadOnly(false);
        schema.setRequired(true);
        schema.setContext(connectionName);

        // add child parameters
        // defines whether schema is built-in or repository
        TaCoKitElementParameter childParameter1 = new TaCoKitElementParameter(getNode());
        childParameter1.setCategory(EComponentCategory.BASIC);
        childParameter1.setContext(connectionName);
        childParameter1.setDisplayName("Schema");
        childParameter1.setFieldType(EParameterFieldType.TECHNICAL);
        childParameter1.setListItemsDisplayCodeName(new String[] { "BUILT_IN", "REPOSITORY" });
        childParameter1.setListItemsDisplayName(new String[] { "Built-In", "Repository" });
        childParameter1.setListItemsValue(new String[] { "BUILT_IN", "REPOSITORY" });
        childParameter1.setName(EParameterName.SCHEMA_TYPE.getName());
        childParameter1.setNumRow(1);
        childParameter1.setParentParameter(schema);
        childParameter1.setShow(true);
        childParameter1.setShowIf("SCHEMA =='REPOSITORY'");
        childParameter1.setValue("BUILT_IN");
        schema.getChildParameters().put(EParameterName.SCHEMA_TYPE.getName(), childParameter1);

        TaCoKitElementParameter childParameter2 = new TaCoKitElementParameter(getNode());
        childParameter2.setCategory(EComponentCategory.BASIC);
        childParameter2.setContext(connectionName);
        childParameter2.setDisplayName("Repository");
        childParameter2.setFieldType(EParameterFieldType.TECHNICAL);
        childParameter2.setListItemsDisplayName(new String[0]);
        childParameter2.setListItemsValue(new String[0]);
        childParameter2.setName(EParameterName.REPOSITORY_SCHEMA_TYPE.getName());
        childParameter2.setParentParameter(schema);
        childParameter2.setRequired(true);
        childParameter2.setShow(false);
        childParameter2.setValue("");
        schema.getChildParameters().put(EParameterName.REPOSITORY_SCHEMA_TYPE.getName(), childParameter2);

        return schema;
    }

    /**
     * Creates {@link TaCoKitElementParameter} and sets common state for different types of parameters
     * 
     * @param node Property tree node
     * @return created {@link TaCoKitElementParameter}
     */
    protected TaCoKitElementParameter createParameter(final PropertyNode node) {
        TaCoKitElementParameter parameter = new TaCoKitElementParameter(iNode);
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
        if (node.getProperty().hasCondition()) {
            createParameterActivator(node, parameter);
        }
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
        SettingsCreator creator = new SettingsCreator(new FakeElement("table"), category, redrawParameter);
        columns.forEach(column -> creator.visit(column));
        return creator.getSettings();
    }

    private void createParameterActivator(final PropertyNode node, final ElementParameter parameter) {
        String[] conditionValues = node.getProperty().getConditionValues();
        ParameterActivator activator = new ParameterActivator(conditionValues, parameter);
        String targetPath = computeTargetPath(node);
        if (!activators.containsKey(targetPath)) {
            activators.put(targetPath, new ArrayList<ParameterActivator>());
        }
        activators.get(targetPath).add(activator);
    }

    /**
     * Computes target path of {@link TaCoKitElementParameter}, which should be listened by Listener
     * 
     * @return target path
     */
    private String computeTargetPath(final PropertyNode node) {
        String currentPath = node.getParentId();
        LinkedList<String> path = new LinkedList<>();
        path.addAll(Arrays.asList(currentPath.split("\\" + DOT_PATH_SEPARATOR)));
        List<String> relativePath = Arrays.asList(node.getProperty().getConditionTarget().split(PATH_SEPARATOR));
        for (String s : relativePath) {
            if (PARENT_NODE.equals(s)) {
                path.removeLast();
            } else {
                path.addLast(s);
            }
        }
        return path.stream().collect(Collectors.joining(DOT_PATH_SEPARATOR));
    }
}
