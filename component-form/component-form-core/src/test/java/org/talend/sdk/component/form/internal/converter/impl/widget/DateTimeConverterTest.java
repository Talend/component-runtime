/**
 * Copyright (C) 2006-2023 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.form.internal.converter.impl.widget;

import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.talend.sdk.component.form.internal.converter.PropertyContext;
import org.talend.sdk.component.form.model.jsonschema.JsonSchema;
import org.talend.sdk.component.form.model.uischema.UiSchema;
import org.talend.sdk.component.server.front.model.PropertyValidation;
import org.talend.sdk.component.server.front.model.SimplePropertyDefinition;

class DateTimeConverterTest {

    @ParameterizedTest
    @CsvSource({ "date,date,dateFormat=YYYY/MM/DD", "time,datetime,useSeconds=true",
            "datetime,datetime,useSeconds=true|dateFormat=YYYY/MM/DD|useUTC=true",
            "zoneddatetime,datetime,useSeconds=false|dateFormat=YYYY-MM-DD|useUTC=false", })
    void convert(final String type, final String widget, final String options)
            throws ExecutionException, InterruptedException {

        final SimplePropertyDefinition definition = new SimplePropertyDefinition("configuration.input", "input",
                "input", "STRING", null, new PropertyValidation(), new HashMap<String, String>() {

                    {
                        put("ui::datetime", type);
                        Map<String, Object> map = loadMap(options);
                        String val = String.valueOf(map.get("dateFormat"));
                        if (map.get("dateFormat") != null) {
                            put("ui::datetime::dateFormat", val);
                        }
                        val = String.valueOf(map.get("useSeconds"));
                        if (map.get("useSeconds") != null) {
                            put("ui::datetime::useSeconds", val);
                        }
                        val = String.valueOf(map.get("useUTC"));
                        if (map.get("useUTC") != null) {
                            put("ui::datetime::useUTC", val);
                        }
                    }
                }, null, null);
        final Collection<UiSchema> schemas = new ArrayList<>();
        final DateTimeConverter converter =
                new DateTimeConverter(schemas, singleton(definition), emptyList(), new JsonSchema(), "en", type);
        converter
                .convert(completedFuture(new PropertyContext<>(definition, null, new PropertyContext.Configuration())))
                .toCompletableFuture()
                .get();

        final UiSchema schema = schemas.iterator().next();
        assertEquals(widget, schema.getWidget());
        assertEquals(loadMap(options).values().toString(), schema.getOptions().values().toString());
    }

    private Map<String, Object> loadMap(final String options) {
        if ("none".equals(options)) {
            return null;
        }
        final Properties properties = new Properties();
        try (final StringReader reader = new StringReader(options.replace("|", "\n"))) {
            try {
                properties.load(reader);
            } catch (final IOException e) {
                fail(e.getMessage());
            }
        }
        return properties
                .stringPropertyNames()
                .stream()
                .collect(toMap(identity(),
                        it -> Optional
                                .of(properties.getProperty(it))
                                .map(value -> "true".equals(value) ? true : value)
                                .orElse(null)));
    }
}
