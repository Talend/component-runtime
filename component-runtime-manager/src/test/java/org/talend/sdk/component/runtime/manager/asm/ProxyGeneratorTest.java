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
package org.talend.sdk.component.runtime.manager.asm;

import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.annotation.Retention;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.service.Service;
import org.talend.sdk.component.api.service.interceptor.InterceptorHandler;
import org.talend.sdk.component.api.service.interceptor.Intercepts;
import org.talend.sdk.component.runtime.manager.serialization.DynamicContainerFinder;

class ProxyGeneratorTest {

    @Test
    void interceptors()
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        final ProxyGenerator generator = new ProxyGenerator();
        final Class<?> proxyClass = generator
                .generateProxy(Thread.currentThread().getContextClassLoader(), Intercepted.class, "test",
                        Intercepted.class.getName());
        final Intercepted proxy = Intercepted.class.cast(proxyClass.getConstructor().newInstance());
        generator.initialize(proxy, (method, args) -> "intercepted");

        assertEquals("123[4]", proxy.ok(1, "2", "3", singletonList("4")));
        assertEquals("intercepted", proxy.preempted(1, "2", "3", singletonList("4")));
    }

    @Test
    void serialization() throws Exception {
        try {
            assertFalse(Serializable.class.isInstance(new CatService())); // if this fails the whole test is pointless

            final ProxyGenerator generator = new ProxyGenerator();
            final Class<?> proxyType = generator
                    .generateProxy(Thread.currentThread().getContextClassLoader(), CatService.class, "test",
                            CatService.class.getName());
            assertNotNull(proxyType);

            final Object proxy = proxyType.getConstructor().newInstance();
            assertProxy(proxy);
            DynamicContainerFinder.SERVICES.put(CatService.class, proxy);
            DynamicContainerFinder.LOADERS.put("test", Thread.currentThread().getContextClassLoader());

            // now we did all the sanity checks let's do a round trip
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (final ObjectOutputStream oos = new ObjectOutputStream(out)) {
                oos.writeObject(proxy);
            }
            try (final ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(out.toByteArray()))) {
                final Object deserialized = ois.readObject();
                assertProxy(deserialized);
                assertSame(deserialized, proxy);
            }
        } finally {
            DynamicContainerFinder.LOADERS.clear();
            DynamicContainerFinder.SERVICES.remove(CatService.class);
        }
    }

    private void assertProxy(final Object proxy) throws Exception {
        assertTrue(() -> CatService.class.isInstance(proxy));
        assertEquals("123[4]", CatService.class.cast(proxy).cat(1, "2", "3", singletonList("4")));
        assertEquals(CatService.class.getName() + "$$TalendServiceProxy", proxy.getClass().getName());

        assertTrue(Serializable.class.isInstance(proxy));
        try {
            proxy.getClass().getMethod("writeReplace");
        } catch (final NoSuchMethodException nsm) {
            fail("proxy should be serializable");
        }
    }

    @Service
    public static class CatService {

        public String cat(final int v1, final String v2, final Object v3, final List<String> v4) {
            return v1 + v2 + v3 + v4;
        }
    }

    @Service
    public static class Intercepted {

        public String ok(final int v1, final String v2, final Object v3, final List<String> v4) {
            return v1 + v2 + v3 + v4;
        }

        @Preempted
        public String preempted(final int v1, final String v2, final Object v3, final List<String> v4) {
            return ok(v1, v2, v3, v4);
        }
    }

    @Intercepts(InterceptorHandler.class)
    @Retention(RUNTIME)
    public @interface Preempted {
    }
}
