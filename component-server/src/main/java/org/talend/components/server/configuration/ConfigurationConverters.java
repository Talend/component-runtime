/**
 *  Copyright (C) 2006-2017 Talend Inc. - www.talend.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.talend.components.server.configuration;

import static lombok.AccessLevel.PRIVATE;

import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

import org.apache.deltaspike.core.api.config.ConfigResolver;

@NoArgsConstructor(access = PRIVATE)
public final class ConfigurationConverters {

    public static class SetConverter implements ConfigResolver.Converter<Set<String>> {

        @Override
        public Set<String> convert(final String value) {
            final Set<String> out = new HashSet<>();
            StringBuilder currentValue = new StringBuilder();
            int length = value.length();
            for (int i = 0; i < length; i++) {
                char c = value.charAt(i);
                if (c == '\\') {
                    if (i < length - 1) {
                        char nextC = value.charAt(i + 1);
                        currentValue.append(nextC);
                        i++;
                    }
                } else if (c == ',') {
                    String trimedVal = currentValue.toString().trim();
                    if (trimedVal.length() > 0) {
                        out.add(trimedVal);
                    }

                    currentValue.setLength(0);
                } else {
                    currentValue.append(c);
                }
            }

            String trimedVal = currentValue.toString().trim();
            if (trimedVal.length() > 0) {
                out.add(trimedVal);
            }
            return out;
        }
    }
}
