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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class ServerTimeoutsTest {

    @Test
    void anActionMayTakeLongerThanACatalogRequest() {
        final ServerTimeouts defaults = ServerTimeouts.defaults();

        assertEquals(Duration.ofSeconds(5), defaults.connect());
        assertEquals(Duration.ofSeconds(30), defaults.request());
        assertEquals(Duration.ofSeconds(60), defaults.action());
    }

    @Test
    void everyTimeoutMustBePositive() {
        final Duration ok = Duration.ofSeconds(1);

        assertThrows(IllegalArgumentException.class, () -> new ServerTimeouts(Duration.ZERO, ok, ok));
        assertThrows(IllegalArgumentException.class, () -> new ServerTimeouts(ok, Duration.ofMillis(-1), ok));
        assertThrows(IllegalArgumentException.class, () -> new ServerTimeouts(ok, ok, Duration.ofSeconds(-5)));
        assertThrows(NullPointerException.class, () -> new ServerTimeouts(ok, ok, null));
    }
}
