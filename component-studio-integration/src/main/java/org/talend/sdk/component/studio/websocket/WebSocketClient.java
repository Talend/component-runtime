/**
 * Copyright (C) 2006-2017 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.studio.websocket;

import static java.util.concurrent.TimeUnit.MINUTES;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.json.bind.Jsonb;
import javax.json.bind.spi.JsonbProvider;
import javax.json.stream.JsonParsingException;
import javax.websocket.ClientEndpointConfig;
import javax.websocket.CloseReason;
import javax.websocket.ContainerProvider;
import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import org.talend.sdk.component.server.front.model.ComponentDetailList;
import org.talend.sdk.component.server.front.model.ComponentIndices;

// we shouldn't need the execution runtime so don't even include it here
//
// technical note: this client includes the transport (websocket) but also the protocol/payload formatting/parsing
// todo: better error handling, can need some server bridge love too to support ERROR responses
public class WebSocketClient implements AutoCloseable {

    private static final byte[] EOM = "^@".getBytes(StandardCharsets.UTF_8);

    private final Map<String, Queue<Session>> sessions = new HashMap<>();

    private final WebSocketContainer container;

    private final String base;

    private final Jsonb jsonb;

    public WebSocketClient(final String base) {
        this.base = base;
        this.container = ContainerProvider.getWebSocketContainer();
        this.jsonb = JsonbProvider.provider("org.apache.johnzon.jsonb.JohnzonProvider").create().build();
    }

    private String buildRequest(final String uri, final Object payload) {
        return "SEND\r\ndestination:" + uri + "\r\nAccept: application/json\r\nContent-Type: " + "application/json\r\n\r\n"
                + (payload == null ? "" : jsonb.toJson(payload)) + "^@";
    }

    private <T> T parseResponse(final byte[] payload, final Class<T> expectedResponse) {
        if (expectedResponse.isInstance(payload)) {
            return expectedResponse.cast(payload);
        }
        if (String.class == expectedResponse) {
            return expectedResponse.cast(new String(payload, StandardCharsets.UTF_8));
        }
        try (final InputStream stream = new ByteArrayInputStream(payload)) {
            return jsonb.fromJson(stream, expectedResponse);
        } catch (final JsonParsingException jpe) {
            throw new IllegalArgumentException("Can't parse JSON: '" + new String(payload, StandardCharsets.UTF_8) + "'");
        } catch (final IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private <T> T sendAndWait(final String id, final String uri, final Object payload, final Class<T> expectedResponse) {
        final Session session = getOrCreateSession(id, uri);

        final PayloadHandler handler = new PayloadHandler();
        session.getUserProperties().put("handler", handler);

        final String buildRequest = buildRequest(uri, payload);
        try {
            try {
                session.getBasicRemote().sendBinary(ByteBuffer.wrap(buildRequest.getBytes(StandardCharsets.UTF_8)));
            } catch (final IOException e) {
                throw new IllegalStateException(e);
            }

            try {
                handler.latch.await(1, MINUTES); // todo: make it configurable? 1mn is already a lot
            } catch (final InterruptedException e) {
                Thread.interrupted();
                throw new IllegalStateException(e);
            }
        } finally {
            doRelease(id, session);
        }

        return parseResponse(handler.payload(), expectedResponse);
    }

    private void doRelease(final String id, final Session session) {
        sessions.get(id).add(session);
    }

    private Session getOrCreateSession(final String id, final String uri) {
        final Queue<Session> session = sessions.computeIfAbsent(id, key -> new ConcurrentLinkedQueue<>());
        Session poll = session.poll();
        if (poll != null && !poll.isOpen()) {
            try {
                poll.close(new CloseReason(CloseReason.CloseCodes.GOING_AWAY, "Session is no more opened"));
            } catch (final Exception e) {
                // just to go through close cycle but should fail since it is not opened, we just ignore any error
            }
            poll = null;
        }
        if (poll == null) {
            poll = doConnect(id.substring("/v1/".length(), id.indexOf('/', "/v1/".length() + 1)), uri);
        }
        return poll;
    }

    private Session doConnect(final String method, final String uri) {
        final URI connectUri = URI.create(base + '/' + method + '/' + uri);
        final ClientEndpointConfig endpointConfig = ClientEndpointConfig.Builder.create().build();
        try {
            return container.connectToServer(new Endpoint() {

                @Override
                public void onOpen(final Session session, final EndpointConfig endpointConfig) {
                    session.addMessageHandler(ByteBuffer.class, new MessageHandler.Partial<ByteBuffer>() {

                        @Override
                        public synchronized void onMessage(final ByteBuffer part, final boolean last) {
                            final Consumer<ByteBuffer> callback = Consumer.class.cast(session.getUserProperties().get("handler"));
                            callback.accept(part);
                        }
                    });
                }

                @Override
                public void onError(final Session session, final Throwable throwable) {
                    final PayloadHandler handler = PayloadHandler.class.cast(session.getUserProperties().get("handler"));
                    if (handler != null) {
                        handler.latch.countDown();
                    }
                    throw new IllegalStateException(throwable);
                }
            }, endpointConfig, connectUri);
        } catch (final DeploymentException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public synchronized void close() {
        sessions.values().stream().flatMap(Collection::stream).forEach(s -> {
            try {
                s.close(new CloseReason(CloseReason.CloseCodes.GOING_AWAY, "Shutting down the studio"));
            } catch (final IOException e) {
                // no-op: todo: define if we want to log it, we will not do anything anyway at that time
            }
        });
        sessions.clear();
    }

    public synchronized V1 v1() {
        return new V1(this);
    }

    public static class V1 {

        private final WebSocketClient root;

        private V1(final WebSocketClient root) {
            this.root = root;
        }

        public V1Action action() {
            return new V1Action(root);
        }

        public V1Component component() {
            return new V1Component(root);
        }
    }

    public static class V1Action {

        private final WebSocketClient root;

        private V1Action(final WebSocketClient root) {
            this.root = root;
        }

        public <T> T execute(final Class<T> expectedResponse, final String family, final String type, final String action,
                final Map<String, String> payload) {
            return root.sendAndWait("/v1/post/action/execute",
                    "/action/execute?family=" + family + "&type=" + type + "&action=" + action, payload, expectedResponse);
        }
    }

    public static class V1Component {

        private final WebSocketClient root;

        private V1Component(final WebSocketClient root) {
            this.root = root;
        }

        public ComponentIndices getIndex(final String language) {
            return root.sendAndWait("/v1/get/component/index", "/component/index?language=" + language, null,
                    ComponentIndices.class);
        }

        public byte[] icon(final String id) {
            return root.sendAndWait("/v1/get/component/icon/{id}", "/icon/" + id, null, byte[].class);
        }

        public ComponentDetailList getDetail(final String language, final String[] identifiers) {
            return root.sendAndWait("/v1/get/component/details", "/component/details?language=" + language + "&identifiers="
                    + Stream.of(identifiers).collect(Collectors.joining(",")), null, ComponentDetailList.class);
        }

        public Map<String, String> migrate(final String id, final int configurationVersion, final Map<String, String> payload) {
            return root.sendAndWait("/v1/post/component/migrate/{id}/{configurationVersion}",
                    "/component/migrate/" + id + "/" + configurationVersion, payload, Map.class);
        }
    }

    private static class PayloadHandler implements Consumer<ByteBuffer> {

        private final CountDownLatch latch = new CountDownLatch(1);

        private final ByteArrayOutputStream out = new ByteArrayOutputStream();

        private final byte[] lastBytes = new byte[2];

        @Override
        public void accept(final ByteBuffer byteBuffer) {
            try {
                final byte[] array = byteBuffer.array();
                if (array.length >= 2) {
                    System.arraycopy(array, array.length - 2, lastBytes, 0, 2);
                } else if (array.length > 0) {
                    lastBytes[0] = lastBytes[1];
                    lastBytes[1] = array[0];
                }
                out.write(array);
            } catch (final IOException e) {
                throw new IllegalStateException(e);
            } finally {
                if (Arrays.equals(lastBytes, EOM)) {
                    latch.countDown();
                }
            }
        }

        private byte[] payload() {
            final byte[] value = out.toByteArray();

            // todo: check status header and fail if > 399 with the error message in the payload

            int start = 0;
            { // find the first empty line which means the payload starts
                boolean visitedEol = false;
                final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                for (int idx = 0; idx < value.length - 1; idx++) {
                    if (value[idx] == '\r' && value[idx + 1] == '\n') {
                        final String header = new String(baos.toByteArray(), StandardCharsets.UTF_8);
                        if (header.startsWith("status:")) {
                            try {
                                if (Integer.parseInt(header.substring("status:".length()).trim()) > 399) {
                                    throw new IllegalStateException("Bad response from server: '" + new String(value) + "'");
                                }
                            } catch (final NumberFormatException nfe) {
                                // no-op: ignore this validation then
                            }
                        }

                        idx++;
                        if (visitedEol) {
                            start = idx + 1;
                            break;
                        }
                        visitedEol = true;
                        baos.reset();
                    } else {
                        baos.write(value[idx]);
                        visitedEol = false;
                    }
                }
            }

            final int len = value.length - EOM.length - start;
            if (len <= 0) {
                return new byte[0];
            }

            final byte[] payload = new byte[len];
            System.arraycopy(value, start, payload, 0, len);
            return payload;
        }
    }
}
