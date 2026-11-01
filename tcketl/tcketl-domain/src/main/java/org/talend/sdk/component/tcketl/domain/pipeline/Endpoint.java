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

/** One end of a link: a flow of a node. The flow name is the server one (for example {@code __default__}). */
public record Endpoint(NodeId node, String flow) {

    public Endpoint {
        Preconditions.requireNonNull(node, "node");
        Preconditions.requireNonBlank(flow, "flow");
    }

    @Override
    public String toString() {
        return node + "." + flow;
    }
}
