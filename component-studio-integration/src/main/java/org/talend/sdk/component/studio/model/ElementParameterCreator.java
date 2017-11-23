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
package org.talend.sdk.component.studio.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.talend.core.CorePlugin;
import org.talend.core.PluginChecker;
import org.talend.core.model.components.ComponentCategory;
import org.talend.core.model.process.EComponentCategory;
import org.talend.core.model.process.EParameterFieldType;
import org.talend.core.model.process.IElementParameter;
import org.talend.core.model.process.INode;
import org.talend.core.prefs.ITalendCorePrefConstants;
import org.talend.core.ui.component.ComponentsFactoryProvider;
import org.talend.designer.core.model.components.AbstractBasicComponent;
import org.talend.designer.core.model.components.EParameterName;
import org.talend.designer.core.model.components.ElementParameter;
import org.talend.designer.core.model.components.EmfComponent;
import org.talend.sdk.component.server.front.model.ComponentDetail;
import org.talend.sdk.component.server.front.model.SimplePropertyDefinition;
import org.talend.sdk.component.studio.ComponentModel;
import org.talend.sdk.component.studio.model.parameter.PropertyNode;
import org.talend.sdk.component.studio.model.parameter.PropertyVisitor;
import org.talend.sdk.component.studio.model.parameter.SettingsCreator;

/**
 * Creates {@link ComponentModel} {@link ElementParameter} list
 */
public class ElementParameterCreator {

    /**
     * Flag representing whether it is startable component
     */
    private static final boolean CAN_START = true;

    private final INode node;

    private final ComponentModel component;
    
    private final ComponentDetail detail;

    private final List<ElementParameter> parameters = new ArrayList<>();

    public ElementParameterCreator(final ComponentModel component, final ComponentDetail detail, final INode node) {
        this.component = component;
        this.detail = detail;
        this.node = node;
    }
    
    public PropertyNode createPropertyTree(Collection<SimplePropertyDefinition> properties) {
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
     * Sorts siblings according that how ther should be shown on UI
     */
    public void sortPropertyRoot(PropertyNode root) {
        root.accept(new PropertyVisitor() {
            
            private static final String OPTIONS_ORDER = "ui::optionsorder::value";
            
            private static final String ORDER_SEPARATOR = ",";

            @Override
            public void visit(PropertyNode node) {
                SimplePropertyDefinition property = node.getProperty();
                String optionsOrder = property.getMetadata().get(OPTIONS_ORDER);
                if (optionsOrder != null) {
                    optionsOrderSort(node, optionsOrder);
                }
                // TODO implement sorting according GridLayout
            }
            
            /**
             * Sorts node children according order specified in OptionsOrder or GridLayout
             * 
             * @param node current node
             * @param optionsOrder metadata value for ui::optionsorder
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
             * @param optionsOrder metadata value for ui::optionsorder
             * @return order
             */
            private HashMap<String, Integer> getOrder(String optionsOrder) {
                String[] values = optionsOrder.split(ORDER_SEPARATOR); 
                HashMap<String, Integer> order = new HashMap<>();
                for (int i=0; i<values.length; i++) {
                    order.put(values[i], i);
                }
                return order;
            }
        });
    }

    public List<? extends IElementParameter> createParameters() {
        addMainParameters();
        addComponentParameters();
        return parameters;
    }
    
    private void addComponentParameters() {
        if (!detail.getProperties().isEmpty()) {
            PropertyNode root = createPropertyTree(detail.getProperties());
            sortPropertyRoot(root);
            SettingsCreator settingsCreator = new SettingsCreator(node);
            root.accept(settingsCreator);
            parameters.addAll(settingsCreator.getSettings());
        }
    }

    private void addMainParameters() {
        ElementParameter param;
        param = new ElementParameter(node);
        param.setName(EParameterName.UNIQUE_NAME.getName());
        param.setValue(""); //$NON-NLS-1$
        param.setDisplayName(EParameterName.UNIQUE_NAME.getDisplayName());
        param.setFieldType(EParameterFieldType.TEXT);
        param.setCategory(EComponentCategory.ADVANCED);
        param.setNumRow(1);
        param.setReadOnly(true);
        param.setShow(false);
        parameters.add(param);
        param = new ElementParameter(node);
        param.setName(EParameterName.COMPONENT_NAME.getName());
        param.setValue(component.getName());
        param.setDisplayName(EParameterName.COMPONENT_NAME.getDisplayName());
        param.setFieldType(EParameterFieldType.TEXT);
        param.setCategory(EComponentCategory.TECHNICAL);
        param.setNumRow(1);
        param.setReadOnly(true);
        param.setShow(false);
        parameters.add(param);
        param = new ElementParameter(node);
        param.setName(EParameterName.FAMILY.getName());
        param.setValue(component.getOriginalFamilyName());
        param.setDisplayName(EParameterName.FAMILY.getDisplayName());
        param.setFieldType(EParameterFieldType.TEXT);
        param.setCategory(EComponentCategory.TECHNICAL);
        param.setNumRow(3);
        param.setReadOnly(true);
        param.setRequired(false);
        param.setShow(false);
        parameters.add(param);
        // TUP-4142
        if (CAN_START) {
            param = new ElementParameter(node);
            param.setName(EParameterName.START.getName());
            param.setValue(false);
            param.setDisplayName(EParameterName.START.getDisplayName());
            param.setFieldType(EParameterFieldType.CHECK);
            param.setCategory(EComponentCategory.TECHNICAL);
            param.setNumRow(5);
            param.setReadOnly(true);
            param.setRequired(false);
            param.setShow(false);
            parameters.add(param);
        }
        // TUP-4142
        param = new ElementParameter(node);
        param.setName(EParameterName.STARTABLE.getName());
        param.setValue(CAN_START);
        param.setDisplayName(EParameterName.STARTABLE.getDisplayName());
        param.setFieldType(EParameterFieldType.CHECK);
        param.setCategory(EComponentCategory.TECHNICAL);
        param.setNumRow(5);
        param.setReadOnly(true);
        param.setRequired(false);
        param.setShow(false);
        parameters.add(param);
        // TUP-4142
        param = new ElementParameter(node);
        param.setName(EParameterName.SUBTREE_START.getName());
        param.setValue(CAN_START);
        param.setDisplayName(EParameterName.SUBTREE_START.getDisplayName());
        param.setFieldType(EParameterFieldType.CHECK);
        param.setCategory(EComponentCategory.TECHNICAL);
        param.setNumRow(5);
        param.setReadOnly(true);
        param.setRequired(false);
        param.setShow(false);
        parameters.add(param);
        // TUP-4142
        param = new ElementParameter(node);
        param.setName(EParameterName.END_OF_FLOW.getName());
        param.setValue(CAN_START);
        param.setDisplayName(EParameterName.END_OF_FLOW.getDisplayName());
        param.setFieldType(EParameterFieldType.CHECK);
        param.setCategory(EComponentCategory.TECHNICAL);
        param.setNumRow(5);
        param.setReadOnly(true);
        param.setRequired(false);
        param.setShow(false);
        parameters.add(param);
        param = new ElementParameter(node);
        param.setName(EParameterName.ACTIVATE.getName());
        param.setValue(true);
        param.setDisplayName(EParameterName.ACTIVATE.getDisplayName());
        param.setFieldType(EParameterFieldType.CHECK);
        param.setCategory(EComponentCategory.TECHNICAL);
        param.setNumRow(5);
        param.setReadOnly(false);
        param.setRequired(false);
        param.setDefaultValue(param.getValue());
        param.setShow(true);
        parameters.add(param);
        // TUP-4143
        param = new ElementParameter(node);
        param.setName(EParameterName.HELP.getName());
        param.setValue(component.PROP_HELP);
        param.setDisplayName(EParameterName.HELP.getDisplayName());
        param.setFieldType(EParameterFieldType.TEXT);
        param.setCategory(EComponentCategory.TECHNICAL);
        param.setNumRow(6);
        param.setReadOnly(true);
        param.setRequired(false);
        param.setShow(false);
        parameters.add(param);
        param = new ElementParameter(node);
        param.setName(EParameterName.UPDATE_COMPONENTS.getName());
        param.setValue(false);
        param.setDisplayName(EParameterName.UPDATE_COMPONENTS.getDisplayName());
        param.setFieldType(EParameterFieldType.CHECK);
        param.setCategory(EComponentCategory.TECHNICAL);
        param.setNumRow(5);
        param.setReadOnly(true);
        param.setRequired(false);
        param.setShow(false);
        parameters.add(param);
        param = new ElementParameter(node);
        param.setName(EParameterName.IREPORT_PATH.getName());
        param.setCategory(EComponentCategory.TECHNICAL);
        param.setFieldType(EParameterFieldType.DIRECTORY);
        param.setDisplayName(EParameterName.IREPORT_PATH.getDisplayName());
        param.setNumRow(99);
        param.setShow(false);
        param.setValue(CorePlugin.getDefault().getPluginPreferences().getString(ITalendCorePrefConstants.IREPORT_PATH));
        param.setReadOnly(true);
        parameters.add(param);
        param = new ElementParameter(node);
        param.setName("PROPERTY");//$NON-NLS-1$
        param.setCategory(EComponentCategory.BASIC);
        param.setDisplayName(EParameterName.PROPERTY_TYPE.getDisplayName());
        param.setFieldType(EParameterFieldType.PROPERTY_TYPE);
        // TODO
        // if (wizardDefinition != null) {
        // param.setRepositoryValue(wizardDefinition.getName());
        // }
        param.setValue("");//$NON-NLS-1$
        param.setNumRow(1);
        // TODO
        // param.setShow(wizardDefinition != null);
        param.setShow(false);
        // param.setTaggedValue(IGenericConstants.IS_PROPERTY_SHOW, wizardDefinition !=
        // null);
        param.setTaggedValue("IS_PROPERTY_SHOW", false);

        ElementParameter newParam = new ElementParameter(node);
        newParam.setCategory(EComponentCategory.BASIC);
        newParam.setName(EParameterName.PROPERTY_TYPE.getName());
        newParam.setDisplayName(EParameterName.PROPERTY_TYPE.getDisplayName());
        newParam.setListItemsDisplayName(
                new String[] { AbstractBasicComponent.TEXT_BUILTIN, AbstractBasicComponent.TEXT_REPOSITORY });
        newParam.setListItemsDisplayCodeName(
                new String[] { AbstractBasicComponent.BUILTIN, AbstractBasicComponent.REPOSITORY });
        newParam.setListItemsValue(new String[] { AbstractBasicComponent.BUILTIN, AbstractBasicComponent.REPOSITORY });
        newParam.setValue(AbstractBasicComponent.BUILTIN);
        newParam.setNumRow(param.getNumRow());
        newParam.setFieldType(EParameterFieldType.TECHNICAL);
        newParam.setShow(false);
        newParam.setShowIf(param.getName() + " =='" + AbstractBasicComponent.REPOSITORY + "'"); //$NON-NLS-1$ //$NON-NLS-2$
        newParam.setReadOnly(param.isReadOnly());
        newParam.setNotShowIf(param.getNotShowIf());
        newParam.setContext("FLOW");
        newParam.setSerialized(true);
        newParam.setParentParameter(param);

        newParam = new ElementParameter(node);
        newParam.setCategory(EComponentCategory.BASIC);
        newParam.setName(EParameterName.REPOSITORY_PROPERTY_TYPE.getName());
        newParam.setDisplayName(EParameterName.REPOSITORY_PROPERTY_TYPE.getDisplayName());
        newParam.setListItemsDisplayName(new String[] {});
        newParam.setListItemsValue(new String[] {});
        newParam.setNumRow(param.getNumRow());
        newParam.setFieldType(EParameterFieldType.TECHNICAL);
        newParam.setValue(""); //$NON-NLS-1$
        newParam.setShow(false);
        newParam.setRequired(true);
        newParam.setReadOnly(param.isReadOnly());
        newParam.setShowIf(param.getName() + " =='" + AbstractBasicComponent.REPOSITORY + "'"); //$NON-NLS-1$//$NON-NLS-2$
        newParam.setNotShowIf(param.getNotShowIf());
        newParam.setContext("FLOW");
        newParam.setSerialized(true);
        newParam.setParentParameter(param);
        parameters.add(param);

        if (ComponentCategory.CATEGORY_4_DI.getName().equals(component.getPaletteType())) {
            boolean tStatCatcherAvailable = ComponentsFactoryProvider.getInstance().get(EmfComponent.TSTATCATCHER_NAME,
                    ComponentCategory.CATEGORY_4_DI.getName()) != null;
            param = new ElementParameter(node);
            param.setName(EParameterName.TSTATCATCHER_STATS.getName());
            param.setValue(Boolean.FALSE);
            param.setDisplayName(EParameterName.TSTATCATCHER_STATS.getDisplayName());
            param.setFieldType(EParameterFieldType.CHECK);
            param.setCategory(EComponentCategory.ADVANCED);
            param.setNumRow(199);
            param.setReadOnly(false);
            param.setRequired(false);
            param.setDefaultValue(param.getValue());
            param.setShow(tStatCatcherAvailable);
            parameters.add(param);
        }

        // These parameters is only work when TIS is loaded
        // GLiu Added for Task http://jira.talendforge.org/browse/TESB-4279
        if (PluginChecker.isTeamEdition()
                && !ComponentCategory.CATEGORY_4_CAMEL.getName().equals(component.getPaletteType())) {
            param = new ElementParameter(node);
            param.setReadOnly(true);
            param.setName(EParameterName.PARALLELIZE.getName());
            param.setValue(Boolean.FALSE);
            param.setDisplayName(EParameterName.PARALLELIZE.getDisplayName());
            param.setFieldType(EParameterFieldType.CHECK);
            param.setCategory(EComponentCategory.ADVANCED);
            param.setNumRow(200);
            param.setShow(true);
            param.setDefaultValue(param.getValue());
            parameters.add(param);

            param = new ElementParameter(node);
            param.setReadOnly(true);
            param.setName(EParameterName.PARALLELIZE_NUMBER.getName());
            param.setValue(2);
            param.setDisplayName(EParameterName.PARALLELIZE_NUMBER.getDisplayName());
            param.setFieldType(EParameterFieldType.TEXT);
            param.setCategory(EComponentCategory.ADVANCED);
            param.setNumRow(200);
            param.setShowIf(EParameterName.PARALLELIZE.getName() + " == 'true'"); //$NON-NLS-1$
            param.setDefaultValue(param.getValue());
            parameters.add(param);

            param = new ElementParameter(node);
            param.setReadOnly(true);
            param.setName(EParameterName.PARALLELIZE_KEEP_EMPTY.getName());
            param.setValue(Boolean.FALSE);
            param.setDisplayName(EParameterName.PARALLELIZE_KEEP_EMPTY.getDisplayName());
            param.setFieldType(EParameterFieldType.CHECK);
            param.setCategory(EComponentCategory.ADVANCED);
            param.setNumRow(200);
            param.setShow(false);
            param.setDefaultValue(param.getValue());
            parameters.add(param);
        }
    }

}
