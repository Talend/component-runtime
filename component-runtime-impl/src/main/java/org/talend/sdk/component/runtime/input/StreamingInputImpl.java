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
package org.talend.sdk.component.runtime.input;

import static java.lang.Thread.sleep;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.concurrent.atomic.AtomicBoolean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class StreamingInputImpl extends InputImpl {

    private RetryConfiguration retryConfiguration;

    private transient Thread shutdownHook;

    private final AtomicBoolean running = new AtomicBoolean();

    public StreamingInputImpl(final String rootName, final String name, final String plugin,
            final Serializable instance, final RetryConfiguration retryConfiguration) {
        super(rootName, name, plugin, instance);
        shutdownHook = new Thread(() -> running.compareAndSet(true, false),
                getClass().getName() + "_" + rootName() + "-" + name() + "_" + hashCode());
        this.retryConfiguration = retryConfiguration;
    }

    protected StreamingInputImpl() {
        // no-op
    }

    @Override
    protected Object readNext() {
        final RetryStrategy strategy = retryConfiguration.getStrategy();
        int retries = retryConfiguration.getMaxRetries();
        while (running.get() && retries > 0) {
            final Object next = super.readNext();
            if (next != null) {
                strategy.reset();
                return next;
            }

            retries--;
            try {
                final long millis = strategy.nextPauseDuration();
                if (millis < 0) { // assume it means "give up"
                    prepareStop();
                } else if (millis > 0) {
                    sleep(millis);
                } // else if millis == 0 no need to call any method
            } catch (final InterruptedException e) {
                prepareStop(); // stop the stream
            }
        }
        return null;
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
    }

    @Override
    protected Object writeReplace() throws ObjectStreamException {
        return new StreamSerializationReplacer(plugin(), rootName(), name(), serializeDelegate(), retryConfiguration);
    }

    private static class StreamSerializationReplacer extends SerializationReplacer {

        private final RetryConfiguration retryConfiguration;

        StreamSerializationReplacer(final String plugin, final String component, final String name, final byte[] value,
                final RetryConfiguration retryConfiguration) {
            super(plugin, component, name, value);
            this.retryConfiguration = retryConfiguration;
        }

        protected Object readResolve() throws ObjectStreamException {
            try {
                return new StreamingInputImpl(component, name, plugin, loadDelegate(), retryConfiguration);
            } catch (final IOException | ClassNotFoundException e) {
                throw new InvalidObjectException(e.getMessage());
            }
        }
    }

    public interface RetryStrategy {

        long nextPauseDuration();

        void reset();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RetryConfiguration implements Serializable {

        private int maxRetries;

        private RetryStrategy strategy;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Constant implements Serializable, RetryStrategy {

            private long timeout;

            @Override
            public long nextPauseDuration() {
                return timeout;
            }

            @Override
            public void reset() {
                // no-op
            }
        }

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class ExponentialBackoff implements Serializable, RetryStrategy {

            private double exponent;

            private double randomizationFactor;

            private long max;

            private long initialBackOff;

            // state
            private int iteration;

            @Override
            public long nextPauseDuration() {
                final double currentIntervalMillis = Math.min(initialBackOff * Math.pow(exponent, iteration), max);
                final double randomOffset = (Math.random() * 2 - 1) * randomizationFactor * currentIntervalMillis;
                final long nextBackoffMillis = Math.min(max, Math.round(currentIntervalMillis + randomOffset));
                iteration += 1;
                return nextBackoffMillis;
            }

            @Override
            public void reset() {
                iteration = 0;
            }
        }
    }
}
