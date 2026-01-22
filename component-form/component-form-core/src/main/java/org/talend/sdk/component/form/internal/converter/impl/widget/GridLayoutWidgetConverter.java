/**
 * Copyright (C) 2006-2026 Talend Inc. - www.talend.com
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

import static java.util.Comparator.comparing;
import static java.util.Optional.ofNullable;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.talend.sdk.component.form.internal.lang.CompletionStages.toStage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.talend.sdk.component.form.api.Client;
import org.talend.sdk.component.form.internal.converter.CustomPropertyConverter;
import org.talend.sdk.component.form.internal.converter.PropertyContext;
import org.talend.sdk.component.form.internal.converter.impl.UiSchemaConverter;
import org.talend.sdk.component.form.internal.lang.CompletionStages;
import org.talend.sdk.component.form.model.jsonschema.JsonSchema;
import org.talend.sdk.component.form.model.uischema.UiSchema;
import org.talend.sdk.component.server.front.model.ActionReference;
import org.talend.sdk.component.server.front.model.SimplePropertyDefinition;

public class GridLayoutWidgetConverter extends ObjectWidgetConverter {

    private final Client client;

    private final String family;

    private final Map<String, String> layouts;

    private final Collection<CustomPropertyConverter> customPropertyConverters;

    public GridLayoutWidgetConverter(final Collection<UiSchema> schemas,
            final Collection<SimplePropertyDefinition> properties, final Collection<ActionReference> actions,
            final Client client, final String family, final Map<String, String> gridLayouts,
            final JsonSchema jsonSchema, final String lang,
            final Collection<CustomPropertyConverter> customPropertyConverters, final AtomicInteger idGenerator) {
        super(schemas, properties, actions, jsonSchema, lang, idGenerator);
        this.client = client;
        this.family = family;
        this.layouts = gridLayouts;
        this.customPropertyConverters = customPropertyConverters;
    }

    @Override
    public CompletionStage<PropertyContext<?>> convert(final CompletionStage<PropertyContext<?>> cs) {
        return cs
                .thenCompose(context -> {
                    // if we have a single tab we don't wrap the forms in tabs otherwise we do
                    if (layouts.size() == 1) {
                        final Map.Entry<String, String> first = layouts.entrySet().iterator().next();
                        return createLayout(context, first.getValue(), first.getKey()).thenApply(uiSchema -> {
                            synchronized (schemas) {
                                schemas.add(uiSchema);
                            }
                            return context;
                        });
                    } else {
                        // if we have multiple tabs, priority is MAIN/ADVANCED pair first
                        // but if they are not present then we use all layouts in "String" order
                        final List<String> tabs = (layouts.containsKey("Main") ? Stream.of("Main", "Advanced")
                                : layouts.keySet().stream().sorted(String::compareToIgnoreCase)).collect(toList());

                        final UiSchema schema = newUiSchema(context);
                        schema.setTitle(null);
                        schema.setKey(null);
                        schema.setWidget("tabs");

                        final List<UiSchema> resolvedLayouts = new ArrayList<>();
                        return CompletableFuture
                                .allOf(tabs
                                        .stream()
                                        .map(tab -> ofNullable(layouts.get(tab))
                                                .map(layoutStr -> createLayout(context, layoutStr, tab)
                                                        .thenApply(layout -> {
                                                            layout.setTitle(tab);
                                                            synchronized (resolvedLayouts) {
                                                                resolvedLayouts.add(layout);
                                                            }
                                                            return layout;
                                                        }))
                                                .orElse(null))
                                        .filter(Objects::nonNull)
                                        .toArray(CompletableFuture[]::new))
                                .thenApply(done -> {
                                    resolvedLayouts.sort(comparing(s -> tabs.indexOf(s.getTitle())));
                                    schema
                                            .setItems(resolvedLayouts
                                                    .stream()
                                                    .filter(it -> it.getItems() != null && !it.getItems().isEmpty())
                                                    .collect(toList()));
                                    return context;
                                });
                    }
                });
    }

    private CompletionStage<UiSchema> createLayout(final PropertyContext<?> root, final String layout,
            final String layoutFilter) {
        final UiSchema uiSchema = newOrphanSchema(root);
        uiSchema.setItems(new ArrayList<>());

        final Collection<SimplePropertyDefinition> visitedProperties = new ArrayList<>();
        final Map<String, SimplePropertyDefinition> childProperties =
                root.findDirectChild(properties).collect(toMap(SimplePropertyDefinition::getName, identity()));
        final String[] lines = layout.split("\\|");
        final Collection<CompletionStage<ListItem>> futures = new ArrayList<>();
        for (int i = 0; i < lines.length; i++) {
            final ListItem line = new ListItem(i, lines[i].split(","));
            futures.add(mapToFuture(root, layoutFilter, visitedProperties, childProperties, line));
        }
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[0])).thenApply(done -> {
            ListItem.merge(futures, uiSchema);
            addActions(root, uiSchema, visitedProperties);
            // remove key to container object
            if ("OBJECT".equals(root.getProperty().getType()) && uiSchema.getWidget() == null) {
                uiSchema.setKey(null);
                uiSchema.setWidget("fieldset");
            }
            return uiSchema;
        });
    }

    private CompletionStage<ListItem> mapToFuture(final PropertyContext<?> root, final String layoutFilter,
            final Collection<SimplePropertyDefinition> visitedProperties,
            final Map<String, SimplePropertyDefinition> childProperties, final ListItem line) {
        if (line.getItems().length == 1 && childProperties.containsKey(line.getItems()[0])) {
            return new UiSchemaConverter(layoutFilter, family, line.getUiSchemas(), visitedProperties, client,
                    jsonSchema, properties, actions, lang, customPropertyConverters, idGenerator)
                    .convert(completedFuture(new PropertyContext<>(childProperties.get(line.getItems()[0]),
                            root.getRootContext(), root.getConfiguration())))
                    .thenApply(r -> line);
        } else if (line.getItems().length > 1) {
            final UiSchema schema = new UiSchema();
            schema.setWidget("columns");
            schema.setItems(new ArrayList<>());
            line.getUiSchemas().add(schema);

            final Collection<CompletableFuture<ListItem>> futures = new ArrayList<>();
            for (int i = 0; i < line.getItems().length; i++) {
                final ListItem item = new ListItem(i, new String[] { line.getItems()[i].trim() });
                final SimplePropertyDefinition property = childProperties.get(item.getItems()[0]);
                if (property == null) {
                    continue;
                }
                final PropertyContext<?> context =
                        new PropertyContext<>(property, root.getRootContext(), root.getConfiguration());
                final List<UiSchema> schemas = new ArrayList<>();
                final UiSchemaConverter columnConverter =
                        new UiSchemaConverter(layoutFilter, family, schemas, visitedProperties, client, jsonSchema,
                                properties, actions, lang, customPropertyConverters, idGenerator);
                final CompletableFuture<ListItem> converted =
                        columnConverter.convert(toStage(context)).thenApply(output -> {
                            item.getUiSchemas().addAll(schemas);
                            return item;
                        }).toCompletableFuture();
                futures.add(converted);
            }
            return CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[0])).thenApply(r -> {
                futures
                        .stream()
                        .map(CompletionStages::get)
                        .sorted(comparing(ListItem::getIndex))
                        .forEach(it -> schema.getItems().addAll(it.getUiSchemas()));
                return line;
            });
        }
        return completedFuture(line);
    }
}
