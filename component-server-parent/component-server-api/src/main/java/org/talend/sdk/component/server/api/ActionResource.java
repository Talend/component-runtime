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
package org.talend.sdk.component.server.api;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.microprofile.openapi.annotations.enums.ParameterIn.QUERY;
import static org.eclipse.microprofile.openapi.annotations.enums.SchemaType.OBJECT;
import static org.eclipse.microprofile.openapi.annotations.enums.SchemaType.STRING;

import java.util.Map;
import java.util.concurrent.CompletionStage;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.talend.sdk.component.server.front.model.ActionList;
import org.talend.sdk.component.server.front.model.error.ErrorPayload;

@Path("action")
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Tag(name = "Action", description = "Endpoints related to callbacks/triggers execution.")
public interface ActionResource {

    @POST
    @Path("execute")
    @Operation(
            description = "This endpoint will execute any UI action and serialize the response as a JSON (pojo model). "
                    + "It takes as input the family, type and name of the related action to identify it and its configuration "
                    + "as a flat key value set using the same kind of mapping than for components (option path as key).")
    @APIResponse(responseCode = "200",
            description = "The action payload serialized in JSON.",
            content = @Content(mediaType = APPLICATION_JSON))
    @APIResponse(responseCode = "400",
            description = "If the action is not set, payload will be an ErrorPayload with the code ACTION_MISSING.",
            content = @Content(mediaType = APPLICATION_JSON,
                    schema = @Schema(type = OBJECT, implementation = ErrorPayload.class)))
    @APIResponse(responseCode = "404",
            description = "If the action can't be found, payload will be an ErrorPayload with the code ACTION_MISSING.",
            content = @Content(mediaType = APPLICATION_JSON,
                    schema = @Schema(type = OBJECT, implementation = ErrorPayload.class)))
    @APIResponse(responseCode = "520",
            description = "If the action execution failed, payload will be an ErrorPayload with the code ACTION_ERROR.",
            content = @Content(mediaType = APPLICATION_JSON,
                    schema = @Schema(type = OBJECT, implementation = ErrorPayload.class)))
    CompletionStage<Response> execute(
            @QueryParam("family") @Parameter(name = "family", required = true, in = QUERY,
                    description = "Component family.") String family,
            @QueryParam("type") @Parameter(name = "type", required = true, in = QUERY,
                    description = "Type of action.") String type,
            @QueryParam("action") @Parameter(name = "action", required = true, in = QUERY,
                    description = "Action name.") String action,
            @QueryParam("lang") @DefaultValue("en") @Parameter(name = "lang", in = QUERY,
                    description = "Requested language (as in a Locale) if supported by the action.",
                    schema = @Schema(defaultValue = "en", type = STRING)) String lang,
            @RequestBody(description = "Action parameters in key/value flat json form.", required = true,
                    content = @Content(mediaType = APPLICATION_JSON,
                            schema = @Schema(type = OBJECT))) Map<String, String> params);

    @GET
    @Path("index")
    @Operation(operationId = "getActionIndex",
            description = "This endpoint returns the list of available actions for a certain family and potentially filters the "
                    + "output limiting it to some families and types of actions.")
    @APIResponse(responseCode = "200",
            description = "The action index.",
            content = @Content(mediaType = APPLICATION_JSON))
    ActionList getIndex(
            @QueryParam("type") @Parameter(name = "type", in = QUERY,
                    description = "Filter the response by type." +
                            "Repeat this parameter to request more than one type.") String[] types,
            @QueryParam("family") @Parameter(name = "family", in = QUERY,
                    description = "Filter the response by family." +
                            "Repeat this parameter to request more than one family.") String[] families,
            @QueryParam("language") @Parameter(name = "language",
                    description = "Response language in i18n format.", in = QUERY,
                    schema = @Schema(defaultValue = "en", type = STRING)) @DefaultValue("en") String language);
}
