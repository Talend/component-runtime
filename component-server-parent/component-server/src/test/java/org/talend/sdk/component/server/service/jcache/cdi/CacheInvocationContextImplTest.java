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
package org.talend.sdk.component.server.service.jcache.cdi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;

import javax.cache.annotation.CacheInvocationParameter;
import javax.cache.annotation.CacheKey;
import javax.cache.annotation.CachePut;
import javax.cache.annotation.CacheResult;
import javax.cache.annotation.CacheValue;

import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.interceptor.InvocationContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CacheInvocationContextImplTest {

    private CDIJCacheHelper helper;

    private final Target target = new Target();

    @BeforeEach
    void setUp() throws Exception {
        helper = new CDIJCacheHelper();
        final java.lang.reflect.Field field = CDIJCacheHelper.class.getDeclaredField("beanManager");
        field.setAccessible(true);
        field.set(helper, mock(BeanManager.class));
    }

    private InvocationContext context(final String name, final Object[] params) throws Exception {
        final Method method = Target.class.getDeclaredMethod(name, String.class, String.class);
        final InvocationContext ic = mock(InvocationContext.class);
        when(ic.getMethod()).thenReturn(method);
        when(ic.getTarget()).thenReturn(target);
        when(ic.getParameters()).thenReturn(params);
        return ic;
    }

    @Test
    void invocationContext() throws Exception {
        final InvocationContext ic = context("put", new Object[] { "k", "v" });
        final CachePut annotation = ic.getMethod().getAnnotation(CachePut.class);
        final CacheInvocationContextImpl<CachePut> impl =
                new CacheInvocationContextImpl<>(ic, annotation, "name", helper.findMeta(ic));

        assertSame(target, impl.getTarget());
        assertSame(ic.getMethod(), impl.getMethod());
        assertSame(annotation, impl.getCacheAnnotation());
        assertEquals("name", impl.getCacheName());
        assertNotNull(impl.getAnnotations());
        final CacheInvocationParameter[] all = impl.getAllParameters();
        assertEquals(2, all.length);
        assertEquals("k", all[0].getValue());
        assertEquals("v", all[1].getValue());
        assertEquals(1, all[1].getParameterPosition());
        assertSame(all, impl.getAllParameters());
        assertSame(impl, impl.unwrap(CacheInvocationContextImpl.class));
        assertThrows(IllegalArgumentException.class, () -> impl.unwrap(String.class));
    }

    @Test
    void nullParameters() throws Exception {
        final InvocationContext ic = context("put", new Object[] { "k", "v" });
        final CacheInvocationContextImpl<CachePut> impl = new CacheInvocationContextImpl<>(ic,
                ic.getMethod().getAnnotation(CachePut.class), "name", helper.findMeta(ic));
        when(ic.getParameters()).thenReturn(null);
        assertEquals(0, impl.getAllParameters().length);
    }

    @Test
    void keyAndValueParameters() throws Exception {
        final InvocationContext ic = context("put", new Object[] { "k", "v" });
        final CacheKeyInvocationContextImpl<CachePut> impl = new CacheKeyInvocationContextImpl<>(ic,
                ic.getMethod().getAnnotation(CachePut.class), "name", helper.findMeta(ic));

        final CacheInvocationParameter[] keys = impl.getKeyParameters();
        assertEquals(1, keys.length);
        assertEquals("k", keys[0].getValue());
        assertSame(keys, impl.getKeyParameters());
        final CacheInvocationParameter value = impl.getValueParameter();
        assertEquals("v", value.getValue());
        assertSame(value, impl.getValueParameter());
    }

    @Test
    void noValueParameter() throws Exception {
        final InvocationContext ic = context("result", new Object[] { "k", "v" });
        final CacheKeyInvocationContextImpl<CacheResult> impl = new CacheKeyInvocationContextImpl<>(ic,
                ic.getMethod().getAnnotation(CacheResult.class), "name", helper.findMeta(ic));
        assertNull(impl.getValueParameter());
        assertEquals(2, impl.getKeyParameters().length);
    }

    private static class Target {

        @CachePut(cacheName = "cic-put")
        void put(@CacheKey final String id, @CacheValue final String value) {
            // no-op
        }

        @CacheResult(cacheName = "cic-result")
        String result(final String id, final String other) {
            return id;
        }
    }
}
