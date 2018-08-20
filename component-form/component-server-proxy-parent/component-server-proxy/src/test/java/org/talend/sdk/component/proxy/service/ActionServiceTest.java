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
package org.talend.sdk.component.proxy.service;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.json.bind.Jsonb;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.proxy.service.client.UiSpecContext;
import org.talend.sdk.component.proxy.service.qualifier.UiSpecProxy;
import org.talend.sdk.component.proxy.test.CdiInject;
import org.talend.sdk.component.proxy.test.WithServer;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpServer;

@CdiInject
@WithServer
class ActionServiceTest {

    @Inject
    private ActionService service;

    @Inject
    @UiSpecProxy
    private Jsonb jsonb;

    @Test
    void http() throws Exception {
        final HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/foo").setHandler(httpExchange -> {
            final Headers headers = httpExchange.getRequestHeaders();
            assertEquals("identity", headers.getFirst("Cookie"));
            final byte[] bytes = ("[{\"id\":\"5b164fa72fdfdsfds6564\",\"name\": \"1-Doc\"},"
                    + "{\"id\":\"541564d564sd5scddf\",\"name\":\"2-Beam\"}]").getBytes(StandardCharsets.UTF_8);
            httpExchange.sendResponseHeaders(200, bytes.length);
            httpExchange.getResponseBody().write(bytes);
            httpExchange.close();
        });
        server.start();
        try {
            final Map<String, Object> result = service
                    .findBuiltInAction("builtin::http::dynamic_values(url=${remoteHttpService}/foo,headers=cookie)",
                            new UiSpecContext("en", key -> {
                                if (key.equalsIgnoreCase("remoteHttpService")) {
                                    return "http://localhost:" + server.getAddress().getPort();
                                }
                                if (key.equalsIgnoreCase("cookie")) {
                                    return "identity";
                                }
                                throw new IllegalStateException("Unexpected key: " + key);
                            }), emptyMap())
                    .toCompletableFuture()
                    .get();
            assertEquals(singletonMap("items", asList(new HashMap<String, Object>() {

                {
                    put("id", "5b164fa72fdfdsfds6564");
                    put("label", "1-Doc");
                }
            }, new HashMap<String, Object>() {

                {
                    put("id", "541564d564sd5scddf");
                    put("label", "2-Beam");
                }
            })), result);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void references() throws Exception {
        final Map<String, Object> result = service
                .findBuiltInAction("builtin::references(type=thetype,name=thename)", new UiSpecContext("en", null),
                        emptyMap())
                .toCompletableFuture()
                .get();
        assertEquals(singletonMap("items", asList(new HashMap<String, Object>() {

            {
                put("id", "thetype1");
                put("label", "thename1");
            }
        }, new HashMap<String, Object>() {

            {
                put("id", "thetype2");
                put("label", "thename2");
            }
        })), result);
    }

    @Test
    void reloadFromParentId() throws Exception {
        final Map<String, Object> result = service
                .findBuiltInAction("builtin::root::reloadFromParentEntityId", new UiSpecContext("en", k -> null),
                        singletonMap("id", "actionServices.reloadFromParentId"))
                .toCompletableFuture()
                .get();
        final ActionService.NewForm form = jsonb.fromJson(jsonb.toJson(result), ActionService.NewForm.class);
        assertNotNull(form);
        assertNotNull(form.getJsonSchema());
        assertNotNull(form.getUiSchema());
        assertNotNull(form.getProperties());
        assertNotNull(form.getMetadata());
        assertEquals("dGVzdC1jb21wb25lbnQjVGhlVGVzdEZhbWlseTIjZGF0YXNldCNkYXRhc2V0LTE", form.getMetadata().getId());
        assertEquals("dataset-1", form.getJsonSchema().getTitle());
        assertEquals(3, form.getJsonSchema().getProperties().size()); // testConfig, config, $datasetMetadata
        assertEquals(3, form.getUiSchema().size());
        assertEquals(
                "{\"config\":{\"connection\":{"
                        + "\"$selfReference\":\"actionServices.reloadFromParentId\",\"url\":\"http://foo\"}},"
                        + "\"$formId\":\"dGVzdC1jb21wb25lbnQjVGhlVGVzdEZhbWlseTIjZGF0YXNldCNkYXRhc2V0LTE\"}",
                form.getProperties().toString());
    }
}
