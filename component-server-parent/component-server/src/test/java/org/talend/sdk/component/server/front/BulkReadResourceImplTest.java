/**
 * Copyright (C) 2006-2019 Talend Inc. - www.talend.com
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
import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;

import org.apache.meecrowave.junit5.MonoMeecrowaveConfig;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.server.front.model.BulkRequests;
import org.talend.sdk.component.server.front.model.BulkResponses;
import org.talend.sdk.component.server.test.ComponentClient;

@MonoMeecrowaveConfig
class BulkReadResourceImplTest {

    @Inject
    private WebTarget base;

    @Inject
    private ComponentClient client;

    @Test
    void valid() {
        final BulkResponses responses =
                base
                        .path("bulk")
                        .request(APPLICATION_JSON_TYPE)
                        .post(entity(
                                new BulkRequests(asList(
                                        new BulkRequests.Request(
                                                singletonMap(HttpHeaders.CONTENT_TYPE, singletonList(APPLICATION_JSON)),
                                                "/api/v1/component/index", emptyMap()),
                                        new BulkRequests.Request(
                                                singletonMap(HttpHeaders.CONTENT_TYPE, singletonList(APPLICATION_JSON)),
                                                "/api/v1/documentation/component/" + client.getJdbcId(), emptyMap()),
                                        new BulkRequests.Request(
                                                singletonMap(HttpHeaders.CONTENT_TYPE, singletonList(APPLICATION_JSON)),
                                                "/api/v1/documentation/component/"
                                                        + client.getComponentId("chain", "list"),
                                                emptyMap()))),
                                APPLICATION_JSON_TYPE), BulkResponses.class);
        final List<BulkResponses.Result> results = responses.getResponses();

        assertEquals(3, results.size());

        results.stream().limit(2).forEach(it -> assertEquals(HttpServletResponse.SC_OK, it.getStatus()));
        results.stream().skip(2).forEach(it -> assertEquals(HttpServletResponse.SC_NOT_FOUND, it.getStatus()));
        results.forEach(it -> assertEquals(singletonList("application/json"), it.getHeaders().get("Content-Type")));

        assertTrue(new String(results.get(0).getResponse(), StandardCharsets.UTF_8)
                .contains("\"pluginLocation\":\"org.talend.comp:jdbc-component:jar:0.0.1:compile\""));
        assertEquals(
                "{\n" + "  \"source\":\"== input\\n\\ndesc\\n\\n=== Configuration\\n\\nSomething1\\n\",\n"
                        + "  \"type\":\"asciidoc\"\n" + "}",
                new String(results.get(1).getResponse(), StandardCharsets.UTF_8));
        assertEquals(
                "{\n" + "  \"code\":\"COMPONENT_MISSING\",\n"
                        + "  \"description\":\"No component 'dGhlLXRlc3QtY29tcG9uZW50I2NoYWluI2xpc3Q'\"\n" + "}",
                new String(results.get(2).getResponse(), StandardCharsets.UTF_8));
    }

    @Test
    void forbidden() {
        final BulkResponses responses =
                base
                        .path("bulk")
                        .request(APPLICATION_JSON_TYPE)
                        .post(entity(new BulkRequests(singletonList(
                                new BulkRequests.Request(emptyMap(), "/api/v1/component/icon/1234", emptyMap()))),
                                APPLICATION_JSON_TYPE), BulkResponses.class);
        assertEquals(1, responses.getResponses().size());
        responses.getResponses().forEach(it -> assertEquals(HttpServletResponse.SC_FORBIDDEN, it.getStatus()));
    }

    @Test
    void error() {
        final BulkResponses responses = base
                .path("bulk")
                .request(APPLICATION_JSON_TYPE)
                .post(entity(new BulkRequests(singletonList(new BulkRequests.Request(emptyMap(),
                        "/api/v1/component/details", singletonMap("identifiers", singletonList("missing"))))),
                        APPLICATION_JSON_TYPE), BulkResponses.class);
        assertEquals(1, responses.getResponses().size());
        responses.getResponses().forEach(it -> assertEquals(HttpServletResponse.SC_BAD_REQUEST, it.getStatus()));
    }
}
