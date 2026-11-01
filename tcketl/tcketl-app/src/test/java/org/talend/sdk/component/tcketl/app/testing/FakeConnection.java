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
package org.talend.sdk.component.tcketl.app.testing;

import java.util.Optional;

import org.talend.sdk.component.tcketl.app.session.ServerConnection;
import org.talend.sdk.component.tcketl.domain.connector.ConnectorId;
import org.talend.sdk.component.tcketl.domain.server.ComponentCatalog;
import org.talend.sdk.component.tcketl.domain.server.ConfigurationMigrator;
import org.talend.sdk.component.tcketl.domain.server.DocumentationSource;
import org.talend.sdk.component.tcketl.domain.server.IconData;
import org.talend.sdk.component.tcketl.domain.server.IconSource;
import org.talend.sdk.component.tcketl.domain.server.IconTheme;
import org.talend.sdk.component.tcketl.domain.server.ServerEndpoint;

/** A connection to a {@link FakeCatalog} that counts how often what it kept was dropped. */
public final class FakeConnection implements ServerConnection {

    private static final IconSource NO_ICONS = new IconSource() {

        @Override
        public Optional<IconData> componentIcon(final ConnectorId id, final IconTheme theme) {
            return Optional.empty();
        }

        @Override
        public Optional<IconData> familyIcon(final String familyId, final IconTheme theme) {
            return Optional.empty();
        }
    };

    private final ServerEndpoint endpoint;

    private final FakeCatalog catalog;

    private int invalidations;

    private ConfigurationMigrator migrator = (id, from, configuration) -> configuration;

    /** The migration the connection offers; by default the configuration comes back unchanged. */
    public FakeConnection withMigrator(final ConfigurationMigrator newMigrator) {
        this.migrator = newMigrator;
        return this;
    }

    @Override
    public ConfigurationMigrator migrator() {
        return migrator;
    }

    public FakeConnection(final ServerEndpoint endpoint, final FakeCatalog catalog) {
        this.endpoint = endpoint;
        this.catalog = catalog;
    }

    /** A connection to {@code http://localhost:<port>/api/v1}. */
    public static FakeConnection onPort(final int port, final FakeCatalog catalog) {
        return new FakeConnection(new ServerEndpoint("http", "localhost", port, "/api/v1"), catalog);
    }

    public int invalidations() {
        return invalidations;
    }

    @Override
    public ServerEndpoint endpoint() {
        return endpoint;
    }

    @Override
    public ComponentCatalog catalog() {
        return catalog;
    }

    @Override
    public IconSource icons() {
        return NO_ICONS;
    }

    @Override
    public DocumentationSource documentation() {
        return id -> Optional.empty();
    }

    @Override
    public void invalidate() {
        invalidations++;
    }
}
