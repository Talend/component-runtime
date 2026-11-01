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

import java.util.List;
import java.util.Map;

/**
 * Change notification fired by the {@link PipelineEditor} once its state is updated, so a listener can query the
 * editor from the callback.
 */
public sealed interface PipelineEvent {

    /** The whole content was replaced. */
    record Reset(Pipeline pipeline) implements PipelineEvent {
    }

    record Renamed(String name) implements PipelineEvent {
    }

    record NodeAdded(Node node) implements PipelineEvent {
    }

    /** The links attached to the removed node are removed with it and listed here. */
    record NodeRemoved(Node node, List<Link> removedLinks) implements PipelineEvent {

        public NodeRemoved {
            removedLinks = List.copyOf(removedLinks);
        }
    }

    record NodeMoved(NodeId node, Position position) implements PipelineEvent {
    }

    record NodeRelabeled(NodeId node, String label) implements PipelineEvent {
    }

    record NodeConfigured(NodeId node, Map<String, String> configuration) implements PipelineEvent {
    }

    record NodeResolutionChanged(NodeId node, Resolution resolution) implements PipelineEvent {
    }

    record LinkAdded(Link link) implements PipelineEvent {
    }

    record LinkRemoved(Link link) implements PipelineEvent {
    }
}
