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
package org.talend.sdk.component.feature.form.config;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.configuration.ui.layout.GridLayout;
import org.talend.sdk.component.api.meta.Documentation;

import lombok.Data;

@Data
@GridLayout(value = {
        @GridLayout.Row({ "aListOfStrings" }),
        @GridLayout.Row({ "aListOfInts" }),
        @GridLayout.Row({ "aListOfBooleans" }),
        @GridLayout.Row({ "listOfAllSupportedTypes" }),
})
public class SomeLists implements Serializable {

    @Option
    @Documentation("A list of Strings (Bugged in web UI).")
    private List<String> aListOfStrings = new ArrayList<>();

    @Option
    @Documentation("A list of int (Bugged in web UI).")
    private List<Integer> aListOfInts = new ArrayList<>();

    @Option
    @Documentation("A list of boolean (Bugged in web UI).")
    private List<Boolean> aListOfBooleans = new ArrayList<>();

    @Option
    @Documentation("A list of all supported types.")
    private List<AllSupportedTypes> listOfAllSupportedTypes = new ArrayList<>();
}
