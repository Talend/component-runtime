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
package org.talend.sdk.component.tools.webapp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JakartaJAXRSClientTest {

    private HttpServer server;

    private volatile String capturedMethod;

    private volatile String capturedPath;

    private volatile String capturedQuery;

    private volatile String capturedContentType;

    private volatile String capturedBody;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/action/execute", this::handle);
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private void handle(final HttpExchange exchange) throws IOException {
        capturedMethod = exchange.getRequestMethod();
        capturedPath = exchange.getRequestURI().getPath();
        capturedQuery = exchange.getRequestURI().getQuery();
        capturedContentType = exchange.getRequestHeaders().getFirst("Content-Type");
        try (final InputStream in = exchange.getRequestBody();
                final ByteArrayOutputStream out =
                        new ByteArrayOutputStream()) {
            final byte[] buffer = new byte[4096];
            int read;
            while ((read = in.read(buffer)) >= 0) {
                out.write(buffer, 0, read);
            }
            capturedBody = new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
        final byte[] response = "{\"status\":\"ok\"}".getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, response.length);
        try (final OutputStream body = exchange.getResponseBody()) {
            body.write(response);
        }
    }

    private String baseUrl() {
        return "http://localhost:" + server.getAddress().getPort() + "/";
    }

    @Test
    void actionSendsRequestAndMapsResponse() throws Exception {
        final Map<String, Object> params = new LinkedHashMap<>();
        params.put("aString", "value");
        params.put("aNumber", 42);
        try (final JakartaJAXRSClient<Void> client = new JakartaJAXRSClient<>(baseUrl())) {
            final Map<String, Object> result =
                    client.action("myFamily", "myType", "myAction", "en", params, null)
                            .toCompletableFuture()
                            .get(10, TimeUnit.SECONDS);

            assertEquals("POST", capturedMethod);
            assertEquals("/action/execute", capturedPath);
            assertTrue(capturedQuery.contains("family=myFamily"));
            assertTrue(capturedQuery.contains("type=myType"));
            assertTrue(capturedQuery.contains("action=myAction"));
            assertTrue(capturedQuery.contains("lang=en"));
            assertTrue(capturedContentType.startsWith("application/json"));
            // payload conversion: every param value is stringified before being sent, incl. numbers
            assertTrue(capturedBody.contains("\"aString\":\"value\""));
            assertTrue(capturedBody.contains("\"aNumber\":\"42\""));

            assertEquals(Map.of("status", "ok"), result);
        }
    }

    @Test
    void closeDelegatesToUnderlyingClientOnlyWhenOwned() {
        final jakarta.ws.rs.client.Client delegate = jakarta.ws.rs.client.ClientBuilder.newClient();
        try {
            // closeClient=false: this instance does not own the delegate, close() must be a no-op on it
            final JakartaJAXRSClient<Void> nonOwning = new JakartaJAXRSClient<>(delegate, baseUrl(), false);
            nonOwning.close();
            // the delegate is still usable after a non-owning close()
            assertNull(probeDelegateClosed(delegate));
        } finally {
            delegate.close();
        }
    }

    /**
     * @return null if the delegate is still open (probing it does not throw), otherwise the thrown exception.
     */
    private Exception probeDelegateClosed(final jakarta.ws.rs.client.Client delegate) {
        try {
            delegate.target(baseUrl());
            return null;
        } catch (final IllegalStateException e) {
            return e;
        }
    }
}
