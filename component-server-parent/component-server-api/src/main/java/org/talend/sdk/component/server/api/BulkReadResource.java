/**
 * Copyright (C) 2006-2023 Talend Inc. - www.talend.com
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

import java.util.concurrent.CompletionStage;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.talend.sdk.component.server.front.model.BulkRequests;
import org.talend.sdk.component.server.front.model.BulkResponses;

@Path("bulk")
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Tag(name = "Bulk", description = "Enables to execute multiple requests at once.")
public interface BulkReadResource {

    @POST
    @Operation(description = "Takes a request aggregating N other endpoint requests and responds all results "
            + "in a normalized HTTP response representation.")
    @APIResponse(responseCode = "200", description = "The request payloads.",
            content = @Content(mediaType = APPLICATION_JSON))
    CompletionStage<BulkResponses> bulk(@RequestBody(
            description = "The requests list as json objects containing a list of request objects.  \n" +
                    "If your request contains multiple identifiers, you must use a list of string.  \n" +
                    "Example :  \n" +
                    "`{  \n" +
                    "\"requests\" : [  \n" +
                    "{  \n" +
                    "  \"path\" : \"/api/v1/component/index\",  \n" +
                    "  \"queryParameters\" : {\"identifiers\" : [\"12345\", \"6789A\"]},  \n" +
                    "  \"verb\" : \"GET\",  \n" +
                    "  \"headers\" : {...},  \n" +
                    "},  \n" +
                    "{ [...]}  \n" +
                    "]  \n" +
                    "}`  ",
            required = true,
            content = @Content(mediaType = APPLICATION_JSON)) final BulkRequests requests);
}
