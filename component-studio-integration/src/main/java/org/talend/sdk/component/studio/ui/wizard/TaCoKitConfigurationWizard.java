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
package org.talend.sdk.component.studio.ui.wizard;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.ui.IWorkbench;
import org.talend.commons.exception.ExceptionHandler;
import org.talend.commons.ui.runtime.exception.ExceptionMessageDialog;
import org.talend.core.model.properties.ConnectionItem;
import org.talend.core.model.repository.RepositoryManager;
import org.talend.core.repository.model.ProxyRepositoryFactory;
import org.talend.metadata.managment.ui.wizard.metadata.connection.Step0WizardPage;
import org.talend.repository.model.IProxyRepositoryFactory;
import org.talend.repository.model.IRepositoryNode.ENodeType;
import org.talend.repository.model.RepositoryNodeUtilities;
import org.talend.sdk.component.server.front.model.ConfigTypeNode;
import org.talend.sdk.component.studio.i18n.Messages;
import org.talend.sdk.component.studio.metadata.model.TaCoKitConfigurationItemModel;
import org.talend.sdk.component.studio.metadata.model.TaCoKitConfigurationModel;
import org.talend.sdk.component.studio.metadata.node.ITaCoKitRepositoryNode;
import org.talend.sdk.component.studio.ui.wizard.page.TaCoKitConfigurationWizardPage;
import org.talend.sdk.component.studio.util.TaCoKitConst;
import org.talend.sdk.component.studio.util.TaCoKitUtil;

/**
 * DOC cmeng class global comment. Detailled comment
 */
public class TaCoKitConfigurationWizard extends AbsTaCoKitWizard {

    private Step0WizardPage propertiesWizardPage;

    private TaCoKitConfigurationWizardPage wizardPage;

    public TaCoKitConfigurationWizard(final IWorkbench workbench, final TaCoKitConfigurationRuntimeData runtimeData) {
        super(workbench, runtimeData);
    }

    @Override
    protected void init() {
        super.init();
        TaCoKitConfigurationRuntimeData runtimeData = getTaCoKitConfigurationRuntimeData();

        creation = runtimeData.isCreation();
        connectionItem = runtimeData.getConnectionItem();
        runtimeData.setReadonly(runtimeData.isReadonly() || !isRepositoryObjectEditable());
        ITaCoKitRepositoryNode repositoryNode = runtimeData.getTaCoKitRepositoryNode();
        IPath basePath = TaCoKitUtil.getTaCoKitBaseFolder(runtimeData.getConfigTypeNode());

        ENodeType nodeType = repositoryNode.getType();
        switch (nodeType) {
        case SIMPLE_FOLDER:
            pathToSave = basePath.append(RepositoryNodeUtilities.getPath(repositoryNode));
            break;
        case REPOSITORY_ELEMENT:
            if (creation) {
                pathToSave = basePath;
            } else {
                pathToSave = basePath.append(RepositoryNodeUtilities.getPath(repositoryNode));
            }
            break;
        case SYSTEM_FOLDER:
            pathToSave = basePath; // $NON-NLS-1$
            break;
        default:
            // nothing todo
        }
        switch (nodeType) {
        case SIMPLE_FOLDER:
        case SYSTEM_FOLDER:
            break;
        case REPOSITORY_ELEMENT:
            initLockStrategy();
            break;
        default:
            // nothing to do
        }
        ConfigTypeNode configTypeNode = runtimeData.getConfigTypeNode();
        if (creation) {
            setWindowTitle(Messages.getString("TaCoKitConfiguration.wizard.title.create", //$NON-NLS-1$
                    configTypeNode.getConfigurationType(), configTypeNode.getDisplayName()));
        } else {
            setWindowTitle(
                    Messages.getString("TaCoKitConfiguration.wizard.title.edit", configTypeNode.getConfigurationType(), //$NON-NLS-1$
                            configTypeNode.getDisplayName()));
        }
        setHelpAvailable(false);
        // setRepositoryLocation(wizard, location, connectionItem.getProperty().getId());
    }

    @Override
    public void addPages() {
        TaCoKitConfigurationRuntimeData runtimeData = getTaCoKitConfigurationRuntimeData();
        propertiesWizardPage = new Step0WizardPage(runtimeData.getConnectionItem().getProperty(), pathToSave,
                TaCoKitConst.METADATA_TACOKIT, runtimeData.isReadonly(), creation);
        ConfigTypeNode configTypeNode = runtimeData.getConfigTypeNode();
        propertiesWizardPage.setTitle(Messages.getString("TaCoKitConfiguration.wizard.title", //$NON-NLS-1$
                configTypeNode.getConfigurationType(), configTypeNode.getDisplayName()));
        propertiesWizardPage.setDescription(""); //$NON-NLS-1$
        addPage(propertiesWizardPage);

        wizardPage = new TaCoKitConfigurationWizardPage(runtimeData);
        addPage(wizardPage);

    }

    @Override
    public boolean performFinish() {
        if (wizardPage.isPageComplete()) {
            try {

                IWorkspace workspace = ResourcesPlugin.getWorkspace();
                IWorkspaceRunnable operation = new IWorkspaceRunnable() {

                    @Override
                    public void run(final IProgressMonitor monitor) throws CoreException {
                        try {
                            createOrUpdateConfigurationItem();
                        } catch (Exception e) {
                            throw new CoreException(
                                    new Status(IStatus.ERROR, TaCoKitConst.BUNDLE_ID, e.getMessage(), e));
                        }
                    }
                };
                ISchedulingRule schedulingRule = workspace.getRoot();
                workspace.run(operation, schedulingRule, IWorkspace.AVOID_UPDATE, new NullProgressMonitor());

                return true;
            } catch (Exception e) {
                ExceptionHandler.process(e);
                String message = e.getLocalizedMessage();
                if (StringUtils.isEmpty(message)) {
                    message = Messages.getString("TaCoKitConfiguration.wizard.exception.message.default"); //$NON-NLS-1$
                }
                ExceptionMessageDialog.openError(getShell(),
                        Messages.getString("TaCoKitConfiguration.wizard.exception.title"), //$NON-NLS-1$
                        message, e);
            }
        }
        return false;
    }

    @Override
    public boolean canFinish() {
        IWizardPage currentPage = this.getContainer().getCurrentPage();
        if (currentPage instanceof Step0WizardPage) {
            return false;
        }
        if (currentPage.isPageComplete()) {
            return true;
        }
        return false;
    }

    private void createOrUpdateConfigurationItem() throws Exception {
        IProxyRepositoryFactory factory = ProxyRepositoryFactory.getInstance();
        if (creation) {
            String nextId = factory.getNextId();
            TaCoKitConfigurationRuntimeData runtimeData = getTaCoKitConfigurationRuntimeData();
            ITaCoKitRepositoryNode taCoKitRepositoryNode = runtimeData.getTaCoKitRepositoryNode();
            ConfigTypeNode configTypeNode = runtimeData.getConfigTypeNode();
            String id = configTypeNode.getId();
            String parentId = configTypeNode.getParentId();
            ConnectionItem connectionItem = runtimeData.getConnectionItem();

            connectionItem.getProperty().setId(nextId);
            TaCoKitConfigurationItemModel itemModel = new TaCoKitConfigurationItemModel(connectionItem);
            TaCoKitConfigurationModel model = itemModel.getConfigurationModel();
            model.setConfigurationId(id);
            model.setParentConfigurationId(parentId);
            if (taCoKitRepositoryNode.isLeafNode()) {
                model.setParentItemId(taCoKitRepositoryNode.getObject().getId());
            }

            factory.create(connectionItem, propertiesWizardPage.getDestinationPath());

            RepositoryManager.refreshCreatedNode(TaCoKitConst.METADATA_TACOKIT);
            // RepositoryUpdateManager.updateFileConnection(connectionItem);

        } else {
            updateConnectionItem();
            refreshInFinish(propertiesWizardPage.isNameModifiedByUser());
        }
    }

}
