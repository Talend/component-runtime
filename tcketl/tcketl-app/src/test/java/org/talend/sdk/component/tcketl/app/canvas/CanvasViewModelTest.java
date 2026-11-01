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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.tcketl.app.session.ServerSession;
import org.talend.sdk.component.tcketl.app.testing.Connectors;
import org.talend.sdk.component.tcketl.app.testing.FakeCatalog;
import org.talend.sdk.component.tcketl.app.testing.FakeConnection;
import org.talend.sdk.component.tcketl.app.testing.TestAsync;
import org.talend.sdk.component.tcketl.domain.connector.ConnectorDefinition;
import org.talend.sdk.component.tcketl.domain.connector.ConnectorId;
import org.talend.sdk.component.tcketl.domain.connector.ConnectorRole;
import org.talend.sdk.component.tcketl.domain.connector.ConnectorSummary;
import org.talend.sdk.component.tcketl.domain.connector.Port;
import org.talend.sdk.component.tcketl.domain.pipeline.Endpoint;
import org.talend.sdk.component.tcketl.domain.pipeline.Link;
import org.talend.sdk.component.tcketl.domain.pipeline.LinkRejection;
import org.talend.sdk.component.tcketl.domain.pipeline.Node;
import org.talend.sdk.component.tcketl.domain.pipeline.NodeId;
import org.talend.sdk.component.tcketl.domain.pipeline.Pipeline;
import org.talend.sdk.component.tcketl.domain.pipeline.PipelineEditor;
import org.talend.sdk.component.tcketl.domain.pipeline.Position;
import org.talend.sdk.component.tcketl.domain.pipeline.Resolution;

class CanvasViewModelTest {

    private static final String MAIN = Port.DEFAULT_FLOW;

    private static final NodeId UNKNOWN = new NodeId("n-99");

    private final TestAsync async = new TestAsync();

    private final PipelineEditor editor = new PipelineEditor();

    private final ServerSession session = new ServerSession(async.runner());

    private final CanvasViewModel model = new CanvasViewModel(editor, session);

    private final ConnectorSummary reader = Connectors.summary("files", "Reader", ConnectorRole.INPUT);

    private final ConnectorSummary filter = Connectors.summary("files", "Filter", ConnectorRole.PROCESSOR);

    private final ConnectorSummary writer = Connectors.summary("files", "Writer", ConnectorRole.OUTPUT);

    @Test
    void aDroppedConnectorBecomesASelectedPendingNodeSnappedToTheGrid() {
        listConnectors();
        assertTrue(model.empty().get());

        final NodeId id = model.dropConnector(reader.id(), new Position(113, 47)).orElseThrow();

        final Node node = editor.node(id).orElseThrow();
        assertEquals(new Position(120, 40), node.position());
        assertEquals("Reader", node.label());
        assertSame(Resolution.PENDING, editor.resolution(id));
        assertEquals(new Selection.OfNode(id), model.selection().get());
        assertFalse(model.empty().get());
    }

    @Test
    void aConnectorTheServerDoesNotListIsNotAddedAndTheNextEditClearsTheSentence() {
        listConnectors();

        assertEquals(Optional.empty(), model.dropConnector(new ConnectorId("missing"), new Position(40, 40)));

        assertTrue(model.empty().get());
        assertFalse(model.feedback().get().isBlank());
        model.addConnector(reader);
        assertEquals("", model.feedback().get());
    }

    @Test
    void nodesAddedWithoutAPositionGetDistinctLabelsAndCascadingSpots() {
        model.addConnector(reader);
        model.addConnector(reader);
        model.addConnector(filter);

        final List<Node> nodes = editor.pipeline().nodes();
        assertEquals(List.of("Reader", "Reader 2", "Filter"), nodes.stream().map(Node::label).toList());
        assertEquals(List.of(new Position(40, 40), new Position(80, 80), new Position(120, 120)),
                nodes.stream().map(Node::position).toList());
    }

    @Test
    void aMoveSnapsToTheGridAndKeepsTheNodeOnTheCanvas() {
        final NodeId id = model.addConnector(reader);

        model.moveNode(id, new Position(133, 149));
        assertEquals(new Position(140, 140), editor.node(id).orElseThrow().position());

        model.moveNode(id, new Position(-50, 3));
        assertEquals(new Position(20, 20), editor.node(id).orElseThrow().position());

        model.moveNode(UNKNOWN, new Position(100, 100));
        assertEquals(1, editor.pipeline().nodes().size());
    }

    @Test
    void aBlankNameDoesNotRenameANode() {
        final NodeId id = model.addConnector(reader);

        assertFalse(model.renameNode(id, "  "));
        assertEquals("Reader", editor.node(id).orElseThrow().label());
        assertTrue(model.renameNode(id, " Customers "));
        assertEquals("Customers", editor.node(id).orElseThrow().label());
        assertFalse(model.renameNode(UNKNOWN, "Anything"));
    }

    @Test
    void deletingTheSelectedNodeRemovesItsLinksToo() {
        final NodeId source = add(Connectors.asInput(reader), 40, 40);
        final NodeId target = add(Connectors.asOutput(writer), 240, 40);
        editor.connect(Link.of(source, MAIN, target, MAIN));
        model.select(source);

        model.deleteSelection();

        assertEquals(List.of(target), editor.pipeline().nodes().stream().map(Node::id).toList());
        assertEquals(List.of(), editor.pipeline().links());
        assertEquals(Selection.NONE, model.selection().get());
    }

    @Test
    void deletingTheSelectedLinkKeepsTheNodes() {
        final NodeId source = add(Connectors.asInput(reader), 40, 40);
        final NodeId target = add(Connectors.asOutput(writer), 240, 40);
        final Link link = Link.of(source, MAIN, target, MAIN);
        editor.connect(link);
        model.select(link);

        model.deleteSelection();

        assertEquals(2, editor.pipeline().nodes().size());
        assertEquals(List.of(), editor.pipeline().links());
        assertEquals(Selection.NONE, model.selection().get());
    }

    @Test
    void whatIsNotInThePipelineCannotBeSelected() {
        final NodeId source = add(Connectors.asInput(reader), 40, 40);
        final NodeId target = add(Connectors.asOutput(writer), 240, 40);
        editor.connect(Link.of(source, MAIN, target, MAIN));

        model.select(UNKNOWN);
        model.select(Link.of(target, MAIN, source, MAIN));

        assertEquals(Selection.NONE, model.selection().get());
    }

    @Test
    void aSelectionDoesNotOutliveItsNodeItsLinkOrThePipeline() {
        final NodeId source = add(Connectors.asInput(reader), 40, 40);
        final NodeId target = add(Connectors.asOutput(writer), 240, 40);
        final Link link = Link.of(source, MAIN, target, MAIN);
        editor.connect(link);

        model.select(link);
        editor.disconnect(link);
        assertEquals(Selection.NONE, model.selection().get());

        editor.connect(link);
        model.select(link);
        editor.removeNode(source);
        assertEquals(Selection.NONE, model.selection().get());

        model.select(target);
        editor.load(Pipeline.empty());
        assertEquals(Selection.NONE, model.selection().get());
    }

    @Test
    void aLinkOnlyStartsFromAnOutputOfALoadedConnector() {
        final NodeId pending = model.addConnector(reader);
        final NodeId source = add(Connectors.asInput(reader), 200, 40);

        assertFalse(model.beginLink(new Endpoint(pending, MAIN)));
        assertEquals(LinkRejection.NODE_NOT_RESOLVED.message(), model.feedback().get());
        assertFalse(model.beginLink(new Endpoint(source, "nope")));
        assertEquals(LinkRejection.UNKNOWN_OUTPUT_FLOW.message(), model.feedback().get());
        assertNull(model.linkSource().get());

        assertTrue(model.beginLink(new Endpoint(source, MAIN)));
        assertEquals(new Endpoint(source, MAIN), model.linkSource().get());
        assertEquals("", model.feedback().get());
    }

    @Test
    void hoveringAPortTellsWhetherTheLinkWouldBeMade() {
        final NodeId source = add(Connectors.asInput(reader), 40, 40);
        final Endpoint output = new Endpoint(source, MAIN);
        final Endpoint input = new Endpoint(add(Connectors.asOutput(writer), 240, 40), MAIN);
        assertTrue(model.check(input).isPresent());

        model.beginLink(output);
        assertEquals(Optional.empty(), model.check(input));
        model.hover(input);
        assertEquals("", model.feedback().get());

        model.hover(output);
        assertEquals(LinkRejection.SELF_LINK.message(), model.feedback().get());
        assertEquals(Optional.of(LinkRejection.SELF_LINK), model.check(output));

        model.hover(null);
        assertEquals("", model.feedback().get());
    }

    @Test
    void releasingOnACompatibleInputMakesTheLinkAndEndsTheGesture() {
        final NodeId source = add(Connectors.asInput(reader), 40, 40);
        final NodeId target = add(Connectors.asOutput(writer), 240, 40);
        model.beginLink(new Endpoint(source, MAIN));

        assertTrue(model.completeLink(new Endpoint(target, MAIN)));

        assertEquals(List.of(Link.of(source, MAIN, target, MAIN)), editor.pipeline().links());
        assertNull(model.linkSource().get());
        assertEquals("", model.feedback().get());
        assertEquals(Selection.NONE, model.selection().get());
    }

    @Test
    void releasingOnAPortThatBreaksARuleMakesNoLinkAndSaysWhy() {
        final NodeId first = add(Connectors.asInput(reader), 40, 40);
        final NodeId second = add(Connectors.asInput(reader), 40, 140);
        final NodeId sink = add(Connectors.asOutput(writer), 240, 40);
        editor.connect(Link.of(first, MAIN, sink, MAIN));
        model.beginLink(new Endpoint(second, MAIN));

        assertFalse(model.completeLink(new Endpoint(sink, MAIN)));

        assertEquals(1, editor.pipeline().links().size());
        assertEquals(LinkRejection.INPUT_PORT_TAKEN.message(), model.feedback().get());
        assertNull(model.linkSource().get());
    }

    @Test
    void releasingOnNothingCancelsTheLink() {
        final NodeId source = add(Connectors.asInput(reader), 40, 40);
        model.beginLink(new Endpoint(source, MAIN));

        assertFalse(model.completeLink(null));

        assertNull(model.linkSource().get());
        assertEquals("", model.feedback().get());
        assertFalse(model.completeLink(new Endpoint(source, MAIN)));
    }

    @Test
    void aLinkBeingDrawnDoesNotOutliveItsSourceNode() {
        final NodeId removed = add(Connectors.asInput(reader), 40, 40);
        final NodeId reloaded = add(Connectors.asInput(reader), 40, 140);
        final NodeId kept = add(Connectors.asInput(reader), 40, 240);

        model.beginLink(new Endpoint(removed, MAIN));
        model.deleteNode(removed);
        assertNull(model.linkSource().get());

        model.beginLink(new Endpoint(reloaded, MAIN));
        editor.markUnresolved(reloaded, "gone");
        assertNull(model.linkSource().get());

        model.beginLink(new Endpoint(kept, MAIN));
        editor.load(Pipeline.empty());
        assertNull(model.linkSource().get());
    }

    @Test
    void theProblemsFollowTheEdits() {
        assertEquals(List.of(), model.problems());

        final NodeId processor = add(Connectors.definition(filter, List.of(MAIN), List.of(MAIN)), 40, 40);
        assertFalse(editor.problems().isEmpty());
        assertEquals(editor.problems(), model.problems());

        editor.removeNode(processor);
        assertEquals(List.of(), model.problems());
    }

    @Test
    void theZoomStaysBetweenItsBounds() {
        for (int i = 0; i < 20; i++) {
            model.zoomIn();
        }
        assertEquals(CanvasViewModel.MAX_ZOOM, model.zoom().get());

        for (int i = 0; i < 40; i++) {
            model.zoomOut();
        }
        assertEquals(CanvasViewModel.MIN_ZOOM, model.zoom().get());

        model.zoomBy(Double.NaN);
        assertEquals(CanvasViewModel.MIN_ZOOM, model.zoom().get());
        model.resetZoom();
        assertEquals(1, model.zoom().get());
    }

    @Test
    void aNodeThatIsNotThereHasNoPorts() {
        assertEquals(new NodePorts(List.of(), List.of()), model.ports(UNKNOWN));

        final NodeId source = add(Connectors.asInput(reader), 40, 40);
        assertEquals(List.of(new NodePorts.Entry(Port.output(MAIN), true)), model.ports(source).outputs());
    }

    @Test
    void aClosedModelStopsFollowingTheEditor() {
        model.close();

        add(Connectors.asInput(reader), 40, 40);

        assertTrue(model.empty().get());
    }

    private NodeId add(final ConnectorDefinition definition, final double x, final double y) {
        return editor.addNode(definition, new Position(x, y));
    }

    private void listConnectors() {
        session
                .connect(FakeConnection
                        .onPort(9001, new FakeCatalog().withConnectors(List.of(reader, filter, writer))));
        async.drain();
    }
}
