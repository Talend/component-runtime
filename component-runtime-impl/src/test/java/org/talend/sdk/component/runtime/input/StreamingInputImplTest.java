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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.talend.sdk.component.runtime.input.Streaming.RetryConfiguration;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.input.Producer;
import org.talend.sdk.component.runtime.input.Streaming.StopConfiguration;
import org.talend.sdk.component.runtime.input.Streaming.StopStrategy;

public class StreamingInputImplTest {

    private static final int TIME_TOLERANCE = 100;

    private static final StopStrategy defaultStopStrategy = new StopConfiguration();

    @Test
    void respectConstantTimeout() {
        final List<Long> timestamps = new ArrayList<>();
        final Input input = new StreamingInputImpl("a", "b", "c", new Serializable() {

            @Producer
            public Object next() {
                timestamps.add(System.nanoTime());
                return timestamps.size() < 3 ? null : new Object();
            }
        }, new RetryConfiguration(5, new RetryConfiguration.Constant(500)), defaultStopStrategy);
        input.start();
        try {
            for (int i = 0; i < 2; i++) {
                final long start = System.nanoTime();
                assertNotNull(input.next());
                final long end = System.nanoTime();
                assertTrue(end - start > 500 * 2);
                assertEquals(3, timestamps.size());
                assertEquals(500, TimeUnit.NANOSECONDS.toMillis(timestamps.get(1) - timestamps.get(0)), TIME_TOLERANCE);
                assertEquals(500, TimeUnit.NANOSECONDS.toMillis(timestamps.get(2) - timestamps.get(1)), TIME_TOLERANCE);
                timestamps.clear();
            }
        } finally {
            input.stop();
        }
    }

    @Test
    void respectExpTimeout() {
        final List<Long> timestamps = new ArrayList<>();
        final RetryConfiguration.ExponentialBackoff strategy =
                new RetryConfiguration.ExponentialBackoff(1.5, 0, Integer.MAX_VALUE, 1000, 0);
        final Input input = new StreamingInputImpl("a", "b", "c", new Serializable() {

            @Producer
            public Object next() {
                timestamps.add(System.nanoTime());
                return timestamps.size() < 3 ? null : new Object();
            }
        }, new RetryConfiguration(5, strategy), defaultStopStrategy);
        input.start();
        try {
            for (int i = 0; i < 2; i++) {
                final long start = System.nanoTime();
                assertNotNull(input.next());
                final long end = System.nanoTime();
                assertTrue(end - start > 500 * 2);
                assertEquals(3, timestamps.size());
                assertEquals(1000, TimeUnit.NANOSECONDS.toMillis(timestamps.get(1) - timestamps.get(0)),
                        TIME_TOLERANCE);
                assertEquals(1500, TimeUnit.NANOSECONDS.toMillis(timestamps.get(2) - timestamps.get(1)),
                        TIME_TOLERANCE);
                timestamps.clear();
            }
        } finally {
            input.stop();
        }
    }

    @Test
    void respectMaxRetry() {
        final List<Long> timestamps = new ArrayList<>();
        final Input input = new StreamingInputImpl("a", "b", "c", new Serializable() {

            @Producer
            public Object next() {
                timestamps.add(System.nanoTime());
                return null;
            }
        }, new RetryConfiguration(5, new RetryConfiguration.Constant(10)), defaultStopStrategy);
        input.start();
        try {
            assertNull(input.next());
            assertEquals(5, timestamps.size());
        } finally {
            input.stop();
        }
    }

    @Test
    void stopStrategy() {
        final StopStrategy defaultStrategy = new StopConfiguration();
        assertFalse(defaultStrategy.isActive());
        assertFalse(defaultStrategy.shouldStop(111));
        // maxRecord
        final StopConfiguration recordStrategy = new StopConfiguration(100L, null, null);
        assertTrue(recordStrategy.isActive());
        assertFalse(recordStrategy.shouldStop(10));
        assertEquals(100, recordStrategy.getMaxReadRecords());
        assertEquals(-1, recordStrategy.getMaxActiveTime());
        assertTrue(recordStrategy.shouldStop(100));
        // maxActiveTime
        Long start = System.currentTimeMillis();
        final StopConfiguration timeStrategy = new StopConfiguration(-1L, 1000L, start);
        assertTrue(timeStrategy.isActive());
        assertFalse(timeStrategy.shouldStop(1));
        assertEquals(-1, timeStrategy.getMaxReadRecords());
        assertEquals(1000, timeStrategy.getMaxActiveTime());
        assertEquals(start, timeStrategy.getStartedAtTime());
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        assertTrue(timeStrategy.shouldStop(-1));
        // mixed
        StopConfiguration bothStrategy = new StopConfiguration(100L, 2000L, start);
        assertTrue(bothStrategy.isActive());
        assertFalse(bothStrategy.shouldStop(1));
        assertEquals(100, bothStrategy.getMaxReadRecords());
        assertEquals(2000, bothStrategy.getMaxActiveTime());
        assertEquals(start, bothStrategy.getStartedAtTime());
        assertFalse(bothStrategy.shouldStop(40));
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        assertTrue(timeStrategy.shouldStop(100));
        try {
            Thread.sleep(950);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        assertTrue(timeStrategy.shouldStop(1));
    }

    @Test
    void respectStopMaxReadRecords() {
        final RetryConfiguration retryStrategy = new RetryConfiguration(1, new RetryConfiguration.Constant(500));
        final StopStrategy stopStrategy = new StopConfiguration(5L, null, null);
        final Input input = new StreamingInputImpl("a", "b", "c", new Serializable() {

            @Producer
            public Object next() {
                return new Object();
            }
        }, retryStrategy, stopStrategy);
        input.start();
        try {
            for (int i = 0; i < 5; i++) {
                assertNotNull(input.next());
            }
            assertNull(input.next());
        } finally {
            input.stop();
        }
    }

    @Test
    void respectStopMaxActiveTime() {
        final RetryConfiguration retryStrategy = new RetryConfiguration(1, new RetryConfiguration.Constant(500));
        final StopStrategy stopStrategy = new StopConfiguration(null, 1000L, System.currentTimeMillis());
        final Input input = new StreamingInputImpl("a", "b", "c", new Serializable() {

            @Producer
            public Object next() {
                return new Object();
            }
        }, retryStrategy, stopStrategy);
        input.start();
        try {
            assertNotNull(input.next());
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            assertNotNull(input.next());

            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            assertNotNull(input.next());

            try {
                Thread.sleep(550);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            assertNull(input.next());
        } finally {
            input.stop();
        }
    }

    @Test
    void respectStopBothMaxReadRecords() {
        final RetryConfiguration retryStrategy = new RetryConfiguration(1, new RetryConfiguration.Constant(500));
        final StopStrategy stopStrategy = new StopConfiguration(5L, 5000L, null);
        final Input input = new StreamingInputImpl("a", "b", "c", new Serializable() {

            @Producer
            public Object next() {
                return new Object();
            }
        }, retryStrategy, stopStrategy);
        input.start();
        try {
            for (int i = 0; i < 5; i++) {
                assertNotNull(input.next());
                try {
                    Thread.sleep(150);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            assertNull(input.next());
        } finally {
            input.stop();
        }
    }

    @Test
    void respectStopBothMaxActiveTime() {
        final RetryConfiguration retryStrategy = new RetryConfiguration(1, new RetryConfiguration.Constant(500));
        final StopStrategy stopStrategy = new StopConfiguration(1000L, 1000L, System.currentTimeMillis());
        final Input input = new StreamingInputImpl("a", "b", "c", new Serializable() {

            @Producer
            public Object next() {
                return new Object();
            }
        }, retryStrategy, stopStrategy);
        input.start();
        try {
            assertNotNull(input.next());
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            assertNotNull(input.next());

            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            assertNotNull(input.next());

            try {
                Thread.sleep(550);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            assertNull(input.next());
        } finally {
            input.stop();
        }
    }

    @Test
    void respectStopMaxDurationWithLaggingInput() {
        final RetryConfiguration retryStrategy = new RetryConfiguration(1, new RetryConfiguration.Constant(500));
        final StopStrategy stopStrategy = new StopConfiguration(-1L, 1000L, System.currentTimeMillis());
        final Input input = new StreamingInputImpl("a", "b", "c", new Serializable() {

            @Producer
            public Object next() {
                try {
                    Thread.sleep(800 + Streaming.MAX_DURATION_TIME_MS_GRACE_PERIOD);
                    return new Object();
                } catch (InterruptedException e) {
                    throw new IllegalStateException(e);
                }
            }
        }, retryStrategy, stopStrategy);
        input.start();
        try {
            assertNotNull(input.next());
            assertNull(input.next());
        } finally {
            input.stop();
        }
    }

    @Test
    void respectStopMaxDurationWithBlockingInput() {
        final RetryConfiguration retryStrategy = new RetryConfiguration(1, new RetryConfiguration.Constant(500));
        final StopStrategy stopStrategy = new StopConfiguration(-1L, 500L, System.currentTimeMillis());
        final Input input = new StreamingInputImpl("a", "b", "c", new Serializable() {

            @Producer
            public Object next() {
                try {
                    Thread.sleep(Long.MAX_VALUE);
                    return new Object();
                } catch (InterruptedException e) {
                    throw new IllegalStateException(e);
                }
            }
        }, retryStrategy, stopStrategy);
        input.start();
        try {
            assertNull(input.next());
        } finally {
            input.stop();
        }
    }

    @Test
    void passMaxDurationAndMaxRecordsOptionsInsidePostConstruct() {
        class CustomEmitter implements Serializable {

            int maxDuration;

            int maxRecords;

            @PostConstruct
            public void start(@Option(Option.MAX_DURATION_PARAMETER) int maxDuration,
                    @Option(Option.MAX_RECORDS_PARAMETER) int maxRecords) {
                this.maxDuration = maxDuration;
                this.maxRecords = maxRecords;
            }
        }

        final RetryConfiguration retryStrategy = new RetryConfiguration(1, new RetryConfiguration.Constant(500));
        final long maxTime = 500L;
        final long maxRecords = 42L;
        final StopStrategy stopStrategy = new StopConfiguration(maxRecords, maxTime, System.currentTimeMillis());
        final CustomEmitter delegate = new CustomEmitter();
        final Input input = new StreamingInputImpl("a", "b", "c", delegate, retryStrategy, stopStrategy);

        assertEquals(0, delegate.maxDuration);
        assertEquals(0, delegate.maxRecords);
        input.start();
        assertEquals(maxTime, delegate.maxDuration);
        assertEquals(maxRecords, delegate.maxRecords);
    }

    @Test
    void passMaxRecordsOptionsInsidePostConstructIncorrectArgument() {
        class CustomEmitter implements Serializable {

            int maxDuration;

            int maxRecords;

            @PostConstruct
            public void start(int maxDuration,
                    @Option(Option.MAX_RECORDS_PARAMETER) int maxRecords) {
                this.maxDuration = maxDuration;
                this.maxRecords = maxRecords;
            }
        }

        final RetryConfiguration retryStrategy = new RetryConfiguration(1, new RetryConfiguration.Constant(500));
        final long maxTime = 500L;
        final long maxRecords = 42L;
        final StopStrategy stopStrategy = new StopConfiguration(maxRecords, maxTime, System.currentTimeMillis());
        final CustomEmitter delegate = new CustomEmitter();
        final Input input = new StreamingInputImpl("a", "b", "c", delegate, retryStrategy, stopStrategy);

        assertThrows(IllegalArgumentException.class, input::start);
    }

}
