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

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.joining;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.server.front.model.PropertyValidation;
import org.talend.sdk.component.server.front.model.SimplePropertyDefinition;

class ConfigurationReaderTest {

    @Test
    void visit() {
        final Collection<ConfigurationVisitor.ConfigurationEntry> entries = new ArrayList<>();
        new ConfigurationReader(new TreeMap<String, String>() {

            {
                put("root.secret", "clear");
                put("root.urls[0]", "first");
                put("root.urls[1]", "second");
            }
        }, new ConfigurationVisitor() {

            @Override
            public void onEntry(final ConfigurationEntry entry) {
                entries.add(entry);
            }
        }, asList(newProperty("root", "OBJECT", emptyMap()),
                newProperty("root.secret", "STRING", singletonMap("ui::credential", "true")),
                newProperty("root.urls", "ARRAY", emptyMap()),
                newProperty("root.urls[${index}]", "STRING", emptyMap()))).visit();
        assertEquals(3, entries.size());
        assertEquals(
                "root.secret/clear/root.secret/true\nroot.urls[0]/first/root.urls[${index}]/false\nroot.urls[1]/second/root.urls[${index}]/false",
                entries
                        .stream()
                        .map(it -> it.getKey() + "/" + it.getValue() + "/" + it.getDefinition().getPath() + '/'
                                + it.isCredential())
                        .sorted()
                        .collect(joining("\n")));
    }

    private SimplePropertyDefinition newProperty(final String path, final String type, final Map<String, String> meta) {
        return new SimplePropertyDefinition(path, path.substring(path.lastIndexOf('.') + 1), "The Bar", type, "set",
                new PropertyValidation(false, null, null, null, null, null, null, false, null, null), meta, null, null);
    }
}
