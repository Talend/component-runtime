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
package org.talend.sdk.component.server.extension.stitch.server.stitch;

import static java.util.Optional.ofNullable;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import org.talend.sdk.component.server.extension.stitch.server.configuration.StitchClientConfiguration;
import org.talend.sdk.component.server.front.model.ComponentDetail;
import org.talend.sdk.component.server.front.model.ConfigTypeNode;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class StitchLoader {

    private final Collection<ComponentDetail> components = new ArrayList<>();

    private final Collection<ConfigTypeNode> configurations = new ArrayList<>();

    private final CountDownLatch latch = new CountDownLatch(1);

    void onStart(@Observes @Initialized(ApplicationScoped.class) final Object start,
            final StitchClientConfiguration configuration, final StitchMapper mapper) {
        if (!configuration.getToken().isPresent()) {
            log.info("Skipping stitch extension since token is not set");
            return;
        }

        log.info("Loading stitch data");

        final int threads = configuration.getParallelism() <= 0 ? Runtime.getRuntime().availableProcessors()
                : configuration.getParallelism();
        final ExecutorService stitchExecutor = new ThreadPoolExecutor(threads, threads, 1, MINUTES,
                new LinkedBlockingQueue<>(), new StitchThreadFactory());
        final ClientBuilder builder = ClientBuilder.newBuilder();
        builder.connectTimeout(configuration.getConnectTimeout(), MILLISECONDS);
        builder.readTimeout(configuration.getReadTimeout(), MILLISECONDS);
        builder.executorService(stitchExecutor);
        final Client stitchClient = builder.build();

        final Runnable whenDone = () -> {
            latch.countDown();
            stitchClient.close();
            stitchExecutor.shutdownNow();
            try {
                stitchExecutor.awaitTermination(1, MINUTES);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };

        new StitchClient(stitchClient, configuration.getBase(),
                configuration.getToken().orElseThrow(IllegalArgumentException::new), configuration.getRetries())
                        .listSources()
                        .thenApply(steps -> {
                            configurations.addAll(steps.stream().flatMap(step -> {
                                final ConfigTypeNode family = mapper.mapFamily(step);
                                final ConfigTypeNode datastore = mapper.mapDataStore(step, family);
                                final ConfigTypeNode dataset = mapper.mapDataSet(step, datastore);
                                components.add(mapper.mapSource(step, dataset));
                                return Stream.of(family, datastore, dataset);
                            }).collect(toList()));
                            return steps;
                        })
                        .whenComplete((steps, error) -> {
                            if (error != null) { // todo: make the state in error, using microprofile-health or metrics?
                                whenDone.run();
                                throw new IllegalStateException(error);
                            }
                            log.info("Loaded {} Stitch components", steps.size());
                            whenDone.run();
                        });
    }

    public Collection<ComponentDetail> getComponents() {
        await();
        return components;
    }

    public Collection<ConfigTypeNode> getConfigurations() {
        await();
        return configurations;
    }

    @PreDestroy
    private void destroy() {
        await();
    }

    private void await() {
        try {
            latch.await(1, MINUTES);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static class StitchThreadFactory implements ThreadFactory {

        private final ThreadGroup group = ofNullable(System.getSecurityManager())
                .map(SecurityManager::getThreadGroup)
                .orElseGet(() -> Thread.currentThread().getThreadGroup());

        private final AtomicInteger threadNumber = new AtomicInteger(1);

        @Override
        public Thread newThread(final Runnable worker) {
            final Thread t =
                    new Thread(group, worker, "talend-server-stitch-extension-" + threadNumber.getAndIncrement(), 0);
            if (t.isDaemon()) {
                t.setDaemon(false);
            }
            if (t.getPriority() != Thread.NORM_PRIORITY) {
                t.setPriority(Thread.NORM_PRIORITY);
            }
            return t;
        }
    }
}
