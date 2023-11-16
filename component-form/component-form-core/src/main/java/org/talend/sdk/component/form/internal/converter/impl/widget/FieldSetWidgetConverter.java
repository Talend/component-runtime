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

import static java.util.Arrays.asList;
import static java.util.Comparator.comparing;
import static java.util.Optional.ofNullable;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;

import org.talend.sdk.component.form.api.Client;
import org.talend.sdk.component.form.internal.converter.CustomPropertyConverter;
import org.talend.sdk.component.form.internal.converter.PropertyContext;
import org.talend.sdk.component.form.internal.converter.impl.UiSchemaConverter;
import org.talend.sdk.component.form.model.jsonschema.JsonSchema;
import org.talend.sdk.component.form.model.uischema.UiSchema;
import org.talend.sdk.component.server.front.model.ActionReference;
import org.talend.sdk.component.server.front.model.SimplePropertyDefinition;

public class FieldSetWidgetConverter extends ObjectWidgetConverter {

    private final Client client;

    private final String family;

    private final List<String> order;

    private final Collection<CustomPropertyConverter> customPropertyConverters;

    private final UiSchema providedUiSchema;

    // CHECKSTYLE:OFF
    public FieldSetWidgetConverter(final Collection<UiSchema> schemas,
            final Collection<SimplePropertyDefinition> properties, final Collection<ActionReference> actions,
            final Client client, final String family, final JsonSchema jsonSchema, final String order,
            final String lang, final Collection<CustomPropertyConverter> customPropertyConverters,
            final UiSchema providedUiSchema, final AtomicInteger idGenerator) {
        // CHECKSTYLE:ON
        super(schemas, properties, actions, jsonSchema, lang, idGenerator);
        this.client = client;
        this.family = family;
        this.customPropertyConverters = customPropertyConverters;
        this.order = ofNullable(order).map(it -> asList(it.split(","))).orElse(null);
        this.providedUiSchema = providedUiSchema;
    }

    @Override
    public CompletionStage<PropertyContext<?>> convert(final CompletionStage<PropertyContext<?>> cs) {
        return cs.thenCompose(context -> {
            final UiSchema uiSchema;
            if (providedUiSchema == null) {
                uiSchema = newUiSchema(context);
                uiSchema.setWidget("fieldset");
                uiSchema.setItems(new ArrayList<>());
                uiSchema.setKey(null);
            } else {
                uiSchema = providedUiSchema;
            }

            final List<SimplePropertyDefinition> properties = new ArrayList<>();

            // Create Nested UI Items
            final List<SimplePropertyDefinition> sortedProperties =
                    context.findDirectChild(this.properties).collect(toList());
            if (order == null) {
                sortedProperties.sort(comparing(SimplePropertyDefinition::getPath));
            } else {
                sortedProperties.sort(comparing(it -> {
                    final int i = order.indexOf(it.getName());
                    if (i < 0) {
                        return Integer.MAX_VALUE;
                    }
                    return i;
                }));
            }
            final Collection<CompletionStage<ListItem>> futures = new ArrayList<>();
            for (int i = 0; i < sortedProperties.size(); i++) {
                final SimplePropertyDefinition definition = sortedProperties.get(i);
                final int index = i;
                final Collection<UiSchema> schemas = new ArrayList<>();
                final CompletableFuture<PropertyContext<?>> propContext = completedFuture(
                        new PropertyContext<>(definition, context.getRootContext(), context.getConfiguration()));
                final UiSchemaConverter converter = new UiSchemaConverter(null, family, schemas, properties, client,
                        jsonSchema, this.properties, actions, lang, customPropertyConverters, idGenerator);
                futures.add(converter.convert(propContext).thenApply(pc -> {
                    final ListItem item = new ListItem(index, new String[] { definition.getName() });
                    item.getUiSchemas().addAll(schemas);
                    return item;
                }));
            }
            return CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[0])).thenApply(done -> {
                ListItem.merge(futures, uiSchema);
                addActions(context, uiSchema, properties);
                return context;
            });
        });
    }
}
