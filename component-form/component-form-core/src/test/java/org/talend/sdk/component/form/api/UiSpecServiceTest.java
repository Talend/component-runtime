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
package org.talend.sdk.component.form.api;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.form.model.Ui;
import org.talend.sdk.component.form.model.jsonschema.JsonSchema;
import org.talend.sdk.component.form.model.uischema.UiSchema;
import org.talend.sdk.component.server.front.model.ComponentDetail;
import org.talend.sdk.component.server.front.model.ComponentDetailList;
import org.talend.sdk.component.server.front.model.ComponentIndices;

class UiSpecServiceTest {

    private final UiSpecService service = new UiSpecService(new Client() {

        @Override
        public Map<String, Object> action(final String family, final String type, final String action,
                final Map<String, Object> params) {
            if ("jdbc".equals(family) && "dynamic_values".equals(type) && "driver".equals(action)) {
                Map<String, String> item = new HashMap<>();
                item.put("id", "some.driver.Jdbc");
                item.put("label", "Jdbc driver");
                return singletonMap("items", singleton(item));
            }
            return params;
        }

        @Override
        public ComponentIndices index(final String language) {
            return null;
        }

        @Override
        public ComponentDetailList details(final String language, final String identifier,
                final String... identifiers) {
            return null;
        }

        @Override
        public void close() {
            // no-op
        }
    });

    @Test
    void jsonSchemaArray() throws Exception {
        final Ui payload = service.convert(load("rest-api.json"));
        final JsonSchema commonConfig = payload
                .getJsonSchema()
                .getProperties()
                .values()
                .iterator()
                .next()
                .getProperties()
                .entrySet()
                .stream()
                .filter(e -> e.getKey().equals("commonConfig"))
                .findFirst()
                .map(Map.Entry::getValue)
                .orElseThrow(IllegalStateException::new);
        assertTrue(commonConfig.getProperties().keySet().stream().anyMatch(p -> p.equals("fields")));
        assertTrue(commonConfig.getProperties().keySet().stream().noneMatch(p -> p.equals("fields[]")));
    }

    @Test
    void gridLayout() throws Exception {
        final Ui payload = service.convert(load("rest-api.json"));
        final UiSchema tableDataSet = payload.getUiSchema().iterator().next();
        assertEquals(2, tableDataSet.getItems().size());
        final Iterator<UiSchema> tableDataSetIt = tableDataSet.getItems().iterator();
        final UiSchema tableDataSetMain = tableDataSetIt.next();
        assertEquals("Main", tableDataSetMain.getTitle());

        assertEquals(3, tableDataSetMain.getItems().size());
        assertEquals(asList("dataStore", "commonConfig", "queryBuilder"),
                tableDataSetMain.getItems().stream().map(UiSchema::getTitle).collect(toList()));

        final Iterator<UiSchema> mainIt = tableDataSetMain.getItems().iterator();
        final UiSchema dataStore = mainIt.next();
        Iterator<UiSchema> dataStoreIt = dataStore.getItems().iterator();
        dataStoreIt.next();
        final UiSchema credentials = dataStoreIt.next();
        assertEquals("columns", credentials.getWidget());
        assertEquals(asList("Username", "Password"),
                credentials.getItems().stream().map(UiSchema::getTitle).collect(toList()));

        final UiSchema tableDataSetAdvanced = tableDataSetIt.next();
        assertEquals("Advanced", tableDataSetAdvanced.getTitle());
    }

    @Test
    void triggerRelativeParameters() throws Exception {
        final Ui payload = service.convert(load("relative-params.json"));
        final Collection<UiSchema> schema = payload.getUiSchema();
        final UiSchema.Trigger driverTrigger = schema
                .iterator()
                .next()
                .getItems()
                .stream()
                .filter(c -> c.getTitle().equals("JDBC Connection"))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No connection"))
                .getItems()
                .iterator()
                .next()
                .getTriggers()
                .iterator()
                .next();
        assertEquals("driver", driverTrigger.getAction());

        // here is what the test validates: the parameters are flattened and translated
        // for the ui
        assertEquals(3, driverTrigger.getParameters().size(), driverTrigger.toString());

        final Iterator<UiSchema.Parameter> params = driverTrigger.getParameters().iterator();
        assertTrue(params.hasNext());
        assertTriggerParameter(params.next(), "value.driver", "configuration.connection.driver");
        assertTriggerParameter(params.next(), "query.timeout", "configuration.query.timeout");
        assertTriggerParameter(params.next(), "query.sql", "configuration.query.sql");
    }

    @Test
    void jsonSchema() throws Exception {
        final Ui payload = service.convert(load("jdbc.json"));
        final JsonSchema jsonSchema = payload.getJsonSchema();
        assertNotNull(jsonSchema);
        assertEquals("JDBC Input", jsonSchema.getTitle());
        assertEquals("object", jsonSchema.getType());

        assertEquals(1, jsonSchema.getProperties().size());
        assertProperty(jsonSchema.getProperties().get("configuration"), "object", "Configuration", p -> {
            final Map<String, JsonSchema> nestedProperties = p.getProperties();
            assertEquals(2, nestedProperties.size());
            assertTrue(TreeMap.class.isInstance(nestedProperties));
            assertEquals(asList("connection", "query"), new ArrayList<>(nestedProperties.keySet()));
            assertProperty(nestedProperties.get("query"), "object", "query", q -> {
                final Map<String, JsonSchema> queryNestedProperties = q.getProperties();
                assertEquals(2, queryNestedProperties.size());
                assertProperty(queryNestedProperties.get("query"), "string", "query",
                        query -> assertNull(query.getProperties()));
                assertProperty(queryNestedProperties.get("timeout"), "number", "timeout",
                        timeout -> assertNull(timeout.getProperties()));
            });
            assertProperty(nestedProperties.get("connection"), "object", "JDBC Connection", q -> {
                final Map<String, JsonSchema> queryNestedProperties = q.getProperties();
                assertEquals(4, queryNestedProperties.size());
                assertProperty(queryNestedProperties.get("password"), "string", "password",
                        pwd -> assertNull(pwd.getProperties()));
                assertProperty(queryNestedProperties.get("driver"), "string", "driver",
                        driver -> assertNull(driver.getProperties()));
                assertProperty(queryNestedProperties.get("url"), "string", "url",
                        url -> assertNull(url.getProperties()));
                assertProperty(queryNestedProperties.get("username"), "string", "username", username -> {
                    assertNull(username.getProperties());
                    assertEquals("sa", username.getDefaultValue());
                });
            });
        });
    }

    @Test
    void properties() throws Exception {
        final Ui payload = service.convert(load("jdbc.json"));
        final Map<String, Object> properties = Map.class.cast(payload.getProperties());
        assertEquals(1, properties.size());

        final Map<String, Object> configuration = Map.class.cast(properties.get("configuration"));
        assertEquals(2, configuration.size());

        final Map<String, Object> connection = Map.class.cast(configuration.get("connection"));
        assertEquals(2, connection.size());

        assertEquals("", connection.get("password"));
        assertEquals("sa", connection.get("username"));
    }

    @Test
    void uiSchema() throws Exception {
        final Ui payload = service.convert(load("jdbc.json"));
        final Collection<UiSchema> uiSchema = payload.getUiSchema();
        assertNotNull(uiSchema);
        assertEquals(1, uiSchema.size());
        assertUiSchema(uiSchema.iterator().next(), "fieldset", "Configuration", "configuration", 3, schema -> {
            final List<UiSchema> items = new ArrayList<>(schema.getItems());
            items.sort(Comparator.comparing(UiSchema::getTitle));
            final Iterator<UiSchema> it = items.iterator();
            assertUiSchema(it.next(), "button", "Guess Schema", "button_schema_configuration", 0, guessSchema -> {
                assertNotNull(guessSchema.getTriggers());
                assertEquals(1, guessSchema.getTriggers().size());
            });
            assertUiSchema(it.next(), "fieldset", "JDBC Connection", "configuration.connection", 5, connection -> {
                final List<UiSchema> connectionItems = new ArrayList<>(connection.getItems());
                connectionItems.sort(Comparator.comparing(UiSchema::getTitle));
                final Iterator<UiSchema> connectionIt = connectionItems.iterator();
                assertUiSchema(connectionIt.next(), "button", "Validate Datastore",
                        "button_healthcheck_configuration.connection", 0, validateDataStore -> {
                            assertEquals(1, validateDataStore.getTriggers().size());
                            final UiSchema.Trigger trigger = validateDataStore.getTriggers().iterator().next();
                            assertEquals(4, trigger.getParameters().size());
                            assertEquals(
                                    Stream
                                            .of("driver", "password", "url", "username")
                                            .map(p -> "configuration.connection." + p)
                                            .collect(toSet()),
                                    trigger.getParameters().stream().map(UiSchema.Parameter::getPath).collect(toSet()));
                            assertEquals(
                                    Stream
                                            .of("driver", "password", "url", "username")
                                            .map(p -> "datastore." + p)
                                            .collect(toSet()),
                                    trigger.getParameters().stream().map(UiSchema.Parameter::getKey).collect(toSet()));
                        });
                assertUiSchema(connectionIt.next(), "multiSelectTag", "driver", "configuration.connection.driver", 0,
                        driver -> {
                            assertNotNull(driver.getTriggers());
                            assertEquals(1, driver.getTriggers().size());
                            final Collection<UiSchema.NameValue> titleMap = driver.getTitleMap();
                            assertEquals(1, titleMap.size());
                            final UiSchema.NameValue firstTitleMap = titleMap.iterator().next();
                            assertEquals("some.driver.Jdbc", firstTitleMap.getName());
                            assertEquals("Jdbc driver", firstTitleMap.getValue());

                            final UiSchema.Trigger trigger = driver.getTriggers().iterator().next();
                            assertEquals("driver", trigger.getAction());
                            assertEquals("jdbc", trigger.getFamily());
                            assertEquals("validation", trigger.getType());

                            assertEquals(1, trigger.getParameters().size());
                            final UiSchema.Parameter parameter = trigger.getParameters().iterator().next();
                            assertEquals("value", parameter.getKey());
                            assertEquals("configuration.connection.driver", parameter.getPath());
                        });
                assertUiSchema(connectionIt.next(), "text", "password", "configuration.connection.password", 0, pwd -> {
                    assertEquals("password", pwd.getType());
                });
                assertUiSchema(connectionIt.next(), "text", "url", "configuration.connection.url", 0, url -> {
                });
                assertUiSchema(connectionIt.next(), "text", "username", "configuration.connection.username", 0,
                        username -> {
                        });
                assertFalse(connectionIt.hasNext());
            });
            assertUiSchema(it.next(), "fieldset", "query", "configuration.query", 2, query -> {
                final List<UiSchema> queryItems = new ArrayList<>(query.getItems());
                queryItems.sort(Comparator.comparing(UiSchema::getKey));
                final Iterator<UiSchema> queryIt = queryItems.iterator();
                assertUiSchema(queryIt.next(), "text", "query", "configuration.query.query", 0, q -> {
                });
                assertUiSchema(queryIt.next(), "text", "timeout", "configuration.query.timeout", 0, timeout -> {
                });
                assertFalse(queryIt.hasNext());
            });
            assertFalse(it.hasNext());
        });
    }

    /*
     * just to log the output
     */
    @Test
    @Disabled("debug test to log the produced model")
    void out() throws Exception {
        final Ui payload = service.convert(load("jdbc.json"));
        System.out.println(JsonbBuilder.create(new JsonbConfig().withFormatting(true)).toJson(payload));
    }

    private void assertUiSchema(final UiSchema schema, final String widget, final String title, final String key,
            final int nestedSize, final Consumer<UiSchema> customValidator) {
        assertEquals(widget, schema.getWidget(), schema.toString());
        assertEquals(title, schema.getTitle());
        assertEquals(key, schema.getKey());
        if (schema.getItems() == null) {
            assertEquals(nestedSize, 0);
        } else {
            assertEquals(nestedSize, schema.getItems().size());
        }
        customValidator.accept(schema);
    }

    private void assertProperty(final JsonSchema property, final String type, final String title,
            final Consumer<JsonSchema> customValidator) {
        assertEquals(type, property.getType());
        assertEquals(title, property.getTitle());
        customValidator.accept(property);
    }

    private ComponentDetail load(final String resource) throws Exception {
        try (final Jsonb jsonb = JsonbBuilder.create();
                final InputStream stream =
                        Thread.currentThread().getContextClassLoader().getResourceAsStream(resource)) {
            return jsonb.fromJson(stream, ComponentDetail.class);
        }
    }

    private void assertTriggerParameter(final UiSchema.Parameter next, final String key, final String path) {
        assertEquals(key, next.getKey());
        assertEquals(path, next.getPath());
    }

}
