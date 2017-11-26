/**
 * Copyright (C) 2006-2017 Talend Inc. - www.talend.com
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
import static java.util.stream.Collectors.toSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;

import org.junit.Ignore;
import org.junit.Test;
import org.talend.sdk.component.form.model.UiSpecPayload;
import org.talend.sdk.component.server.front.model.ComponentDetail;
import org.talend.sdk.component.server.front.model.ComponentDetailList;
import org.talend.sdk.component.server.front.model.ComponentIndices;

public class UiSpecServiceTest {

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
    public void triggerRelativeParameters() throws Exception {
        final UiSpecPayload payload = service.convert(load("relative-params.json"));
        final Collection<UiSpecPayload.UiSchema> schema = payload.getUiSchema();
        final UiSpecPayload.Trigger driverTrigger = schema
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
        assertEquals(driverTrigger.toString(), 3, driverTrigger.getParameters().size());

        final Iterator<UiSpecPayload.Trigger.Parameter> params = driverTrigger.getParameters().iterator();
        assertTrue(params.hasNext());
        assertTriggerParameter(params.next(), "value.driver", "configuration.connection.driver");
        assertTriggerParameter(params.next(), "query.timeout", "configuration.query.timeout");
        assertTriggerParameter(params.next(), "query.sql", "configuration.query.sql");
    }

    @Test
    public void jsonSchema() throws Exception {
        final UiSpecPayload payload = service.convert(load("jdbc.json"));
        final UiSpecPayload.JsonSchema jsonSchema = payload.getJsonSchema();
        assertNotNull(jsonSchema);
        assertEquals("JDBC Input", jsonSchema.getTitle());
        assertEquals("object", jsonSchema.getType());

        assertEquals(1, jsonSchema.getProperties().size());
        assertProperty(jsonSchema.getProperties().get("configuration"), "object", "Configuration", p -> {
            final Map<String, UiSpecPayload.JsonSchema> nestedProperties = p.getProperties();
            assertEquals(2, nestedProperties.size());
            assertTrue(TreeMap.class.isInstance(nestedProperties));
            assertEquals(asList("connection", "query"), new ArrayList<>(nestedProperties.keySet()));
            assertProperty(nestedProperties.get("query"), "object", "query", q -> {
                final Map<String, UiSpecPayload.JsonSchema> queryNestedProperties = q.getProperties();
                assertEquals(2, queryNestedProperties.size());
                assertProperty(queryNestedProperties.get("query"), "string", "query",
                        query -> assertNull(query.getProperties()));
                assertProperty(queryNestedProperties.get("timeout"), "number", "timeout",
                        timeout -> assertNull(timeout.getProperties()));
            });
            assertProperty(nestedProperties.get("connection"), "object", "JDBC Connection", q -> {
                final Map<String, UiSpecPayload.JsonSchema> queryNestedProperties = q.getProperties();
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
    public void properties() throws Exception {
        final UiSpecPayload payload = service.convert(load("jdbc.json"));
        final Map<String, Object> properties = payload.getProperties();
        assertEquals(1, properties.size());

        final Map<String, Object> configuration = Map.class.cast(properties.get("configuration"));
        assertEquals(2, configuration.size());

        final Map<String, Object> connection = Map.class.cast(configuration.get("connection"));
        assertEquals(2, connection.size());

        assertEquals("", connection.get("password"));
        assertEquals("sa", connection.get("username"));
    }

    @Test
    public void uiSchema() throws Exception {
        final UiSpecPayload payload = service.convert(load("jdbc.json"));
        final Collection<UiSpecPayload.UiSchema> uiSchema = payload.getUiSchema();
        assertNotNull(uiSchema);
        assertEquals(1, uiSchema.size());
        assertUiSchema(uiSchema.iterator().next(), "fieldset", "Configuration", null, 3, schema -> {
            final List<UiSpecPayload.UiSchema> items = new ArrayList<>(schema.getItems());
            items.sort(Comparator.comparing(UiSpecPayload.UiSchema::getTitle));
            final Iterator<UiSpecPayload.UiSchema> it = items.iterator();
            assertUiSchema(it.next(), "button", "Guess Schema", "button_schema_configuration", 0, guessSchema -> {
                assertNotNull(guessSchema.getTriggers());
                assertEquals(1, guessSchema.getTriggers().size());
            });
            assertUiSchema(it.next(), "fieldset", "JDBC Connection", null, 5, connection -> {
                final List<UiSpecPayload.UiSchema> connectionItems = new ArrayList<>(connection.getItems());
                connectionItems.sort(Comparator.comparing(UiSpecPayload.UiSchema::getTitle));
                final Iterator<UiSpecPayload.UiSchema> connectionIt = connectionItems.iterator();
                assertUiSchema(connectionIt.next(), "button", "Validate Datastore",
                        "button_healthcheck_configuration.connection", 0, validateDataStore -> {
                            assertEquals(1, validateDataStore.getTriggers().size());
                            final UiSpecPayload.Trigger trigger = validateDataStore.getTriggers().iterator().next();
                            assertEquals(4, trigger.getParameters().size());
                            assertEquals(
                                    Stream
                                            .of("driver", "password", "url", "username")
                                            .map(p -> "configuration.connection." + p)
                                            .collect(toSet()),
                                    trigger
                                            .getParameters()
                                            .stream()
                                            .map(UiSpecPayload.Trigger.Parameter::getPath)
                                            .collect(toSet()));
                            assertEquals(
                                    Stream
                                            .of("driver", "password", "url", "username")
                                            .map(p -> "datastore." + p)
                                            .collect(toSet()),
                                    trigger
                                            .getParameters()
                                            .stream()
                                            .map(UiSpecPayload.Trigger.Parameter::getKey)
                                            .collect(toSet()));
                        });
                assertUiSchema(connectionIt.next(), "datalist", "driver", "configuration.connection.driver", 0,
                        driver -> {
                            assertNotNull(driver.getTriggers());
                            assertEquals(1, driver.getTriggers().size());
                            final Collection<UiSpecPayload.NameValue> titleMap = driver.getTitleMap();
                            assertEquals(1, titleMap.size());
                            final UiSpecPayload.NameValue firstTitleMap = titleMap.iterator().next();
                            assertEquals("some.driver.Jdbc", firstTitleMap.getName());
                            assertEquals("Jdbc driver", firstTitleMap.getValue());

                            final UiSpecPayload.Trigger trigger = driver.getTriggers().iterator().next();
                            assertEquals("driver", trigger.getAction());
                            assertEquals("jdbc", trigger.getFamily());
                            assertEquals("validation", trigger.getType());

                            assertEquals(1, trigger.getParameters().size());
                            final UiSpecPayload.Trigger.Parameter parameter = trigger.getParameters().iterator().next();
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
            assertUiSchema(it.next(), "fieldset", "query", null, 2, query -> {
                final List<UiSpecPayload.UiSchema> queryItems = new ArrayList<>(query.getItems());
                queryItems.sort(Comparator.comparing(UiSpecPayload.UiSchema::getKey));
                final Iterator<UiSpecPayload.UiSchema> queryIt = queryItems.iterator();
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
    @Ignore("debug test to log the produced model")
    public void out() throws Exception {
        final UiSpecPayload payload = service.convert(load("jdbc.json"));
        System.out.println(JsonbBuilder.create(new JsonbConfig().withFormatting(true)).toJson(payload));
    }

    private void assertUiSchema(final UiSpecPayload.UiSchema schema, final String widget, final String title,
            final String key, final int nestedSize, final Consumer<UiSpecPayload.UiSchema> customValidator) {
        assertEquals(schema.toString(), widget, schema.getWidget());
        assertEquals(title, schema.getTitle());
        assertEquals(key, schema.getKey());
        if (schema.getItems() == null) {
            assertEquals(nestedSize, 0);
        } else {
            assertEquals(nestedSize, schema.getItems().size());
        }
        customValidator.accept(schema);
    }

    private void assertProperty(final UiSpecPayload.JsonSchema property, final String type, final String title,
            final Consumer<UiSpecPayload.JsonSchema> customValidator) {
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

    private void assertTriggerParameter(final UiSpecPayload.Trigger.Parameter next, final String key,
            final String path) {
        assertEquals(key, next.getKey());
        assertEquals(path, next.getPath());
    }
}
