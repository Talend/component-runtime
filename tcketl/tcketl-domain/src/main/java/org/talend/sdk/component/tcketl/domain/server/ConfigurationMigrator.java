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
package org.talend.sdk.component.tcketl.domain.server;

import java.util.Map;

import org.talend.sdk.component.tcketl.domain.connector.ConnectorId;

/**
 * Asks the server to bring the configuration saved with an older version of a connector to its current version. The
 * call blocks, so it MUST NOT be made from the JavaFX application thread; a failure is a {@link ServerException}.
 */
public interface ConfigurationMigrator {

    /**
     * @param id the connector.
     * @param fromVersion the version the configuration was saved with.
     * @param configuration the saved flat configuration.
     * @return the configuration for the current version of the connector.
     */
    Map<String, String> migrate(ConnectorId id, int fromVersion, Map<String, String> configuration);
}
