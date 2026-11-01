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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.GZIPOutputStream;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import org.talend.sdk.component.tcketl.domain.server.ServerEndpoint;

/**
 * A component server stand-in on the loopback interface: answers are registered per path below the base path, every
 * request is recorded. A path nobody registered is answered with a {@code 404} error payload.
 */
final class StubServer implements AutoCloseable {

    static final String LOOPBACK = "127.0.0.1";

    private final HttpServer server;

    private final ExecutorService executor;

    private final String basePath;

    private final Map<String, Responder> routes = new ConcurrentHashMap<>();

    private final List<Request> requests = new CopyOnWriteArrayList<>();

    private StubServer(final HttpServer server, final ExecutorService executor, final String basePath) {
        this.server = server;
        this.executor = executor;
        this.basePath = basePath;
    }

    static StubServer start() throws IOException {
        return start("/api/v1");
    }

    static StubServer start(final String basePath) throws IOException {
        final HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getByName(LOOPBACK), 0), 0);
        final ExecutorService executor = Executors.newFixedThreadPool(4, runnable -> {
            final Thread thread = new Thread(runnable, "stub-server");
            thread.setDaemon(true);
            return thread;
        });
        final StubServer stub = new StubServer(server, executor, basePath);
        server.setExecutor(executor);
        server.createContext("/", stub::handle);
        server.start();
        return stub;
    }

    /** Registers the answer for {@code <base path><path>}, whatever the method and the query. */
    StubServer on(final String path, final Responder responder) {
        routes.put(basePath + path, responder);
        return this;
    }

    StubServer on(final String path, final Reply reply) {
        return on(path, request -> reply);
    }

    ServerEndpoint endpoint() {
        return new ServerEndpoint("http", LOOPBACK, server.getAddress().getPort(), basePath);
    }

    List<Request> requests() {
        return List.copyOf(requests);
    }

    @Override
    public void close() {
        server.stop(0);
        executor.shutdownNow();
    }

    private void handle(final HttpExchange exchange) throws IOException {
        final Map<String, String> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        exchange.getRequestHeaders().forEach((name, values) -> headers.put(name, String.join(",", values)));
        final String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        final Request request = new Request(exchange.getRequestMethod(), exchange.getRequestURI(), headers, body);
        requests.add(request);
        Reply reply;
        try {
            final Responder responder = routes.get(request.uri().getPath());
            reply = responder == null ? Reply.json(404, "{\"code\":\"UNEXPECTED\",\"description\":\"No stub\"}")
                    : responder.respond(request);
        } catch (final InterruptedException e) {
            exchange.close();
            return;
        } catch (final Exception e) {
            reply = Reply.json(500, "{\"code\":\"UNEXPECTED\",\"description\":\"" + e.getMessage() + "\"}");
        }
        try {
            reply.headers().forEach((name, value) -> exchange.getResponseHeaders().add(name, value));
            exchange.getResponseHeaders().add("Content-Type", reply.contentType());
            if (reply.body().length == 0) {
                exchange.sendResponseHeaders(reply.status(), -1);
            } else {
                exchange.sendResponseHeaders(reply.status(), reply.body().length);
                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(reply.body());
                }
            }
        } finally {
            exchange.close();
        }
    }

    @FunctionalInterface
    interface Responder {

        Reply respond(Request request) throws Exception;
    }

    record Request(String method, URI uri, Map<String, String> headers, String body) {

        String header(final String name) {
            return headers.get(name);
        }

        /** The values of a query parameter, decoded, in the order they were sent. */
        List<String> parameters(final String name) {
            final List<String> values = new ArrayList<>();
            final String query = uri.getRawQuery();
            if (query != null) {
                for (final String pair : query.split("&")) {
                    final int equals = pair.indexOf('=');
                    if (equals > 0
                            && URLDecoder.decode(pair.substring(0, equals), StandardCharsets.UTF_8).equals(name)) {
                        values.add(URLDecoder.decode(pair.substring(equals + 1), StandardCharsets.UTF_8));
                    }
                }
            }
            return values;
        }
    }

    record Reply(int status, String contentType, byte[] body, Map<String, String> headers) {

        static Reply json(final int status, final String body) {
            return bytes(status, "application/json", body.getBytes(StandardCharsets.UTF_8));
        }

        static Reply bytes(final int status, final String contentType, final byte[] body) {
            return new Reply(status, contentType, body, Map.of());
        }

        static Reply fixture(final String name) {
            return json(200, Fixtures.readText(name));
        }

        Reply gzipped() {
            final ByteArrayOutputStream compressed = new ByteArrayOutputStream();
            try (GZIPOutputStream gzip = new GZIPOutputStream(compressed)) {
                gzip.write(body);
            } catch (final IOException e) {
                throw new IllegalStateException(e);
            }
            final Map<String, String> withEncoding = new LinkedHashMap<>(headers);
            withEncoding.put("Content-Encoding", "gzip");
            return new Reply(status, contentType, compressed.toByteArray(), withEncoding);
        }
    }
}
