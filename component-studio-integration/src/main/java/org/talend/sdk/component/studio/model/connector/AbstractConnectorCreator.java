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

import static org.talend.core.model.process.EConnectionType.ON_COMPONENT_ERROR;
import static org.talend.core.model.process.EConnectionType.ON_COMPONENT_OK;
import static org.talend.core.model.process.EConnectionType.ON_SUBJOB_ERROR;
import static org.talend.core.model.process.EConnectionType.ON_SUBJOB_OK;
import static org.talend.core.model.process.EConnectionType.RUN_IF;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.talend.core.CorePlugin;
import org.talend.core.model.process.EConnectionType;
import org.talend.core.model.process.INode;
import org.talend.core.model.process.INodeConnector;
import org.talend.designer.core.model.components.NodeConnector;
import org.talend.sdk.component.server.front.model.ComponentDetail;

/**
 * Common implementation for {@link ConnectorCreator}
 */
abstract class AbstractConnectorCreator implements ConnectorCreator {

    protected final ComponentDetail detail;

    protected final INode node;

    protected final Set<EConnectionType> existingTypes = new HashSet<>();

    public static final String MAIN_CONNECTOR_NAME = EConnectionType.FLOW_MAIN.getName();

    public static final String MAIN_CONNECTOR_DISPLAY_NAME = EConnectionType.FLOW_MAIN.getDefaultMenuName();

    public static final String MAIN_CONNECTOR_LINK_NAME = EConnectionType.FLOW_MAIN.getDefaultLinkName();

    protected AbstractConnectorCreator(final ComponentDetail detail, final INode node) {
        this.detail = detail;
        this.node = node;
    }

    /**
     * Maps connection name to {@link EConnectionType} It returns
     * {@link EConnectionType#FLOW_MAIN}, if <code>connectionName</code> is "Main"
     * It returns {@link EConnectionType#REJECT}. if <code>connectionName</code> is
     * "Reject" It returns {@link EConnectionType#FLOW_MAIN} for any other
     * <code>connectionName</code> including "__default__"
     * 
     * @param connectionName
     * name of this connection
     * @return {@link EConnectionType} of connection
     */
    protected static EConnectionType getType(final String connectionName) {
        if (connectionName == null) {
            throw new IllegalArgumentException("arg should not be null");
        }
        if ("Reject".equalsIgnoreCase(connectionName)) {
            return EConnectionType.REJECT;
        }
        return EConnectionType.FLOW_MAIN;
    }

    /**
     * Returns "Main", if connectionName is "__default__", else returns input
     * argument unchanged
     * 
     * @param connectionName
     * name of this connection
     * @return name, which should be used as INodeConnector name
     */
    protected static String getName(final String connectionName) {
        if (connectionName == null) {
            throw new IllegalArgumentException("arg should not be null");
        }
        if ("__default__".equals(connectionName)) {
            return MAIN_CONNECTOR_NAME;
        }
        return connectionName;
    }

    protected static INodeConnector createConnector(final EConnectionType type, final String name, final INode node) {
        return createConnector(type, name, node, type);
    }

    protected static INodeConnector createConnector(final EConnectionType type, final String name, final INode node,
            final EConnectionType defaultConnectionType) {
        final NodeConnector connector = new TaCoKitNodeConnector(node);
        connector.setName(name);
        connector.setBaseSchema(type.getName());
        connector.setDefaultConnectionType(defaultConnectionType);
        connector.setLinkName(name.equals(EConnectionType.FLOW_MAIN.getName()) ? MAIN_CONNECTOR_DISPLAY_NAME : name);
        connector.setMenuName(name.equals(EConnectionType.FLOW_MAIN.getName()) ? MAIN_CONNECTOR_DISPLAY_NAME : name);
        connector.addConnectionProperty(CorePlugin.getDefault() == null ? null : type, type.getRGB(),
                type.getDefaultLineStyle());
        connector.setMinLinkInput(0);
        connector.setMaxLinkInput(0);
        connector.setMinLinkOutput(0);
        connector.setMaxLinkOutput(0);
        return connector;
    }

    /**
     * Creates component connectors in following way:
     *
     * <ol>
     * <li>create Main connectors, it may have both incoming and outgoing connections</li>
     * <li>create optional Reject connector, it may have only outgoing connections</li>
     * <li>create Iterate connector</li>
     * <li>create standard for all components connectors: ON_COMPONENT_OK, ON_COMPONENT_ERROR, ON_SUBJOB_OK,
     * ON_SUBJOB_ERROR, RUN_IF</li>
     * <li>create all remaining connectors</li>
     * </ol>
     *
     * @return the connectors for the underlying component.
     */
    @Override
    public List<INodeConnector> createConnectors() {
        ArrayList<INodeConnector> connectors = new ArrayList<>();
        connectors.addAll(createMainConnectors());
        createRejectConnector().ifPresent(c -> connectors.add(c));
        connectors.add(createIterateConnector());
        connectors.addAll(createStandardConnectors());
        connectors.addAll(createRestConnectors());
        return connectors;
    }

    /**
     * Creates connectors of type {@link EConnectionType#FLOW_MAIN}
     * 
     * @return Main connectors
     */
    protected abstract List<INodeConnector> createMainConnectors();

    /**
     * Creates optional connector of type {@link EConnectionType#REJECT}
     * 
     * @return Reject connector
     */
    protected abstract Optional<INodeConnector> createRejectConnector();

    /**
     * Create connector of type {@link EConnectionType#ITERATE}
     * 
     * @return Iterate connector
     */
    protected abstract INodeConnector createIterateConnector();

    /**
     * Creates connectors common for all components
     * 
     * @return common connectors
     */
    protected final List<INodeConnector> createStandardConnectors() {
        existingTypes.addAll(Arrays.asList(RUN_IF, ON_COMPONENT_OK, ON_COMPONENT_ERROR, ON_SUBJOB_OK, ON_SUBJOB_ERROR));
        return Arrays.asList( //
                createConnector(RUN_IF, RUN_IF.getName(), node),
                createConnector(ON_COMPONENT_OK, ON_COMPONENT_OK.getName(), node),
                createConnector(ON_COMPONENT_ERROR, ON_COMPONENT_ERROR.getName(), node),
                createConnector(ON_SUBJOB_OK, ON_SUBJOB_OK.getName(), node),
                createConnector(ON_SUBJOB_ERROR, ON_SUBJOB_ERROR.getName(), node));
    }

    /**
     * Creates all remaining connectors
     * 
     * @return remaining connectors
     */
    protected final List<INodeConnector> createRestConnectors() {
        return Arrays
                .stream(EConnectionType.values()) //
                .filter(type -> !existingTypes.contains(type)) //
                .map(type -> {
                    INodeConnector connector = createConnector(type, type.getName(), node); //
                    if ((type == EConnectionType.PARALLELIZE) || (type == EConnectionType.SYNCHRONIZE)) { //
                        connector.setMaxLinkInput(1); //
                    } //
                    return connector;
                })
                .collect(Collectors.toList()); //
    }

}
