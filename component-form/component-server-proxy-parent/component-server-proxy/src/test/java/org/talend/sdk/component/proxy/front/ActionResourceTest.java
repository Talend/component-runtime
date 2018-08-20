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
import static java.util.stream.Collectors.toMap;
import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.form.model.Ui;
import org.talend.sdk.component.proxy.model.ProxyErrorPayload;
import org.talend.sdk.component.proxy.service.qualifier.UiSpecProxy;
import org.talend.sdk.component.proxy.test.CdiInject;
import org.talend.sdk.component.proxy.test.WithServer;
import org.talend.sdk.component.server.front.model.ErrorDictionary;

@CdiInject
@WithServer
class ActionResourceTest {

    private final GenericType<Map<String, Object>> mapType = new GenericType<Map<String, Object>>() {

    };

    @Inject
    @UiSpecProxy
    private Jsonb jsonb;

    @Test
    void findRoots(final WebTarget proxyClient) {
        final Map<String, Object> result = proxyClient
                .path("actions/execute")
                .queryParam("family", "whatever")
                .queryParam("type", "dynamic_values")
                .queryParam("action", "builtin::roots")
                .request(APPLICATION_JSON_TYPE)
                .post(entity(emptyMap(), APPLICATION_JSON_TYPE), mapType);

        assertEquals(1, result.size());
        final List<Map<String, String>> items = List.class.cast(result.get("items"));
        assertEquals(4, items.size());
        final Map<String, String> itemsMap = items.stream().collect(toMap(e -> e.get("id"), e -> e.get("label")));
        assertEquals(new HashMap<String, String>() {

            {
                put("dGVzdC1jb21wb25lbnQjVGhlVGVzdEZhbWlseTIjZGF0YXN0b3JlI0Nvbm5lY3Rpb24tMQ", "Connection-1");
                put("dGVzdC1jb21wb25lbnQjVGhlVGVzdEZhbWlseSNkYXRhc3RvcmUjQ29ubmVjdGlvbi0y", "Connection-2");
                put("dGVzdC1jb21wb25lbnQjVGhlVGVzdEZhbWlseSNkYXRhc3RvcmUjQ29ubmVjdGlvbi0z", "Connection-3");
                put("dGVzdC1jb21wb25lbnQjVGhlVGVzdEZhbWlseSN0ZXN0I2RlZmF1bHR0ZXN0", "defaulttest");
            }
        }, itemsMap);
    }

    @Test
    void newForm(final WebTarget proxyClient) {
        final Map<String, Object> wrapper = proxyClient
                .path("actions/execute")
                .queryParam("family", "whatever")
                .queryParam("type", "reloadForm")
                .queryParam("action", "builtin::root::reloadFromId")
                .request(APPLICATION_JSON_TYPE)
                .post(entity(
                        singletonMap("id", "dGVzdC1jb21wb25lbnQjVGhlVGVzdEZhbWlseTIjZGF0YXN0b3JlI0Nvbm5lY3Rpb24tMQ"),
                        APPLICATION_JSON_TYPE), mapType);

        assertEquals(4, wrapper.size());
        final Map<String, Object> meta = Map.class.cast(wrapper.get("metadata"));
        assertEquals("myicon", meta.get("icon"));
        final Ui ui = jsonb.fromJson(jsonb.toJson(wrapper), Ui.class);
        assertNotNull(ui.getProperties());
        assertNotNull(ui.getJsonSchema());
        assertNotNull(ui.getUiSchema());
        // just some sanity checks, we assume the serialization works here and form-core already tested that part
        assertEquals(1, ui.getUiSchema().size());
        assertEquals(3, ui.getUiSchema().iterator().next().getItems().size());
    }

    @Test
    void newFormReset(final WebTarget proxyClient) {
        final Map<String, Object> wrapper = proxyClient
                .path("actions/execute")
                .queryParam("family", "whatever")
                .queryParam("type", "reloadForm")
                .queryParam("action", "builtin::root::reloadFromId")
                .request(APPLICATION_JSON_TYPE)
                .post(entity(singletonMap("id", ""), APPLICATION_JSON_TYPE), mapType);

        assertEquals(4, wrapper.size());
        final Ui ui = jsonb.fromJson(jsonb.toJson(wrapper), Ui.class);
        assertNotNull(ui.getProperties());
        assertNotNull(ui.getJsonSchema());
        assertNotNull(ui.getUiSchema());
        assertTrue(ui.getUiSchema().isEmpty()); // no enrichment in tests for datastore (other types are used)
    }

    @Test
    void actionHealthcheckOK(final WebTarget proxyClient) {
        final Map<String, Object> payload = singletonMap("configuration.url", "http://localhost");
        final Map<String, Object> result = proxyClient
                .path("actions/execute")
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
        final Map<String, Object> payload = singletonMap("configuration.url", "na://localhost");
        final Map<String, Object> result = proxyClient
                .path("actions/execute")
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
                    .path("actions/execute")
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
                    .path("actions/execute")
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
                    .path("actions/execute")
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
