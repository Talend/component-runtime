/**
 * Copyright (C) 2006-2026 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.runtime.manager.service.http;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

class HttpRequestTest {

    @Test
    void getBodyIsCalledOnce() {
        final AtomicInteger counter = new AtomicInteger();
        final HttpRequest httpRequest =
                new HttpRequest("foo", "GET", emptyList(), emptyMap(), null, emptyMap(), (a, b) -> {
                    assertEquals(0, counter.getAndIncrement());
                    return Optional.of(new byte[0]);
                }, new Object[0], null);
        assertEquals(0, counter.get());
        IntStream.range(0, 3).forEach(idx -> {
            assertTrue(httpRequest.getBody().isPresent());
            assertEquals(1, counter.get());
        });
    }
}
