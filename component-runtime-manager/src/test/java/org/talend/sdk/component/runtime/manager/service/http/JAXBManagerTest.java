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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.service.http.Encoder;

class JAXBManagerTest {

    @Test
    void concurrentContextInitialization() throws Exception {
        final JAXBManager manager = new JAXBManager();
        assertTrue(manager.isEmpty());

        final Method[] methods = { Api.class.getMethod("a", RootA.class), Api.class.getMethod("b", RootB.class),
                Api.class.getMethod("c", TypeC.class), Api.class.getMethod("d", RootD.class) };
        final int threads = 16;
        final ExecutorService executor = Executors.newFixedThreadPool(threads);
        try {
            final CountDownLatch start = new CountDownLatch(1);
            final List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                final int offset = i;
                futures.add(executor.submit(() -> {
                    start.await();
                    for (int j = 0; j < methods.length; j++) {
                        manager.initJaxbContext(methods[(j + offset) % methods.length]);
                    }
                    return null;
                }));
            }
            start.countDown();
            for (final Future<?> future : futures) {
                future.get(30, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }

        assertFalse(manager.isEmpty());
        final Encoder encoder = manager.newEncoder();
        assertTrue(new String(encoder.encode(new RootA()), StandardCharsets.UTF_8).contains("rootA"));
        assertTrue(new String(encoder.encode(new RootB()), StandardCharsets.UTF_8).contains("rootB"));
        assertTrue(new String(encoder.encode(new RootD()), StandardCharsets.UTF_8).contains("rootD"));
    }

    public interface Api {

        void a(RootA a);

        void b(RootB b);

        void c(TypeC c);

        void d(RootD d);
    }

    @XmlRootElement(name = "rootA")
    public static class RootA {
    }

    @XmlRootElement(name = "rootB")
    public static class RootB {
    }

    @XmlType(name = "typeC")
    public static class TypeC {
    }

    @XmlRootElement(name = "rootD")
    public static class RootD {
    }
}
