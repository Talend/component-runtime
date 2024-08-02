/**
 * Copyright (C) 2006-2024 Talend Inc. - www.talend.com
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

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Locale.ROOT;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Stream;

import org.talend.sdk.component.form.internal.converter.PropertyContext;
import org.talend.sdk.component.form.model.jsonschema.JsonSchema;
import org.talend.sdk.component.form.model.uischema.UiSchema;
import org.talend.sdk.component.server.front.model.ActionReference;
import org.talend.sdk.component.server.front.model.SimplePropertyDefinition;

import lombok.extern.slf4j.Slf4j;

@Slf4j
abstract class ObjectWidgetConverter extends AbstractWidgetConverter {

    protected final AtomicInteger idGenerator;

    ObjectWidgetConverter(final Collection<UiSchema> schemas, final Collection<SimplePropertyDefinition> properties,
            final Collection<ActionReference> actions, final JsonSchema jsonSchema, final String lang,
            final AtomicInteger idGenerator) {
        super(schemas, properties, actions, jsonSchema, lang);
        this.idGenerator = idGenerator;
    }

    protected void addActions(final PropertyContext<?> root, final UiSchema uiSchema,
            final Collection<SimplePropertyDefinition> includedProperties) {
        final Collection<UiSchema> buttons = new ArrayList<>();

        includedProperties
                .stream()
                .filter(p -> "OUT".equals(p.getMetadata().get("ui::structure::type")))
                .findFirst()
                .ifPresent(schemaBinding -> addGuessSchemaButton(root, schemaBinding, buttons));
        addHealthCheck(root, buttons);
        addUpdate(root).ifPresent(button -> {
            final String name = root.getProperty().getMetadata().get("action::update::after");
            if (name == null) {
                buttons.add(button);
            } else {
                findChild(uiSchema, name).map(element -> {
                    final Collection<UiSchema> items = element.getItems();
                    if (items != null) {
                        items.add(element);
                        return element;
                    }
                    final UiSchema elementCopy = copy(element);
                    reset(element);
                    element.setWidget("fieldset");
                    element.setItems(asList(elementCopy, button));
                    return element;
                }).orElseGet(() -> { // unlikely
                    log.warn("Didn't find {} in {}", name, root.getProperty().getPath());
                    buttons.add(button);
                    return null;
                });
            }
        });

        uiSchema.getItems().addAll(buttons); // potentially use <Buttons />?
    }

    private UiSchema copy(final UiSchema element) {
        final UiSchema uiSchema = new UiSchema();
        uiSchema.setKey(element.getKey());
        uiSchema.setTitle(element.getTitle());
        uiSchema.setWidget(element.getWidget());
        uiSchema.setType(element.getType());
        uiSchema.setItems(element.getItems());
        uiSchema.setOptions(element.getOptions());
        uiSchema.setAutoFocus(element.getAutoFocus());
        uiSchema.setDisabled(element.getDisabled());
        uiSchema.setReadOnly(element.getReadOnly());
        uiSchema.setRequired(element.getRequired());
        uiSchema.setRestricted(element.getRestricted());
        uiSchema.setPlaceholder(element.getPlaceholder());
        uiSchema.setTriggers(element.getTriggers());
        uiSchema.setTitleMap(element.getTitleMap());
        uiSchema.setDescription(element.getDescription());
        uiSchema.setCondition(element.getCondition());
        return uiSchema;
    }

    private void reset(final UiSchema uiSchema) {
        uiSchema.setKey(null);
        uiSchema.setTitle(null);
        uiSchema.setWidget(null);
        uiSchema.setType(null);
        uiSchema.setItems(null);
        uiSchema.setOptions(null);
        uiSchema.setAutoFocus(null);
        uiSchema.setDisabled(null);
        uiSchema.setReadOnly(null);
        uiSchema.setRequired(null);
        uiSchema.setRestricted(null);
        uiSchema.setPlaceholder(null);
        uiSchema.setTriggers(null);
        uiSchema.setTitleMap(null);
        uiSchema.setDescription(null);
        uiSchema.setCondition(null);
    }

    protected Optional<UiSchema> findChild(final UiSchema uiSchema, final String name) {
        return ofNullable(uiSchema.getItems())
                .orElseGet(Collections::emptyList)
                .stream()
                .flatMap(this::unwrapItems)
                .filter(it -> it.getKey() != null && it.getKey().endsWith("." + name))
                .findFirst();
    }

    private Stream<UiSchema> unwrapItems(final UiSchema ui) {
        if ("tabs".equals(ui.getWidget())) {
            return ui.getItems().stream().flatMap(this::unwrapItems);
        }
        if (("fieldset".equals(ui.getWidget()) || "columns".equals(ui.getWidget())) && ui.getItems() != null) {
            return ui.getItems().stream();
        }
        return Stream.of(ui);
    }

    private Optional<UiSchema> addUpdate(final PropertyContext<?> root) {
        final SimplePropertyDefinition property = root.getProperty();
        return ofNullable(property.getMetadata().get("action::update"))
                .flatMap(v -> (actions == null ? Stream.<ActionReference> empty() : actions.stream())
                        .filter(a -> a.getName().equals(v) && "update".equals(a.getType()))
                        .findFirst())
                .map(action -> {
                    final UiSchema.Trigger trigger = toTrigger(properties, root.getProperty(), action);
                    final String path = property.getPath();
                    trigger
                            .setOptions(singletonList(new UiSchema.Option.Builder()
                                    .withPath(path.endsWith("[]") ? path.substring(0, path.length() - "[]".length())
                                            : path)
                                    .withType(property.getType().toLowerCase(ROOT))
                                    .build()));
                    final UiSchema button = new UiSchema();
                    button.setKey(root.getProperty().getPath() + '_' + idGenerator.getAndIncrement());
                    button
                            .setTitle(action.getDisplayName() == null ? action.getName() + " (" + action.getType() + ')'
                                    : action.getDisplayName());
                    button.setWidget("button");
                    button.setTriggers(singletonList(trigger));

                    final String activeIf = root.getProperty().getMetadata().get("action::update::activeIf");
                    if (activeIf != null) {
                        button.setCondition(getCondition(root, path, activeIf));
                    }

                    return button;
                });
    }

    private Map<String, Collection<Object>> getCondition(final PropertyContext<?> root, final String actionPath,
            final String activeIf) {
        final String[] split = activeIf.split(",");
        final String keySuffix = split[0].split("=")[1].trim();
        final String valueKey = "condition::if::value::" + keySuffix;
        final String path = actionPath + "." + keySuffix;
        final SimplePropertyDefinition definition =
                properties.stream().filter(p -> p.getPath().equals(path)).findFirst().orElse(null);
        final Function<String, Object> converter = findStringValueMapper(definition);

        final List<Map<String, Collection<Object>>> values = Stream
                .of(root.getProperty().getMetadata().getOrDefault(valueKey, "true").split(","))
                .map(converter)
                .map(val -> toCondition(path, "", val, definition))
                .collect(toList());
        return values.size() == 1 ? values.iterator().next()
                : new UiSchema.ConditionBuilder().withOperator("or").withValues(values).build();
    }

    private void addHealthCheck(final PropertyContext<?> root, final Collection<UiSchema> items) {
        ofNullable(root.getProperty().getMetadata().get("action::healthcheck"))
                .flatMap(v -> (actions == null ? Stream.<ActionReference> empty() : actions.stream())
                        .filter(a -> a.getName().equals(v) && "healthcheck".equals(a.getType()))
                        .findFirst())
                .ifPresent(ref -> {
                    final UiSchema.Trigger trigger = toTrigger(properties, root.getProperty(), ref);
                    if (trigger.getParameters() == null || trigger.getParameters().isEmpty()) {
                        if ("datastore".equals(root.getProperty().getMetadata().get("configurationtype::type"))) {
                            trigger
                                    .setParameters(toParams(properties, root.getProperty(), ref,
                                            root.getProperty().getPath()));
                        } else {
                            // find the matching datastore
                            properties
                                    .stream()
                                    .filter(nested -> "datastore"
                                            .equals(nested.getMetadata().get("configurationtype::type"))
                                            && ref
                                                    .getName()
                                                    .equals(nested
                                                            .getMetadata()
                                                            .getOrDefault("action::healthcheck",
                                                                    nested
                                                                            .getMetadata()
                                                                            .get("configurationtype::name"))))
                                    .findFirst()
                                    .ifPresent(datastore -> {
                                        final List<UiSchema.Parameter> parameters =
                                                toParams(properties, datastore, ref, datastore.getPath());
                                        if (parameters != null && !parameters.isEmpty()) {
                                            trigger.setParameters(parameters);
                                        } else {
                                            final UiSchema.Parameter parameter = new UiSchema.Parameter();
                                            parameter
                                                    .setKey(ofNullable(ref.getProperties())
                                                            .orElse(Collections.emptyList())
                                                            .stream()
                                                            .filter(p -> !p.getPath().contains("."))
                                                            .findFirst()
                                                            .map(SimplePropertyDefinition::getName)
                                                            .orElse("datastore"));
                                            parameter.setPath(datastore.getPath());
                                            trigger
                                                    .setParameters(
                                                            toParams(properties, datastore, ref, datastore.getPath()));
                                        }
                                    });
                        }
                    }

                    final UiSchema button = new UiSchema();
                    button.setKey(root.getProperty().getPath() + '_' + idGenerator.getAndIncrement());
                    button
                            .setTitle(ref.getDisplayName() == null || ref.getName().equals(ref.getDisplayName())
                                    ? "Validate Connection"
                                    : ref.getDisplayName());
                    button.setWidget("button");
                    button.setTriggers(singletonList(trigger));
                    synchronized (items) {
                        items.add(button);
                    }
                });
    }

    private void addGuessSchemaButton(final PropertyContext<?> root, final SimplePropertyDefinition bindingProp,
            final Collection<UiSchema> items) {
        final String schemaActionName =
                ofNullable(bindingProp.getMetadata().get("action::schema")).filter(n -> !n.isEmpty()).orElse("default");
        actions
                .stream()
                .filter(a -> a.getName().equals(schemaActionName) && "schema".equals(a.getType()))
                .findFirst()
                .ifPresent(ref -> {
                    final UiSchema.Trigger trigger = toTrigger(properties, root.getProperty(), ref);
                    trigger
                            .setOptions(singletonList(new UiSchema.Option.Builder()
                                    .withPath(bindingProp.getPath().replace("[]", ""))
                                    .withType("array".equalsIgnoreCase(bindingProp.getType()) ? "array" : "object")
                                    .build()));
                    if (trigger.getParameters() == null || trigger.getParameters().isEmpty()) {
                        // find the matching dataset
                        Optional<SimplePropertyDefinition> findParameters = properties
                                .stream()
                                .filter(nested -> ref
                                        .getName()
                                        .equals(nested.getMetadata().get("configurationtype::name"))
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
                            parameter
                                    .setKey(ofNullable(ref.getProperties())
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
                    button.setKey(root.getProperty().getPath() + '_' + idGenerator.getAndIncrement());
                    button
                            .setTitle(ref.getDisplayName() == null || ref.getName().equals(ref.getDisplayName())
                                    ? "Guess Schema"
                                    : ref.getDisplayName());
                    button.setWidget("button");
                    button.setTriggers(singletonList(trigger));
                    synchronized (items) {
                        items.add(button);
                    }
                });
    }
}
