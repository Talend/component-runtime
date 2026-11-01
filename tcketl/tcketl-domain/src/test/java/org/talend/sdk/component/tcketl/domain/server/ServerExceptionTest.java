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
import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.IOException;

import org.junit.jupiter.api.Test;

class ServerExceptionTest {

    @Test
    void messageCombinesStatusCodeAndDescription() {
        final ServerException exception = new ServerException(404, "COMPONENT_MISSING", "No component with this id");

        assertEquals("HTTP 404 COMPONENT_MISSING: No component with this id", exception.getMessage());
        assertEquals(404, exception.status());
        assertEquals("COMPONENT_MISSING", exception.code());
        assertEquals("No component with this id", exception.description());
    }

    @Test
    void messageToleratesMissingParts() {
        assertEquals("HTTP 500", new ServerException(500, null, null).getMessage());
        assertEquals("HTTP 500: boom", new ServerException(500, "", "boom").getMessage());
        assertEquals("Component server error", new ServerException(0, null, "  ").getMessage());
        assertEquals("", new ServerException(0, null, null).code());
    }

    @Test
    void unavailableHasNoStatusAndKeepsTheCause() {
        final IOException cause = new IOException("Connection refused");

        final ServerUnavailableException exception =
                new ServerUnavailableException("Cannot reach http://localhost:8080", cause);

        assertEquals(0, exception.status());
        assertEquals("UNAVAILABLE", exception.code());
        assertEquals("UNAVAILABLE: Cannot reach http://localhost:8080", exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    @Test
    void protocolErrorsKeepTheHttpStatus() {
        final ServerProtocolException exception = new ServerProtocolException(200, "Not a JSON document", null);

        assertEquals(200, exception.status());
        assertEquals("PROTOCOL", exception.code());
        assertEquals("HTTP 200 PROTOCOL: Not a JSON document", exception.getMessage());
    }
}
