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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.tcketl.app.testing.FakeCatalog;
import org.talend.sdk.component.tcketl.app.testing.FakeConnection;
import org.talend.sdk.component.tcketl.app.testing.TestAsync;
import org.talend.sdk.component.tcketl.domain.server.ServerStatus;

/** Polling the server for a change of its stamp. */
class EnvironmentWatcherTest {

    private final TestAsync async = new TestAsync();

    private final ServerSession session = new ServerSession(async.runner());

    private final EnvironmentWatcher watcher = new EnvironmentWatcher(session, async.runner());

    private final FakeCatalog catalog = new FakeCatalog().withConnectors(List.of());

    @Test
    void aNewStampRaisesTheFlagAndAReloadClearsIt() {
        session.connect(FakeConnection.onPort(9001, catalog));
        async.drain();

        watcher.poll();
        async.drain();
        assertFalse(watcher.changed().get());

        catalog.withStatus(new ServerStatus("1.0.0", "abc123", "other"));
        watcher.poll();
        async.drain();
        assertTrue(watcher.changed().get());

        session.refresh();
        async.drain();
        assertFalse(watcher.changed().get());
    }

    @Test
    void anUnreachableServerIsNotAChange() {
        session.connect(FakeConnection.onPort(9001, catalog));
        async.drain();

        catalog.unreachable("down");
        watcher.poll();
        async.drain();

        assertFalse(watcher.changed().get());
    }
}
