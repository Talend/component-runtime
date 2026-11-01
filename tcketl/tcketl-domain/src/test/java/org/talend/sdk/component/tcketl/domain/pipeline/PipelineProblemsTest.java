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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.talend.sdk.component.tcketl.domain.pipeline.EditorSupport.ORIGIN;
import static org.talend.sdk.component.tcketl.domain.pipeline.EditorSupport.add;
import static org.talend.sdk.component.tcketl.domain.pipeline.EditorSupport.kinds;
import static org.talend.sdk.component.tcketl.domain.pipeline.EditorSupport.main;
import static org.talend.sdk.component.tcketl.domain.pipeline.EditorSupport.node;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.tcketl.domain.connector.TestConnectors;
import org.talend.sdk.component.tcketl.domain.pipeline.PipelineProblem.Kind;
import org.talend.sdk.component.tcketl.domain.pipeline.PipelineProblem.Severity;

class PipelineProblemsTest {

    private final PipelineEditor editor = new PipelineEditor();

    @Test
    void anEmptyPipelineHasNoProblem() {
        assertTrue(editor.problems().isEmpty());
    }

    @Test
    void aCompleteChainHasNoProblem() {
        final NodeId reader = add(editor, TestConnectors.input("Reader"));
        final NodeId proc = add(editor, TestConnectors.processor("Proc"));
        final NodeId sink = add(editor, TestConnectors.output("Sink"));
        editor.connect(main(reader, proc));
        editor.connect(main(proc, sink));

        assertEquals(List.of(), editor.problems());
    }

    @Test
    void aPipelineWithoutSourceIsReported() {
        final NodeId proc = add(editor, TestConnectors.processor("Proc"));

        final List<PipelineProblem> problems = editor.problems();

        assertEquals(List.of(Kind.INPUT_PORT_NOT_CONNECTED, Kind.NO_INPUT_NODE), kinds(problems));
        assertEquals(Severity.WARNING, problems.get(0).severity());
        assertEquals(proc, problems.get(0).node());
        assertEquals("__default__", problems.get(0).flow());
        assertFalse(problems.get(1).hasNode());
        assertEquals(Severity.WARNING, problems.get(1).severity());
    }

    @Test
    void anUnlinkedInputIsReportedUnlessItsRowIsOptional() {
        final NodeId mandatory = add(editor, TestConnectors.input("Reader"));

        final List<PipelineProblem> problems = editor.problems();

        assertEquals(List.of(Kind.INPUT_NOT_CONNECTED), kinds(problems));
        assertEquals(mandatory, problems.get(0).node());

        editor.removeNode(mandatory);
        add(editor, TestConnectors.optionalInput("Optional"));

        assertEquals(List.of(), editor.problems());
    }

    @Test
    void aStandaloneConnectorNeedsNoLinkAndCountsAsASource() {
        add(editor, TestConnectors.standalone("Job"));

        assertEquals(List.of(), editor.problems());
    }

    @Test
    void everyUnlinkedInputPortOfAJoinIsReported() {
        final NodeId reader = add(editor, TestConnectors.input("Reader"));
        final NodeId join = add(editor, TestConnectors.join("Join"));
        editor.connect(main(reader, join));

        final List<PipelineProblem> problems = editor.problems();

        assertEquals(List.of(Kind.INPUT_PORT_NOT_CONNECTED), kinds(problems));
        assertEquals(join, problems.get(0).node());
        assertEquals("lookup", problems.get(0).flow());
    }

    @Test
    void pendingNodesAreNotProblemsButDeferTheSourceCheck() {
        add(editor, TestConnectors.processor("Proc"));
        editor.addNode(TestConnectors.ref("Later"), null, ORIGIN);

        assertEquals(List.of(Kind.INPUT_PORT_NOT_CONNECTED), kinds(editor.problems()));
    }

    @Test
    void unresolvedNodesAreErrors() {
        final NodeId id = editor.addNode(TestConnectors.ref("Gone"), "My node", ORIGIN);
        editor.markUnresolved(id, "COMPONENT_MISSING");

        final List<PipelineProblem> problems = editor.problems();

        assertEquals(List.of(Kind.NODE_UNRESOLVED), kinds(problems));
        assertEquals(Severity.ERROR, problems.get(0).severity());
        assertEquals(id, problems.get(0).node());
        assertTrue(problems.get(0).message().contains("Gone"));
        assertTrue(problems.get(0).message().contains("COMPONENT_MISSING"));
    }

    @Test
    void problemsFollowTheCurrentDefinitions() {
        final Node reader = node("Reader");
        final Node sink = node("Sink");
        editor
                .load(new Pipeline("stored", List.of(reader, sink),
                        List.of(Link.of(reader.id(), "legacy", sink.id(), "__default__"))));
        editor.bind(reader.id(), TestConnectors.input("Reader"));
        editor.bind(sink.id(), TestConnectors.output("Sink"));

        final List<PipelineProblem> problems = editor.problems();

        assertEquals(List.of(Kind.LINK_FLOW_UNKNOWN), kinds(problems));
        assertEquals(Severity.ERROR, problems.get(0).severity());
        assertEquals(reader.id(), problems.get(0).node());
        assertEquals("legacy", problems.get(0).flow());

        editor.bind(reader.id(), TestConnectors.definition("Reader", List.of(), List.of("legacy")));

        assertEquals(List.of(), editor.problems());
    }

    @Test
    void aLinkToAnUnknownInputFlowIsAnError() {
        final Node reader = node("Reader");
        final Node proc = node("Proc");
        editor
                .load(new Pipeline("stored", List.of(reader, proc),
                        List.of(Link.of(reader.id(), "__default__", proc.id(), "legacy"))));
        editor.bind(reader.id(), TestConnectors.input("Reader"));
        editor.bind(proc.id(), TestConnectors.processor("Proc"));

        final List<PipelineProblem> problems = editor.problems();

        assertTrue(kinds(problems).contains(Kind.LINK_FLOW_UNKNOWN));
        final PipelineProblem unknown =
                problems.stream().filter(p -> p.kind() == Kind.LINK_FLOW_UNKNOWN).findFirst().orElseThrow();
        assertEquals(proc.id(), unknown.node());
        assertEquals("legacy", unknown.flow());
    }
}
