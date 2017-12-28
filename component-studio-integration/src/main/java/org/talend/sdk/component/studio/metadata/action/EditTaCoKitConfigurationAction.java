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
package org.talend.sdk.component.studio.metadata.action;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.PlatformUI;
import org.talend.core.model.properties.ConnectionItem;
import org.talend.core.repository.model.ProxyRepositoryFactory;
import org.talend.core.repository.ui.actions.metadata.AbstractCreateAction;
import org.talend.repository.ProjectManager;
import org.talend.repository.model.IProxyRepositoryFactory;
import org.talend.repository.model.RepositoryNode;
import org.talend.repository.ui.views.IRepositoryView;
import org.talend.sdk.component.server.front.model.ConfigTypeNode;
import org.talend.sdk.component.studio.i18n.Messages;
import org.talend.sdk.component.studio.metadata.node.ITaCoKitRepositoryNode;
import org.talend.sdk.component.studio.ui.wizard.TaCoKitConfigurationRuntimeData;
import org.talend.sdk.component.studio.ui.wizard.TaCoKitConfigurationWizard;

/**
 * DOC cmeng class global comment. Detailled comment
 */
public class EditTaCoKitConfigurationAction extends AbstractCreateAction {

    private static final int DEFAULT_WIZARD_WIDTH = 700;

    private static final int DEFAULT_WIZARD_HEIGHT = 400;

    private ITaCoKitRepositoryNode repositoryNode;

    private ConfigTypeNode configTypeNode;

    public EditTaCoKitConfigurationAction() {
        super();
    }

    @Override
    protected void doRun() {
        TaCoKitConfigurationRuntimeData runtimeData = new TaCoKitConfigurationRuntimeData();
        runtimeData.setTaCoKitRepositoryNode(repositoryNode);
        runtimeData.setConfigTypeNode(repositoryNode.getConfigTypeNode());
        runtimeData.setConnectionItem((ConnectionItem) repositoryNode.getObject().getProperty().getItem());
        runtimeData.setCreation(false);
        runtimeData.setReadonly(false);

        IWizard wizard = new TaCoKitConfigurationWizard(PlatformUI.getWorkbench(), runtimeData);
        WizardDialog wizardDialog =
                new WizardDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), wizard);
        if (Platform.getOS().equals(Platform.OS_LINUX)) {
            wizardDialog.setPageSize(getWizardWidth(), getWizardHeight() + 80);
        }
        wizardDialog.create();
        int result = wizardDialog.open();
        if (result == WizardDialog.OK) {
            IRepositoryView viewPart = getViewPart();
            if (viewPart != null) {
                viewPart.setFocus();
                refresh(repositoryNode);
            }
        }
    }

    @Override
    protected void init(final RepositoryNode node) {
        boolean isLeafNode = false;
        if (node instanceof ITaCoKitRepositoryNode) {
            isLeafNode = ((ITaCoKitRepositoryNode) node).isLeafNode();
        }
        if (!isLeafNode) {
            setEnabled(false);
            return;
        }
        this.repositoryNode = (ITaCoKitRepositoryNode) node;
        this.configTypeNode = repositoryNode.getConfigTypeNode();
        this.setText(getCreateLabel());
        this.setToolTipText(getEditLabel());
        Image nodeImage = getNodeImage();
        if (nodeImage != null) {
            this.setImageDescriptor(ImageDescriptor.createFromImage(nodeImage));
        }
        IProxyRepositoryFactory factory = ProxyRepositoryFactory.getInstance();
        switch (node.getType()) {
        case SIMPLE_FOLDER:
        case SYSTEM_FOLDER:
            if (factory.isUserReadOnlyOnCurrentProject()
                    || !ProjectManager.getInstance().isInCurrentMainProject(node)) {
                setEnabled(false);
                return;
            }
            if (node.getObject() != null && node.getObject().getProperty().getItem().getState().isDeleted()) {
                setEnabled(false);
                return;
            }
            this.setText(getCreateLabel());
            collectChildNames(node);
            break;
        case REPOSITORY_ELEMENT:
            if (factory.isPotentiallyEditable(node.getObject()) && isLastVersion(node)) {
                this.setText(getEditLabel());
                collectSiblingNames(node);
            } else {
                this.setText(getOpenLabel());
            }
            break;
        default:
            return;
        }
        setEnabled(true);
    }

    protected int getWizardWidth() {
        return DEFAULT_WIZARD_WIDTH;
    }

    protected int getWizardHeight() {
        return DEFAULT_WIZARD_HEIGHT;
    }

    protected String getCreateLabel() {
        return Messages.getString("TaCoKitConfiguration.action.createLabel", configTypeNode.getConfigurationType(), //$NON-NLS-1$
                configTypeNode.getDisplayName());
    }

    protected String getEditLabel() {
        return Messages.getString("TaCoKitConfiguration.action.editLabel", configTypeNode.getConfigurationType(), //$NON-NLS-1$
                configTypeNode.getDisplayName());
    }

    protected String getOpenLabel() {
        return Messages.getString("TaCoKitConfiguration.action.openLabel", configTypeNode.getConfigurationType(), //$NON-NLS-1$
                configTypeNode.getDisplayName());
    }

    protected String getNodeLabel() {
        return repositoryNode.getDisplayText();
    }

    protected Image getNodeImage() {
        return null;
    }

    @Override
    public Class getClassForDoubleClick() {
        return ConnectionItem.class;
    }

}
