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
package org.talend.sdk.component.studio.model.connector;

import static org.talend.core.model.process.EConnectionType.FLOW_MAIN;
import static org.talend.sdk.component.studio.model.connector.AbstractConnectorCreator.getType;

import org.talend.core.model.process.INode;
import org.talend.sdk.component.server.front.model.ComponentDetail;

/**
 * Creates appropriate {@link ConnectorCreator} according component meta
 */
public final class ConnectorCreatorFactory {

    private ConnectorCreatorFactory() {
        // no-op
    }

    public static ConnectorCreator create(ComponentDetail component, INode node) {
        if (!hasInputs(component) && !hasOutputs(component)) {
            return new StandAloneConnectorCreator(component, node);
        } else if (!hasInputs(component) && hasOutputs(component)) {
            return new PartitionMapperConnectorCreator(component, node);
        } else if (hasInputs(component) && !hasOutputs(component)) {
            return new OutputConnectorCreator(component, node);
        } else {
            return new ProcessorConnectorCreator(component, node);
        }
    }

    private static boolean hasInputs(ComponentDetail component) {
        return component.getInputFlows().stream() //
                .filter(input -> FLOW_MAIN.equals(getType(input))) //
                .count() > 0; //
    }

    private static boolean hasOutputs(ComponentDetail component) {
        return component.getOutputFlows().stream() //
                .filter(output -> FLOW_MAIN.equals(getType(output))) //
                .count() > 0; //
    }
}
