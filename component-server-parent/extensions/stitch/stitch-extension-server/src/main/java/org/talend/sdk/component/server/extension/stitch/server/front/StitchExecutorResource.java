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
package org.talend.sdk.component.server.extension.stitch.server.front;

import static java.util.Optional.ofNullable;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.stream.Collectors.toSet;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;

import java.time.Clock;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.JsonObject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.sse.Sse;
import javax.ws.rs.sse.SseEventSink;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.talend.sdk.component.server.extension.stitch.model.Task;
import org.talend.sdk.component.server.extension.stitch.server.execution.ProcessExecutor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Path("executor")
@ApplicationScoped
public class StitchExecutorResource {

    @Inject
    private ProcessExecutor executor;

    @Inject
    @ConfigProperty(name = "talend.stitch.service.executor.taskValidityDuration", defaultValue = "120000")
    private Long validityDuration;

    @Inject
    @ConfigProperty(name = "talend.stitch.service.executor.evitionInterval", defaultValue = "60000")
    private Long evictionInterval;

    private final Clock clock = Clock.systemUTC();

    // requires sticky session like routing (same node)
    private final ConcurrentMap<String, TaskDefinition> submittedTasks = new ConcurrentHashMap<>();

    private ScheduledExecutorService cleanerExecutor;

    @PostConstruct
    private void init() {
        cleanerExecutor = Executors
                .newSingleThreadScheduledExecutor(
                        r -> new Thread(r, StitchExecutorResource.this.getClass().getName() + "_cleaner_guard"));
        cleanerExecutor.scheduleAtFixedRate(() -> {
            final long now = clock.millis();
            submittedTasks
                    .entrySet()
                    .stream()
                    .filter(it -> it.getValue().endOfValidity < now)
                    .map(Map.Entry::getKey)
                    .peek(it -> log.warn("Evicting task '{}'", it))
                    .collect(toSet())
                    .forEach(submittedTasks::remove);
        }, evictionInterval, evictionInterval, MILLISECONDS);
    }

    @PreDestroy
    private void destroy() {
        cleanerExecutor.shutdownNow();
        try {
            cleanerExecutor.awaitTermination(1, MINUTES);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @POST
    @Path("submit/{task}")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public Task execute(@PathParam("task") final String task, final JsonObject properties) {
        final BiConsumer<SseEventSink, Sse> executionImpl = (sink, sse) -> {
            final AtomicLong idGenerator = new AtomicLong();
            try (final SseEventSink toClose = sink) {
                executor
                        .execute(task, properties, () -> !sink.isClosed(),
                                (type, data) -> sink
                                        .send(sse
                                                .newEventBuilder()
                                                .id(Long.toString(idGenerator.incrementAndGet()))
                                                .name(type)
                                                .data(JsonObject.class, data)
                                                .mediaType(APPLICATION_JSON_TYPE)
                                                .build()));
            }
        };
        String id;
        do {
            id = task + "_" + UUID.randomUUID().toString().replace("-", "");
        } while (submittedTasks
                .putIfAbsent(id, new TaskDefinition(clock.millis() + validityDuration, executionImpl)) != null);
        return new Task(id);
    }

    @GET
    @Path("read/{id}")
    @Produces("text/event-stream")
    public void execute(@PathParam("id") final String id, @Context final SseEventSink sink, @Context final Sse sse) {
        ofNullable(submittedTasks.remove(id))
                .orElseThrow(() -> new WebApplicationException(Response.Status.BAD_REQUEST)).impl.accept(sink, sse);
    }

    @RequiredArgsConstructor
    private static class TaskDefinition {

        private final long endOfValidity;

        private final BiConsumer<SseEventSink, Sse> impl;
    }
}
