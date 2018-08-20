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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import javax.cache.annotation.CacheDefaults;
import javax.cache.annotation.CacheResult;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.talend.sdk.component.proxy.config.ProxyConfiguration;
import org.talend.sdk.component.proxy.jcache.CacheResolverManager;
import org.talend.sdk.component.proxy.jcache.ProxyCacheKeyGenerator;
import org.talend.sdk.component.proxy.model.ProxyErrorDictionary;
import org.talend.sdk.component.proxy.model.ProxyErrorPayload;
import org.talend.sdk.component.proxy.service.qualifier.UiSpecProxy;
import org.talend.sdk.component.server.front.model.ConfigTypeNode;
import org.talend.sdk.component.server.front.model.ConfigTypeNodes;

@ApplicationScoped
@CacheDefaults(cacheResolverFactory = CacheResolverManager.class, cacheKeyGenerator = ProxyCacheKeyGenerator.class)
public class ConfigurationClient {

    @Inject
    @UiSpecProxy
    private WebTarget webTarget;

    @Inject
    private ProxyConfiguration configuration;

    @Inject
    private ConfigurationClient self;

    @CacheResult(cacheName = "org.talend.sdk.component.proxy.configurations.all")
    public CompletionStage<ConfigTypeNodes> getAllConfigurations(final String language,
            final Function<String, String> placeholderProvider) {
        final CompletableFuture<ConfigTypeNodes> result = new CompletableFuture<>();
        configuration
                .getHeaderAppender()
                .apply(this.webTarget
                        .path("configurationtype/index")
                        .queryParam("language", language)
                        .queryParam("lightPayload", false)
                        .request(MediaType.APPLICATION_JSON_TYPE), placeholderProvider)
                .async()
                .get(new RxInvocationCallback<ConfigTypeNodes>(result) {
                });
        return result;
    }

    public CompletionStage<ConfigTypeNode> getDetails(final String language, final String id,
            final Function<String, String> placeholderProvider) {
        return getAllConfigurations(language, placeholderProvider).thenApply(configNodes -> {
            final ConfigTypeNode node = configNodes.getNodes().get(id);
            if (node == null) {
                throw new WebApplicationException(Response
                        .status(Response.Status.BAD_REQUEST)
                        .entity(new ProxyErrorPayload(ProxyErrorDictionary.CONFIGURATION_NOT_FOUND.name(),
                                "Configuration not found"))
                        .build());
            }
            return node;
        });
    }
}
