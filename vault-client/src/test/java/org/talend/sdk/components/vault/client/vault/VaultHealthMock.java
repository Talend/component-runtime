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
package org.talend.sdk.components.vault.client.vault;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Mocks Vault's {@code v1/sys/health} endpoint so {@link org.talend.sdk.components.vault.client.VaultClient#ping()}
 * can be exercised against a controllable HTTP status code in tests.
 */
@Path("v1/sys/health")
@ApplicationScoped
public class VaultHealthMock {

    private static volatile int status = 200;

    public static void setStatus(final int newStatus) {
        status = newStatus;
    }

    public static void reset() {
        status = 200;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response health() {
        return Response.status(status).entity("{}").build();
    }
}
