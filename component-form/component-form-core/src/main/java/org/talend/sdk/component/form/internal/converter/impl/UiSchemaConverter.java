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

import static java.util.stream.Collectors.toList;

import java.util.Collection;
import java.util.Locale;

import org.talend.sdk.component.form.api.Client;
import org.talend.sdk.component.form.internal.converter.PropertyContext;
import org.talend.sdk.component.form.internal.converter.PropertyConverter;
import org.talend.sdk.component.form.internal.converter.impl.widget.CodeWidgetConverter;
import org.talend.sdk.component.form.internal.converter.impl.widget.CredentialWidgetConverter;
import org.talend.sdk.component.form.internal.converter.impl.widget.DataListWidgetConverter;
import org.talend.sdk.component.form.internal.converter.impl.widget.FieldSetWidgetConverter;
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

    private final String family;

    private final Collection<UiSchema> schemas;

    private final Client client;

    private Collection<SimplePropertyDefinition> properties;

    private Collection<ActionReference> actions;

    @Override
    public void convert(final PropertyContext p) {
        final String type = p.getProperty().getType().toLowerCase(Locale.ROOT);
        switch (type) {
        case "object":
            new FieldSetWidgetConverter(schemas, properties, actions, client, family).convert(p);
            break;
        case "boolean":
            new ToggleWidgetConverter(schemas, properties, actions).convert(p);
            break;
        case "enum":
            new DataListWidgetConverter(schemas, properties, actions).convert(p);
            break;
        case "number":
            new NumberWidgetConverter(schemas, properties, actions).convert(p);
            break;
        case "array":
            final String nestedPrefix = p.getProperty().getPath() + "[].";
            final int from = nestedPrefix.length();
            final Collection<SimplePropertyDefinition> nested = properties
                    .stream()
                    .filter(prop -> prop.getPath().startsWith(nestedPrefix) && prop.getPath().indexOf('.', from) < 0)
                    .collect(toList());
            if (!nested.isEmpty()) {
                new ObjectArrayWidgetConverter(schemas, properties, actions, nested, family, client).convert(p);
            } else {
                new MultiSelectTagWidgetConverter(schemas, properties, actions, client, family).convert(p);
            }
            break;
        case "string":
        default:
            if (p.getProperty().getPath().endsWith("[]")) {
                return;
            }
            if ("true".equalsIgnoreCase(p.getProperty().getMetadata().get("ui::credential"))) {
                new CredentialWidgetConverter(schemas, properties, actions).convert(p);
            } else if (p.getProperty().getMetadata().containsKey("ui::code::value")) {
                new CodeWidgetConverter(schemas, properties, actions).convert(p);
            } else if (p.getProperty().getMetadata() != null
                    && p.getProperty().getMetadata().containsKey("action::dynamic_values")) {
                new MultiSelectTagWidgetConverter(schemas, properties, actions, client, family).convert(p);
            } else {
                new TextWidgetConverter(schemas, properties, actions).convert(p);
            }
            break;
        }

    }

}