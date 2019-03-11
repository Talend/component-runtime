/**
 * Copyright (C) 2006-2019 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.server.extension.stitch.server;

import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.MINUTES;
import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.sse.SseEventSource;

import org.apache.meecrowave.Meecrowave;
import org.apache.meecrowave.junit5.MonoMeecrowaveConfig;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.server.extension.stitch.model.Task;

@MonoMeecrowaveConfig
class StitchExecutorResourceTest {

    @Inject
    private Meecrowave.Builder server;

    @Test
    void readInvalidId() {
        withClient(target -> assertEquals(Response.Status.BAD_REQUEST.getStatusCode(),
                target
                        .path("executor/read/{id}")
                        .resolveTemplate("id", "invalid")
                        .request("text/event-stream")
                        .get()
                        .getStatus()));
    }

    @Test
    void submitAndRead() {
        withClient(target -> {
            final Task task = target
                    .path("executor/submit/{tap}")
                    .resolveTemplate("tap", "ProcessExecutorTest#execute")
                    .request(APPLICATION_JSON_TYPE)
                    .post(entity(Json.createObjectBuilder().add("config", "value").build(), APPLICATION_JSON_TYPE),
                            Task.class);
            final List<String> events = new ArrayList<>();
            final CountDownLatch latch = new CountDownLatch(7);
            try (final SseEventSource source = SseEventSource
                    .target(target.path("executor/read/{id}").resolveTemplate("id", task.getId()))
                    .build()) {
                source.register((event) -> {
                    synchronized (events) {
                        events.add(event.getName());
                    }
                    assertNotNull(event.readData());
                    latch.countDown();
                });
                source.open();
                try {
                    latch.await(1, MINUTES);
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            events.sort(String.CASE_INSENSITIVE_ORDER);
            assertEquals(7, events.size());
            assertEquals(asList("exception", "log", "log", "metrics", "record", "schema", "state"), events);
        });
    }

    @Test
    void discover() {
        withClient(target -> {
            final JsonObject payload = Json
                    .createObjectBuilder()
                    .add("tap", "ProcessExecutorTest#execute")
                    .add("configuration", Json.createObjectBuilder().add("config", "value"))
                    .build();
            final JsonObject result = target
                    .path("executor/discover")
                    .request(APPLICATION_JSON_TYPE)
                    .post(entity(payload, APPLICATION_JSON_TYPE), JsonObject.class);
            assertEquals(2, result.size());
            assertEquals(0, result.getInt("exitCode"));
            final JsonObject data = result.getJsonObject("data");
            assertTrue(data.getBoolean("success"));
            // we don't validate the whole payload here since it depends the tap
            // but we ensure we have data
            assertEquals("collaborators",
                    data
                            .getJsonObject("data")
                            .getJsonArray("streams")
                            .iterator()
                            .next()
                            .asJsonObject()
                            .getString("stream"),
                    result::toString);
        });
    }

    private void withClient(final Consumer<WebTarget> clientConsumer) {
        final Client client = ClientBuilder.newClient();
        try {
            clientConsumer.accept(client.target("http://localhost:" + server.getHttpPort() + "/api/v1"));
        } finally {
            client.close();
        }
    }
}
