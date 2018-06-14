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

import static java.util.Arrays.asList;
import static java.util.Comparator.comparing;
import static java.util.Optional.ofNullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Stream;

import org.talend.sdk.component.form.api.Client;
import org.talend.sdk.component.form.internal.converter.PropertyContext;
import org.talend.sdk.component.form.internal.converter.impl.UiSchemaConverter;
import org.talend.sdk.component.form.internal.lang.CompletionStages;
import org.talend.sdk.component.form.model.jsonschema.JsonSchema;
import org.talend.sdk.component.form.model.uischema.UiSchema;
import org.talend.sdk.component.server.front.model.ActionReference;
import org.talend.sdk.component.server.front.model.SimplePropertyDefinition;

public class FieldSetWidgetConverter extends ObjectWidgetConverter {

    private final Client client;

    private final String family;

    private final List<String> order;

    public FieldSetWidgetConverter(final Collection<UiSchema> schemas,
            final Collection<SimplePropertyDefinition> properties, final Collection<ActionReference> actions,
            final Client client, final String family, final JsonSchema jsonSchema, final String order,
            final String lang) {
        super(schemas, properties, actions, jsonSchema, lang);
        this.client = client;
        this.family = family;
        this.order = ofNullable(order).map(it -> asList(it.split(","))).orElse(null);
    }

    @Override
    public CompletionStage<PropertyContext<?>> convert(final CompletionStage<PropertyContext<?>> cs) {
        return cs.thenCompose(context -> {
            final UiSchema uiSchema = newUiSchema(context);
            uiSchema.setWidget("fieldset");
            uiSchema.setItems(new ArrayList<>());

            final List<SimplePropertyDefinition> properties = new ArrayList<>();
            final UiSchemaConverter uiSchemaConverter = new UiSchemaConverter(null, family, uiSchema.getItems(),
                    properties, client, jsonSchema, this.properties, actions, lang);

            // Create Nested UI Items
            final Stream<SimplePropertyDefinition> nestedProperties =
                    this.properties.stream().filter(context::isDirectChild);
            final Stream<SimplePropertyDefinition> sortedProperties =
                    order == null ? nestedProperties.sorted(comparing(SimplePropertyDefinition::getPath))
                            : nestedProperties.sorted(comparing(it -> order.indexOf(it.getName())));
            return CompletableFuture
                    .allOf(sortedProperties
                            .map(it -> new PropertyContext<>(it, context.getRootContext()))
                            .map(CompletionStages::toStage)
                            .map(uiSchemaConverter::convert)
                            .toArray(CompletableFuture[]::new))
                    .thenApply(done -> {
                        addActions(context, uiSchema, properties);
                        return context;
                    });
        });
    }
}
