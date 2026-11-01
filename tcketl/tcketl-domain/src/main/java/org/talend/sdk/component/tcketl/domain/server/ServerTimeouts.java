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

import java.time.Duration;

import org.talend.sdk.component.tcketl.domain.Preconditions;

/**
 * How long to wait for the server: to open a connection, to get the answer of a catalog request and to get the
 * answer of a service call (an action may run a real connection test on the server side).
 */
public record ServerTimeouts(Duration connect, Duration request, Duration action) {

    public ServerTimeouts {
        requirePositive(connect, "connect timeout");
        requirePositive(request, "request timeout");
        requirePositive(action, "action timeout");
    }

    public static ServerTimeouts defaults() {
        return new ServerTimeouts(Duration.ofSeconds(5), Duration.ofSeconds(30), Duration.ofSeconds(60));
    }

    private static void requirePositive(final Duration value, final String name) {
        Preconditions.requireNonNull(value, name);
        if (value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(name + " must be positive: " + value);
        }
    }
}
