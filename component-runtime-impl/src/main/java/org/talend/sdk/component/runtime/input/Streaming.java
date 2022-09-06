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

import static java.util.Collections.emptySet;
import static java.util.Optional.ofNullable;

import java.io.Serializable;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.talend.sdk.component.api.service.configuration.LocalConfiguration;
import org.talend.sdk.component.runtime.serialization.ContainerFinder;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Streaming {

    private static Supplier<LocalConfiguration> defaultLocalConfiguration = () -> new LocalConfiguration() {

        @Override
        public String get(final String key) {
            return null;
        }

        @Override
        public Set<String> keys() {
            return emptySet();
        }
    };

    public static RetryConfiguration loadRetryConfiguration(final String plugin) {
        // note: this configuratoin could be read on the mapper too and distributed
        final LocalConfiguration configuration = ofNullable(ContainerFinder.Instance.get().find(plugin))
                .map(it -> it.findService(LocalConfiguration.class))
                .orElseGet(defaultLocalConfiguration);
        final int maxRetries = ofNullable(configuration.get("talend.input.streaming.retry.maxRetries"))
                .map(Integer::parseInt)
                .orElse(Integer.MAX_VALUE);
        return new RetryConfiguration(maxRetries, getStrategy(configuration));
    }

    public static RetryStrategy getStrategy(final LocalConfiguration configuration) {
        switch (ofNullable(configuration.get("talend.input.streaming.retry.strategy")).orElse("constant")) {
        case "exponential":
            return new RetryConfiguration.ExponentialBackoff(
                    ofNullable(configuration.get("talend.input.streaming.retry.exponential.exponent"))
                            .map(Double::parseDouble)
                            .orElse(1.5),
                    ofNullable(configuration.get("talend.input.streaming.retry.exponential.randomizationFactor"))
                            .map(Double::parseDouble)
                            .orElse(.5),
                    ofNullable(configuration.get("talend.input.streaming.retry.exponential.maxDuration"))
                            .map(Long::parseLong)
                            .orElse(TimeUnit.MINUTES.toMillis(5)),
                    ofNullable(configuration.get("talend.input.streaming.retry.exponential.initialBackOff"))
                            .map(Long::parseLong)
                            .orElse(TimeUnit.SECONDS.toMillis(1)),
                    0);
        case "constant":
        default:
            return new RetryConfiguration.Constant(
                    ofNullable(configuration.get("talend.input.streaming.retry.constant.timeout"))
                            .map(Long::parseLong)
                            .orElse(500L));
        }
    }

    public static StopStrategy loadStopStrategy(final String plugin,
            final java.util.Map<String, String> internalConfiguration) {
        final LocalConfiguration configuration = ofNullable(ContainerFinder.Instance.get().find(plugin))
                .map(it -> it.findService(LocalConfiguration.class))
                .orElseGet(defaultLocalConfiguration);
        final Long maxReadRecords =
                ofNullable(internalConfiguration.entrySet()
                        .stream()
                        .filter(e -> e.getKey().startsWith("$maxRecords") || e.getKey().contains(".$maxRecords"))
                        .findFirst()
                        .map(e -> e.getValue())
                        .map(Long::parseLong)).get()
                                .orElse(
                                        ofNullable(System.getProperty(
                                                String.format("%s.talend.input.streaming.maxRecords", plugin)))
                                                        .map(Long::parseLong)
                                                        .orElse(ofNullable(
                                                                configuration.get("talend.input.streaming.maxRecords"))
                                                                        .map(Long::parseLong)
                                                                        .orElse(null)));
        Long maxActiveTime = ofNullable(internalConfiguration.entrySet()
                .stream()
                .filter(e -> e.getKey().startsWith("$maxDurationMs") || e.getKey().contains(".$maxDurationMs"))
                .findFirst()
                .map(e -> e.getValue())
                .map(Long::parseLong)).get()
                        .orElse(
                                ofNullable(System
                                        .getProperty(String.format("%s.talend.input.streaming.maxDurationMs", plugin)))
                                                .map(Long::parseLong)
                                                .orElse(ofNullable(
                                                        configuration.get("talend.input.streaming.maxDurationMs"))
                                                                .map(Long::parseLong)
                                                                .orElse(null)));
        log.debug("[loadStopStrategy] Records: {}; Duration: {}.", maxReadRecords, maxActiveTime);
        return new StopConfiguration(maxReadRecords, maxActiveTime, null);
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

    public interface StopStrategy {

        /**
         * Check if the stop strategy is active according specified conditions.
         * 
         * @return true if strategy is active, false otherwise.
         */
        boolean isActive();

        /**
         * Check the stop strategy conditions.
         * 
         * @param read - already read records.
         * @return true if the lifecycle should be stopped, false otherwise.
         */
        boolean shouldStop(long read);

        /**
         * Maximum records to read.
         * 
         * @return max number of records to read.
         */
        long getMaxReadRecords();

        /**
         * Maximum duration the lifecycle should run in ms.
         * 
         * @return max activity duration.
         */
        long getMaxActiveTime();

        /**
         * The system time in millis when the lifecycle started.
         * 
         * @return started time.
         */
        long getStartedAtTime();

    }

    @Data
    public static class StopConfiguration implements StopStrategy, Serializable {

        private long maxReadRecords;

        private long maxActiveTime;

        private long startedAtTime;

        public StopConfiguration() {
            maxReadRecords = -1L;
            maxActiveTime = -1L;
            startedAtTime = System.currentTimeMillis();
        }

        public StopConfiguration(final Long maxRecords, final Long maxTime, final Long start) {
            maxReadRecords = maxRecords == null ? -1L : maxRecords;
            maxActiveTime = maxTime == null ? -1L : maxTime;
            startedAtTime = start == null ? System.currentTimeMillis() : start;
        }

        @Override
        public boolean isActive() {
            return (maxReadRecords > -1) || (maxActiveTime > -1);
        }

        private boolean hasEnoughRecords(final long read) {
            return maxReadRecords != -1 && read >= maxReadRecords;
        }

        private boolean isTimePassed() {
            return maxActiveTime != -1 && System.currentTimeMillis() - startedAtTime >= maxActiveTime;
        }

        @Override
        public boolean shouldStop(final long readRecords) {
            return hasEnoughRecords(readRecords) || isTimePassed();
        }
    }

}
