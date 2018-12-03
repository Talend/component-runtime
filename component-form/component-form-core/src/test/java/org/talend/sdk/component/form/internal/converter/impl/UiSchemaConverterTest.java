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
package org.talend.sdk.component.form.internal.converter.impl;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.CompletionStage;

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
                    emptyList()).convert(completedFuture(propertyContext)).toCompletableFuture().get();
        }
        assertEquals(1, schemas.size());
        final UiSchema configuration = schemas.iterator().next();
        assertEquals("configuration", configuration.getKey());
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
                    })).convert(completedFuture(propertyContext)).toCompletableFuture().get();
        }
        assertEquals(1, schemas.size());
        final UiSchema entrySchema = schemas.iterator().next();
        assertEquals("datalist", entrySchema.getWidget());
        assertEquals(1, entrySchema.getTitleMap().size());
        final UiSchema.NameValue proposal = entrySchema.getTitleMap().iterator().next();
        assertEquals("a", proposal.getName());
        assertEquals("A", proposal.getValue());
    }
}
