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

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import java.io.InputStream;
import java.util.concurrent.CompletionStage;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.talend.sdk.component.runtime.server.vault.proxy.endpoint.jaxrs.RequestForwarder;

@Path("bulk")
@ApplicationScoped
public class BulkReadResourceProxy {

    private final String warningMessage = "This endpoint does not use caching, "
            + "it is better to stick to other endpoints and rely on client caching with an "
            + "eviction based on /environment endpoint timestamp.";

    @Inject
    private RequestForwarder forwarder;

    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public CompletionStage<Response> bulk(final InputStream payload) {
        return forwarder
                .forward(payload,
                        response -> Response
                                .fromResponse(response)
                                .header("Talend-Vault-Cache", "false")
                                .header("Talend-Vault-Warning", warningMessage)
                                .build());
    }
}
