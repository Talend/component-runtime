/**
 * Copyright (C) 2006-2022 Talend Inc. - www.talend.com
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

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;

import org.talend.sdk.component.form.internal.converter.CustomPropertyConverter;
import org.talend.sdk.component.form.internal.converter.PropertyContext;
import org.talend.sdk.component.form.internal.converter.impl.JsonSchemaConverter;
import org.talend.sdk.component.form.internal.converter.impl.PropertiesConverter;
import org.talend.sdk.component.form.internal.converter.impl.UiSchemaConverter;
import org.talend.sdk.component.form.internal.lang.CompletionStages;
import org.talend.sdk.component.form.model.Ui;
import org.talend.sdk.component.form.model.jsonschema.JsonSchema;
import org.talend.sdk.component.server.front.model.ActionReference;
import org.talend.sdk.component.server.front.model.ComponentDetail;
import org.talend.sdk.component.server.front.model.ConfigTypeNode;
import org.talend.sdk.component.server.front.model.SimplePropertyDefinition;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UiSpecService<T> implements AutoCloseable {

    private final Client client;

    private final Jsonb jsonb;

    private final boolean closeJsonb;

    private final Collection<CustomPropertyConverter> customPropertyConverters = new ArrayList<>();

    @Setter // optional config, false by default until it is on by default in the UI
    private PropertyContext.Configuration configuration = new PropertyContext.Configuration(false);

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

    public UiSpecService<T> withConverter(final CustomPropertyConverter converter) {
        customPropertyConverters.add(converter);
        return this;
    }

    /**
     * Converts a configuration model to a uiSpec.
     *
     * @param family the family (you must browse the configuration tree to find the root parent id).
     * @param node the configuration model to convert to a uiSpec.
     * @param context an optional custom context to propagate some parameters.
     * @return a Ui promise.
     */
    public CompletionStage<Ui> convert(final String family, final String lang, final ConfigTypeNode node,
            final T context) {
        // extract root properties
        final Collection<String> rootProperties =
                node.getProperties().stream().map(SimplePropertyDefinition::getPath).collect(toSet());
        rootProperties
                .removeIf(path -> node.getProperties().stream().anyMatch(p -> path.startsWith(p.getPath() + '.')));
        if (rootProperties.isEmpty()) {
            log.warn("No root properties for configuration node {} (family={})", node.getId(), family);
        }

        // [TCOMP-767] 0.0.7 -> 0.0.8 compat
        final Collection<SimplePropertyDefinition> props;
        final Predicate<SimplePropertyDefinition> isRootProperty;
        if (rootProperties.size() == 1 && rootProperties.iterator().next().startsWith("configuration.")) {
            final String root = rootProperties.iterator().next();
            final SimplePropertyDefinition def =
                    node.getProperties().stream().filter(prop -> prop.getPath().equals(root)).findFirst().get();
            props = node
                    .getProperties()
                    .stream()
                    .map(prop -> new SimplePropertyDefinition(def.getName() + prop.getPath().substring(root.length()),
                            prop.getName(), prop.getDisplayName(), prop.getType(), prop.getDefaultValue(),
                            prop.getValidation(), prop.getMetadata(), prop.getPlaceholder(),
                            prop.getProposalDisplayNames()))
                    .collect(toList());
            isRootProperty = p -> p.getPath().equals(def.getName());
        } else {
            props = node.getProperties();
            isRootProperty = p -> rootProperties.contains(p.getPath());
        }

        return convert(node::getDisplayName, () -> family, () -> props, node::getActions, isRootProperty, context,
                lang);
    }

    /**
     * Converts a component form to a uiSpec.
     *
     * @param detail the component model.
     * @param context an optional custom context to propagate some parameters.
     * @return the uiSpec corresponding to the model.
     */
    public CompletionStage<Ui> convert(final ComponentDetail detail, final String lang, final T context) {
        return convert(detail::getDisplayName, detail.getId()::getFamily, detail::getProperties, detail::getActions,
                p -> p.getName().equals(p.getPath()), context, lang);
    }

    private CompletionStage<Ui> convert(final Supplier<String> displayName, final Supplier<String> family,
            final Supplier<Collection<SimplePropertyDefinition>> properties,
            final Supplier<Collection<ActionReference>> actions,
            final Predicate<SimplePropertyDefinition> isRootProperty, final T context, final String lang) {
        final Collection<SimplePropertyDefinition> props = properties.get();

        final Ui ui = new Ui();
        ui.setUiSchema(new ArrayList<>());
        ui.setProperties(new TreeMap<>());
        ui.setJsonSchema(new JsonSchema());
        ui.getJsonSchema().setTitle(displayName.get());
        ui.getJsonSchema().setType("object");
        ui
                .getJsonSchema()
                .setRequired(props
                        .stream()
                        .filter(isRootProperty)
                        .filter(p -> new PropertyContext<>(p, context, configuration).isRequired())
                        .map(SimplePropertyDefinition::getName)
                        .collect(toSet()));

        final JsonSchemaConverter jsonSchemaConverter = new JsonSchemaConverter(jsonb, ui.getJsonSchema(), props);
        final UiSchemaConverter uiSchemaConverter =
                new UiSchemaConverter(null, family.get(), ui.getUiSchema(), new ArrayList<>(), client,
                        ui.getJsonSchema(), props, actions.get(), lang, customPropertyConverters, new AtomicInteger(1));
        final PropertiesConverter propertiesConverter =
                new PropertiesConverter(jsonb, Map.class.cast(ui.getProperties()), props);

        return CompletableFuture
                .allOf(props
                        .stream()
                        .filter(Objects::nonNull)
                        .filter(isRootProperty)
                        .map(it -> new PropertyContext<>(it, context, configuration))
                        .map(CompletionStages::toStage)
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
