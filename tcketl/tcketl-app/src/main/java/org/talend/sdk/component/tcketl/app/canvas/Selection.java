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

import org.talend.sdk.component.tcketl.domain.Preconditions;
import org.talend.sdk.component.tcketl.domain.pipeline.Link;
import org.talend.sdk.component.tcketl.domain.pipeline.NodeId;

/** What is selected on the canvas: nothing, one node or one link. */
public sealed interface Selection {

    None NONE = new None();

    default boolean selects(final NodeId id) {
        return this instanceof OfNode node && node.id().equals(id);
    }

    default boolean selects(final Link link) {
        return this instanceof OfLink selected && selected.link().equals(link);
    }

    record None() implements Selection {
    }

    record OfNode(NodeId id) implements Selection {

        public OfNode {
            Preconditions.requireNonNull(id, "id");
        }
    }

    record OfLink(Link link) implements Selection {

        public OfLink {
            Preconditions.requireNonNull(link, "link");
        }
    }
}
