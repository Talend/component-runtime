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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.tcketl.domain.connector.ConnectorDefinition;
import org.talend.sdk.component.tcketl.domain.connector.ConnectorId;
import org.talend.sdk.component.tcketl.domain.connector.ConnectorRole;
import org.talend.sdk.component.tcketl.domain.connector.ConnectorSummary;
import org.talend.sdk.component.tcketl.domain.server.ConnectorDocumentation;
import org.talend.sdk.component.tcketl.domain.server.IconData;
import org.talend.sdk.component.tcketl.domain.server.IconTheme;
import org.talend.sdk.component.tcketl.domain.server.ServerException;
import org.talend.sdk.component.tcketl.domain.server.ServerProtocolException;
import org.talend.sdk.component.tcketl.domain.server.ServerStatus;
import org.talend.sdk.component.tcketl.domain.server.ServerTimeouts;

class HttpComponentCatalogTest {

    private static final String INPUT_ID = "Y29uZmlndXJhdGlvbkZvcm1TYW1wbGUjc2FtcGxlRm9ybSNJbnB1dA";

    private static final String INPUT_FAMILY_ID = "Y29uZmlndXJhdGlvbkZvcm1TYW1wbGUjc2FtcGxlRm9ybQ";

    private static final String DETAILS_FIXTURE = "sample-component-details-sample-form-input.json";

    private static HttpComponentCatalog catalog(final StubServer stub, final String language) {
        return new HttpComponentCatalog(new HttpTransport(stub.endpoint(), ServerTimeouts.defaults()), language);
    }

    @Test
    void statusCombinesWhatTheServerReportsAboutItsComponents() throws IOException {
        try (StubServer stub = StubServer.start()) {
            stub.on("/environment", StubServer.Reply.fixture("sample-environment.json"));

            final ServerStatus status = catalog(stub, "en").status();

            assertEquals("1.91.0", status.version());
            assertEquals("8d69f1361af1c412d5e5c9984668e94dbc74d250", status.commit());
            assertEquals("2026-07-17T21:37:06.859Z[UTC]|"
                    + "cdc10526e43f4fc208f0ad4252290c724c070ffdfbf2d8a81e24a331e6bca003", status.stamp());
        }
    }

    @Test
    void theIndexBecomesTheListOfConnectors() throws IOException {
        try (StubServer stub = StubServer.start()) {
            stub.on("/component/index", StubServer.Reply.fixture("sample-component-index.json"));

            final List<ConnectorSummary> connectors = catalog(stub, "fr").connectors();

            assertEquals(List.of("Client", "Input", "AdlsGen2Output"),
                    connectors.stream().map(ConnectorSummary::name).toList());
            assertEquals("language=fr&includeIconContent=false", stub.requests().get(0).uri().getRawQuery());
        }
    }

    @Test
    void anIndexEntryThatCannotBeIdentifiedIsSkipped() throws IOException {
        try (StubServer stub = StubServer.start()) {
            stub
                    .on("/component/index",
                            StubServer.Reply
                                    .json(200, "{\"components\":[{\"displayName\":\"Nameless\",\"type\":\"input\"},"
                                            + "{\"id\":{\"family\":\"f\",\"familyId\":\"Zg\",\"id\":\"Zi1u\","
                                            + "\"name\":\"n\",\"plugin\":\"p\"},\"type\":\"processor\",\"version\":2}]}"));

            final List<ConnectorSummary> connectors = catalog(stub, "en").connectors();

            assertEquals(1, connectors.size());
            assertEquals("n", connectors.get(0).name());
            assertEquals(2, connectors.get(0).ref().version());
        }
    }

    @Test
    void definitionsAreRequestedOnceByIdentifier() throws IOException {
        try (StubServer stub = StubServer.start()) {
            stub.on("/component/details", StubServer.Reply.fixture(DETAILS_FIXTURE));
            final ConnectorId id = new ConnectorId(INPUT_ID);

            final Map<ConnectorId, ConnectorDefinition> definitions = catalog(stub, "en").connectors(List.of(id, id));

            assertEquals(List.of(id), List.copyOf(definitions.keySet()));
            assertEquals(ConnectorRole.INPUT, definitions.get(id).role());
            assertEquals(1, stub.requests().size());
            assertEquals(List.of(INPUT_ID), stub.requests().get(0).parameters("identifiers"));
            assertEquals(List.of("en"), stub.requests().get(0).parameters("language"));
        }
    }

    @Test
    void identifiersAreRequestedByBatchesOfTen() throws IOException {
        try (StubServer stub = StubServer.start()) {
            stub.on("/component/details", StubServer.Reply.json(200, "{\"details\":[]}"));
            final List<ConnectorId> ids =
                    IntStream.range(0, 25).mapToObj(index -> new ConnectorId("id" + index)).toList();

            assertTrue(catalog(stub, "en").connectors(ids).isEmpty());

            assertEquals(List.of(10, 10, 5),
                    stub.requests().stream().map(request -> request.parameters("identifiers").size()).toList());
        }
    }

    @Test
    void rejectedIdentifiersAreDroppedAndTheOthersAskedAgain() throws IOException {
        try (StubServer stub = StubServer.start()) {
            stub.on("/component/details", request -> {
                final List<String> asked = request.parameters("identifiers");
                if (asked.contains("bad1") || asked.contains("bad2")) {
                    return StubServer.Reply
                            .json(400, "{\"bad1\":{\"code\":\"COMPONENT_MISSING\",\"description\":\"unknown\"},"
                                    + "\"bad2\":{\"code\":\"PLUGIN_MISSING\",\"description\":\"gone\"}}");
                }
                return StubServer.Reply.fixture(DETAILS_FIXTURE);
            });

            final Map<ConnectorId, ConnectorDefinition> definitions = catalog(stub, "en")
                    .connectors(List.of(new ConnectorId("bad1"), new ConnectorId(INPUT_ID), new ConnectorId("bad2")));

            assertEquals(Set.of(new ConnectorId(INPUT_ID)), definitions.keySet());
            assertEquals(List.of(List.of("bad1", INPUT_ID, "bad2"), List.of(INPUT_ID)),
                    stub.requests().stream().map(request -> request.parameters("identifiers")).toList());
        }
    }

    @Test
    void aBatchWhoseIdentifiersAreAllRejectedGivesNothing() throws IOException {
        try (StubServer stub = StubServer.start()) {
            stub
                    .on("/component/details",
                            StubServer.Reply
                                    .json(400,
                                            "{\"bad\":{\"code\":\"COMPONENT_MISSING\",\"description\":\"unknown\"}}"));

            assertTrue(catalog(stub, "en").connectors(List.of(new ConnectorId("bad"))).isEmpty());
            assertEquals(1, stub.requests().size());
        }
    }

    @Test
    void aRejectionThatNamesNoIdentifierIsRaisedWithoutRetry() throws IOException {
        try (StubServer stub = StubServer.start()) {
            stub
                    .on("/component/details",
                            StubServer.Reply.json(400, "{\"code\":\"BAD_FORMAT\",\"description\":\"No identifier\"}"));

            final ServerException error = assertThrows(ServerException.class,
                    () -> catalog(stub, "en").connectors(List.of(new ConnectorId("x"))));

            assertEquals(400, error.status());
            assertEquals("BAD_FORMAT", error.code());
            assertEquals(1, stub.requests().size());
        }
    }

    @Test
    void aServerErrorIsRaised() throws IOException {
        try (StubServer stub = StubServer.start()) {
            stub.on("/component/index",
                    StubServer.Reply.json(500, "{\"code\":\"UNEXPECTED\",\"description\":\"boom\"}"));

            final ServerException error = assertThrows(ServerException.class, () -> catalog(stub, "en").connectors());

            assertEquals(500, error.status());
            assertEquals("boom", error.description());
        }
    }

    @Test
    void anUnreadablePayloadIsAProtocolError() throws IOException {
        try (StubServer stub = StubServer.start()) {
            stub.on("/component/index", StubServer.Reply.json(200, "not json"));

            final ServerProtocolException error =
                    assertThrows(ServerProtocolException.class, () -> catalog(stub, "en").connectors());

            assertEquals(200, error.status());
            assertTrue(error.description().startsWith("Unexpected ComponentIndexes payload"), error.description());
        }
    }

    @Test
    void detailsOfAConnectorThatCannotBeIdentifiedAreAProtocolError() throws IOException {
        try (StubServer stub = StubServer.start()) {
            stub.on("/component/details", StubServer.Reply.json(200, "{\"details\":[{\"displayName\":\"x\"}]}"));

            final ServerProtocolException error = assertThrows(ServerProtocolException.class,
                    () -> catalog(stub, "en").connectors(List.of(new ConnectorId("x"))));

            assertTrue(error.description().startsWith("Malformed component details"), error.description());
        }
    }

    @Test
    void detailsOfConnectorsThatWereNotAskedForAreIgnored() throws IOException {
        try (StubServer stub = StubServer.start()) {
            stub.on("/component/details", StubServer.Reply.fixture(DETAILS_FIXTURE));

            assertTrue(catalog(stub, "en").connectors(List.of(new ConnectorId("another"))).isEmpty());
        }
    }

    @Test
    void anIconIsRequestedForTheThemeAndKeepsItsMediaType() throws IOException {
        try (StubServer stub = StubServer.start()) {
            final byte[] svg = "<svg/>".getBytes(StandardCharsets.UTF_8);
            stub
                    .on("/component/icon/" + INPUT_ID, StubServer.Reply.bytes(200, "image/svg+xml;charset=UTF-8", svg))
                    .on("/component/icon/family/" + INPUT_FAMILY_ID,
                            StubServer.Reply.bytes(200, "image/png", new byte[] { 1, 2, 3 }));
            final HttpComponentCatalog catalog = catalog(stub, "en");

            final IconData component = catalog.componentIcon(new ConnectorId(INPUT_ID), IconTheme.DARK).orElseThrow();
            final IconData family = catalog.familyIcon(INPUT_FAMILY_ID, IconTheme.LIGHT).orElseThrow();

            assertEquals("image/svg+xml", component.mediaType());
            assertArrayEquals(svg, component.content());
            assertEquals("image/png", family.mediaType());
            assertArrayEquals(new byte[] { 1, 2, 3 }, family.content());
            assertEquals(List.of("dark"), stub.requests().get(0).parameters("theme"));
            assertEquals(List.of("light"), stub.requests().get(1).parameters("theme"));
            assertEquals("application/octet-stream", stub.requests().get(0).header("Accept"));
        }
    }

    @Test
    void aMissingIconIsEmptyButAFailingServerIsNot() throws IOException {
        try (StubServer stub = StubServer.start()) {
            stub
                    .on("/component/icon/missing",
                            StubServer.Reply.json(404, "{\"code\":\"ICON_MISSING\",\"description\":\"no icon\"}"))
                    .on("/component/icon/broken",
                            StubServer.Reply.json(500, "{\"code\":\"UNEXPECTED\",\"description\":\"boom\"}"));
            final HttpComponentCatalog catalog = catalog(stub, "en");

            assertTrue(catalog.componentIcon(new ConnectorId("missing"), IconTheme.DARK).isEmpty());
            assertThrows(ServerException.class, () -> catalog.componentIcon(new ConnectorId("broken"), IconTheme.DARK));
        }
    }

    @Test
    void documentationIsReadForTheLanguage() throws IOException {
        try (StubServer stub = StubServer.start()) {
            stub
                    .on("/documentation/component/" + INPUT_ID,
                            StubServer.Reply.fixture("sample-component-documentation-cyberark-input.json"))
                    .on("/documentation/component/missing",
                            StubServer.Reply.json(404, "{\"code\":\"COMPONENT_MISSING\",\"description\":\"no\"}"));
            final HttpComponentCatalog catalog = catalog(stub, "de");

            final ConnectorDocumentation documentation = catalog.documentation(new ConnectorId(INPUT_ID)).orElseThrow();

            assertEquals("asciidoc", documentation.format());
            assertTrue(documentation.text().startsWith("== Input"));
            assertEquals(List.of("de"), stub.requests().get(0).parameters("language"));
            assertTrue(catalog.documentation(new ConnectorId("missing")).isEmpty());
        }
    }
}
