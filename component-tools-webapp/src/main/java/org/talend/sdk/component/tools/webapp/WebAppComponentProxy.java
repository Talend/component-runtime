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
package org.talend.sdk.component.tools.webapp;

import static java.util.Collections.emptyMap;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;

import java.util.Map;
import java.util.concurrent.CompletionException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Response;

import org.talend.sdk.component.form.api.ActionService;
import org.talend.sdk.component.form.api.Client;
import org.talend.sdk.component.form.api.UiSpecService;
import org.talend.sdk.component.form.api.WebException;
import org.talend.sdk.component.form.model.UiActionResult;
import org.talend.sdk.component.server.front.model.ComponentDetailList;
import org.talend.sdk.component.server.front.model.ComponentIndices;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
@Path("application")
public class WebAppComponentProxy {

    private static final String[] EMPTY_ARRAY = new String[0];

    @Inject
    private Client<Object> client;

    @Inject
    private ActionService actionService;

    @Inject
    private UiSpecService<Object> uiSpecService;

    @Inject
    private WebTarget target;

    @POST
    @Path("action")
    public void action(@Suspended final AsyncResponse response, @QueryParam("family") final String family,
            @QueryParam("type") final String type, @QueryParam("action") final String action,
            final Map<String, Object> params) {
        client.action(family, type, action, params, null).handle((r, e) -> {
            if (e != null) {
                onException(response, e);
            } else {
                response.resume(actionService.map(type, r));
            }
            return null;
        });
    }

    @GET
    @Path("index")
    public void getIndex(@Suspended final AsyncResponse response,
            @QueryParam("language") @DefaultValue("en") final String language) {
        target
                .path("component/index")
                .queryParam("language", language)
                .request(APPLICATION_JSON_TYPE)
                .rx()
                .get(ComponentIndices.class)
                .toCompletableFuture()
                .handle((index, e) -> {
                    if (e != null) {
                        onException(response, e);
                    } else {
                        index
                                .getComponents()
                                .stream()
                                .flatMap(c -> c.getLinks().stream())
                                .forEach(link -> link
                                        .setPath(link.getPath().replaceFirst("/component/", "/application/").replace(
                                                "/details?identifiers=", "/detail/")));
                        response.resume(index);
                    }
                    return null;
                });
    }

    @GET
    @Path("detail/{id}")
    public void getDetail(@Suspended final AsyncResponse response,
            @QueryParam("language") @DefaultValue("en") final String language, @PathParam("id") final String id) {
        target
                .path("component/details")
                .queryParam("language", language)
                .queryParam("identifiers", id)
                .request(APPLICATION_JSON_TYPE)
                .rx()
                .get(ComponentDetailList.class)
                .toCompletableFuture()
                .thenCompose(result -> uiSpecService.convert(result.getDetails().iterator().next(), null))
                .handle((result, e) -> {
                    if (e != null) {
                        onException(response, e);
                    } else {
                        response.resume(result);
                    }
                    return null;
                });
    }

    private void onException(final AsyncResponse response, final Throwable e) {
        final UiActionResult payload;
        final int status;
        if (WebException.class.isInstance(e)) {
            final WebException we = WebException.class.cast(e);
            status = we.getStatus();
            payload = actionService.map(we);
        } else if (CompletionException.class.isInstance(e)) {
            final CompletionException actualException = CompletionException.class.cast(e);
            log.error(actualException.getMessage(), actualException);
            status = Response.Status.BAD_GATEWAY.getStatusCode();
            payload = actionService.map(new WebException(actualException, -1, emptyMap()));
        } else {
            log.error(e.getMessage(), e);
            status = Response.Status.BAD_GATEWAY.getStatusCode();
            payload = actionService.map(new WebException(e, -1, emptyMap()));
        }
        response.resume(new WebApplicationException(Response.status(status).entity(payload).build()));
    }
}
