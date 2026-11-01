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
package org.talend.sdk.component.tcketl.app.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.tcketl.app.session.ServerSession.State;
import org.talend.sdk.component.tcketl.app.testing.Connectors;
import org.talend.sdk.component.tcketl.app.testing.FakeCatalog;
import org.talend.sdk.component.tcketl.app.testing.FakeConnection;
import org.talend.sdk.component.tcketl.app.testing.TestAsync;
import org.talend.sdk.component.tcketl.domain.connector.ConnectorRole;
import org.talend.sdk.component.tcketl.domain.connector.ConnectorSummary;
import org.talend.sdk.component.tcketl.domain.server.ServerStatus;

class ServerSessionTest {

    private static final String URL = "http://localhost:8080/api/v1";

    private final TestAsync async = new TestAsync();

    private final ServerSession session = new ServerSession(async.runner());

    private final ConnectorSummary input = Connectors.summary("family", "Input", ConnectorRole.INPUT);

    private final ConnectorSummary output = Connectors.summary("family", "Output", ConnectorRole.OUTPUT);

    private final FakeCatalog catalog = new FakeCatalog()
            .withStatus(new ServerStatus("1.2611.0", "abc", "stamp-1"))
            .withConnectors(List.of(input, output));

    private final FakeConnection connection = FakeConnection.onPort(8080, catalog);

    @Test
    void startsDisconnected() {
        assertEquals(State.DISCONNECTED, session.state().get());
        assertEquals("Not connected", session.description().get());
    }

    @Test
    void connectingLoadsTheStatusAndTheConnectors() {
        session.connect(connection);
        assertEquals(State.CONNECTING, session.state().get());
        assertEquals("Connecting to " + URL + "\u2026", session.description().get());
        assertEquals(List.of(), session.connectors().get());

        async.drain();

        assertEquals(State.CONNECTED, session.state().get());
        assertEquals(List.of(input, output), session.connectors().get());
        assertEquals("1.2611.0", session.serverStatus().get().version());
        assertEquals("Connected to " + URL + ", server 1.2611.0", session.description().get());
        assertEquals("", session.problem().get());
    }

    @Test
    void aFailureIsExplainedAndKeepsWhatWasLoadedBefore() {
        session.connect(connection);
        async.drain();

        catalog.unreachable("Cannot reach " + URL);
        session.reload();
        async.drain();

        assertEquals(State.FAILED, session.state().get());
        assertEquals("Cannot reach " + URL, session.problem().get());
        assertEquals("Not connected to " + URL, session.description().get());
        assertNull(session.serverStatus().get());
        assertEquals(List.of(input, output), session.connectors().get());

        catalog.working();
        session.reload();
        async.drain();

        assertEquals(State.CONNECTED, session.state().get());
        assertEquals("", session.problem().get());
    }

    @Test
    void refreshDropsWhatTheConnectionKeptBeforeAskingAgain() {
        session.connect(connection);
        async.drain();

        session.refresh();
        assertEquals(1, connection.invalidations());
        async.drain();

        assertEquals(2, catalog.listCalls());
    }

    @Test
    void reloadingTheSameListStillTellsThatItWasLoaded() {
        session.connect(connection);
        async.drain();
        final List<Long> revisions = new ArrayList<>();
        session.revision().addListener((observed, before, after) -> revisions.add(after.longValue()));

        session.reload();
        async.drain();

        assertEquals(1, revisions.size());
    }

    @Test
    void anotherServerReplacesTheListAndTheAnswerOfTheFirstOneIsIgnored() {
        final FakeCatalog other = new FakeCatalog()
                .withStatus(new ServerStatus("2.0.0", "def", "stamp-2"))
                .withConnectors(List.of(output));
        session.connect(connection);
        async.io().runNext();
        assertEquals(1, async.ui().pending());

        session.connect(FakeConnection.onPort(9090, other));
        assertEquals(List.of(), session.connectors().get());
        async.drain();

        assertEquals(List.of(output), session.connectors().get());
        assertEquals("2.0.0", session.serverStatus().get().version());
        assertEquals("Connected to http://localhost:9090/api/v1, server 2.0.0", session.description().get());
    }

    @Test
    void closingAbandonsTheLoadInFlight() {
        session.connect(connection);
        session.close();
        async.drain();

        assertEquals(State.CONNECTING, session.state().get());
        assertTrue(session.connectors().get().isEmpty());
    }
}
