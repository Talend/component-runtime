/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
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

import javax.json.JsonObject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbConfig;

import org.talend.sdk.component.runtime.manager.ComponentManager;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LoopState implements AutoCloseable {

    private static final Map<String, LoopState> STATES = new ConcurrentHashMap<>();

    final AtomicInteger referenceCounting = new AtomicInteger();

    @Getter
    final String id = UUID.randomUUID().toString();

    final String plugin;

    private final Queue<JsonObject> queue = new ConcurrentLinkedQueue<>();

    private final Semaphore semaphore = new Semaphore(0);

    @Getter
    private final AtomicLong recordCount = new AtomicLong(0);

    private volatile Jsonb jsonb;

    private volatile boolean done;

    LoopState(final String plugin) {
        this.plugin = plugin;
        STATES.putIfAbsent(id, this);
    }

    public void push(final Object value) {
        if (value == null) {
            return;
        }
        queue.add(JsonObject.class.isInstance(value) ? JsonObject.class.cast(value) : toJsonObject(value));
        semaphore.release();
    }

    public JsonObject next() {
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

    private JsonObject toJsonObject(final Object value) {
        if (jsonb == null) {
            synchronized (this) {
                if (jsonb == null) {
                    final ComponentManager manager = ComponentManager.instance();
                    jsonb = manager
                            .getJsonbProvider()
                            .create()
                            .withProvider(manager.getJsonpProvider())
                            .withConfig(new JsonbConfig().setProperty("johnzon.cdi.activated", false))
                            .build();
                }
            }
        }
        return jsonb.fromJson(jsonb.toJson(value), JsonObject.class);
    }
}
