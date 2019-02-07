/**
 * Copyright (C) 2006-2019 Talend Inc. - www.talend.com
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

import static java.util.concurrent.TimeUnit.SECONDS;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.util.Optional;
import java.util.Properties;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.FactoryBuilder;
import javax.cache.configuration.MutableCacheEntryListenerConfiguration;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;
import javax.cache.spi.CachingProvider;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.talend.sdk.component.runtime.server.vault.proxy.service.DecryptedValue;

@ApplicationScoped
public class JCacheSetup {

    @Inject
    @ConfigProperty(name = "talend.vault.cache.jcache.manager.uri",
            defaultValue = "geronimo://simple-jcache.properties")
    private String configurationUri;

    @Inject
    @ConfigProperty(name = "talend.vault.cache.jcache.manager.properties", defaultValue = "")
    private String configurationProperties;

    @Inject
    @ConfigProperty(name = "talend.vault.cache.jcache.cache.management", defaultValue = "false")
    private Boolean cacheManagement;

    @Inject
    @ConfigProperty(name = "talend.vault.cache.jcache.cache.statistics", defaultValue = "false")
    private Boolean cacheStatistics;

    @Inject
    @ConfigProperty(name = "talend.vault.cache.jcache.cache.expiry", defaultValue = "3600")
    private Long cacheExpiry;

    @Produces
    @ApplicationScoped
    public CachingProvider cachingProvider() {
        return Caching.getCachingProvider(Thread.currentThread().getContextClassLoader());
    }

    public void releaseCachingProvider(@Disposes final CachingProvider provider) {
        provider.close(); // will close manager as well
    }

    @Produces
    @ApplicationScoped
    public CacheManager cacheManager(final CachingProvider provider) {
        return provider
                .getCacheManager(URI.create(configurationUri), Thread.currentThread().getContextClassLoader(),
                        Optional.of(configurationProperties).filter(it -> !it.isEmpty()).map(it -> {
                            final Properties properties = new Properties();
                            try (final StringReader reader = new StringReader(it)) {
                                properties.load(reader);
                            } catch (final IOException e) {
                                throw new IllegalArgumentException(e);
                            }
                            return properties;
                        }).orElseGet(provider::getDefaultProperties));
    }

    @Produces
    @ApplicationScoped
    public Cache<String, DecryptedValue> cache(final CacheManager manager, final CacheSizeManager cacheSizeManager) {
        return manager
                .createCache("talend.cache.vault", new MutableConfiguration<String, DecryptedValue>()
                        .setStoreByValue(false)
                        .setExpiryPolicyFactory(CreatedExpiryPolicy.factoryOf(new Duration(SECONDS, cacheExpiry)))
                        .setManagementEnabled(cacheManagement)
                        .setStatisticsEnabled(cacheStatistics)
                        .addCacheEntryListenerConfiguration(new MutableCacheEntryListenerConfiguration<>(
                                new FactoryBuilder.SingletonFactory<>(cacheSizeManager), null, false, false)));
    }

    public void releaseCache(@Disposes final Cache<String, DecryptedValue> cache) {
        cache.close();
    }
}
