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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.input.Producer;
import org.talend.sdk.component.runtime.input.StreamingInputImpl.RetryConfiguration;
import org.talend.sdk.component.runtime.input.StreamingInputImpl.StopConfiguration;
import org.talend.sdk.component.runtime.input.StreamingInputImpl.StopStrategy;

class StreamingInputImplTest {

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
        ZonedDateTime start = ZonedDateTime.now();
        final StopConfiguration timeStrategy = new StopConfiguration(-1L, 1L, start);
        assertTrue(timeStrategy.isActive());
        assertFalse(timeStrategy.shouldStop(1));
        assertEquals(-1, timeStrategy.getMaxReadRecords());
        assertEquals(1, timeStrategy.getMaxActiveTime());
        assertEquals(start, timeStrategy.getStarted());
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        assertTrue(timeStrategy.shouldStop(-1));
        // mixed
        StopConfiguration bothStrategy = new StopConfiguration(100L, 2L, start);
        assertTrue(bothStrategy.isActive());
        assertFalse(bothStrategy.shouldStop(1));
        assertEquals(100, bothStrategy.getMaxReadRecords());
        assertEquals(2, bothStrategy.getMaxActiveTime());
        assertEquals(start, bothStrategy.getStarted());
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
        final StopStrategy stopStrategy = new StopConfiguration(null, 1L, ZonedDateTime.now());
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
        final StopStrategy stopStrategy = new StopConfiguration(5L, 5L, null);
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
        final StopStrategy stopStrategy = new StopConfiguration(1000L, 1L, ZonedDateTime.now());
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

}
