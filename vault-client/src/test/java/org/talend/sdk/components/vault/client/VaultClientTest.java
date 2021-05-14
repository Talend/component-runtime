/**
 * Copyright (C) 2006-2021 Talend Inc. - www.talend.com
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

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.apache.meecrowave.Meecrowave;
import org.apache.meecrowave.junit5.MonoMeecrowaveConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@MonoMeecrowaveConfig
class VaultClientTest {

    @Inject
    private Meecrowave meecrowave;

    @Inject
    private ClientSetup setup;

    @Inject
    private Client client;

    @Inject
    private VaultClient vault;

    private WebTarget base() {
        return base(meecrowave.getConfiguration().getHttpPort());
    }

    private WebTarget base(final int port) {
        return client.target("http://localhost:" + port);
    }

    @BeforeEach
    void setDecryptEndpoint() {
        vault.setDecryptEndpoint("/api/v1/mock/vault/decrypt/{x-talend-tenant-id}");
        vault.getCache().clear();
    }

    @Test
    void decryptWithTenant() {
        final HashMap<String, String> config = new HashMap<String, String>() {

            {
                put("configuration.username", "username0");
                put("configuration.password", "vault:v1:hcccVPODe9oZpcr/sKam8GUrbacji8VkuDRGfuDt7bg7VA==");
            }
        };
        final Map<String, String> result = vault.decrypt(config, "00001");
        assertEquals("username0", result.get("configuration.username"));
        assertEquals("test", result.get("configuration.password"));
    }

    @Test
    void decryptWithoutTenant() {
        vault.setDecryptEndpoint("/api/v1/mock/vault/decrypt/000001");
        final HashMap<String, String> config = new HashMap<String, String>() {

            {
                put("configuration.username", "username0");
                put("configuration.password", "vault:v1:hcccVPODe9oZpcr/sKam8GUrbacji8VkuDRGfuDt7bg7VA==");
            }
        };
        final Map<String, String> result = vault.decrypt(config, null);
        assertEquals("username0", result.get("configuration.username"));
        assertEquals("test", result.get("configuration.password"));
    }

    @Test
    void executeWithEncrypted() {
        final Response response = base()
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
                        put("configuration.password", "vault:v1:hcccVPODe9oZpcr/sKam8GUrbacji8VkuDRGfuDt7bg7VA==");
                    }
                }, APPLICATION_JSON_TYPE));
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
    void executeWithEncryptedAndInvalidTenant() {
        final Response response = base()
                .path("/api/v1/mock/vault/execute")
                .queryParam("family", "vault")
                .queryParam("type", "auth")
                .queryParam("action", "do")
                .queryParam("lang", "fr")
                .request(APPLICATION_JSON_TYPE)
                .header("x-talend-tenant-id", "")
                .post(Entity.entity(new HashMap<String, String>() {

                    {
                        put("configuration.username", "username0");
                        put("configuration.password", "vault:v1:hcccVPODe9oZpcr/sKam8GUrbacji8VkuDRGfuDt7bg7VA==");
                    }
                }, APPLICATION_JSON_TYPE));
        assertEquals(500, response.getStatus());
    }

    @Test
    void executeWithNothingEncrypted() {
        final Response response = base()
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
        assertEquals(200, response.getStatus());
        final Map<String, String> result = response.readEntity(Map.class);
        assertEquals("username0", result.get("configuration.username"));
        assertEquals("test", result.get("configuration.password"));
        assertEquals("vault", result.get("family"));
        assertEquals("auth", result.get("type"));
        assertEquals("do", result.get("action"));
        assertEquals("fr", result.get("lang"));
    }

    /**
     * To test with a "real" vault server instance
     * <code>
     * mysh> docker run -p 8200:8200 --cap-add=IPC_LOCK -e 'VAULT_DEV_ROOT_TOKEN_ID=00000000-0000-0000-0000-000000000000' -e 'VAULT_DEV_LISTEN_ADDRESS=0.0.0.0:8200' vault
     * mysh> docker exec -ti "dck_instance" sh
     * / # export VAULT_ADDR='http://0.0.0.0:8200'
     * / # vault login 00000000-0000-0000-0000-000000000000
     * / # vault auth enable approle
     * / # vault write auth/approle/role/Test-Role secret_id_ttl=0 token_ttl=4
     * / # vault read auth/approle/role/Test-Role/role-id                                  # get role_id
     * / # vault write -f auth/approle/role/Test-Role/secret-id                            # get secret_id
     * / # vault secrets enable transit
     * / # vault write -f transit/keys/test-tenant
     * / # vault write transit/encrypt/test-tenant plaintext="dGVzdAo="                    # use: echo -n "test" | base64
     * / # vault write transit/encrypt/test-tenant plaintext="VGhlIHF1aWNrIGJyb3duIGZveCBqdW1wcyBvdmVyIHRoZSBsYXp5IGRvZw==" # "The quick..."
     * add to policies:
     * path "transit/keys/test-tenant" {capabilities = ["read"]}
     * path "transit/decrypt/test-tenant" { capabilities = ["read", "update"]}
     * </code>
     */
    @Test
    // @Disabled
    void executeWithRealVault() {
        setup.setVaultUrl("http://localhost:8200");
        final Client realClt = setup.vaultClient(setup.vaultExecutorService());
        vault.setVault(setup.vaultTarget(realClt));
        vault.setAuthEndpoint("/v1/auth/approle/login");
        vault.setDecryptEndpoint("/v1/transit/decrypt/{x-talend-tenant-id}");
        vault.setRole(() -> "0230450d-e0fd-c5c8-d794-545e27bd3729");
        vault.setSecret(() -> "65d03209-c644-9877-70a3-694e258d6245");

        final HashMap<String, String> config = new HashMap<String, String>() {

            {
                put("configuration.username", "username0");
                put("configuration.password", "vault:v1:OdRahcsOe/c9S5qEIfknX6o/ohqktptW81Dw7ofjCtE=");
                put("foxy",
                        "vault:v1:pHmJIMI5xgVJwBX/0hPJa9koa846s/ru4yOYOOUr2DAMR+M4ZcIvB3JxWpfIW+XuQv0nt0G34vhbh16LJvmbiee3Nm6Teh4=");
            }
        };
        Map<String, String> result = vault.decrypt(config, "test-tenant");
        assertEquals("test", result.get("configuration.password"));
        assertEquals("The quick brown fox jumps over the lazy dog", result.get("foxy"));

        final Response response = base()
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

}