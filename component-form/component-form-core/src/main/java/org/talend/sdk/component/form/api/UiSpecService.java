/**
 * Copyright (C) 2006-2017 Talend Inc. - www.talend.com
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

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static java.util.Locale.ENGLISH;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

import org.talend.sdk.component.form.model.UiSpecPayload;
import org.talend.sdk.component.server.front.model.ActionReference;
import org.talend.sdk.component.server.front.model.ComponentDetail;
import org.talend.sdk.component.server.front.model.PropertyValidation;
import org.talend.sdk.component.server.front.model.SimplePropertyDefinition;

// todo: support @Group and all the elements of the API
public class UiSpecService {

    private final Client client;

    public UiSpecService(final Client client) {
        this.client = client;
    }

    public UiSpecPayload convert(final ComponentDetail detail) {
        final UiSpecPayload converted = new UiSpecPayload();
        { // build the jsonschema
            final UiSpecPayload.JsonSchema jsonSchema = new UiSpecPayload.JsonSchema();
            jsonSchema.setTitle(detail.getDisplayName());
            jsonSchema.setType("object");
            jsonSchema.setProperties(createJsonSchemaPropertiesLevel("", detail.getProperties()));
            jsonSchema.setRequired(detail
                    .getProperties()
                    .stream()
                    .filter(p -> p.getName().equals(p.getPath()))
                    .filter(this::isRequired)
                    .map(SimplePropertyDefinition::getName)
                    .collect(toSet()));
            converted.setJsonSchema(jsonSchema);
        }
        { // build ui spec based on the object model
            converted.setUiSchema(createUiSchemaLevel(detail, "", detail.getProperties()));
        }
        { // build properties - default values in the case of this logic
            converted.setProperties(createPropertiesLevel("", detail.getProperties()));
        }
        return converted;
    }

    private Map<String, Object> createPropertiesLevel(final String prefix,
            final Collection<SimplePropertyDefinition> properties) {
        if (properties == null) {
            return null;
        }
        // todo: array
        return filterPropertyLevel(prefix, properties)
                .filter(p -> "object".equalsIgnoreCase(p.getType())
                        || p.getMetadata().get("ui::defaultvalue::value") != null)
                .collect(toMap(SimplePropertyDefinition::getName, p -> {
                    if ("object".equalsIgnoreCase(p.getType())) {
                        return createPropertiesLevel(p.getPath() + '.', properties);
                    }
                    final String def = p.getMetadata().get("ui::defaultvalue::value");
                    if ("number".equalsIgnoreCase(p.getType())) {
                        return Double.parseDouble(def);
                    }
                    if ("boolean".equalsIgnoreCase(p.getType())) {
                        return Boolean.parseBoolean(def);
                    }
                    return def;
                }))
                .entrySet()
                .stream()
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Collection<UiSpecPayload.UiSchema> createUiSchemaLevel(final ComponentDetail detail, final String prefix,
            final Collection<SimplePropertyDefinition> properties) {
        if (properties == null) {
            return null;
        }
        return filterPropertyLevel(prefix, properties).map(prop -> toUiSchema(detail, properties, prop)).collect(
                toList());
    }

    private UiSpecPayload.UiSchema toUiSchema(final ComponentDetail detail,
            final Collection<SimplePropertyDefinition> properties, final SimplePropertyDefinition prop) {
        if ("object".equalsIgnoreCase(prop.getType())) {
            final UiSpecPayload.UiSchema schema = new UiSpecPayload.UiSchema();
            // schema.setKey(prop.getPath());
            schema.setTitle(prop.getDisplayName());
            schema.setWidget("fieldset");
            final Collection<UiSpecPayload.UiSchema> items =
                    createUiSchemaLevel(detail, prop.getPath() + '.', properties);
            schema.setItems(items);

            // add common actions if needed
            ofNullable(prop.getMetadata().get("action::schema"))
                    .flatMap(v -> detail
                            .getActions()
                            .stream()
                            .filter(a -> a.getName().equals(v) && "schema".equals(a.getType()))
                            .findFirst())
                    .ifPresent(ref -> {
                        final UiSpecPayload.Trigger trigger = toTrigger(properties, prop, ref);
                        if (trigger.getParameters() == null || trigger.getParameters().isEmpty()) {
                            // find the matching dataset
                            detail
                                    .getProperties()
                                    .stream()
                                    .filter(p -> ref.getName().equals(p.getMetadata().get("dataset")))
                                    .findFirst()
                                    .ifPresent(dataset -> {
                                        final UiSpecPayload.Trigger.Parameter parameter =
                                                new UiSpecPayload.Trigger.Parameter();
                                        parameter.setKey("dataset");
                                        parameter.setPath(prop.getPath());
                                        trigger.setParameters(toParams(properties, prop, ref, prop.getPath()));
                                    });
                        }

                        final UiSpecPayload.UiSchema button = new UiSpecPayload.UiSchema();
                        button.setKey("button_schema_" + prop.getPath());
                        button.setTitle("Guess Schema");
                        button.setWidget("button");
                        button.setTriggers(singletonList(trigger));
                        items.add(button);
                    });
            ofNullable(prop.getMetadata().get("action::healthcheck"))
                    .flatMap(v -> (detail.getActions() == null ? Stream.<ActionReference> empty()
                            : detail.getActions().stream())
                                    .filter(a -> a.getName().equals(v) && "healthcheck".equals(a.getType()))
                                    .findFirst())
                    .ifPresent(ref -> {
                        final UiSpecPayload.Trigger trigger = toTrigger(properties, prop, ref);
                        if (trigger.getParameters() == null || trigger.getParameters().isEmpty()) {
                            // find the matching dataset
                            detail
                                    .getProperties()
                                    .stream()
                                    .filter(p -> ref.getName().equals(p.getMetadata().get("datastore")))
                                    .findFirst()
                                    .ifPresent(datastore -> trigger
                                            .setParameters(toParams(properties, datastore, ref, datastore.getPath())));
                        }

                        final UiSpecPayload.UiSchema button = new UiSpecPayload.UiSchema();
                        button.setKey("button_healthcheck_" + prop.getPath());
                        button.setTitle("Validate Datastore");
                        button.setWidget("button");
                        button.setTriggers(singletonList(trigger));
                        items.add(button);
                    });

            return schema;
        } // array: todo
        return primitiveToUiSchema(properties, detail, prop);
    }

    private UiSpecPayload.Trigger toTrigger(final Collection<SimplePropertyDefinition> properties,
            final SimplePropertyDefinition prop, final ActionReference ref) {
        final UiSpecPayload.Trigger trigger = new UiSpecPayload.Trigger();
        trigger.setAction(ref.getName());
        trigger.setFamily(ref.getFamily());
        trigger.setType(ref.getType());
        trigger.setParameters(
                toParams(properties, prop, ref, prop.getMetadata().get("action::" + ref.getType() + "::parameters")));
        return trigger;
    }

    private UiSpecPayload.UiSchema primitiveToUiSchema(final Collection<SimplePropertyDefinition> properties,
            final ComponentDetail detail, final SimplePropertyDefinition prop) {
        final UiSpecPayload.UiSchema schema = new UiSpecPayload.UiSchema();
        schema.setKey(prop.getPath());
        schema.setTitle(prop.getDisplayName());
        if ("true".equalsIgnoreCase(prop.getMetadata().get("ui::credential"))) {
            schema.setWidget("text");
            schema.setType("password");
        } else if (prop.getMetadata().containsKey("ui::code::value")) {
            final String codeLang = prop.getMetadata().get("ui::code::value");
            schema.setWidget("code");
            schema.setOptions(singletonMap("language", codeLang));
        } else if ("boolean".equalsIgnoreCase(prop.getType())) {
            schema.setWidget("toggle");
        } else if ("enum".equalsIgnoreCase(prop.getType())) {
            schema.setWidget("datalist");
            schema.setTitleMap(prop.getValidation().getEnumValues().stream().sorted().map(v -> {
                final UiSpecPayload.NameValue nameValue = new UiSpecPayload.NameValue();
                nameValue.setName(v);
                nameValue.setValue(v);
                return nameValue;
            }).collect(toList()));

            final UiSpecPayload.JsonSchema jsonSchema = new UiSpecPayload.JsonSchema();
            jsonSchema.setType("string");
            jsonSchema.setEnumValues(prop.getValidation().getEnumValues());
            schema.setSchema(jsonSchema);
        } else if ("number".equalsIgnoreCase(prop.getType())) {
            schema.setWidget("text"); // todo: check it is ok, looks so but also not that common so can need some
                                      // revisit
        } else if (prop.getMetadata() != null && prop.getMetadata().containsKey("action::dynamic_values")) {
            schema.setWidget("datalist");
            schema.setRestricted(false);

            final UiSpecPayload.JsonSchema jsonSchema = new UiSpecPayload.JsonSchema();
            jsonSchema.setType("string");
            schema.setSchema(jsonSchema);

            if (client != null) {
                // todo: add caching here with a small eviction (each 5mn?)
                final Map<String, Object> values = client.action(detail.getId().getFamily(), "dynamic_values",
                        prop.getMetadata().get("action::dynamic_values"), emptyMap());

                final List<UiSpecPayload.NameValue> namedValues =
                        ofNullable(values).map(v -> v.get("items")).filter(Collection.class::isInstance).map(c -> {
                            final Collection<?> dynamicValues = Collection.class.cast(c);
                            return dynamicValues
                                    .stream()
                                    .filter(Map.class::isInstance)// .map(m ->
                                                                  // Map.class.cast(m).get("id"))
                                    .filter(m -> Map.class.cast(m).get("id") != null
                                            && Map.class.cast(m).get("id") instanceof String)
                                    .map(Map.class::cast)
                                    .map(entry -> {
                                        UiSpecPayload.NameValue val = new UiSpecPayload.NameValue();
                                        val.setName((String) entry.get("id"));
                                        val.setValue(entry.get("label") == null ? (String) entry.get("id")
                                                : (String) entry.get("label"));
                                        return val;
                                    })
                                    .collect(toList());
                        }).orElse(emptyList());
                schema.setTitleMap(namedValues);
                jsonSchema.setEnumValues(namedValues.stream().map(c -> c.getName()).sorted().collect(toList()));
            } else {
                schema.setTitleMap(emptyList());
                jsonSchema.setEnumValues(emptyList());
            }
        } else {
            schema.setWidget("text");
        }
        schema.setRequired(isRequired(prop));
        schema.setPlaceholder(prop.getName() + " ...");

        if (detail.getActions() != null) {
            ofNullable(prop.getMetadata().get("action::validation"))
                    .flatMap(v -> detail
                            .getActions()
                            .stream()
                            .filter(a -> a.getName().equals(v) && "validation".equals(a.getType()))
                            .findFirst())
                    .ifPresent(ref -> {
                        schema.setTriggers(singletonList(toTrigger(properties, prop, ref)));
                    });
        }

        return schema;
    }

    private boolean isRequired(final SimplePropertyDefinition prop) {
        return prop.getValidation() != null && prop.getValidation().getRequired() != null
                && prop.getValidation().getRequired();
    }

    private List<UiSpecPayload.Trigger.Parameter> toParams(final Collection<SimplePropertyDefinition> properties,
            final SimplePropertyDefinition prop, final ActionReference ref, final String parameters) {
        final Iterator<SimplePropertyDefinition> expectedProperties = ref.getProperties().iterator();
        return ofNullable(parameters).map(params -> Stream.of(params.split(",")).flatMap(paramRef -> {
            if (!expectedProperties.hasNext()) {
                return Stream.empty();
            }
            final String parameterPrefix = expectedProperties.next().getPath();
            final String propertiesPrefix = resolveProperty(prop, paramRef);
            final List<UiSpecPayload.Trigger.Parameter> resolvedParams = properties
                    .stream()
                    .filter(p -> p.getPath().startsWith(propertiesPrefix))
                    .filter(o -> !"object".equalsIgnoreCase(o.getType()) && !"array".equalsIgnoreCase(o.getType()))
                    .map(o -> {
                        final UiSpecPayload.Trigger.Parameter parameter = new UiSpecPayload.Trigger.Parameter();
                        parameter.setKey(parameterPrefix + o.getPath().substring(propertiesPrefix.length()));
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

    private String resolveProperty(final SimplePropertyDefinition prop, final String paramRef) {
        if (".".equals(paramRef)) {
            return prop.getPath();
        }
        if (paramRef.startsWith("..")) {
            String current = prop.getPath();
            String ref = paramRef;
            while (ref.startsWith("..")) {
                final int lastDot = current.lastIndexOf('.');
                if (lastDot < 0) {
                    break;
                }
                current = current.substring(0, lastDot);
                ref = ref.substring("..".length(), ref.length());
                if (ref.startsWith("/")) {
                    ref = ref.substring(1);
                }
            }
            return current + (!ref.isEmpty() ? "." : "") + ref.replace('/', '.');
        }
        if (paramRef.startsWith(".") || paramRef.startsWith("./")) {
            return prop.getPath() + '.' + paramRef.replaceFirst("\\./?", "").replace('/', '/');
        }
        return paramRef;
    }

    private Map<String, UiSpecPayload.JsonSchema> createJsonSchemaPropertiesLevel(final String prefix,
            final Collection<SimplePropertyDefinition> properties) {
        if (properties == null) {
            return null;
        }
        return filterPropertyLevel(prefix, properties).collect(toMap(SimplePropertyDefinition::getName, prop -> {
            final UiSpecPayload.JsonSchema property = new UiSpecPayload.JsonSchema();
            property.setTitle(prop.getDisplayName());

            switch (prop.getType().toUpperCase(ENGLISH)) { // tcomp meta model
            case "ENUM": {
                property.setType("string");
                property.setEnumValues(prop.getValidation().getEnumValues());
                break;
            }
            default:
                property.setType(prop.getType().toLowerCase(ENGLISH));
            }

            final Map<String, UiSpecPayload.JsonSchema> nestedProperties =
                    createJsonSchemaPropertiesLevel(prop.getPath() + '.', properties);
            final String order = prop.getMetadata().get("ui::optionsorder::value");
            if (order != null) {
                property.setProperties(new TreeMap<String, UiSpecPayload.JsonSchema>(new Comparator<String>() {

                    private final List<String> propertiesOrder = new ArrayList<>(asList(order.split(",")));

                    @Override
                    public int compare(final String o1, final String o2) {
                        final int i = propertiesOrder.indexOf(o1) - propertiesOrder.indexOf(o2);
                        return i == 0 ? o1.compareTo(o2) : i;
                    }
                }) {

                    {
                        putAll(nestedProperties);
                    }
                });
            } else if (!nestedProperties.isEmpty()) {
                property.setProperties(nestedProperties);
            }

            if ("object".equalsIgnoreCase(prop.getType())) {
                property.setRequired(properties
                        .stream()
                        .filter(p -> nestedProperties.keySet().contains(p.getName())
                                && p.getPath().equals(prop.getPath() + '.' + p.getName()))
                        .filter(this::isRequired)
                        .map(SimplePropertyDefinition::getName)
                        .collect(toSet()));
            }

            ofNullable(prop.getMetadata().get("ui::defaultvalue::value")).ifPresent(property::setDefaultValue);

            final PropertyValidation validation = prop.getValidation();
            if (validation != null) {
                ofNullable(validation.getMin()).ifPresent(m -> property.setMinimum(m.doubleValue()));
                ofNullable(validation.getMax()).ifPresent(m -> property.setMaximum(m.doubleValue()));
                ofNullable(validation.getMinItems()).ifPresent(property::setMinItems);
                ofNullable(validation.getMaxItems()).ifPresent(property::setMaxItems);
                ofNullable(validation.getMinLength()).ifPresent(property::setMinLength);
                ofNullable(validation.getMaxLength()).ifPresent(property::setMaxLength);
                ofNullable(validation.getUniqueItems()).ifPresent(property::setUniqueItems);
                ofNullable(validation.getPattern()).ifPresent(property::setPattern);
            }

            return property;
        }));
    }

    private Stream<SimplePropertyDefinition> filterPropertyLevel(final String prefix,
            final Collection<SimplePropertyDefinition> properties) {
        return properties.stream().filter(
                p -> p.getPath().startsWith(prefix) && p.getPath().indexOf('.', prefix.length()) < 0);
    }
}
