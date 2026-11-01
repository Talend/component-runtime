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
package org.talend.sdk.component.tcketl.domain.pipeline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.talend.sdk.component.tcketl.domain.pipeline.EditorSupport.ORIGIN;
import static org.talend.sdk.component.tcketl.domain.pipeline.EditorSupport.add;
import static org.talend.sdk.component.tcketl.domain.pipeline.EditorSupport.main;
import static org.talend.sdk.component.tcketl.domain.pipeline.EditorSupport.node;
import static org.talend.sdk.component.tcketl.domain.pipeline.EditorSupport.recordEvents;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.tcketl.domain.connector.ConnectorDefinition;
import org.talend.sdk.component.tcketl.domain.connector.TestConnectors;

class PipelineEditorTest {

    private final PipelineEditor editor = new PipelineEditor("demo");

    @Test
    void newEditorIsEmptyAndNamed() {
        assertEquals("demo", editor.pipeline().name());
        assertTrue(editor.pipeline().nodes().isEmpty());
        assertEquals(0, editor.revision());
        assertEquals(Pipeline.DEFAULT_NAME, new PipelineEditor().pipeline().name());
        assertEquals(Pipeline.DEFAULT_NAME, new PipelineEditor("  ").pipeline().name());
    }

    @Test
    void aNodeAddedFromADefinitionIsResolvedAndLabelledWithItsDisplayName() {
        final List<PipelineEvent> events = recordEvents(editor);
        final ConnectorDefinition reader = TestConnectors.input("Reader");

        final NodeId id = editor.addNode(reader, new Position(10, 20));

        final Node node = editor.node(id).orElseThrow();
        assertEquals(new NodeId("n-1"), id);
        assertEquals("Display of Reader", node.label());
        assertEquals(new Position(10, 20), node.position());
        assertEquals(reader.ref(), node.connector());
        assertEquals(new Resolution.Resolved(reader), editor.resolution(id));
        assertEquals(Optional.of(reader), editor.definition(id));
        assertEquals(List.of(new PipelineEvent.NodeAdded(node)), events);
    }

    @Test
    void aNodeAddedFromAReferenceIsPendingAndLabelledWithTheConnectorName() {
        final NodeId unnamed = editor.addNode(TestConnectors.ref("Reader"), null, ORIGIN);
        final NodeId named = editor.addNode(TestConnectors.ref("Reader"), "Custom", ORIGIN);

        assertEquals("Reader", editor.node(unnamed).orElseThrow().label());
        assertEquals("Custom", editor.node(named).orElseThrow().label());
        assertEquals(Resolution.PENDING, editor.resolution(unnamed));
        assertTrue(editor.definition(unnamed).isEmpty());
    }

    @Test
    void generatedIdsAreSequentialAndNeverReused() {
        final NodeId first = add(editor, TestConnectors.input("A"));
        final NodeId second = add(editor, TestConnectors.input("B"));
        editor.removeNode(second);
        final NodeId third = add(editor, TestConnectors.input("C"));

        assertEquals(List.of("n-1", "n-2", "n-3"), List.of(first.value(), second.value(), third.value()));
    }

    @Test
    void loadedIdsAdvanceTheCounter() {
        editor.load(new Pipeline("loaded", List.of(node("n-7"), node("custom"), node("n-123456789012")), List.of()));

        assertEquals("n-8", add(editor, TestConnectors.input("A")).value());
    }

    @Test
    void everyEditFiresItsEventAndBumpsTheRevision() {
        final NodeId reader = add(editor, TestConnectors.input("Reader"));
        final NodeId sink = add(editor, TestConnectors.output("Sink"));
        final Link link = main(reader, sink);
        final List<PipelineEvent> events = recordEvents(editor);
        final long before = editor.revision();

        editor.rename("renamed");
        editor.moveNode(reader, new Position(5, 6));
        editor.setLabel(reader, "  My reader ");
        editor.setConfiguration(reader, Map.of("a", "1"));
        editor.connect(link);
        editor.disconnect(link);

        assertEquals(List
                .of(new PipelineEvent.Renamed("renamed"), new PipelineEvent.NodeMoved(reader, new Position(5, 6)),
                        new PipelineEvent.NodeRelabeled(reader, "My reader"),
                        new PipelineEvent.NodeConfigured(reader, Map.of("a", "1")), new PipelineEvent.LinkAdded(link),
                        new PipelineEvent.LinkRemoved(link)),
                events);
        assertEquals(before + 6, editor.revision());
        assertEquals("renamed", editor.pipeline().name());
    }

    @Test
    void unchangedEditsAreSilent() {
        final NodeId id = add(editor, TestConnectors.input("Reader"));
        editor.setConfiguration(id, Map.of("a", "1"));
        final List<PipelineEvent> events = recordEvents(editor);
        final long revision = editor.revision();

        editor.rename("demo");
        editor.moveNode(id, ORIGIN);
        editor.setLabel(id, "Display of Reader");
        editor.setConfiguration(id, new TreeMap<>(Map.of("a", "1")));

        assertFalse(editor.disconnect(main(id, new NodeId("zzz"))));
        assertTrue(events.isEmpty());
        assertEquals(revision, editor.revision());
    }

    @Test
    void resolutionChangesAreNotPersistedContent() {
        final NodeId id = editor.addNode(TestConnectors.ref("Reader"), null, ORIGIN);
        final List<PipelineEvent> events = recordEvents(editor);
        final long revision = editor.revision();
        final ConnectorDefinition reader = TestConnectors.input("Reader");

        editor.bind(id, reader);
        editor.bind(id, reader);
        editor.markUnresolved(id, "gone");

        assertEquals(List
                .of(new PipelineEvent.NodeResolutionChanged(id, new Resolution.Resolved(reader)),
                        new PipelineEvent.NodeResolutionChanged(id, new Resolution.Unresolved("gone"))),
                events);
        assertEquals(revision, editor.revision());
    }

    @Test
    void bindRefusesTheDefinitionOfAnotherConnector() {
        final NodeId id = editor.addNode(TestConnectors.ref("Reader"), null, ORIGIN);

        assertThrows(IllegalArgumentException.class, () -> editor.bind(id, TestConnectors.input("Other")));
        assertEquals(Resolution.PENDING, editor.resolution(id));
    }

    @Test
    void unknownNodesAreReported() {
        final NodeId unknown = new NodeId("zzz");

        assertTrue(editor.node(unknown).isEmpty());
        assertTrue(editor.definition(unknown).isEmpty());
        assertThrows(NoSuchElementException.class, () -> editor.resolution(unknown));
        assertThrows(NoSuchElementException.class, () -> editor.moveNode(unknown, ORIGIN));
        assertThrows(NoSuchElementException.class, () -> editor.setLabel(unknown, "x"));
        assertThrows(NoSuchElementException.class, () -> editor.setConfiguration(unknown, Map.of()));
        assertThrows(NoSuchElementException.class, () -> editor.removeNode(unknown));
        assertThrows(NoSuchElementException.class, () -> editor.markUnresolved(unknown, "x"));
        assertThrows(NoSuchElementException.class, () -> editor.bind(unknown, TestConnectors.input("A")));
    }

    @Test
    void blankNamesAndLabelsAreRefused() {
        final NodeId id = add(editor, TestConnectors.input("Reader"));

        assertThrows(IllegalArgumentException.class, () -> editor.setLabel(id, " "));
        assertThrows(IllegalArgumentException.class, () -> editor.rename(" "));
        assertEquals("Display of Reader", editor.node(id).orElseThrow().label());
        assertEquals("demo", editor.pipeline().name());
    }

    @Test
    void configurationIsStoredAsASortedCopy() {
        final NodeId id = add(editor, TestConnectors.input("Reader"));
        final Map<String, String> configuration = new HashMap<>();
        configuration.put("b", "2");
        configuration.put("a", "1");

        editor.setConfiguration(id, configuration);
        configuration.put("c", "3");

        assertEquals(List.of("a", "b"), List.copyOf(editor.node(id).orElseThrow().configuration().keySet()));
    }

    @Test
    void removingANodeRemovesItsLinks() {
        final NodeId reader = add(editor, TestConnectors.input("Reader"));
        final NodeId proc = add(editor, TestConnectors.processor("Proc"));
        final NodeId sink = add(editor, TestConnectors.output("Sink"));
        final Link in = main(reader, proc);
        final Link out = main(proc, sink);
        editor.connect(in);
        editor.connect(out);
        final Node removedNode = editor.node(proc).orElseThrow();
        final List<PipelineEvent> events = recordEvents(editor);
        final long revision = editor.revision();

        final List<Link> removed = editor.removeNode(proc);

        assertEquals(List.of(in, out), removed);
        assertEquals(List.of(new PipelineEvent.NodeRemoved(removedNode, List.of(in, out))), events);
        assertTrue(editor.pipeline().links().isEmpty());
        assertEquals(List.of(reader, sink), editor.pipeline().nodes().stream().map(Node::id).toList());
        assertTrue(editor.node(proc).isEmpty());
        assertEquals(revision + 1, editor.revision());
    }

    @Test
    void theSnapshotIsReusedUntilTheNextChange() {
        final Pipeline before = editor.pipeline();
        assertSame(before, editor.pipeline());

        add(editor, TestConnectors.input("Reader"));

        assertNotSame(before, editor.pipeline());
        assertEquals(1, editor.pipeline().nodes().size());
        assertTrue(before.nodes().isEmpty());
    }

    @Test
    void loadReplacesTheContentAndForgetsTheResolutions() {
        final NodeId old = add(editor, TestConnectors.input("Old"));
        final Pipeline stored =
                new Pipeline("stored", List.of(node("a"), node("b")), List.of(main(new NodeId("a"), new NodeId("b"))));
        final List<PipelineEvent> events = recordEvents(editor);

        editor.load(stored);

        assertEquals(stored, editor.pipeline());
        assertEquals(List.of(new PipelineEvent.Reset(stored)), events);
        assertTrue(editor.node(old).isEmpty());
        assertEquals(Resolution.PENDING, editor.resolution(new NodeId("a")));
        assertEquals(Resolution.PENDING, editor.resolution(new NodeId("b")));
    }

    @Test
    void listenersSeeTheUpdatedState() {
        final List<Integer> nodeCounts = new ArrayList<>();
        editor.addListener(event -> nodeCounts.add(editor.pipeline().nodes().size()));

        final NodeId id = add(editor, TestConnectors.input("Reader"));
        editor.removeNode(id);

        assertEquals(List.of(1, 0), nodeCounts);
    }

    @Test
    void aFailingListenerBlocksNeitherTheOthersNorTheEdit() {
        final List<PipelineEvent> events = new ArrayList<>();
        editor.addListener(event -> {
            throw new IllegalStateException("expected by the test");
        });
        editor.addListener(events::add);

        final NodeId id = add(editor, TestConnectors.input("Reader"));

        assertEquals(1, events.size());
        assertTrue(editor.node(id).isPresent());
    }

    @Test
    void removedListenersAreNoLongerNotified() {
        final List<PipelineEvent> events = new ArrayList<>();
        final PipelineListener listener = events::add;
        editor.addListener(listener);
        editor.removeListener(listener);

        add(editor, TestConnectors.input("Reader"));

        assertTrue(events.isEmpty());
    }
}
