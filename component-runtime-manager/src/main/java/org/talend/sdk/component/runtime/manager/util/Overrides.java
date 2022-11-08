/**
 * Copyright (C) 2006-2022 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.runtime.manager.util;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toMap;
import static lombok.AccessLevel.PRIVATE;

import java.util.HashMap;
import java.util.Map;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = PRIVATE)
public class Overrides {

    public static Map<String, String> override(final String prefix, final Map<String, String> input) {
        return ofNullable(input)
                .orElseGet(HashMap::new)
                .entrySet()
                .stream()
                .collect(toMap(Map.Entry::getKey,
                        e -> ofNullable(System.getProperty(prefix + "." + e.getKey())).orElseGet(e::getValue)));
    }
}
