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
package org.talend.sdk.component.tcketl.app.connection;

import org.talend.sdk.component.tcketl.config.ApplicationConfig;
import org.talend.sdk.component.tcketl.domain.server.ServerStatus;

/** Asks a server who it is, to check that the settings of the connection lead to a component server. */
@FunctionalInterface
public interface ConnectionTester {

    /**
     * Blocking.
     *
     * @throws org.talend.sdk.component.tcketl.domain.server.ServerException when the server cannot be used
     */
    ServerStatus test(ApplicationConfig config);
}
