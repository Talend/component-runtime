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

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.function.Function;

import javax.inject.Inject;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.apache.meecrowave.junit5.MonoMeecrowaveConfig;
import org.junit.jupiter.api.RepeatedTest;
import org.talend.sdk.component.server.front.model.DocumentationContent;
import org.talend.sdk.component.server.front.model.ErrorDictionary;
import org.talend.sdk.component.server.front.model.error.ErrorPayload;
import org.talend.sdk.component.server.test.ComponentClient;
import org.talend.sdk.component.server.test.websocket.WebsocketClient;

@MonoMeecrowaveConfig
class DocumentationResourceImplTest {

    @Inject
    private WebTarget base;

    @Inject
    private ComponentClient client;

    @Inject
    private WebsocketClient ws;

    @RepeatedTest(2)
    void selectById() {
        final String foo = new DocumentationResourceImpl()
                .selectById("Foo1",
                        "== Foo0\n\n00000\n\n" + "=== Configuration\n\nWhatever0\n\n== Foo1\n\nThe description\n\n"
                                + "=== Configuration\n\nWhatever1\n\n"
                                + "== Foo2\n\n2222\n\n=== Configuration\n\nWhatever2",
                        DocumentationResourceImpl.DocumentationSegment.DESCRIPTION);
        assertEquals("The description", foo.trim());
    }

    @RepeatedTest(2)
    void selectByIdUsingComments() {
        final String content = "//component_start:my\n" + "\n" + "== my\n" + "\n" + "super my component\n" + "\n"
                + "//configuration_start\n" + "\n" + "=== Configuration\n" + "\n"
                + "[cols=\"d,d,m,a,e,d\",options=\"header\"]\n" + "|===\n"
                + "|Display Name|Description|Default Value|Enabled If|Configuration Path|Configuration Type\n"
                + "|configuration|configuration configuration|-|Always enabled|configuration|-\n"
                + "|input|the input value|-|Always enabled|configuration.input|-\n"
                + "|nested|it is nested|-|Always enabled|configuration.nested|dataset\n"
                + "|datastore|the datastore|-|Always enabled|configuration.nested.datastore|datastore\n"
                + "|user|the user to log in|unknown|Always enabled|configuration.nested.user|dataset\n" + "|===\n"
                + "\n" + "//configuration_end\n" + "\n" + "//component_end:my\n" + "\n" + "//component_start:my2\n"
                + "\n" + "== my2\n" + "\n" + "super my component2\n" + "\n" + "//configuration_start\n" + "\n"
                + "=== Configuration\n" + "\n" + "[cols=\"d,d,m,a,e,d\",options=\"header\"]\n" + "|===\n"
                + "|Display Name|Description|Default Value|Enabled If|Configuration Path|Configuration Type\n"
                + "|ds|ds configuration|-|Always enabled|ds|dataset\n"
                + "|datastore|the datastore|-|Always enabled|ds.datastore|datastore\n" + "|===\n" + "\n"
                + "//configuration_end\n" + "\n" + "//component_end:my2\n" + "\n" + "//component_start:my3\n" + "\n"
                + "== my2\n" + "\n" + "super my componentv3\n" + "\n" + "//configuration_start\n" + "\n"
                + "=== Configuration\n" + "\n" + "[cols=\"d,d,m,a,e,d\",options=\"header\"]\n" + "|===\n"
                + "|Display Name|Description|Default Value|Enabled If|Configuration Path|Configuration Type\n"
                + "|datastore|the datastore|-|Always enabled|ds.datastore|datastore\n" + "|===\n" + "\n"
                + "//configuration_end\n" + "\n" + "//component_end:my3\n";
        final DocumentationResourceImpl impl = new DocumentationResourceImpl();
        assertEquals("super my component",
                impl.selectById("my", content, DocumentationResourceImpl.DocumentationSegment.DESCRIPTION).trim());
        assertEquals("[cols=\"d,d,m,a,e,d\",options=\"header\"]\n" + "|===\n"
                + "|Display Name|Description|Default Value|Enabled If|Configuration Path|Configuration Type\n"
                + "|configuration|configuration configuration|-|Always enabled|configuration|-\n"
                + "|input|the input value|-|Always enabled|configuration.input|-\n"
                + "|nested|it is nested|-|Always enabled|configuration.nested|dataset\n"
                + "|datastore|the datastore|-|Always enabled|configuration.nested.datastore|datastore\n"
                + "|user|the user to log in|unknown|Always enabled|configuration.nested.user|dataset\n" + "|===",
                impl.selectById("my", content, DocumentationResourceImpl.DocumentationSegment.CONFIGURATION).trim());
        assertEquals(
                "== my\n" + "\n" + "super my component\n" + "\n" + "//configuration_start\n" + "\n"
                        + "=== Configuration\n" + "\n" + "[cols=\"d,d,m,a,e,d\",options=\"header\"]\n" + "|===\n"
                        + "|Display Name|Description|Default Value|Enabled If|Configuration Path|Configuration Type\n"
                        + "|configuration|configuration configuration|-|Always enabled|configuration|-\n"
                        + "|input|the input value|-|Always enabled|configuration.input|-\n"
                        + "|nested|it is nested|-|Always enabled|configuration.nested|dataset\n"
                        + "|datastore|the datastore|-|Always enabled|configuration.nested.datastore|datastore\n"
                        + "|user|the user to log in|unknown|Always enabled|configuration.nested.user|dataset\n"
                        + "|===\n" + "\n" + "//configuration_end",
                impl.selectById("my", content, DocumentationResourceImpl.DocumentationSegment.ALL).trim());
        assertEquals("super my component2",
                impl.selectById("my2", content, DocumentationResourceImpl.DocumentationSegment.DESCRIPTION).trim());
        assertEquals(
                "[cols=\"d,d,m,a,e,d\",options=\"header\"]\n" + "|===\n"
                        + "|Display Name|Description|Default Value|Enabled If|Configuration Path|Configuration Type\n"
                        + "|ds|ds configuration|-|Always enabled|ds|dataset\n"
                        + "|datastore|the datastore|-|Always enabled|ds.datastore|datastore\n" + "|===",
                impl.selectById("my2", content, DocumentationResourceImpl.DocumentationSegment.CONFIGURATION).trim());
        assertEquals(
                "== my2\n" + "\n" + "super my component2\n" + "\n" + "//configuration_start\n" + "\n"
                        + "=== Configuration\n" + "\n" + "[cols=\"d,d,m,a,e,d\",options=\"header\"]\n" + "|===\n"
                        + "|Display Name|Description|Default Value|Enabled If|Configuration Path|Configuration Type\n"
                        + "|ds|ds configuration|-|Always enabled|ds|dataset\n"
                        + "|datastore|the datastore|-|Always enabled|ds.datastore|datastore\n" + "|===\n" + "\n"
                        + "//configuration_end",
                impl.selectById("my2", content, DocumentationResourceImpl.DocumentationSegment.ALL).trim());
        assertEquals("super my componentv3",
                impl.selectById("my3", content, DocumentationResourceImpl.DocumentationSegment.DESCRIPTION).trim());
        assertEquals(
                "[cols=\"d,d,m,a,e,d\",options=\"header\"]\n" + "|===\n"
                        + "|Display Name|Description|Default Value|Enabled If|Configuration Path|Configuration Type\n"
                        + "|datastore|the datastore|-|Always enabled|ds.datastore|datastore\n" + "|===",
                impl.selectById("my3", content, DocumentationResourceImpl.DocumentationSegment.CONFIGURATION).trim());
        assertEquals(
                "== my2\n" + "\n" + "super my componentv3\n" + "\n" + "//configuration_start\n" + "\n"
                        + "=== Configuration\n" + "\n" + "[cols=\"d,d,m,a,e,d\",options=\"header\"]\n" + "|===\n"
                        + "|Display Name|Description|Default Value|Enabled If|Configuration Path|Configuration Type\n"
                        + "|datastore|the datastore|-|Always enabled|ds.datastore|datastore\n" + "|===\n" + "\n"
                        + "//configuration_end",
                impl.selectById("my3", content, DocumentationResourceImpl.DocumentationSegment.ALL).trim());
    }

    @RepeatedTest(2)
    void wsDoc() {
        final DocumentationContent content =
                ws.read(DocumentationContent.class, "GET", "/documentation/component/" + client.getJdbcId(), null);
        assertEquals("== input\n\ndesc\n\n=== Configuration\n\nSomething1", content.getSource());
    }

    @RepeatedTest(2)
    void getDoc() {
        final DocumentationContent content = base
                .path("documentation/component/{id}")
                .resolveTemplate("id", client.getJdbcId())
                .request(APPLICATION_JSON_TYPE)
                .get(DocumentationContent.class);
        assertEquals("asciidoc", content.getType());
        assertEquals("== input\n\ndesc\n\n=== Configuration\n\nSomething1", content.getSource());
    }

    @RepeatedTest(2)
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

    @RepeatedTest(2)
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

    @RepeatedTest(2)
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

    @RepeatedTest(2)
    void selectDocByName() {
        {
            final String id = client.getComponentId("jdbc", "input");
            final String response = base
                    .path("documentation/component/{id}")
                    .resolveTemplate("id", id)
                    .request(APPLICATION_JSON_TYPE)
                    .get(DocumentationContent.class)
                    .getSource();
            assertEquals("== input\n\ndesc\n\n=== Configuration\n\nSomething1", response);
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

    @RepeatedTest(2)
    void preferredLocaleDoc() {
        final String componentId = client.getComponentId("custom", "noop");
        final Function<String, String> get = lang -> base
                .path("documentation/component/{id}")
                .resolveTemplate("id", componentId)
                .queryParam("language", lang)
                .request(APPLICATION_JSON_TYPE)
                .get(DocumentationContent.class)
                .getSource()
                .trim();
        assertEquals("= Default", get.apply("en"));
        assertEquals("= Fr doc", get.apply("fr"));
    }

    @RepeatedTest(2)
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
