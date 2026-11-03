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

import org.talend.sdk.component.tcketl.client.ComponentServerClient;
import org.talend.sdk.component.tcketl.config.ApplicationConfig;
import org.talend.sdk.component.tcketl.domain.server.ActionInvoker;
import org.talend.sdk.component.tcketl.domain.server.ComponentCatalog;
import org.talend.sdk.component.tcketl.domain.server.ConfigurationMigrator;
import org.talend.sdk.component.tcketl.domain.server.DocumentationSource;
import org.talend.sdk.component.tcketl.domain.server.IconSource;
import org.talend.sdk.component.tcketl.domain.server.ServerEndpoint;

/** The connection backed by the HTTP client of {@code tcketl-component-client}. */
public final class ClientServerConnection implements ServerConnection {

    private final ServerEndpoint endpoint;

    private final ComponentServerClient client;

    private ClientServerConnection(final ServerEndpoint endpoint, final ComponentServerClient client) {
        this.endpoint = endpoint;
        this.client = client;
    }

    public static ClientServerConnection create(final ApplicationConfig config) {
        return new ClientServerConnection(config.server(),
                ComponentServerClient.create(config.server(), config.timeouts(), config.language()));
    }

    @Override
    public ServerEndpoint endpoint() {
        return endpoint;
    }

    @Override
    public ComponentCatalog catalog() {
        return client.catalog();
    }

    @Override
    public IconSource icons() {
        return client.icons();
    }

    @Override
    public DocumentationSource documentation() {
        return client.documentation();
    }

    @Override
    public ConfigurationMigrator migrator() {
        return client.migrator();
    }

    @Override
    public ActionInvoker actions() {
        return client.actions();
    }

    @Override
    public void invalidate() {
        client.invalidate();
    }
}
