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

import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;

import org.talend.sdk.component.form.api.Client;
import org.talend.sdk.component.form.internal.converter.CustomPropertyConverter;
import org.talend.sdk.component.form.internal.converter.PropertyContext;
import org.talend.sdk.component.form.internal.converter.PropertyConverter;
import org.talend.sdk.component.form.internal.converter.impl.widget.CodeWidgetConverter;
import org.talend.sdk.component.form.internal.converter.impl.widget.CredentialWidgetConverter;
import org.talend.sdk.component.form.internal.converter.impl.widget.DataListWidgetConverter;
import org.talend.sdk.component.form.internal.converter.impl.widget.DateTimeConverter;
import org.talend.sdk.component.form.internal.converter.impl.widget.FieldSetWidgetConverter;
import org.talend.sdk.component.form.internal.converter.impl.widget.GridLayoutWidgetConverter;
import org.talend.sdk.component.form.internal.converter.impl.widget.MultiSelectWidgetConverter;
import org.talend.sdk.component.form.internal.converter.impl.widget.NumberWidgetConverter;
import org.talend.sdk.component.form.internal.converter.impl.widget.ObjectArrayWidgetConverter;
import org.talend.sdk.component.form.internal.converter.impl.widget.SuggestionWidgetConverter;
import org.talend.sdk.component.form.internal.converter.impl.widget.TextAreaWidgetConverter;
import org.talend.sdk.component.form.internal.converter.impl.widget.TextWidgetConverter;
import org.talend.sdk.component.form.internal.converter.impl.widget.ToggleWidgetConverter;
import org.talend.sdk.component.form.model.jsonschema.JsonSchema;
import org.talend.sdk.component.form.model.uischema.UiSchema;
import org.talend.sdk.component.server.front.model.ActionReference;
import org.talend.sdk.component.server.front.model.SimplePropertyDefinition;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class UiSchemaConverter implements PropertyConverter {

    private final String gridLayoutFilter;

    private final String family;

    private final Collection<UiSchema> schemas;

    private final Collection<SimplePropertyDefinition> includedProperties;

    private final Client client;

    private final JsonSchema jsonSchema;

    private Collection<SimplePropertyDefinition> properties;

    private Collection<ActionReference> actions;

    private final String lang;

    private final Collection<CustomPropertyConverter> customConverters;

    private final AtomicInteger idGenerator;

    @Override
    public CompletionStage<PropertyContext<?>> convert(final CompletionStage<PropertyContext<?>> cs) {
        return cs
                .thenCompose(context -> customConverters
                        .stream()
                        .filter(it -> it.supports(context))
                        .findFirst()
                        .map(it -> it
                                .convert(cs,
                                        new CustomPropertyConverter.ConverterContext(family, schemas,
                                                includedProperties, client, jsonSchema, properties, actions, lang)))
                        .orElseGet(() -> {
                            final SimplePropertyDefinition property = context.getProperty();
                            final String type = property.getType().toLowerCase(Locale.ROOT);
                            final Map<String, String> metadata = property.getMetadata();
                            switch (type) {
                            case "object":
                                return convertObject(context, metadata, null, idGenerator);
                            case "boolean":
                                includedProperties.add(property);
                                return new ToggleWidgetConverter(schemas, properties, actions, jsonSchema, lang)
                                        .convert(CompletableFuture.completedFuture(context));
                            case "enum":
                                includedProperties.add(property);
                                return new DataListWidgetConverter(schemas, properties, actions, client, family,
                                        jsonSchema, lang).convert(CompletableFuture.completedFuture(context));
                            case "number":
                                includedProperties.add(property);
                                return new NumberWidgetConverter(schemas, properties, actions, jsonSchema, lang)
                                        .convert(CompletableFuture.completedFuture(context));
                            case "array":
                                includedProperties.add(property);
                                final String nestedPrefix = property.getPath() + "[].";
                                final int from = nestedPrefix.length();
                                final Collection<SimplePropertyDefinition> nested = properties
                                        .stream()
                                        .filter(prop -> prop.getPath().startsWith(nestedPrefix)
                                                && prop.getPath().indexOf('.', from) < 0)
                                        .collect(toList());
                                if (!nested.isEmpty()) {
                                    return new ObjectArrayWidgetConverter(schemas, properties, actions, family, client,
                                            gridLayoutFilter, jsonSchema, lang, customConverters, metadata, idGenerator)
                                            .convert(CompletableFuture.completedFuture(context));
                                }
                                return new MultiSelectWidgetConverter(schemas, properties, actions, client, family,
                                        jsonSchema, lang).convert(CompletableFuture.completedFuture(context));
                            case "string":
                            default:
                                if (property.getPath().endsWith("[]")) {
                                    return CompletableFuture.completedFuture(context);
                                }
                                includedProperties.add(property);
                                if ("true".equalsIgnoreCase(metadata.get("ui::credential"))) {
                                    return new CredentialWidgetConverter(schemas, properties, actions, jsonSchema, lang)
                                            .convert(CompletableFuture.completedFuture(context));
                                } else if (metadata.containsKey("ui::code::value")) {
                                    return new CodeWidgetConverter(schemas, properties, actions, jsonSchema, lang)
                                            .convert(CompletableFuture.completedFuture(context));
                                } else if (metadata.containsKey("action::suggestions")
                                        || metadata.containsKey("action::built_in_suggestable")) {
                                    return new SuggestionWidgetConverter(schemas, properties, actions, jsonSchema, lang)
                                            .convert(CompletableFuture.completedFuture(context));
                                } else if (metadata.containsKey("action::dynamic_values")) {
                                    return new DataListWidgetConverter(schemas, properties, actions, client, family,
                                            jsonSchema, lang).convert(CompletableFuture.completedFuture(context));
                                } else if (metadata.containsKey("ui::textarea")
                                        && Boolean.valueOf(metadata.get("ui::textarea"))) {
                                    return new TextAreaWidgetConverter(schemas, properties, actions, jsonSchema, lang)
                                            .convert(CompletableFuture.completedFuture(context));
                                } else if (metadata.containsKey("ui::datetime")) {
                                    return new DateTimeConverter(schemas, properties, actions, jsonSchema, lang,
                                            metadata.get("ui::datetime"))
                                            .convert(CompletableFuture.completedFuture(context));
                                }

                                return new TextWidgetConverter(schemas, properties, actions, jsonSchema, lang)
                                        .convert(CompletableFuture.completedFuture(context));
                            }
                        }));
    }

    public CompletionStage<PropertyContext<?>> convertObject(final PropertyContext<?> outputContext,
            final Map<String, String> metadata, final UiSchema parentUiSchema, final AtomicInteger idGenerator) {
        final Map<String, String> gridLayouts = metadata
                .entrySet()
                .stream()
                .filter(e -> e.getKey().startsWith("ui::gridlayout::") && e
                        .getKey()

                        .endsWith("::value"))
                .collect(toMap(
                        e -> e
                                .getKey()
                                .substring("ui::gridlayout::".length(), e.getKey().length() - "::value".length()),
                        Map.Entry::getValue, (a, b) -> {
                            throw new IllegalArgumentException("Can't merge " + a + " and " + b);
                        }, () -> new TreeMap<>(String.CASE_INSENSITIVE_ORDER)));
        if (!gridLayouts.isEmpty()) {
            return new GridLayoutWidgetConverter(schemas, properties, actions, client, family,
                    gridLayoutFilter != null && gridLayouts.containsKey(gridLayoutFilter)
                            ? singletonMap(gridLayoutFilter, gridLayouts.get(gridLayoutFilter))
                            : gridLayouts,
                    jsonSchema, lang, customConverters, idGenerator)
                    .convert(CompletableFuture.completedFuture(outputContext));
        }
        final String forcedOrder = metadata.get("ui::optionsorder::value");
        return new FieldSetWidgetConverter(schemas, properties, actions, client, family, jsonSchema, forcedOrder, lang,
                customConverters, parentUiSchema, idGenerator)
                .convert(CompletableFuture.completedFuture(outputContext));
    }
}