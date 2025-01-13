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
package org.talend.sdk.component.documentation.sample.dropdownlist.components.service;

import java.util.Arrays;

import org.talend.sdk.component.api.service.Service;
import org.talend.sdk.component.api.service.completion.DynamicValues;
import org.talend.sdk.component.api.service.completion.Values;

@Service
public class DropdownlistComponentService {

    // you can put logic here you can reuse in components
    @DynamicValues("valuesProvider")
    public Values actions() {
        return new Values(Arrays
                .asList(new Values.Item("1", "Delete"), new Values.Item("2", "Insert"),
                        new Values.Item("3", "Update")));
    }
}