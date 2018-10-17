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
package org.talend.sdk.component.proxy.front;

import static java.util.Optional.ofNullable;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.talend.sdk.component.proxy.config.SwaggerDoc.ERROR_HEADER_DESC;

import java.util.Map;
import java.util.concurrent.CompletionStage;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;

import org.talend.sdk.component.proxy.front.error.AutoErrorHandling;
import org.talend.sdk.component.proxy.model.ProxyErrorPayload;
import org.talend.sdk.component.proxy.service.ActionService;
import org.talend.sdk.component.proxy.service.ErrorProcessor;
import org.talend.sdk.component.proxy.service.PlaceholderProviderFactory;
import org.talend.sdk.component.proxy.service.client.UiSpecContext;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.ResponseHeader;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@AutoErrorHandling
@Api(description = "Endpoint responsible to handle any server side interaction (validation, healthcheck, ...)",
        tags = { "action" })
@ApplicationScoped
@Path("actions")
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
public class ActionResource {

    @Inject
    private ActionService service;

    @Inject
    private PlaceholderProviderFactory placeholderProviderFactory;

    @ApiOperation(value = "This endpoint execute an action required by a form.",
            notes = "configuration types has action that can be executed using this endpoint",
            tags = { "action", "configurations" }, produces = APPLICATION_JSON, consumes = APPLICATION_JSON,
            responseContainer = "Map",
            responseHeaders = { @ResponseHeader(name = ErrorProcessor.Constants.HEADER_TALEND_COMPONENT_SERVER_ERROR,
                    description = ERROR_HEADER_DESC, response = Boolean.class) })
    @ApiResponses({
            @ApiResponse(code = HttpServletResponse.SC_NOT_FOUND,
                    message = "This response is returned when no action is found", response = ProxyErrorPayload.class),
            @ApiResponse(code = SC_BAD_REQUEST, message = "This response is returned when the action is null",
                    response = ProxyErrorPayload.class),
            @ApiResponse(code = 520, message = "This response is returned when the action raise an unhandled error",
                    response = ProxyErrorPayload.class) })
    @POST
    @Path("execute")
    public CompletionStage<Map<String, Object>> execute(@QueryParam("family") final String family,
            @QueryParam("type") final String type, @QueryParam("action") final String action,
            @QueryParam("language") final String lang, @Context final HttpServletRequest request,
            final Map<String, Object> params) {
        return service
                .createStage(family, type, action, new UiSpecContext(ofNullable(lang).orElse("en"),
                        placeholderProviderFactory.newProvider(request)), params);
    }
}
