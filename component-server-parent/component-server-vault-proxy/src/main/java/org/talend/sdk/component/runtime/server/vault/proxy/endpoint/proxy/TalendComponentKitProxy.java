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
package org.talend.sdk.component.runtime.server.vault.proxy.endpoint.proxy;

import static java.util.Locale.ROOT;
import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;

import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.CompletionStage;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.talend.sdk.component.runtime.server.vault.proxy.service.http.Http;
import org.talend.sdk.component.runtime.server.vault.proxy.service.talendcomponentkit.TalendComponentKitService;

@Path("") // we fully fake component server
@ApplicationScoped
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class TalendComponentKitProxy {

    @Inject
    @Http(Http.Type.TALEND_COMPONENT_KIT)
    private WebTarget client;

    @Inject
    private TalendComponentKitService service;

    // todo: add other endpoint (passthrough)

    @POST
    @Path("component/migrate/{id}/{configurationVersion}")
    public CompletionStage<Response> migrateComponent(@PathParam("id") final String id,
            @PathParam("configurationVersion") final int version, final Map<String, String> config) {
        return service
                .getComponentSpec(id)
                .thenCompose(spec -> service.decrypt(spec, config))
                .thenApply(payload -> decorate(client
                        .path("component/migrate/{id}/{configurationVersion}")
                        .resolveTemplate("id", id)
                        .resolveTemplate("configurationVersion", version)
                        .request(APPLICATION_JSON_TYPE)
                        .post(entity(payload, APPLICATION_JSON_TYPE))));
    }

    @POST
    @Path("configurationtype/migrate/{id}/{configurationVersion}")
    public CompletionStage<Response> migrateConfiguration(@PathParam("id") final String id,
            @PathParam("configurationVersion") final int version, final Map<String, String> config) {
        return service
                .getConfigurationSpec(id)
                .thenCompose(spec -> service.decrypt(spec, config))
                .thenApply(payload -> decorate(client
                        .path("configurationtype/migrate/{id}/{configurationVersion}")
                        .resolveTemplate("id", id)
                        .resolveTemplate("configurationVersion", version)
                        .request(APPLICATION_JSON_TYPE)
                        .post(entity(payload, APPLICATION_JSON_TYPE))));
    }

    @POST
    @Path("action/execute")
    public CompletionStage<Response> execute(@QueryParam("family") final String family,
            @QueryParam("type") final String type, @QueryParam("action") final String action,
            @QueryParam("lang") final String lang, final Map<String, String> params) {
        return service
                .getActionSpec(family, type, action)
                .thenCompose(spec -> service.decrypt(spec, params))
                .thenApply(payload -> {
                    final Response response = client
                            .path("action/execute")
                            .queryParam("family", family)
                            .queryParam("type", type)
                            .queryParam("action", action)
                            .queryParam("lang", lang)
                            .request(APPLICATION_JSON_TYPE)
                            .post(entity(payload, APPLICATION_JSON_TYPE));
                    return decorate(response);
                });
    }

    private Response decorate(final Response source) {
        final Response.ResponseBuilder builder = Response.status(source.getStatus());
        source
                .getStringHeaders()
                .entrySet()
                .stream()
                .filter(it -> !isBlacklistedHeader(it.getKey()))
                .forEach(e -> builder.header(e.getKey(), String.join(",", e.getValue())));
        return builder.entity(source.readEntity(InputStream.class)).build();
    }

    private boolean isBlacklistedHeader(final String name) {
        final String header = name.toLowerCase(ROOT);
        // tracing headers, setup recomputes them to propagate them properly
        return header.startsWith("x-b3-") || header.startsWith("baggage-")
        // set by container
                || header.startsWith("content-type") || header.startsWith("transfer-encoding");
    }
}
