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
package org.talend.sdk.component.tcketl.app.testing;

import java.util.List;

import org.talend.sdk.component.tcketl.domain.connector.ConnectorDefinition;
import org.talend.sdk.component.tcketl.domain.connector.ConnectorId;
import org.talend.sdk.component.tcketl.domain.connector.ConnectorRef;
import org.talend.sdk.component.tcketl.domain.connector.ConnectorRole;
import org.talend.sdk.component.tcketl.domain.connector.ConnectorSummary;
import org.talend.sdk.component.tcketl.domain.connector.Port;

/** Palette entries and definitions for the tests. */
public final class Connectors {

    private Connectors() {
        // static helpers only
    }

    /** A connector of the family {@code family}, which is also its display name. */
    public static ConnectorSummary summary(final String family, final String name, final ConnectorRole role) {
        return summary(family, family, name, List.of(), role);
    }

    public static ConnectorSummary summary(final String familyId, final String familyDisplayName,
            final String displayName, final List<String> categories, final ConnectorRole role) {
        final ConnectorRef ref =
                new ConnectorRef(new ConnectorId(familyId + "/" + displayName), familyId, displayName, "plugin", 1);
        return new ConnectorSummary(ref, familyId, familyDisplayName, displayName, categories, role, false, "");
    }

    /** What the server says about {@code summary}, with the flows that give it its real role. */
    public static ConnectorDefinition definition(final ConnectorSummary summary, final List<String> inputFlows,
            final List<String> outputFlows) {
        return new ConnectorDefinition(summary.ref(), summary.displayName(), inputFlows, outputFlows, false, false,
                "");
    }

    public static ConnectorDefinition asInput(final ConnectorSummary summary) {
        return definition(summary, List.of(), List.of(Port.DEFAULT_FLOW));
    }

    public static ConnectorDefinition asOutput(final ConnectorSummary summary) {
        return definition(summary, List.of(Port.DEFAULT_FLOW), List.of());
    }
}
