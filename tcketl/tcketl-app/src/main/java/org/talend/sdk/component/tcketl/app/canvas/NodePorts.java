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
package org.talend.sdk.component.tcketl.app.canvas;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.talend.sdk.component.tcketl.domain.connector.Port;
import org.talend.sdk.component.tcketl.domain.pipeline.Link;
import org.talend.sdk.component.tcketl.domain.pipeline.NodeId;
import org.talend.sdk.component.tcketl.domain.pipeline.Resolution;

/**
 * The ports a node shows: the flows its connector declares, then the flows its links use without the connector
 * declaring them. The latter also cover a node whose connector is not loaded: a link stays visible, so it can be
 * selected and deleted, whatever the state of the node it leads to.
 *
 * @param inputs the input ports, from the top
 * @param outputs the output ports, from the top
 */
public record NodePorts(List<Entry> inputs, List<Entry> outputs) {

    /** {@code declared} is false for a flow that only a link refers to. */
    public record Entry(Port port, boolean declared) {
    }

    public NodePorts {
        inputs = List.copyOf(inputs);
        outputs = List.copyOf(outputs);
    }

    public static NodePorts of(final NodeId node, final Resolution resolution, final Collection<Link> links) {
        final List<String> declaredInputs =
                resolution instanceof Resolution.Resolved resolved ? resolved.definition().inputFlows() : List.of();
        final List<String> declaredOutputs =
                resolution instanceof Resolution.Resolved resolved ? resolved.definition().outputFlows() : List.of();
        final Set<String> linkedInputs = new LinkedHashSet<>();
        final Set<String> linkedOutputs = new LinkedHashSet<>();
        for (final Link link : links) {
            if (link.to().node().equals(node)) {
                linkedInputs.add(link.to().flow());
            }
            if (link.from().node().equals(node)) {
                linkedOutputs.add(link.from().flow());
            }
        }
        return new NodePorts(entries(declaredInputs, linkedInputs, Port::input),
                entries(declaredOutputs, linkedOutputs, Port::output));
    }

    private static List<Entry> entries(final List<String> declared, final Set<String> linked,
            final Function<String, Port> port) {
        final List<Entry> entries = new ArrayList<>();
        declared.forEach(flow -> entries.add(new Entry(port.apply(flow), true)));
        linked
                .stream()
                .filter(flow -> !declared.contains(flow))
                .forEach(flow -> entries.add(new Entry(port.apply(flow), false)));
        return entries;
    }
}
