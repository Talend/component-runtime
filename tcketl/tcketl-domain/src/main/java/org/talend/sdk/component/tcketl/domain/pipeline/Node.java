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

import java.util.Map;

import org.talend.sdk.component.tcketl.domain.Preconditions;
import org.talend.sdk.component.tcketl.domain.connector.ConnectorRef;

/**
 * A connector instance placed on the canvas. {@code configuration} is the flat TCK configuration map
 * ({@code a.b.c=value}); the component version is kept in {@code connector} and never inside the map.
 */
public record Node(
        NodeId id,
        String label,
        ConnectorRef connector,
        Position position,
        Map<String, String> configuration
) {

    public Node {
        Preconditions.requireNonNull(id, "id");
        Preconditions.requireNonBlank(label, "label");
        Preconditions.requireNonNull(connector, "connector");
        Preconditions.requireNonNull(position, "position");
        configuration = Preconditions.sortedCopy(configuration, "configuration");
    }

    public Node withLabel(final String newLabel) {
        return new Node(id, newLabel, connector, position, configuration);
    }

    public Node withPosition(final Position newPosition) {
        return new Node(id, label, connector, newPosition, configuration);
    }

    public Node withConfiguration(final Map<String, String> newConfiguration) {
        return new Node(id, label, connector, position, newConfiguration);
    }
}
