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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.talend.sdk.component.tcketl.domain.pipeline.EditorSupport.main;
import static org.talend.sdk.component.tcketl.domain.pipeline.EditorSupport.node;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class PipelineTest {

    @Test
    void emptyPipelineHasTheDefaultName() {
        final Pipeline pipeline = Pipeline.empty();

        assertEquals(Pipeline.DEFAULT_NAME, pipeline.name());
        assertTrue(pipeline.nodes().isEmpty());
        assertTrue(pipeline.links().isEmpty());
    }

    @Test
    void blankNameFallsBackToTheDefault() {
        assertEquals(Pipeline.DEFAULT_NAME, new Pipeline(" ", List.of(), List.of()).name());
        assertEquals(Pipeline.DEFAULT_NAME, new Pipeline(null, List.of(), List.of()).name());
    }

    @Test
    void duplicateNodeIdsAreRefused() {
        assertThrows(IllegalArgumentException.class,
                () -> new Pipeline("p", List.of(node("a"), node("a")), List.of()));
    }

    @Test
    void linksMustReferToKnownNodes() {
        final Link dangling = main(node("a").id(), new NodeId("missing"));

        assertThrows(IllegalArgumentException.class, () -> new Pipeline("p", List.of(node("a")), List.of(dangling)));
    }

    @Test
    void duplicateLinksAreRefused() {
        final Link link = main(node("a").id(), node("b").id());

        assertThrows(IllegalArgumentException.class,
                () -> new Pipeline("p", List.of(node("a"), node("b")), List.of(link, link)));
    }

    @Test
    void lookupsFollowTheLinks() {
        final NodeId a = node("a").id();
        final NodeId b = node("b").id();
        final NodeId c = node("c").id();
        final Link ab = main(a, b);
        final Link bc = main(b, c);
        final Pipeline pipeline = new Pipeline("p", List.of(node("a"), node("b"), node("c")), List.of(ab, bc));

        assertEquals(List.of(ab), pipeline.outgoing(a));
        assertEquals(List.of(ab), pipeline.incoming(b));
        assertEquals(List.of(bc), pipeline.outgoing(b));
        assertTrue(pipeline.incoming(a).isEmpty());
        assertTrue(pipeline.outgoing(c).isEmpty());
        assertEquals("b", pipeline.node(b).orElseThrow().label());
        assertTrue(pipeline.node(new NodeId("zzz")).isEmpty());
    }

    @Test
    void listsAreCopiedAndUnmodifiable() {
        final List<Node> nodes = new ArrayList<>(List.of(node("a")));
        final Pipeline pipeline = new Pipeline("p", nodes, List.of());

        nodes.add(node("b"));

        assertEquals(1, pipeline.nodes().size());
        assertThrows(UnsupportedOperationException.class, () -> pipeline.nodes().add(node("c")));
        assertThrows(UnsupportedOperationException.class, () -> pipeline.links().clear());
    }
}
