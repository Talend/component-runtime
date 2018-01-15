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

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.talend.sdk.component.form.api.Client;
import org.talend.sdk.component.form.internal.converter.PropertyContext;
import org.talend.sdk.component.form.model.uischema.UiSchema;
import org.talend.sdk.component.server.front.model.ActionReference;
import org.talend.sdk.component.server.front.model.SimplePropertyDefinition;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MultiSelectTagWidgetConverter extends AbstractWidgetConverter {

    private final Client client;

    private final String family;

    public MultiSelectTagWidgetConverter(final Collection<UiSchema> schemas,
            final Collection<SimplePropertyDefinition> properties, final Collection<ActionReference> actions,
            final Client client, final String family) {
        super(schemas, properties, actions);

        this.client = client;
        this.family = family;
    }

    @Override
    public void convert(final PropertyContext p) {
        final UiSchema schema = newUiSchema(p);
        schema.setWidget("multiSelectTag");
        schema.setRestricted(false);
        if (client != null) {
            try {
                final Map<String, Object> values = client.action(family, "dynamic_values",
                        p.getProperty().getMetadata().get("action::dynamic_values"), emptyMap());

                final List<UiSchema.NameValue> namedValues =
                        ofNullable(values).map(v -> v.get("items")).filter(Collection.class::isInstance).map(c -> {
                            final Collection<?> dynamicValues = Collection.class.cast(

                                    c);
                            return dynamicValues
                                    .stream()
                                    .filter(Map.class::isInstance)
                                    .filter(m -> Map.class.cast(m).get("id") != null
                                            && Map.class.cast(m).get("id") instanceof String)
                                    .map(Map.class::cast)
                                    .map(entry -> {
                                        final UiSchema.NameValue val = new UiSchema.NameValue();
                                        val.setName((String) entry.get("id"));
                                        val.setValue(entry.get("label") == null ? (String) entry.get("id")
                                                : (String) entry.get("label"));
                                        return val;
                                    })
                                    .collect(toList());
                        }).orElse(emptyList());
                schema.setTitleMap(namedValues);
            } catch (final RuntimeException error) {
                schema.setTitleMap(emptyList());
                log.debug(error.getMessage(), error);
            }
        } else {
            schema.setTitleMap(emptyList());
        }
    }
}
