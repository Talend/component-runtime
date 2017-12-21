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
import java.util.List;

import org.talend.core.CorePlugin;
import org.talend.core.PluginChecker;
import org.talend.core.model.components.ComponentCategory;
import org.talend.core.model.components.IComponent;
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
import org.talend.sdk.component.studio.ComponentModel;

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

    /**
     * This parameter is checked during refresh to decide whether UI should be redrawn
     * If UI should be redrawn, when some Parameter is changed, such Parameter should set this
     * Parameter to {@code true}
     */
    private ElementParameter updateComponentsParameter;

    public ElementParameterCreator(final ComponentModel component, final ComponentDetail detail, final INode node) {
        this.component = component;
        this.detail = detail;
        this.node = node;
    }

    public List<? extends IElementParameter> createParameters() {
        addCommonParameters();
        addComponentParameters();
        return parameters;
    }

    private void addComponentParameters() {
        if (!detail.getProperties().isEmpty()) {
            Collection<PropertyDefinitionDecorator> properties =
                    PropertyDefinitionDecorator.wrap(detail.getProperties());
            PropertyNode root = PropertyNodeUtils.createPropertyTree(properties);
            // add main parameters
            SettingsCreator mainCreator =
                    new SchemaSettingsCreator(node, EComponentCategory.BASIC, updateComponentsParameter);
            root.accept(mainCreator, Metadatas.MAIN_FORM);
            parameters.addAll(mainCreator.getSettings());
            // add advanced parameters
            SettingsCreator advancedCreator =
                    new SettingsCreator(node, EComponentCategory.ADVANCED, updateComponentsParameter);
            root.accept(advancedCreator, Metadatas.ADVANCED_FORM);
            parameters.addAll(advancedCreator.getSettings());
        }
    }

    /**
     * Creates and adds {@link ElementParameter} common for all components
     */
    private void addCommonParameters() {
        addUniqueNameParameter();
        addComponentNameParameter();
        addVersionParameter();
        addFamilyParameter();
        addStartParameter();
        addStartableParameter();
        addSubtreeStartParameter();
        addEndOfFlowParameter();
        addActivateParameter();
        addHelpParameter();
        addUpdateComponentsParameter();
        addIReportPathParameter();
        addPropertyParameter();
        addStatCatcherParameter();
        // following parameters are added only in TIS
        addParallelizeParameter();
        addParallelizeNumberParameter();
        addParallelizeKeepEmptyParameter();
    }

    /**
     * Creates and adds {@link EParameterName#FAMILY} parameter
     * This parameter is used during code generation to know which component runtime to use
     */
    private void addFamilyParameter() {
        ElementParameter parameter = new ElementParameter(node);
        parameter.setName(EParameterName.FAMILY.getName());
        parameter.setValue(detail.getId().getFamily());
        parameter.setDisplayName(EParameterName.FAMILY.getDisplayName());
        parameter.setFieldType(EParameterFieldType.TEXT);
        parameter.setCategory(EComponentCategory.TECHNICAL);
        parameter.setNumRow(3);
        parameter.setReadOnly(true);
        parameter.setRequired(false);
        parameter.setShow(false);
        parameters.add(parameter);
    }

    /**
     * Creates and adds {@link EParameterName#START} parameter
     * See TUP-4142
     */
    private void addStartParameter() {
        if (CAN_START) {
            ElementParameter parameter = new ElementParameter(node);
            parameter.setName(EParameterName.START.getName());
            parameter.setValue(false);
            parameter.setDisplayName(EParameterName.START.getDisplayName());
            parameter.setFieldType(EParameterFieldType.CHECK);
            parameter.setCategory(EComponentCategory.TECHNICAL);
            parameter.setNumRow(5);
            parameter.setReadOnly(true);
            parameter.setRequired(false);
            parameter.setShow(false);
            parameters.add(parameter);
        }
    }

    /**
     * Creates and adds {@link EParameterName#STARTABLE} parameter
     * See TUP-4142
     */
    private void addStartableParameter() {
        ElementParameter parameter = new ElementParameter(node);
        parameter.setName(EParameterName.STARTABLE.getName());
        parameter.setValue(CAN_START);
        parameter.setDisplayName(EParameterName.STARTABLE.getDisplayName());
        parameter.setFieldType(EParameterFieldType.CHECK);
        parameter.setCategory(EComponentCategory.TECHNICAL);
        parameter.setNumRow(5);
        parameter.setReadOnly(true);
        parameter.setRequired(false);
        parameter.setShow(false);
        parameters.add(parameter);
    }

    /**
     * Creates and adds {@link EParameterName#SUBTREE_START} parameter
     * See TUP-4142
     */
    private void addSubtreeStartParameter() {
        ElementParameter parameter = new ElementParameter(node);
        parameter.setName(EParameterName.SUBTREE_START.getName());
        parameter.setValue(CAN_START);
        parameter.setDisplayName(EParameterName.SUBTREE_START.getDisplayName());
        parameter.setFieldType(EParameterFieldType.CHECK);
        parameter.setCategory(EComponentCategory.TECHNICAL);
        parameter.setNumRow(5);
        parameter.setReadOnly(true);
        parameter.setRequired(false);
        parameter.setShow(false);
        parameters.add(parameter);
    }

    /**
     * Creates and adds {@link EParameterName#END_OF_FLOW} parameter
     * See TUP-4142
     */
    private void addEndOfFlowParameter() {
        ElementParameter parameter = new ElementParameter(node);
        parameter.setName(EParameterName.END_OF_FLOW.getName());
        parameter.setValue(CAN_START);
        parameter.setDisplayName(EParameterName.END_OF_FLOW.getDisplayName());
        parameter.setFieldType(EParameterFieldType.CHECK);
        parameter.setCategory(EComponentCategory.TECHNICAL);
        parameter.setNumRow(5);
        parameter.setReadOnly(true);
        parameter.setRequired(false);
        parameter.setShow(false);
        parameters.add(parameter);
    }

    /**
     * Creates and adds {@link EParameterName#ACTIVATE} parameter
     * See TUP-4142
     */
    private void addActivateParameter() {
        ElementParameter parameter = new ElementParameter(node);
        parameter.setName(EParameterName.ACTIVATE.getName());
        parameter.setValue(true);
        parameter.setDisplayName(EParameterName.ACTIVATE.getDisplayName());
        parameter.setFieldType(EParameterFieldType.CHECK);
        parameter.setCategory(EComponentCategory.TECHNICAL);
        parameter.setNumRow(5);
        parameter.setReadOnly(false);
        parameter.setRequired(false);
        parameter.setDefaultValue(parameter.getValue());
        parameter.setShow(true);
        parameters.add(parameter);
    }

    /**
     * Creates and adds {@link EParameterName#HELP} parameter
     * See TUP-4143
     */
    private void addHelpParameter() {
        ElementParameter parameter = new ElementParameter(node);
        parameter.setName(EParameterName.HELP.getName());
        parameter.setValue(IComponent.PROP_HELP);
        parameter.setDisplayName(EParameterName.HELP.getDisplayName());
        parameter.setFieldType(EParameterFieldType.TEXT);
        parameter.setCategory(EComponentCategory.TECHNICAL);
        parameter.setNumRow(6);
        parameter.setReadOnly(true);
        parameter.setRequired(false);
        parameter.setShow(false);
        parameters.add(parameter);
    }

    /**
     * Creates and adds {@link EParameterName#UPDATE_COMPONENTS} parameter
     * This parameter is used to decide whether UI should be redrawn during Composite refresh
     */
    private void addUpdateComponentsParameter() {
        ElementParameter parameter = new ElementParameter(node);
        parameter.setName(EParameterName.UPDATE_COMPONENTS.getName());
        parameter.setValue(false);
        parameter.setDisplayName(EParameterName.UPDATE_COMPONENTS.getDisplayName());
        parameter.setFieldType(EParameterFieldType.CHECK);
        parameter.setCategory(EComponentCategory.TECHNICAL);
        parameter.setNumRow(5);
        parameter.setReadOnly(true);
        parameter.setRequired(false);
        parameter.setShow(false);
        parameters.add(parameter);
        updateComponentsParameter = parameter;
    }

    /**
     * Creates and adds {@link EParameterName#IREPORT_PATH} parameter
     */
    private void addIReportPathParameter() {
        ElementParameter parameter = new ElementParameter(node);
        parameter.setName(EParameterName.IREPORT_PATH.getName());
        parameter.setCategory(EComponentCategory.TECHNICAL);
        parameter.setFieldType(EParameterFieldType.DIRECTORY);
        parameter.setDisplayName(EParameterName.IREPORT_PATH.getDisplayName());
        parameter.setNumRow(99);
        parameter.setShow(false);
        parameter.setValue(
                CorePlugin.getDefault().getPluginPreferences().getString(ITalendCorePrefConstants.IREPORT_PATH));
        parameter.setReadOnly(true);
        parameters.add(parameter);
    }

    /**
     * Creates and adds "PROPERTY" parameter
     * Looks like this is a property switch between repository properties and this component properties
     * TODO move it to Component parameters
     */
    private void addPropertyParameter() {
        ElementParameter parameter = new ElementParameter(node);
        parameter.setName("PROPERTY");
        parameter.setCategory(EComponentCategory.BASIC);
        parameter.setDisplayName(EParameterName.PROPERTY_TYPE.getDisplayName());
        parameter.setFieldType(EParameterFieldType.PROPERTY_TYPE);
        // TODO
        // if (wizardDefinition != null) {
        // param.setRepositoryValue(wizardDefinition.getName());
        // }
        parameter.setValue("");
        parameter.setNumRow(1);
        // TODO
        // param.setShow(wizardDefinition != null);
        parameter.setShow(false);
        // param.setTaggedValue(IGenericConstants.IS_PROPERTY_SHOW, wizardDefinition !=
        // null);
        parameter.setTaggedValue("IS_PROPERTY_SHOW", false);

        ElementParameter child1 = new ElementParameter(node);
        child1.setCategory(EComponentCategory.BASIC);
        child1.setName(EParameterName.PROPERTY_TYPE.getName());
        child1.setDisplayName(EParameterName.PROPERTY_TYPE.getDisplayName());
        child1.setListItemsDisplayName(
                new String[] { AbstractBasicComponent.TEXT_BUILTIN, AbstractBasicComponent.TEXT_REPOSITORY });
        child1.setListItemsDisplayCodeName(
                new String[] { AbstractBasicComponent.BUILTIN, AbstractBasicComponent.REPOSITORY });
        child1.setListItemsValue(new String[] { AbstractBasicComponent.BUILTIN, AbstractBasicComponent.REPOSITORY });
        child1.setValue(AbstractBasicComponent.BUILTIN);
        child1.setNumRow(parameter.getNumRow());
        child1.setFieldType(EParameterFieldType.TECHNICAL);
        child1.setShow(false);
        child1.setShowIf(parameter.getName() + " =='" + AbstractBasicComponent.REPOSITORY + "'");
        child1.setReadOnly(parameter.isReadOnly());
        child1.setNotShowIf(parameter.getNotShowIf());
        child1.setContext("FLOW");
        child1.setSerialized(true);
        child1.setParentParameter(parameter);

        ElementParameter child2 = new ElementParameter(node);
        child2.setCategory(EComponentCategory.BASIC);
        child2.setName(EParameterName.REPOSITORY_PROPERTY_TYPE.getName());
        child2.setDisplayName(EParameterName.REPOSITORY_PROPERTY_TYPE.getDisplayName());
        child2.setListItemsDisplayName(new String[] {});
        child2.setListItemsValue(new String[] {});
        child2.setNumRow(parameter.getNumRow());
        child2.setFieldType(EParameterFieldType.TECHNICAL);
        child2.setValue("");
        child2.setShow(false);
        child2.setRequired(true);
        child2.setReadOnly(parameter.isReadOnly());
        child2.setShowIf(parameter.getName() + " =='" + AbstractBasicComponent.REPOSITORY + "'");
        child2.setNotShowIf(parameter.getNotShowIf());
        child2.setContext("FLOW");
        child2.setSerialized(true);
        child2.setParentParameter(parameter);

        parameters.add(parameter);
    }

    /**
     * Creates and adds {@link EParameterName#TSTATCATCHER_STATS} parameter
     */
    private void addStatCatcherParameter() {
        if (ComponentCategory.CATEGORY_4_DI.getName().equals(component.getPaletteType())) {
            boolean isCatcherAvailable = ComponentsFactoryProvider.getInstance().get(EmfComponent.TSTATCATCHER_NAME,
                    ComponentCategory.CATEGORY_4_DI.getName()) != null;
            ElementParameter parameter = new ElementParameter(node);
            parameter.setName(EParameterName.TSTATCATCHER_STATS.getName());
            parameter.setValue(Boolean.FALSE);
            parameter.setDisplayName(EParameterName.TSTATCATCHER_STATS.getDisplayName());
            parameter.setFieldType(EParameterFieldType.CHECK);
            parameter.setCategory(EComponentCategory.ADVANCED);
            parameter.setNumRow(199);
            parameter.setReadOnly(false);
            parameter.setRequired(false);
            parameter.setDefaultValue(parameter.getValue());
            parameter.setShow(isCatcherAvailable);
            parameters.add(parameter);
        }
    }

    /**
     * Creates and adds {@link EParameterName#PARALLELIZE} parameter
     * See TESB-4279
     */
    private void addParallelizeParameter() {
        if (isCamelCategory()) {
            ElementParameter parameter = new ElementParameter(node);
            parameter.setReadOnly(true);
            parameter.setName(EParameterName.PARALLELIZE.getName());
            parameter.setValue(Boolean.FALSE);
            parameter.setDisplayName(EParameterName.PARALLELIZE.getDisplayName());
            parameter.setFieldType(EParameterFieldType.CHECK);
            parameter.setCategory(EComponentCategory.ADVANCED);
            parameter.setNumRow(200);
            parameter.setShow(true);
            parameter.setDefaultValue(parameter.getValue());
            parameters.add(parameter);
        }
    }

    /**
     * Creates and adds {@link EParameterName#PARALLELIZE_NUMBER} parameter
     * See TESB-4279
     */
    private void addParallelizeNumberParameter() {
        if (isCamelCategory()) {
            ElementParameter parameter = new ElementParameter(node);
            parameter.setReadOnly(true);
            parameter.setName(EParameterName.PARALLELIZE_NUMBER.getName());
            parameter.setValue(2);
            parameter.setDisplayName(EParameterName.PARALLELIZE_NUMBER.getDisplayName());
            parameter.setFieldType(EParameterFieldType.TEXT);
            parameter.setCategory(EComponentCategory.ADVANCED);
            parameter.setNumRow(200);
            parameter.setShowIf(EParameterName.PARALLELIZE.getName() + " == 'true'");
            parameter.setDefaultValue(parameter.getValue());
            parameters.add(parameter);
        }
    }

    /**
     * Creates and adds {@link EParameterName#PARALLELIZE_KEEP_EMPTY} parameter
     * See TESB-4279
     */
    private void addParallelizeKeepEmptyParameter() {
        if (isCamelCategory()) {
            ElementParameter parameter = new ElementParameter(node);
            parameter.setReadOnly(true);
            parameter.setName(EParameterName.PARALLELIZE_KEEP_EMPTY.getName());
            parameter.setValue(Boolean.FALSE);
            parameter.setDisplayName(EParameterName.PARALLELIZE_KEEP_EMPTY.getDisplayName());
            parameter.setFieldType(EParameterFieldType.CHECK);
            parameter.setCategory(EComponentCategory.ADVANCED);
            parameter.setNumRow(200);
            parameter.setShow(false);
            parameter.setDefaultValue(parameter.getValue());
            parameters.add(parameter);
        }
    }

    private boolean isCamelCategory() {
        return PluginChecker.isTeamEdition()
                && !ComponentCategory.CATEGORY_4_CAMEL.getName().equals(component.getPaletteType());
    }

    /**
     * Creates and adds {@link EParameterName#UNIQUE_NAME} parameter
     * This parameter stores unique id of component instance in current job/process
     * It's value is like following "tJiraInput_1", "tJiraInput_2"
     * Value is computed later and can't be computed here as {@link ComponentModel}
     * doesn't now how many component instances were created before
     */
    private void addUniqueNameParameter() {
        ElementParameter parameter = new ElementParameter(node);
        parameter.setName(EParameterName.UNIQUE_NAME.getName());
        parameter.setValue("");
        parameter.setDisplayName(EParameterName.UNIQUE_NAME.getDisplayName());
        parameter.setFieldType(EParameterFieldType.TEXT);
        parameter.setCategory(EComponentCategory.ADVANCED);
        parameter.setNumRow(1);
        parameter.setReadOnly(true);
        parameter.setShow(false);
        parameters.add(parameter);
    }

    /**
     * Creates and adds {@link EParameterName#COMPONENT_NAME} parameter
     * It is used in code generation to know which component to load for runtime
     */
    private void addComponentNameParameter() {
        ElementParameter parameter = new ElementParameter(node);
        parameter.setName(EParameterName.COMPONENT_NAME.getName());
        parameter.setValue(detail.getId().getName());
        parameter.setDisplayName(EParameterName.COMPONENT_NAME.getDisplayName());
        parameter.setFieldType(EParameterFieldType.TEXT);
        parameter.setCategory(EComponentCategory.TECHNICAL);
        parameter.setNumRow(1);
        parameter.setReadOnly(true);
        parameter.setShow(false);
        parameters.add(parameter);
    }

    /**
     * Creates and adds {@link EParameterName#VERSION} parameter
     * Its value is component version. More specifically it is a version of component configuration.
     * It is used for migration to check whether serialized configuration
     * has the same version as component in Studio. If version is smaller than component in Studio,
     * migration is launched.
     */
    private void addVersionParameter() {
        ElementParameter parameter = new ElementParameter(node);
        parameter.setName(EParameterName.VERSION.getName());
        parameter.setValue(String.valueOf(detail.getVersion()));
        parameter.setDisplayName(EParameterName.VERSION.getDisplayName());
        parameter.setFieldType(EParameterFieldType.TEXT);
        parameter.setCategory(EComponentCategory.TECHNICAL);
        parameter.setNumRow(1);
        parameter.setReadOnly(true);
        parameter.setShow(false);
        parameters.add(parameter);
    }

}
