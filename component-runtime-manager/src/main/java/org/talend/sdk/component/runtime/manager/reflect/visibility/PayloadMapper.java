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
package org.talend.sdk.component.runtime.manager.reflect.visibility;

import static java.util.Collections.emptyMap;
import static java.util.Comparator.comparing;
import static java.util.Optional.ofNullable;
import static javax.json.stream.JsonCollectors.toJsonArray;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;

import javax.json.JsonArray;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import javax.json.spi.JsonProvider;

import org.talend.sdk.component.runtime.manager.ParameterMeta;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PayloadMapper {

    // we don't need the runtime one here
    private final JsonProvider jsonp = JsonProvider.provider();

    private final JsonBuilderFactory factory = jsonp.createBuilderFactory(emptyMap());

    private final OnParameter parameterVisitor;

    public JsonObject visitAndMap(final Collection<ParameterMeta> parameters, final Map<String, String> payload) {
        return unflatten("", ofNullable(parameters).orElseGet(Collections::emptyList),
                payload == null ? emptyMap() : payload);
    }

    private JsonObject unflatten(final String contextualPrefix, final Collection<ParameterMeta> definitions,
            final Map<String, String> config) {
        final JsonObjectBuilder json = factory.createObjectBuilder();
        ofNullable(definitions)
                .map(Collection::stream)
                .orElseGet(Stream::empty)
                .sorted(comparing(ParameterMeta::getName))
                .forEach(meta -> onProperty(contextualPrefix, meta.getNestedParameters(), config, json, meta));
        return json.build();
    }

    private void onProperty(final String contextualPrefix, final Collection<ParameterMeta> definitions,
            final Map<String, String> config, final JsonObjectBuilder json, final ParameterMeta definition) {
        final String name = definition.getName();
        final String newPath = contextualPrefix + (contextualPrefix.isEmpty() ? "" : ".") + name;
        switch (definition.getType()) {
        case OBJECT: {
            onObject(definitions, definition, config, json, name, newPath);
            break;
        }
        case ARRAY: {
            onArray(definition.getNestedParameters(), definition, config, newPath, json, name);
            break;
        }
        case BOOLEAN:
            final String boolValue = config.get(newPath);
            if (boolValue == null || boolValue.isEmpty()) {
                parameterVisitor.onParameter(definition, JsonValue.NULL);
            } else {
                final boolean value = Boolean.parseBoolean(boolValue.trim());
                parameterVisitor.onParameter(definition, value ? JsonValue.TRUE : JsonValue.FALSE);
                json.add(name, value);
            }
            ofNullable(boolValue)
                    .map(String::trim)
                    .filter(v -> !v.isEmpty())
                    .ifPresent(v -> json.add(name, Boolean.parseBoolean(v)));
            break;
        case NUMBER:
            final String numberValue = config.get(newPath);
            if (numberValue == null || numberValue.isEmpty()) {
                parameterVisitor.onParameter(definition, JsonValue.NULL);
            } else {
                final Double value = Double.valueOf(numberValue.trim());
                parameterVisitor.onParameter(definition, jsonp.createValue(value));
                final long asLong = value.longValue();
                if (value == asLong) {
                    json.add(name, asLong);
                } else {
                    json.add(name, value);
                }
            }
            break;
        case ENUM:
        case STRING: {
            final String value = config.get(newPath);
            parameterVisitor.onParameter(definition, value == null ? JsonValue.NULL : jsonp.createValue(value));
            ofNullable(value).ifPresent(v -> json.add(name, v));
            break;
        }
        default:
        }
    }

    private void onObject(final Collection<ParameterMeta> definitions, final ParameterMeta meta,
            final Map<String, String> config, final JsonObjectBuilder json, final String name,
            final String currentPath) {
        final JsonObject unflatten = unflatten(currentPath, definitions, config);
        if (!unflatten.isEmpty()) {
            json.add(name, unflatten);
            parameterVisitor.onParameter(meta, unflatten);
        } else {
            parameterVisitor.onParameter(meta, JsonValue.NULL);
        }
    }

    private void onArray(final Collection<ParameterMeta> definitions, final ParameterMeta definition,
            final Map<String, String> config, final String currentPrefix, final JsonObjectBuilder json,
            final String name) {
        final JsonArray array;
        if (definitions.size() == 1 && definitions.iterator().next().getPath().endsWith("[${index}]")) { // primitive
            final ParameterMeta primitiveMeta = definitions.stream().iterator().next();
            array = config
                    .entrySet()
                    .stream()
                    .filter(it -> it.getKey().startsWith(currentPrefix + '['))
                    .map(e -> new ArrayEntry(e, currentPrefix))
                    .distinct()
                    // sort by index
                    .sorted(comparing(it -> it.index))
                    .map(entry -> onArrayPrimitive(primitiveMeta, entry))
                    .collect(toJsonArray());
        } else {
            array = config
                    .entrySet()
                    .stream()
                    .filter(it -> it.getKey().startsWith(currentPrefix + '['))
                    .map(e -> new ArrayEntry(e, currentPrefix).index)
                    .distinct()
                    // sort by index
                    .sorted(comparing(it -> it))
                    .map(index -> unflatten(currentPrefix + '[' + index + ']', definitions, config))
                    .collect(toJsonArray());
        }
        if (!array.isEmpty()) {
            json.add(name, array);
            parameterVisitor.onParameter(definition, array);
        } else {
            parameterVisitor.onParameter(definition, JsonValue.NULL);
        }
    }

    private JsonValue onArrayPrimitive(final ParameterMeta itemDef, final ArrayEntry e) {
        final String value = e.entry.getValue();
        switch (itemDef.getType()) {
        case BOOLEAN:
            return Boolean.parseBoolean(value.trim()) ? JsonValue.TRUE : JsonValue.FALSE;
        case NUMBER:
            final Double number = Double.valueOf(value.trim());
            return number == number.longValue() ? jsonp.createValue(number.longValue()) : jsonp.createValue(number);
        case ENUM:
        case STRING:
            return jsonp.createValue(value);
        default:
            throw new IllegalArgumentException("Unsupported structure in " + "array: " + itemDef.getType());
        }
    }

    private static class ArrayEntry {

        private final Map.Entry<String, String> entry;

        private final int index;

        private ArrayEntry(final Map.Entry<String, String> entry, final String name) {
            this.entry = entry;

            final String indexStr =
                    entry.getKey().substring(name.length() + 1, entry.getKey().indexOf(']', name.length()));
            this.index = Integer.parseInt(indexStr);
        }
    }

    public interface OnParameter {

        void onParameter(ParameterMeta meta, JsonValue value);
    }
}
