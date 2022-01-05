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
package org.talend.sdk.component.runtime.di.beam;

import static java.util.Optional.ofNullable;

import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbConfig;

import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;
import org.talend.sdk.component.runtime.manager.ComponentManager;
import org.talend.sdk.component.runtime.record.RecordConverters;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LoopState implements AutoCloseable {

    private static final Map<String, LoopState> STATES = new ConcurrentHashMap<>();

    final AtomicInteger referenceCounting = new AtomicInteger();

    @Getter
    final String id = UUID.randomUUID().toString();

    final String plugin;

    private final Queue<Record> queue = new ConcurrentLinkedQueue<>();

    private final Semaphore semaphore = new Semaphore(0);

    @Getter
    private final AtomicLong recordCount = new AtomicLong(0);

    private volatile RecordConverters recordConverters;

    private volatile RecordConverters.MappingMetaRegistry registry;

    private volatile Jsonb jsonb;

    private volatile RecordBuilderFactory recordBuilderFactory;

    private volatile boolean done;

    LoopState(final String plugin) {
        this.plugin = plugin;
        STATES.putIfAbsent(id, this);
    }

    public void push(final Object value) {
        if (value == null) {
            return;
        }
        queue.add(Record.class.isInstance(value) ? Record.class.cast(value) : toRecord(value));
        semaphore.release();
    }

    public Record next() {
        try {
            semaphore.acquire();
            return queue.poll();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    public boolean isDone() {
        return done;
    }

    public synchronized void done() {
        done = true;
    }

    public void end() {
        log.debug("Ending state {}", id);
        done();
        semaphore.release();
    }

    @Override
    public void close() {
        ofNullable(STATES.remove(id)).ifPresent(v -> {
            log.debug("Closing state {}", id);
            if (!done) {
                end();
            }
            ofNullable(jsonb).ifPresent(j -> {
                try {
                    j.close();
                } catch (final Exception e) {
                    // no-op
                }
            });
        });
    }

    public static LoopState newTracker(final String plugin) {
        return new LoopState(plugin);
    }

    public static LoopState lookup(final String stateId) {
        return STATES.get(stateId);
    }

    private Record toRecord(final Object value) {
        if (recordConverters == null) {
            synchronized (this) {
                if (recordConverters == null) {
                    final ComponentManager manager = ComponentManager.instance();
                    jsonb = manager
                            .getJsonbProvider()
                            .create()
                            .withProvider(manager.getJsonpProvider())
                            .withConfig(new JsonbConfig().setProperty("johnzon.cdi.activated", false))
                            .build();
                    recordConverters = new RecordConverters();
                    registry = new RecordConverters.MappingMetaRegistry();
                    recordBuilderFactory = manager.getRecordBuilderFactoryProvider().apply(null);
                }
            }
        }
        return recordConverters.toRecord(registry, value, () -> jsonb, () -> recordBuilderFactory);
    }
}
