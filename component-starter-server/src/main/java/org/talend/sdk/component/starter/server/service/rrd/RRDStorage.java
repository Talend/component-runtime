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
package org.talend.sdk.component.starter.server.service.rrd;

import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toList;
import static org.rrd4j.ConsolFun.TOTAL;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.rrd4j.DsType;
import org.rrd4j.core.ArcDef;
import org.rrd4j.core.FetchData;
import org.rrd4j.core.RrdBackendFactory;
import org.rrd4j.core.RrdDb;
import org.rrd4j.core.RrdDef;
import org.rrd4j.core.RrdNioBackendFactory;
import org.rrd4j.core.Sample;
import org.rrd4j.core.Util;
import org.rrd4j.graph.RrdGraph;
import org.rrd4j.graph.RrdGraphConstants;
import org.rrd4j.graph.RrdGraphDef;
import org.talend.sdk.component.starter.server.service.ProjectGenerator;
import org.talend.sdk.component.starter.server.service.statistic.StatisticService;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class RRDStorage {

    @Inject
    private RRDConfig config;

    @Inject
    private ProjectGenerator projectGenerator;

    private volatile Stats stats = new Stats();

    private RrdBackendFactory backendFactory;

    private ScheduledExecutorService executor;

    private ScheduledFuture<?> updater;

    private List<String> facets;

    private CompletionStage<RrdDb> db;

    private volatile boolean dbAvailable;

    private volatile boolean savedData;

    private volatile long lastDump = System.currentTimeMillis();

    public void onStart(@Observes @Initialized(ApplicationScoped.class) final Object startEvt) {
        if (config.getSkipped()) {
            return;
        }

        facets = projectGenerator
                .getFacets()
                .keySet()
                .stream()
                .map(this::normalizeFacetName)
                .sorted()
                .collect(toList());

        final int syncPeriod = config.getSyncPeriod();
        final CompletableFuture<RrdDb> rrdFuture = new CompletableFuture<>();
        try {
            backendFactory = new RrdNioBackendFactory(syncPeriod, Executors
                    .newScheduledThreadPool(config.getSynchronizerThreads(), newThreadFactory("synchronizer")));

            final Path exportFile = getExportFile();
            if (!Files.exists(exportFile)) {
                log.info("No existing RRD at '{}', will create the it from scratch", exportFile);

                final Integer heartbeat = config.getHeartbeat();

                final RrdDef rrdDef = new RrdDef(getLocation().toString(), config.getSteps());
                rrdDef.setStartTime(Util.getTime());
                rrdDef.addDatasource("projects!", DsType.GAUGE, heartbeat, 0, Double.NaN);
                rrdDef.addDatasource("sources!", DsType.GAUGE, heartbeat, 0, Double.NaN);
                rrdDef.addDatasource("processors!", DsType.GAUGE, heartbeat, 0, Double.NaN);
                rrdDef.addDatasource("endpoints!", DsType.GAUGE, heartbeat, 0, Double.NaN);
                facets.forEach(k -> rrdDef.addDatasource(k + '!', DsType.GAUGE, heartbeat, 0, Double.NaN));

                final List<Integer> archiveRows = config.getArchiveRows();
                if (config.getArchiveSteps().size() != archiveRows.size()) {
                    throw new IllegalArgumentException("Ensure archive steps and rows have the same number of values");
                }

                final Iterator<Integer> archiveRowsIt = archiveRows.iterator();
                config
                        .getArchiveSteps()
                        .forEach(step -> rrdDef
                                .addArchive(new ArcDef(TOTAL, config.getArchiveXff(), step, archiveRowsIt.next())));

                rrdFuture.complete(RrdDb.getBuilder().setRrdDef(rrdDef).setBackendFactory(backendFactory).build());
                this.db = rrdFuture;
                dbAvailable = true;
            } else { // todo: async/CompletionStage?
                log.info("Loading RRD '{}'", exportFile);
                this.db = rrdFuture.thenApply(instance -> {
                    log.info("RRD loaded");
                    dbAvailable = true;
                    return instance;
                });
                new Thread(() -> {
                    try {
                        rrdFuture
                                .complete(RrdDb
                                        .getBuilder()
                                        .setPath(getLocation().toString())
                                        .setExternalPath(exportFile.toString())
                                        .setBackendFactory(backendFactory)
                                        .build());
                    } catch (final IOException e) {
                        rrdFuture.completeExceptionally(e);
                    }
                }, getClass().getName() + "-loader").start();
            }
        } catch (final IOException e) {
            rrdFuture.completeExceptionally(e);
        }

        executor = Executors.newSingleThreadScheduledExecutor(newThreadFactory("updater"));
        updater = executor.scheduleAtFixedRate(this::doUpdate, syncPeriod, syncPeriod, SECONDS);
    }

    public void onEvent(@Observes final StatisticService.Representation representation) {
        if (config.getSkipped()) {
            return;
        }

        final Stats current;
        synchronized (this) { // prefer a limited lock and loose a not luck update at that time than the opposite
            current = this.stats;
        }

        current.projects.incrementAndGet();
        if (representation.getOpenapiEndpoints() != null) {
            current.endpoints.addAndGet(representation.getOpenapiEndpoints());
        }
        current.sources.addAndGet(representation.getSourcesCount());
        current.processors.addAndGet(representation.getProcessorsCount());
        if (representation.getFacets() != null) {
            representation.getFacets().stream().map(this::normalizeFacetName).forEach(key -> {
                AtomicLong counter = current.facets.get(key); // don't use computeIfAbsent which can lock
                if (counter == null) {
                    counter = new AtomicLong();
                    final AtomicLong existing = current.facets.putIfAbsent(key, counter);
                    if (existing != null) {
                        counter = existing;
                    }
                }
                counter.incrementAndGet();
            });
        }
    }

    public CompletionStage<FetchData> fetch(final long start, final long end) {
        return this.db.thenApply(db -> {
            try {
                final long correctedStart = start <= 0 ? db.getArchive(0).getStartTime() : start;
                final long correctedEnd = end <= 0 ? db.getArchive(0).getEndTime() : end;
                if (correctedStart >= correctedEnd) {
                    throw new IllegalStateException("end can't be before start");
                }
                return db
                        .createFetchRequest(TOTAL, correctedStart, correctedEnd,
                                getFetchStep(correctedStart, correctedEnd))
                        .fetchData();
            } catch (final IOException e) {
                throw new IllegalStateException(e);
            }
        });
    }

    public CompletionStage<InputStream> render(final long start, final long end, final int width, final int height) {
        if (config.getSkipped()) {
            throw new IllegalStateException("RRD is not active");
        }
        return db.thenApply(db -> {
            try {
                final long correctedStart = start <= 0 ? db.getArchive(0).getStartTime() : start;
                final long correctedEnd = end <= 0 ? db.getArchive(0).getEndTime() : end;
                if (correctedStart >= correctedEnd) {
                    throw new IllegalStateException("end can't be before start");
                }
                if (width <= 0 || height <= 0) {
                    throw new IllegalStateException("height and width can't be null");
                }
                return doRender(correctedStart, correctedEnd, width, height);
            } catch (final IOException e) {
                throw new IllegalStateException(e);
            }
        });
    }

    private long getFetchStep(final long correctedStart, final long correctedEnd) {
        return Math.max(1, (correctedEnd - correctedStart) / config.getMaxFetchPoints());
    }

    private InputStream doRender(final long start, final long end, final int width, final int height)
            throws IOException {
        final String rrdPath = getLocation().toAbsolutePath().toString();

        final RrdGraphDef def = new RrdGraphDef();
        def.setImageFormat("png");
        def.setShowSignature(false);
        def.setWidth(Math.min(width, 4000));
        def.setHeight(Math.min(height, 4000));
        def.setFilename(RrdGraphConstants.IN_MEMORY_IMAGE);
        def.setTitle("Statistics from " + Util.getDate(start) + " to " + Util.getDate(end));
        def.setTimeSpan(start, end);
        def.setStep(getFetchStep(start, end));
        def.datasource("projects!", rrdPath, "projects!", TOTAL);
        def.datasource("sources!", rrdPath, "sources!", TOTAL);
        def.datasource("processors!", rrdPath, "processors!", TOTAL);
        def.datasource("endpoints!", rrdPath, "endpoints!", TOTAL);
        facets.forEach(k -> {
            final String name = k + "!";
            def.datasource(name, rrdPath, name, TOTAL);
        });
        def.line("projects!", Color.GRAY, "#projects", 2.f);
        def.line("sources!", Color.BLUE, "#sources", 2.f);
        def.line("processors!", Color.CYAN, "#processors", 2.f);
        def.line("endpoints!", Color.GREEN, "#endpoints", 2.f);

        final AtomicInteger lastFacetColor = new AtomicInteger();
        facets.forEach(k -> def.line(k + '!', new Color(100 + lastFacetColor.addAndGet(20) % 255, 0, 0), "#" + k, 2.f));

        return new ByteArrayInputStream(new RrdGraph(def).getRrdGraphInfo().getBytes());
    }

    @PreDestroy
    private void destroy() {
        if (config.getSkipped()) {
            return;
        }

        try {
            updater.cancel(true);
            executor.shutdownNow();
            try { // not important here
                executor.awaitTermination(1, SECONDS);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        } finally {
            try {
                db.thenAccept(db -> {
                    try {
                        doDump(db);
                    } finally {
                        try {
                            db.close();
                        } catch (final IOException e) {
                            log.error(e.getMessage(), e);
                        }
                    }
                }).toCompletableFuture().get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (final ExecutionException e) {
                log.error(e.getMessage(), e.getCause());
            }

            try {
                backendFactory.close();
            } catch (final IOException e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    private void doDump(final RrdDb db) {
        if (savedData) {
            log.info("Saving RRD");
            try {
                db.exportXml(getExportFile().toString());
                savedData = false;
            } catch (final IOException e) {
                log.error("Can't dump the rrd, next restart will likely not be able to reuse it: " + e.getMessage(), e);
            }
        } else {
            log.debug("No project created, no RRD dump update");
        }
    }

    private String normalizeFacetName(final String it) {
        return "facet_" + it.replace(' ', '_');
    }

    private void doUpdate() {
        if (!dbAvailable) {
            return;
        }
        final Stats stats;
        synchronized (this) {
            stats = this.stats;
            if (stats.projects.get() == 0) {
                return;
            }
            this.stats = new Stats();
            save(stats);
        }
    }

    private void save(final Stats stats) {
        db.thenApply(db -> {
            try {
                final Sample sample = db.createSample();
                sample.setValue("projects!", stats.projects.get());
                sample.setValue("sources!", stats.sources.get());
                sample.setValue("processors!", stats.processors.get());
                sample.setValue("endpoints!", stats.endpoints.get());
                stats.facets.forEach((k, v) -> sample.setValue(k + '!', v.get()));
                sample.update();
                savedData = true;
                log.debug("Updated RRD storage");

                final long autoDumpTimeout = config.getAutoDumpTimeout();
                long now;
                if (autoDumpTimeout > 0 && ((now = System.currentTimeMillis()) - lastDump) > autoDumpTimeout) {
                    doDump(db);
                    lastDump = now;
                }
            } catch (final IOException e) {
                log.error(e.getMessage() + ", stats: " + stats, e);
            }
            return db;
        });
    }

    private Path getExportFile() {
        return ensureParent(config
                .getExport()
                .map(Paths::get)
                .orElseGet(() -> getDefaultLocation("statistics.xml").toAbsolutePath()));
    }

    private Path getLocation() {
        return ensureParent(config.getLocation().map(Paths::get).orElseGet(() -> getDefaultLocation("statistics.rrd")));
    }

    private Path getDefaultLocation(final String name) {
        return ensureParent(Paths.get(System.getProperty("meecrowave.base", "target")).resolve("work").resolve(name));
    }

    private Path ensureParent(final Path path) {
        if (!Files.exists(path.getParent())) {
            try {
                Files.createDirectories(path.getParent());
            } catch (final IOException e) {
                throw new IllegalStateException(e);
            }
        }
        return path.toAbsolutePath();
    }

    private static ThreadFactory newThreadFactory(final String name) {
        return new ThreadFactory() {

            private final AtomicInteger counter = new AtomicInteger();

            @Override
            public Thread newThread(final Runnable task) {
                final Thread thread =
                        new Thread(task, getClass().getName() + '-' + name + '-' + counter.incrementAndGet());
                thread.setPriority(Thread.NORM_PRIORITY);
                thread.setDaemon(false);
                return thread;
            }
        };
    }

    @Data
    private static class Stats {

        private final AtomicLong projects = new AtomicLong();

        private final AtomicLong sources = new AtomicLong();

        private final AtomicLong processors = new AtomicLong();

        private final AtomicLong endpoints = new AtomicLong();

        private final ConcurrentMap<String, AtomicLong> facets = new ConcurrentHashMap<>();
    }
}
