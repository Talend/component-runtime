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
package org.talend.sdk.component.runtime.server.vault.proxy.proxy;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.joining;
import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.talend.sdk.component.runtime.server.vault.proxy.service.http.Http.Type.TALEND_COMPONENT_KIT;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;

import org.apache.meecrowave.Meecrowave;
import org.apache.meecrowave.junit5.MonoMeecrowaveConfig;
import org.apache.meecrowave.testing.ConfigurationInject;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.talend.sdk.component.runtime.server.vault.proxy.service.http.Http;
import org.talend.sdk.component.server.front.model.ActionList;
import org.talend.sdk.component.server.front.model.BulkRequests;
import org.talend.sdk.component.server.front.model.BulkResponses;
import org.talend.sdk.component.server.front.model.DocumentationContent;
import org.talend.sdk.component.server.front.model.Environment;

@MonoMeecrowaveConfig
class ComponentServerProxyTest {

    @ConfigurationInject
    private Meecrowave.Builder serverConfig;

    @Inject
    @Http(TALEND_COMPONENT_KIT) // not important in tests
    private Client client;

    @Test
    void environment() {
        final Environment environment =
                base().path("api/v1/environment").request(APPLICATION_JSON_TYPE).get(Environment.class);
        assertEquals(1, environment.getLatestApiVersion());
        assertEquals("test", environment.getCommit());
    }

    @RepeatedTest(2) // this also checks the cache and queries usage
    void actionIndex() {
        { // default
            final ActionList index =
                    base().path("api/v1/action/index").request(APPLICATION_JSON_TYPE).get(ActionList.class);
            assertEquals(1, index.getItems().size());
            assertEquals("testf", index.getItems().iterator().next().getComponent());
        }
        { // change the family
            final ActionList index = base()
                    .path("api/v1/action/index")
                    .queryParam("family", "foo")
                    .request(APPLICATION_JSON_TYPE)
                    .get(ActionList.class);
            assertEquals(1, index.getItems().size());
            assertEquals("foo", index.getItems().iterator().next().getComponent());
        }
    }

    // this also checks the cache and path usage
    @ParameterizedTest
    @ValueSource(strings = { "first", "second", "first", "second", "third" })
    void documentation(final String id) {
        final DocumentationContent content = base()
                .path("api/v1/documentation/component/{id}")
                .resolveTemplate("id", id)
                .request(APPLICATION_JSON_TYPE)
                .get(DocumentationContent.class);
        assertEquals(id, content.getSource());
    }

    @Test
    void action() { // the mock returns its input to allow is to test input is deciphered
        final Map<String, String> decrypted = base()
                .path("api/v1/action/execute")
                .queryParam("family", "testf")
                .queryParam("type", "testt")
                .queryParam("action", "testa")
                .queryParam("lang", "testl")
                .request(APPLICATION_JSON_TYPE)
                .header("x-talend-tenant-id", "test-tenant")
                .post(entity(new HashMap<String, String>() {

                    {
                        put("configuration.username", "simple");
                        put("configuration.password", "vault:v1:hcccVPODe9oZpcr/sKam8GUrbacji8VkuDRGfuDt7bg7VA==");
                    }
                }, APPLICATION_JSON_TYPE), new GenericType<Map<String, String>>() {
                });
        assertEquals(new HashMap<String, String>() {

            {
                // original value (not ciphered)
                put("configuration.username", "simple");
                // deciphered value
                put("configuration.password", "test");
                // action input config
                put("family", "testf");
                put("type", "testt");
                put("action", "testa");
                put("lang", "testl");
            }
        }, decrypted);
    }

    @Test
    void actionClear() {
        final Map<String, String> decrypted = base()
                .path("api/v1/action/execute")
                .queryParam("family", "testf")
                .queryParam("type", "testt")
                .queryParam("action", "testa")
                .queryParam("lang", "testl")
                .request(APPLICATION_JSON_TYPE)
                .header("x-talend-tenant-id", "test-tenant")
                .post(entity(new HashMap<String, String>() {

                    {
                        put("configuration.username", "simple");
                        put("configuration.password", "clearValue");
                    }
                }, APPLICATION_JSON_TYPE), new GenericType<Map<String, String>>() {
                });
        assertEquals(new HashMap<String, String>() {

            {
                // original value (not ciphered)
                put("configuration.username", "simple");
                // passthrough value
                put("configuration.password", "clearValue");
                // action input config
                put("family", "testf");
                put("type", "testt");
                put("action", "testa");
                put("lang", "testl");
            }
        }, decrypted);
    }

    @Test
    void bulk() {
        final BulkResponses results =
                base()
                        .path("api/v1/bulk")
                        .request(APPLICATION_JSON_TYPE)
                        .post(entity(
                                new BulkRequests(asList(
                                        new BulkRequests.Request(null, null,
                                                singletonMap(HttpHeaders.CONTENT_TYPE, singletonList(APPLICATION_JSON)),
                                                "/api/v1/component/index", emptyMap()),
                                        new BulkRequests.Request("GET", null,
                                                singletonMap(HttpHeaders.CONTENT_TYPE, singletonList(APPLICATION_JSON)),
                                                "/api/v1/documentation/component/mocked", emptyMap()))),
                                APPLICATION_JSON_TYPE), BulkResponses.class);
        assertEquals("200/ok\n400/bad",
                results
                        .getResponses()
                        .stream()
                        .map(it -> it.getStatus() + "/" + new String(it.getResponse(), StandardCharsets.UTF_8))
                        .collect(joining("\n")));
    }

    private WebTarget base() {
        return client.target("http://localhost:" + serverConfig.getHttpPort());
    }
}
