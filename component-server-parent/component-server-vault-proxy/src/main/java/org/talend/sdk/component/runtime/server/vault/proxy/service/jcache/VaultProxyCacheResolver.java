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
package org.talend.sdk.component.runtime.server.vault.proxy.service.jcache;

import static java.util.Optional.ofNullable;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;

import java.lang.annotation.Annotation;
import java.util.concurrent.TimeUnit;
import java.util.stream.StreamSupport;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.cache.Cache;
import javax.cache.CacheException;
import javax.cache.CacheManager;
import javax.cache.annotation.CacheMethodDetails;
import javax.cache.annotation.CacheResolver;
import javax.cache.annotation.CacheResolverFactory;
import javax.cache.annotation.CacheResult;
import javax.cache.configuration.Configuration;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.client.WebTarget;

import org.apache.geronimo.jcache.simple.cdi.CacheResolverImpl;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.talend.sdk.component.runtime.server.vault.proxy.configuration.Documentation;
import org.talend.sdk.component.runtime.server.vault.proxy.service.http.Http;
import org.talend.sdk.component.server.front.model.Environment;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class VaultProxyCacheResolver implements CacheResolverFactory {

    @Inject
    private CacheManager cacheManager;

    @Inject
    private CacheConfigurationFactory cacheConfiguration;

    @Inject
    @Documentation("How often (in ms) the Component Server should be checked to invalidate the caches on the component parameters (to identify credentials).")
    @ConfigProperty(name = "talend.vault.cache.jcache.refresh.period", defaultValue = "30000")
    private Long refreshPeriod;

    @Inject
    @Http(Http.Type.TALEND_COMPONENT_KIT)
    private WebTarget client;

    private long lastUpdated;

    private volatile boolean running = true;

    private Thread thread;

    @PostConstruct
    private void startRefresh() {
        thread = new Thread(() -> refreshThread(refreshPeriod));
        thread.setName(getClass().getName() + "-refresher");
        thread.setPriority(Thread.NORM_PRIORITY);
        thread.setDaemon(false);
        thread.setUncaughtExceptionHandler((t, e) -> log.error(e.getMessage(), e));
        thread.start();
    }

    @PreDestroy
    private void stopRefresh() {
        running = false;
        ofNullable(thread).ifPresent(it -> {
            try {
                it.interrupt();
                it.join(TimeUnit.SECONDS.toMillis(5)); // not super important here
            } catch (final InterruptedException e) {
                log.warn(e.getMessage());
                Thread.currentThread().interrupt();
            }
        });
    }

    private void refreshThread(final long delay) {
        try {
            while (running) {
                try {
                    updateIfNeeded();
                } catch (final Exception e) {
                    log.warn(e.getMessage(), e);
                }
                Thread.sleep(delay);
            }
        } catch (final InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private void updateIfNeeded() {
        final Environment environment =
                client.path("environment").request(APPLICATION_JSON_TYPE).get(Environment.class);
        // assumes time are synch-ed but not a high assumption
        if (lastUpdated < environment.getLastUpdated().getTime()) {
            clearCaches();
            lastUpdated = System.currentTimeMillis();
        }
    }

    private void clearCaches() {
        StreamSupport
                .stream(cacheManager.getCacheNames().spliterator(), false)
                .filter(name -> name.startsWith("org.talend.sdk.component.runtime.server.vault."))
                .forEach(r -> cacheManager.getCache(r).clear());
    }

    @Override
    public CacheResolver getCacheResolver(final CacheMethodDetails<? extends Annotation> cacheMethodDetails) {
        return findCacheResolver(cacheMethodDetails.getCacheName());
    }

    @Override
    public CacheResolver getExceptionCacheResolver(final CacheMethodDetails<CacheResult> cacheMethodDetails) {
        return findCacheResolver(cacheMethodDetails.getCacheAnnotation().exceptionCacheName());
    }

    private CacheResolver findCacheResolver(final String exceptionCacheName) {
        Cache<?, ?> cache = cacheManager.getCache(exceptionCacheName);
        if (cache == null) {
            try {
                cache = createCache(exceptionCacheName);
            } catch (final CacheException ce) {
                cache = cacheManager.getCache(exceptionCacheName);
            }
        }
        return new CacheResolverImpl(cache);
    }

    private Cache<?, ?> createCache(final String exceptionCacheName) {
        final CacheSizeManager<Object, Object> listener = new CacheSizeManager<>(cacheConfiguration.maxSize());
        final Configuration<Object, Object> configuration = cacheConfiguration.createConfiguration(listener);
        final Cache<Object, Object> instance = cacheManager.createCache(exceptionCacheName, configuration);
        listener.accept(instance);
        return instance;
    }
}
