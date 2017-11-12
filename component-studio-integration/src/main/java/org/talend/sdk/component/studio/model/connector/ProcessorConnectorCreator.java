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
import static org.talend.core.model.process.EConnectionType.FLOW_MERGE;
import static org.talend.core.model.process.EConnectionType.FLOW_REF;
import static org.talend.core.model.process.EConnectionType.ITERATE;
import static org.talend.core.model.process.EConnectionType.REJECT;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.eclipse.swt.graphics.RGB;
import org.talend.core.model.process.EConnectionType;
import org.talend.core.model.process.INode;
import org.talend.core.model.process.INodeConnector;
import org.talend.sdk.component.server.front.model.ComponentDetail;

/**
 * Creates connectors for Processor component
 */
class ProcessorConnectorCreator extends AbstractConnectorCreator {

    protected ProcessorConnectorCreator(ComponentDetail detail, INode node) {
        super(detail, node);
    }

    /**
     * Processor component may have both incoming and outgoing connections
     */
    @Override
    protected List<INodeConnector> createMainConnectors() {
        List<INodeConnector> mains = new ArrayList<>();
        detail.getInputFlows().stream() //
                .filter(input -> FLOW_MAIN.equals(getType(input))) //
                .forEach(input -> { //
                    INodeConnector main = createConnector(getType(input), getName(input), node);
                    main.setMaxLinkInput(1);
                    main.addConnectionProperty(FLOW_REF, FLOW_REF.getRGB(), FLOW_REF.getDefaultLineStyle());
                    main.addConnectionProperty(FLOW_MERGE, FLOW_MERGE.getRGB(), FLOW_MERGE.getDefaultLineStyle());
                    mains.add(main);
                    existingTypes.add(getType(input));
                }); //

        detail.getOutputFlows().stream() //
                .filter(output -> FLOW_MAIN.equals(getType(output))) //
                .forEach(output -> { //
                    INodeConnector main = createConnector(getType(output), getName(output), node);
                    main.setMaxLinkOutput(1);
                    main.addConnectionProperty(FLOW_REF, FLOW_REF.getRGB(), FLOW_REF.getDefaultLineStyle());
                    main.addConnectionProperty(FLOW_MERGE, FLOW_MERGE.getRGB(), FLOW_MERGE.getDefaultLineStyle());
                    mains.add(main);
                    existingTypes.add(getType(output));
                }); //

        return mains;
    }

    /**
     * Processor component may have outgoing Reject connections
     */
    @Override
    protected Optional<INodeConnector> createRejectConnector() {
        return detail.getOutputFlows().stream() //
                .filter(output -> REJECT.equals(getType(output))) //
                .findFirst() //
                .map(output -> { //
                    INodeConnector reject = createConnector(getType(output), getName(output), node);
                    reject.setMaxLinkOutput(1);
                    reject.addConnectionProperty(EConnectionType.FLOW_MAIN, new RGB(255, 0, 0), 2);
                    reject.getConnectionProperty(EConnectionType.FLOW_MAIN).setRGB(new RGB(255, 0, 0));
                    existingTypes.add(getType(output));
                    return reject;
                }); //
    }

    /**
     * Processor component has 0 incoming and infinite outgoing iterate connections
     */
    @Override
    protected INodeConnector createIterateConnector() {
        INodeConnector iterate = createConnector(ITERATE, ITERATE.getName(), node);
        iterate.setMinLinkInput(0);
        iterate.setMaxLinkInput(0);
        iterate.setMinLinkOutput(0);
        iterate.setMaxLinkOutput(-1);
        existingTypes.add(ITERATE);
        return iterate;
    }

}
