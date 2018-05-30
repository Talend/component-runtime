/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.proxy.jcache;

import static java.util.Optional.ofNullable;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.lang.annotation.Annotation;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.StreamSupport;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.cache.Cache;
import javax.cache.CacheException;
import javax.cache.CacheManager;
import javax.cache.annotation.CacheInvocationContext;
import javax.cache.annotation.CacheMethodDetails;
import javax.cache.annotation.CacheResolver;
import javax.cache.annotation.CacheResolverFactory;
import javax.cache.annotation.CacheResult;
import javax.cache.configuration.Factory;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;
import javax.cache.expiry.EternalExpiryPolicy;
import javax.cache.expiry.ExpiryPolicy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.talend.sdk.component.proxy.config.ProxyConfiguration;
import org.talend.sdk.component.proxy.service.client.EnvironmentClient;
import org.talend.sdk.component.proxy.service.qualifier.UiSpecProxy;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class CacheResolverManager implements CacheResolverFactory {

    @Inject
    private ProxyConfiguration configuration;

    @Inject
    @UiSpecProxy
    private CacheManager cacheManager;

    @Inject
    private EnvironmentClient client;

    private final ConcurrentMap<String, CacheResolver> noCacheCache = new ConcurrentHashMap<>();

    private Runnable onDestroy;

    private long lastTimestamp;

    private volatile boolean running = true;

    void eagerInit(@Observes @Initialized(ApplicationScoped.class) final Object init) {
        updateIfNeeded();
    }

    @PostConstruct
    private void startRefresh() {
        if (!configuration.getJcacheActive() || configuration.getJcacheRefreshPeriod() <= 0) {
            return;
        }
        final long delay = TimeUnit.SECONDS.toMillis(configuration.getJcacheRefreshPeriod());
        final CountDownLatch latch = new CountDownLatch(1);
        final Thread thread = new Thread(() -> refreshThread(delay, latch));
        thread.setName(getClass().getName() + "-refresher");
        thread.setPriority(Thread.NORM_PRIORITY);
        thread.setDaemon(false);
        thread.setUncaughtExceptionHandler((t, e) -> log.error(e.getMessage(), e));
        thread.start();
        onDestroy = () -> {
            try {
                latch.await();
            } catch (final InterruptedException e) {
                log.warn(e.getMessage());
                Thread.currentThread().interrupt();
            }
        };
    }

    @PreDestroy
    private void stopRefresh() {
        running = false;
        ofNullable(onDestroy).ifPresent(Runnable::run);
    }

    @Override
    public CacheResolver getCacheResolver(final CacheMethodDetails<? extends Annotation> cacheMethodDetails) {
        return toResolver(cacheMethodDetails.getCacheName());
    }

    @Override
    public CacheResolver getExceptionCacheResolver(final CacheMethodDetails<CacheResult> cacheMethodDetails) {
        return toResolver(cacheMethodDetails.getCacheAnnotation().exceptionCacheName());
    }

    private CompletionStage<?> updateIfNeeded() {
        return client.current(k -> null).thenApply(e -> {
            final long newTimestamp = e.getLastUpdated().getTime();
            if (newTimestamp <= 0 /* no support on this env */ || newTimestamp == lastTimestamp) {
                return e;
            }
            lastTimestamp = newTimestamp;
            clearCaches();
            return e;
        }).handle((r, t) -> {
            if (t != null) {
                log.warn("Can't contact Component Server", t);
            }
            return null;
        });
    }

    private void clearCaches() {
        StreamSupport.stream(cacheManager.getCacheNames().spliterator(), false).forEach(
                r -> cacheManager.getCache(r).clear());
    }

    private CacheResolver toResolver(final String cacheName) {
        if (!configuration.getJcacheActive()) {
            return noCacheCache.computeIfAbsent(cacheName,
                    k -> new CacheResolverImpl(new EmptyCache<>(cacheName, cacheManager)));
        }

        Cache<Object, Object> cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            final String keyCacheName = cacheName.replaceAll("\\([^\\)]*\\)", "");
            log.debug("Creating cache {}", keyCacheName);

            final Config config = ConfigProvider.getConfig();
            final Factory<ExpiryPolicy> policy = config
                    .getOptionalValue("jcache.caches." + keyCacheName + ".expiry.duration", Long.class)
                    .map(l -> new Duration(TimeUnit.SECONDS, l))
                    .map(CreatedExpiryPolicy::factoryOf)
                    .orElseGet(EternalExpiryPolicy::factoryOf);

            final MutableConfiguration<Object, Object> conf = new MutableConfiguration<>();
            conf.setExpiryPolicyFactory(policy);
            conf.setStoreByValue(false);
            conf.setManagementEnabled(config
                    .getOptionalValue("jcache.caches." + keyCacheName + ".management.active", Boolean.class)
                    .orElse(false));
            conf.setStatisticsEnabled(config
                    .getOptionalValue("jcache.caches." + keyCacheName + ".statistics.active", Boolean.class)
                    .orElse(false));

            try {
                cache = cacheManager.createCache(cacheName, conf);
            } catch (final CacheException ce) {
                cache = cacheManager.getCache(cacheName);
            }
        }
        return new CacheResolverImpl(cache);
    }

    private void refreshThread(final long delay, final CountDownLatch onExit) {
        try {
            while (running) {
                try {
                    updateIfNeeded().toCompletableFuture().get(delay, MILLISECONDS);
                } catch (final ExecutionException | TimeoutException e) {
                    // no-op
                } catch (final InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
                try {
                    Thread.sleep(delay);
                } catch (final InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        } finally {
            onExit.countDown();
        }
    }

    private static class CacheResolverImpl implements CacheResolver {

        private final Cache<?, ?> delegate;

        private CacheResolverImpl(final Cache<?, ?> cache) {
            delegate = cache;
        }

        @Override
        public <K, V> Cache<K, V>
                resolveCache(final CacheInvocationContext<? extends Annotation> cacheInvocationContext) {
            return (Cache<K, V>) delegate;
        }
    }
}
