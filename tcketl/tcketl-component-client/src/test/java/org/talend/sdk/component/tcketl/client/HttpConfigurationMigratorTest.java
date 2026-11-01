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
package org.talend.sdk.component.tcketl.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.tcketl.domain.connector.ConnectorId;
import org.talend.sdk.component.tcketl.domain.server.ServerException;
import org.talend.sdk.component.tcketl.domain.server.ServerTimeouts;

/** {@code POST /component/migrate/{id}/{version}}: the configuration sent and the one that comes back. */
class HttpConfigurationMigratorTest {

    @Test
    void theSavedMapIsPostedToTheVersionAndTheMigratedMapReturned() throws IOException {
        try (StubServer stub = StubServer.start()) {
            stub.on("/component/migrate/abc/1", StubServer.Reply.json(200, "{\"a\":\"2\",\"b\":null,\"c\":3}"));

            final Map<String, String> migrated = ComponentServerClient
                    .create(stub.endpoint(), ServerTimeouts.defaults(), "en")
                    .migrator()
                    .migrate(new ConnectorId("abc"), 1, Map.of("a", "1"));

            assertEquals(Map.of("a", "2", "c", "3"), migrated);
            assertEquals("POST", stub.requests().get(0).method());
            assertEquals("{\"a\":\"1\"}", stub.requests().get(0).body());
        }
    }

    @Test
    void aServerErrorIsReported() throws IOException {
        try (StubServer stub = StubServer.start()) {
            stub.on("/component/migrate/abc/1",
                    StubServer.Reply.json(520, "{\"code\":\"UNEXPECTED\",\"description\":\"x\"}"));

            assertThrows(ServerException.class, () -> ComponentServerClient
                    .create(stub.endpoint(), ServerTimeouts.defaults(), "en")
                    .migrator()
                    .migrate(new ConnectorId("abc"), 1, Map.of()));
        }
    }
}
