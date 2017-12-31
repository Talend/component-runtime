/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
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

import static lombok.AccessLevel.PRIVATE;
import static org.talend.core.model.process.EConnectionType.FLOW_MAIN;
import static org.talend.sdk.component.studio.model.connector.AbstractConnectorCreator.getType;

import org.talend.core.model.process.INode;
import org.talend.sdk.component.server.front.model.ComponentDetail;

import lombok.NoArgsConstructor;

/**
 * Creates appropriate {@link ConnectorCreator} according component meta
 */
@NoArgsConstructor(access = PRIVATE)
public final class ConnectorCreatorFactory {

    public static ConnectorCreator create(final ComponentDetail component, final INode node) {
        if (!hasInputs(component) && !hasOutputs(component)) {
            return new StandAloneConnectorCreator(component, node);
        } else if (!hasInputs(component) && hasOutputs(component)) {
            return new PartitionMapperConnectorCreator(component, node);
        } else if (hasInputs(component) && !hasOutputs(component)) {
            return new OutputConnectorCreator(component, node);
        }
        return new ProcessorConnectorCreator(component, node);
    }

    private static boolean hasInputs(final ComponentDetail component) {
        return component
                .getInputFlows()
                .stream() //
                .anyMatch(input -> FLOW_MAIN.equals(getType(input)));
    }

    private static boolean hasOutputs(final ComponentDetail component) {
        return component
                .getOutputFlows()
                .stream() //
                .anyMatch(output -> FLOW_MAIN.equals(getType(output)));
    }
}
