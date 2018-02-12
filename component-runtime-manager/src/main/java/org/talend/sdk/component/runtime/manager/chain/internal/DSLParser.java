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

import static java.util.stream.Collectors.toMap;
import static lombok.AccessLevel.PRIVATE;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = PRIVATE)
public class DSLParser {

    public static Step parse(final String rawUri) {
        final URI uri = URI.create(rawUri);
        final Map<String, String> query = parseQuery(uri.getQuery());
        final String version = query.remove("__version");
        return new Step(uri.getScheme(), uri.getAuthority(), version != null ? Integer.parseInt(version.trim()) : -1,
                query);
    }

    private static Map<String, String> parseQuery(final String query) {
        return query == null ? new HashMap<>() : Stream.of(query.split("&")).map(s -> {
            final int equal = s.indexOf('=');
            if (equal > 0) {
                return new String[] { s.substring(0, equal), s.substring(equal + 1, s.length()) };
            }
            return new String[] { s, "true" };
        }).collect(toMap(s -> s[0], s -> s[1]));
    }

    @Data
    public static class Step {

        private final String family;

        private final String component;

        private final int version;

        private final Map<String, String> configuration;
    }
}
