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
package org.talend.sdk.component.server.extension.stitch.server.configuration;

import static java.util.Collections.emptyMap;
import static java.util.Optional.ofNullable;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.talend.sdk.component.server.extension.stitch.server.configuration.Threads.Type.EXECUTOR;
import static org.talend.sdk.component.server.extension.stitch.server.configuration.Threads.Type.STREAMS;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonReaderFactory;
import javax.json.JsonWriterFactory;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@ApplicationScoped
public class StitchExecutorConfiguration {

    @Inject
    @Documentation("Core thread number for the task executor.")
    @ConfigProperty(name = "talend.stitch.executor.threads.core", defaultValue = "16")
    private Integer executorCoreSize;

    @Inject
    @Documentation("Max thread number for the task executor.")
    @ConfigProperty(name = "talend.stitch.executor.threads.max", defaultValue = "128")
    private Integer executorMaxSize;

    @Inject
    @Documentation("Keep alive duration (ms) for the task executor.")
    @ConfigProperty(name = "talend.stitch.executor.threads.keepAlive", defaultValue = "10000")
    private Long executorKeepAliveMs;

    @Inject
    @Documentation("Queue capacity (or -1 for an unbounded queue) for the task executor.")
    @ConfigProperty(name = "talend.stitch.executor.threads.queueSize", defaultValue = "-1")
    private Integer executorQueueCapacity;

    @Inject
    @Documentation("Core thread number for the stream redirection.")
    @ConfigProperty(name = "talend.stitch.executor.streams.threads.core", defaultValue = "32")
    private Integer streamsExecutorCoreSize;

    @Inject
    @Documentation("Max thread number for the stream redirection (recommended to be == executor one).")
    @ConfigProperty(name = "talend.stitch.executor.streams.threads.max", defaultValue = "128")
    private Integer streamsExecutorMaxSize;

    @Inject
    @Documentation("Keep alive duration (ms) for the stream redirection.")
    @ConfigProperty(name = "talend.stitch.executor.streams.threads.keepAlive", defaultValue = "10000")
    private Long streamsExecutorKeepAliveMs;

    @Inject
    @Documentation("Queue capacity (or -1 for an unbounded queue) for the streams redirection.")
    @ConfigProperty(name = "talend.stitch.executor.streams.threads.queueSize", defaultValue = "-1")
    private Integer streamsExecutorQueueCapacity;

    @Inject
    @Documentation("Max duration for executions.")
    @ConfigProperty(name = "talend.stitch.executor.executor.timeout", defaultValue = "120000")
    private Long executionTimeout;

    @Inject
    @Documentation("Where extensions can create files.")
    @ConfigProperty(name = "talend.stitch.work.directory", defaultValue = "${base}/work/application")
    private String workDirectory;

    @Getter
    private Path configurationsWorkingDirectory;

    @PostConstruct
    private void init() {
        configurationsWorkingDirectory = Paths
                .get(workDirectory
                        .replace("${base}",
                                System.getProperty("catalina.base", System.getProperty("meecrowave.base", ""))));
    }

    @App
    @Produces
    public JsonReaderFactory jsonReaderFactory() {
        return Json.createReaderFactory(emptyMap());
    }

    @App
    @Produces
    public JsonBuilderFactory jsonBuilderFactory() {
        return Json.createBuilderFactory(emptyMap());
    }

    @App
    @Produces
    public JsonWriterFactory jsonWriterFactory() {
        return Json.createWriterFactory(emptyMap());
    }

    @Produces
    @Threads(EXECUTOR)
    @ApplicationScoped
    public ExecutorService createExecutorPool() {
        return doCreatePool("executor", executorCoreSize, executorMaxSize, executorKeepAliveMs, executorQueueCapacity);
    }

    public void releaseExecutorPool(@Disposes @Threads(EXECUTOR) final ExecutorService executorService) {
        doReleasePool(executorService);
    }

    @Produces
    @Threads(STREAMS)
    @ApplicationScoped
    public ExecutorService createStreamsPool() {
        return doCreatePool("streams", streamsExecutorCoreSize, streamsExecutorMaxSize, streamsExecutorKeepAliveMs,
                streamsExecutorQueueCapacity);
    }

    public void releaseStreamsPool(@Disposes @Threads(STREAMS) final ExecutorService executorService) {
        doReleasePool(executorService);
    }

    private ThreadPoolExecutor doCreatePool(final String nameMarker, final int core, final int max,
            final long keepAlive, final int capacity) {
        return new ThreadPoolExecutor(core, max, keepAlive, MILLISECONDS,
                capacity < 0 ? new LinkedBlockingQueue<>() : new ArrayBlockingQueue<>(executorQueueCapacity),
                new ThreadFactory() {

                    private final ThreadGroup group = ofNullable(System.getSecurityManager())
                            .map(SecurityManager::getThreadGroup)
                            .orElseGet(() -> Thread.currentThread().getThreadGroup());

                    private final AtomicInteger threadNumber = new AtomicInteger(1);

                    @Override
                    public Thread newThread(final Runnable r) {
                        final Thread t = new Thread(group, r,
                                "talend-stitch-" + nameMarker + "-" + threadNumber.getAndIncrement(), 0);
                        if (t.isDaemon()) {
                            t.setDaemon(false);
                        }
                        if (t.getPriority() != Thread.NORM_PRIORITY) {
                            t.setPriority(Thread.NORM_PRIORITY);
                        }
                        return t;
                    }
                });
    }

    private void doReleasePool(final ExecutorService executorService) {
        executorService.shutdownNow();
        try {
            executorService.awaitTermination(1, MINUTES);
        } catch (final InterruptedException e) {
            log.error(e.getLocalizedMessage(), e);
            Thread.currentThread().interrupt();
        }
    }
}
