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
package org.talend.sdk.component.tcketl.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WebSettingsTest {

    private static final String DEFAULT = "http://tck.example.com:8080/api/v1";

    @Test
    void everyHostIsAllowedByDefault() {
        assertTrue(new WebSettings(DEFAULT, "", false).isAllowed("localhost"));
    }

    @Test
    void anExplicitListWinsAndTheDefaultServerIsAlwaysAllowed() {
        final WebSettings settings = new WebSettings(DEFAULT, "a.example.com, B.example.com", false);

        assertTrue(settings.isAllowed("b.EXAMPLE.com"));
        assertFalse(settings.isAllowed("c.example.com"));
        assertTrue(settings.isAllowed("tck.example.com"));
    }

    @Test
    void privateAddressesAreRefusedWhenBlocked() {
        final WebSettings settings = new WebSettings(DEFAULT, "", true);

        assertFalse(settings.isAllowed("localhost"));
        assertFalse(settings.isAllowed("127.0.0.1"));
        assertFalse(settings.isAllowed("169.254.169.254"));
        assertFalse(settings.isAllowed("10.1.2.3"));
        assertFalse(settings.isAllowed("192.168.0.10"));
        assertFalse(settings.isAllowed("0.0.0.0"));
        assertFalse(settings.isAllowed(""));
        assertTrue(settings.isAllowed("8.8.8.8"));
    }
}
