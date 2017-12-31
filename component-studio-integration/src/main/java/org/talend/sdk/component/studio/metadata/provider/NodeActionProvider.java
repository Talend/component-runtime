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
package org.talend.sdk.component.studio.metadata.provider;

import java.util.Map;
import java.util.Set;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.talend.commons.ui.swt.actions.ITreeContextualAction;
import org.talend.repository.view.di.metadata.action.MetedataNodeActionProvier;
import org.talend.sdk.component.server.front.model.ConfigTypeNode;
import org.talend.sdk.component.studio.Lookups;
import org.talend.sdk.component.studio.metadata.TaCoKitCache;
import org.talend.sdk.component.studio.metadata.action.CreateTaCoKitConfigurationAction;
import org.talend.sdk.component.studio.metadata.node.ITaCoKitRepositoryNode;
import org.talend.sdk.component.studio.metadata.node.TaCoKitFamilyRepositoryNode;

public class NodeActionProvider extends MetedataNodeActionProvier {

    @Override
    public void fillContextMenu(final IMenuManager manager) {
        final IStructuredSelection sel = IStructuredSelection.class.cast(getContext().getSelection());
        if (1 < sel.size()) {
            super.fillContextMenu(manager);
            return;
        }
        final Object selObj = sel.getFirstElement();
        if (selObj instanceof ITaCoKitRepositoryNode) {
            ITaCoKitRepositoryNode tacokitNode = (ITaCoKitRepositoryNode) selObj;
            if (!(tacokitNode instanceof TaCoKitFamilyRepositoryNode)) {
                ConfigTypeNode configTypeNode = tacokitNode.getConfigTypeNode();
                manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));

                if (tacokitNode.isLeafNode()) {
                    Set<String> edges = configTypeNode.getEdges();
                    if (edges != null && !edges.isEmpty()) {
                        TaCoKitCache cache = Lookups.taCoKitCache();
                        Map<String, ConfigTypeNode> configTypeNodeMap = cache.getConfigTypeNodeMap();
                        for (String edge : edges) {
                            ConfigTypeNode subTypeNode = configTypeNodeMap.get(edge);
                            ITreeContextualAction createAction = new CreateTaCoKitConfigurationAction(subTypeNode);
                            createAction.init((TreeViewer) getActionSite().getStructuredViewer(), sel);
                            manager.add(createAction);
                        }
                    }
                } else {
                    ITreeContextualAction createAction = new CreateTaCoKitConfigurationAction(configTypeNode);
                    createAction.init((TreeViewer) getActionSite().getStructuredViewer(), sel);
                    manager.add(createAction);
                }
            }
            // manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
        }
        super.fillContextMenu(manager);
    }

}
