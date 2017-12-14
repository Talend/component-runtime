/**
 * Copyright (C) 2006-2017 Talend Inc. - www.talend.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.talend.sdk.component.studio.ui.wizard.page;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.talend.core.model.process.EComponentCategory;
import org.talend.core.model.process.Element;
import org.talend.designer.core.generic.constants.IElementParameterEventProperties;
import org.talend.designer.core.model.FakeElement;
import org.talend.designer.core.model.components.DummyComponent;
import org.talend.designer.core.model.components.ElementParameter;
import org.talend.designer.core.model.process.DataNode;
import org.talend.sdk.component.server.front.model.ConfigTypeNode;
import org.talend.sdk.component.studio.i18n.Messages;
import org.talend.sdk.component.studio.metadata.TaCoKitElementParameter;
import org.talend.sdk.component.studio.metadata.TaCoKitElementParameter.IValueChangedListener;
import org.talend.sdk.component.studio.metadata.model.TaCoKitConfigurationItemModel;
import org.talend.sdk.component.studio.metadata.model.TaCoKitConfigurationModel;
import org.talend.sdk.component.studio.model.parameter.PropertyNode;
import org.talend.sdk.component.studio.model.parameter.PropertyNodeUtils;
import org.talend.sdk.component.studio.model.parameter.SettingsCreator;
import org.talend.sdk.component.studio.ui.composite.TaCoKitWizardComposite;
import org.talend.sdk.component.studio.ui.wizard.TaCoKitConfigurationRuntimeData;

/**
 * DOC cmeng class global comment. Detailled comment
 */
public class TaCoKitConfigurationWizardPage extends AbsTaCoKitWizardPage implements PropertyChangeListener {

    private Element element;

    private TaCoKitWizardComposite tacokitComposite;

    private TaCoKitConfigurationModel configurationModel;

    private IStatus tocokitConfigStatus;

    public TaCoKitConfigurationWizardPage(final TaCoKitConfigurationRuntimeData runtimeData) {
        super(Messages.getString("WizardPage.TaCoKitConfiguration"), runtimeData); //$NON-NLS-1$
        ConfigTypeNode configTypeNode = runtimeData.getConfigTypeNode();
        setTitle(Messages.getString("TaCoKitConfiguration.wizard.title", configTypeNode.getConfigurationType(), //$NON-NLS-1$
                configTypeNode.getDisplayName()));
        setDescription(Messages.getString("TaCoKitConfiguration.wizard.description.edit", //$NON-NLS-1$
                configTypeNode.getConfigurationType(), configTypeNode.getDisplayName()));
    }

    @Override
    public void createControl(final Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        container.setLayoutData(new GridData(GridData.FILL_BOTH));
        container.setLayout(new FormLayout());
        setControl(container);

        TaCoKitConfigurationRuntimeData runtimeData = getTaCoKitConfigurationRuntimeData();
        TaCoKitConfigurationItemModel itemModel = new TaCoKitConfigurationItemModel(runtimeData.getConnectionItem());
        configurationModel = itemModel.getConfigurationModel();
        boolean addContextFields = runtimeData.isAddContextFields();

        ConfigTypeNode configTypeNode = runtimeData.getConfigTypeNode();
        DummyComponent component = new DummyComponent(configTypeNode.getDisplayName());
        final DataNode node = new DataNode(component, component.getName());
        PropertyNode root = PropertyNodeUtils.createPropertyTree(configTypeNode);
        SettingsCreator settingsCreator = new SettingsCreator(node, EComponentCategory.BASIC);
        root.accept(settingsCreator);
        List<ElementParameter> parameters = settingsCreator.getSettings();

        element = new FakeElement(runtimeData.getTaCoKitRepositoryNode().getConfigTypeNode().getDisplayName());
        element.setReadOnly(runtimeData.isReadonly());
        element.setElementParameters(parameters);

        tacokitComposite = new TaCoKitWizardComposite(container, SWT.H_SCROLL | SWT.V_SCROLL | SWT.NO_FOCUS,
                EComponentCategory.BASIC, element, configurationModel, true, container.getBackground());
        tacokitComposite.setLayoutData(createMainFormData(addContextFields));
        tacokitComposite.addPropertyChangeListener(this);
        tacokitComposite.addValueChangedListener(new IValueChangedListener() {

            @Override
            public void onValueChanged(final TaCoKitElementParameter elementParameter, final Object oldValue,
                    final Object newValue) {
                configurationModel.setValue(elementParameter.getName(), newValue);
            }
        });

        if (addContextFields) {
            // Composite contextParentComp = new Composite(container, SWT.NONE);
            // contextParentComp.setLayoutData(createFooterFormData(tacokitComposite));
            // contextParentComp.setLayout(new GridLayout());
            // ContextComposite contextComp = addContextFields(contextParentComp);
            // contextComp.addPropertyChangeListener(tacokitComposite);
            // contextComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        }
    }

    private FormData createMainFormData(final boolean addContextSupport) {
        FormData data = new FormData();
        data.left = new FormAttachment(0, 0);
        data.right = new FormAttachment(100, 0);
        data.top = new FormAttachment(0, 0);
        if (addContextSupport) {
            data.bottom = new FormAttachment(85, 0);
        } else {
            data.bottom = new FormAttachment(100, 0);
        }
        return data;
    }

    @Override
    protected IStatus[] getStatuses() {
        return Arrays.asList(super.getStatuses(), tocokitConfigStatus).toArray(new IStatus[0]);
    }

    @Override
    public void propertyChange(final PropertyChangeEvent evt) {
        String propertyName = evt.getPropertyName();
        if (IElementParameterEventProperties.EVENT_PROPERTY_NAME_CHANGED.equals(propertyName)) {
            String newPropertyName = String.valueOf(evt.getNewValue());
            updateProperty(newPropertyName);
        }
    }

    private void updateProperty(final String newPropertyName) {
        // TODO
    }

}
