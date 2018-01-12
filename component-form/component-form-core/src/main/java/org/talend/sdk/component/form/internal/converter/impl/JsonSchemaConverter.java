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
package org.talend.sdk.component.form.internal.converter.impl;

import static java.util.Arrays.asList;
import static java.util.Locale.ROOT;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

import org.talend.sdk.component.form.internal.converter.PropertyContext;
import org.talend.sdk.component.form.internal.converter.PropertyConverter;
import org.talend.sdk.component.form.internal.converter.impl.schema.ArrayPropertyConverter;
import org.talend.sdk.component.form.internal.converter.impl.schema.EnumPropertyConverter;
import org.talend.sdk.component.form.model.jsonschema.JsonSchema;
import org.talend.sdk.component.server.front.model.PropertyValidation;
import org.talend.sdk.component.server.front.model.SimplePropertyDefinition;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class JsonSchemaConverter implements PropertyConverter {

    private final JsonSchema rootJsonSchema;

    private final Collection<SimplePropertyDefinition> properties;

    @Override
    public void convert(final PropertyContext p) {
        final JsonSchema jsonSchema = new JsonSchema();
        jsonSchema.setTitle(p.getProperty().getDisplayName());
        switch (p.getProperty().getType().toLowerCase(ROOT)) {
        case "enum":
            new EnumPropertyConverter(jsonSchema).convert(p);
            break;
        case "array":
            new ArrayPropertyConverter(jsonSchema, properties).convert(p);
            break;
        default:
            jsonSchema.setType(p.getProperty().getType().toLowerCase(ROOT));
            jsonSchema.setRequired(properties
                    .stream()
                    .filter(p::isDirectChild)
                    .filter(nested -> new PropertyContext(nested).isRequired())
                    .map(SimplePropertyDefinition::getName)
                    .collect(toSet()));
            break;
        }
        ofNullable(p.getProperty().getMetadata().get("ui::defaultvalue::value")).ifPresent(jsonSchema::setDefaultValue);

        final PropertyValidation validation = p.getProperty().getValidation();
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

        if (properties
                .stream() // has child
                .anyMatch(p::isDirectChild)) {
            final boolean isOrder = p.getProperty().getMetadata().get("ui::optionsorder::value") != null;
            if (isOrder) {
                jsonSchema.setProperties(new TreeMap<>(new Comparator<String>() {

                    final String order = p.getProperty().getMetadata().get("ui::optionsorder::value");

                    private final List<String> propertiesOrder = new ArrayList<>(asList(order.split(",")));

                    @Override
                    public int compare(final String o1, final String o2) {
                        final int i = propertiesOrder.indexOf(o1) - propertiesOrder.indexOf(o2);
                        return i == 0 ? o1.compareTo(o2) : i;
                    }
                }));
            } else {
                jsonSchema.setProperties(new HashMap<>());
            }

            final JsonSchemaConverter jsonSchemaConverter = new JsonSchemaConverter(jsonSchema, properties);
            properties.stream().filter(p::isDirectChild).map(PropertyContext::new).forEach(
                    jsonSchemaConverter::convert);
        }

        if (rootJsonSchema.getProperties() == null) {
            rootJsonSchema.setProperties(new HashMap<>());
        }
        rootJsonSchema.getProperties().put(p.getProperty().getName(), jsonSchema);
    }
}
