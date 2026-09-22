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
 * class org.apache.geronimo.jcache.simple.cdi.CacheResultInterceptor.
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
import javax.cache.annotation.CacheResolver;
import javax.cache.annotation.CacheResolverFactory;
import javax.cache.annotation.CacheResult;
import javax.cache.annotation.GeneratedCacheKey;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@CacheResult
@Interceptor
@Priority(/* LIBRARY_BEFORE */1000)
public class CacheResultInterceptor implements Serializable {

    @Inject
    private CDIJCacheHelper helper;

    @AroundInvoke
    public Object cache(final InvocationContext ic) throws Throwable {
        final CDIJCacheHelper.MethodMeta methodMeta = helper.findMeta(ic);

        final String cacheName = methodMeta.getCacheResultCacheName();

        final CacheResult cacheResult = methodMeta.getCacheResult();
        final CacheKeyInvocationContext<CacheResult> context =
                new CacheKeyInvocationContextImpl<CacheResult>(ic, cacheResult,
                        cacheName, methodMeta);

        final CacheResolverFactory cacheResolverFactory = methodMeta.getCacheResultResolverFactory();
        final CacheResolver cacheResolver = cacheResolverFactory.getCacheResolver(context);
        final Cache<Object, Object> cache = cacheResolver.resolveCache(context);

        final GeneratedCacheKey cacheKey = methodMeta.getCacheResultKeyGenerator().generateCacheKey(context);

        Cache<Object, Object> exceptionCache = null; // lazily created

        Object result;
        if (!cacheResult.skipGet()) {
            result = cache.get(cacheKey);
            if (result != null) {
                return result;
            }

            if (!cacheResult.exceptionCacheName().isEmpty()) {
                exceptionCache = cacheResolverFactory.getExceptionCacheResolver(context).resolveCache(context);
                final Object exception = exceptionCache.get(cacheKey);
                if (exception != null) {
                    if (methodMeta.isCompletionStage()) {
                        return exception;
                    }
                    throw Throwable.class.cast(exception);
                }
            }
        }

        try {
            result = ic.proceed();
            if (result != null) {
                cache.put(cacheKey, result);
                if (CompletionStage.class.isInstance(result)) {
                    final CompletionStage<?> completionStage = CompletionStage.class.cast(result);
                    completionStage.exceptionally(t -> {
                        if (helper.isIncluded(t.getClass(), cacheResult.cachedExceptions(),
                                cacheResult.nonCachedExceptions())) {
                            cacheResolverFactory.getExceptionCacheResolver(context)
                                    .resolveCache(context)
                                    .put(cacheKey, completionStage);
                        } else {
                            cache.remove(cacheKey);
                        }
                        if (RuntimeException.class.isInstance(t)) {
                            throw RuntimeException.class.cast(t);
                        }
                        throw new IllegalStateException(t);
                    });
                }
            }

            return result;
        } catch (final Throwable t) {
            // Deliberately catches Throwable (Sonar S2221 accepted exception): a generic JSR-107 caching
            // interceptor must observe and always rethrow whatever ic.proceed() throws, of any type, in
            // order to decide cache-eviction/exception-caching behavior - it can never narrow to a specific
            // exception type since it wraps an arbitrary intercepted method. Ported unchanged from upstream
            // geronimo-jcache-simple, which used the same javax.interceptor pattern.
            if (helper.isIncluded(t.getClass(), cacheResult.cachedExceptions(), cacheResult.nonCachedExceptions())) {
                if (exceptionCache == null) {
                    exceptionCache = cacheResolverFactory.getExceptionCacheResolver(context).resolveCache(context);
                }
                exceptionCache.put(cacheKey, t);
            }
            throw t;
        }
    }
}
