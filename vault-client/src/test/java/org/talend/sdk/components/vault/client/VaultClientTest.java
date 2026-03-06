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
package org.talend.sdk.components.vault.client;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import javax.enterprise.inject.se.SeContainer;
import javax.enterprise.inject.se.SeContainerInitializer;
import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.apache.meecrowave.Meecrowave;
import org.apache.meecrowave.junit5.MonoMeecrowaveConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.talend.sdk.components.vault.server.error.ErrorPayload;

@MonoMeecrowaveConfig
class VaultClientTest {

    @Inject
    private Meecrowave meecrowave;

    @Inject
    private VaultClientSetup setup;

    @Inject
    @VaultHttp
    private Client client;

    @Inject
    private VaultClient vault;

    private WebTarget vaultBase() {
        return vaultBase(meecrowave.getConfiguration().getHttpPort());
    }

    private WebTarget vaultBase(final int port) {
        return client.target("http://localhost:" + port);
    }

    @BeforeEach
    void setup() {
        vault.setDecryptEndpoint("/api/v1/mock/vault/decrypt/{x-talend-tenant-id}");
        vault.setRole(() -> "Test-Role");
        vault.setSecret(() -> "Test-Secret");
        vault.getAuthToken().set(null);
        vault.getCache().clear();
    }

    public static final HashMap<String, String> DEMO_MAP = new HashMap<String, String>() {

        {
            put("configuration.username", "username0");
            put("configuration.password", "vault:v1:hcccVPODe9oZpcr/sKam8GUrbacji8VkuDRGfuDt7bg7VA==");
        }
    };

    private void assertSuccess(final Response response) {
        assertEquals(200, response.getStatus());
        final Map<String, String> result = response.readEntity(Map.class);
        assertEquals("username0", result.get("configuration.username"));
        assertEquals("test", result.get("configuration.password"));
        assertEquals("vault", result.get("family"));
        assertEquals("auth", result.get("type"));
        assertEquals("do", result.get("action"));
        assertEquals("fr", result.get("lang"));
    }

    @Test
    void decryptWithTenant() {
        final HashMap<String, String> config = DEMO_MAP;
        final Map<String, String> result = vault.decrypt(config, "00001");
        assertEquals("username0", result.get("configuration.username"));
        assertEquals("test", result.get("configuration.password"));
    }

    @Test
    void decryptUsingCache() throws InterruptedException {
        final HashMap<String, String> config = DEMO_MAP;
        // first pass to fill cache with correct endpoint
        Map<String, String> result = vault.decrypt(config, "000001");
        assertEquals("username0", result.get("configuration.username"));
        assertEquals("test", result.get("configuration.password"));
        // with an invalid tenant (but w/ a valid token) we should have a 404 if cache weren't used...
        result = vault.decrypt(config, "");
        assertEquals("username0", result.get("configuration.username"));
        assertEquals("test", result.get("configuration.password"));
    }

    @Test
    void decryptWithoutTenant() {
        vault.setDecryptEndpoint("/api/v1/mock/vault/decrypt/000001");
        final HashMap<String, String> config = DEMO_MAP;
        final Map<String, String> result = vault.decrypt(config, null);
        assertEquals("username0", result.get("configuration.username"));
        assertEquals("test", result.get("configuration.password"));
    }

    @Test
    void executeWithEncrypted() {
        final Response response = vaultBase()
                .path("/api/v1/mock/vault/execute")
                .queryParam("family", "vault")
                .queryParam("type", "auth")
                .queryParam("action", "do")
                .queryParam("lang", "fr")
                .request(APPLICATION_JSON_TYPE)
                .header("x-talend-tenant-id", "test-tenant")
                .post(Entity.entity(DEMO_MAP, APPLICATION_JSON_TYPE));
        assertSuccess(response);
    }

    @Test
    void nullTenantInHeaders() {
        final Response response = vaultBase()
                .path("/api/v1/mock/vault/execute")
                .queryParam("family", "vault")
                .queryParam("type", "auth")
                .queryParam("action", "do")
                .queryParam("lang", "fr")
                .request(APPLICATION_JSON_TYPE)
                .header("x-talend-tenant-id", null)
                .post(Entity.entity(DEMO_MAP, APPLICATION_JSON_TYPE));
        assertEquals(404, response.getStatus());
        assertEquals("No header x-talend-tenant-id", response.readEntity(ErrorPayload.class).getDescription());
    }

    @Test
    void noRoleProvided() {
        vault.setToken(() -> "-");
        vault.setRole(() -> "-");
        final Response response = vaultBase()
                .path("/api/v1/mock/vault/execute")
                .queryParam("family", "vault")
                .queryParam("type", "auth")
                .queryParam("action", "do")
                .queryParam("lang", "fr")
                .request(APPLICATION_JSON_TYPE)
                .header("x-talend-tenant-id", "test-tenant")
                .post(Entity.entity(DEMO_MAP, APPLICATION_JSON_TYPE));
        assertEquals(400, response.getStatus());
        assertEquals("{\"errors\":[\"missing role_id\"]}", response.readEntity(ErrorPayload.class).getDescription());
    }

    @Test
    void noSecretProvided() {
        vault.setToken(() -> "-");
        vault.setSecret(() -> "-");
        final Response response = vaultBase()
                .path("/api/v1/mock/vault/execute")
                .queryParam("family", "vault")
                .queryParam("type", "auth")
                .queryParam("action", "do")
                .queryParam("lang", "fr")
                .request(APPLICATION_JSON_TYPE)
                .header("x-talend-tenant-id", "test-tenant")
                .post(Entity.entity(DEMO_MAP, APPLICATION_JSON_TYPE));
        assertEquals(400, response.getStatus());
        assertEquals("{\"errors\":[\"missing secret_id\"]}", response.readEntity(ErrorPayload.class).getDescription());
    }

    @Test
    void rateLimitReachedWithRetry() {
        vault.setRole(() -> "rate-limit-except");
        final long delay = 1000L;
        vault.setRefreshDelayOnFailure(delay);
        final long start = System.currentTimeMillis();
        final Response response = vaultBase()
                .path("/api/v1/mock/vault/execute")
                .queryParam("family", "vault")
                .queryParam("type", "auth")
                .queryParam("action", "do")
                .queryParam("lang", "fr")
                .request(APPLICATION_JSON_TYPE)
                .header("x-talend-tenant-id", "test-tenant")
                .post(Entity.entity(DEMO_MAP, APPLICATION_JSON_TYPE));
        final long end = System.currentTimeMillis();
        assertSuccess(response);
        assertTrue((end - start) > delay);
    }

    @Test
    void tokenBasedAuth() {
        vault.setRole(() -> "-");
        vault.setToken(() -> "client-test-token");
        vault.setRefreshDelayOnFailure(100L);
        final Response response = vaultBase()
                .path("/api/v1/mock/vault/execute")
                .queryParam("family", "vault")
                .queryParam("type", "auth")
                .queryParam("action", "do")
                .queryParam("lang", "fr")
                .request(APPLICATION_JSON_TYPE)
                .header("x-talend-tenant-id", "test-tenant")
                .post(Entity.entity(DEMO_MAP, APPLICATION_JSON_TYPE));
        assertSuccess(response);
    }

    @Test
    void badTokenWithRenew() {
        vault.setRole(() -> "-");
        vault.setToken(() -> "client-bad-token");
        vault.setRefreshDelayOnFailure(100L);
        final Response response = vaultBase()
                .path("/api/v1/mock/vault/execute")
                .queryParam("family", "vault")
                .queryParam("type", "auth")
                .queryParam("action", "do")
                .queryParam("lang", "fr")
                .request(APPLICATION_JSON_TYPE)
                .header("x-talend-tenant-id", "test-tenant")
                .post(Entity.entity(DEMO_MAP, APPLICATION_JSON_TYPE));
        assertSuccess(response);
    }

    @Test
    void badTokenWithOutRenew() {
        vault.setRole(() -> "-");
        vault.setToken(() -> "client-very-bad-token");
        vault.setRefreshDelayOnFailure(500L);
        final Response response = vaultBase()
                .path("/api/v1/mock/vault/execute")
                .queryParam("family", "vault")
                .queryParam("type", "auth")
                .queryParam("action", "do")
                .queryParam("lang", "fr")
                .request(APPLICATION_JSON_TYPE)
                .header("x-talend-tenant-id", "test-tenant")
                .post(Entity.entity(DEMO_MAP, APPLICATION_JSON_TYPE));
        assertEquals(403, response.getStatus());
        assertEquals("{\"errors\":[\"missing vault_auth\"]}", response.readEntity(ErrorPayload.class).getDescription());
    }

    @Test
    void executeWithNothingEncrypted() {
        final Response response = vaultBase()
                .path("/api/v1/mock/vault/execute")
                .queryParam("family", "vault")
                .queryParam("type", "auth")
                .queryParam("action", "do")
                .queryParam("lang", "fr")
                .request(APPLICATION_JSON_TYPE)
                .header("x-talend-tenant-id", "test-tenant")
                .post(Entity.entity(new HashMap<String, String>() {

                    {
                        put("configuration.username", "username0");
                        put("configuration.password", "test");
                    }
                }, APPLICATION_JSON_TYPE));
        assertSuccess(response);
    }

    @Test
    void executeWithBadEncrypted() {
        final Response response = vaultBase()
                .path("/api/v1/mock/vault/execute")
                .queryParam("family", "vault")
                .queryParam("type", "auth")
                .queryParam("action", "do")
                .queryParam("lang", "fr")
                .request(APPLICATION_JSON_TYPE)
                .header("x-talend-tenant-id", "test-tenant")
                .post(Entity.entity(new HashMap<String, String>() {

                    {
                        put("configuration.username", "username0");
                        put("configuration.password", "vault:v1:hccc");
                    }
                }, APPLICATION_JSON_TYPE));
        assertEquals(400, response.getStatus());
        assertEquals("{\"errors\":[\"wrong vault_encrypt\"]}",
                response.readEntity(ErrorPayload.class).getDescription());
    }

    /**
     * Demonstration purpose: with a "real" vault server instance
     * 
     * <code>
     * mysh>
     * mysh> docker exec -ti "dck_instance" sh
     * / # export VAULT_ADDR='http://0.0.0.0:8200'
     * / # vault login 00000000-0000-0000-0000-000000000000
     * / # vault auth enable approle
     * / # vault write auth/approle/role/Test-Role secret_id_ttl=256000 token_ttl=256000
     * / # vault read auth/approle/role/Test-Role/role-id                                  # get role_id
     * / # vault write -f auth/approle/role/Test-Role/secret-id                            # get secret_id
     * / # vault secrets enable transit
     * / # vault write -f transit/keys/test-tenant
     * / # vault write transit/encrypt/test-tenant plaintext="dGVzdA=="                    # use: echo -n "test" | base64
     * / # vault write transit/encrypt/test-tenant plaintext="VGhlIHF1aWNrIGJyb3duIGZveCBqdW1wcyBvdmVyIHRoZSBsYXp5IGRvZw==" # "The quick..."
     * add to policies:
     * path "transit/keys/test-tenant" {capabilities = ["read"]}
     * path "transit/decrypt/test-tenant" { capabilities = ["read", "update"]}
     * </code>
     */
    @Test
    @Disabled
    void executeWithRealVault() {
        // setup.setVaultUrl("http://localhost:8200");
        final Client realClt = setup.vaultClient(setup.vaultExecutorService());
        // vault.setVault(setup.vaultTarget(realClt));
        vault.setAuthEndpoint("/v1/auth/approle/login");
        vault.setDecryptEndpoint("/v1/transit/decrypt/{x-talend-tenant-id}");
        vault.setRole(() -> "_role-id_");
        vault.setSecret(() -> "_secret-id_");

        final HashMap<String, String> config = new HashMap<String, String>() {

            {
                put("configuration.username", "username0");
                put("configuration.password", "vault:v1:_test_encrypted_");
                put("foxy", "vault:v1:_the_quick_brown_encrypted_");
            }
        };
        Map<String, String> result = vault.decrypt(config, "test-tenant");
        assertEquals("test", result.get("configuration.password"));
        assertEquals("The quick brown fox jumps over the lazy dog", result.get("foxy"));

        final Response response = vaultBase()
                .path("/api/v1/mock/vault/execute")
                .queryParam("family", "vault")
                .queryParam("type", "auth")
                .queryParam("action", "do")
                .queryParam("lang", "fr")
                .request(APPLICATION_JSON_TYPE)
                .header("x-talend-tenant-id", "test-tenant")
                .post(Entity.entity(config, APPLICATION_JSON_TYPE));

        assertEquals(200, response.getStatus());
        result = response.readEntity(Map.class);
        assertEquals("username0", result.get("configuration.username"));
        assertEquals("test", result.get("configuration.password"));
        assertEquals("The quick brown fox jumps over the lazy dog", result.get("foxy"));
        assertEquals("vault", result.get("family"));
        assertEquals("auth", result.get("type"));
        assertEquals("do", result.get("action"));
        assertEquals("fr", result.get("lang"));
    }

    @Test
    @Disabled
    /**
     * Demonstration purpose: using a standalone container
     *
     * add to deps : openwebbeans-se
     */
    void testWithStandaloneContainer() {
        System.setProperty("talend.vault.cache.vault.auth.roleId", "_role-id_");
        System.setProperty("talend.vault.cache.vault.auth.secretId", "_secret-id_");
        System.setProperty("talend.vault.cache.vault.url", "http://localhost:8200");
        System.setProperty("talend.vault.cache.vault.auth.endpoint", "/v1/auth/approle/login");
        System.setProperty("talend.vault.cache.vault.decrypt.endpoint", "/v1/transit/decrypt/test-tenant");
        try (final SeContainer container = SeContainerInitializer
                .newInstance()
                .addProperty("skipHttp", true)
                .addProperty("org.apache.webbeans.scanBeansXmlOnly", "true")
                .initialize()) {
            final VaultClient service = container.select(VaultClient.class).get();
            final HashMap<String, String> config = new HashMap<String, String>() {

                {
                    put("configuration.username", "username0");
                    put("configuration.password", "vault:v1:_test_encrypted_");
                }
            };
            final Map<String, String> result = service.decrypt(config);
            assertEquals("username0", result.get("configuration.username"));
            assertEquals("test", result.get("configuration.password"));
        }
    }
}