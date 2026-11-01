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
package org.talend.sdk.component.tcketl.domain.connector;

import java.util.List;

/** Connector definitions covering the shapes the server can describe. */
public final class TestConnectors {

    private static final String MAIN = Port.DEFAULT_FLOW;

    private TestConnectors() {
        // static helpers only
    }

    public static ConnectorRef ref(final String name) {
        return new ConnectorRef(new ConnectorId("id-" + name), "family", name, "plugin", 1);
    }

    public static ConnectorDefinition definition(final String name, final List<String> inputFlows,
            final List<String> outputFlows) {
        return new ConnectorDefinition(ref(name), "Display of " + name, inputFlows, outputFlows, false, false, "");
    }

    public static ConnectorDefinition input(final String name) {
        return definition(name, List.of(), List.of(MAIN));
    }

    /** An input that may stay unlinked. */
    public static ConnectorDefinition optionalInput(final String name) {
        return new ConnectorDefinition(ref(name), name, List.of(), List.of(MAIN), false, true, "");
    }

    /** Main and reject outputs. */
    public static ConnectorDefinition processor(final String name) {
        return definition(name, List.of(MAIN), List.of(MAIN, Port.REJECT_FLOW));
    }

    /** Two input flows, one output flow. */
    public static ConnectorDefinition join(final String name) {
        return definition(name, List.of(MAIN, "lookup"), List.of(MAIN));
    }

    public static ConnectorDefinition output(final String name) {
        return definition(name, List.of(MAIN), List.of());
    }

    public static ConnectorDefinition standalone(final String name) {
        return definition(name, List.of(), List.of());
    }
}
