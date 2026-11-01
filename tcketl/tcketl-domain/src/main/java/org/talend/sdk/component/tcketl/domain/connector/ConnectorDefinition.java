/**
 * Copyright (C) 2006-2026 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.tcketl.domain.connector;

import java.util.List;
import java.util.stream.Collectors;

import org.talend.sdk.component.tcketl.domain.Preconditions;
import org.talend.sdk.component.tcketl.domain.property.ActionDefinition;
import org.talend.sdk.component.tcketl.domain.property.PropertyDefinition;

/**
 * Component definition as served by the component details endpoint. {@code ref.version()} is the current
 * configuration version of the component on the server. {@code optionalRow} tells that an input may stay unlinked.
 * {@code properties} is the flat list of configuration properties in server order and {@code actions} the services
 * the component references.
 */
public record ConnectorDefinition(
        ConnectorRef ref,
        String displayName,
        List<String> inputFlows,
        List<String> outputFlows,
        boolean streaming,
        boolean optionalRow,
        String documentation,
        List<PropertyDefinition> properties,
        List<ActionDefinition> actions
) {

    public ConnectorDefinition {
        Preconditions.requireNonNull(ref, "ref");
        displayName = Preconditions.orDefault(displayName, ref.name());
        inputFlows = Preconditions.distinctNonBlank(inputFlows, "inputFlows");
        outputFlows = Preconditions.distinctNonBlank(outputFlows, "outputFlows");
        documentation = Preconditions.nullToEmpty(documentation);
        properties = Preconditions.immutableCopy(properties, "properties");
        actions = Preconditions.immutableCopy(actions, "actions");
    }

    /** A definition that has no configuration property and no action. */
    public ConnectorDefinition(final ConnectorRef ref, final String displayName, final List<String> inputFlows,
            final List<String> outputFlows, final boolean streaming, final boolean optionalRow,
            final String documentation) {
        this(ref, displayName, inputFlows, outputFlows, streaming, optionalRow, documentation, List.of(), List.of());
    }

    public ConnectorId id() {
        return ref.id();
    }

    public ConnectorRole role() {
        return ConnectorRole.fromFlows(inputFlows, outputFlows);
    }

    public List<Port> inputPorts() {
        return inputFlows.stream().map(Port::input).collect(Collectors.toUnmodifiableList());
    }

    public List<Port> outputPorts() {
        return outputFlows.stream().map(Port::output).collect(Collectors.toUnmodifiableList());
    }

    public boolean hasInputFlow(final String flow) {
        return inputFlows.contains(flow);
    }

    public boolean hasOutputFlow(final String flow) {
        return outputFlows.contains(flow);
    }
}
