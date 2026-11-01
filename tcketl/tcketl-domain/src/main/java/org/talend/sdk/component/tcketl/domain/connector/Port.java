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

import org.talend.sdk.component.tcketl.domain.Preconditions;

/** A connection point of a connector. The flow name is the server one and is always used verbatim in links. */
public record Port(String flow, Direction direction) {

    public static final String DEFAULT_FLOW = "__default__";

    public static final String REJECT_FLOW = "REJECT";

    private static final String DEFAULT_LABEL = "main";

    public enum Direction {
        INPUT,
        OUTPUT
    }

    public Port {
        Preconditions.requireNonBlank(flow, "flow");
        Preconditions.requireNonNull(direction, "direction");
    }

    public static Port input(final String flow) {
        return new Port(flow, Direction.INPUT);
    }

    public static Port output(final String flow) {
        return new Port(flow, Direction.OUTPUT);
    }

    public boolean isDefault() {
        return DEFAULT_FLOW.equals(flow);
    }

    /** Display hint only: the match ignores the case because the flow name is chosen by the component author. */
    public boolean isReject() {
        return REJECT_FLOW.equalsIgnoreCase(flow);
    }

    public String label() {
        return isDefault() ? DEFAULT_LABEL : flow;
    }
}
