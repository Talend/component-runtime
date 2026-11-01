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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.talend.sdk.component.tcketl.domain.connector.ConnectorDefinition;
import org.talend.sdk.component.tcketl.domain.connector.Port;
import org.talend.sdk.component.tcketl.domain.connector.TestConnectors;

/** Shortcuts shared by the pipeline tests. */
final class EditorSupport {

    static final Position ORIGIN = new Position(0, 0);

    private EditorSupport() {
        // static helpers only
    }

    static NodeId add(final PipelineEditor editor, final ConnectorDefinition definition) {
        return editor.addNode(definition, ORIGIN);
    }

    /** A node whose connector is the one of {@code TestConnectors.xxx(name)}. */
    static Node node(final String name) {
        return new Node(new NodeId(name), name, TestConnectors.ref(name), ORIGIN, Map.of());
    }

    /** Link between the default flows. */
    static Link main(final NodeId from, final NodeId to) {
        return Link.of(from, Port.DEFAULT_FLOW, to, Port.DEFAULT_FLOW);
    }

    /** Collects the events fired from now on. */
    static List<PipelineEvent> recordEvents(final PipelineEditor editor) {
        final List<PipelineEvent> events = new ArrayList<>();
        editor.addListener(events::add);
        return events;
    }

    static List<PipelineProblem.Kind> kinds(final List<PipelineProblem> problems) {
        return problems.stream().map(PipelineProblem::kind).toList();
    }
}
