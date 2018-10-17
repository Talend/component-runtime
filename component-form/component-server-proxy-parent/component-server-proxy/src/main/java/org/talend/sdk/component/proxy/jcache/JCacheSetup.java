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

import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.spi.CachingProvider;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;

import org.talend.sdk.component.proxy.config.ProxyConfiguration;
import org.talend.sdk.component.proxy.service.qualifier.UiSpecProxy;

@ApplicationScoped
public class JCacheSetup {

    @Produces
    @UiSpecProxy
    public CacheManager manager(@UiSpecProxy final CachingProvider provider) {
        return provider.getCacheManager();
    }

    @Produces
    @UiSpecProxy
    public CachingProvider manager(final ProxyConfiguration configuration) {
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        return configuration
                .getJcacheProvider()
                .map(p -> Caching.getCachingProvider(p, loader))
                .orElseGet(() -> Caching.getCachingProvider(loader));
    }

    // will release manager and caches transitively
    public void releaseProvider(@Disposes final CachingProvider provider) {
        provider.close();
    }
}
