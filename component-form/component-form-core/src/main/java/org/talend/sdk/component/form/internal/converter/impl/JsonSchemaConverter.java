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
package org.talend.sdk.component.form.internal.converter.impl;

import static java.util.Arrays.asList;
import static java.util.Locale.ROOT;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.json.bind.Jsonb;

import org.talend.sdk.component.form.internal.converter.PropertyContext;
import org.talend.sdk.component.form.internal.converter.PropertyConverter;
import org.talend.sdk.component.form.internal.converter.impl.schema.ArrayPropertyConverter;
import org.talend.sdk.component.form.internal.converter.impl.schema.EnumPropertyConverter;
import org.talend.sdk.component.form.internal.lang.CompletionStages;
import org.talend.sdk.component.form.model.jsonschema.JsonSchema;
import org.talend.sdk.component.server.front.model.PropertyValidation;
import org.talend.sdk.component.server.front.model.SimplePropertyDefinition;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class JsonSchemaConverter implements PropertyConverter {

    private final Jsonb jsonb;

    private final JsonSchema rootJsonSchema;

    private final Collection<SimplePropertyDefinition> properties;

    @Override
    public CompletionStage<PropertyContext<?>> convert(final CompletionStage<PropertyContext<?>> cs) {
        return cs.thenCompose(context -> {
            final JsonSchema jsonSchema = new JsonSchema();
            jsonSchema.setTitle(context.getProperty().getDisplayName());
            final String type = context.getProperty().getType();
            switch (type.toLowerCase(ROOT)) {
            case "enum":
                return new EnumPropertyConverter(jsonSchema)
                        .convert(CompletableFuture.completedFuture(context))
                        .thenCompose(c -> postHandling(context, jsonSchema, type));
            case "array":
                return new ArrayPropertyConverter(jsonb, jsonSchema, properties)
                        .convert(CompletableFuture.completedFuture(context))
                        .thenCompose(c -> postHandling(context, jsonSchema, type));
            default:
                if (context.getProperty().getPath().endsWith("[]")) {
                    return CompletableFuture.completedFuture(context);
                }
                jsonSchema.setType(type.toLowerCase(ROOT));
                of(context
                        .findDirectChild(properties)
                        .filter(nested -> new PropertyContext<>(nested, context.getRootContext(),
                                context.getConfiguration()).isRequired())
                        .map(SimplePropertyDefinition::getName)
                        .collect(toSet())).filter(s -> !s.isEmpty()).ifPresent(jsonSchema::setRequired);
                return CompletableFuture
                        .completedFuture(context)
                        .thenCompose(c -> postHandling(context, jsonSchema, type));
            }
        });
    }

    private CompletionStage<PropertyContext<?>> postHandling(final PropertyContext<?> context,
            final JsonSchema jsonSchema, final String type) {
        final String defaultValue = context
                .getProperty()
                .getMetadata()
                .getOrDefault("ui::defaultvalue::value", context.getProperty().getDefaultValue());
        convertDefaultValue(type, defaultValue).ifPresent(jsonSchema::setDefaultValue);

        final PropertyValidation validation = context.getProperty().getValidation();
        if (validation != null) {
            ofNullable(validation.getMin()).ifPresent(m -> jsonSchema.setMinimum(m.doubleValue()));
            ofNullable(validation.getMax()).ifPresent(m -> jsonSchema.setMaximum(m.doubleValue()));
            ofNullable(validation.getMinItems()).ifPresent(jsonSchema::setMinItems);
            ofNullable(validation.getMaxItems()).ifPresent(jsonSchema::setMaxItems);
            ofNullable(validation.getMinLength()).ifPresent(jsonSchema::setMinLength);
            ofNullable(validation.getMaxLength()).ifPresent(jsonSchema::setMaxLength);
            ofNullable(validation.getUniqueItems()).ifPresent(jsonSchema::setUniqueItems);
            ofNullable(validation.getPattern()).ifPresent(jsonSchema::setPattern);
        }

        synchronized (rootJsonSchema) {
            if (rootJsonSchema.getProperties() == null) {
                rootJsonSchema.setProperties(new TreeMap<>());
            }
            if (rootJsonSchema.getProperties().put(context.getProperty().getName(), jsonSchema) != null) {
                throw new IllegalStateException(
                        "Conflicting attribute: " + context.getProperty() + ", in " + rootJsonSchema);
            }
        }

        final Set<SimplePropertyDefinition> nestedProperties = context.findDirectChild(properties).collect(toSet());
        if (!nestedProperties.isEmpty()) {
            final String order = context.getProperty().getMetadata().get("ui::optionsorder::value");
            if (order != null) {
                jsonSchema.setProperties(new TreeMap<>(new Comparator<String>() {

                    private final List<String> propertiesOrder = new ArrayList<>(asList(order.split(",")));

                    @Override
                    public int compare(final String o1, final String o2) {
                        final int i = propertiesOrder.indexOf(o1) - propertiesOrder.indexOf(o2);
                        return i == 0 ? o1.compareTo(o2) : i;
                    }
                }));
            } else {
                jsonSchema.setProperties(new TreeMap<>());
            }

            final JsonSchemaConverter jsonSchemaConverter = new JsonSchemaConverter(jsonb, jsonSchema, properties);
            return CompletableFuture
                    .allOf(nestedProperties
                            .stream()
                            .map(it -> new PropertyContext<>(it, context.getRootContext(), context.getConfiguration()))
                            .map(CompletionStages::toStage)
                            .map(jsonSchemaConverter::convert)
                            .toArray(CompletableFuture[]::new))
                    .thenApply(r -> context);
        }

        return CompletableFuture.completedFuture(context);
    }

    private Optional<Object> convertDefaultValue(final String type, final String defaultValue) {
        return ofNullable(defaultValue).map(def -> {
            if ("array".equalsIgnoreCase(type)) {
                return jsonb.fromJson(def, Object[].class);
            } else if ("object".equalsIgnoreCase(type)) {
                return jsonb.fromJson(def, Map.class);
            } else if ("boolean".equalsIgnoreCase(type)) {
                return Boolean.parseBoolean(def.trim());
            } else if ("number".equalsIgnoreCase(type)) {
                return Double.parseDouble(def.trim());
            }
            return def;
        });
    }
}
