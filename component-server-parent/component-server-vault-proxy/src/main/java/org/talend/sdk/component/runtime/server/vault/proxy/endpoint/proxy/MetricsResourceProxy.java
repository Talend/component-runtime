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

import java.util.concurrent.CompletionStage;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

import org.talend.sdk.component.runtime.server.vault.proxy.endpoint.jaxrs.RequestForwarder;
import org.talend.sdk.component.runtime.server.vault.proxy.service.http.Http;
import org.talend.sdk.component.runtime.server.vault.proxy.service.talendcomponentkit.TalendComponentKitService;

@Path("proxy/metrics")
@ApplicationScoped
public class MetricsResourceProxy {

    @Inject
    @Http(Http.Type.TALEND_COMPONENT_KIT)
    private WebTarget client;

    @Inject
    private TalendComponentKitService service;

    @Inject
    private RequestForwarder forwarder;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public CompletionStage<Response> getText(@Context final SecurityContext securityContext,
            @Context final UriInfo uriInfo) {
        return forwarder.forward("proxy/");
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public CompletionStage<Response> getJson(@Context final SecurityContext securityContext,
            @Context final UriInfo uriInfo) {
        return forwarder.forward("proxy/");
    }

    @GET
    @Path("{registry}")
    @Produces(MediaType.APPLICATION_JSON)
    public CompletionStage<Response> getJson(@PathParam("registry") final String registry,
            @Context final SecurityContext securityContext, @Context final UriInfo uriInfo) {
        return forwarder.forward("proxy/");
    }

    @GET
    @Path("{registry}")
    @Produces(MediaType.TEXT_PLAIN)
    public CompletionStage<Response> getText(@PathParam("registry") final String registry,
            @Context final SecurityContext securityContext, @Context final UriInfo uriInfo) {
        return forwarder.forward("proxy/");
    }

    @GET
    @Path("{registry}/{metric}")
    @Produces(MediaType.APPLICATION_JSON)
    public CompletionStage<Response> getJson(@PathParam("registry") final String registry,
            @PathParam("metric") final String name, @Context final SecurityContext securityContext,
            @Context final UriInfo uriInfo) {
        return forwarder.forward("proxy/");
    }

    @GET
    @Path("{registry}/{metric}")
    @Produces(MediaType.TEXT_PLAIN)
    public CompletionStage<Response> getText(@PathParam("registry") final String registry,
            @PathParam("metric") final String name, @Context final SecurityContext securityContext,
            @Context final UriInfo uriInfo) {
        return forwarder.forward("proxy/");
    }

    @OPTIONS
    @Path("{registry}/{metric}")
    @Produces(MediaType.APPLICATION_JSON)
    public CompletionStage<Response> getMetadata(@PathParam("registry") final String registry,
            @PathParam("metric") final String name, @Context final SecurityContext securityContext,
            @Context final UriInfo uriInfo) {
        return forwarder.forward("proxy/");
    }

    @OPTIONS
    @Path("{registry}")
    @Produces(MediaType.APPLICATION_JSON)
    public CompletionStage<Response> getMetadata(@PathParam("registry") final String registry,
            @Context final SecurityContext securityContext, @Context final UriInfo uriInfo) {
        return forwarder.forward("proxy/");
    }

}
