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
package org.talend.sdk.component.proxy.service;

import static java.util.Collections.singletonMap;
import static java.util.Comparator.comparing;
import static java.util.Locale.ROOT;
import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static javax.json.stream.JsonCollectors.toJsonArray;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonBuilderFactory;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.spi.JsonProvider;

import org.talend.sdk.component.proxy.api.service.ConfigurationFormatter;
import org.talend.sdk.component.proxy.service.qualifier.UiSpecProxy;
import org.talend.sdk.component.server.front.model.SimplePropertyDefinition;

// Note: this can pby be rewritten using a visitor but keeping it all self contained is easier for now
@ApplicationScoped
public class ConfigurationFormatterImpl implements ConfigurationFormatter {

    @Inject
    @UiSpecProxy
    private JsonBuilderFactory factory;

    @Inject
    @UiSpecProxy
    private JsonProvider jsonp;

    @Override
    public Map<String, String> flatten(final JsonObject form) {
        final Map<String, String> keyValues = new HashMap<>();
        if (form == null || form.isEmpty()) {
            return keyValues;
        }
        return form
                .entrySet()
                .stream()
                .filter(v -> v.getValue().getValueType() != JsonValue.ValueType.NULL)
                .map(this::flattenValue)
                .flatMap(map -> map.entrySet().stream())
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Map<String, String> flattenValue(final Map.Entry<String, JsonValue> e) {
        switch (e.getValue().getValueType()) {
        case STRING:
            return singletonMap(e.getKey(), JsonString.class.cast(e.getValue()).getString());
        case FALSE:
            return singletonMap(e.getKey(), "false");
        case TRUE:
            return singletonMap(e.getKey(), "true");
        case NUMBER:
            return singletonMap(e.getKey(), String.valueOf(JsonNumber.class.cast(e.getValue()).doubleValue()));
        case OBJECT:
            return flatten(JsonObject.class.cast(e.getValue()))
                    .entrySet()
                    .stream()
                    .collect(toMap(it -> e.getKey() + '.' + it.getKey(), Map.Entry::getValue));
        case ARRAY:
            final JsonArray array = JsonArray.class.cast(e.getValue());
            int index = 0;
            final Map<String, String> converted = new HashMap<>();
            for (final JsonValue value : array) {
                if (value.getValueType() != JsonValue.ValueType.NULL) { // ignore null values
                    converted
                            .putAll(flattenValue(new AbstractMap.SimpleEntry<>(e.getKey() + '[' + index + ']', value)));
                }
                index++;
            }
            return converted;
        default:
            throw new IllegalArgumentException("Unsupported entry: " + e.getValue());
        }
    }

    @Override
    public JsonObject unflatten(final Collection<SimplePropertyDefinition> definitions,
            final Map<String, String> config) {
        return unflatten("", definitions, config);
    }

    private JsonObject unflatten(final String prefix, final Collection<SimplePropertyDefinition> definitions,
            final Map<String, String> config) {
        final List<SimplePropertyDefinition> defs = new ArrayList<>(definitions);
        defs.sort(comparing(SimplePropertyDefinition::getPath));

        final JsonObjectBuilder json = factory.createObjectBuilder();

        final Collection<String> matched = new HashSet<>();
        new ArrayList<>(definitions)
                .stream()
                .filter(it -> it.getPath().equals(prefix + it.getName()))
                .peek(it -> matched.add(it.getPath()))
                .forEach(prop -> onProperty(prefix, definitions, config, json, prop));

        // handle virtual *properties* ($xxx) which are not spec-ed, ex: foo.$maxBatchSize
        config
                .entrySet()
                .stream()
                .filter(it -> it.getKey().startsWith("$") && !it.getKey().contains("."))
                .filter(it -> matched.add(prefix + it.getKey())) // if matched from the def (w/ type) don't override it
                .forEach(e -> json.add(e.getKey(), e.getValue()));

        return json.build();
    }

    private void processObject(final Collection<SimplePropertyDefinition> definitions,
            final SimplePropertyDefinition prop, final Map<String, String> config, final JsonObjectBuilder json,
            final String name, final String currentPath) {
        final Map<String, String> subConfig = extractSubConfig(config, name);
        if (!subConfig.isEmpty()) {
            json.add(name, unflatten(currentPath, definitions, subConfig));
        } else {
            new ArrayList<>(definitions)
                    .stream()
                    .filter(it -> isNested(it.getPath(), prop.getPath()))
                    .forEach(p -> onProperty(currentPath, definitions, extractSubConfig(config, p.getName() + '.'),
                            json, p));
        }
    }

    private boolean isNested(final String path, final String path2) {
        return path.startsWith(path2 + '.');
    }

    private Map<String, String> extractSubConfig(final Map<String, String> config, final String prefix) {
        return config
                .entrySet()
                .stream()
                .filter(it -> isNested(it.getKey(), prefix))
                .collect(toMap(it -> it.getKey().substring(prefix.length() + 1), Map.Entry::getValue));
    }

    private void onProperty(final String prefix, final Collection<SimplePropertyDefinition> definitions,
            final Map<String, String> config, final JsonObjectBuilder json, final SimplePropertyDefinition definition) {
        final String name = definition.getName();
        switch (getSwitchType(definition)) {
        case "OBJECT": {
            final String currentPath = prefix + name;
            processObject(definitions, definition, config, json, name, currentPath + '.');
            break;
        }
        case "ARRAY": {
            ofNullable(definitions
                    .stream()
                    // primitive arrays
                    .filter(it -> it.getPath().equals(definition.getPath() + "[]"))
                    .findFirst()
                    .map(itemDef -> config
                            .entrySet()
                            .stream()
                            .filter(it -> it.getKey().startsWith(definition.getName() + '['))
                            .map(e -> new ArrayEntry(e, definition.getName()))
                            // sort by index
                            .sorted(comparing(e -> e.index))
                            .map(e -> onArrayPrimitive(itemDef, e))
                            .collect(toJsonArray()))
                    // object arrays (we don't support yet arrays of arrays)
                    .orElseGet(() -> onArrayObjectItem(definitions, config, definition))).ifPresent(array -> {
                        if (!array.isEmpty()) { // else no config so no instance
                            json.add(name, array);
                        }
                    });
            break;
        }
        case "BOOLEAN":
            ofNullable(config.get(name))
                    .map(String::trim)
                    .filter(v -> !v.isEmpty())
                    .ifPresent(v -> json.add(name, Boolean.parseBoolean(v)));
            break;
        case "NUMBER":
            ofNullable(config.get(name))
                    .map(String::trim)
                    .filter(v -> !v.isEmpty())
                    .ifPresent(v -> json.add(name, Double.parseDouble(v)));
            break;
        case "ENUM":
        case "STRING":
            ofNullable(config.get(name)).ifPresent(v -> json.add(name, v));
            break;
        case "IGNORE":
        default:
        }
    }

    private JsonArray onArrayObjectItem(final Collection<SimplePropertyDefinition> definitions,
            final Map<String, String> config, final SimplePropertyDefinition definition) {
        final List<SimplePropertyDefinition> objectOptions = definitions.stream().filter(it -> {
            final String leadingStr = definition.getPath() + "[].";
            return it.getPath().startsWith(leadingStr) && it.getPath().indexOf('.', leadingStr.length() + 1) < 0;
        }).collect(toList());
        if (objectOptions.isEmpty()) {
            throw new IllegalArgumentException(
                    "Invalid model: " + definition + ", didn't find nested structure, available: "
                            + definitions.stream().map(SimplePropertyDefinition::getPath).collect(toList()));
        }

        return config
                .entrySet()
                .stream()
                .filter(it -> it.getKey().startsWith(definition.getName() + '['))
                .map(e -> new ArrayEntry(e, definition.getName()).index)
                .distinct()
                // sort by index
                .sorted(comparing(identity()))
                .map(index -> {
                    final String itemPrefix = definition.getPath() + "[].";
                    final String configFilter = definition.getName() + "[" + index + "].";
                    return unflatten(itemPrefix, objectOptions, config
                            .entrySet()
                            .stream()
                            .filter(sc -> sc.getKey().startsWith(configFilter))
                            .collect(toMap(sc -> sc.getKey().substring(configFilter.length()), Map.Entry::getValue)));
                })
                .collect(toJsonArray());
    }

    private JsonValue onArrayPrimitive(final SimplePropertyDefinition itemDef, final ArrayEntry e) {
        final String value = e.entry.getValue();
        switch (getSwitchType(itemDef)) {
        case "BOOLEAN":
            return Boolean.parseBoolean(value.trim()) ? JsonValue.TRUE : JsonValue.FALSE;
        case "NUMBER":
            return jsonp.createValue(Double.parseDouble(value.trim()));
        case "ENUM":
        case "STRING":
            return jsonp.createValue(value);
        default:
            throw new IllegalArgumentException("Unsupported structure in " + "array: " + itemDef.getType());
        }
    }

    private String getSwitchType(final SimplePropertyDefinition def) {
        return ofNullable(def.getType()).orElse("IGNORE").toUpperCase(ROOT);
    }

    private static class ArrayEntry {

        private final Map.Entry<String, String> entry;

        private final int index;

        private ArrayEntry(final Map.Entry<String, String> entry, final String name) {
            this.entry = entry;
            this.index = Integer.parseInt(entry.getKey().substring(name.length() + 1, entry.getKey().indexOf(']')));
        }
    }
}
