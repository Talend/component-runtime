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
package org.talend.sdk.components.vault.jcache;

import static java.util.concurrent.TimeUnit.SECONDS;

import javax.cache.configuration.Configuration;
import javax.cache.configuration.FactoryBuilder;
import javax.cache.configuration.MutableCacheEntryListenerConfiguration;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.talend.sdk.components.vault.configuration.Documentation;

@ApplicationScoped
public class CacheConfigurationFactory {

    @Inject
    @Documentation("Should JCache MBeans be registered.")
    @ConfigProperty(name = "talend.vault.cache.jcache.cache.management", defaultValue = "false")
    private Boolean cacheManagement;

    @Inject
    @Documentation("Should JCache statistics be enabled.")
    @ConfigProperty(name = "talend.vault.cache.jcache.cache.statistics", defaultValue = "false")
    private Boolean cacheStatistics;

    @Inject
    @Documentation("JCache expiry for decrypted values (ms).")
    @ConfigProperty(name = "talend.vault.cache.jcache.cache.expiry", defaultValue = "3600")
    private Long cacheExpiry;

    @Inject
    @Documentation("JCache max size per cache.")
    @ConfigProperty(name = "talend.vault.cache.jcache.maxCacheSize", defaultValue = "100000")
    private Integer maxCacheSize; // not strict constraint - for perf - but we ensure it is bound to avoid issues

    public <K, T> Configuration<K, T> createConfiguration(final CacheSizeManager<K, T> listener) {
        return new MutableConfiguration<K, T>()
                .setStoreByValue(false)
                .setExpiryPolicyFactory(CreatedExpiryPolicy.factoryOf(new Duration(SECONDS, cacheExpiry)))
                .setManagementEnabled(cacheManagement)
                .setStatisticsEnabled(cacheStatistics)
                .addCacheEntryListenerConfiguration(new MutableCacheEntryListenerConfiguration<>(
                        new FactoryBuilder.SingletonFactory<>(listener), null, false, false));
    }

    public int maxSize() {
        return maxCacheSize;
    }
}
