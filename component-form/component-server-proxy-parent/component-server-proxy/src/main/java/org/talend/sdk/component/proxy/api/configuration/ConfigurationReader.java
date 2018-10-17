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
package org.talend.sdk.component.proxy.api.configuration;

import static java.util.Objects.requireNonNull;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import org.talend.sdk.component.server.front.model.SimplePropertyDefinition;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ConfigurationReader {

    private final Map<String, String> entries;

    private final ConfigurationVisitor visitor;

    private final Collection<SimplePropertyDefinition> definitions;

    public void visit() {
        if (entries == null || entries.isEmpty() || visitor == null) {
            return;
        }
        requireNonNull(definitions, "Definitions can't be null to visit a configuration");
        entries
                .forEach((key, value) -> findDefinition(key)
                        .ifPresent(
                                def -> visitor.onEntry(new ConfigurationVisitor.ConfigurationEntry(key, value, def))));
    }

    private Optional<SimplePropertyDefinition> findDefinition(final String key) {
        final String lookupKey = getLookupKey(key);
        return definitions.stream().filter(it -> it.getPath().equals(lookupKey)).findFirst();
    }

    private String getLookupKey(final String key) {
        String lookupKey = key;
        while (true) {
            final int start = lookupKey.indexOf('[');
            final int end = lookupKey.indexOf(']');
            if (start < 0 || end < 0 || end < start) {
                break;
            }
            final String indexStr = lookupKey.substring(start + 1, end);
            try {
                Integer.parseInt(indexStr);
            } catch (final NumberFormatException nfe) {
                break;
            }
            lookupKey = lookupKey.substring(0, start + 1) + "${index}" + lookupKey.substring(end);
        }
        return lookupKey;
    }
}
