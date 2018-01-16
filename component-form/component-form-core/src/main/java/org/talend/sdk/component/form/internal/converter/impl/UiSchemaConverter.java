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

import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import org.talend.sdk.component.form.api.Client;
import org.talend.sdk.component.form.internal.converter.PropertyContext;
import org.talend.sdk.component.form.internal.converter.PropertyConverter;
import org.talend.sdk.component.form.internal.converter.impl.widget.CodeWidgetConverter;
import org.talend.sdk.component.form.internal.converter.impl.widget.CredentialWidgetConverter;
import org.talend.sdk.component.form.internal.converter.impl.widget.DataListWidgetConverter;
import org.talend.sdk.component.form.internal.converter.impl.widget.FieldSetWidgetConverter;
import org.talend.sdk.component.form.internal.converter.impl.widget.GridLayoutWidgetConverter;
import org.talend.sdk.component.form.internal.converter.impl.widget.MultiSelectTagWidgetConverter;
import org.talend.sdk.component.form.internal.converter.impl.widget.NumberWidgetConverter;
import org.talend.sdk.component.form.internal.converter.impl.widget.ObjectArrayWidgetConverter;
import org.talend.sdk.component.form.internal.converter.impl.widget.TextWidgetConverter;
import org.talend.sdk.component.form.internal.converter.impl.widget.ToggleWidgetConverter;
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

    private Collection<SimplePropertyDefinition> properties;

    private Collection<ActionReference> actions;

    @Override
    public void convert(final PropertyContext context) {
        final String type = context.getProperty().getType().toLowerCase(Locale.ROOT);
        switch (type) {
        case "object":
            final Map<String, String> gridLayouts = context
                    .getProperty()
                    .getMetadata()
                    .entrySet()
                    .stream()
                    .filter(e -> e.getKey().startsWith("ui::gridlayout::") && e.getKey().endsWith("::value"))
                    .collect(toMap(e -> e.getKey().substring("ui::gridlayout::".length(),
                            e.getKey().length() - "::value".length()), Map.Entry::getValue, (a, b) -> {
                                throw new IllegalArgumentException("Can't merge " + a + " and " + b);
                            }, () -> new TreeMap<>(String.CASE_INSENSITIVE_ORDER)));
            if (!gridLayouts.isEmpty()) {
                new GridLayoutWidgetConverter(schemas, properties, actions, client, family,
                        gridLayoutFilter != null && gridLayouts.containsKey(gridLayoutFilter)
                                ? singletonMap(gridLayoutFilter, gridLayouts.get(gridLayoutFilter))
                                : gridLayouts).convert(context);
            } else {
                new FieldSetWidgetConverter(schemas, properties, actions, client, family).convert(context);
            }
            break;
        case "boolean":
            includedProperties.add(context.getProperty());
            new ToggleWidgetConverter(schemas, properties, actions).convert(context);
            break;
        case "enum":
            includedProperties.add(context.getProperty());
            new DataListWidgetConverter(schemas, properties, actions).convert(context);
            break;
        case "number":
            includedProperties.add(context.getProperty());
            new NumberWidgetConverter(schemas, properties, actions).convert(context);
            break;
        case "array":
            includedProperties.add(context.getProperty());
            final String nestedPrefix = context.getProperty().getPath() + "[].";
            final int from = nestedPrefix.length();
            final Collection<SimplePropertyDefinition> nested = properties
                    .stream()
                    .filter(prop -> prop.getPath().startsWith(nestedPrefix) && prop.getPath().indexOf('.', from) < 0)
                    .collect(toList());
            if (!nested.isEmpty()) {
                new ObjectArrayWidgetConverter(schemas, properties, actions, nested, family, client, gridLayoutFilter)
                        .convert(context);
            } else {
                new MultiSelectTagWidgetConverter(schemas, properties, actions, client, family).convert(context);
            }
            break;
        case "string":
        default:
            if (context.getProperty().getPath().endsWith("[]")) {
                return;
            }
            includedProperties.add(context.getProperty());
            if ("true".equalsIgnoreCase(context.getProperty().getMetadata().get("ui::credential"))) {
                new CredentialWidgetConverter(schemas, properties, actions).convert(context);
            } else if (context.getProperty().getMetadata().containsKey("ui::code::value")) {
                new CodeWidgetConverter(schemas, properties, actions).convert(context);
            } else if (context.getProperty().getMetadata() != null
                    && context.getProperty().getMetadata().containsKey("action::dynamic_values")) {
                new MultiSelectTagWidgetConverter(schemas, properties, actions, client, family).convert(context);
            } else {
                new TextWidgetConverter(schemas, properties, actions).convert(context);
            }
            break;
        }

    }

}