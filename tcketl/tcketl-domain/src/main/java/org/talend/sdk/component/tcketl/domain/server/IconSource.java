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

import java.util.Optional;

import org.talend.sdk.component.tcketl.domain.connector.ConnectorId;

/**
 * Icons of the component server. A missing icon is an empty result; failing to reach the server is a
 * {@link ServerException}. Implementations block: call them off the UI thread.
 */
public interface IconSource {

    Optional<IconData> componentIcon(ConnectorId id, IconTheme theme);

    /** {@code familyId} is the encoded family identifier found in the connector index. */
    Optional<IconData> familyIcon(String familyId, IconTheme theme);
}
