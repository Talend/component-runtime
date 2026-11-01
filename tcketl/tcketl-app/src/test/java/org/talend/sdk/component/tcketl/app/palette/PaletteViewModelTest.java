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
package org.talend.sdk.component.tcketl.app.palette;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.tcketl.app.session.ServerSession;
import org.talend.sdk.component.tcketl.app.testing.Connectors;
import org.talend.sdk.component.tcketl.app.testing.FakeCatalog;
import org.talend.sdk.component.tcketl.app.testing.FakeConnection;
import org.talend.sdk.component.tcketl.app.testing.TestAsync;
import org.talend.sdk.component.tcketl.domain.connector.ConnectorRole;
import org.talend.sdk.component.tcketl.domain.connector.ConnectorSummary;

class PaletteViewModelTest {

    private final TestAsync async = new TestAsync();

    private final ServerSession session = new ServerSession(async.runner());

    private final PaletteViewModel model = new PaletteViewModel(session, async.runner());

    private final ConnectorSummary reader =
            Connectors.summary("files", "Local files", "CSV Reader", List.of(), ConnectorRole.INPUT);

    // the index reports a sink as a processor, only its flows tell
    private final ConnectorSummary writer =
            Connectors.summary("files", "Local files", "CSV Writer", List.of(), ConnectorRole.PROCESSOR);

    private final ConnectorSummary sql =
            Connectors.summary("db", "Databases", "SQL Reader", List.of(), ConnectorRole.INPUT);

    private final FakeCatalog catalog = new FakeCatalog()
            .withConnectors(List.of(reader, writer, sql))
            .define(Connectors.asInput(reader))
            .define(Connectors.asOutput(writer))
            .define(Connectors.asInput(sql));

    @Test
    void explainsWhyThereIsNothingToShow() {
        assertEquals("Not connected", model.placeholderText().get());

        session.connect(FakeConnection.onPort(8080, catalog));
        assertEquals("Loading components\u2026", model.placeholderText().get());
        async.drain();
        assertEquals("", model.placeholderText().get());

        session.connect(FakeConnection.onPort(8081, new FakeCatalog()));
        async.drain();
        assertEquals("This server has no components", model.placeholderText().get());

        session.connect(FakeConnection.onPort(8082, new FakeCatalog().unreachable("down")));
        async.drain();
        assertEquals("Components unavailable", model.placeholderText().get());
    }

    @Test
    void theRoleIsTheOneOfTheIndexUntilTheDefinitionIsLoaded() {
        session.connect(FakeConnection.onPort(8080, catalog));
        async.io().runNext();
        async.ui().runNext();
        assertEquals(ConnectorRole.PROCESSOR, model.roleOf(writer));
        assertFalse(model.isResolved(writer));

        async.drain();

        assertEquals(ConnectorRole.OUTPUT, model.roleOf(writer));
        assertEquals(ConnectorRole.INPUT, model.roleOf(reader));
        assertTrue(model.isResolved(writer));
    }

    @Test
    void searchKeepsTheMatchingConnectorsAndTellsWhenThereAreNone() {
        session.connect(FakeConnection.onPort(8080, catalog));
        async.drain();
        assertEquals("3 connectors", model.countText().get());

        model.query().set("csv");
        assertEquals(2, model.visible().get().size());
        assertEquals("2 of 3", model.countText().get());

        model.query().set(" zzz ");
        assertEquals("No component matches \u201czzz\u201d", model.placeholderText().get());
    }

    @Test
    void knownRolesSurviveARefreshButNotAChangeOfServer() {
        session.connect(FakeConnection.onPort(8080, catalog));
        async.drain();

        session.refresh();
        async.io().runNext();
        async.ui().runNext();
        assertTrue(model.isResolved(writer));

        final FakeCatalog other = new FakeCatalog().withConnectors(List.of(sql)).define(Connectors.asInput(sql));
        session.connect(FakeConnection.onPort(8081, other));
        assertEquals(Set.of(), model.roles().get().keySet());
        async.drain();
        assertEquals(Set.of(sql.id()), model.roles().get().keySet());
    }
}
