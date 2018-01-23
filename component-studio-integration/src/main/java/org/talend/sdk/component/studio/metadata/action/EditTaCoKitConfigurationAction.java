/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
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

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.talend.core.model.properties.ConnectionItem;
import org.talend.core.repository.model.ProxyRepositoryFactory;
import org.talend.repository.model.IProxyRepositoryFactory;
import org.talend.repository.model.RepositoryNode;
import org.talend.sdk.component.studio.metadata.node.ITaCoKitRepositoryNode;
import org.talend.sdk.component.studio.ui.wizard.TaCoKitConfigurationRuntimeData;
import org.talend.sdk.component.studio.ui.wizard.TaCoKitEditWizard;

/**
 * Metadata contextual action which creates WizardDialog used to edit Component configuration.
 * Repository node may have only 1 edit action. This action is registered as extension point.
 * Thus, it supports double click out of the box
 */
public class EditTaCoKitConfigurationAction extends TaCoKitMetadataContextualAction {

    public EditTaCoKitConfigurationAction() {
        super();
    }

    @Override
    public void init(final RepositoryNode node) {
        boolean isLeafNode = false;
        if (node instanceof ITaCoKitRepositoryNode) {
            isLeafNode = ((ITaCoKitRepositoryNode) node).isLeafNode();
        }
        if (!isLeafNode) {
            setEnabled(false);
            return;
        }
        setRepositoryNode((ITaCoKitRepositoryNode) node);
        setConfigTypeNode(repositoryNode.getConfigTypeNode());
        setToolTipText(getEditLabel());
        Image nodeImage = getNodeImage();
        if (nodeImage != null) {
            this.setImageDescriptor(ImageDescriptor.createFromImage(nodeImage));
        }
        IProxyRepositoryFactory factory = ProxyRepositoryFactory.getInstance();
        switch (node.getType()) {
        case SIMPLE_FOLDER:
        case SYSTEM_FOLDER:
            if (isUserReadOnly() || belongsToCurrentProject(node) || isDeleted(node)) {
                setEnabled(false);
                return;
            } else {
                this.setText(getCreateLabel());
                collectChildNames(node);
                setEnabled(true);
            }
            break;
        case REPOSITORY_ELEMENT:
            if (factory.isPotentiallyEditable(node.getObject()) && isLastVersion(node)) {
                this.setText(getEditLabel());
                collectSiblingNames(node);
                setReadonly(false);
            } else {
                this.setText(getOpenLabel());
                setReadonly(true);
            }
            setEnabled(true);
            break;
        default:
            return;
        }
    }

    @Override
    protected WizardDialog createWizardDialog() {
        IWizard wizard = createWizard(PlatformUI.getWorkbench());
        return new WizardDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), wizard);
    }

    public TaCoKitEditWizard createWizard(final IWorkbench wb) {
        return new TaCoKitEditWizard(wb, createRuntimeData());
    }

    private TaCoKitConfigurationRuntimeData createRuntimeData() {
        TaCoKitConfigurationRuntimeData runtimeData = new TaCoKitConfigurationRuntimeData();
        runtimeData.setTaCoKitRepositoryNode(repositoryNode);
        runtimeData.setConfigTypeNode(repositoryNode.getConfigTypeNode());
        runtimeData.setConnectionItem((ConnectionItem) repositoryNode.getObject().getProperty().getItem());
        runtimeData.setCreation(false);
        runtimeData.setReadonly(isReadonly());
        return runtimeData;
    }

}
