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
import org.talend.sdk.component.tcketl.domain.connector.ConnectorDefinition;

/**
 * Runtime state of a node: its connector definition is not part of the persisted pipeline, it is fetched from the
 * server after the node is created or loaded.
 */
public sealed interface Resolution {

    Pending PENDING = new Pending();

    /** The definition is being loaded. */
    record Pending() implements Resolution {
    }

    /** The definition is known: ports and properties are available. */
    record Resolved(ConnectorDefinition definition) implements Resolution {

        public Resolved {
            Preconditions.requireNonNull(definition, "definition");
        }
    }

    /** The definition cannot be obtained (missing component, server error): the node is kept untouched. */
    record Unresolved(String reason) implements Resolution {

        public Unresolved {
            reason = Preconditions.nullToEmpty(reason);
        }
    }
}
