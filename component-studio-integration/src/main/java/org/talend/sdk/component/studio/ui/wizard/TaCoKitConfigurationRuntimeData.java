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
package org.talend.sdk.component.studio.ui.wizard;

import org.talend.core.model.properties.ConnectionItem;
import org.talend.sdk.component.server.front.model.ConfigTypeNode;
import org.talend.sdk.component.studio.metadata.node.ITaCoKitRepositoryNode;

import lombok.Data;

@Data
public class TaCoKitConfigurationRuntimeData {

    private ITaCoKitRepositoryNode taCoKitRepositoryNode;

    private ConnectionItem connectionItem;

    private ConfigTypeNode configTypeNode;

    private boolean isCreation = true;

    private boolean isReadonly = false;

    private boolean isAddContextFields = false;

    private String[] existingNames;

}
