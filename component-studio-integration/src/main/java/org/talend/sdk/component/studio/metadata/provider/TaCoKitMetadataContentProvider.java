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
package org.talend.sdk.component.studio.metadata.provider;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.talend.commons.exception.ExceptionHandler;
import org.talend.commons.runtime.model.repository.ERepositoryStatus;
import org.talend.commons.ui.runtime.image.ECoreImage;
import org.talend.commons.utils.data.container.Container;
import org.talend.commons.utils.data.container.RootContainer;
import org.talend.core.model.properties.ConnectionItem;
import org.talend.core.model.properties.Property;
import org.talend.core.model.repository.ERepositoryObjectType;
import org.talend.core.model.repository.Folder;
import org.talend.core.model.repository.IRepositoryViewObject;
import org.talend.core.repository.model.ProjectRepositoryNode;
import org.talend.core.repository.model.ProxyRepositoryFactory;
import org.talend.repository.ProjectManager;
import org.talend.repository.model.IProxyRepositoryFactory;
import org.talend.repository.model.IRepositoryNode;
import org.talend.repository.model.RepositoryNode;
import org.talend.repository.model.nodes.IProjectRepositoryNode;
import org.talend.repository.view.di.metadata.content.AbstractMetadataContentProvider;
import org.talend.sdk.component.server.front.model.ConfigTypeNode;
import org.talend.sdk.component.studio.Lookups;
import org.talend.sdk.component.studio.metadata.model.TaCoKitConfigurationItemModel;
import org.talend.sdk.component.studio.metadata.model.TaCoKitConfigurationModel;
import org.talend.sdk.component.studio.metadata.node.ITaCoKitRepositoryNode;
import org.talend.sdk.component.studio.metadata.node.TaCoKitConfigurationRepositoryNode;
import org.talend.sdk.component.studio.metadata.node.TaCoKitFamilyRepositoryNode;
import org.talend.sdk.component.studio.metadata.node.TaCoKitFolderRepositoryNode;
import org.talend.sdk.component.studio.metadata.node.TaCoKitLeafRepositoryNode;
import org.talend.sdk.component.studio.util.TaCoKitConst;

public class TaCoKitMetadataContentProvider extends AbstractMetadataContentProvider {

    private final Object[] EMPTY_ARRAY = new Object[0];

    private Set<RepositoryNode> familyNodesCache;

    @Override
    protected RepositoryNode
            getTopLevelNodeFromProjectRepositoryNode(final ProjectRepositoryNode projectRepositoryNode) {
        return projectRepositoryNode.getRootRepositoryNode(TaCoKitConst.METADATA_TACOKIT);
    }

    @Override
    public boolean hasChildren(final Object element) {
        if (element instanceof ITaCoKitRepositoryNode) {
            List<IRepositoryNode> children = ((ITaCoKitRepositoryNode) element).getChildren();
            if (children == null || children.isEmpty()) {
                return false;
            } else {
                return true;
            }
        }
        return super.hasChildren(element);
    }

    @Override
    public Object[] getChildren(final Object element) {
        if (isRootNodeType(element)) {
            return getTopLevelNodes().toArray();
        }
        if (element instanceof RepositoryNode) {
            RepositoryNode repNode = (RepositoryNode) element;
            if (!repNode.isInitialized()) {
                try {
                    RootContainer<String, IRepositoryViewObject> metadata =
                            ProxyRepositoryFactory.getInstance().getMetadata(
                                    ProjectManager.getInstance().getCurrentProject(), TaCoKitConst.METADATA_TACOKIT,
                                    true);
                    getConfigurations((ITaCoKitRepositoryNode) repNode, metadata);
                    repNode.setInitialized(true);
                } catch (Exception e) {
                    ExceptionHandler.process(e);
                }
            }
            return ((RepositoryNode) element).getChildren().toArray();
        }
        return EMPTY_ARRAY;
    }

    @Override
    public Set<RepositoryNode> getTopLevelNodes() {
        try {
            RepositoryNode repoNode =
                    ProjectRepositoryNode.getInstance().getRootRepositoryNode(ERepositoryObjectType.METADATA);
            if (!repoNode.isInitialized()) {
                if (!isAllInitialized(familyNodesCache) || !repoNode.getChildren().containsAll(familyNodesCache)) {
                    if (familyNodesCache != null && !familyNodesCache.isEmpty()) {
                        repoNode.getChildren().removeAll(familyNodesCache);
                    }
                    clearCache();
                    RootContainer<String, IRepositoryViewObject> metadata =
                            ProxyRepositoryFactory.getInstance().getMetadata(
                                    ProjectManager.getInstance().getCurrentProject(), TaCoKitConst.METADATA_TACOKIT,
                                    true);
                    familyNodesCache = getTaCoKitFamilies(repoNode, metadata);
                }
            }
            return familyNodesCache;
        } catch (Exception e) {
            ExceptionHandler.process(e);
        }
        return Collections.EMPTY_SET;
    }

    private boolean isAllInitialized(final Set<RepositoryNode> repoNodes) {
        if (repoNodes == null || repoNodes.isEmpty()) {
            return false;
        }
        for (RepositoryNode repoNode : repoNodes) {
            if (!repoNode.isInitialized()) {
                return false;
            }
        }
        return true;
    }

    private Object[] getConfigurations(final ITaCoKitRepositoryNode tacoNode,
            final Container<String, IRepositoryViewObject> itemObjs) {
        ConfigTypeNode configTypeNode = tacoNode.getConfigTypeNode();
        Set<String> edges = null;
        if (!tacoNode.isFolderNode() && !tacoNode.isLeafNode()) {
            edges = configTypeNode.getEdges();
        }

        // 1. create sub configuration
        if (edges != null && !edges.isEmpty()) {
            Map<String, ConfigTypeNode> nodes = Lookups.taCoKitCache().getConfigTypeNodeMap();
            for (String edge : edges) {
                ConfigTypeNode edgeNode = nodes.get(edge);
                TaCoKitConfigurationRepositoryNode configurationRepositoryNode =
                        createConfigurationRepositoryNode((RepositoryNode) tacoNode, tacoNode, edgeNode);
                tacoNode.getChildren().add(configurationRepositoryNode);
                getConfigurations(configurationRepositoryNode, itemObjs);
                configurationRepositoryNode.setInitialized(true);
            }
        }

        // 2. create nodes from storage
        if (!tacoNode.isFamilyNode() && itemObjs != null) {
            IProxyRepositoryFactory factory = ProxyRepositoryFactory.getInstance();
            // 2.1 create folders and their children
            List<Container<String, IRepositoryViewObject>> subContainers = itemObjs.getSubContainer();
            if (subContainers != null && !subContainers.isEmpty()) {
                Iterator<Container<String, IRepositoryViewObject>> subContainerIter = subContainers.iterator();
                while (subContainerIter.hasNext()) {
                    Container<String, IRepositoryViewObject> subContainer = subContainerIter.next();
                    Folder oFolder = new Folder((Property) subContainer.getProperty(), TaCoKitConst.METADATA_TACOKIT);
                    if (factory.getStatus(oFolder) != ERepositoryStatus.DELETED) {
                        ITaCoKitRepositoryNode parentTaCoKitNode = null;
                        if (tacoNode.isFolderNode()) {
                            parentTaCoKitNode = tacoNode.getParentTaCoKitNode();
                        } else if (tacoNode.isLeafNode()) {
                            parentTaCoKitNode = tacoNode;
                        } else {
                            parentTaCoKitNode = tacoNode;
                        }
                        TaCoKitFolderRepositoryNode folderNode = new TaCoKitFolderRepositoryNode(oFolder,
                                (RepositoryNode) tacoNode, parentTaCoKitNode, subContainer.getLabel(), configTypeNode);
                        tacoNode.getChildren().add(folderNode);
                        getConfigurations(folderNode, subContainer);
                        folderNode.setInitialized(true);
                    }
                }
            }
            // 2.2 create nodes
            List<IRepositoryViewObject> members = itemObjs.getMembers();
            if (members != null && !members.isEmpty()) {
                Iterator<IRepositoryViewObject> iter = members.iterator();
                Map<String, IRepositoryViewObject> repoViewObjMap = new HashMap<>();
                while (iter.hasNext()) {
                    IRepositoryViewObject viewObject = iter.next();
                    repoViewObjMap.put(viewObject.getId(), viewObject);
                }
                Map<String, ITaCoKitRepositoryNode> repoNodeMap = new HashMap<>();
                Set<IRepositoryViewObject> visitedCollection = new HashSet<>();
                iter = members.iterator();
                while (iter.hasNext()) {
                    IRepositoryViewObject viewObject = iter.next();
                    if (visitedCollection.contains(viewObject)) {
                        continue;
                    }
                    if (factory.getStatus(viewObject) != ERepositoryStatus.DELETED) {
                        try {
                            createLeafNode(tacoNode, viewObject, repoViewObjMap, repoNodeMap, visitedCollection);
                        } catch (Exception e) {
                            ExceptionHandler.process(e);
                        }
                    }
                    visitedCollection.add(viewObject);
                }
            }
        }
        return tacoNode.getChildren().toArray();
    }

    private void createLeafNode(final ITaCoKitRepositoryNode tacoNode, final IRepositoryViewObject viewObject,
            final Map<String, IRepositoryViewObject> repoViewObjMap,
            final Map<String, ITaCoKitRepositoryNode> repoNodeMap, final Set<IRepositoryViewObject> visitedCollection)
            throws Exception {
        if (visitedCollection.contains(viewObject)) {
            return;
        } else {
            visitedCollection.add(viewObject);
        }
        RepositoryNode parentNode = (RepositoryNode) tacoNode;
        ITaCoKitRepositoryNode parentTaCoKitNode = tacoNode;
        ConnectionItem item = (ConnectionItem) viewObject.getProperty().getItem();
        TaCoKitConfigurationItemModel itemModule = new TaCoKitConfigurationItemModel(item);
        TaCoKitConfigurationModel module = itemModule.getConfigurationModel();
        String parentItemId = module.getParentItemId();
        if (StringUtils.isNotEmpty(parentItemId)) {
            IRepositoryViewObject parentViewObj = repoViewObjMap.get(parentItemId);
            if (parentViewObj == null) {
                throw new Exception("Can't find parent node: " + parentItemId);
            }
            parentTaCoKitNode = repoNodeMap.get(parentItemId);
            if (parentTaCoKitNode == null) {
                createLeafNode(tacoNode, parentViewObj, repoViewObjMap, repoNodeMap, visitedCollection);
                parentTaCoKitNode = repoNodeMap.get(parentItemId);
            }
            if (parentTaCoKitNode == null) {
                // not created, means don't need to create
                return;
            }
        }
        if (parentTaCoKitNode.isFolderNode()) {
            parentTaCoKitNode = tacoNode.getParentTaCoKitNode();
            if (!StringUtils.equals(module.getConfigurationId(), parentTaCoKitNode.getConfigTypeNode().getId())) {
                return;
            }
        } else if (parentTaCoKitNode.isLeafNode()) {
            if (!StringUtils.equals(module.getParentConfigurationId(), parentTaCoKitNode.getConfigTypeNode().getId())) {
                return;
            }
            parentNode = (RepositoryNode) parentTaCoKitNode;
        } else {
            if (!StringUtils.equals(module.getConfigurationId(), parentTaCoKitNode.getConfigTypeNode().getId())) {
                return;
            }
        }
        TaCoKitLeafRepositoryNode leafRepositoryNode = createLeafRepositoryNode(parentNode, parentTaCoKitNode,
                itemModule, Lookups.taCoKitCache().getConfigTypeNodeMap().get(module.getConfigurationId()), viewObject);
        parentNode.getChildren().add(leafRepositoryNode);
        leafRepositoryNode.setInitialized(true);
        repoNodeMap.put(leafRepositoryNode.getId(), leafRepositoryNode);
    }

    private Set<RepositoryNode> getTaCoKitFamilies(final RepositoryNode repositoryNode,
            final RootContainer<String, IRepositoryViewObject> itemObjs) {
        try {
            Map<String, ConfigTypeNode> nodes = Lookups.taCoKitCache().getConfigTypeNodeMap();
            Set<RepositoryNode> familyNodes = new HashSet<>();
            if (nodes != null) {
                for (ConfigTypeNode node : nodes.values()) {
                    String parentId = node.getParentId();
                    String configType = node.getConfigurationType();
                    if (StringUtils.isNotEmpty(parentId) || StringUtils.isNotEmpty(configType)) {
                        continue;
                    }

                    TaCoKitFamilyRepositoryNode familyRepositoryNode = createFamilyRepositoryNode(repositoryNode, node);
                    initilizeContentProviderWithTopLevelNode(familyRepositoryNode);
                    repositoryNode.getChildren().add(familyRepositoryNode);
                    familyNodes.add(familyRepositoryNode);
                    getConfigurations(familyRepositoryNode, itemObjs);
                    familyRepositoryNode.setInitialized(true);
                }
            }
            return familyNodes;
        } catch (Exception e) {
            ExceptionHandler.process(e);
        }
        return Collections.EMPTY_SET;
    }

    private TaCoKitFamilyRepositoryNode createFamilyRepositoryNode(final RepositoryNode parentNode,
            final ConfigTypeNode tacokitFamilyNode) {
        TaCoKitFamilyRepositoryNode familyRepositoryNode = new TaCoKitFamilyRepositoryNode(parentNode,
                tacokitFamilyNode.getDisplayName(), ECoreImage.FOLDER_CLOSE_ICON, tacokitFamilyNode);
        familyRepositoryNode.setChildrenObjectType(TaCoKitConst.METADATA_TACOKIT);
        return familyRepositoryNode;
    }

    private TaCoKitConfigurationRepositoryNode createConfigurationRepositoryNode(final RepositoryNode parentNode,
            final ITaCoKitRepositoryNode parentTaCoKitNode, final ConfigTypeNode configurationNode) {
        TaCoKitConfigurationRepositoryNode configurationRepositoryNode = new TaCoKitConfigurationRepositoryNode(null,
                parentNode, parentTaCoKitNode, configurationNode.getDisplayName(), configurationNode); // $NON-NLS-1$
        return configurationRepositoryNode;
    }

    private TaCoKitLeafRepositoryNode createLeafRepositoryNode(final RepositoryNode parentNode,
            final ITaCoKitRepositoryNode parentTaCoKitNode, final TaCoKitConfigurationItemModel model,
            final ConfigTypeNode configurationTypeNode, final IRepositoryViewObject viewObject) {
        TaCoKitLeafRepositoryNode leafNode = new TaCoKitLeafRepositoryNode(viewObject, parentNode, parentTaCoKitNode,
                model.getDisplayLabel(), configurationTypeNode);
        return leafNode;
    }

    @Override
    protected IPath getWorkspaceTopNodePath(final RepositoryNode topLevelNode) {
        IPath workspaceRelativePath = null;
        IProjectRepositoryNode root = topLevelNode.getRoot();
        if (root != null) {
            String projectName = root.getProject().getTechnicalLabel();
            if (projectName != null) {
                workspaceRelativePath =
                        Path.fromPortableString('/' + projectName).append(TaCoKitConst.METADATA_TACOKIT_PATH); // $NON-NLS-1$
            }
        }
        return workspaceRelativePath;
    }

    @Override
    public void clearCache() {
        super.clearCache();
        Lookups.taCoKitCache().clearCache();

        if (familyNodesCache != null && !familyNodesCache.isEmpty()) {
            familyNodesCache.clear();
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        clearCache();
    }
}
