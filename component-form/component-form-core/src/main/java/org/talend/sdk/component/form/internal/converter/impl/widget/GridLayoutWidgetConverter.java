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
package org.talend.sdk.component.form.internal.converter.impl.widget;

import static java.util.Comparator.comparing;
import static java.util.Optional.ofNullable;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
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
            final Collection<CustomPropertyConverter> customPropertyConverters) {
        super(schemas, properties, actions, jsonSchema, lang);
        this.client = client;
        this.family = family;
        this.layouts = gridLayouts;
        this.customPropertyConverters = customPropertyConverters;
    }

    @Override
    public CompletionStage<PropertyContext<?>> convert(final CompletionStage<PropertyContext<?>> cs) {
        return cs.thenCompose(context -> {
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
                schema.setWidget("tabs");

                final List<UiSchema> resolvedLayouts = new ArrayList<>();
                return CompletableFuture
                        .allOf(tabs
                                .stream()
                                .map(tab -> ofNullable(layouts.get(tab))
                                        .map(layoutStr -> createLayout(context, layoutStr, tab).thenApply(layout -> {
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
                            schema.setItems(resolvedLayouts);
                            return context;
                        });
            }
        });
    }

    private CompletionStage<UiSchema> createLayout(final PropertyContext root, final String layout,
            final String layoutFilter) {
        final UiSchema uiSchema = newOrphanSchema(root);
        uiSchema.setItems(new ArrayList<>());

        final Collection<SimplePropertyDefinition> visitedProperties = new ArrayList<>();
        final Map<String, SimplePropertyDefinition> childProperties =
                properties.stream().filter(root::isDirectChild).collect(
                        toMap(SimplePropertyDefinition::getName, identity()));
        return CompletableFuture.allOf(Stream.of(layout.split("\\|")).map(line -> line.split(",")).map(line -> {
            if (line.length == 1 && childProperties.containsKey(line[0])) {
                return new UiSchemaConverter(layoutFilter, family, uiSchema.getItems(), visitedProperties, client,
                        jsonSchema, properties, actions, lang, customPropertyConverters)
                                .convert(completedFuture(new PropertyContext<>(childProperties.get(line[0]),
                                        root.getRootContext(), root.getConfiguration())))
                                .thenApply(r -> uiSchema);
            } else if (line.length > 1) {
                final UiSchema schema = new UiSchema();
                schema.setWidget("columns");
                schema.setItems(new ArrayList<>());

                final UiSchemaConverter columnConverter = new UiSchemaConverter(layoutFilter, family, schema.getItems(),
                        visitedProperties, client, jsonSchema, properties, actions, lang, customPropertyConverters);

                return CompletableFuture
                        .allOf(Stream
                                .of(line)
                                .map(String::trim)
                                .map(childProperties::get)
                                .filter(Objects::nonNull)
                                .map(it -> new PropertyContext<>(it, root.getRootContext(), root.getConfiguration()))
                                .map(CompletionStages::toStage)
                                .map(columnConverter::convert)
                                .toArray(CompletableFuture[]::new))
                        .thenApply(r -> {
                            final Collection<UiSchema> items = uiSchema.getItems();
                            synchronized (items) {
                                items.add(schema);
                            }
                            return uiSchema;
                        });
            }
            return completedFuture(null);
        }).toArray(CompletableFuture[]::new)).thenApply(done -> {
            addActions(root, uiSchema, visitedProperties);
            return uiSchema;
        });
    }
}
