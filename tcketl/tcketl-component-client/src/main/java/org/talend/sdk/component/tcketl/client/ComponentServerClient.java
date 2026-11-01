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

import org.talend.sdk.component.tcketl.domain.Preconditions;
import org.talend.sdk.component.tcketl.domain.server.ActionInvoker;
import org.talend.sdk.component.tcketl.domain.server.ComponentCatalog;
import org.talend.sdk.component.tcketl.domain.server.ConfigurationMigrator;
import org.talend.sdk.component.tcketl.domain.server.DocumentationSource;
import org.talend.sdk.component.tcketl.domain.server.IconSource;
import org.talend.sdk.component.tcketl.domain.server.ServerEndpoint;
import org.talend.sdk.component.tcketl.domain.server.ServerTimeouts;

/**
 * Entry point of the module and the only public type: it hands out the domain ports backed by one component server.
 * Every call is blocking and MUST NOT be made from the JavaFX application thread. The endpoint, the timeouts and the
 * language are fixed for the life of the instance: create a new client when the settings change.
 */
public final class ComponentServerClient {

    /** The language the server falls back to for anything it does not translate. */
    public static final String DEFAULT_LANGUAGE = "en";

    private final HttpComponentCatalog server;

    private final HttpActionInvoker actions;

    private final HttpConfigurationMigrator migrator;

    private final CachingComponentCatalog catalog;

    private ComponentServerClient(final HttpComponentCatalog server, final HttpActionInvoker actions,
            final HttpConfigurationMigrator migrator) {
        this.server = server;
        this.actions = actions;
        this.migrator = migrator;
        this.catalog = new CachingComponentCatalog(server);
    }

    public static ComponentServerClient create(final ServerEndpoint endpoint, final ServerTimeouts timeouts,
            final String language) {
        final HttpTransport transport = new HttpTransport(endpoint, timeouts);
        final String lang = Preconditions.orDefault(language, DEFAULT_LANGUAGE);
        return new ComponentServerClient(new HttpComponentCatalog(transport, lang),
                new HttpActionInvoker(transport, lang), new HttpConfigurationMigrator(transport));
    }

    /** Brings a configuration saved with an older version of a component to its current version. */
    public ConfigurationMigrator migrator() {
        return migrator;
    }

    /** Runs the services of the components (health check, suggestions, update, schema...). */
    public ActionInvoker actions() {
        return actions;
    }

    /**
     * The index and the definitions are kept until the server reports another stamp or {@link #invalidate()} is
     * called; {@link ComponentCatalog#status()} is the cheap call to poll for that.
     */
    public ComponentCatalog catalog() {
        return catalog;
    }

    public IconSource icons() {
        return server;
    }

    public DocumentationSource documentation() {
        return server;
    }

    /** Drops what the catalog kept, the next call goes to the server. */
    public void invalidate() {
        catalog.invalidate();
    }
}
