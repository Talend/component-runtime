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

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.form.internal.converter.CustomPropertyConverter;
import org.talend.sdk.component.form.internal.converter.PropertyContext;
import org.talend.sdk.component.form.internal.converter.impl.widget.DataListWidgetConverter;
import org.talend.sdk.component.form.model.jsonschema.JsonSchema;
import org.talend.sdk.component.form.model.uischema.UiSchema;
import org.talend.sdk.component.server.front.model.PropertyValidation;
import org.talend.sdk.component.server.front.model.SimplePropertyDefinition;

class UiSchemaConverterTest {

    private static final PropertyValidation NVAL = new PropertyValidation();

    private static final LinkedHashMap<String, String> NPROPS = new LinkedHashMap<>();

    @Test
    void listOfObject() throws Exception {
        final List<SimplePropertyDefinition> properties = asList(
                new SimplePropertyDefinition("configuration", "configuration", "configuration", "OBJECT", null, NVAL,
                        emptyMap(), null, NPROPS),
                new SimplePropertyDefinition("configuration.list", "list", "list", "ARRAY", null, NVAL, emptyMap(),
                        null, NPROPS),
                new SimplePropertyDefinition("configuration.list[].name", "name", "name", "STRING", null, NVAL,
                        emptyMap(), null, NPROPS));
        final List<UiSchema> schemas = getUiSchemas(properties);
        assertEquals(1, schemas.size());
        final UiSchema configuration = schemas.iterator().next();
        assertNull(configuration.getKey());
        assertEquals(1, configuration.getItems().size());
        final UiSchema list = configuration.getItems().iterator().next();
        assertEquals("configuration.list", list.getKey());
        assertEquals("collapsibleFieldset", list.getItemWidget());
        assertEquals(1, list.getItems().size());
        final UiSchema name = list.getItems().iterator().next();
        assertEquals("configuration.list[].name", name.getKey());
        assertEquals("text", name.getWidget());
    }

    @Test
    void moduleListWithBeanList() throws Exception {
        final Map<String, String> metadata = new HashMap<>();
        metadata.put("ui::modulelist", "true");
        metadata.put("ui::readonly", "true");
        final List<SimplePropertyDefinition> properties = asList(
                new SimplePropertyDefinition("configuration", "configuration", "configuration", "OBJECT", null, NVAL,
                        emptyMap(), null, NPROPS),
                new SimplePropertyDefinition("configuration.drivers", "drivers", "drivers", "ARRAY", null, NVAL,
                        emptyMap(),
                        null, NPROPS),
                new SimplePropertyDefinition("configuration.drivers[].path", "path", "path", "STRING", null, NVAL,
                        metadata, null, NPROPS));
        final List<UiSchema> schemas = getUiSchemas(properties);
        assertEquals(1, schemas.size());
        final UiSchema configuration = schemas.iterator().next();
        assertNull(configuration.getKey());
        assertNull(configuration.getReadOnly());
        assertEquals(1, configuration.getItems().size());
        final UiSchema list = configuration.getItems().iterator().next();
        assertNull(list.getReadOnly());
        assertEquals("configuration.drivers", list.getKey());
        assertEquals("collapsibleFieldset", list.getItemWidget());
        assertEquals(1, list.getItems().size());
        final UiSchema name = list.getItems().iterator().next();
        assertEquals("configuration.drivers[].path", name.getKey());
        assertTrue(name.getReadOnly());
        // use text type in react ui form for the item, it mean ignore @ModuleList which only works for studio now
        assertEquals("text", name.getWidget());
    }

    private List<UiSchema> getUiSchemas(List<SimplePropertyDefinition> properties) throws Exception {
        final PropertyContext<Object> propertyContext =
                new PropertyContext<>(properties.iterator().next(), null, new PropertyContext.Configuration(false));
        final List<UiSchema> schemas = new ArrayList<>();
        try (final Jsonb jsonb = JsonbBuilder.create()) {
            final JsonSchema jsonSchema = new JsonSchema();
            new JsonSchemaConverter(jsonb, jsonSchema, properties)
                    .convert(completedFuture(propertyContext))
                    .toCompletableFuture()
                    .get();
            new UiSchemaConverter(null, "test", schemas, properties, null, jsonSchema, properties, emptyList(), "en",
                    emptyList(), new AtomicInteger(1))
                            .convert(completedFuture(propertyContext))
                            .toCompletableFuture()
                            .get();
        }
        return schemas;
    }

    @Test
    void customConverter() throws Exception {
        final List<SimplePropertyDefinition> properties = singletonList(new SimplePropertyDefinition("entry", "entry",
                "Entry", "String", null, NVAL, emptyMap(), null, NPROPS));
        final PropertyContext<Object> propertyContext =
                new PropertyContext<>(properties.iterator().next(), null, new PropertyContext.Configuration(false));
        final List<UiSchema> schemas = new ArrayList<>();
        try (final Jsonb jsonb = JsonbBuilder.create()) {
            final JsonSchema jsonSchema = new JsonSchema();
            new JsonSchemaConverter(jsonb, jsonSchema, properties)
                    .convert(completedFuture(propertyContext))
                    .toCompletableFuture()
                    .get();
            new UiSchemaConverter(null, "test", schemas, properties, null, jsonSchema, properties, emptyList(), "en",
                    singletonList(new CustomPropertyConverter() {

                        @Override
                        public boolean supports(final PropertyContext<?> context) {
                            return context.getProperty().getName().equals("entry");
                        }

                        @Override
                        public CompletionStage<PropertyContext<?>> convert(
                                final CompletionStage<PropertyContext<?>> context,
                                final ConverterContext converterContext) {
                            return new DataListWidgetConverter(converterContext.getSchemas(),
                                    converterContext.getProperties(), converterContext.getActions(),
                                    converterContext.getClient(), converterContext.getFamily(),
                                    converterContext.getJsonSchema(), converterContext.getLang()) {

                                @Override
                                protected CompletionStage<PropertyContext<?>> fillProposalsAndReturn(
                                        final PropertyContext<?> context, final UiSchema schema,
                                        final JsonSchema jsonSchema) {
                                    schema
                                            .setTitleMap(singletonList(new UiSchema.NameValue.Builder()
                                                    .withName("a")
                                                    .withValue("A")
                                                    .build()));
                                    jsonSchema.setEnumValues(singletonList("A"));
                                    return completedFuture(context);
                                }
                            }.convert(context);
                        }
                    }), new AtomicInteger(1)).convert(completedFuture(propertyContext)).toCompletableFuture().get();
        }
        assertEquals(1, schemas.size());
        final UiSchema entrySchema = schemas.iterator().next();
        assertEquals("datalist", entrySchema.getWidget());
        assertEquals(1, entrySchema.getTitleMap().size());
        final UiSchema.NameValue proposal = (UiSchema.NameValue) entrySchema.getTitleMap().iterator().next();
        assertEquals("a", proposal.getName());
        assertEquals("A", proposal.getValue());
    }

    @Test
    void customConverterWithTitleNamedValueTitleMap() throws Exception {
        final List<SimplePropertyDefinition> properties = singletonList(new SimplePropertyDefinition("entry", "entry",
                "Entry", "String", null, NVAL, emptyMap(), null, NPROPS));
        final PropertyContext<Object> propertyContext =
                new PropertyContext<>(properties.iterator().next(), null, new PropertyContext.Configuration(false));
        final List<UiSchema> schemas = new ArrayList<>();
        try (final Jsonb jsonb = JsonbBuilder.create()) {
            final JsonSchema jsonSchema = new JsonSchema();
            new JsonSchemaConverter(jsonb, jsonSchema, properties)
                    .convert(completedFuture(propertyContext))
                    .toCompletableFuture()
                    .get();
            new UiSchemaConverter(null, "test", schemas, properties, null, jsonSchema, properties, emptyList(), "en",
                    singletonList(new CustomPropertyConverter() {

                        @Override
                        public boolean supports(final PropertyContext<?> context) {
                            return context.getProperty().getName().equals("entry");
                        }

                        @Override
                        public CompletionStage<PropertyContext<?>> convert(
                                final CompletionStage<PropertyContext<?>> context,
                                final ConverterContext converterContext) {
                            return new DataListWidgetConverter(converterContext.getSchemas(),
                                    converterContext.getProperties(), converterContext.getActions(),
                                    converterContext.getClient(), converterContext.getFamily(),
                                    converterContext.getJsonSchema(), converterContext.getLang()) {

                                @Override
                                protected CompletionStage<PropertyContext<?>> fillProposalsAndReturn(
                                        final PropertyContext<?> context, final UiSchema schema,
                                        final JsonSchema jsonSchema) {
                                    List<UiSchema.TitledNameValue> titleMapContent = new ArrayList<>();
                                    List<UiSchema.NameValue> level1 = new ArrayList<>();
                                    List<UiSchema.NameValue> level2 = new ArrayList<>();
                                    level1.add(new UiSchema.NameValue.Builder().withName("a1").withValue("A1").build());
                                    level1.add(new UiSchema.NameValue.Builder().withName("b1").withValue("B1").build());
                                    level2.add(new UiSchema.NameValue.Builder().withName("a2").withValue("A2").build());
                                    level2.add(new UiSchema.NameValue.Builder().withName("b2").withValue("B2").build());
                                    level2.add(new UiSchema.NameValue.Builder().withName("c2").withValue("C2").build());
                                    titleMapContent
                                            .add(new UiSchema.TitledNameValue.Builder()
                                                    .withTitle("title1")
                                                    .withSuggestions(level1)
                                                    .build());
                                    titleMapContent
                                            .add(new UiSchema.TitledNameValue.Builder()
                                                    .withTitle("title2")
                                                    .withSuggestions(level2)
                                                    .build());
                                    schema.setTitleMap(titleMapContent);
                                    List<String> enumValues = new ArrayList<>();
                                    enumValues.add("A1");
                                    enumValues.add("B1");
                                    enumValues.add("A2");
                                    enumValues.add("B2");
                                    enumValues.add("C2");
                                    jsonSchema.setEnumValues(enumValues);
                                    return completedFuture(context);
                                }
                            }.convert(context);
                        }
                    }), new AtomicInteger(1)).convert(completedFuture(propertyContext)).toCompletableFuture().get();
        }
        assertEquals(1, schemas.size());
        final UiSchema entrySchema = schemas.iterator().next();
        assertEquals("datalist", entrySchema.getWidget());
        assertEquals(2, entrySchema.getTitleMap().size());
        Iterator<? extends UiSchema.TitleMapContent> titleMapContent = entrySchema.getTitleMap().iterator();
        UiSchema.TitledNameValue proposal = (UiSchema.TitledNameValue) titleMapContent.next();
        assertEquals("title1", proposal.getTitle());
        assertEquals(2, proposal.getSuggestions().size());
        proposal = (UiSchema.TitledNameValue) titleMapContent.next();
        assertEquals("title2", proposal.getTitle());
        assertEquals(3, proposal.getSuggestions().size());
    }
}
