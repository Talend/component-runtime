/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
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
import org.talend.core.model.process.IConnectionCategory;
import org.talend.core.model.process.IElement;
import org.talend.core.model.process.IElementParameter;
import org.talend.core.model.process.INodeConnector;
import org.talend.designer.core.model.FakeElement;
import org.talend.designer.core.model.components.EParameterName;
import org.talend.designer.core.model.components.ElementParameter;
import org.talend.designer.core.ui.editor.nodes.Node;
import org.talend.sdk.component.server.front.model.PropertyValidation;
import org.talend.sdk.component.studio.model.parameter.listener.ParameterActivator;

/**
 * Creates properties from leafs
 */
public class SettingsCreator implements PropertyVisitor {

    /**
     * Specifies row number, on which schema properties (schema widget and guess schema button) should be displayed
     * On the 1st row Repository switch widget is located
     */
    private static final int SCHEMA_ROW_NUMBER = 2;

    /**
     * Stores created component parameters.
     * Key is parameter name (which is also its path)
     */
    private final Map<String, IElementParameter> settings = new LinkedHashMap<>();

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
     * Stores created {@link ParameterActivator} for further registering them into corresponding
     * {@link ElementParameter}
     */
    private final Map<String, List<ParameterActivator>> activators = new HashMap<>();

    /**
     * {@link TaCoKitElementParameter} has numRow field which stores widget relative position (row number on which it
     * appears)
     * If numRow = 10, this does not necessarily mean that widget will be shown on 10th line,
     * but when 1 parameter has numRow = 8, and 2 has numRow = 10, then 2 will shown under 1
     * It is initialized to 3, because 1 row is for repository value switch widget and 2 is for schema
     */
    private int lastRowNumber = 3;

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
    public List<IElementParameter> getSettings() {

        activators.forEach((path, activators) -> {
            IElementParameter targetParameter = settings.get(path);
            if (TaCoKitElementParameter.class.isInstance(targetParameter)) {
                final TaCoKitElementParameter param = TaCoKitElementParameter.class.cast(targetParameter);
                param.setRedrawParameter(redrawParameter);
                activators.forEach(activator -> {
                    param.registerListener(EVENT_PROPERTY_VALUE_CHANGED, activator);
                    initVisibility(param, activator);
                });
            }
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
     */
    @Override
    public void visit(final PropertyNode node) {
        if (node.isLeaf()) {
            switch (node.getFieldType()) {
            case CHECK:
                CheckElementParameter check = visitCheck(node);
                settings.put(check.getName(), check);
                break;
            case CLOSED_LIST:
                TaCoKitElementParameter closedList = visitClosedList(node);
                settings.put(closedList.getName(), closedList);
                break;
            case TABLE:
                TaCoKitElementParameter table = visitTable((TablePropertyNode) node);
                settings.put(table.getName(), table);
                break;
            case SCHEMA_TYPE:
                TaCoKitElementParameter schema = visitSchema(node);
                settings.put(schema.getName(), schema);
                break;
            default:
                final IElementParameter text;
                if (node.getProperty().getPlaceholder() == null) {
                    text = new TaCoKitElementParameter(iNode);
                } else {
                    final TextElementParameter advancedText = new TextElementParameter(iNode);
                    advancedText.setMessage(node.getProperty().getPlaceholder());
                    text = advancedText;
                }
                commonSetup(text, node);
                settings.put(text.getName(), text);

                break;
            }

        }
    }

    IElement getNode() {
        return this.iNode;
    }

    /**
     * Creates {@link TaCoKitElementParameter} for Check field type
     * Converts default value from String to Boolean and sets it
     */
    private CheckElementParameter visitCheck(final PropertyNode node) {
        CheckElementParameter parameter = new CheckElementParameter(iNode);
        commonSetup(parameter, node);
        return parameter;
    }

    /**
     * Creates {@link TaCoKitElementParameter} for Closed List field type
     * Sets Closed List possible values and sets 1st element as default
     */
    private TaCoKitElementParameter visitClosedList(final PropertyNode node) {
        TaCoKitElementParameter parameter = new TaCoKitElementParameter(iNode);
        commonSetup(parameter, node);
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
        TaCoKitElementParameter parameter = createTableParameter(tableNode);

        List<IElementParameter> tableParameters = createTableParameters(tableNode);
        List<String> codeNames = new ArrayList<>(tableParameters.size());
        List<String> displayNames = new ArrayList<>(tableParameters.size());
        for (IElementParameter param : tableParameters) {
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
        String connectionType = node.getProperty().getMetadata().get(UI_STRUCTURE_TYPE);
        String connectionName = node.getProperty().getMetadata().get(UI_STRUCTURE_VALUE);
        connectionName = connectionName.equals("__default__") ? EConnectionType.FLOW_MAIN.getName() : connectionName;
        String schemaName = connectionType + "$$" + node.getId();
        return createSchemaParameter(connectionName, schemaName);
    }

    protected TaCoKitElementParameter createSchemaParameter(final String connectionName, final String schemaName) {
        // Maybe need to find some other condition. this way we will show schema widget for main flow only.
        boolean show = EConnectionType.FLOW_MAIN.getName().equalsIgnoreCase(connectionName);
        TaCoKitElementParameter schema = new TaCoKitElementParameter(getNode());
        schema.setName(schemaName);
        schema.setDisplayName("!!!SCHEMA.NAME!!!");
        schema.setCategory(EComponentCategory.BASIC);
        schema.setFieldType(EParameterFieldType.SCHEMA_TYPE);
        schema.setNumRow(SCHEMA_ROW_NUMBER);
        schema.setShow(show);
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
        childParameter1.setShow(show);
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
        childParameter2.setShow(show);
        childParameter2.setValue("");
        schema.getChildParameters().put(EParameterName.REPOSITORY_SCHEMA_TYPE.getName(), childParameter2);

        if (canAddGuessSchema()) {
            String tacokitGuessSchema = "Guess Schema";
            TaCoKitElementParameter guessSchemaParameter = new TaCoKitElementParameter(getNode());
            guessSchemaParameter.setCategory(EComponentCategory.BASIC);
            guessSchemaParameter.setContext(EConnectionType.FLOW_MAIN.getName());
            guessSchemaParameter.setDisplayName(tacokitGuessSchema);
            guessSchemaParameter.setFieldType(EParameterFieldType.TACOKIT_GUESS_SCHEMA);
            guessSchemaParameter.setListItemsDisplayName(new String[0]);
            guessSchemaParameter.setListItemsValue(new String[0]);
            guessSchemaParameter.setName(tacokitGuessSchema);
            guessSchemaParameter.setNumRow(SCHEMA_ROW_NUMBER);
            guessSchemaParameter.setParentParameter(schema);
            guessSchemaParameter.setReadOnly(false);
            guessSchemaParameter.setRequired(false);
            guessSchemaParameter.setShow(show);
            guessSchemaParameter.setValue("");
            guessSchemaParameter.getChildParameters().put(tacokitGuessSchema, guessSchemaParameter);
        }

        return schema;
    }

    /**
     * Creates {@link TableElementParameter} and sets common state
     *
     * @param node Property tree node
     * @return created {@link TableElementParameter}
     */
    private TableElementParameter createTableParameter(final PropertyNode node) {
        TableElementParameter parameter = new TableElementParameter(iNode);
        commonSetup(parameter, node);
        return parameter;
    }

    /**
     * Setup common for all {@link TaCoKitElementParameter} fields
     *
     * @param parameter parameter to setup
     * @param node property tree node
     */
    private void commonSetup(final IElementParameter parameter, final PropertyNode node) {
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
    }

    /**
     * Creates table parameters (columns) for Table property
     *
     * @param tableNode {@link TablePropertyNode}
     * @return list of table parameters
     */
    private List<IElementParameter> createTableParameters(final TablePropertyNode tableNode) {
        List<PropertyNode> columns = tableNode.getColumns();
        SettingsCreator creator = new SettingsCreator(new FakeElement("table"), category, redrawParameter);
        columns.forEach(creator::visit);
        return creator.getSettings();
    }

    private void createParameterActivator(final PropertyNode node, final IElementParameter parameter) {
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

    private boolean canAddGuessSchema() {
        boolean canAddGuessSchema = false;
        IElement node = getNode();
        if (node instanceof Node) {
            boolean hasIncommingConnection = false;
            List<? extends INodeConnector> listConnector = ((Node) node).getListConnector();
            if (listConnector != null) {
                for (INodeConnector connector : listConnector) {
                    EConnectionType connectionType = connector.getDefaultConnectionType();
                    if (connectionType != null && connectionType.hasConnectionCategory(IConnectionCategory.FLOW)
                            && 0 < connector.getMaxLinkInput()) {
                        hasIncommingConnection = true;
                        break;
                    }
                }
            }
            canAddGuessSchema = !hasIncommingConnection;
        }
        return canAddGuessSchema;
    }
}
