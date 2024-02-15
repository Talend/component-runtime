/**
 * Copyright (C) 2006-2024 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.form.internal.converter.impl;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonMap;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.form.internal.converter.PropertyContext;
import org.talend.sdk.component.server.front.model.PropertyValidation;
import org.talend.sdk.component.server.front.model.SimplePropertyDefinition;

class PropertiesConverterTest {

    @Test
    void datalistDefault() throws Exception {
        final Map<String, Object> values = new HashMap<>();
        try (final Jsonb jsonb = JsonbBuilder.create()) {
            new PropertiesConverter(jsonb, values, emptyList())
                    .convert(completedFuture(new PropertyContext<>(new SimplePropertyDefinition("configuration.foo.bar",
                            "bar", "Bar", "STRING", "def", new PropertyValidation(),
                            singletonMap("action::suggestionS", "yes"), null, new LinkedHashMap<>()), null,
                            new PropertyContext.Configuration())))
                    .toCompletableFuture()
                    .get();
        }
        assertEquals(2, values.size());
        assertEquals("def", values.get("bar"));
        assertEquals("def", values.get("$bar_name"));
    }
}
