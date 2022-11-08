/**
 * Copyright (C) 2006-2022 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.starter.server.service.statistic;

import static java.lang.Thread.sleep;
import static java.util.Optional.ofNullable;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.json.bind.Jsonb;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.talend.sdk.component.starter.server.service.event.CreateProject;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
public class StatisticService {

    @Inject
    private Jsonb jsonb;

    @Inject
    private Event<Representation> representationEvent;

    private final Logger logger = LoggerFactory.getLogger("talend.component.starter.statistics");

    // TODO: move to an actual backend like elasticsearch
    public void save(final CreateProject event) {
        final String project = event.getRequest().getBuildConfiguration().getGroup() + ':'
                + event.getRequest().getBuildConfiguration().getArtifact();
        try {
            final Representation representation = new Representation(project,
                    event.getRequest().getSources() == null ? 0 : event.getRequest().getSources().size(),
                    event.getRequest().getProcessors() == null ? 0 : event.getRequest().getProcessors().size(),
                    event.getRequest().getOpenapi() != null ? countEndpoints(event.getRequest().getOpenapi()) : null,
                    ofNullable(event.getRequest().getFacets()).orElseGet(Collections::emptyList));
            representationEvent.fire(representation);
            logger.info(jsonb.toJson(representation));
        } catch (final RuntimeException re) {
            logger.error(re.getMessage(), re);
        }
    }

    private long countEndpoints(final JsonObject openapi) {
        return ofNullable(openapi.getJsonObject("paths"))
                .filter(p -> p.getValueType() == JsonValue.ValueType.OBJECT)
                .map(JsonObject::asJsonObject)
                .map(paths -> paths
                        .values()
                        .stream()
                        .filter(path -> path.getValueType() == JsonValue.ValueType.OBJECT)
                        .flatMap(path -> path.asJsonObject().values().stream())
                        .filter(endpoint -> endpoint.getValueType() == JsonValue.ValueType.OBJECT)
                        .count())
                .orElse(0L);
    }

    @Data
    public static class Representation {

        private final String id;

        private final int sourcesCount;

        private final int processorsCount;

        private final Long openapiEndpoints;

        private final Collection<String> facets;
    }

    @Slf4j
    @ApplicationScoped
    public static class ProjectListener {

        @Inject
        private StatisticService statistics;

        @Inject
        @ConfigProperty(name = "talend.component.starter.statistics.threads", defaultValue = "8")
        private Integer threads;

        @Inject
        @ConfigProperty(name = "talend.component.starter.statistics.retries", defaultValue = "3")
        private Integer retries;

        @Inject
        @ConfigProperty(name = "talend.component.starter.statistics.retry-sleep", defaultValue = "250")
        private Integer retrySleep;

        @Inject
        @ConfigProperty(name = "talend.component.starter.statistics.shutdown-timeout", defaultValue = "50000")
        private Integer shutdownTimeout;

        private ExecutorService executorService;

        private volatile boolean skip = false;

        @PostConstruct
        private void init() {
            final AtomicInteger counter = new AtomicInteger(1);
            executorService = Executors.newFixedThreadPool(threads, r -> {
                final Thread thread = new Thread(r);
                thread.setName("talend-starter-statistcs-" + counter.getAndIncrement());
                thread.setPriority(Thread.NORM_PRIORITY);
                thread.setDaemon(false);
                return thread;
            });
        }

        // don't block to return ASAP to the client, not very important if it fails for
        // the end user
        public void capture(@Observes final CreateProject createProject) {
            if (skip) {
                return;
            }
            executorService.submit(() -> {
                for (int i = 0; i < retries; i++) {
                    try {
                        statistics.save(createProject);
                        return;
                    } catch (final Exception te) {
                        final Throwable e = ofNullable(te.getCause()).orElse(te);
                        if (retries - 1 == i) { // no need to retry
                            failed(createProject);
                            throw RuntimeException.class.isInstance(e) ? RuntimeException.class.cast(e)
                                    : new IllegalStateException(e);
                        }

                        if (retrySleep > 0) {
                            try {
                                sleep(retrySleep);
                            } catch (final InterruptedException ie) {
                                Thread.currentThread().interrupt();
                                break;
                            }
                        }
                    }
                }
                // we shouldn't come there so warn
                failed(createProject);
            });
        }

        private void failed(final CreateProject createProject) {
            log.warn("Can't save statistics of " + createProject + " in " + retries + " retries.");
        }

        @PreDestroy
        private void tryToSaveCurrentTasks() {
            executorService.shutdown();

            skip = true;
            try {
                if (!executorService.awaitTermination(shutdownTimeout, MILLISECONDS)) {
                    log
                            .warn("Some statistics have been missed, this is not important but reporting can not be 100% accurate");
                    executorService.shutdownNow();
                }
            } catch (final InterruptedException e) {
                log.warn("interruption during statistics shutdown, {}", e.getMessage());
                Thread.currentThread().interrupt();
            }
        }
    }
}
