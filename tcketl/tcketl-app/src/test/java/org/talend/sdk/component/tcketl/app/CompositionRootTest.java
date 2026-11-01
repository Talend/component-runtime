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
package org.talend.sdk.component.tcketl.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.tcketl.app.async.AsyncRunner;
import org.talend.sdk.component.tcketl.app.session.ServerConnection;
import org.talend.sdk.component.tcketl.app.session.ServerSession.State;
import org.talend.sdk.component.tcketl.app.testing.Connectors;
import org.talend.sdk.component.tcketl.app.testing.FakeCatalog;
import org.talend.sdk.component.tcketl.app.testing.FakeConnection;
import org.talend.sdk.component.tcketl.config.ApplicationConfig;
import org.talend.sdk.component.tcketl.config.ConfigLoadResult;
import org.talend.sdk.component.tcketl.config.ConfigStore;
import org.talend.sdk.component.tcketl.domain.connector.ConnectorRole;
import org.talend.sdk.component.tcketl.domain.server.ServerEndpoint;
import org.talend.sdk.component.tcketl.domain.server.ServerStatus;

class CompositionRootTest {

    private final ApplicationConfig initial = onPort(9001);

    private final RecordingStore store = new RecordingStore();

    private final List<ApplicationConfig> connectedTo = new ArrayList<>();

    private CompositionRoot root = rootFor(ConfigLoadResult.loaded(initial));

    @Test
    void startsOnTheServerOfTheLoadedSettings() {
        root.start();

        assertEquals(List.of(initial), connectedTo);
        assertEquals(State.CONNECTED, root.session().state().get());
        assertEquals(1, root.palette().visible().get().size());
    }

    @Test
    void applyingSettingsSavesThemAndSwitchesToTheirServer() {
        root.start();
        final ApplicationConfig other = onPort(9002);

        root.apply(other);

        assertEquals(List.of(other), store.saved);
        assertEquals(other, root.config().get());
        assertEquals(List.of(initial, other), connectedTo);
        assertEquals(9002, root.session().connection().get().endpoint().port());
        assertEquals("", root.notice().get());
    }

    @Test
    void settingsThatCannotBeSavedAreStillUsedAndTheUserIsTold() {
        store.failure = new IOException("disk full");
        final ApplicationConfig other = onPort(9002);

        root.apply(other);

        assertEquals("The settings could not be saved to " + store.file() + ": disk full", root.notice().get());
        assertEquals(other, root.config().get());
        assertEquals(State.CONNECTED, root.session().state().get());

        store.failure = null;
        root.apply(other);
        assertEquals("", root.notice().get());
    }

    @Test
    void aSettingsFileThatCouldNotBeReadIsReportedUntilTheSettingsAreSaved() {
        root = rootFor(ConfigLoadResult.fallback(initial, "config.json is not valid: unexpected end"));
        assertEquals("config.json is not valid: unexpected end", root.notice().get());

        root.apply(initial);

        assertEquals("", root.notice().get());
    }

    @Test
    void testingSettingsAsksTheServerTheyDescribeWithoutSwitchingToIt() {
        final ServerStatus status = root.tester().test(onPort(9005));

        assertEquals("1.0.9005", status.version());
        assertEquals(List.of(onPort(9005)), connectedTo);
        assertNull(root.session().connection().get());
    }

    private CompositionRoot rootFor(final ConfigLoadResult loaded) {
        return new CompositionRoot(store, loaded, AsyncRunner.direct(), this::connect);
    }

    private ServerConnection connect(final ApplicationConfig settings) {
        connectedTo.add(settings);
        final int port = settings.server().port();
        final FakeCatalog catalog = new FakeCatalog()
                .withStatus(new ServerStatus("1.0." + port, "commit", "stamp"))
                .withConnectors(List.of(Connectors.summary("files", "CSV Reader", ConnectorRole.INPUT)));
        return FakeConnection.onPort(port, catalog);
    }

    private static ApplicationConfig onPort(final int port) {
        return ApplicationConfig.defaults().withServer(new ServerEndpoint("http", "localhost", port, "/api/v1"));
    }

    private static final class RecordingStore implements ConfigStore {

        private final List<ApplicationConfig> saved = new ArrayList<>();

        private IOException failure;

        @Override
        public Path file() {
            return Path.of("settings", "config.json");
        }

        @Override
        public ConfigLoadResult load() {
            throw new UnsupportedOperationException("the loaded settings are given to the root");
        }

        @Override
        public void save(final ApplicationConfig config) throws IOException {
            if (failure != null) {
                throw failure;
            }
            saved.add(config);
        }
    }
}
