/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.proxy.service.lang;

import static java.util.Optional.ofNullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class Substitutor {

    public Function<Function<String, String>, String> compile(final String value) {
        return compile(new HashSet<>(), value);
    }

    public Function<Function<String, String>, String> compile(final Set<String> dynamicHeaderKeys, final String value) {
        if (value.contains("${") && value.contains("}")) {
            final Map<String, String> toReplace = new HashMap<>();
            int lastEnd = -1;
            do {
                final int start = value.indexOf("${", lastEnd);
                if (start < 0) {
                    break;
                }
                final int end = value.indexOf('}', start);
                if (end < start) {
                    break;
                }

                final String rawKey = value.substring(start + "${".length(), end);
                dynamicHeaderKeys.add(rawKey);
                toReplace.put(value.substring(start, end + 1), rawKey);
                lastEnd = end;
            } while (lastEnd > 0);
            if (!toReplace.isEmpty()) {
                return placeholders -> {
                    String output = value;
                    for (final Map.Entry<String, String> placeholder : toReplace.entrySet()) {
                        output = output.replace(placeholder.getKey(),
                                ofNullable(placeholders.apply(placeholder.getValue())).orElse(""));
                    }
                    return output;
                };
            }
        }
        return ignored -> value;
    }
}
