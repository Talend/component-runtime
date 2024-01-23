/**
 * Copyright (C) 2006-2024 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.server.test.websocket;

import static java.util.concurrent.TimeUnit.MINUTES;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.spi.JsonbProvider;
import javax.websocket.ClientEndpointConfig;
import javax.websocket.CloseReason;
import javax.websocket.ContainerProvider;
import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.RemoteEndpoint;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import org.apache.meecrowave.Meecrowave;

@ApplicationScoped
public class WebsocketClient {

    @Inject
    private Meecrowave.Builder config;

    public <T> T read(final Class<T> response, final String method, final String uri, final String body) {
        return read(response, method, uri, body, "application/json");
    }

    public <T> T read(final Class<T> response, final String method, final String uri, final String body,
            final String type) {
        final WebSocketContainer container = ContainerProvider.getWebSocketContainer();
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<String> responseHolder = new AtomicReference<>();
        final ClientEndpointConfig clientEndpointConfig = ClientEndpointConfig.Builder.create().build();
        clientEndpointConfig.getUserProperties().put("org.apache.tomcat.websocket.IO_TIMEOUT_MS", "60000");

        final Session session;
        try {
            session = container.connectToServer(new Endpoint() {

                @Override
                public void onOpen(final Session session, final EndpointConfig endpointConfig) {
                    final StringBuilder builder = new StringBuilder();
                    session.addMessageHandler(ByteBuffer.class, new MessageHandler.Partial<ByteBuffer>() {

                        @Override
                        public synchronized void onMessage(final ByteBuffer part, final boolean last) {
                            try {
                                builder.append(new String(part.array()));
                            } finally {
                                if (builder.toString().endsWith("^@")) {
                                    responseHolder.set(builder.toString());
                                    doClose(session);
                                }
                            }
                        }
                    });
                }

                @Override
                public void onClose(final Session session, final CloseReason closeReason) {
                    latch.countDown();
                }

                @Override
                public void onError(final Session session, final Throwable throwable) {
                    latch.countDown();
                    if (session.isOpen()) {
                        doClose(session);
                    }
                    fail(throwable.getMessage());
                }

                private void doClose(final Session session) {
                    try {
                        session.close(new CloseReason(CloseReason.CloseCodes.GOING_AWAY, "bye bye"));
                    } catch (final IOException e) {
                        fail(e.getMessage());
                    }
                }
            }, clientEndpointConfig, URI.create("ws://localhost:" + config.getHttpPort() + "/websocket/v1/bus"));
        } catch (final DeploymentException | IOException e) {
            fail(e.getMessage());
            throw new IllegalStateException(e);
        }

        boolean awaited = true;
        try {
            final RemoteEndpoint.Async asyncRemote = session.getAsyncRemote();
            final String payload = "SEND\r\ndestination:" + uri + "\r\ndestinationMethod:" + method + "\r\nAccept: "
                    + type + "\r\nContent-Type: " + "application/json\r\n\r\n" + body + "^@";
            asyncRemote.sendBinary(ByteBuffer.wrap(payload.getBytes(StandardCharsets.UTF_8)));

            awaited = !latch.await(1, MINUTES);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            fail(e.getMessage());
        }
        try {
            final String index = responseHolder.get();
            assertNotNull(index, "No response to " + method + " " + uri + "(timeout? " + awaited + ")");
            assertTrue(index.startsWith("MESSAGE\r\n"), index);
            assertTrue(index.contains("Content-Type: " + type + "\r\n"), index);
            final int startJson = index.indexOf('{');
            final int endJson = index.indexOf("^@");
            assertTrue(startJson > 0, index);
            assertTrue(endJson > startJson, index);
            if (String.class == response) {
                return response.cast(index.substring(startJson, endJson));
            }
            try (final Jsonb jsonb = JsonbProvider.provider().create().build()) {
                final T ci = jsonb.fromJson(index.substring(startJson, endJson), response);
                assertNotNull(ci);
                return ci;
            } catch (final Exception e) {
                fail(e.getMessage());
                throw new IllegalStateException(e);
            }
        } finally {
            if (session.isOpen()) {
                try {
                    session.close(new CloseReason(CloseReason.CloseCodes.GOING_AWAY, "bye bye"));
                } catch (IOException e) {
                    fail(e.getMessage());
                }
            }
        }
    }
}
