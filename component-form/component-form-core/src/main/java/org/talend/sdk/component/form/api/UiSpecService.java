/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.talend.sdk.component.form.api;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;

import org.talend.sdk.component.form.internal.converter.PropertyContext;
import org.talend.sdk.component.form.internal.converter.impl.JsonSchemaConverter;
import org.talend.sdk.component.form.internal.converter.impl.PropertiesConverter;
import org.talend.sdk.component.form.internal.converter.impl.UiSchemaConverter;
import org.talend.sdk.component.form.model.Ui;
import org.talend.sdk.component.form.model.jsonschema.JsonSchema;
import org.talend.sdk.component.server.front.model.ActionReference;
import org.talend.sdk.component.server.front.model.ComponentDetail;
import org.talend.sdk.component.server.front.model.ConfigTypeNode;
import org.talend.sdk.component.server.front.model.SimplePropertyDefinition;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UiSpecService implements AutoCloseable {

    private final Client client;

    private final Jsonb jsonb;

    private final boolean closeJsonb;

    public UiSpecService(final Client client) {
        this.client = client;
        this.jsonb = JsonbBuilder.create(new JsonbConfig().setProperty("johnzon.cdi.activated", false));
        this.closeJsonb = true;
    }

    public UiSpecService(final Client client, final Jsonb jsonb) {
        this.client = client;
        this.jsonb = jsonb;
        this.closeJsonb = false;
    }

    /**
     * Converts a configuration model to a uiSpec.
     *
     * @param family the family (you must browse the configuration tree to find the root parent id).
     * @param node the configuration model to convert to a uiSpec.
     * @return a Ui promise.
     */
    public CompletionStage<Ui> convert(final String family, final ConfigTypeNode node) {
        // extract root property
        final Collection<String> rootProperties =
                node.getProperties().stream().map(SimplePropertyDefinition::getPath).collect(toSet());
        final Iterator<String> it = rootProperties.iterator();
        while (it.hasNext()) {
            final String path = it.next() + '.';
            if (node.getProperties().stream().noneMatch(p -> p.getPath().startsWith(path))) {
                it.remove();
            }
        }
        if (rootProperties.isEmpty()) {
            log.warn("No root properties for configuration node {} (family={})", node.getId(), family);
        }

        // [TCOMP-767] 0.0.7 -> 0.0.8 compat
        final Collection<SimplePropertyDefinition> props;
        if (rootProperties.size() == 1 && "configuration".equals(rootProperties.iterator().next())) {
            final SimplePropertyDefinition def = node
                    .getProperties()
                    .stream()
                    .filter(prop -> prop.getPath().equals("configuration"))
                    .findFirst()
                    .get();
            if ("configuration".equals(def.getName())) {
                props = node.getProperties();
            } else {
                props = node
                        .getProperties()
                        .stream()
                        .map(prop -> new SimplePropertyDefinition(
                                def.getName() + prop.getPath().substring("configuration".length()), prop.getName(),
                                prop.getDisplayName(), prop.getType(), prop.getDefaultValue(), prop.getValidation(),
                                prop.getMetadata(), prop.getPlaceholder(), prop.getProposalDisplayNames()))
                        .collect(toList());
            }
        } else {
            props = node.getProperties();
        }

        return convert(node::getDisplayName, () -> family, () -> props, node::getActions,
                p -> rootProperties.contains(p.getPath()));
    }

    /**
     * Converts a component form to a uiSpec.
     *
     * @param detail the component model.
     * @return the uiSpec corresponding to the model.
     */
    public CompletionStage<Ui> convert(final ComponentDetail detail) {
        return convert(detail::getDisplayName, detail.getId()::getFamily, detail::getProperties, detail::getActions,
                p -> p.getName().equals(p.getPath()));
    }

    private CompletionStage<Ui> convert(final Supplier<String> displayName, final Supplier<String> family,
            final Supplier<Collection<SimplePropertyDefinition>> properties,
            final Supplier<Collection<ActionReference>> actions,
            final Predicate<SimplePropertyDefinition> isRootProperty) {
        final Collection<SimplePropertyDefinition> props = properties.get();

        final Ui ui = new Ui();
        ui.setUiSchema(new ArrayList<>());
        ui.setProperties(new HashMap<>());
        ui.setJsonSchema(new JsonSchema());
        ui.getJsonSchema().setTitle(displayName.get());
        ui.getJsonSchema().setType("object");
        ui
                .getJsonSchema()
                .setRequired(props
                        .stream()
                        .filter(isRootProperty)
                        .filter(p -> new PropertyContext(p).isRequired())
                        .map(SimplePropertyDefinition::getName)
                        .collect(toSet()));

        final JsonSchemaConverter jsonSchemaConverter = new JsonSchemaConverter(jsonb, ui.getJsonSchema(), props);
        final UiSchemaConverter uiSchemaConverter = new UiSchemaConverter(null, family.get(), ui.getUiSchema(),
                new ArrayList<>(), client, props, actions.get());
        final PropertiesConverter propertiesConverter =
                new PropertiesConverter(jsonb, Map.class.cast(ui.getProperties()), props);

        return CompletableFuture
                .allOf(props
                        .stream()
                        .filter(Objects::nonNull)
                        .filter(isRootProperty)
                        .map(PropertyContext::new)
                        .map(CompletableFuture::completedFuture)
                        .map(jsonSchemaConverter::convert)
                        .map(uiSchemaConverter::convert)
                        .map(propertiesConverter::convert)
                        .toArray(CompletableFuture[]::new))
                .thenApply(r -> ui);
    }

    @Override
    public void close() throws Exception {
        if (closeJsonb) {
            jsonb.close();
        }
    }
}
