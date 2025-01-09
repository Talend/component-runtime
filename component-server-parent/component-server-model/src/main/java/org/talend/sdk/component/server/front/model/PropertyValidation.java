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
package org.talend.sdk.component.server.front.model;

import java.util.Collection;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PropertyValidation { // note: we keep names specific per type to ensure we never get any overlap

    private Boolean required;

    // for numbers
    private Integer min;

    // for numbers
    private Integer max;

    // for strings
    private Integer minLength;

    // for strings
    private Integer maxLength;

    // for arrays
    private Integer minItems;

    // for arrays
    private Integer maxItems;

    // for arrays
    private Boolean uniqueItems;

    // for strings
    private String pattern; // for js use http://xregexp.com/

    // for enum
    private Collection<String> enumValues;
}
