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
package org.talend.sdk.component.tools.webapp.standalone.generator;

import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.tools.webapp.standalone.Route;

class StaticResourceGeneratorTest {

    @Test
    void routeWithNullContentHasEmptyBody() {
        final Route route = StaticResourceGenerator
                .route("component_server_environment", "/api/v1/cache/clear", new TreeMap<>(), emptyMap(), emptyMap(),
                        null);

        assertEquals(200, route.getStatus());
        assertEquals("/api/v1/cache/clear", route.getPath());
        assertArrayEquals(new byte[0], route.getContent());
    }

    @Test
    void routeWithContentKeepsUtf8Body() {
        final Route route = StaticResourceGenerator
                .route("component_server_environment", "/api/v1/environment", new TreeMap<>(), emptyMap(), emptyMap(),
                        "{\"a\":\"é\"}");

        assertArrayEquals("{\"a\":\"é\"}".getBytes(StandardCharsets.UTF_8), route.getContent());
    }
}
