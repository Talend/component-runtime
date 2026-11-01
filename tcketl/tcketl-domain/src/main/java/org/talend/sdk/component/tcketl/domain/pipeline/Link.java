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

/** Directed connection from an output flow ({@code from}) to an input flow ({@code to}). */
public record Link(Endpoint from, Endpoint to) {

    public Link {
        Preconditions.requireNonNull(from, "from");
        Preconditions.requireNonNull(to, "to");
    }

    public static Link of(final NodeId source, final String outputFlow, final NodeId target, final String inputFlow) {
        return new Link(new Endpoint(source, outputFlow), new Endpoint(target, inputFlow));
    }

    @Override
    public String toString() {
        return from + " -> " + to;
    }
}
