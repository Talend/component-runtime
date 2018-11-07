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
package org.talend.sdk.component.proxy.service;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.bind.Jsonb;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.proxy.service.qualifier.UiSpecProxy;
import org.talend.sdk.component.proxy.test.CdiInject;
import org.talend.sdk.component.proxy.test.WithServer;
import org.talend.sdk.component.server.front.model.ComponentDetail;
import org.talend.sdk.component.server.front.model.PropertyValidation;
import org.talend.sdk.component.server.front.model.SimplePropertyDefinition;

@CdiInject
@WithServer
class ConfigurationFormatterImplTest {

    @Inject
    private ConfigurationFormatterImpl formatter;

    @Inject
    @UiSpecProxy
    private JsonBuilderFactory factory;

    @Inject
    @UiSpecProxy
    private Jsonb jsonb;

    @Test
    void flattenMarketo() {
        final Map<String, String> properties = formatter
                .flatten(factory
                        .createObjectBuilder()
                        .add("dataStore",
                                factory
                                        .createObjectBuilder()
                                        .add("clientId", "xxxx")
                                        .add("clientSecret", "yyyy")
                                        .add("endpoint", "https://foo.bar.colm"))
                        .build());
        assertEquals(3, properties.size());
        assertEquals(properties.get("dataStore.clientId"), "xxxx");
        assertEquals(properties.get("dataStore.clientSecret"), "yyyy");
        assertEquals(properties.get("dataStore.endpoint"), "https://foo.bar.colm");
    }

    @Test
    void unflattenMarketo() throws IOException {
        final ComponentDetail detail;
        try (final InputStream stream =
                Thread.currentThread().getContextClassLoader().getResourceAsStream("marketo.form.json")) {
            detail = jsonb.fromJson(stream, ComponentDetail.class);
        }

        final Map<String, String> properties = new HashMap<>();
        properties.put("configuration.activityTypeIds[0].activity", "FOO");

        final JsonObject object = formatter.unflatten(detail.getProperties(), properties);
        assertEquals(1, object.size());
    }

    @Test
    void flattenFlatObject() {
        final JsonObject from = factory.createObjectBuilder().add("name", "N").add("age", 20.).build();
        final Map<String, String> flatten = formatter.flatten(from);
        assertEquals(new HashMap<String, String>() {

            {
                put("name", "N");
                put("age", "20.0");
            }
        }, flatten);
    }

    @Test
    void flattenMaxBatchSize() {
        final JsonObject model = formatter
                .unflatten(asList(
                        new SimplePropertyDefinition("configuration", "configuration", "configuration", "OBJECT", null,
                                new PropertyValidation(), emptyMap(), null, new LinkedHashMap<>()),
                        new SimplePropertyDefinition("configuration.$maxBatchSize", "$maxBatchSize", "$maxBatchSize",
                                "NUMBER", null, new PropertyValidation(), emptyMap(), null, new LinkedHashMap<>())),
                        singletonMap("configuration.$maxBatchSize", "1000.0"));
        assertEquals(1000., model.getJsonObject("configuration").getJsonNumber("$maxBatchSize").doubleValue());
    }

    @Test
    void flattenSimpleArray() {
        final JsonObject from = factory
                .createObjectBuilder()
                .add("urls", factory.createArrayBuilder().add("a").add("b").build())
                .build();
        final Map<String, String> flatten = formatter.flatten(from);
        assertEquals(new HashMap<String, String>() {

            {
                put("urls[0]", "a");
                put("urls[1]", "b");
            }
        }, flatten);
    }

    @Test
    void flattenArrayWithNull() {
        final JsonObject from = factory
                .createObjectBuilder()
                .add("urls", factory.createArrayBuilder().add("a").addNull().add("b").build())
                .build();
        final Map<String, String> flatten = formatter.flatten(from);
        assertEquals(new HashMap<String, String>() {

            {
                put("urls[0]", "a");
                put("urls[2]", "b");
            }
        }, flatten);
    }

    @Test
    void flattenObjectArray() {
        final JsonObject from = factory
                .createObjectBuilder()
                .add("people",
                        factory
                                .createArrayBuilder()
                                .add(factory.createObjectBuilder().add("name", "First").add("age", 20))
                                .add(factory.createObjectBuilder().add("name", "Second").add("age", 30))
                                .build())
                .build();
        final Map<String, String> flatten = formatter.flatten(from);
        assertEquals(new HashMap<String, String>() {

            {
                put("people[0].name", "First");
                put("people[0].age", "20.0");
                put("people[1].name", "Second");
                put("people[1].age", "30.0");
            }
        }, flatten);
    }

    @Test
    void flattenNestedObject() {
        final JsonObject from = factory
                .createObjectBuilder()
                .add("config", factory.createObjectBuilder().add("name", "N").add("age", 20.).build())
                .build();
        final Map<String, String> flatten = formatter.flatten(from);
        assertEquals(new HashMap<String, String>() {

            {
                put("config.name", "N");
                put("config.age", "20.0");
            }
        }, flatten);
    }

    @Test
    void unflattenTable() {
        final JsonObject object = formatter
                .unflatten(asList(prop("root", "OBJECT"), prop("root.table", "ARRAY"), prop("root.table[]", "STRING")),
                        new HashMap<String, String>() {

                            {
                                put("root.table[0]", "a");
                                put("root.table[1]", "b");
                            }
                        });
        assertEquals(1, object.size());
        final JsonObject root = object.getJsonObject("root");
        assertEquals(1, root.size());
        assertEquals(asList("a", "b"),
                root
                        .getJsonArray("table")
                        .stream()
                        .map(JsonString.class::cast)
                        .map(JsonString::getString)
                        .collect(toList()));
    }

    @Test
    void unflattenTableOfObject() {
        final JsonObject object =
                formatter
                        .unflatten(
                                asList(prop("root", "OBJECT"), prop("root.table", "ARRAY"),
                                        prop("root.table[].name", "STRING"), prop("root.table[].age", "NUMBER")),
                                new HashMap<String, String>() {

                                    {
                                        put("root.table[0].name", "a");
                                        put("root.table[0].age", "20");
                                        put("root.table[1].name", "b");
                                        put("root.table[1].age", "30");
                                    }
                                });
        assertEquals(1, object.size());
        final JsonObject root = object.getJsonObject("root");
        assertEquals(1, root.size());
        final JsonArray table = root.getJsonArray("table");
        assertEquals(2, table.size());
        assertEquals(asList("a", "b"),
                table.stream().map(JsonObject.class::cast).map(o -> o.getString("name")).collect(toList()));
    }

    @Test
    void unflattenComplex() {
        final JsonObject object =
                formatter
                        .unflatten(
                                asList(prop("root", "OBJECT"), prop("root.table", "ARRAY"),
                                        prop("root.table[].urls", "ARRAY"), prop("root.table[].urls[]", "STRING")),
                                new HashMap<String, String>() {

                                    {
                                        put("root.table[0].name", "a");
                                        put("root.table[0].age", "20");
                                        put("root.table[0].urls[1]", "http://2");
                                        put("root.table[0].urls[0]", "http://1");
                                        put("root.table[1].name", "b");
                                        put("root.table[1].age", "30");
                                    }
                                });
        assertEquals(1, object.size());
        final JsonArray table = object.getJsonObject("root").getJsonArray("table");
        assertEquals(2, table.size());
        assertFalse(table.getJsonObject(1).containsKey("urls"));
        final JsonObject first = table.getJsonObject(0);
        assertTrue(first.containsKey("urls"));
        assertEquals(asList("http://1", "http://2"),
                first
                        .getJsonArray("urls")
                        .stream()
                        .map(JsonString.class::cast)
                        .map(JsonString::getString)
                        .collect(toList()));
    }

    @Test
    void unflattenPrimitivesRoot() {
        final JsonObject object = formatter
                .unflatten(asList(prop("root", "OBJECT"), prop("root.name", "STRING"), prop("root.age", "NUMBER"),
                        prop("root.toggle", "BOOLEAN")), new HashMap<String, String>() {

                            {
                                put("root.name", "Sombody");
                                put("root.age", "30");
                                put("root.toggle", "true");
                            }
                        });
        assertEquals(1, object.size());
        final JsonObject root = object.getJsonObject("root");
        assertEquals(3, root.size());
        assertEquals("Sombody", root.getString("name"));
        assertEquals(30, root.getJsonNumber("age").longValue());
        assertTrue(root.getBoolean("toggle"));
    }

    @Test
    void unflattenPrimitivesNested() {
        final JsonObject object = formatter
                .unflatten(
                        asList(prop("root", "OBJECT"), prop("root.nested1", "OBJECT"), prop("root.nested2", "OBJECT"),
                                prop("root.nested3", "OBJECT"), // ignored in this test
                                prop("root.nested2.name", "STRING"), prop("root.nested1.name", "STRING"),
                                prop("root.nested1.age", "NUMBER"), prop("root.nested1.toggle", "BOOLEAN")),
                        new HashMap<String, String>() {

                            {
                                put("root.nested1.name", "Sombody");
                                put("root.nested1.age", "30");
                                put("root.nested1.toggle", "true");
                                put("root.nested2.name", "Other");
                            }
                        });
        assertEquals(1, object.size());
        final JsonObject root = object.getJsonObject("root");
        assertEquals(2, root.size());
        final JsonObject nested1 = root.getJsonObject("nested1");
        assertEquals(3, nested1.size());
        final JsonObject nested2 = root.getJsonObject("nested2");
        assertFalse(root.containsKey("nested3"));
        assertEquals(1, nested2.size());
        assertEquals("Sombody", nested1.getString("name"));
        assertEquals("Other", nested2.getString("name"));
        assertEquals(30, nested1.getJsonNumber("age").longValue());
        assertTrue(nested1.getBoolean("toggle"));
    }

    private SimplePropertyDefinition prop(final String path, final String type) {
        return new SimplePropertyDefinition(path, path.substring(path.lastIndexOf('.') + 1), null, type, null, null,
                emptyMap(), null, new LinkedHashMap<>());
    }
}
