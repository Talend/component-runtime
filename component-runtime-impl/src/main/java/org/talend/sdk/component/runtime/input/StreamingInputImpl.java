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
package org.talend.sdk.component.runtime.input;

import static java.lang.Thread.sleep;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.talend.sdk.component.runtime.input.Streaming.RetryStrategy;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.talend.sdk.component.runtime.input.Streaming.RetryConfiguration;
import org.talend.sdk.component.runtime.input.Streaming.StopStrategy;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StreamingInputImpl extends InputImpl {

    private RetryConfiguration retryConfiguration;

    private transient Thread shutdownHook;

    private final AtomicBoolean running = new AtomicBoolean();

    private transient Semaphore semaphore;

    private StopStrategy stopStrategy;

    private transient long readRecords = 0L;

    private transient ScheduledExecutorService executor;

    public StreamingInputImpl(final String rootName, final String name, final String plugin,
            final Serializable instance, final RetryConfiguration retryConfiguration, final StopStrategy stopStrategy) {
        super(rootName, name, plugin, instance);
        shutdownHook = new Thread(() -> running.compareAndSet(true, false),
                getClass().getName() + "_" + rootName() + "-" + name() + "_" + hashCode());
        this.retryConfiguration = retryConfiguration;
        this.stopStrategy = stopStrategy;
        log.warn("[StreamingInputImpl] Created with retryStrategy: {}, stopStrategy: {}.", this.retryConfiguration,
                this.stopStrategy);
        if (stopStrategy.isActive() && Streaming.StopConfiguration.class.isInstance(stopStrategy)
                && Streaming.StopConfiguration.class.cast(stopStrategy).getMaxActiveTime() > -1) {
            executor = Executors.newSingleThreadScheduledExecutor();
            executor.schedule(() -> checkBlockingQueue(),
                    Streaming.StopConfiguration.class.cast(stopStrategy).getMaxActiveTime(), TimeUnit.MILLISECONDS);
            log.warn("[StreamingInputImpl] created executor: {}", executor);
        }
    }

    protected StreamingInputImpl() {
        // no-op
    }

    @Override
    protected Object readNext() {
        log.warn("[readNext] running {}", running.get());
        if (!running.get()) {
            return null;
        }
        log.warn("[readNext] before");
        if (stopStrategy.isActive() && stopStrategy.shouldStop(readRecords)) {
            log.warn("[readNext] condition ok! running {}", running.get());
            if (executor != null) {
                log.warn("[prepareStop] shutting down");
                executor.shutdownNow();
                try {
                    executor.awaitTermination(5, SECONDS);
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            return null;
        }
        try {
            semaphore.acquire();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
        try {
            final RetryStrategy strategy = retryConfiguration.getStrategy();
            int retries = retryConfiguration.getMaxRetries();
            log.warn("[readNext] in retry running {}", running.get());
            while (running.get() && retries > 0) {
                log.warn("[readNext] before super({}).readNext {}", super.getClass().getName(), running.get());
                // TODO blocking pb is here
                Object next = null;
                ExecutorService executor = Executors.newSingleThreadExecutor();
                Future future = executor.submit(() -> super.readNext());
                try {
                    next = future.get(7, TimeUnit.SECONDS);
                } catch (TimeoutException e) {
                    future.cancel(true);
                } catch (Exception e) {
                    // handle other exceptions
                } finally {
                    executor.shutdownNow();
                }
                log.warn("[readNext] after super.readNext {}", running.get());
                if (next != null) {
                    strategy.reset();
                    readRecords++;
                    return next;
                }

                retries--;
                log.warn("[readNext] in retry running {} retry {}", running.get(), retries);
                try {
                    final long millis = strategy.nextPauseDuration();
                    if (millis < 0) { // assume it means "give up"
                        prepareStop();
                    } else if (millis > 0) { // we can wait 1s but not minutes to quit
                        if (millis < 1000) {
                            sleep(millis);
                        } else {
                            long remaining = millis;
                            while (running.get() && remaining > 0) {
                                final long current = Math.min(remaining, 250);
                                remaining -= current;
                                sleep(current);
                            }
                        }
                    } // else if millis == 0 no need to call any method
                } catch (final InterruptedException e) {
                    prepareStop(); // stop the stream
                }
            }
            return null;
        } finally {
            semaphore.release();
        }
    }

    @Override
    protected void init() {
        super.init();
        semaphore = new Semaphore(1);
    }

    @Override
    public void start() {
        super.start();
        running.compareAndSet(false, true);
        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }

    @Override
    public void stop() {
        prepareStop();
        super.stop();
    }

    private void prepareStop() {
        running.compareAndSet(true, false);
        if (shutdownHook != null) {
            try {
                Runtime.getRuntime().removeShutdownHook(shutdownHook);
            } catch (final IllegalStateException itse) {
                // ok to ignore
            }
        }
        try {
            semaphore.acquire();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    protected void checkBlockingQueue() {
        log.warn("[checkBlockingQueue] stop? {}", stopStrategy.shouldStop(readRecords));
        if (stopStrategy.shouldStop(readRecords)) {
            running.compareAndSet(true, false);
            readNext();
        }
    }

    @Override
    protected Object writeReplace() throws ObjectStreamException {
        return new StreamSerializationReplacer(plugin(), rootName(), name(), serializeDelegate(), retryConfiguration,
                stopStrategy);
    }

    private static class StreamSerializationReplacer extends SerializationReplacer {

        private final RetryConfiguration retryConfiguration;

        private final StopStrategy stopStrategy;

        StreamSerializationReplacer(final String plugin, final String component, final String name, final byte[] value,
                final RetryConfiguration retryConfiguration, final StopStrategy stopStrategy) {
            super(plugin, component, name, value);
            this.retryConfiguration = retryConfiguration;
            this.stopStrategy = stopStrategy;
        }

        protected Object readResolve() throws ObjectStreamException {
            try {
                return new StreamingInputImpl(component, name, plugin, loadDelegate(), retryConfiguration,
                        stopStrategy);
            } catch (final IOException | ClassNotFoundException e) {
                final InvalidObjectException invalidObjectException = new InvalidObjectException(e.getMessage());
                invalidObjectException.initCause(e);
                throw invalidObjectException;
            }
        }
    }
}
