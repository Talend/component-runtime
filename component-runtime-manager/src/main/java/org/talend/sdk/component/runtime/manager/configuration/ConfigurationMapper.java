/**
 * Copyright (C) 2006-2026 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.runtime.manager.configuration;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.lang.reflect.Field;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.runtime.manager.ParameterMeta;

public class ConfigurationMapper {

    public Map<String, String> map(final List<ParameterMeta> nestedParameters, final Object instance) {
        return map(nestedParameters, instance, new HashMap<>());
    }

    private Map<String, String> map(final List<ParameterMeta> nestedParameters, final Object instance,
            final Map<Integer, Integer> indexes) {
        if (nestedParameters == null) {
            return emptyMap();
        }
        return nestedParameters.stream().map(param -> {
            final Object value = getValue(instance, param.getName());
            if (value == null) {
                return Collections.<String, String> emptyMap();
            }

            switch (param.getType()) {
                case OBJECT:
                    return map(param.getNestedParameters(), value, indexes);
                case ARRAY:
                    final Collection<Object> values = Collection.class.isInstance(value) ? Collection.class.cast(value)
                            : /* array */asList(Object[].class.cast(value));
                    final int arrayIndex = indexes.keySet().size();
                    final AtomicInteger valuesIndex = new AtomicInteger(0);
                    final Map<String, String> config = values.stream()
                            .map((Object item) -> {
                                indexes.put(arrayIndex, valuesIndex.getAndIncrement());
                                final Map<String, String> res = param
                                        .getNestedParameters()
                                        .stream()
                                        .filter(ConfigurationMapper::isPrimitive)
                                        .map(p -> new AbstractMap.SimpleImmutableEntry<>(
                                                evaluateIndexes(p.getPath(), indexes),
                                                getValue(item, p.getName())))
                                        .filter(p -> p.getValue() != null)
                                        .collect(toMap(AbstractMap.SimpleImmutableEntry::getKey,
                                                p -> String.valueOf(p.getValue())));

                                res.putAll(map(
                                        param.getNestedParameters()
                                                .stream()
                                                .filter(p -> !isPrimitive(p))
                                                .collect(toList()),
                                        item, indexes));
                                return res;
                            })
                            .flatMap(m -> m.entrySet().stream())
                            .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));

                    indexes.remove(arrayIndex); // clear index after the end of array handling
                    return config;

                default: // primitives
                    return singletonMap(evaluateIndexes(param.getPath(), indexes), value.toString());
            }
        }).flatMap(m -> m.entrySet().stream()).collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private static boolean isPrimitive(final ParameterMeta next) {
        return Stream
                .of(ParameterMeta.Type.STRING, ParameterMeta.Type.BOOLEAN, ParameterMeta.Type.ENUM,
                        ParameterMeta.Type.NUMBER)
                .anyMatch(v -> v == next.getType());
    }

    private static Object getValue(final Object instance, final String name) {
        if (name.endsWith("[${index}]")) {
            return instance;
        }

        Field declaredField = null;
        Class<?> current = instance.getClass();
        while (current != null && current != Object.class) {
            final Optional<Field> field = Stream
                    .of(current.getDeclaredFields())
                    .filter(it -> ofNullable(it.getAnnotation(Option.class))
                            .map(Option::value)
                            .filter(val -> !val.isEmpty())
                            .orElseGet(it::getName)
                            .equals(name))
                    .findFirst();
            if (!field.isPresent()) {
                current = current.getSuperclass();
                continue;
            }
            declaredField = field.get();
            break;
        }
        if (declaredField == null) {
            throw new IllegalArgumentException("No field '" + name + "' in " + instance);
        }
        if (!declaredField.isAccessible()) {
            declaredField.setAccessible(true);
        }
        try {
            return declaredField.get(instance);
        } catch (final IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String evaluateIndexes(final String path, final Map<Integer, Integer> indexes) {
        if (indexes == null || indexes.isEmpty()) {
            return path;
        }
        final String placeholder = "${index}";
        String p = path;
        StringBuilder evaluatedPath = new StringBuilder();
        for (Map.Entry<Integer, Integer> index : indexes.entrySet()) {
            int i = p.indexOf(placeholder);
            evaluatedPath.append(p, 0, i).append(index.getValue()).append("]");
            p = p.substring(i + placeholder.length() + 1);
        }
        return evaluatedPath.append(p).toString();
    }
}
