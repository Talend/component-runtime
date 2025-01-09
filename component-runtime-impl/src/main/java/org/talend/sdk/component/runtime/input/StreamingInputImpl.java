/**
 * Copyright (C) 2006-2025 Talend Inc. - www.talend.com
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
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.talend.sdk.component.runtime.input.Streaming.RetryStrategy;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.PostConstruct;

import org.talend.sdk.component.api.configuration.Option;
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

    public StreamingInputImpl(final String rootName, final String name, final String plugin,
            final Serializable instance, final RetryConfiguration retryConfiguration, final StopStrategy stopStrategy) {
        super(rootName, name, plugin, instance);
        shutdownHook = new Thread(() -> running.compareAndSet(true, false),
                getClass().getName() + "_" + rootName() + "-" + name() + "_" + hashCode());
        this.retryConfiguration = retryConfiguration;
        this.stopStrategy = stopStrategy;
        log.debug("[StreamingInputImpl] Created with retryStrategy: {}, stopStrategy: {}.", this.retryConfiguration,
                this.stopStrategy);
    }

    protected StreamingInputImpl() {
        // no-op
    }

    @Override
    protected Object readNext() {
        if (!running.get()) {
            return null;
        }
        if (stopStrategy.isActive() && stopStrategy.shouldStop(readRecords)) {
            log.debug("[readNext] stopStrategy condition validated.");
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
            while (running.get() && retries > 0) {
                Object next = null;
                if (stopStrategy.isActive() && stopStrategy.getMaxActiveTime() > -1) {
                    // Some connectors do not block input and return null (rabbitmq for instance). Thus, the future
                    // timeout is never reached and retryStrategy is run then. So, need to check timeout in the loop.
                    if (stopStrategy.shouldStop(readRecords)) {
                        log.debug("[readNext] shouldStop now! Duration {}ms",
                                System.currentTimeMillis() - stopStrategy.getStartedAtTime());
                        return null;
                    }
                    final ExecutorService executor = Executors.newSingleThreadExecutor();
                    final Future<Object> reader = executor.submit(super::readNext);
                    // manage job latency...
                    // timeout + hardcoded grace period
                    final long maxActiveTimeWithGracePeriod =
                            stopStrategy.getMaxActiveTime() + Streaming.MAX_DURATION_TIME_MS_GRACE_PERIOD;
                    final long estimatedTimeout = maxActiveTimeWithGracePeriod
                            - (System.currentTimeMillis() - stopStrategy.getStartedAtTime());
                    final long timeout = estimatedTimeout < -1 ? 10 : estimatedTimeout;
                    log.debug(
                            "[readNext] Applying duration strategy for reading record: will interrupt in {}ms (estimated:{} ms Duration:{}ms).",
                            timeout, estimatedTimeout, maxActiveTimeWithGracePeriod);
                    try {
                        next = reader.get(timeout, MILLISECONDS);
                    } catch (TimeoutException e) {
                        log.debug("[readNext] Read record: timeout received.");
                        reader.cancel(true);
                        return next;
                    } catch (Exception e) {
                        // nop
                    } finally {
                        executor.shutdownNow();
                    }
                } else {
                    next = super.readNext();
                }
                if (next != null) {
                    strategy.reset();
                    readRecords++;
                    return next;
                }

                retries--;
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
    protected Object[] evaluateParameters(final Class<? extends Annotation> marker, final Method method) {
        if (marker != PostConstruct.class) {
            return super.evaluateParameters(marker, method);
        }

        final Object[] args = new Object[method.getParameters().length];
        for (int i = 0; i < method.getParameters().length; i++) {
            final Parameter parameter = method.getParameters()[i];

            final Option annotation = parameter.getAnnotation(Option.class);
            if (annotation == null) {
                args[i] = null;
            } else if (Option.MAX_DURATION_PARAMETER.equals(annotation.value())) {
                if (parameter.getType() == Integer.class || parameter.getType() == int.class) {
                    args[i] = (int) stopStrategy.getMaxActiveTime();
                } else if (parameter.getType() == Long.class || parameter.getType() == long.class) {
                    args[i] = stopStrategy.getMaxActiveTime();
                } else {
                    log.warn("The parameter {} is marked as timeout but type is not long.", parameter.getName());
                    args[i] = null;
                }
            } else if (Option.MAX_RECORDS_PARAMETER.equals(annotation.value())) {
                if (parameter.getType() == Integer.class || parameter.getType() == int.class) {
                    args[i] = (int) stopStrategy.getMaxReadRecords();
                } else if (parameter.getType() == Long.class || parameter.getType() == long.class) {
                    args[i] = stopStrategy.getMaxReadRecords();
                } else {
                    log.warn("The parameter {} is marked as max records limitation but type is not long.",
                            parameter.getName());
                    args[i] = null;
                }

            }
        }
        return args;
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
