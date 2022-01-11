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

import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM;
import static org.talend.sdk.component.runtime.server.vault.proxy.endpoint.jaxrs.Responses.decorate;

import java.util.Map;
import java.util.concurrent.CompletionStage;

import javax.cache.annotation.CacheDefaults;
import javax.cache.annotation.CacheResult;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.talend.sdk.component.runtime.server.vault.proxy.endpoint.jaxrs.RequestForwarder;
import org.talend.sdk.component.runtime.server.vault.proxy.service.http.Http;
import org.talend.sdk.component.runtime.server.vault.proxy.service.jcache.VaultProxyCacheKeyGenerator;
import org.talend.sdk.component.runtime.server.vault.proxy.service.jcache.VaultProxyCacheResolver;
import org.talend.sdk.component.runtime.server.vault.proxy.service.talendcomponentkit.TalendComponentKitService;

@Path("component")
@ApplicationScoped
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@CacheDefaults(cacheResolverFactory = VaultProxyCacheResolver.class,
        cacheKeyGenerator = VaultProxyCacheKeyGenerator.class)
public class ComponentResourceProxy {

    @Inject
    @Http(Http.Type.TALEND_COMPONENT_KIT)
    private WebTarget client;

    @Inject
    private TalendComponentKitService service;

    @Inject
    private RequestForwarder handler;

    @POST
    @Path("migrate/{id}/{configurationVersion}")
    public CompletionStage<Response> migrateComponent(@PathParam("id") final String id,
            @PathParam("configurationVersion") final int version, final Map<String, String> config,
            @Context final HttpHeaders headers) {
        return service
                .getComponentSpec(id)
                .thenCompose(spec -> service.decrypt(spec, config, headers))
                .thenApply(payload -> decorate(client
                        .path("component/migrate/{id}/{configurationVersion}")
                        .resolveTemplate("id", id)
                        .resolveTemplate("configurationVersion", version)
                        .request(APPLICATION_JSON_TYPE)
                        .post(entity(payload, APPLICATION_JSON_TYPE))));
    }

    @GET
    @Path("dependencies")
    @CacheResult
    public CompletionStage<Response> getDependencies() {
        return handler.forward();
    }

    @GET
    @Path("dependency/{id}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public CompletionStage<Response> getDependency() {
        return handler.forward();
    }

    @GET
    @Path("index")
    @CacheResult
    public CompletionStage<Response> getIndex() {
        return handler.forward();
    }

    @GET
    @Path("icon/family/{id}")
    @CacheResult
    @Produces({ APPLICATION_JSON, APPLICATION_OCTET_STREAM, "image/svg+xml" })
    public CompletionStage<Response> familyIcon() {
        return handler.forward();
    }

    @GET
    @Path("icon/{id}")
    @Produces({ APPLICATION_JSON, APPLICATION_OCTET_STREAM, "image/svg+xml" })
    @CacheResult
    public CompletionStage<Response> icon() {
        return handler.forward();
    }

    @GET
    @Path("details")
    @CacheResult
    public CompletionStage<Response> getDetail() {
        return handler.forward();
    }
}
