/**
 * Copyright (C) 2006-2024 Talend Inc. - www.talend.com
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
import org.talend.sdk.component.api.service.connection.CloseConnection;
import org.talend.sdk.component.api.service.connection.CloseConnectionObject;

@Service
public class CloseConnectionServices {

    /**
     * In this service sample class we will implement existing particular actions to check their API usages.
     * Services actions are listed here: https://talend.github.io/component-runtime/main/latest/services-actions.html
     *
     * Implemented:
     * - CloseConnection https://talend.github.io/component-runtime/main/latest/services-actions.html#_close_connection
     *
     */

    public final static String CLOSE_CONNECTION_TRUE = "action_CLOSE_CONNECTION_TRUE";

    public final static String CLOSE_CONNECTION_FALSE = "action_CLOSE_CONNECTION_FALSE";

    /**
     * Close Connection action True
     *
     * Documentation: https://talend.github.io/component-runtime/main/latest/services-actions.html#_close_connection
     * Type: close_connection
     * API: @org.talend.sdk.component.api.service.connection.CloseConnection
     *
     * Returned type: org.talend.sdk.component.api.service.connection.CloseConnectionObject
     */
    @CloseConnection(CLOSE_CONNECTION_TRUE)
    public CloseConnectionObject closeConnectionTrue() {
        return new CloseConnectionObject() {

            public boolean close() {
                return true;
            }
        };
    }

    /**
     * Close Connection action False
     *
     * Documentation: https://talend.github.io/component-runtime/main/latest/services-actions.html#_close_connection
     * Type: close_connection
     * API: @org.talend.sdk.component.api.service.connection.CloseConnection
     *
     * Returned type: org.talend.sdk.component.api.service.connection.CloseConnectionObject
     */
    @CloseConnection(CLOSE_CONNECTION_FALSE)
    public CloseConnectionObject closeConnectionFalse() {
        return new CloseConnectionObject() {

            public boolean close() {
                return false;
            }
        };
    }

}
