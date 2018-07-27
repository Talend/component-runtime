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
import static java.util.Locale.ROOT;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.stream.Stream;

import org.talend.sdk.component.form.api.Client;
import org.talend.sdk.component.form.internal.converter.PropertyContext;
import org.talend.sdk.component.form.internal.converter.PropertyConverter;
import org.talend.sdk.component.form.internal.converter.impl.widget.path.AbsolutePathResolver;
import org.talend.sdk.component.form.model.jsonschema.JsonSchema;
import org.talend.sdk.component.form.model.uischema.UiSchema;
import org.talend.sdk.component.server.front.model.ActionReference;
import org.talend.sdk.component.server.front.model.SimplePropertyDefinition;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public abstract class AbstractWidgetConverter implements PropertyConverter {

    private final AbsolutePathResolver pathResolver = new AbsolutePathResolver();

    protected final Collection<UiSchema> schemas;

    protected final Collection<SimplePropertyDefinition> properties;

    protected final Collection<ActionReference> actions;

    protected final JsonSchema jsonSchema;

    protected final String lang;

    protected <T> CompletionStage<List<UiSchema.NameValue>> loadDynamicValues(final Client<T> client,
            final String family, final String actionName, final T context) {
        return client.action(family, "dynamic_values", actionName, lang, emptyMap(), context).exceptionally(e -> {
            log.warn(e.getMessage(), e);
            return emptyMap();
        }).thenApply(
                values -> ofNullable(values).map(v -> v.get("items")).filter(Collection.class::isInstance).map(c -> {
                    final Collection<?> dynamicValues = Collection.class.cast(c);
                    return dynamicValues
                            .stream()
                            .filter(Map.class::isInstance)
                            .filter(m -> Map.class.cast(m).get("id") != null
                                    && Map.class.cast(m).get("id") instanceof String)
                            .map(Map.class::cast)
                            .map(entry -> {
                                final UiSchema.NameValue val = new UiSchema.NameValue();
                                val.setName(entry.get("label") == null ? (String) entry.get("id")
                                        : String.class.cast(entry.get("label")));
                                val.setValue(String.class.cast(entry.get("id")));
                                return val;
                            })
                            .collect(toList());
                }).orElse(emptyList()));
    }

    protected UiSchema.Trigger toTrigger(final Collection<SimplePropertyDefinition> properties,
            final SimplePropertyDefinition prop, final ActionReference ref) {
        final UiSchema.Trigger trigger = new UiSchema.Trigger();
        trigger.setAction(ref.getName());
        trigger.setFamily(ref.getFamily());
        trigger.setType(ref.getType());
        trigger.setParameters(
                toParams(properties, prop, ref, prop.getMetadata().get("action::" + ref.getType() + "::parameters")));
        return trigger;
    }

    protected List<UiSchema.Parameter> toParams(final Collection<SimplePropertyDefinition> properties,
            final SimplePropertyDefinition prop, final ActionReference ref, final String parameters) {
        final Iterator<SimplePropertyDefinition> expectedProperties = ref.getProperties().iterator();
        return ofNullable(parameters).map(params -> Stream.of(params.split(",")).flatMap(paramRef -> {
            if (!expectedProperties.hasNext()) {
                return Stream.empty();
            }
            final String parameterPrefix = expectedProperties.next().getPath();
            final String propertiesPrefix = pathResolver.resolveProperty(prop.getPath(), paramRef);
            final List<UiSchema.Parameter> resolvedParams = properties
                    .stream()
                    .filter(p -> p.getPath().startsWith(propertiesPrefix))
                    .filter(o -> !"object".equalsIgnoreCase(o.getType()) && !"array".equalsIgnoreCase(o.getType()))
                    .map(o -> {
                        final UiSchema.Parameter parameter = new UiSchema.Parameter();
                        final String key = parameterPrefix + o.getPath().substring(propertiesPrefix.length());
                        parameter.setKey(key.replace("[]", "")); // not a lodash path otherwise
                        parameter.setPath(o.getPath());
                        return parameter;
                    })
                    .collect(toList());
            if (resolvedParams.isEmpty()) {
                throw new IllegalArgumentException("No resolved parameters for " + prop.getPath() + " in "
                        + ref.getFamily() + "/" + ref.getType() + "/" + ref.getName());
            }
            return resolvedParams.stream();
        }).collect(toList())).orElse(null);
    }

    protected UiSchema newUiSchema(final PropertyContext ctx) {
        final UiSchema schema = newOrphanSchema(ctx);
        synchronized (schemas) {
            schemas.add(schema);
        }
        return schema;
    }

    protected UiSchema newOrphanSchema(final PropertyContext ctx) {
        final UiSchema schema = new UiSchema();
        schema.setTitle(ctx.getProperty().getDisplayName());
        schema.setKey(ctx.getProperty().getPath());
        schema.setRequired(ctx.isRequired());
        schema.setPlaceholder(ctx.getProperty().getPlaceholder());
        if (actions != null) {
            final List<UiSchema.Trigger> triggers = Stream
                    .concat(Stream.concat(createValidationTrigger(ctx.getProperty()),
                            createOtherActions(ctx.getProperty())), createSuggestionTriggers(ctx.getProperty()))
                    .collect(toList());
            if (!triggers.isEmpty()) {
                schema.setTriggers(triggers);
            }
        }
        schema.setConditions(createConditions(ctx));
        return schema;
    }

    private Stream<UiSchema.Trigger> createSuggestionTriggers(final SimplePropertyDefinition property) {
        return ofNullable(property.getMetadata().get("action::suggestions"))
                .flatMap(v -> actions
                        .stream()
                        .filter(a -> actionMatch(v, a) && "suggestions".equals(a.getType()))
                        .findFirst())
                .map(ref -> Stream.of(toTrigger(properties, property, ref)).peek(
                        trigger -> trigger.setOnEvent("focus")))
                .orElseGet(Stream::empty);
    }

    private boolean actionMatch(final String name, final ActionReference action) {
        return deParameterize(action.getName()).equals(deParameterize(name));
    }

    private String deParameterize(final String actionName) {
        if (actionName == null) {
            return null;
        }
        if (isParameterizedAction(actionName)) {
            return actionName.substring(0, actionName.indexOf('('));
        }
        return actionName;
    }

    // see the proxy but it allows to have actions with () and still match
    private boolean isParameterizedAction(final String name) {
        final int start = name.indexOf('(');
        if (start <= 0) {
            return false;
        }
        final int end = name.indexOf('(', start);
        return end > 0;
    }

    private Stream<UiSchema.Trigger> createOtherActions(final SimplePropertyDefinition property) {
        return property
                .getMetadata()
                .entrySet()
                .stream()
                .filter(it -> it.getKey().startsWith("action::") && !isBuiltInAction(it.getKey()))
                .map(v -> actions
                        .stream()
                        .filter(a -> actionMatch(v.getValue(), a)
                                && v.getKey().substring("action::".length()).equals(a.getType()))
                        .findFirst()
                        .map(ref -> toTrigger(properties, property, ref))
                        .orElse(null))
                .filter(Objects::nonNull);
    }

    private Stream<UiSchema.Trigger> createValidationTrigger(final SimplePropertyDefinition property) {
        return ofNullable(property.getMetadata().get("action::validation"))
                .flatMap(v -> actions
                        .stream()
                        .filter(a -> actionMatch(v, a) && "validation".equals(a.getType()))
                        .findFirst())
                .map(ref -> Stream.of(toTrigger(properties, property, ref)))
                .orElseGet(Stream::empty);
    }

    // means the actions is processed in the converter in a custom fashion and doesn't need to be passthrough to the
    // client
    private boolean isBuiltInAction(final String key) {
        return key.equals("action::dynamic_values") || key.equals("action::healthcheck")
                || key.equals("action::validation") || key.equals("action::suggestions");
    }

    protected List<UiSchema.Condition> createConditions(final PropertyContext ctx) {
        return ctx
                .getProperty()
                .getMetadata()
                .entrySet()
                .stream()
                .filter(e -> e.getKey().startsWith("condition::if::target"))
                .map(e -> {
                    final String[] split = e.getKey().split("::");
                    final String keySuffix = split.length == 4 ? "::" + split[split.length - 1] : "";
                    final String valueKey = "condition::if::value" + keySuffix;
                    final String negateKey = "condition::if::negate" + keySuffix;
                    final String strategyKey = "condition::if::evaluationStrategy" + keySuffix;
                    final String paramRef = e.getValue();
                    final String path = pathResolver.resolveProperty(ctx.getProperty().getPath(), paramRef);
                    final SimplePropertyDefinition definition =
                            properties.stream().filter(p -> p.getPath().equals(path)).findFirst().orElse(null);
                    final Function<String, Object> converter = findStringValueMapper(definition);
                    return new UiSchema.Condition.Builder()
                            .withPath(path)
                            .withShouldBe(!Boolean
                                    .parseBoolean(ctx.getProperty().getMetadata().getOrDefault(negateKey, "false")))
                            .withStrategy(ctx.getProperty().getMetadata().get(strategyKey))
                            .withValues(Stream
                                    .of(ctx.getProperty().getMetadata().getOrDefault(valueKey, "true").split(","))
                                    .map(converter)
                                    .collect(toSet()))
                            .build();
                })
                .collect(toList());
    }

    private Function<String, Object> findStringValueMapper(final SimplePropertyDefinition definition) {
        if (definition == null) {
            return s -> s;
        }
        switch (definition.getType().toLowerCase(ROOT)) {
        case "boolean":
            return Boolean::parseBoolean;
        case "number":
            return Double::parseDouble;

        // assume object and array are not supported
        default:
            return s -> s;
        }
    }

    protected JsonSchema findJsonSchema(final PropertyContext<?> cs) {
        final String[] segments = cs.getProperty().getPath().split("\\.");
        JsonSchema schema = jsonSchema;
        for (final String current : segments) {
            if (current.endsWith("[]")) {
                schema = schema.getProperties().get(current.substring(0, current.length() - "[]".length())).getItems();
            } else {
                schema = schema.getProperties().get(current);
            }
            if (schema != null && "array".equals(schema.getType()) && schema.getItems() != null) {
                schema = schema.getItems();
            }
            if (schema == null) { // unexpected
                log.warn("Didn't find json schema for {}", cs.getProperty().getPath());
                return null;
            }
        }
        return schema;
    }
}
