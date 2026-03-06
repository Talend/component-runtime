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
package org.talend.sdk.component.test.connectors.service;

import javax.json.Json;
import javax.json.JsonObject;

import org.talend.sdk.component.api.service.Action;
import org.talend.sdk.component.api.service.Service;

@Service
public class UserServices {

    /**
     * In this service sample class we will implement existing particular actions to check their API usages.
     * Services actions are listed here: https://talend.github.io/component-runtime/main/latest/services-actions.html
     *
     * Implemented:
     * - User
     * https://talend.github.io/component-runtime/main/latest/services-actions.html#_user
     *
     */

    public final static String USER = "action_USER";

    /**
     * User action
     *
     * Documentation: https://talend.github.io/component-runtime/main/latest/services-actions.html#_user
     * Type: user
     * API: @org.talend.sdk.component.api.service.Action
     *
     */

    @Action(USER)
    public JsonObject userAction() {
        JsonObject jsonObject = Json.createObjectBuilder().add("username", "talend").add("password", "123456").build();
        return jsonObject;
    }

}
