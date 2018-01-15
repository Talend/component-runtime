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

import static java.util.Collections.singletonList;

import java.util.ArrayList;
import java.util.Collection;

import org.talend.sdk.component.form.api.Client;
import org.talend.sdk.component.form.internal.converter.PropertyContext;
import org.talend.sdk.component.form.internal.converter.impl.UiSchemaConverter;
import org.talend.sdk.component.form.model.uischema.UiSchema;
import org.talend.sdk.component.server.front.model.ActionReference;
import org.talend.sdk.component.server.front.model.SimplePropertyDefinition;

public class ObjectArrayWidgetConverter extends AbstractWidgetConverter {

    private final Client client;

    private final String family;

    private final Collection<SimplePropertyDefinition> nestedProperties;

    public ObjectArrayWidgetConverter(final Collection<UiSchema> schemas,
            final Collection<SimplePropertyDefinition> properties, final Collection<ActionReference> actions,
            final Collection<SimplePropertyDefinition> nested, final String family, final Client client) {
        super(schemas, properties, actions);
        this.nestedProperties = nested;
        this.family = family;
        this.client = client;
    }

    @Override
    public void convert(final PropertyContext ctx) {
        final UiSchema arraySchema = newUiSchema(ctx);
        arraySchema.setTitle(ctx.getProperty().getDisplayName());
        arraySchema.setItems(new ArrayList<>());
        final UiSchemaConverter converter =
                new UiSchemaConverter(family, arraySchema.getItems(), client, properties, actions);
        nestedProperties.forEach(p -> converter.convert(new PropertyContext(p)));

        // until previous schema supports a title
        final UiSchema fieldSetWrapper = new UiSchema();
        fieldSetWrapper.setTitle(arraySchema.getTitle());
        fieldSetWrapper.setItems(singletonList(arraySchema));
        fieldSetWrapper.setWidget("fieldset");
        schemas.remove(arraySchema);
        schemas.add(fieldSetWrapper);

    }
}
