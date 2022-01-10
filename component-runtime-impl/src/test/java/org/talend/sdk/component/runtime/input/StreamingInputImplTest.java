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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.input.Producer;

class StreamingInputImplTest {

    private static final int TIME_TOLERANCE = 100;

    @Test
    void respectConstantTimeout() {
        final List<Long> timestamps = new ArrayList<>();
        final Input input = new StreamingInputImpl("a", "b", "c", new Serializable() {

            @Producer
            public Object next() {
                timestamps.add(System.nanoTime());
                return timestamps.size() < 3 ? null : new Object();
            }
        }, new StreamingInputImpl.RetryConfiguration(5, new StreamingInputImpl.RetryConfiguration.Constant(500)));
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
        final StreamingInputImpl.RetryConfiguration.ExponentialBackoff strategy =
                new StreamingInputImpl.RetryConfiguration.ExponentialBackoff(1.5, 0, Integer.MAX_VALUE, 1000, 0);
        final Input input = new StreamingInputImpl("a", "b", "c", new Serializable() {

            @Producer
            public Object next() {
                timestamps.add(System.nanoTime());
                return timestamps.size() < 3 ? null : new Object();
            }
        }, new StreamingInputImpl.RetryConfiguration(5, strategy));
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
        }, new StreamingInputImpl.RetryConfiguration(5, new StreamingInputImpl.RetryConfiguration.Constant(10)));
        input.start();
        try {
            assertNull(input.next());
            assertEquals(5, timestamps.size());
        } finally {
            input.stop();
        }
    }
}
