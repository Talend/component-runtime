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
package org.talend.sdk.component.tcketl.app.pipeline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.tcketl.app.session.ServerSession;
import org.talend.sdk.component.tcketl.app.testing.Connectors;
import org.talend.sdk.component.tcketl.app.testing.FakeCatalog;
import org.talend.sdk.component.tcketl.app.testing.FakeConnection;
import org.talend.sdk.component.tcketl.app.testing.TestAsync;
import org.talend.sdk.component.tcketl.domain.connector.ConnectorDefinition;
import org.talend.sdk.component.tcketl.domain.connector.ConnectorRef;
import org.talend.sdk.component.tcketl.domain.connector.ConnectorRole;
import org.talend.sdk.component.tcketl.domain.connector.ConnectorSummary;
import org.talend.sdk.component.tcketl.domain.connector.Port;
import org.talend.sdk.component.tcketl.domain.pipeline.Node;
import org.talend.sdk.component.tcketl.domain.pipeline.NodeId;
import org.talend.sdk.component.tcketl.domain.pipeline.Pipeline;
import org.talend.sdk.component.tcketl.domain.pipeline.PipelineEditor;
import org.talend.sdk.component.tcketl.domain.pipeline.Position;
import org.talend.sdk.component.tcketl.domain.pipeline.Resolution;

class NodeResolverTest {

    private static final NodeId FIRST = new NodeId("n-1");

    private static final NodeId SECOND = new NodeId("n-2");

    private static final NodeId THIRD = new NodeId("n-3");

    private final TestAsync async = new TestAsync();

    private final PipelineEditor editor = new PipelineEditor();

    private final ServerSession session = new ServerSession(async.runner());

    private final NodeResolver resolver = new NodeResolver(editor, session, async.runner());

    private final ConnectorSummary reader = Connectors.summary("files", "Reader", ConnectorRole.INPUT);

    private final ConnectorSummary writer = Connectors.summary("files", "Writer", ConnectorRole.OUTPUT);

    private final FakeCatalog catalog = new FakeCatalog()
            .withConnectors(List.of(reader, writer))
            .define(Connectors.asInput(reader))
            .define(Connectors.asOutput(writer));

    @AfterEach
    void close() {
        resolver.close();
    }

    @Test
    void anOpenedPipelineIsResolvedWithOneCallForTheDistinctConnectors() {
        connectAndSettle(catalog);

        editor.load(pipeline(node(FIRST, reader), node(SECOND, writer), node(THIRD, reader)));
        async.drain();

        assertEquals(List.of(List.of(reader.id(), writer.id())), catalog.batches());
        assertEquals(Connectors.asInput(reader), editor.definition(FIRST).orElseThrow());
        assertEquals(Connectors.asOutput(writer), editor.definition(SECOND).orElseThrow());
        assertEquals(Connectors.asInput(reader), editor.definition(THIRD).orElseThrow());
    }

    @Test
    void aDroppedNodeIsResolvedButANodeThatHasItsDefinitionIsNot() {
        connectAndSettle(catalog);

        editor.addNode(Connectors.asOutput(writer), new Position(40, 40));
        async.drain();
        assertEquals(List.of(), catalog.batches());

        final NodeId dropped = editor.addNode(reader.ref(), null, new Position(200, 40));
        assertSame(Resolution.PENDING, editor.resolution(dropped));
        async.drain();

        assertEquals(List.of(List.of(reader.id())), catalog.batches());
        assertEquals(Connectors.asInput(reader), editor.definition(dropped).orElseThrow());
    }

    @Test
    void aConnectorTheServerDoesNotKnowIsLeftUnresolvedWithTheServerNamed() {
        connectAndSettle(new FakeCatalog().withConnectors(List.of(reader)).define(Connectors.asInput(reader)));

        editor.load(pipeline(node(FIRST, reader), node(SECOND, writer)));
        async.drain();

        assertTrue(editor.definition(FIRST).isPresent());
        assertEquals(new Resolution.Unresolved("it is not deployed on http://localhost:9001/api/v1"),
                editor.resolution(SECOND));
        assertEquals(2, editor.pipeline().nodes().size());
    }

    @Test
    void withoutAnyServerTheNodesSayNoServerIsConnected() {
        editor.load(pipeline(node(FIRST, reader)));

        assertEquals(new Resolution.Unresolved(NodeResolver.NO_SERVER), editor.resolution(FIRST));
    }

    @Test
    void anUnreachableServerLeavesTheNodesUnresolvedUntilTheServerIsBack() {
        connectAndSettle(catalog.unreachable("Connection refused"));
        editor.load(pipeline(node(FIRST, reader)));
        async.drain();
        assertEquals(new Resolution.Unresolved("Connection refused"), editor.resolution(FIRST));

        catalog.working();
        session.reload();
        async.drain();

        assertEquals(Connectors.asInput(reader), editor.definition(FIRST).orElseThrow());
    }

    @Test
    void aNodeKeepsItsDefinitionWhenTheServerCannotBeReached() {
        connectAndSettle(catalog);
        editor.load(pipeline(node(FIRST, reader)));
        async.drain();

        catalog.unreachable("Connection refused");
        resolver.resolveAll();
        async.drain();

        assertEquals(Connectors.asInput(reader), editor.definition(FIRST).orElseThrow());
    }

    @Test
    void aServerThatChangesTheDefinitionIsFollowed() {
        connectAndSettle(catalog);
        editor.load(pipeline(node(FIRST, reader)));
        async.drain();
        final ConnectorDefinition moreOutputs =
                Connectors.definition(reader, List.of(), List.of(Port.DEFAULT_FLOW, "second"));
        catalog.define(moreOutputs);

        session.refresh();
        async.drain();

        assertEquals(moreOutputs, editor.definition(FIRST).orElseThrow());
    }

    @Test
    void theAnswerForAPipelineThatWasReplacedIsDropped() {
        connectAndSettle(catalog);
        editor.load(pipeline(node(FIRST, reader)));
        editor.load(pipeline(node(FIRST, writer)));

        async.drain();

        assertEquals(Connectors.asOutput(writer), editor.definition(FIRST).orElseThrow());
    }

    @Test
    void theAnswerOfAServerThatWasLeftIsDropped() {
        final FakeCatalog other = new FakeCatalog()
                .withConnectors(List.of(reader))
                .define(Connectors.definition(reader, List.of(), List.of("other")));
        connect(catalog);
        editor.load(pipeline(node(FIRST, reader)));
        session.connect(FakeConnection.onPort(9002, other));

        while (catalog.batches().isEmpty()) {
            async.io().runNext();
        }
        async.ui().runNext();

        assertSame(Resolution.PENDING, editor.resolution(FIRST));
        async.drain();
        assertEquals(List.of("other"), editor.definition(FIRST).orElseThrow().outputFlows());
    }

    @Test
    void aClosedResolverLeavesTheNodesAlone() {
        connectAndSettle(catalog);

        resolver.close();
        editor.load(pipeline(node(FIRST, reader)));
        editor.addNode(reader.ref(), null, new Position(40, 40));
        resolver.resolveAll();
        async.drain();

        assertEquals(List.of(), catalog.batches());
        assertSame(Resolution.PENDING, editor.resolution(FIRST));
    }

    @Test
    void aNodeSavedWithAnOlderVersionIsMigratedByTheServer() {
        session.connect(FakeConnection
                .onPort(9001, catalog)
                .withMigrator((id, from, configuration) -> Map.of("migrated.from", Integer.toString(from))));
        async.drain();
        final ConnectorRef old = new ConnectorRef(reader.id(), reader.ref().family(), reader.ref().name(),
                reader.ref().plugin(), 0);

        editor.load(new Pipeline("test", List.of(new Node(FIRST, "r", old, new Position(40, 40), Map.of("a", "b"))),
                List.of()));
        async.drain();

        final Node migrated = editor.node(FIRST).orElseThrow();
        assertEquals(1, migrated.connector().version());
        assertEquals(Map.of("migrated.from", "0"), migrated.configuration());
    }

    @Test
    void aNodeSavedWithANewerVersionIsLeftUntouched() {
        session.connect(FakeConnection.onPort(9001, catalog).withMigrator((id, from, configuration) -> {
            throw new AssertionError("must not migrate");
        }));
        async.drain();
        final ConnectorRef newer = new ConnectorRef(reader.id(), reader.ref().family(), reader.ref().name(),
                reader.ref().plugin(), 7);

        editor.load(new Pipeline("test", List.of(new Node(FIRST, "r", newer, new Position(40, 40), Map.of("a", "b"))),
                List.of()));
        async.drain();

        assertEquals(7, editor.node(FIRST).orElseThrow().connector().version());
        assertEquals(Map.of("a", "b"), editor.node(FIRST).orElseThrow().configuration());
    }

    private void connect(final FakeCatalog server) {
        session.connect(FakeConnection.onPort(9001, server));
    }

    /** The state of an application that has been running for a while: connected, nothing left to deliver. */
    private void connectAndSettle(final FakeCatalog server) {
        connect(server);
        async.drain();
    }

    private static Pipeline pipeline(final Node... nodes) {
        return new Pipeline("test", List.of(nodes), List.of());
    }

    private static Node node(final NodeId id, final ConnectorSummary connector) {
        return new Node(id, connector.displayName(), connector.ref(), new Position(40, 40), Map.of());
    }
}
