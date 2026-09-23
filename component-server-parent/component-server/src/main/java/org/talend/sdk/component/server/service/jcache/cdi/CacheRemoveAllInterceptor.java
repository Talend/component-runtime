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

/*
 * NOTE (Talend): This file is adapted from
 * org.apache.geronimo:geronimo-jcache-simple:1.0.5 (Apache License, Version 2.0),
 * class org.apache.geronimo.jcache.simple.cdi.CacheRemoveAllInterceptor.
 * It has been repackaged into the CDI/Interceptors "jakarta.*" namespace (JSR-107/
 * "javax.cache.*" types are intentionally left unchanged, since the JCache specification
 * itself has not migrated to a "jakarta.cache" package) so that the JSR-107 declarative
 * caching annotations (@CacheResult, @CachePut, @CacheRemove, @CacheRemoveAll) keep working
 * once component-server runs on a jakarta CDI container. See the original Apache License,
 * Version 2.0 header below, retained from the upstream source file.
 */
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.Serializable;
import java.util.concurrent.CompletionStage;

import javax.cache.Cache;
import javax.cache.annotation.CacheKeyInvocationContext;
import javax.cache.annotation.CacheRemoveAll;
import javax.cache.annotation.CacheResolver;
import javax.cache.annotation.CacheResolverFactory;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@CacheRemoveAll
@Interceptor
@Priority(/* LIBRARY_BEFORE */1000)
public class CacheRemoveAllInterceptor implements Serializable {

    private final CDIJCacheHelper helper;

    @Inject
    public CacheRemoveAllInterceptor(final CDIJCacheHelper helper) {
        this.helper = helper;
    }

    @AroundInvoke
    public Object cache(final InvocationContext ic) throws Throwable {
        final CDIJCacheHelper.MethodMeta methodMeta = helper.findMeta(ic);

        final String cacheName = methodMeta.getCacheRemoveAllCacheName();

        final CacheResolverFactory cacheResolverFactory = methodMeta.getCacheRemoveAllResolverFactory();
        final CacheKeyInvocationContext<CacheRemoveAll> context = new CacheKeyInvocationContextImpl<>(ic,
                methodMeta.getCacheRemoveAll(), cacheName, methodMeta);
        final CacheResolver cacheResolver = cacheResolverFactory.getCacheResolver(context);
        final Cache<Object, Object> cache = cacheResolver.resolveCache(context);

        // NOTE (Talend): originally ported as-is from upstream geronimo-jcache-simple's
        // MakeJCacheCDIInterceptorFriendly, which read the co-located @CachePut's afterInvocation flag instead
        // of @CacheRemoveAll's own. Fixed during review (QTDI-3358 round 2) to read the correct annotation so
        // eviction timing (before/after invocation) always follows this method's own @CacheRemoveAll setting,
        // regardless of whether @CachePut is also present.
        final CacheRemoveAll cacheRemoveAll = methodMeta.getCacheRemoveAll();
        final boolean afterInvocation = cacheRemoveAll.afterInvocation();
        if (!afterInvocation) {
            cache.removeAll();
        }

        final Object result;
        try {
            result = ic.proceed();
            if (result instanceof CompletionStage) {
                final CompletionStage<?> completionStage = (CompletionStage<?>) result;
                completionStage
                        .exceptionally(t -> onFailure(t, cache, cacheRemoveAll, afterInvocation));
            }
        } catch (final Throwable t) {
            // Deliberately catches Throwable (Sonar S2221 accepted exception): a generic JSR-107 caching
            // interceptor must observe and always rethrow whatever ic.proceed() throws, of any type, in
            // order to decide cache-eviction/exception-caching behavior - it can never narrow to a specific
            // exception type since it wraps an arbitrary intercepted method. Ported unchanged from upstream
            // geronimo-jcache-simple, which used the same javax.interceptor pattern.
            removeAllIfIncluded(t, cache, cacheRemoveAll, afterInvocation);
            throw t;
        }

        if (afterInvocation) {
            cache.removeAll();
        }

        return result;
    }

    private <T> T onFailure(final Throwable t, final Cache<Object, Object> cache, final CacheRemoveAll cacheRemoveAll,
            final boolean afterInvocation) {
        removeAllIfIncluded(t, cache, cacheRemoveAll, afterInvocation);
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        }
        throw new IllegalStateException(t);
    }

    private void removeAllIfIncluded(final Throwable t, final Cache<Object, Object> cache,
            final CacheRemoveAll cacheRemoveAll, final boolean afterInvocation) {
        if (afterInvocation
                && helper.isIncluded(t.getClass(), cacheRemoveAll.evictFor(), cacheRemoveAll.noEvictFor())) {
            cache.removeAll();
        }
    }
}
