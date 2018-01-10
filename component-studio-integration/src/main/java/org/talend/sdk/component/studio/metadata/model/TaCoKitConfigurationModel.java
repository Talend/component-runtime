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
package org.talend.sdk.component.studio.metadata.model;

import java.util.List;
import java.util.Map;

import org.talend.core.model.metadata.builder.connection.Connection;
import org.talend.sdk.component.server.front.model.ConfigTypeNode;
import org.talend.sdk.component.server.front.model.SimplePropertyDefinition;
import org.talend.sdk.component.studio.Lookups;
import org.talend.sdk.component.studio.model.parameter.TaCoKitElementParameter;
import org.talend.sdk.component.studio.util.TaCoKitUtil;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * DOC cmeng class global comment. Detailled comment
 * Provides convenient API for updating {@link Connection} properties
 */
public class TaCoKitConfigurationModel {

    private static final String TACOKIT_CONFIG_ID = "__TACOKIT_CONFIG_ID"; //$NON-NLS-1$

    private static final String TACOKIT_CONFIG_PARENT_ID = "__TACOKIT_CONFIG_PARENT_ID"; //$NON-NLS-1$

    private static final String TACOKIT_PARENT_ITEM_ID = "__TACOKIT_PARENT_ITEM_ID"; //$NON-NLS-1$

    private final Connection connection;

    private ConfigTypeNode configTypeNodeCache;

    private String configTypeNodeIdCache;

    private TaCoKitConfigurationModel parentConfigurationModelCache;

    private String parentConfigurationModelItemIdCache;

    public TaCoKitConfigurationModel(final Connection connection) {
        this.connection = connection;
    }

    public String getConfigurationId() {
        return (String) getProperties().get(TACOKIT_CONFIG_ID);
    }

    @SuppressWarnings("unchecked")
    public void setConfigurationId(final String id) {
        getProperties().put(TACOKIT_CONFIG_ID, id);
    }

    public String getParentConfigurationId() {
        return (String) getProperties().get(TACOKIT_CONFIG_PARENT_ID);
    }

    @SuppressWarnings("unchecked")
    public void setParentConfigurationId(final String parentId) {
        getProperties().put(TACOKIT_CONFIG_PARENT_ID, parentId);
    }

    public String getParentItemId() {
        return (String) getProperties().get(TACOKIT_PARENT_ITEM_ID);
    }

    @SuppressWarnings("unchecked")
    public void setParentItemId(final String parentItemId) {
        getProperties().put(TACOKIT_PARENT_ITEM_ID, parentItemId);
    }

    public ValueModel getValue(final String key) throws Exception {
        TaCoKitConfigurationModel parentModel = getParentConfigurationModel();
        if (parentModel != null) {
            ValueModel modelValue = parentModel.getValue(key);
            if (modelValue == null) {
                if (parentModel.containsKey(key)) {
                    return new ValueModel(parentModel, null);
                }
            } else {
                return modelValue;
            }
        }
        String value = getValueOfSelf(key);
        if (value != null || containsKey(key)) {
            return new ValueModel(this, value);
        }
        return null;
    }

    public ConfigTypeNode getFirstConfigTypeNodeContains(final String key) throws Exception {
        ConfigTypeNode configTypeNode = null;
        TaCoKitConfigurationModel parentModel = getParentConfigurationModel();
        if (parentModel != null) {
            configTypeNode = parentModel.getFirstConfigTypeNodeContains(key);
        }
        if (configTypeNode == null) {
            if (containsKey(key)) {
                configTypeNode = getConfigTypeNode();
            }
        }
        return configTypeNode;
    }

    public boolean containsKey(final String key) throws Exception {
        List<SimplePropertyDefinition> properties = getConfigTypeNode().getProperties();
        if (key == null || key.isEmpty() || properties == null || properties.isEmpty()) {
            return false;
        }
        for (SimplePropertyDefinition property : properties) {
            if (TaCoKitUtil.equals(key, property.getPath())) {
                return true;
            }
        }
        return false;
    }

    public String getValueOfSelf(final String key) {
        return (String) getProperties().get(key);
    }

    @SuppressWarnings("unchecked")
    public void setValue(final TaCoKitElementParameter parameter) {
        getProperties().put(parameter.getName(), parameter.getStringValue());
    }

    public ConfigTypeNode getConfigTypeNode() throws Exception {
        if (configTypeNodeCache == null || !TaCoKitUtil.equals(configTypeNodeIdCache, getConfigurationId())) {
            configTypeNodeCache = null;
            configTypeNodeIdCache = getConfigurationId();
            if (!TaCoKitUtil.isEmpty(configTypeNodeIdCache)) {
                configTypeNodeCache = Lookups.taCoKitCache().getConfigTypeNodeMap().get(configTypeNodeIdCache);
            }
        }
        return configTypeNodeCache;
    }

    public TaCoKitConfigurationModel getParentConfigurationModel() throws Exception {
        if (parentConfigurationModelCache == null
                || !TaCoKitUtil.equals(parentConfigurationModelItemIdCache, getParentItemId())) {
            parentConfigurationModelCache = null;
            parentConfigurationModelItemIdCache = getParentItemId();
            if (!TaCoKitUtil.isEmpty(parentConfigurationModelItemIdCache)) {
                parentConfigurationModelCache =
                        TaCoKitUtil.getTaCoKitConfigurationModel(parentConfigurationModelItemIdCache);
            }
        }
        return parentConfigurationModelCache;
    }

    /**
     * Retrieves {@link Connection} properties holder.
     * Properties should contain only String values as they values are not converted further during serialization
     * 
     * @return map of connection properties
     */
    @SuppressWarnings({ "deprecation", "rawtypes" })
    private Map getProperties() {
        return connection.getProperties();
    }

    @Data
    @AllArgsConstructor
    public static class ValueModel {

        private TaCoKitConfigurationModel configurationModel;

        private String value;

    }

}
