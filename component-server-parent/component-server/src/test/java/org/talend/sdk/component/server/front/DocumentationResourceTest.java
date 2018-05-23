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
package org.talend.sdk.component.server.front;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.inject.Inject;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.apache.meecrowave.junit5.MonoMeecrowaveConfig;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.server.front.model.DocumentationContent;
import org.talend.sdk.component.server.front.model.ErrorDictionary;
import org.talend.sdk.component.server.front.model.error.ErrorPayload;
import org.talend.sdk.component.server.test.ComponentClient;
import org.talend.sdk.component.server.test.websocket.WebsocketClient;

@MonoMeecrowaveConfig
class DocumentationResourceTest {

    @Inject
    private WebTarget base;

    @Inject
    private ComponentClient client;

    @Inject
    private WebsocketClient ws;

    @Test
    void wsDoc() {
        final DocumentationContent content = ws.read(DocumentationContent.class, "GET",
                "/documentation/component/" + client.getJdbcId() + "?format=html", null);
        assertEquals("html", content.getType());
        assertEquals(
                "<h1 id=\"_test\">Test</h1>\n" + "<h2 id=\"_component\">Component</h2>\n"
                        + "<div class=\"paragraph\">\n" + "<p>\n" + "Something\n" + "</p>\n" + "</div>\n",
                content.getSource());
    }

    @Test
    void getDoc() {
        final DocumentationContent content = base
                .path("documentation/component/{id}")
                .resolveTemplate("id", client.getJdbcId())
                .request(APPLICATION_JSON_TYPE)
                .get(DocumentationContent.class);
        assertEquals("asciidoc", content.getType());
        assertEquals("= Test\n\n== Component\n\nSomething", content.getSource());
    }

    @Test
    void getDocHtml() {
        final DocumentationContent content = base
                .path("documentation/component/{id}")
                .resolveTemplate("id", client.getJdbcId())
                .queryParam("format", "html")
                .queryParam("headerFooter", false)
                .request(APPLICATION_JSON_TYPE)
                .get(DocumentationContent.class);
        assertEquals("html", content.getType());
        assertEquals(
                "<h1 id=\"_test\">Test</h1>\n" + "<h2 id=\"_component\">Component</h2>\n"
                        + "<div class=\"paragraph\">\n" + "<p>\n" + "Something\n" + "</p>\n" + "</div>\n",
                content.getSource());
    }

    @Test
    void missingDoc() {
        final String id = client.getComponentId("chain", "list");
        final Response response = base
                .path("documentation/component/{id}")
                .resolveTemplate("id", id)
                .request(APPLICATION_JSON_TYPE)
                .get();
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
        final ErrorPayload payload = response.readEntity(ErrorPayload.class);
        assertEquals(ErrorDictionary.COMPONENT_MISSING, payload.getCode());
        assertEquals("No component '" + id + "'", payload.getDescription());
    }
}
