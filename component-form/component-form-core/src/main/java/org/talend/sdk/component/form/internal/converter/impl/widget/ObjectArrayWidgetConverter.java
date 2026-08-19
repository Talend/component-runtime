/**
 * Copyright (C) 2006-2026 Talend Inc. - www.talend.com
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;

import org.talend.sdk.component.form.api.Client;
import org.talend.sdk.component.form.internal.converter.CustomPropertyConverter;
import org.talend.sdk.component.form.internal.converter.PropertyContext;
import org.talend.sdk.component.form.internal.converter.impl.UiSchemaConverter;
import org.talend.sdk.component.form.model.jsonschema.JsonSchema;
import org.talend.sdk.component.form.model.uischema.UiSchema;
import org.talend.sdk.component.server.front.model.ActionReference;
import org.talend.sdk.component.server.front.model.SimplePropertyDefinition;

public class ObjectArrayWidgetConverter extends AbstractWidgetConverter {

    private final String gridLayoutFilter;

    private final Client client;

    private final String family;

    private final Collection<CustomPropertyConverter> customPropertyConverters;

    private final Map<String, String> metaToPropagate;

    private final AtomicInteger idGenerator;

    // CHECKSTYLE:OFF
    public ObjectArrayWidgetConverter(final Collection<UiSchema> schemas,
            final Collection<SimplePropertyDefinition> properties, final Collection<ActionReference> actions,
            final String family, final Client client, final String gridLayoutFilter, final JsonSchema jsonSchema,
            final String lang, final Collection<CustomPropertyConverter> customPropertyConverters,
            final Map<String, String> metaToPropagate, final AtomicInteger idGenerator) {
        // CHECKSTYLE:ON
        super(schemas, properties, actions, jsonSchema, lang);
        this.family = family;
        this.client = client;
        this.gridLayoutFilter = gridLayoutFilter;
        this.customPropertyConverters = customPropertyConverters;
        this.metaToPropagate = metaToPropagate;
        this.idGenerator = idGenerator;
    }

    @Override
    public CompletionStage<PropertyContext<?>> convert(final CompletionStage<PropertyContext<?>> cs) {
        return cs.thenCompose(context -> {
            final UiSchema arraySchema = newUiSchema(context);
            final SimplePropertyDefinition original = context.getProperty();
            arraySchema.setTitle(original.getDisplayName());
            arraySchema.setItems(new ArrayList<>());
            arraySchema.setItemWidget("collapsibleFieldset");
            final UiSchemaConverter converter =
                    new UiSchemaConverter(gridLayoutFilter, family, arraySchema.getItems(), new ArrayList<>(), client,
                            jsonSchema, properties, actions, lang, customPropertyConverters, idGenerator);
            return converter
                    .convertObject(new PropertyContext<>(
                            new SimplePropertyDefinition(original.getPath() + "[]", original.getName(), null,
                                    original.getType(), original.getDefaultValue(), original.getValidation(),
                                    metaToPropagate, original.getPlaceholder(), original.getProposalDisplayNames()),
                            context.getRootContext(), context.getConfiguration()), metaToPropagate, arraySchema,
                            idGenerator);
        });
    }
}
