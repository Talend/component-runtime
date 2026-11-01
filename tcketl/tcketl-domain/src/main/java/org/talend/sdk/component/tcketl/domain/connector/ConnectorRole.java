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

import java.util.Collection;

/**
 * What a connector does in a pipeline. It is derived from the flows of the component definition because the
 * {@code type} of the component index is not reliable (sinks are reported as {@code processor}).
 */
public enum ConnectorRole {

    /** No input flow, at least one output flow: starts a branch. */
    INPUT,

    /** Both input and output flows. A component whose only output is {@code REJECT} is still a processor. */
    PROCESSOR,

    /** At least one input flow, no output flow: ends a branch. */
    OUTPUT,

    /** Neither input nor output flow: runs on its own and cannot be linked. */
    STANDALONE;

    public static ConnectorRole fromFlows(final Collection<String> inputFlows, final Collection<String> outputFlows) {
        final boolean withoutInput = inputFlows.isEmpty();
        final boolean withoutOutput = outputFlows.isEmpty();
        if (withoutInput && withoutOutput) {
            return STANDALONE;
        }
        if (withoutInput) {
            return INPUT;
        }
        if (withoutOutput) {
            return OUTPUT;
        }
        return PROCESSOR;
    }
}
