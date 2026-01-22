/**
 * Copyright (C) 2006-2026 Talend Inc. - www.talend.com
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
import static org.eclipse.microprofile.openapi.annotations.enums.ParameterIn.PATH;
import static org.eclipse.microprofile.openapi.annotations.enums.ParameterIn.QUERY;
import static org.eclipse.microprofile.openapi.annotations.enums.SchemaType.BOOLEAN;
import static org.eclipse.microprofile.openapi.annotations.enums.SchemaType.OBJECT;
import static org.eclipse.microprofile.openapi.annotations.enums.SchemaType.STRING;

import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.talend.sdk.component.server.front.model.ConfigTypeNodes;
import org.talend.sdk.component.server.front.model.error.ErrorPayload;

@Path("configurationtype")
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Tag(name = "Configuration Type",
        description = "Endpoints related to configuration types (reusable configuration) metadata access.")
public interface ConfigurationTypeResource {

    @GET
    @Path("index")
    @Operation(description = "Returns all available configuration type - storable models. "
            + "Note that the lightPayload flag allows to load all of them at once when you eagerly need "
            + " to create a client model for all configurations.")
    @APIResponse(responseCode = "200",
            description = "List of available and storable configurations (datastore, dataset, ...).",
            content = @Content(mediaType = APPLICATION_JSON))
    ConfigTypeNodes getRepositoryModel(
            @QueryParam("language") @DefaultValue("en") @Parameter(name = "language",
                    description = "Response language in i18n format.", in = QUERY,
                    schema = @Schema(type = STRING, defaultValue = "en")) String language,
            @QueryParam("lightPayload") @DefaultValue("true") @Parameter(name = "lightPayload",
                    description = "Should the payload skip the forms and actions associated to the configuration." +
                            "Default value is `true`.",
                    in = QUERY, schema = @Schema(type = BOOLEAN, defaultValue = "true")) boolean lightPayload,
            @QueryParam("q") @Parameter(name = "q",
                    description = "Query in simple query language to filter configurations. "
                            + "It provides access to the configuration `type`, `name`, `type` and "
                            + "first configuration property `metadata`. "
                            + "See component index endpoint for a syntax example.",
                    in = QUERY, schema = @Schema(type = STRING)) String query);

    @GET
    @Path("details")
    @Operation(operationId = "getConfigurationDetail",
            description = "Returns the set of metadata about one or multiples configuration identified by their 'id'.")
    @APIResponse(responseCode = "200",
            description = "List of details for the requested configuration.",
            content = @Content(mediaType = APPLICATION_JSON))
    ConfigTypeNodes getDetail(
            @QueryParam("language") @DefaultValue("en") @Parameter(name = "language",
                    description = "Response language in i18n format.",
                    in = QUERY,
                    schema = @Schema(type = STRING, defaultValue = "en")) String language,
            @QueryParam("identifiers") @Parameter(name = "identifiers",
                    description = "The identifier id to request. " +
                            "Repeat this parameter to request more than one element.",
                    in = QUERY) String[] ids);

    @POST
    @Path("migrate/{id}/{configurationVersion}")
    @Operation(operationId = "migrateConfiguration",
            description = "Allows to migrate a configuration without calling any component execution.")
    @APIResponse(responseCode = "200",
            description = "New values for that configuration (or the same if no migration was needed).",
            content = @Content(mediaType = APPLICATION_JSON))
    @APIResponse(responseCode = "404",
            description = "If the configuration is missing, payload will be an ErrorPayload with the code CONFIGURATION_MISSING.",
            content = @Content(mediaType = APPLICATION_JSON,
                    schema = @Schema(type = OBJECT, implementation = ErrorPayload.class)))
    @APIResponse(responseCode = "520",
            description = "An unexpected error occurred during migration, payload will be an ErrorPayload with the code UNEXPECTED.",
            content = @Content(mediaType = APPLICATION_JSON))
    Map<String, String> migrate(
            @PathParam("id") @Parameter(name = "id",
                    description = "The configuration identifier.",
                    in = PATH) String id,
            @PathParam("configurationVersion") @Parameter(name = "configurationVersion",
                    description = "The configuration version you send in provided body.",
                    in = PATH) int version,
            @RequestBody(
                    description = "Configuration to migrate in key/value json form.", required = true,
                    content = @Content(mediaType = APPLICATION_JSON,
                            schema = @Schema(type = OBJECT))) Map<String, String> config);
}
