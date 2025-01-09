/**
 * Copyright (C) 2006-2025 Talend Inc. - www.talend.com
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

import static java.util.Optional.ofNullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.json.bind.Jsonb;

import org.talend.sdk.component.form.internal.converter.PropertyContext;
import org.talend.sdk.component.form.internal.converter.PropertyConverter;
import org.talend.sdk.component.form.internal.lang.CompletionStages;
import org.talend.sdk.component.server.front.model.SimplePropertyDefinition;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class PropertiesConverter implements PropertyConverter {

    private final Jsonb jsonb;

    private final Map<String, Object> defaults;

    private final Collection<SimplePropertyDefinition> properties;

    @Override
    public CompletionStage<PropertyContext<?>> convert(final CompletionStage<PropertyContext<?>> cs) {
        return cs.thenCompose(context -> {
            final SimplePropertyDefinition property = context.getProperty();
            if ("object".equalsIgnoreCase(property.getType())) {
                final Map<String, Object> childDefaults = new HashMap<>();
                defaults.put(property.getName(), childDefaults);
                final PropertiesConverter propertiesConverter =
                        new PropertiesConverter(jsonb, childDefaults, properties);

                return CompletableFuture
                        .allOf(context
                                .findDirectChild(properties)
                                .map(it -> new PropertyContext<>(it, context.getRootContext(),
                                        context.getConfiguration()))
                                .map(CompletionStages::toStage)
                                .map(propertiesConverter::convert)
                                .toArray(CompletableFuture[]::new))
                        .thenApply(done -> context);
            }

            ofNullable(property.getMetadata().getOrDefault("ui::defaultvalue::value", property.getDefaultValue()))
                    .ifPresent(value -> {
                        if ("number".equalsIgnoreCase(property.getType())) {
                            defaults.put(property.getName(), Double.parseDouble(value));
                        } else if ("boolean".equalsIgnoreCase(property.getType())) {
                            defaults.put(property.getName(), Boolean.parseBoolean(value));
                        } else if ("array".equalsIgnoreCase(property.getType())) {
                            defaults.put(property.getName(), jsonb.fromJson(value, Object[].class));
                        } else if ("object".equalsIgnoreCase(property.getType())) {
                            defaults.put(property.getName(), jsonb.fromJson(value, Map.class));
                        } else {
                            if ("string".equalsIgnoreCase(property.getType()) && property
                                    .getMetadata()
                                    .keySet()
                                    .stream()
                                    .anyMatch(k -> k.equalsIgnoreCase("action::suggestions")
                                            || k.equalsIgnoreCase("action::dynamic_values"))) {
                                defaults.putIfAbsent("$" + property.getName() + "_name", value);
                            }
                            defaults.put(property.getName(), value);
                        }
                    });
            return CompletableFuture.completedFuture(context);
        });
    }
}
