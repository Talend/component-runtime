/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.proxy.front;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.proxy.model.ProxyErrorPayload;
import org.talend.sdk.component.proxy.test.WithProxy;
import org.talend.sdk.component.server.front.model.ErrorDictionary;

@WithProxy
class ActionResourceProxyTest {

    private final GenericType<Map<String, Object>> mapType = new GenericType<Map<String, Object>>() {

    };

    @Test
    void actionHealthcheckOK(final WebTarget proxyClient) {
        Map<String, Object> payload = singletonMap("configuration.url", "http://localhost");
        final Map<String, Object> result = proxyClient
                .path("action/execute")
                .queryParam("family", "TheTestFamily")
                .queryParam("type", "healthcheck")
                .queryParam("action", "validate-connection-1")
                .request(APPLICATION_JSON_TYPE)
                .post(entity(payload, APPLICATION_JSON_TYPE), mapType);

        assertNotNull(result);
        assertEquals("OK", result.get("status"));
        assertFalse(String.valueOf(result.get("comment")).isEmpty());
    }

    @Test
    void actionHealthcheckKO(final WebTarget proxyClient) {
        Map<String, Object> payload = singletonMap("configuration.url", "na://localhost");
        final Map<String, Object> result = proxyClient
                .path("action/execute")
                .queryParam("family", "TheTestFamily")
                .queryParam("type", "healthcheck")
                .queryParam("action", "validate-connection-1")
                .request(APPLICATION_JSON_TYPE)
                .post(entity(payload, APPLICATION_JSON_TYPE), mapType);
        assertNotNull(result);
        assertEquals("KO", result.get("status"));
        assertFalse(String.valueOf(result.get("comment")).isEmpty());
    }

    @Test
    void actionNull(final WebTarget proxyClient) {
        final WebApplicationException error = assertThrows(WebApplicationException.class, () -> {
            final Map<String, Object> result = proxyClient
                    .path("action/execute")
                    .queryParam("family", "TheTestFamily")
                    .queryParam("type", "healthcheck")
                    .request(APPLICATION_JSON_TYPE)
                    .post(entity(emptyMap(), APPLICATION_JSON_TYPE), mapType);
        });

        assertEquals(400, error.getResponse().getStatus());
        final ProxyErrorPayload errorPayload = error.getResponse().readEntity(ProxyErrorPayload.class);
        assertEquals("ACTION_MISSING", errorPayload.getCode());
    }

    @Test
    void actionInvalid(final WebTarget proxyClient) {
        final WebApplicationException error = assertThrows(WebApplicationException.class, () -> {
            final Map<String, Object> result = proxyClient
                    .path("action/execute")
                    .queryParam("family", "TheTestFamily")
                    .queryParam("type", "healthcheck")
                    .queryParam("action", "do-not-exist")
                    .request(APPLICATION_JSON_TYPE)
                    .post(entity(emptyMap(), APPLICATION_JSON_TYPE), mapType);
        });

        assertEquals(404, error.getResponse().getStatus());
        final ProxyErrorPayload errorPayload = error.getResponse().readEntity(ProxyErrorPayload.class);
        assertEquals("ACTION_MISSING", errorPayload.getCode());
    }

    @Test
    void unhandledErrorFromAction(final WebTarget proxyClient) {
        final WebApplicationException error = assertThrows(WebApplicationException.class, () -> {
            final Map<String, Object> result = proxyClient
                    .path("action/execute")
                    .queryParam("family", "TheTestFamily")
                    .queryParam("type", "dynamic_values")
                    .queryParam("action", "action-with-error")
                    .request(APPLICATION_JSON_TYPE)
                    .post(entity(emptyMap(), APPLICATION_JSON_TYPE), mapType);
        });

        assertEquals(520, error.getResponse().getStatus());
        final ProxyErrorPayload errorPayload = error.getResponse().readEntity(ProxyErrorPayload.class);
        assertEquals(ErrorDictionary.ACTION_ERROR.name(), errorPayload.getCode());
    }

}
