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

import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.talend.core.model.repository.ERepositoryObjectType;
import org.talend.core.repository.ui.actions.CreateFolderAction;
import org.talend.core.repository.ui.wizard.folder.FolderWizard;
import org.talend.repository.model.IRepositoryNode.EProperties;
import org.talend.repository.model.RepositoryConstants;
import org.talend.repository.model.RepositoryNodeUtilities;
import org.talend.sdk.component.studio.metadata.node.ITaCoKitRepositoryNode;
import org.talend.sdk.component.studio.util.TaCoKitUtil;

/**
 * DOC cmeng class global comment. Detailled comment
 */
public class CreateTaCoKitFolderAction extends CreateFolderAction {

    @Override
    protected void doRun() {
        ISelection selection = getSelection();
        Object obj = ((IStructuredSelection) selection).getFirstElement();
        ITaCoKitRepositoryNode node = (ITaCoKitRepositoryNode) obj;

        ERepositoryObjectType objectType = null;
        IPath path = null;
        IPath parentPath = TaCoKitUtil.getTaCoKitBaseFolder(node.getConfigTypeNode());
        path = parentPath.append(RepositoryNodeUtilities.getPath(node));
        if (RepositoryConstants.isSystemFolder(path.toString())) {
            return;
        }
        objectType = (ERepositoryObjectType) node.getProperties(EProperties.CONTENT_TYPE);

        if (objectType != null) {
            FolderWizard processWizard = new FolderWizard(path, objectType, null);
            Shell activeShell = Display.getCurrent().getActiveShell();
            WizardDialog dialog = new WizardDialog(activeShell, processWizard);
            dialog.setPageSize(400, 60);
            dialog.create();
            dialog.open();
        }
    }

}
