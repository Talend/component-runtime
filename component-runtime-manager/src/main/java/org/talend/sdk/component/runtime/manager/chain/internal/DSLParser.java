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
package org.talend.sdk.component.runtime.manager.chain.internal;

import static lombok.AccessLevel.PRIVATE;
import static org.talend.sdk.component.runtime.manager.util.Overrides.override;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = PRIVATE)
public class DSLParser {

    public static Step parse(final String rawUri) {
        final URI uri = URI.create(rawUri);
        final Map<String, String> query = parseQuery(uri.getRawQuery());
        final String version = query.remove("__version");
        return new Step(uri.getScheme(), uri.getAuthority(), version != null ? Integer.parseInt(version.trim()) : -1,
                query);
    }

    // taken from tomcat and adapted to this need
    private static Map<String, String> parseQuery(final String query) {
        final Map<String, String> parameters = new HashMap<>();
        if (query == null || query.trim().isEmpty()) {
            return parameters;
        }

        final byte[] bytes = query.getBytes(StandardCharsets.UTF_8);

        int pos = 0;
        int end = bytes.length;

        String name;
        String value;
        while (pos < end) {
            int nameStart = pos;
            int nameEnd = -1;
            int valueStart = -1;
            int valueEnd = -1;

            boolean parsingName = true;
            boolean decodeName = false;
            boolean decodeValue = false;
            boolean parameterComplete = false;

            do {
                switch (bytes[pos]) {
                case '=':
                    if (parsingName) {
                        nameEnd = pos;
                        parsingName = false;
                        valueStart = ++pos;
                    } else {
                        pos++;
                    }
                    break;
                case '&':
                    if (parsingName) {
                        // Name finished. No value.
                        nameEnd = pos;
                    } else {
                        // Value finished
                        valueEnd = pos;
                    }
                    parameterComplete = true;
                    pos++;
                    break;
                case '%':
                case '+':
                    // Decoding required
                    if (parsingName) {
                        decodeName = true;
                    } else {
                        decodeValue = true;
                    }
                    pos++;
                    break;
                default:
                    pos++;
                    break;
                }
            } while (!parameterComplete && pos < end);

            if (pos == end) {
                if (nameEnd == -1) {
                    nameEnd = pos;
                } else if (valueStart > -1 && valueEnd == -1) {
                    valueEnd = pos;
                }
            }

            if (nameEnd <= nameStart) {
                continue;
            }

            name = new String(bytes, nameStart, nameEnd - nameStart);
            if (decodeName) {
                name = decode(name);
            }
            if (valueStart >= 0) {
                value = new String(bytes, valueStart, valueEnd - valueStart);
                if (decodeValue) {
                    value = decode(value);
                }
            } else {
                value = "true"; // tomcat defaults to "", but for us (configuration) true is likely better
            }

            parameters.put(name, value);
        }
        return parameters;
    }

    private static String decode(final String query) {
        // cheap impl
        return URI.create("foo://bar?" + query).getQuery();
    }

    @Data
    public static class Step {

        private final String family;

        private final String component;

        private final int version;

        private final Map<String, String> configuration;

        public Step withOverride(final boolean override) {
            if (override) {
                return new Step(family, component, version, override(family + '.' + component, configuration));
            }
            return this;
        }
    }
}
