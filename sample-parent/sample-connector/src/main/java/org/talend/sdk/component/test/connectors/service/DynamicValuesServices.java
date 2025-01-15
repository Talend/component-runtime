/**
 * Copyright (C) 2006-2025 Talend Inc. - www.talend.com
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

import java.util.Arrays;

import org.talend.sdk.component.api.service.Service;
import org.talend.sdk.component.api.service.completion.DynamicValues;
import org.talend.sdk.component.api.service.completion.Values;

@Service
public class DynamicValuesServices {

    /**
     * In this service sample class we will implement existing particular actions to check their API usages.
     * Services actions are listed here: https://talend.github.io/component-runtime/main/latest/services-actions.html
     *
     * Implemented:
     * - DynamicValues https://talend.github.io/component-runtime/main/latest/ref-actions.html#_dynamic_values
     *
     */

    public final static String DYNAMIC_VALUES = "action_DYNAMIC_VALUES";

    /**
     * DynamicValues action
     * Documentation: https://talend.github.io/component-runtime/main/latest/ref-actions.html#_dynamic_values
     * Type: dynamic_values
     * API: @org.talend.sdk.component.api.service.completion.DynamicValues
     * Returned type: org.talend.sdk.component.api.service.completion.Values
     */

    @DynamicValues(DYNAMIC_VALUES)
    public Values actions() {
        return new Values(Arrays.asList(
                new Values.Item("1", "Delete"),
                new Values.Item("2", "Insert"),
                new Values.Item("3", "Update")));
    }

}
