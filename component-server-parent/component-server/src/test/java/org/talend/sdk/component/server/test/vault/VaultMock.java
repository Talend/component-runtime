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
package org.talend.sdk.component.server.test.vault;

import static java.util.Collections.singletonList;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.Consumes;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.talend.sdk.components.vault.client.VaultClient;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Path("mock/vault")
@ApplicationScoped
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class VaultMock {

    @POST
    @Path("login")
    public VaultClient.AuthResponse login(final VaultClient.AuthRequest request) {
        if (!"Test-Role".equals(request.getRoleId())) {
            throw new WebApplicationException(
                    Response.status(400).entity("{\"errors\":[\"missing role_id\"]}").build());
        }
        if (!"Test-Secret".equals(request.getSecretId())) {
            throw new WebApplicationException(
                    Response.status(400).entity("{\"errors\":[\"missing secret_id\"]}").build());
        }

        final VaultClient.Auth auth = new VaultClient.Auth();
        auth.setClientToken("client-test-token");
        auth.setRenewable(true);
        auth.setLeaseDuration(800);

        final VaultClient.AuthResponse response = new VaultClient.AuthResponse();
        response.setAuth(auth);
        return response;
    }

    @POST
    @Path("decrypt/{tenant}")
    public VaultClient.DecryptResponse decrypt(@HeaderParam("X-Vault-Token") final String token,
            @PathParam("tenant") final String tenant, final VaultClient.DecryptRequest request) {
        if (!"client-test-token".equals(token) || tenant == null || tenant.isEmpty()
                || "x-talend-tenant-id".equals(tenant)) {
            throw new ForbiddenException();
        }
        if (!"vault:v1:hcccVPODe9oZpcr/sKam8GUrbacji8VkuDRGfuDt7bg7VA=="
                .equals(request.getBatchInput().iterator().next().getCiphertext())) {
            throw new WebApplicationException(
                    Response.status(400).entity("{\"errors\":[\"wrong vault_encrypt\"]}").build());
        }

        final VaultClient.DecryptResult result = new VaultClient.DecryptResult();
        result.setPlaintext(Base64.getEncoder().encodeToString("test".getBytes(StandardCharsets.UTF_8)));

        final VaultClient.DecryptData data = new VaultClient.DecryptData();
        data.setBatchResults(singletonList(result));
        final VaultClient.DecryptResponse response = new VaultClient.DecryptResponse();
        response.setData(data);
        return response;
    }
}
