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
package org.talend.sdk.component.tcketl.domain.server;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.talend.sdk.component.tcketl.domain.connector.ConnectorDefinition;
import org.talend.sdk.component.tcketl.domain.connector.ConnectorId;
import org.talend.sdk.component.tcketl.domain.connector.ConnectorSummary;

/**
 * Read access to the components deployed on a server. The calls are blocking and MUST NOT be made from the JavaFX
 * application thread. The language, the endpoint and the timeouts are settings of the implementation. A failure to
 * obtain an answer is reported with a {@link ServerException}.
 */
public interface ComponentCatalog {

    ServerStatus status();

    List<ConnectorSummary> connectors();

    /** Definitions of the requested connectors, restricted to those the server knows. */
    Map<ConnectorId, ConnectorDefinition> connectors(Collection<ConnectorId> ids);

    default Optional<ConnectorDefinition> connector(final ConnectorId id) {
        return Optional.ofNullable(connectors(List.of(id)).get(id));
    }
}
