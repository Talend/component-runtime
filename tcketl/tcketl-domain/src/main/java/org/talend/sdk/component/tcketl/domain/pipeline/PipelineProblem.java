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

import org.talend.sdk.component.tcketl.domain.Preconditions;

/**
 * Non-blocking finding about a pipeline. {@code node} is {@code null} for pipeline-wide problems and {@code flow} is
 * empty when the problem does not concern a port.
 */
public record PipelineProblem(Severity severity, Kind kind, NodeId node, String flow, String message) {

    public enum Severity {
        WARNING,
        ERROR
    }

    public enum Kind {
        NO_INPUT_NODE,
        INPUT_NOT_CONNECTED,
        INPUT_PORT_NOT_CONNECTED,
        NODE_UNRESOLVED,
        LINK_FLOW_UNKNOWN
    }

    public PipelineProblem {
        Preconditions.requireNonNull(severity, "severity");
        Preconditions.requireNonNull(kind, "kind");
        flow = Preconditions.nullToEmpty(flow);
        Preconditions.requireNonBlank(message, "message");
    }

    public boolean hasNode() {
        return node != null;
    }
}
