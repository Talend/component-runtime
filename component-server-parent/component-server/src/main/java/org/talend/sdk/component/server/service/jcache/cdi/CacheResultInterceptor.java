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
import java.util.Optional;
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

    private final CDIJCacheHelper helper;

    @Inject
    public CacheResultInterceptor(final CDIJCacheHelper helper) {
        this.helper = helper;
    }

    // Sonar S112 (generic Exception) / S1181 (catch Throwable) accepted: this JSR-107 declarative caching
    // interceptor wraps an arbitrary intercepted method and must propagate whatever it throws - of any
    // type - unmodified, so the signature/catch clause can never narrow to a specific exception type. See
    // the inline rationale in the catch block below (ported unchanged from upstream geronimo-jcache-simple).
    @SuppressWarnings({ "java:S112", "java:S1181" })
    @AroundInvoke
    public Object cache(final InvocationContext ic) throws Throwable {
        final CDIJCacheHelper.MethodMeta methodMeta = helper.findMeta(ic);

        final String cacheName = methodMeta.getCacheResultCacheName();

        final CacheResult cacheResult = methodMeta.getCacheResult();
        final CacheKeyInvocationContext<CacheResult> context =
                new CacheKeyInvocationContextImpl<>(ic, cacheResult, cacheName, methodMeta);

        final CacheResolverFactory cacheResolverFactory = methodMeta.getCacheResultResolverFactory();
        final CacheResolver cacheResolver = cacheResolverFactory.getCacheResolver(context);
        final Cache<Object, Object> cache = cacheResolver.resolveCache(context);

        final GeneratedCacheKey cacheKey = methodMeta.getCacheResultKeyGenerator().generateCacheKey(context);

        final Optional<Object> cached =
                lookupCachedResult(cache, cacheKey, cacheResult, cacheResolverFactory, context, methodMeta);
        if (cached.isPresent()) {
            return cached.get();
        }

        try {
            final Object result = ic.proceed();
            if (result != null) {
                cache.put(cacheKey, result);
                if (result instanceof CompletionStage) {
                    final CompletionStage<?> completionStage = (CompletionStage<?>) result;
                    completionStage.exceptionally(t -> onAsyncFailure(t, cache, cacheKey, context,
                            cacheResolverFactory, cacheResult, completionStage));
                }
            }
            return result;
        } catch (final Throwable t) {
            // Deliberately catches Throwable (Sonar S2221 accepted exception): a generic JSR-107 caching
            // interceptor must observe and always rethrow whatever ic.proceed() throws, of any type, in
            // order to decide cache-eviction/exception-caching behavior - it can never narrow to a specific
            // exception type since it wraps an arbitrary intercepted method. Ported unchanged from upstream
            // geronimo-jcache-simple, which used the same javax.interceptor pattern.
            cacheThrowableIfIncluded(t, cacheKey, context, cacheResolverFactory, cacheResult);
            throw t;
        }
    }

    // Extracted out of #cache to keep its Cognitive Complexity manageable (Sonar S3776): resolves whether a
    // previously cached result or cached exception already answers this invocation, without touching the
    // underlying network call. Returns Optional.empty() when the intercepted method must still be invoked.
    // Sonar S112 (generic Exception) accepted: rethrows whatever Throwable was cached from a prior
    // #cache invocation (see the catch block above) - it can never narrow to a specific exception type
    // since that cached value originated from an arbitrary intercepted method.
    @SuppressWarnings("java:S112")
    private Optional<Object> lookupCachedResult(final Cache<Object, Object> cache, final GeneratedCacheKey cacheKey,
            final CacheResult cacheResult, final CacheResolverFactory cacheResolverFactory,
            final CacheKeyInvocationContext<CacheResult> context, final CDIJCacheHelper.MethodMeta methodMeta)
            throws Throwable {
        if (cacheResult.skipGet()) {
            return Optional.empty();
        }
        final Object result = cache.get(cacheKey);
        if (result != null) {
            return Optional.of(result);
        }
        if (cacheResult.exceptionCacheName().isEmpty()) {
            return Optional.empty();
        }
        final Object exception =
                cacheResolverFactory.getExceptionCacheResolver(context).resolveCache(context).get(cacheKey);
        if (exception == null) {
            return Optional.empty();
        }
        if (methodMeta.isCompletionStage()) {
            return Optional.of(exception);
        }
        throw (Throwable) exception;
    }

    private <T> T onAsyncFailure(final Throwable t, final Cache<Object, Object> cache,
            final GeneratedCacheKey cacheKey, final CacheKeyInvocationContext<CacheResult> context,
            final CacheResolverFactory cacheResolverFactory, final CacheResult cacheResult,
            final CompletionStage<?> completionStage) {
        if (helper.isIncluded(t.getClass(), cacheResult.cachedExceptions(), cacheResult.nonCachedExceptions())) {
            cacheResolverFactory.getExceptionCacheResolver(context)
                    .resolveCache(context)
                    .put(cacheKey,
                            completionStage);
        } else {
            cache.remove(cacheKey);
        }
        if (t instanceof RuntimeException runtimeException) {
            throw runtimeException;
        }
        throw new IllegalStateException(t);
    }

    private void cacheThrowableIfIncluded(final Throwable t, final GeneratedCacheKey cacheKey,
            final CacheKeyInvocationContext<CacheResult> context, final CacheResolverFactory cacheResolverFactory,
            final CacheResult cacheResult) {
        if (helper.isIncluded(t.getClass(), cacheResult.cachedExceptions(), cacheResult.nonCachedExceptions())) {
            cacheResolverFactory.getExceptionCacheResolver(context).resolveCache(context).put(cacheKey, t);
        }
    }
}
