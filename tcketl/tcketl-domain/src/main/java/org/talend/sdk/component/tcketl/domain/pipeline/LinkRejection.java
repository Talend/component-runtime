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

/** Why a link is refused. The constants are declared in the order the rules are checked. */
public enum LinkRejection {

    UNKNOWN_NODE("One of the nodes is not in the pipeline"),
    SELF_LINK("A node cannot be linked to itself"),
    NODE_NOT_RESOLVED("The connector definition is not loaded yet"),
    SOURCE_HAS_NO_OUTPUT_PORT("The source connector has no output port"),
    TARGET_HAS_NO_INPUT_PORT("The target connector has no input port"),
    UNKNOWN_OUTPUT_FLOW("The source connector has no output flow with this name"),
    UNKNOWN_INPUT_FLOW("The target connector has no input flow with this name"),
    DUPLICATE_LINK("These ports are already linked"),
    INPUT_PORT_TAKEN("The input port already has an incoming link"),
    CYCLE("The link would create a cycle");

    private final String message;

    LinkRejection(final String message) {
        this.message = message;
    }

    public String message() {
        return message;
    }
}
