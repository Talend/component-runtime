/**
 * Copyright (C) 2006-2024 Talend Inc. - www.talend.com
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
import static java.util.Optional.ofNullable;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletionException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
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
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.talend.sdk.component.form.api.ActionService;
import org.talend.sdk.component.form.api.Client;
import org.talend.sdk.component.form.api.UiSpecService;
import org.talend.sdk.component.form.api.WebException;
import org.talend.sdk.component.form.model.UiActionResult;
import org.talend.sdk.component.server.front.model.ComponentDetailList;
import org.talend.sdk.component.server.front.model.ComponentIndices;
import org.talend.sdk.component.server.front.model.ConfigTypeNode;
import org.talend.sdk.component.server.front.model.ConfigTypeNodes;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
@Path("application")
public class WebAppComponentProxy {

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
            @QueryParam("lang") final String language, @QueryParam("type") final String type,
            @QueryParam("action") final String action, final Map<String, Object> params,
            @Context final HttpServletRequest request) {
        client.action(family, type, action, getLanguage(language, request), params, null).handle((r, e) -> {
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
    public void getIndex(@Suspended final AsyncResponse response, @QueryParam("language") final String language,
            @QueryParam("configuration") @DefaultValue("false") final boolean configuration,
            @Context final HttpServletRequest request) {
        if (configuration) {
            target
                    .path("configurationtype/index")
                    .queryParam("language", getLanguage(language, request))
                    .request(APPLICATION_JSON_TYPE)
                    .rx()
                    .get(ConfigTypeNodes.class)
                    .toCompletableFuture()
                    .handle((index, e) -> {
                        if (e != null) {
                            onException(response, e);
                        } else {
                            response.resume(index);
                        }
                        return null;
                    });
        } else {
            target
                    .path("component/index")
                    .queryParam("language", getLanguage(language, request))
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
                                            .setPath(link
                                                    .getPath()
                                                    .replaceFirst("/component/", "/application/")
                                                    .replace("/details?identifiers=", "/detail/")));
                            response.resume(index);
                        }
                        return null;
                    });
        }
    }

    @GET
    @Path("detail/{id}")
    public void getDetail(@Suspended final AsyncResponse response, @QueryParam("language") final String language,
            @PathParam("id") final String id,
            @QueryParam("configuration") @DefaultValue("false") final boolean configuration,
            @Context final HttpServletRequest request) {
        final String lang = ofNullable(request.getLocale()).map(Locale::getLanguage).orElse("en");
        if (configuration) {
            target
                    .path("configurationtype/details")
                    .queryParam("language", getLanguage(language, request))
                    .queryParam("identifiers", id)
                    .request(APPLICATION_JSON_TYPE)
                    .rx()
                    .get(ConfigTypeNodes.class)
                    .toCompletableFuture()
                    .thenCompose(result -> {
                        final ConfigTypeNode node = result.getNodes().values().iterator().next();
                        return uiSpecService.convert(extractFamilyFromNode(node.getId()), lang, node, null);
                    })
                    .handle((result, e) -> {
                        if (e != null) {
                            onException(response, e);
                        } else {
                            response.resume(result);
                        }
                        return null;
                    });
        } else {
            target
                    .path("component/details")
                    .queryParam("language", getLanguage(language, request))
                    .queryParam("identifiers", id)
                    .request(APPLICATION_JSON_TYPE)
                    .rx()
                    .get(ComponentDetailList.class)
                    .toCompletableFuture()
                    .thenCompose(result -> uiSpecService.convert(result.getDetails().iterator().next(), lang, null))
                    .handle((result, e) -> {
                        if (e != null) {
                            onException(response, e);
                        } else {
                            response.resume(result);
                        }
                        return null;
                    });
        }
    }

    private String getLanguage(final String language, final HttpServletRequest request) {
        return ofNullable(language)
                .orElseGet(() -> ofNullable(request.getLocale())
                        .map(Locale::getLanguage)
                        .filter(it -> !it.isEmpty())
                        .orElse("en"));
    }

    private String extractFamilyFromNode(final String id) {
        final String decoded = new String(Base64.getUrlDecoder().decode(id), StandardCharsets.UTF_8);
        if (decoded.startsWith("extension::")) {
            // quick workaround for now, we should just grab component extension manager probably
            final String[] split = decoded.split("::");
            return split[split.length - 1];
        }
        return decoded.split("#")[1];
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
            if (WebApplicationException.class.isInstance(actualException.getCause())) {
                final Response resp = WebApplicationException.class.cast(actualException.getCause()).getResponse();
                if (response != null) {
                    final String s = resp.readEntity(String.class);
                    response.resume(Response.status(resp.getStatus()).entity(s).type(APPLICATION_JSON_TYPE).build());
                    return;
                }
            }
            payload = actionService.map(new WebException(actualException, -1, emptyMap()));
        } else {
            log.error(e.getMessage(), e);
            status = Response.Status.BAD_GATEWAY.getStatusCode();
            payload = actionService.map(new WebException(e, -1, emptyMap()));
        }
        response.resume(new WebApplicationException(Response.status(status).entity(payload).build()));
    }
}
