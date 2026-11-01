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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.tcketl.app.testing.Connectors;
import org.talend.sdk.component.tcketl.domain.connector.ConnectorRole;
import org.talend.sdk.component.tcketl.domain.connector.Port;
import org.talend.sdk.component.tcketl.domain.pipeline.Link;
import org.talend.sdk.component.tcketl.domain.pipeline.NodeId;
import org.talend.sdk.component.tcketl.domain.pipeline.Resolution;

class NodePortsTest {

    private static final NodeId NODE = new NodeId("n-1");

    private static final NodeId OTHER = new NodeId("n-2");

    private final Resolution filter = new Resolution.Resolved(
            Connectors
                    .definition(Connectors.summary("files", "Filter", ConnectorRole.PROCESSOR),
                            List.of(Port.DEFAULT_FLOW, "lookup"), List.of(Port.DEFAULT_FLOW, Port.REJECT_FLOW)));

    @Test
    void aResolvedNodeShowsTheFlowsOfItsConnectorInOrder() {
        final NodePorts ports = NodePorts.of(NODE, filter, List.of());

        assertEquals(List.of(declared(Port.input(Port.DEFAULT_FLOW)), declared(Port.input("lookup"))),
                ports.inputs());
        assertEquals(List.of(declared(Port.output(Port.DEFAULT_FLOW)), declared(Port.output(Port.REJECT_FLOW))),
                ports.outputs());
    }

    @Test
    void aFlowOnlyLinksUseIsShownOnceAfterTheDeclaredOnesAsUndeclared() {
        final List<Link> links = List
                .of(Link.of(OTHER, "out", NODE, "gone"), Link.of(OTHER, "other", NODE, "gone"),
                        Link.of(NODE, Port.REJECT_FLOW, OTHER, "in"), Link.of(NODE, "stale", OTHER, "in2"));

        final NodePorts ports = NodePorts.of(NODE, filter, links);

        assertEquals(List
                .of(declared(Port.input(Port.DEFAULT_FLOW)), declared(Port.input("lookup")),
                        new NodePorts.Entry(Port.input("gone"), false)),
                ports.inputs());
        assertEquals(List
                .of(declared(Port.output(Port.DEFAULT_FLOW)), declared(Port.output(Port.REJECT_FLOW)),
                        new NodePorts.Entry(Port.output("stale"), false)),
                ports.outputs());
    }

    @Test
    void aNodeWhoseConnectorIsNotLoadedShowsTheFlowsItsLinksUseOnly() {
        final List<Link> links = List
                .of(Link.of(OTHER, Port.DEFAULT_FLOW, NODE, Port.DEFAULT_FLOW),
                        Link.of(NODE, Port.DEFAULT_FLOW, OTHER, "in"));

        for (final Resolution resolution : List.of(Resolution.PENDING, new Resolution.Unresolved("gone"))) {
            final NodePorts ports = NodePorts.of(NODE, resolution, links);

            assertEquals(List.of(new NodePorts.Entry(Port.input(Port.DEFAULT_FLOW), false)), ports.inputs());
            assertEquals(List.of(new NodePorts.Entry(Port.output(Port.DEFAULT_FLOW), false)), ports.outputs());
            assertEquals(new NodePorts(List.of(), List.of()),
                    NodePorts.of(NODE, resolution, List.of(Link.of(OTHER, "a", OTHER, "b"))));
        }
    }

    private static NodePorts.Entry declared(final Port port) {
        return new NodePorts.Entry(port, true);
    }
}
