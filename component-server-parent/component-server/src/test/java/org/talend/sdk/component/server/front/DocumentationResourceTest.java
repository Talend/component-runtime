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
    void selectById() {
        final String foo = new DocumentationResource()
                .selectById("Foo1",
                        "== Foo0\n\n00000\n\n" + "=== Configuration\n\nWhatever0\n\n== Foo1\n\nThe description\n\n"
                                + "=== Configuration\n\nWhatever1\n\n"
                                + "== Foo2\n\n2222\n\n=== Configuration\n\nWhatever2",
                        DocumentationResource.DocumentationSegment.DESCRIPTION);
        assertEquals("The description", foo.trim());
    }

    @Test
    void wsDoc() {
        final DocumentationContent content =
                ws.read(DocumentationContent.class, "GET", "/documentation/component/" + client.getJdbcId(), null);
        assertEquals("== input\n\ndesc\n\n=== Configuration\n\nSomething1\n", content.getSource());
    }

    @Test
    void getDoc() {
        final DocumentationContent content = base
                .path("documentation/component/{id}")
                .resolveTemplate("id", client.getJdbcId())
                .request(APPLICATION_JSON_TYPE)
                .get(DocumentationContent.class);
        assertEquals("asciidoc", content.getType());
        assertEquals("== input\n\ndesc\n\n=== Configuration\n\nSomething1\n", content.getSource());
    }

    @Test
    void getDocDescription() {
        final DocumentationContent content = base
                .path("documentation/component/{id}")
                .resolveTemplate("id", client.getJdbcId())
                .queryParam("segment", "DESCRIPTION")
                .request(APPLICATION_JSON_TYPE)
                .get(DocumentationContent.class);
        assertEquals("asciidoc", content.getType());
        assertEquals("desc", content.getSource().trim());
    }

    @Test
    void getDocConfig() {
        final DocumentationContent content = base
                .path("documentation/component/{id}")
                .resolveTemplate("id", client.getJdbcId())
                .queryParam("segment", "CONFIGURATION")
                .request(APPLICATION_JSON_TYPE)
                .get(DocumentationContent.class);
        assertEquals("asciidoc", content.getType());
        assertEquals("Something1", content.getSource().trim());
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

    @Test
    void selectDocByName() {
        {
            final String id = client.getComponentId("jdbc", "input");
            final String response = base
                    .path("documentation/component/{id}")
                    .resolveTemplate("id", id)
                    .request(APPLICATION_JSON_TYPE)
                    .get(DocumentationContent.class)
                    .getSource();
            assertEquals("== input\n\ndesc\n\n=== Configuration\n\nSomething1\n", response);
        }
        {
            final String id = client.getComponentId("jdbc", "output");
            final String response = base
                    .path("documentation/component/{id}")
                    .resolveTemplate("id", id)
                    .request(APPLICATION_JSON_TYPE)
                    .get(DocumentationContent.class)
                    .getSource();
            assertEquals("== output\n\n=== Configuration\n\nSomething else", response);
        }
    }

    @Test
    void overridenDoc() {
        final String id = client.getComponentId("chain", "list");
        final String response = base
                .path("documentation/component/{id}")
                .resolveTemplate("id", id)
                .queryParam("language", "test")
                .request(APPLICATION_JSON_TYPE)
                .get(DocumentationContent.class)
                .getSource();
        assertEquals("== Input\n\nSome Input Overriden For Test locale\n\n=== Configuration\n\nblabla", response);
    }
}
