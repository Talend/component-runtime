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

import static java.util.Collections.singletonList;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.talend.sdk.components.vault.client.VaultClient;

@Path("/api/v1/mock/vault")
@ApplicationScoped
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
public class VaultMock {

    @Inject
    private VaultClient client;

    @Context
    private HttpHeaders headers;

    private static int rateLimited = 0;

    private static int tokenRenewal = 0;

    @POST
    @Path("login")
    public VaultClient.AuthResponse login(final VaultClient.AuthRequest request) {
        String role = request.getRoleId();
        if ("rate-limit-except".equals(role)) {
            if (rateLimited == 0) {
                rateLimited++;
                throw new WebApplicationException(
                        Response.status(Status.TOO_MANY_REQUESTS).entity("Tried to many times").build());
            } else {
                role = "Test-Role";
            }
        }
        if (!"Test-Role".equals(role)) {
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
    public VaultClient.DecryptResponse decrypt(@HeaderParam("X-Vault-Token") final String xVaultToken,
            @PathParam("tenant") final String tenant, final VaultClient.DecryptRequest request) {
        String token = xVaultToken;
        if (token.equals("client-bad-token")) {
            if (tokenRenewal == 0) {
                tokenRenewal++;
                throw new WebApplicationException(
                        Response.status(400).entity("{\"errors\":[\"missing x_vault_token\"]}").build());
            } else {
                token = "client-test-token";
            }
        }
        if (!"client-test-token".equals(token) || tenant == null || tenant.isEmpty()
                || "x-talend-tenant-id".equals(tenant)) {
            throw new WebApplicationException(
                    Response.status(403).entity("{\"errors\":[\"missing vault_auth\"]}").build());
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

    @POST
    @Path("execute")
    public Response execute(@QueryParam("family") final String family, @QueryParam("type") final String type,
            @QueryParam("action") final String action, @QueryParam("lang") @DefaultValue("en") final String lang,
            final Map<String, String> params) {
        final Map<String, String> deciphered = client.decrypt(params, headers.getHeaderString("x-talend-tenant-id"));

        final Map result = new HashMap<String, String>() {

            {
                put("family", family);
                put("type", type);
                put("action", action);
                put("lang", lang);
                putAll(deciphered);
            }

        };
        return Response.ok(result).type(APPLICATION_JSON_TYPE).build();
    }
}
