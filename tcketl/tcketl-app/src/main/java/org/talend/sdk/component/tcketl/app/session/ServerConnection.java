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

import org.talend.sdk.component.tcketl.domain.server.ActionInvoker;
import org.talend.sdk.component.tcketl.domain.server.ComponentCatalog;
import org.talend.sdk.component.tcketl.domain.server.ConfigurationMigrator;
import org.talend.sdk.component.tcketl.domain.server.DocumentationSource;
import org.talend.sdk.component.tcketl.domain.server.IconSource;
import org.talend.sdk.component.tcketl.domain.server.ServerEndpoint;

/**
 * What the application uses of one configured component server. Everything but {@link #invalidate()} blocks and MUST
 * NOT be called on the UI thread.
 */
public interface ServerConnection {

    ServerEndpoint endpoint();

    ComponentCatalog catalog();

    IconSource icons();

    DocumentationSource documentation();

    /** Brings a saved configuration to the current version of its component. Blocks. */
    default ConfigurationMigrator migrator() {
        return (id, fromVersion, configuration) -> {
            throw new UnsupportedOperationException("This connection cannot migrate configurations");
        };
    }

    /** Runs the services of the components. Blocks, like the rest of the connection. */
    default ActionInvoker actions() {
        return request -> {
            throw new UnsupportedOperationException("This connection cannot run the services of a component");
        };
    }

    /** Drops what was kept from the server: the next catalog call goes to the network. Does not block. */
    void invalidate();
}
