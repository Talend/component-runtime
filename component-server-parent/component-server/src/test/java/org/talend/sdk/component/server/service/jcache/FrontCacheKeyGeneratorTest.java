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
package org.talend.sdk.component.server.service.jcache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Locale;

import javax.cache.annotation.CacheInvocationParameter;
import javax.cache.annotation.CacheKeyInvocationContext;
import javax.cache.annotation.GeneratedCacheKey;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.UriInfo;

import org.junit.jupiter.api.Test;

class FrontCacheKeyGeneratorTest {

    private FrontCacheKeyGenerator generator(final String path, final boolean failing) throws Exception {
        final FrontCacheKeyGenerator generator = new FrontCacheKeyGenerator();
        final UriInfo uriInfo = mock(UriInfo.class);
        final HttpHeaders headers = mock(HttpHeaders.class);
        if (failing) {
            when(uriInfo.getPath()).thenThrow(new IllegalStateException("no request"));
        } else {
            when(uriInfo.getPath()).thenReturn(path);
            when(uriInfo.getQueryParameters()).thenReturn(new MultivaluedHashMap<>());
            when(headers.getLanguage()).thenReturn(Locale.ENGLISH);
            when(headers.getHeaderString(HttpHeaders.ACCEPT)).thenReturn("application/json");
        }
        set(generator, "request", mock(Request.class));
        set(generator, "uriInfo", uriInfo);
        set(generator, "headers", headers);
        return generator;
    }

    private static void set(final Object instance, final String name, final Object value) throws Exception {
        final Field field = FrontCacheKeyGenerator.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(instance, value);
    }

    @SuppressWarnings("unchecked")
    private static CacheKeyInvocationContext<java.lang.annotation.Annotation> context(final Object value) {
        final CacheInvocationParameter parameter = mock(CacheInvocationParameter.class);
        when(parameter.getValue()).thenReturn(value);
        final CacheKeyInvocationContext<java.lang.annotation.Annotation> context = mock(CacheKeyInvocationContext.class);
        when(context.getKeyParameters()).thenReturn(new CacheInvocationParameter[] { parameter });
        return context;
    }

    @Test
    void equalContextsGiveEqualKeys() throws Exception {
        final GeneratedCacheKey first = generator("/a", false).generateCacheKey(context("p"));
        final GeneratedCacheKey second = generator("/a", false).generateCacheKey(context("p"));
        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertEquals(first, first);
    }

    @Test
    void differentContextsGiveDifferentKeys() throws Exception {
        final GeneratedCacheKey key = generator("/a", false).generateCacheKey(context("p"));
        assertNotEquals(key, generator("/b", false).generateCacheKey(context("p")));
        assertNotEquals(key, generator("/a", false).generateCacheKey(context("q")));
        assertNotEquals(null, key);
    }

    @Test
    void notApplicableContextFallsBackToParametersOnly() throws Exception {
        final GeneratedCacheKey withContext = generator("/a", false).generateCacheKey(context("p"));
        final GeneratedCacheKey without = generator("/a", true).generateCacheKey(context("p"));
        assertNotEquals(withContext, without);
        assertEquals(without, generator("/z", true).generateCacheKey(context("p")));
    }
}
