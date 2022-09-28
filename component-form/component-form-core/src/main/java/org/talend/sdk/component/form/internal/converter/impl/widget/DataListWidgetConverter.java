/**
 * Copyright (C) 2006-2022 Talend Inc. - www.talend.com
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

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.talend.sdk.component.form.api.Client;
import org.talend.sdk.component.form.internal.converter.PropertyContext;
import org.talend.sdk.component.form.model.jsonschema.JsonSchema;
import org.talend.sdk.component.form.model.uischema.UiSchema;
import org.talend.sdk.component.server.front.model.ActionReference;
import org.talend.sdk.component.server.front.model.SimplePropertyDefinition;

public class DataListWidgetConverter extends AbstractWidgetConverter {

    private final Client client;

    private final String family;

    public DataListWidgetConverter(final Collection<UiSchema> schemas,
            final Collection<SimplePropertyDefinition> properties, final Collection<ActionReference> actions,
            final Client client, final String family, final JsonSchema jsonSchema, final String lang) {
        super(schemas, properties, actions, jsonSchema, lang);
        this.client = client;
        this.family = family;
    }

    @Override
    public CompletionStage<PropertyContext<?>> convert(final CompletionStage<PropertyContext<?>> cs) {
        return cs.thenCompose(context -> {
            final UiSchema schema = newUiSchema(context);
            setupUiSchema(context, schema);

            final JsonSchema jsonSchema = findJsonSchema(context);
            if (jsonSchema == null) { // unexpected
                return CompletableFuture.completedFuture(context);
            }

            if (jsonSchema.getType() == null) {
                jsonSchema.setType("string");
            }
            return fillProposalsAndReturn(context, schema, jsonSchema);
        });
    }

    protected CompletionStage<PropertyContext<?>> fillProposalsAndReturn(final PropertyContext<?> context,
            final UiSchema schema, final JsonSchema jsonSchema) {
        if (context.getProperty().getValidation() != null
                && context.getProperty().getValidation().getEnumValues() != null) {
            schema
                    .setTitleMap(context.getProperty().getProposalDisplayNames() != null
                            ? context.getProperty().getProposalDisplayNames().entrySet().stream().map(v -> {
                                final UiSchema.NameValue nameValue = new UiSchema.NameValue();
                                nameValue.setName(v.getValue());
                                nameValue.setValue(v.getKey());
                                return nameValue;
                            }).collect(toList())
                            : context.getProperty().getValidation().getEnumValues().stream().sorted().map(v -> {
                                final UiSchema.NameValue nameValue = new UiSchema.NameValue();
                                nameValue.setName(v);
                                nameValue.setValue(v);
                                return nameValue;
                            }).collect(toList()));
            schema.setRestricted(true);
            jsonSchema.setEnumValues(context.getProperty().getValidation().getEnumValues());
        } else {
            final String actionName = context.getProperty().getMetadata().get("action::dynamic_values");
            if (client != null && actionName != null) {
                final CompletionStage<List<UiSchema.NameValue>> pairs =
                        loadDynamicValues(client, family, actionName, context.getRootContext());
                return pairs.thenApply(namedValues -> {
                    schema.setTitleMap(namedValues);
                    schema.setRestricted(true);
                    jsonSchema.setEnumValues(namedValues.stream().map(UiSchema.NameValue::getValue).collect(toList()));
                    return context;
                });
            } else {
                schema.setTitleMap(emptyList());
            }
        }
        return CompletableFuture.completedFuture(context);
    }

    protected void setupUiSchema(final PropertyContext<?> context, final UiSchema schema) {
        schema.setWidget("datalist");
    }
}
