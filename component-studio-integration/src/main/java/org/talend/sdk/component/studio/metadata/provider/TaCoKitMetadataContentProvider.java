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
package org.talend.sdk.component.studio.metadata.provider;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.talend.commons.exception.ExceptionHandler;
import org.talend.commons.ui.runtime.image.ECoreImage;
import org.talend.core.repository.model.ProjectRepositoryNode;
import org.talend.repository.model.RepositoryNode;
import org.talend.repository.view.di.metadata.content.AbstractMetadataContentProvider;
import org.talend.sdk.component.server.front.model.ConfigTypeNode;
import org.talend.sdk.component.server.front.model.ConfigTypeNodes;
import org.talend.sdk.component.studio.Lookups;
import org.talend.sdk.component.studio.metadata.node.AbsTaCoKitRepositoryNode;
import org.talend.sdk.component.studio.metadata.node.TaCoKitConfigurationRepositoryNode;
import org.talend.sdk.component.studio.metadata.node.TaCoKitFamilyRepositoryNode;
import org.talend.sdk.component.studio.util.TaCoKitConst;

public class TaCoKitMetadataContentProvider extends AbstractMetadataContentProvider {

    private final Object[] EMPTY_ARRAY = new Object[0];

    private ConfigTypeNodes configTypeNodesCache;

    private Map<String, ConfigTypeNode> configTypeNodeMapCache;

    private Set<RepositoryNode> familyNodesCache;

    @Override
    protected RepositoryNode
            getTopLevelNodeFromProjectRepositoryNode(final ProjectRepositoryNode projectRepositoryNode) {
        return projectRepositoryNode.getRootRepositoryNode(TaCoKitConst.METADATA_TACOKIT);
    }

    @Override
    public boolean hasChildren(final Object element) {
        if (element instanceof AbsTaCoKitRepositoryNode) {
            ConfigTypeNode configTypeNode = ((AbsTaCoKitRepositoryNode) element).getConfigTypeNode();
            Set<String> edges = configTypeNode.getEdges();
            if (edges == null || edges.isEmpty()) {
                return false;
            } else {
                return true;
            }
        } else {
            return true;
        }
    }

    @Override
    public Object[] getChildren(final Object element) {
        if (isRootNodeType(element)) {
            if (!((RepositoryNode) element).isInitialized()) {
                clearCache();
            }
            familyNodesCache = getTaCoKitFamilies((RepositoryNode) element);
            return familyNodesCache.toArray();
        }
        if (element instanceof TaCoKitFamilyRepositoryNode) {
            return getConfigurations((TaCoKitFamilyRepositoryNode) element);
        }
        if (element instanceof TaCoKitConfigurationRepositoryNode) {
            return getConfigurations((TaCoKitConfigurationRepositoryNode) element);
        }
        return EMPTY_ARRAY;
    }

    @Override
    public Set<RepositoryNode> getTopLevelNodes() {
        if (familyNodesCache == null) {
            return Collections.EMPTY_SET;
        } else {
            return familyNodesCache;
        }
    }

    @Override
    protected Object[] getRepositoryNodeChildren(final RepositoryNode repositoryNode) {
        if (repositoryNode instanceof TaCoKitFamilyRepositoryNode) {
            return getConfigurations((TaCoKitFamilyRepositoryNode) repositoryNode);
        } else if (repositoryNode instanceof TaCoKitConfigurationRepositoryNode) {
            return getConfigurations((TaCoKitConfigurationRepositoryNode) repositoryNode);
        } else {
            return getTaCoKitFamilies(repositoryNode).toArray();
        }
    }

    private Object[] getConfigurations(final AbsTaCoKitRepositoryNode tacoNode) {
        ConfigTypeNode configTypeNode = tacoNode.getConfigTypeNode();
        Set<String> edges = configTypeNode.getEdges();
        if (edges != null && !edges.isEmpty()) {
            Map<String, ConfigTypeNode> nodes = getConfigTypeNodeMap();
            for (String edge : edges) {
                ConfigTypeNode edgeNode = nodes.get(edge);
                try {
                    TaCoKitConfigurationRepositoryNode configurationRepositoryNode =
                            createConfigurationRepositoryNode(tacoNode, edgeNode);
                    tacoNode.getChildren().add(configurationRepositoryNode);
                } catch (Exception e) {
                    ExceptionHandler.process(e);
                }
            }
        }
        return tacoNode.getChildren().toArray();
    }

    private Set<RepositoryNode> getTaCoKitFamilies(final RepositoryNode repositoryNode) {
        try {
            Map<String, ConfigTypeNode> nodes = getConfigTypeNodeMap();
            Set<RepositoryNode> familyNodes = new HashSet<>();
            if (nodes != null) {
                for (ConfigTypeNode node : nodes.values()) {
                    String parentId = node.getParentId();
                    String configType = node.getConfigurationType();
                    if (StringUtils.isNotEmpty(parentId) || StringUtils.isNoneEmpty(configType)) {
                        continue;
                    }

                    TaCoKitFamilyRepositoryNode familyRepositoryNode = createFamilyRepositoryNode(repositoryNode, node);
                    repositoryNode.getChildren().add(familyRepositoryNode);
                    familyNodes.add(familyRepositoryNode);
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

    private TaCoKitConfigurationRepositoryNode createConfigurationRepositoryNode(
            final AbsTaCoKitRepositoryNode parentNode, final ConfigTypeNode configurationNode) {
        TaCoKitConfigurationRepositoryNode configurationRepositoryNode = new TaCoKitConfigurationRepositoryNode(
                parentNode, configurationNode.getDisplayName(), ECoreImage.FOLDER_CLOSE_ICON, configurationNode); // $NON-NLS-1$
        configurationRepositoryNode.setChildrenObjectType(TaCoKitConst.METADATA_TACOKIT);
        return configurationRepositoryNode;
    }

    private ConfigTypeNodes getConfigTypeNodes() {
        if (configTypeNodesCache == null) {
            configTypeNodesCache = Lookups.client().v1().configurationType().getRepositoryModel();
        }
        return configTypeNodesCache;
    }

    private Map<String, ConfigTypeNode> getConfigTypeNodeMap() {
        if (configTypeNodeMapCache == null || configTypeNodeMapCache.isEmpty()) {
            configTypeNodeMapCache = getConfigTypeNodes().getNodes();
        }
        return configTypeNodeMapCache;
    }

    private void clearCache() {
        if (configTypeNodeMapCache != null) {
            configTypeNodeMapCache.clear();
        }

        configTypeNodesCache = null;

        if (familyNodesCache != null) {
            familyNodesCache.clear();
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        clearCache();
    }

}
