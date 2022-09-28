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
package org.talend.sdk.component.server.api;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.microprofile.openapi.annotations.enums.ParameterIn.PATH;
import static org.eclipse.microprofile.openapi.annotations.enums.ParameterIn.QUERY;
import static org.eclipse.microprofile.openapi.annotations.enums.SchemaType.OBJECT;
import static org.eclipse.microprofile.openapi.annotations.enums.SchemaType.STRING;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.talend.sdk.component.server.front.model.DocumentationContent;
import org.talend.sdk.component.server.front.model.error.ErrorPayload;

@Path("documentation")
@Tag(name = "Documentation", description = "Endpoint to retrieve embedded component documentation.")
public interface DocumentationResource {

    @GET
    @Path("component/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            description = "Returns an asciidoctor version of the documentation for the component represented by its identifier `id`.")
    @APIResponse(responseCode = "200",
            description = "the list of available and storable configurations (datastore, dataset, ...).",
            content = @Content(mediaType = APPLICATION_JSON))
    @APIResponse(responseCode = "404",
            description = "If the component is missing, payload will be an ErrorPayload with the code PLUGIN_MISSING.",
            content = @Content(mediaType = APPLICATION_JSON,
                    schema = @Schema(type = OBJECT, implementation = ErrorPayload.class)))
    DocumentationContent getDocumentation(
            @PathParam("id") @Parameter(name = "id", description = "the component identifier", in = PATH) String id,
            @QueryParam("language") @DefaultValue("en") @Parameter(name = "language",
                    description = "the language for display names.", in = QUERY,
                    schema = @Schema(type = STRING, defaultValue = "en")) String language,
            @QueryParam("segment") @DefaultValue("ALL") @Parameter(name = "segment",
                    description = "the part of the documentation to extract.", in = QUERY,
                    schema = @Schema(type = STRING, defaultValue = "ALL")) DocumentationSegment segment);

    enum DocumentationSegment {
        ALL,
        DESCRIPTION,
        CONFIGURATION
    }
}
