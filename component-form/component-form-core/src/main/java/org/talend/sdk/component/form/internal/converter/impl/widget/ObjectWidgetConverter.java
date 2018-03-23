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
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toSet;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Stream;

import org.talend.sdk.component.form.internal.converter.PropertyContext;
import org.talend.sdk.component.form.model.uischema.UiSchema;
import org.talend.sdk.component.server.front.model.ActionReference;
import org.talend.sdk.component.server.front.model.SimplePropertyDefinition;

abstract class ObjectWidgetConverter extends AbstractWidgetConverter {

    ObjectWidgetConverter(final Collection<UiSchema> schemas, final Collection<SimplePropertyDefinition> properties,
            final Collection<ActionReference> actions) {
        super(schemas, properties, actions);
    }

    protected void addActions(final PropertyContext root, final UiSchema uiSchema,
            final Collection<SimplePropertyDefinition> includedProperties) {
        final Optional<SimplePropertyDefinition> schemaBinding = includedProperties
                .stream()
                .filter(p -> "OUT".equals(p.getMetadata().get("ui::structure::type")))
                .findFirst();
        final Collection<UiSchema> items = uiSchema.getItems();
        if (schemaBinding.isPresent()) {
            SimplePropertyDefinition bindingProp = schemaBinding.get();
            final String schemaActionName =
                    ofNullable(bindingProp.getMetadata().get("action::schema")).filter(n -> !n.isEmpty()).orElse(
                            "default");
            actions
                    .stream()
                    .filter(a -> a.getName().equals(schemaActionName) && "schema".equals(a.getType()))
                    .findFirst()
                    .ifPresent(ref -> {
                        final UiSchema.Trigger trigger = toTrigger(properties, root.getProperty(), ref);
                        trigger.setOptions(singletonList(new UiSchema.Option.Builder()
                                .withPath(bindingProp.getPath().replace("[]", ""))
                                .withType("array".equalsIgnoreCase(bindingProp.getType()) ? "array" : "object")
                                .build()));
                        if (trigger.getParameters() == null || trigger.getParameters().isEmpty()) {
                            // find the matching dataset
                            Optional<SimplePropertyDefinition> findParameters = properties
                                    .stream()
                                    .filter(nested -> ref.getName().equals(
                                            nested.getMetadata().get("configurationtype::name"))
                                            && "dataset".equals(nested.getMetadata().get("configurationtype::type")))
                                    .findFirst();
                            if (!findParameters.isPresent()) { // if not ambiguous grab the unique dataset
                                final Collection<SimplePropertyDefinition> datasets = properties
                                        .stream()
                                        .filter(nested -> "dataset"
                                                .equals(nested.getMetadata().get("configurationtype::type")))
                                        .collect(toSet());
                                if (datasets.size() == 1) {
                                    findParameters = of(datasets.iterator().next());
                                }
                            }
                            findParameters.ifPresent(dataset -> {
                                final UiSchema.Parameter parameter = new UiSchema.Parameter();
                                parameter.setKey(ofNullable(ref.getProperties())
                                        .orElse(Collections.emptyList())
                                        .stream()
                                        .filter(p -> !p.getPath().contains("."))
                                        .findFirst()
                                        .map(SimplePropertyDefinition::getName)
                                        .orElse("dataset"));
                                parameter.setPath(dataset.getPath());
                                trigger.setParameters(toParams(properties, dataset, ref, dataset.getPath()));
                            });
                        }

                        final UiSchema button = new UiSchema();
                        button.setKey("button_schema_" + root.getProperty().getPath());
                        button.setTitle("Guess Schema");
                        button.setWidget("button");
                        button.setTriggers(singletonList(trigger));
                        synchronized (items) {
                            items.add(button);
                        }
                    });
        }

        ofNullable(root.getProperty().getMetadata().get("action::healthcheck"))
                .flatMap(v -> (actions == null ? Stream.<ActionReference> empty() : actions.stream())
                        .filter(a -> a.getName().equals(v) && "healthcheck".equals(a.getType()))
                        .findFirst())
                .ifPresent(ref -> {
                    final UiSchema.Trigger trigger = toTrigger(properties, root.getProperty(), ref);
                    if (trigger.getParameters() == null || trigger.getParameters().isEmpty()) {
                        // find the matching dataset
                        properties
                                .stream()
                                .filter(nested -> "datastore"
                                        .equals(nested.getMetadata().get("configurationtype::type"))
                                        && ref.getName().equals(nested.getMetadata().get("configurationtype::name")))
                                .findFirst()
                                .ifPresent(datastore -> trigger
                                        .setParameters(toParams(properties, datastore, ref, datastore.getPath())));
                    }

                    final UiSchema button = new UiSchema();
                    button.setKey("button_healthcheck_" + root.getProperty().getPath());
                    button.setTitle("Validate Datastore");
                    button.setWidget("button");
                    button.setTriggers(singletonList(trigger));
                    synchronized (items) {
                        items.add(button);
                    }
                });
    }
}
