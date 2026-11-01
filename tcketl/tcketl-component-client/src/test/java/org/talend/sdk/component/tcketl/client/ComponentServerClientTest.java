/**
 * Copyright (C) 2006-2026 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.tcketl.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.tcketl.domain.connector.ConnectorId;
import org.talend.sdk.component.tcketl.domain.server.IconTheme;
import org.talend.sdk.component.tcketl.domain.server.ServerTimeouts;

class ComponentServerClientTest {

    private static long count(final StubServer stub, final String path) {
        return stub.requests().stream().filter(request -> request.uri().getPath().endsWith(path)).count();
    }

    private static StubServer.Request first(final StubServer stub, final String path) {
        return stub
                .requests()
                .stream()
                .filter(request -> request.uri().getPath().endsWith(path))
                .findFirst()
                .orElseThrow();
    }

    @Test
    void aBlankLanguageFallsBackToEnglish() throws IOException {
        try (StubServer stub = StubServer.start()) {
            stub
                    .on("/environment", StubServer.Reply.fixture("sample-environment.json"))
                    .on("/component/index", StubServer.Reply.fixture("sample-component-index.json"));
            final ComponentServerClient client =
                    ComponentServerClient.create(stub.endpoint(), ServerTimeouts.defaults(), " ");

            assertEquals(3, client.catalog().connectors().size());
            assertEquals(List.of("en"), first(stub, "/component/index").parameters("language"));
        }
    }

    @Test
    void theCatalogIsKeptUntilItIsInvalidated() throws IOException {
        try (StubServer stub = StubServer.start()) {
            stub
                    .on("/environment", StubServer.Reply.fixture("sample-environment.json"))
                    .on("/component/index", StubServer.Reply.fixture("sample-component-index.json"));
            final ComponentServerClient client =
                    ComponentServerClient.create(stub.endpoint(), ServerTimeouts.defaults(), "en");

            client.catalog().connectors();
            client.catalog().connectors();
            assertEquals(1, count(stub, "/component/index"));

            client.invalidate();
            client.catalog().connectors();
            assertEquals(2, count(stub, "/component/index"));
        }
    }

    @Test
    void aNewEnvironmentStampMakesTheCatalogReloadOnItsOwn() throws IOException {
        final AtomicReference<String> lastUpdated = new AtomicReference<>("2026-01-01T00:00:00Z[UTC]");
        try (StubServer stub = StubServer.start()) {
            stub
                    .on("/environment",
                            request -> StubServer.Reply
                                    .json(200,
                                            "{\"version\":\"1.91.0\",\"lastUpdated\":\"" + lastUpdated.get() + "\"}"))
                    .on("/component/index", StubServer.Reply.fixture("sample-component-index.json"));
            final ComponentServerClient client =
                    ComponentServerClient.create(stub.endpoint(), ServerTimeouts.defaults(), "en");
            client.catalog().connectors();

            client.catalog().status();
            client.catalog().connectors();
            assertEquals(1, count(stub, "/component/index"));

            lastUpdated.set("2026-02-02T00:00:00Z[UTC]");
            assertEquals("2026-02-02T00:00:00Z[UTC]|", client.catalog().status().stamp());
            client.catalog().connectors();
            assertEquals(2, count(stub, "/component/index"));
        }
    }

    @Test
    void documentationAndIconsAreServedByTheSameServer() throws IOException {
        try (StubServer stub = StubServer.start()) {
            stub
                    .on("/documentation/component/some-id",
                            StubServer.Reply.json(200, "{\"source\":\"== Doc\",\"type\":\"asciidoc\"}"))
                    .on("/component/icon/some-id", StubServer.Reply.bytes(200, "image/png", new byte[] { 1 }));
            final ComponentServerClient client =
                    ComponentServerClient.create(stub.endpoint(), ServerTimeouts.defaults(), "fr");
            final ConnectorId id = new ConnectorId("some-id");

            assertEquals("== Doc", client.documentation().documentation(id).orElseThrow().text());
            assertEquals("image/png", client.icons().componentIcon(id, IconTheme.DARK).orElseThrow().mediaType());

            assertEquals(List.of("fr"), first(stub, "/documentation/component/some-id").parameters("language"));
        }
    }
}
