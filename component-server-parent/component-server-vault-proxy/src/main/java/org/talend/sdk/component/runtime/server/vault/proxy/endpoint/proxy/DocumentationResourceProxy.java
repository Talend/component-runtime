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
package org.talend.sdk.component.runtime.server.vault.proxy.endpoint.proxy;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import java.util.concurrent.CompletionStage;

import javax.cache.annotation.CacheDefaults;
import javax.cache.annotation.CacheResult;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.talend.sdk.component.runtime.server.vault.proxy.endpoint.jaxrs.RequestForwarder;
import org.talend.sdk.component.runtime.server.vault.proxy.service.jcache.VaultProxyCacheKeyGenerator;
import org.talend.sdk.component.runtime.server.vault.proxy.service.jcache.VaultProxyCacheResolver;

@ApplicationScoped
@Path("documentation")
@Produces(APPLICATION_JSON)
@CacheDefaults(cacheResolverFactory = VaultProxyCacheResolver.class,
        cacheKeyGenerator = VaultProxyCacheKeyGenerator.class)
public class DocumentationResourceProxy {

    @Inject
    private RequestForwarder forwarder;

    @GET
    @Path("component/{id}")
    @CacheResult
    public CompletionStage<Response> getDocumentation() {
        return forwarder.forward();
    }
}
