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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.talend.sdk.component.tcketl.domain.pipeline.EditorSupport.ORIGIN;
import static org.talend.sdk.component.tcketl.domain.pipeline.EditorSupport.add;
import static org.talend.sdk.component.tcketl.domain.pipeline.EditorSupport.main;
import static org.talend.sdk.component.tcketl.domain.pipeline.EditorSupport.recordEvents;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.tcketl.domain.connector.Port;
import org.talend.sdk.component.tcketl.domain.connector.TestConnectors;

class LinkRulesTest {

    private final PipelineEditor editor = new PipelineEditor();

    private final NodeId reader = add(editor, TestConnectors.input("Reader"));

    private final NodeId otherReader = add(editor, TestConnectors.input("Other"));

    private final NodeId first = add(editor, TestConnectors.processor("First"));

    private final NodeId second = add(editor, TestConnectors.processor("Second"));

    private final NodeId join = add(editor, TestConnectors.join("Join"));

    private final NodeId sink = add(editor, TestConnectors.output("Sink"));

    private final NodeId standalone = add(editor, TestConnectors.standalone("Job"));

    private Optional<LinkRejection> check(final NodeId from, final String outputFlow, final NodeId to,
            final String inputFlow) {
        return editor.checkLink(Link.of(from, outputFlow, to, inputFlow));
    }

    @Test
    void compatibleDefaultFlowsAreAccepted() {
        assertEquals(Optional.empty(), editor.checkLink(main(reader, first)));

        editor.connect(main(reader, first));

        assertEquals(List.of(main(reader, first)), editor.pipeline().links());
    }

    @Test
    void anOutputCanFeedSeveralInputs() {
        editor.connect(main(reader, first));
        editor.connect(main(reader, second));
        editor.connect(main(reader, sink));

        assertEquals(3, editor.pipeline().links().size());
    }

    @Test
    void theRejectFlowIsAnOrdinaryOutput() {
        editor.connect(Link.of(first, Port.REJECT_FLOW, sink, Port.DEFAULT_FLOW));

        assertEquals(1, editor.pipeline().links().size());
    }

    @Test
    void aJoinTakesOneLinkPerInputFlow() {
        editor.connect(main(reader, join));
        editor.connect(Link.of(otherReader, Port.DEFAULT_FLOW, join, "lookup"));

        assertEquals(Optional.of(LinkRejection.INPUT_PORT_TAKEN), check(first, Port.DEFAULT_FLOW, join, "lookup"));
    }

    @Test
    void unknownNodesAreRejected() {
        final NodeId ghost = new NodeId("ghost");

        assertEquals(Optional.of(LinkRejection.UNKNOWN_NODE), editor.checkLink(main(ghost, first)));
        assertEquals(Optional.of(LinkRejection.UNKNOWN_NODE), editor.checkLink(main(reader, ghost)));
    }

    @Test
    void aNodeCannotBeLinkedToItself() {
        assertEquals(Optional.of(LinkRejection.SELF_LINK), editor.checkLink(main(first, first)));
    }

    @Test
    void nodesWithoutDefinitionCannotBeLinked() {
        final NodeId pending = editor.addNode(TestConnectors.ref("Later"), null, ORIGIN);

        assertEquals(Optional.of(LinkRejection.NODE_NOT_RESOLVED), editor.checkLink(main(reader, pending)));
        assertEquals(Optional.of(LinkRejection.NODE_NOT_RESOLVED), editor.checkLink(main(pending, first)));
        assertEquals(Optional.of(LinkRejection.SELF_LINK), editor.checkLink(main(pending, pending)));

        editor.markUnresolved(pending, "gone");

        assertEquals(Optional.of(LinkRejection.NODE_NOT_RESOLVED), editor.checkLink(main(reader, pending)));
    }

    @Test
    void sinksHaveNoOutputPortAndInputsNoInputPort() {
        assertEquals(Optional.of(LinkRejection.SOURCE_HAS_NO_OUTPUT_PORT), editor.checkLink(main(sink, first)));
        assertEquals(Optional.of(LinkRejection.TARGET_HAS_NO_INPUT_PORT), editor.checkLink(main(first, reader)));
    }

    @Test
    void standaloneConnectorsCannotBeLinkedAtAll() {
        assertEquals(Optional.of(LinkRejection.SOURCE_HAS_NO_OUTPUT_PORT), editor.checkLink(main(standalone, first)));
        assertEquals(Optional.of(LinkRejection.TARGET_HAS_NO_INPUT_PORT), editor.checkLink(main(first, standalone)));
    }

    @Test
    void flowNamesAreMatchedVerbatim() {
        assertEquals(Optional.of(LinkRejection.UNKNOWN_OUTPUT_FLOW), check(first, "reject", sink, Port.DEFAULT_FLOW));
        assertEquals(Optional.of(LinkRejection.UNKNOWN_OUTPUT_FLOW), check(reader, "nope", first, Port.DEFAULT_FLOW));
        assertEquals(Optional.of(LinkRejection.UNKNOWN_INPUT_FLOW), check(reader, Port.DEFAULT_FLOW, first, "MAIN"));
    }

    @Test
    void aDuplicateIsReportedBeforeATakenPort() {
        editor.connect(main(reader, first));

        assertEquals(Optional.of(LinkRejection.DUPLICATE_LINK), editor.checkLink(main(reader, first)));
        assertEquals(Optional.of(LinkRejection.INPUT_PORT_TAKEN), editor.checkLink(main(otherReader, first)));
    }

    @Test
    void connectThrowsTheRejection() {
        editor.connect(main(reader, first));

        final LinkRejectedException error =
                assertThrows(LinkRejectedException.class, () -> editor.connect(main(otherReader, first)));

        assertEquals(LinkRejection.INPUT_PORT_TAKEN, error.rejection());
        assertEquals(LinkRejection.INPUT_PORT_TAKEN.message(), error.getMessage());
    }

    @Test
    void aRefusedLinkLeavesTheEditorUntouched() {
        editor.connect(main(reader, first));
        final List<PipelineEvent> events = recordEvents(editor);
        final long revision = editor.revision();
        final Pipeline before = editor.pipeline();

        assertThrows(LinkRejectedException.class, () -> editor.connect(main(first, first)));

        assertTrue(events.isEmpty());
        assertEquals(revision, editor.revision());
        assertEquals(before, editor.pipeline());
    }

    @Test
    void aTwoNodeCycleIsRejectedWhateverTheOutputFlow() {
        editor.connect(main(first, second));

        assertEquals(Optional.of(LinkRejection.CYCLE), editor.checkLink(main(second, first)));
        assertEquals(Optional.of(LinkRejection.CYCLE),
                editor.checkLink(Link.of(second, Port.REJECT_FLOW, first, Port.DEFAULT_FLOW)));
    }

    @Test
    void aLongerCycleIsRejected() {
        final NodeId third = add(editor, TestConnectors.processor("Third"));
        editor.connect(main(first, second));
        editor.connect(main(second, third));

        assertEquals(Optional.of(LinkRejection.CYCLE), editor.checkLink(main(third, first)));
    }

    @Test
    void convergingBranchesAreNotCycles() {
        editor.connect(main(reader, first));
        editor.connect(main(first, join));
        editor.connect(main(reader, second));

        assertEquals(Optional.empty(), check(second, Port.DEFAULT_FLOW, join, "lookup"));
    }

    @Test
    void disconnectingFreesThePort() {
        editor.connect(main(reader, first));

        assertTrue(editor.disconnect(main(reader, first)));
        assertEquals(Optional.empty(), editor.checkLink(main(otherReader, first)));
        assertFalse(editor.disconnect(main(reader, first)));
    }

    @Test
    void removingANodeFreesThePortsItWasLinkedTo() {
        editor.connect(main(reader, first));

        editor.removeNode(reader);

        assertEquals(Optional.empty(), editor.checkLink(main(otherReader, first)));
    }
}
