/**
 * Copyright (C) 2006-2025 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.server.front;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static jakarta.ws.rs.client.Entity.entity;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.stream.IntStream;

import javax.inject.Inject;
import javax.json.bind.Jsonb;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.HttpHeaders;

import org.apache.meecrowave.junit5.MonoMeecrowaveConfig;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.server.front.model.BulkRequests;
import org.talend.sdk.component.server.front.model.BulkResponses;
import org.talend.sdk.component.server.service.qualifier.ComponentServer;
import org.talend.sdk.component.server.test.ComponentClient;

@MonoMeecrowaveConfig
class BulkReadResourceImplTest {

    @Inject
    private WebTarget base;

    @Inject
    private ComponentClient client;

    @Inject
    @ComponentServer
    private Jsonb defaultMapper;

    @Test
    void valid() {
        final BulkRequests.Request okTrigger = new BulkRequests.Request(HttpMethod.POST, "{\"enum\":\"V1\"}",
                singletonMap(HttpHeaders.CONTENT_TYPE, singletonList(APPLICATION_JSON)), "/api/v1/action/execute",
                new HashMap<String, List<String>>() {

                    {
                        put("type", singletonList("user"));
                        put("family", singletonList("jdbc"));
                        put("action", singletonList("custom"));
                    }
                });
        final BulkResponses responses =
                base
                        .path("bulk")
                        .request(APPLICATION_JSON_TYPE)
                        .post(entity(
                                new BulkRequests(asList(
                                        new BulkRequests.Request(HttpMethod.GET, null,
                                                singletonMap(HttpHeaders.CONTENT_TYPE, singletonList(APPLICATION_JSON)),
                                                "/api/v1/component/index", emptyMap()),
                                        new BulkRequests.Request(HttpMethod.GET, null,
                                                singletonMap(HttpHeaders.CONTENT_TYPE, singletonList(APPLICATION_JSON)),
                                                "/api/v1/documentation/component/" + client.getJdbcId(), emptyMap()),
                                        new BulkRequests.Request(HttpMethod.GET, null,
                                                singletonMap(HttpHeaders.CONTENT_TYPE, singletonList(APPLICATION_JSON)),
                                                "/api/v1/documentation/component/"
                                                        + client.getComponentId("chain", "list"),
                                                emptyMap()),
                                        okTrigger,
                                        new BulkRequests.Request(HttpMethod.POST, "{\"enum\":\"FAIL\"}",
                                                singletonMap(HttpHeaders.CONTENT_TYPE, singletonList(APPLICATION_JSON)),
                                                "/api/v1/action/execute", new HashMap<String, List<String>>() {

                                                    {
                                                        put("type", singletonList("user"));
                                                        put("family", singletonList("jdbc"));
                                                        put("action", singletonList("custom"));
                                                    }
                                                }),
                                        okTrigger)),
                                APPLICATION_JSON_TYPE), BulkResponses.class);
        final List<BulkResponses.Result> results = responses.getResponses();

        assertEquals(6, results.size());

        IntStream
                .of(0, 1, 3, 5)
                .mapToObj(results::get)
                .forEach(it -> assertEquals(HttpServletResponse.SC_OK, it.getStatus()));
        assertEquals(HttpServletResponse.SC_NOT_FOUND, results.get(2).getStatus());
        assertEquals(520, results.get(4).getStatus());
        results.forEach(it -> assertEquals(singletonList("application/json"), it.getHeaders().get("Content-Type")));

        assertEquals("{\"value\":\"V1\"}", results.get(3).getResponse().toString());
        assertEquals(
                "{\"code\":\"ACTION_ERROR\",\"description\":\"Action execution failed with: this action failed intentionally\"}",
                results.get(4).getResponse().toString());

        assertTrue(results.get(0)
                .getResponse()
                .asJsonObject()
                .toString()
                .contains("org.talend.comp:jdbc-component:jar:0.0.1:compile"));

        assertEquals("{\"source\":\"== input\\n\\ndesc\\n\\n=== Configuration\\n\\nSomething1\",\"type\":\"asciidoc\"}",
                results.get(1).getResponse().toString());
        assertEquals(
                "{\"code\":\"COMPONENT_MISSING\",\"description\":\"No component 'dGhlLXRlc3QtY29tcG9uZW50I2NoYWluI2xpc3Q'\"}",
                results.get(2).getResponse().toString());
    }

    @Test
    void forbidden() {
        final BulkResponses responses =
                base
                        .path("bulk")
                        .request(APPLICATION_JSON_TYPE)
                        .post(entity(
                                new BulkRequests(singletonList(new BulkRequests.Request(HttpMethod.GET, null,
                                        emptyMap(), "/api/v1/component/icon/1234", emptyMap()))),
                                APPLICATION_JSON_TYPE), BulkResponses.class);
        assertEquals(1, responses.getResponses().size());
        responses.getResponses().forEach(it -> assertEquals(HttpServletResponse.SC_FORBIDDEN, it.getStatus()));
    }

    @Test
    void error() {
        final BulkResponses responses = base
                .path("bulk")
                .request(APPLICATION_JSON_TYPE)
                .post(entity(
                        new BulkRequests(singletonList(new BulkRequests.Request(HttpMethod.GET, null, emptyMap(),
                                "/api/v1/component/details", singletonMap("identifiers", singletonList("missing"))))),
                        APPLICATION_JSON_TYPE), BulkResponses.class);
        assertEquals(1, responses.getResponses().size());
        responses.getResponses().forEach(it -> assertEquals(HttpServletResponse.SC_BAD_REQUEST, it.getStatus()));
    }
}
