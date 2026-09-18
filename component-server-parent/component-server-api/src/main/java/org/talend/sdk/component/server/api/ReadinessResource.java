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

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.talend.sdk.component.server.front.model.HealthStatus;

@Path("readiness")
@Tag(name = "Health", description = "Kubernetes readiness probe endpoint.")
public interface ReadinessResource {

    @GET
    @Produces(APPLICATION_JSON)
    @Operation(operationId = "getReadiness",
            description = "Readiness probe: returns 200 when the component index is loaded and the server is ready "
                    + "to serve traffic. Returns 503 with a cause otherwise.")
            @APIResponse(responseCode = "200",
                    description = "Server is ready.",
                    content = @Content(mediaType = APPLICATION_JSON,
                            schema = @Schema(implementation = HealthStatus.class)))
            @APIResponse(responseCode = "503",
                    description = "Server is not ready.",
                    content = @Content(mediaType = APPLICATION_JSON,
                            schema = @Schema(implementation = HealthStatus.class)))
    Response getReadiness();
}
