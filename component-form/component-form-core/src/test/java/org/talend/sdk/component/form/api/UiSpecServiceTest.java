/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.talend.sdk.component.form.api;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
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
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;
import javax.json.bind.config.PropertyOrderStrategy;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.form.model.Ui;
import org.talend.sdk.component.form.model.jsonschema.JsonSchema;
import org.talend.sdk.component.form.model.uischema.UiSchema;
import org.talend.sdk.component.server.front.model.ComponentDetail;
import org.talend.sdk.component.server.front.model.ConfigTypeNode;
import org.talend.sdk.component.server.front.model.ConfigTypeNodes;
import org.talend.sdk.component.server.front.model.SimplePropertyDefinition;

import lombok.Data;

class UiSpecServiceTest {

    private final UiSpecService<Object> service = new UiSpecService<>(new Client<Object>() {

        @Override
        public CompletionStage<Map<String, Object>> action(final String family, final String type, final String action,
                final String lang, final Map<String, Object> params, final Object ignored) {
            if ("jdbc".equals(family) && "dynamic_values".equals(type) && "driver".equals(action)) {
                final Map<String, String> item = new HashMap<>();
                item.put("id", "some.driver.Jdbc");
                item.put("label", "Jdbc driver");
                return CompletableFuture.completedFuture(singletonMap("items", singleton(item)));
            }
            return CompletableFuture.completedFuture(params);
        }

        @Override
        public void close() {
            // no-op
        }
    });

    @Test // ensure the resolution works, even when an nested array item request a parent sibling parameter
    void paramResolutionToParent() throws Exception {
        final ConfigTypeNode node = load("param-resolution-to-parent.json", ConfigTypeNode.class);
        final Ui payload = service.convert("test", "en", node, null).toCompletableFuture().get();
        final List<UiSchema> rootItems = List.class.cast(payload.getUiSchema().iterator().next().getItems());
        final UiSchema.Trigger trigger = rootItems
                .stream()
                .filter(it -> "configuration.customObjectName".equals(it.getKey()))
                .findFirst()
                .orElseThrow(IllegalStateException::new)
                .getTriggers()
                .stream()
                .findFirst()
                .orElseThrow(IllegalStateException::new);
        assertEquals("CUSTOM_OBJECT_NAMES", trigger.getAction());
        assertEquals("Marketo", trigger.getFamily());
        assertEquals("suggestions", trigger.getType());
        assertEquals("focus", trigger.getOnEvent());
        assertEquals(1, trigger.getParameters().size());
        final UiSchema.Parameter parameter = trigger.getParameters().iterator().next();
        assertEquals("dataStore", parameter.getKey());
        assertEquals("configuration.dataSet.dataStore", parameter.getPath());
    }

    @Test
    void updatable() throws Exception {
        final ConfigTypeNode node = load("update.json", ConfigTypeNode.class);
        final Ui payload = service.convert("test", "en", node, null).toCompletableFuture().get();
        final Iterator<UiSchema> rootItems = payload.getUiSchema().iterator().next().getItems().iterator();
        rootItems.next(); // datastore
        final Iterator<UiSchema> updatableConfig = rootItems.next().getItems().iterator();
        updatableConfig.next(); // name
        final Collection<UiSchema.Trigger> updateTriggers = updatableConfig.next().getTriggers();
        assertEquals(1, updateTriggers.size());

        final UiSchema.Trigger trigger = updateTriggers.iterator().next();
        assertEquals("guessMe", trigger.getAction());
        assertEquals("test", trigger.getFamily());
        assertEquals("update", trigger.getType());

        assertEquals(1, trigger.getParameters().size());
        final UiSchema.Parameter parameter = trigger.getParameters().iterator().next();
        assertEquals("root.updatable_config", parameter.getPath());
        assertEquals("arg0", parameter.getKey());

        assertEquals(1, trigger.getOptions().size());
        final UiSchema.Option option = trigger.getOptions().iterator().next();
        assertEquals("root.updatable_config", option.getPath());
        assertEquals("object", option.getType());
    }

    @Test
    void conditionAnd() throws Exception {
        final ComponentDetail node = load("condition-and.json", ComponentDetail.class);
        final Ui payload = service.convert(node, "en", null).toCompletableFuture().get();
        final UiSchema schema = payload
                .getUiSchema()
                .iterator()
                .next()
                .getItems()
                .iterator()
                .next()
                .getItems()
                .stream()
                .filter(it -> "conf.activeIfAnd".equals(it.getKey()))
                .findFirst()
                .orElseThrow(NoSuchElementException::new);
        final Map<String, Collection<Object>> condition = schema.getCondition();
        final Collection<Object> and = condition.get("and");
        and.forEach(it -> assertFalse(Collection.class.isInstance(it)));
        final Map<String, Collection<Object>> firstCond = Map.class.cast(and.iterator().next());
        assertEquals(asList(singletonMap("var", "conf.str"), "value"), firstCond.get("==="));
    }

    @Test
    void updatableAfter() throws Exception {
        final ConfigTypeNode node = load("update_after.json", ConfigTypeNode.class);
        final Ui payload = service.convert("test", "en", node, null).toCompletableFuture().get();
        final Iterator<UiSchema> rootItems = payload.getUiSchema().iterator().next().getItems().iterator();
        rootItems.next(); // datastore
        final Iterator<UiSchema> updatableConfig = rootItems.next().getItems().iterator();
        final Iterator<UiSchema> wrapper = updatableConfig.next().getItems().iterator().next().getItems().iterator();
        assertEquals("Id", wrapper.next().getTitle());
        final Collection<UiSchema.Trigger> updateTriggers = wrapper.next().getTriggers();
        assertEquals(1, updateTriggers.size());

        final UiSchema.Trigger trigger = updateTriggers.iterator().next();
        assertEquals("guessMe", trigger.getAction());
        assertEquals("test", trigger.getFamily());
        assertEquals("update", trigger.getType());
        assertEquals(1, trigger.getParameters().size());
        assertEquals(1, trigger.getOptions().size());
    }

    @Test
    void optionsOrderInArray() throws Exception {
        final ConfigTypeNode node =
                load("config.json", ConfigTypeNodes.class).getNodes().get("U2VydmljZU5vdyNkYXRhc2V0I3RhYmxl");
        final Ui payload = service.convert("Jdbc", "en", node, null).toCompletableFuture().get();
        final Iterator<UiSchema> items =
                payload.getUiSchema().iterator().next().getItems().iterator().next().getItems().iterator();
        items.next();
        items.next();
        items.next();
        assertEquals(asList("Order", "Field"),
                items
                        .next()
                        .getItems()
                        .iterator()
                        .next()
                        .getItems()
                        .iterator()
                        .next()
                        .getItems()
                        .stream()
                        .map(UiSchema::getTitle)
                        .collect(toList()));
    }

    @Test
    void enumAsRestrictedList() throws Exception {
        final ConfigTypeNode node =
                load("config.json", ConfigTypeNodes.class).getNodes().get("U2VydmljZU5vdyNkYXRhc2V0I3RhYmxl");
        final Ui payload = service.convert("Jdbc", "en", node, null).toCompletableFuture().get();
        final Iterator<UiSchema> items =
                payload.getUiSchema().iterator().next().getItems().iterator().next().getItems().iterator();
        items.next();
        items.next();
        items.next();
        // grab order and fields which are enums
        final Collection<UiSchema> enumItems =
                items.next().getItems().iterator().next().getItems().iterator().next().getItems();
        assertEquals(2, enumItems.size());
        enumItems.forEach(it -> assertTrue(it.getRestricted()));
    }

    @Test
    void optionsOrder() throws Exception {
        final ConfigTypeNode node = load("optionsorder.json", ConfigTypeNode.class);
        final SimplePropertyDefinition root = node
                .getProperties()
                .stream()
                .filter(it -> it.getPath().equals("configuration"))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("bad config"));
        final String[] expectedOrder = root.getMetadata().get("ui::optionsorder::value").split(",");
        final Ui payload = service.convert("FileIO", "en", node, null).toCompletableFuture().get();
        final List<String> actualOrder = payload
                .getUiSchema()
                .iterator()
                .next()
                .getItems()
                .stream()
                .map(UiSchema::getKey)
                .map(s -> s.substring(s.lastIndexOf('.') + 1))
                .collect(toList());
        assertEquals(asList(expectedOrder), actualOrder);
    }

    @Test
    void proposablePrimitives() throws Exception {
        final ComponentDetail detail = load("multiSelectTagString.json", ComponentDetail.class);
        final Ui payload = service.convert(detail, "en", null).toCompletableFuture().get();
        final UiSchema proposable =
                ((List<UiSchema>) payload.getUiSchema().iterator().next().getItems().iterator().next().getItems())
                        .get(1);
        assertEquals("configuration.vendor", proposable.getKey());
        assertEquals("datalist", proposable.getWidget());
    }

    @Test
    void configuration() throws Exception {
        final ConfigTypeNodes load = load("config.json", ConfigTypeNodes.class);
        final String family = load
                .getNodes()
                .entrySet()
                .stream()
                .filter(e -> e.getValue().getParentId() == null)
                .map(e -> e.getValue().getId())
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No family"));
        final Ui payload = service
                .convert(family, "en",
                        load
                                .getNodes()
                                .values()
                                .stream()
                                .filter(e -> "U2VydmljZU5vdyNkYXRhc3RvcmUjYmFzaWNBdXRo".equals(e.getId()))
                                .findFirst()
                                .orElseThrow(() -> new IllegalArgumentException(
                                        "No U2VydmljZU5vdyNkYXRhc3RvcmUjYmFzaWNBdXRo config")),
                        null)
                .toCompletableFuture()
                .get();
        assertTrue(payload.getJsonSchema().getProperties().containsKey("dataStore"), "dataStore");
        assertEquals(3, payload.getUiSchema().iterator().next().getItems().size());

        final Set<String> uiSchemaKeys = flattenUiSchema(payload.getUiSchema().stream())
                .map(UiSchema::getKey)
                .filter(it -> Stream
                        .of("dataStore", "button_healthcheck_dataStore")
                        .noneMatch(ignored -> ignored.equals(it)))
                .filter(Objects::nonNull)
                .collect(toSet());
        final Set<String> jsonSchemaKeys = flattenJsonSchema(Stream.of(new Pair("", payload.getJsonSchema())))
                .map(it -> it.prefix)
                .filter(it -> Stream.of("", "dataStore").noneMatch(ignored -> ignored.equals(it)))
                .collect(toSet());
        assertEquals(uiSchemaKeys, jsonSchemaKeys);
    }

    @Test
    void condition() throws Exception {
        final Ui payload = service.convert(load("rest-api.json"), "en", null).toCompletableFuture().get();
        final UiSchema root = payload.getUiSchema().iterator().next();
        final Map<String, Collection<Object>> condition = root
                .getItems()
                .stream()
                .filter(i -> i.getTitle().equals("Main"))
                .findFirst()
                .get()
                .getItems()
                .stream()
                .filter(i -> i.getTitle().equals("Order"))
                .findFirst()
                .get()
                .getCondition();
        assertNotNull(condition);
        assertEquals(1, condition.size());

        final Map.Entry<String, Collection<Object>> entry = condition.entrySet().iterator().next();
        assertEquals("===", entry.getKey());
        assertEquals(2, entry.getValue().size());

        final Iterator<Object> values = entry.getValue().iterator();
        assertEquals(singletonMap("var", "tableDataSet.ordered"), values.next());
        assertEquals(true, values.next());

        // serialization
        try (final Jsonb jsonb = JsonbBuilder
                .create(new JsonbConfig().withPropertyOrderStrategy(PropertyOrderStrategy.LEXICOGRAPHICAL))) {
            assertEquals("{\"===\":[{\"var\":\"tableDataSet.ordered\"},true]}", jsonb.toJson(condition));
        }
    }

    @Test
    void guessSchema() throws Exception {
        final Ui payload = service.convert(load("rest-api.json"), "en", null).toCompletableFuture().get();
        final UiSchema root = payload.getUiSchema().iterator().next();
        final UiSchema advanced =
                root.getItems().stream().filter(i -> i.getTitle().equals("Advanced")).findFirst().get();
        final UiSchema commongConfig =
                advanced.getItems().stream().filter(i -> i.getTitle().equals("commonConfig")).findFirst().get();
        assertTrue(commongConfig.getItems().stream().anyMatch(i -> i.getWidget().equals("button")));
    }

    @Test
    void jsonSchemaArray() throws Exception {
        final Ui payload = service.convert(load("rest-api.json"), "en", null).toCompletableFuture().get();
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
        final Ui payload = service.convert(load("rest-api.json"), "en", null).toCompletableFuture().get();
        final UiSchema tableDataSet = payload.getUiSchema().iterator().next();
        assertEquals(2, tableDataSet.getItems().size());
        final Iterator<UiSchema> tableDataSetIt = tableDataSet.getItems().iterator();
        final UiSchema tableDataSetMain = tableDataSetIt.next();
        assertEquals("Main", tableDataSetMain.getTitle());

        assertEquals(5, tableDataSetMain.getItems().size());
        assertEquals(asList("dataStore", "commonConfig", "Query", "Ordered", "Order"),
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
    void defaultValues() throws Exception {
        final Ui payload = service.convert(load("rest-api.json"), "en", null).toCompletableFuture().get();
        assertEquals(10000., read(payload.getProperties(), "tableDataSet.limit"));
    }

    @Test
    void triggerRelativeParameters() throws Exception {
        final Ui payload = service.convert(load("relative-params.json"), "en", null).toCompletableFuture().get();
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
        assertEquals(2, driverTrigger.getParameters().size(), driverTrigger.toString());

        final Iterator<UiSchema.Parameter> params = driverTrigger.getParameters().iterator();
        assertTrue(params.hasNext());
        assertTriggerParameter(params.next(), "value", "configuration.connection");
        assertTrue(params.hasNext());
        assertTriggerParameter(params.next(), "query", "configuration.query");
    }

    @Test
    void suggestions() throws Exception {
        final Ui payload = service
                .convert("jdbc", "en", load("suggestions.json", ConfigTypeNode.class), null)
                .toCompletableFuture()
                .get();
        final Collection<UiSchema> schema = payload.getUiSchema();
        final UiSchema.Trigger driverTrigger = schema
                .iterator()
                .next()
                .getItems()
                .stream()
                .filter(c -> c.getKey().equals("configuration.driver"))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No driver"))
                .getTriggers()
                .iterator()
                .next();
        assertEquals("focus", driverTrigger.getOnEvent());
        assertEquals("suggestions", driverTrigger.getType());
        assertEquals("SuggestionForJdbcDrivers", driverTrigger.getAction());
        assertEquals(singletonList("currentValue/configuration.driver"),
                driverTrigger.getParameters().stream().map(it -> it.getKey() + '/' + it.getPath()).collect(toList()));
    }

    @Test
    void jsonSchema() throws Exception {
        final Ui payload = service.convert(load("jdbc.json"), "en", null).toCompletableFuture().get();
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
        final Ui payload = service.convert(load("jdbc.json"), "en", null).toCompletableFuture().get();
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
        final Ui payload = service.convert(load("jdbc.json"), "en", null).toCompletableFuture().get();
        final Collection<UiSchema> uiSchema = payload.getUiSchema();
        assertNotNull(uiSchema);
        assertEquals(1, uiSchema.size());
        assertUiSchema(uiSchema.iterator().next(), "fieldset", "Configuration", "configuration", 2, schema -> {
            final List<UiSchema> items = new ArrayList<>(schema.getItems());
            items.sort(Comparator.comparing(UiSchema::getTitle));
            final Iterator<UiSchema> it = items.iterator();
            assertUiSchema(it.next(), "fieldset", "JDBC Connection", "configuration.connection", 5, connection -> {
                final List<UiSchema> connectionItems = new ArrayList<>(connection.getItems());
                connectionItems.sort(Comparator.comparing(UiSchema::getTitle));
                final Iterator<UiSchema> connectionIt = connectionItems.iterator();
                assertUiSchema(connectionIt.next(), "button", "Validate Connection", null, 0, validateDataStore -> {
                    assertEquals(1, validateDataStore.getTriggers().size());
                    final UiSchema.Trigger trigger = validateDataStore.getTriggers().iterator().next();
                    assertEquals(1, trigger.getParameters().size());
                    assertEquals(singleton("configuration.connection"),
                            trigger.getParameters().stream().map(UiSchema.Parameter::getPath).collect(toSet()));
                    assertEquals(singleton("datastore"),
                            trigger.getParameters().stream().map(UiSchema.Parameter::getKey).collect(toSet()));
                });
                assertUiSchema(connectionIt.next(), "datalist", "driver", "configuration.connection.driver", 0,
                        driver -> {
                            assertNotNull(driver.getTriggers());
                            assertEquals(1, driver.getTriggers().size());
                            final Collection<UiSchema.NameValue> titleMap = driver.getTitleMap();
                            assertEquals(1, titleMap.size());
                            final UiSchema.NameValue firstTitleMap = titleMap.iterator().next();
                            assertEquals("some.driver.Jdbc", firstTitleMap.getValue());
                            assertEquals("Jdbc driver", firstTitleMap.getName());

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
        final Ui payload = service.convert(load("jdbc.json"), "en", null).toCompletableFuture().get();
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

    private <T> T load(final String resource, final Class<T> type) throws Exception {
        try (final Jsonb jsonb = JsonbBuilder.create();
                final InputStream stream =
                        Thread.currentThread().getContextClassLoader().getResourceAsStream(resource)) {
            return jsonb.fromJson(stream, type);
        }
    }

    private void assertTriggerParameter(final UiSchema.Parameter next, final String key, final String path) {
        assertEquals(key, next.getKey());
        assertEquals(path, next.getPath());
    }

    private Object read(final Object rootMap, final String path) {
        Map<String, ?> current = Map.class.cast(rootMap);
        final String[] split = path.split("\\.");
        for (int i = 0; i < split.length - 1; i++) {
            current = Map.class.cast(current.get(split[i]));
        }
        return current.get(split[split.length - 1]);
    }

    private Stream<UiSchema> flattenUiSchema(final Stream<UiSchema> uiSchema) {
        return uiSchema
                .flatMap(u -> u.getItems() == null ? Stream.of(u)
                        : Stream.concat(Stream.of(u), flattenUiSchema(u.getItems().stream())));
    }

    private Stream<Pair> flattenJsonSchema(final Stream<Pair> jsonSchemaStream) {
        return jsonSchemaStream
                .flatMap(u -> u.schema.getProperties() == null ? Stream.of(u)
                        : Stream
                                .concat(Stream.of(u),
                                        flattenJsonSchema(u.schema
                                                .getProperties()
                                                .entrySet()
                                                .stream()
                                                .map(it -> new Pair(
                                                        u.prefix + (u.prefix.isEmpty() ? "" : ".") + it.getKey(),
                                                        it.getValue())))));
    }

    @Data
    private static class Pair {

        private final String prefix;

        private final JsonSchema schema;
    }
}
