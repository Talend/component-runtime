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
package org.talend.sdk.component.proxy.service.client;

import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM;

import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import javax.cache.annotation.CacheDefaults;
import javax.cache.annotation.CacheResult;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import org.talend.sdk.component.proxy.config.ProxyConfiguration;
import org.talend.sdk.component.proxy.jcache.CacheResolverManager;
import org.talend.sdk.component.proxy.jcache.ProxyCacheKeyGenerator;
import org.talend.sdk.component.proxy.service.qualifier.UiSpecProxy;
import org.talend.sdk.component.server.front.model.ComponentDetail;
import org.talend.sdk.component.server.front.model.ComponentDetailList;
import org.talend.sdk.component.server.front.model.ComponentIndices;

@ApplicationScoped
@CacheDefaults(cacheResolverFactory = CacheResolverManager.class, cacheKeyGenerator = ProxyCacheKeyGenerator.class)
public class ComponentClient {

    @Inject
    @UiSpecProxy
    private WebTarget webTarget;

    @Inject
    private ProxyConfiguration configuration;

    @CacheResult(cacheName = "org.talend.sdk.component.proxy.components.all")
    public CompletionStage<ComponentIndices> getAllComponents(final String language,
            final Function<String, String> placeholderProvider) {
        return configuration
                .getHeaderAppender()
                .apply(this.webTarget
                        .path("component/index")
                        .queryParam("language", language)
                        .queryParam("includeIconContent", false)
                        .request(MediaType.APPLICATION_JSON_TYPE), placeholderProvider)
                .rx()
                .get(ComponentIndices.class);
    }

    @CacheResult(cacheName = "org.talend.sdk.component.proxy.components.family.icon")
    public CompletionStage<byte[]> getFamilyIconById(final String id,
            final Function<String, String> placeholderProvider) {
        return configuration
                .getHeaderAppender()
                .apply(this.webTarget.path("component/icon/family/" + id).request(APPLICATION_OCTET_STREAM),
                        placeholderProvider)
                .rx()
                .get(byte[].class);
    }

    @CacheResult(cacheName = "org.talend.sdk.component.proxy.components.detail")
    public CompletionStage<ComponentDetail> getComponentDetail(final String language,
            final Function<String, String> placeholderProvider, final String id) {
        return configuration
                .getHeaderAppender()
                .apply(this.webTarget
                        .path("component/details")
                        .queryParam("language", language)
                        .queryParam("identifiers", id)
                        .request(MediaType.APPLICATION_JSON_TYPE), placeholderProvider)
                .rx()
                .get(ComponentDetailList.class)
                .thenApply(l -> l.getDetails().iterator().next());
    }

    @CacheResult(cacheName = "org.talend.sdk.component.proxy.components.icon")
    public CompletionStage<byte[]> getComponentIconById(final Function<String, String> placeholderProvider,
            final String id) {
        return configuration
                .getHeaderAppender()
                .apply(this.webTarget.path("component/icon/" + id).request(APPLICATION_OCTET_STREAM),
                        placeholderProvider)
                .rx()
                .get(byte[].class);
    }

}
