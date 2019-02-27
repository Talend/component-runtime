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
package org.talend.sdk.component.server.extension.stitch.server.test;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.talend.sdk.component.server.extension.stitch.server.stitch.StitchClient;

@Path("stitch")
@ApplicationScoped
public class StitchMock {

    @GET
    @Path("source-types")
    public List<StitchClient.Steps> getSourceTypes(@HeaderParam(HttpHeaders.AUTHORIZATION) final String auth) {
        if (!"Bearer test-token".equals(auth)) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }
        return asList(new StitchClient.Steps("platform.postgres", 1, asList(new StitchClient.Step("form", null,
                asList(new StitchClient.Property("image_version", false, false, false, false, true, null),
                        new StitchClient.Property("frequency_in_minutes", false, false, false, false, true,
                                new StitchClient.JsonSchema("string", "^1$|^30$|^60$|^360$|^720$|^1440$", null, null)),
                        new StitchClient.Property("anchor_time", false, false, false, false, false,
                                new StitchClient.JsonSchema("string", null, "date-time", null)) // ....
                )), new StitchClient.Step("discover_schema", null, emptyList()),
                new StitchClient.Step("field_selection", null, emptyList()),
                new StitchClient.Step("fully_configured", null, emptyList()))),
                new StitchClient.Steps("platform.mysql", 1, singletonList(new StitchClient.Step("form", null,
                        singletonList(new StitchClient.Property("dbname", false, false, false, false, true, null))))));
    }
}
