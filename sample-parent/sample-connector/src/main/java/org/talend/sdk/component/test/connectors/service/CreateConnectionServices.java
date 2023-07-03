/**
 * Copyright (C) 2006-2023 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.test.connectors.service;

import org.talend.sdk.component.api.service.Service;
import org.talend.sdk.component.api.service.connection.CreateConnection;

@Service
public class CreateConnectionServices {

    /**
     * In this service sample class we will implement existing particular actions to check their API usages.
     * Services actions are listed here: https://talend.github.io/component-runtime/main/latest/services-actions.html
     *
     * Implemented:
     * - CreateConnection
     * https://talend.github.io/component-runtime/main/latest/services-actions.html#_create_connection
     *
     */

    public final static String CREATE_CONNECTION = "action_CREATE_CONNECTION";

    /**
     * Create Connection action
     *
     * Documentation: https://talend.github.io/component-runtime/main/latest/services-actions.html#_create_connection
     * Type: create_connection
     * API: @org.talend.sdk.component.api.service.connection.CreateConnection
     *
     */

    @CreateConnection(CREATE_CONNECTION)
    public Object createConnection() {
        return "{\"connection_create_status\":\"successful\"}";
    }

}
