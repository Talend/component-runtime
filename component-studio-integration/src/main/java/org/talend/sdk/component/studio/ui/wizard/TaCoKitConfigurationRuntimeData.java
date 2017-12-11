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

import org.talend.core.model.properties.ConnectionItem;
import org.talend.sdk.component.server.front.model.ConfigTypeNode;
import org.talend.sdk.component.studio.metadata.node.ITaCoKitRepositoryNode;

public class TaCoKitConfigurationRuntimeData {

    private ITaCoKitRepositoryNode tacokitRepositoryNode;

    private ConnectionItem connectionItem;

    private ConfigTypeNode configTypeNode;

    private boolean isCreation = true;

    private boolean isReadonly = false;

    private boolean isAddContextFields = false;

    private String[] existingNames;

    public ITaCoKitRepositoryNode getTaCoKitRepositoryNode() {
        return this.tacokitRepositoryNode;
    }

    public void setTaCoKitRepositoryNode(final ITaCoKitRepositoryNode tacokitRepositoryNode) {
        this.tacokitRepositoryNode = tacokitRepositoryNode;
    }

    public boolean isCreation() {
        return this.isCreation;
    }

    public void setCreation(final boolean isCreation) {
        this.isCreation = isCreation;
    }

    public boolean isReadonly() {
        return this.isReadonly;
    }

    public void setReadonly(final boolean isReadonly) {
        this.isReadonly = isReadonly;
    }

    public boolean isAddContextFields() {
        return this.isAddContextFields;
    }

    public void setAddContextFields(final boolean isAddContextFields) {
        this.isAddContextFields = isAddContextFields;
    }

    public String[] getExistingNames() {
        return this.existingNames;
    }

    public void setExistingNames(final String[] existingNames) {
        this.existingNames = existingNames;
    }

    public ConnectionItem getConnectionItem() {
        return this.connectionItem;
    }

    public void setConnectionItem(final ConnectionItem connectionItem) {
        this.connectionItem = connectionItem;
    }

    public ConfigTypeNode getConfigTypeNode() {
        return this.configTypeNode;
    }

    public void setConfigTypeNode(final ConfigTypeNode configTypeNode) {
        this.configTypeNode = configTypeNode;
    }

}
