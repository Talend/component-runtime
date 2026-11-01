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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class ServerEndpointTest {

    @Test
    void defaultsPointToALocalServer() {
        assertEquals("http://localhost:8080/api/v1", ServerEndpoint.defaults().baseUri().toString());
        assertEquals("http://localhost:8080/api/v1", ServerEndpoint.defaults().toString());
    }

    @ParameterizedTest(name = "[{0}] is normalized to [{1}]")
    @CsvSource(value = { "api/v1/,/api/v1", "/api/v1//,/api/v1", "'  /tck/api/v1  ',/tck/api/v1", "/,''", "'',''" })
    void basePathIsNormalized(final String given, final String expected) {
        assertEquals(expected, new ServerEndpoint("http", "localhost", 8080, given).basePath());
    }

    @Test
    void anAbsentBasePathMeansTheRoot() {
        final ServerEndpoint endpoint = new ServerEndpoint("http", "localhost", 8080, null);

        assertEquals("", endpoint.basePath());
        assertEquals("http://localhost:8080", endpoint.baseUri().toString());
    }

    @Test
    void schemeIsCaseInsensitiveAndLimitedToHttp() {
        assertEquals("https://example.org:8443/tck/api/v1",
                new ServerEndpoint(" HTTPS ", "example.org", 8443, "/tck/api/v1").baseUri().toString());
        assertThrows(IllegalArgumentException.class, () -> new ServerEndpoint("ftp", "localhost", 21, "/"));
    }

    @ParameterizedTest
    @ValueSource(ints = { -1, 0, 65536 })
    void portMustBeInTheTcpRange(final int port) {
        assertThrows(IllegalArgumentException.class, () -> new ServerEndpoint("http", "localhost", port, "/"));
    }

    @Test
    void boundaryPortsAreAccepted() {
        assertEquals(1, new ServerEndpoint("http", "localhost", 1, "/").port());
        assertEquals(65535, new ServerEndpoint("http", "localhost", 65535, "/").port());
    }

    @ParameterizedTest
    @ValueSource(strings = { "", "  ", "bad host", "a/b", "http://localhost", "localhost:8080" })
    void hostMustBeUsable(final String host) {
        assertThrows(IllegalArgumentException.class, () -> new ServerEndpoint("http", host, 8080, "/api/v1"));
    }

    @Test
    void ipv6LiteralsGetTheirBrackets() {
        assertEquals("http://[::1]:8080/api/v1",
                new ServerEndpoint("http", "::1", 8080, "/api/v1").baseUri().toString());
    }
}
