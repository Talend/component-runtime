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
package org.talend.sdk.component.form.api;

import static java.util.stream.Collectors.toSet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.talend.sdk.component.form.internal.converter.PropertyContext;
import org.talend.sdk.component.form.internal.converter.impl.JsonSchemaConverter;
import org.talend.sdk.component.form.internal.converter.impl.PropertiesConverter;
import org.talend.sdk.component.form.internal.converter.impl.UiSchemaConverter;
import org.talend.sdk.component.form.model.Ui;
import org.talend.sdk.component.form.model.jsonschema.JsonSchema;
import org.talend.sdk.component.server.front.model.ComponentDetail;
import org.talend.sdk.component.server.front.model.SimplePropertyDefinition;

import lombok.extern.slf4j.Slf4j;

// todo: support @Group and all the elements of the API
@Slf4j
public class UiSpecService {

    private final Client client;

    public UiSpecService(final Client client) {
        this.client = client;
    }

    public Ui convert(final ComponentDetail detail) {
        final Ui ui = new Ui();
        ui.setUiSchema(new ArrayList<>());
        ui.setProperties(new HashMap<>());
        ui.setJsonSchema(new JsonSchema());
        ui.getJsonSchema().setTitle(detail.getDisplayName());
        ui.getJsonSchema().setType("object");
        ui
                .getJsonSchema()
                .setRequired(detail
                        .getProperties()
                        .stream()
                        .filter(p -> p.getName().equals(p.getPath()))
                        .filter(p -> new PropertyContext(p).isRequired())
                        .map(SimplePropertyDefinition::getName)
                        .collect(toSet()));

        final JsonSchemaConverter jsonSchemaConverter =
                new JsonSchemaConverter(ui.getJsonSchema(), detail.getProperties());
        final UiSchemaConverter uiSchemaConverter = new UiSchemaConverter(null, detail.getId().getFamily(),
                ui.getUiSchema(), client, detail.getProperties(), detail.getActions());
        final PropertiesConverter propertiesConverter =
                new PropertiesConverter(Map.class.cast(ui.getProperties()), detail.getProperties());

        detail
                .getProperties()
                .stream()
                .filter(Objects::nonNull)
                .filter(p -> p.getName().equals(p.getPath()))
                .map(PropertyContext::new)
                .forEach(p -> {
                    jsonSchemaConverter.convert(p);
                    uiSchemaConverter.convert(p);
                    propertiesConverter.convert(p);
                });

        return ui;
    }

}
