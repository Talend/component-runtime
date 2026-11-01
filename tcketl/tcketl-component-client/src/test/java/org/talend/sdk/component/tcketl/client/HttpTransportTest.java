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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.tcketl.domain.server.ServerEndpoint;
import org.talend.sdk.component.tcketl.domain.server.ServerException;
import org.talend.sdk.component.tcketl.domain.server.ServerProtocolException;
import org.talend.sdk.component.tcketl.domain.server.ServerTimeouts;
import org.talend.sdk.component.tcketl.domain.server.ServerUnavailableException;

class HttpTransportTest {

    @Test
    void callsAreMadeBelowTheBasePathWithAnEncodedQuery() throws IOException {
        try (StubServer stub = StubServer.start("/proxy/api/v1")) {
            stub.on("/component/details", StubServer.Reply.json(200, "{}"));
            final HttpTransport transport = new HttpTransport(stub.endpoint(), ServerTimeouts.defaults());

            final HttpTransport.Query query = new HttpTransport.Query()
                    .add("identifiers", "a b")
                    .add("identifiers", "\u00e9&=+")
                    .add("language", "en");
            final HttpTransport.Response response =
                    transport.getOrThrow(HttpTransport.JSON, query, "component", "details");
            transport.get(HttpTransport.JSON, new HttpTransport.Query(), "component", "a b/c");

            assertEquals(200, response.status());
            final StubServer.Request request = stub.requests().get(0);
            assertEquals("GET", request.method());
            assertEquals("/proxy/api/v1/component/details", request.uri().getRawPath());
            assertEquals("identifiers=a%20b&identifiers=%C3%A9%26%3D%2B&language=en", request.uri().getRawQuery());
            assertEquals("application/json", request.header("Accept"));
            assertEquals("gzip", request.header("Accept-Encoding"));
            assertEquals("/proxy/api/v1/component/a%20b%2Fc", stub.requests().get(1).uri().getRawPath());
        }
    }

    @Test
    void queryEncodingKeepsOnlyUnreservedCharacters() {
        assertEquals("AZaz09-._~", HttpTransport.Query.encode("AZaz09-._~"));
        assertEquals("a%20b%2Bc%26d%3De%2F%25", HttpTransport.Query.encode("a b+c&d=e/%"));
        assertEquals("%C3%A9%E2%82%AC", HttpTransport.Query.encode("\u00e9\u20ac"));
    }

    @Test
    void gzipAnswersAreInflated() throws IOException {
        try (StubServer stub = StubServer.start()) {
            stub.on("/environment", StubServer.Reply.json(200, "{\"version\":\"1.91.0\"}").gzipped());

            final HttpTransport.Response response = new HttpTransport(stub.endpoint(), ServerTimeouts.defaults())
                    .getOrThrow(HttpTransport.JSON, new HttpTransport.Query(), "environment");

            assertEquals("{\"version\":\"1.91.0\"}", new String(response.body(), StandardCharsets.UTF_8));
        }
    }

    @Test
    void corruptGzipIsAProtocolError() throws IOException {
        try (StubServer stub = StubServer.start()) {
            stub
                    .on("/environment",
                            new StubServer.Reply(200, HttpTransport.JSON, "plain".getBytes(StandardCharsets.UTF_8),
                                    Map.of("Content-Encoding", "gzip")));
            final HttpTransport transport = new HttpTransport(stub.endpoint(), ServerTimeouts.defaults());

            assertThrows(ServerProtocolException.class,
                    () -> transport.get(HttpTransport.JSON, new HttpTransport.Query(), "environment"));
        }
    }

    @Test
    void anErrorStatusCarriesTheServerPayload() throws IOException {
        try (StubServer stub = StubServer.start()) {
            stub
                    .on("/component/details",
                            StubServer.Reply
                                    .json(404,
                                            "{\"code\":\"COMPONENT_MISSING\",\"description\":\"No component 'x'\"}"));
            final HttpTransport transport = new HttpTransport(stub.endpoint(), ServerTimeouts.defaults());

            assertFalse(transport.get(HttpTransport.JSON, new HttpTransport.Query(), "component", "details")
                    .isSuccess());
            final ServerException error = assertThrows(ServerException.class,
                    () -> transport.getOrThrow(HttpTransport.JSON, new HttpTransport.Query(), "component", "details"));
            assertEquals(404, error.status());
            assertEquals("COMPONENT_MISSING", error.code());
            assertEquals("No component 'x'", error.description());
        }
    }

    @Test
    void anErrorWithoutPayloadIsUnexpectedAndShowsAnExcerptOfTheBody() {
        final ServerException page = new HttpTransport.Response(502,
                "<html>\n  <body>Bad   gateway</body>\n</html>".getBytes(StandardCharsets.UTF_8), "text/html")
                .toException();
        assertEquals(502, page.status());
        assertEquals("UNEXPECTED", page.code());
        assertEquals("<html> <body>Bad gateway</body> </html>", page.description());

        final ServerException unrelatedJson =
                new HttpTransport.Response(500, "{\"foo\":1}".getBytes(StandardCharsets.UTF_8), "application/json")
                        .toException();
        assertEquals("UNEXPECTED", unrelatedJson.code());

        final ServerException huge =
                new HttpTransport.Response(500, "x".repeat(500).getBytes(StandardCharsets.UTF_8), "text/plain")
                        .toException();
        assertEquals(203, huge.description().length());
        assertTrue(huge.description().endsWith("..."));
    }

    @Test
    void aServerThatDoesNotAnswerInTimeIsUnavailable() throws IOException {
        try (StubServer stub = StubServer.start()) {
            stub.on("/environment", request -> {
                Thread.sleep(5_000);
                return StubServer.Reply.json(200, "{}");
            });
            final ServerTimeouts timeouts =
                    new ServerTimeouts(Duration.ofSeconds(5), Duration.ofMillis(300), Duration.ofSeconds(5));

            final ServerUnavailableException error = assertThrows(ServerUnavailableException.class,
                    () -> new HttpTransport(stub.endpoint(), timeouts)
                            .get(HttpTransport.JSON, new HttpTransport.Query(), "environment"));

            assertEquals(0, error.status());
            assertEquals("UNAVAILABLE", error.code());
            assertEquals("Cannot reach " + stub.endpoint() + ": no answer within 300 ms", error.description());
        }
    }

    @Test
    void aRefusedConnectionIsUnavailable() throws IOException {
        final int port;
        try (ServerSocket socket = new ServerSocket(0, 1, InetAddress.getByName(StubServer.LOOPBACK))) {
            port = socket.getLocalPort();
        }
        final ServerEndpoint endpoint = new ServerEndpoint("http", StubServer.LOOPBACK, port, "/api/v1");

        final ServerUnavailableException error = assertThrows(ServerUnavailableException.class,
                () -> new HttpTransport(endpoint, ServerTimeouts.defaults())
                        .get(HttpTransport.JSON, new HttpTransport.Query(), "environment"));

        assertTrue(error.description().startsWith("Cannot reach " + endpoint + ": "), error.description());
    }
}
