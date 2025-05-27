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
import org.talend.sdk.component.api.configuration.constraint.Max;
import org.talend.sdk.component.api.configuration.constraint.Min;
import org.talend.sdk.component.api.configuration.constraint.Pattern;
import org.talend.sdk.component.api.configuration.constraint.Required;
import org.talend.sdk.component.api.configuration.constraint.Uniques;
import org.talend.sdk.component.api.configuration.ui.layout.GridLayout;
import org.talend.sdk.component.api.meta.Documentation;

import lombok.Data;

@Data
@GridLayout(value = {
        @GridLayout.Row({ "stringWithMaxLength" }),
        @GridLayout.Row({ "stringWithMinLength" }),
        @GridLayout.Row({ "stringWithPattern" }),
        @GridLayout.Row({ "intWithMin" }),
        @GridLayout.Row({ "intWithMax" }),
        @GridLayout.Row({ "aRequiredString" }),
        @GridLayout.Row({ "listWithMaxItems" }),
        @GridLayout.Row({ "listWithMinItems" }),
        @GridLayout.Row({ "listWithUniqueItems" }),
})
public class ElementsWithConstraints implements Serializable {

    @Option
    @Documentation("A string with a max length.")
    @Max(10)
    private String stringWithMaxLength;

    @Option
    @Documentation("A string with a min length.")
    @Min(3)
    private String stringWithMinLength;

    @Option
    @Documentation("A string with pattern.")
    @Pattern("^start[abcdef]{0,5}END$")
    private String stringWithPattern;

    @Option
    @Documentation("An integer with 100 as min value.")
    @Min(100)
    private Integer intWithMin;

    // Todo: Issue in studio
    @Option
    @Documentation("An integer with 100 as max value.")
    @Max(100)
    private Integer intWithMax;

    @Option
    @Documentation("A required string field.")
    @Required
    private String aRequiredString;

    // Todo: not implemented in studio
    @Option
    @Documentation("A list of key/value pairs with a maximum number of items.")
    @Max(5)
    private List<KeyValue> listWithMaxItems = new ArrayList<>();

    // Todo: not implemented in studio
    @Option
    @Documentation("A list of key/value pairs with a minimum number of items.")
    @Min(2)
    private List<KeyValue> listWithMinItems = new ArrayList<>();

    // Todo: not implemented nor in studio nor in webUI
    @Option
    @Documentation("A list of key/value pairs with a uniq items.")
    @Uniques
    private List<KeyValue> listWithUniqueItems = new ArrayList<>();
}