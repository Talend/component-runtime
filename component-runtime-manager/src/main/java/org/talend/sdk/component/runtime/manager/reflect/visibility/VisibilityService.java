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
package org.talend.sdk.component.runtime.manager.reflect.visibility;

import static java.util.Locale.ROOT;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static lombok.AccessLevel.PRIVATE;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.json.JsonArrayBuilder;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonPointer;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.spi.JsonProvider;

import org.talend.sdk.component.runtime.manager.ParameterMeta;

import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@RequiredArgsConstructor
public class VisibilityService {

    private final AbsolutePathResolver pathResolver = new AbsolutePathResolver();

    private final JsonProvider jsonProvider;

    public ConditionGroup build(final ParameterMeta param) {
        final boolean and =
                "AND".equalsIgnoreCase(param.getMetadata().getOrDefault("tcomp::condition::ifs::operator", "AND"));
        Map<String, Condition> conditions = new HashMap<>();
        ConditionGroup group = new ConditionGroup(param
                .getMetadata()
                .entrySet()
                .stream()
                .filter(meta -> meta.getKey().startsWith("tcomp::condition::if::target"))
                .map(meta -> {
                    final String[] split = meta.getKey().split("::");
                    final String index = split.length == 5 ? "::" + split[split.length - 1] : "";
                    final String valueKey = "tcomp::condition::if::value" + index;
                    final String negateKey = "tcomp::condition::if::negate" + index;
                    final String evaluationStrategyKey = "tcomp::condition::if::evaluationStrategy" + index;
                    final String absoluteTargetPath = pathResolver.resolveProperty(param.getPath(), meta.getValue());
                    Condition condition = conditions.get(absoluteTargetPath);
                    if (condition == null) {
                        condition = new Condition('/' + absoluteTargetPath.replace('.', '/'),
                                toPointer(absoluteTargetPath),
                                Boolean.parseBoolean(param.getMetadata().getOrDefault(negateKey, "false")),
                                param.getMetadata().getOrDefault(evaluationStrategyKey, "DEFAULT").toUpperCase(ROOT),
                                param.getMetadata().getOrDefault(valueKey, "true").split(","));
                        conditions.put(absoluteTargetPath, condition);
                    } else {
                        List<String> collection = Arrays.stream(condition.values).collect(toList());
                        collection.add(String.valueOf(param.getMetadata().getOrDefault(valueKey, "true")));
                        condition = new Condition('/' + absoluteTargetPath.replace('.', '/'),
                                toPointer(absoluteTargetPath),
                                Boolean.parseBoolean(param.getMetadata().getOrDefault(negateKey, "false")),
                                param.getMetadata().getOrDefault(evaluationStrategyKey, "DEFAULT").toUpperCase(ROOT),
                                collection.toArray(new String[0]));
                        conditions.replace(absoluteTargetPath, condition);
                    }
                    return condition;
                })
                .collect(toList()), and ? stream -> stream.allMatch(i -> i) : stream -> stream.anyMatch(i -> i));
        if (and) {
            group.conditions.clear();
            group.conditions.addAll(conditions.values());
        }
        return group;
    }

    private JsonPointer toPointer(final String absoluteTargetPath) {
        return jsonProvider.createPointer('/' + absoluteTargetPath.replace('.', '/'));
    }

    // check org.talend.sdk.component.form.internal.converter.impl.widget.path.AbsolutePathResolver,
    // we don't want component-form-core dependency here, we can move it to another module later if relevant
    @NoArgsConstructor(access = PRIVATE)
    private static class AbsolutePathResolver {

        private String resolveProperty(final String propPath, final String paramRef) {
            return doResolveProperty(propPath, normalizeParamRef(paramRef));
        }

        private String normalizeParamRef(final String paramRef) {
            return (!paramRef.contains(".") ? "../" : "") + paramRef;
        }

        private String doResolveProperty(final String propPath, final String paramRef) {
            if (".".equals(paramRef)) {
                return propPath;
            }
            if (paramRef.startsWith("..")) {
                String current = propPath;
                String ref = paramRef;
                while (ref.startsWith("..")) {
                    int lastDot = current.lastIndexOf('.');
                    if (lastDot < 0) {
                        lastDot = 0;
                    }
                    current = current.substring(0, lastDot);
                    ref = ref.substring("..".length(), ref.length());
                    if (ref.startsWith("/")) {
                        ref = ref.substring(1);
                    }
                    if (current.isEmpty()) {
                        break;
                    }
                }
                return Stream.of(current, ref.replace('/', '.')).filter(it -> !it.isEmpty()).collect(joining("."));
            }
            if (paramRef.startsWith(".") || paramRef.startsWith("./")) {
                return propPath + '.' + paramRef.replaceFirst("\\./?", "").replace('/', '.');
            }
            return paramRef;
        }
    }

    @RequiredArgsConstructor(access = PRIVATE)
    @ToString
    public static class Condition {

        private static final Function<Object, String> TO_STRING = v -> v == null ? null : String.valueOf(v);

        private static final Function<Object, String> TO_LOWERCASE =
                v -> v == null ? null : String.valueOf(v).toLowerCase(ROOT);

        private final String path;

        private final JsonPointer pointer;

        private final boolean negation;

        private final String evaluationStrategy;

        private final String[] values;

        boolean evaluateCondition(final JsonObject payload) {
            return negation != Stream.of(values).anyMatch(val -> evaluate(val, payload));
        }

        private boolean evaluate(final String expected, final JsonObject payload) {
            final Object actual = extractValue(payload);
            switch (evaluationStrategy) {
                case "DEFAULT":
                    if (Collection.class.isInstance(actual)) {
                        //if values >= actual, return true, it actual contains any element out of values, return false;
                        for (Object s : Collection.class.cast(actual)) {
                            if (!Arrays.stream(values).collect(toList()).contains(s)) {
                                return false;
                            }
                        }
                        return true;
                    }
                    return expected.equals(TO_STRING.apply(actual));
                case "LENGTH":
                    if (actual == null) {
                        return "0".equals(expected);
                    }
                    final int expectedSize = Integer.parseInt(expected);
                    if (Collection.class.isInstance(actual)) {
                        return expectedSize == Collection.class.cast(actual).size();
                    }
                    if (actual.getClass().isArray()) {
                        return expectedSize == Array.getLength(actual);
                    }
                    if (String.class.isInstance(actual)) {
                        return expectedSize == String.class.cast(actual).length();
                    }
                    return false;
                default:
                    Function<Object, String> preprocessor = TO_STRING;
                    if (evaluationStrategy.startsWith("CONTAINS")) {
                        final int start = evaluationStrategy.indexOf('(');
                        if (start >= 0) {
                            final int end = evaluationStrategy.indexOf(')', start);
                            if (end >= 0) {
                                final Map<String, String> configuration = Stream
                                        .of(evaluationStrategy.substring(start + 1, end).split(","))
                                        .map(String::trim)
                                        .filter(it -> !it.isEmpty())
                                        .map(it -> {
                                            final int sep = it.indexOf('=');
                                            if (sep > 0) {
                                                return new String[]{it.substring(0, sep), it.substring(sep + 1)};
                                            }
                                            return new String[]{"value", it};
                                        })
                                        .collect(toMap(a -> a[0], a -> a[1]));
                                if (Boolean.parseBoolean(configuration.getOrDefault("lowercase", "false"))) {
                                    preprocessor = TO_LOWERCASE;
                                }
                            }
                        }
                        if (actual == null) {
                            return false;
                        }
                        if (CharSequence.class.isInstance(actual)) {
                            return ofNullable(preprocessor.apply(TO_STRING.apply(actual)))
                                    .map(it -> it.contains(expected))
                                    .orElse(false);
                        }
                        if (Collection.class.isInstance(actual)) {
                            final Collection<?> collection = Collection.class.cast(actual);
                            return collection.stream().map(preprocessor).anyMatch(it -> it.contains(expected));
                        }
                        if (actual.getClass().isArray()) {
                            return IntStream
                                    .range(0, Array.getLength(actual))
                                    .mapToObj(i -> Array.get(actual, i))
                                    .map(preprocessor)
                                    .anyMatch(it -> it.contains(expected));
                        }
                        return false;
                    }
                    throw new IllegalArgumentException("Not supported operation '" + evaluationStrategy + "'");
            }
        }

        private Object extractValue(final JsonObject payload) {
            if (!pointer.containsValue(payload)) {
                if (path.contains("[${index}]")) {
                    final JsonPointer ptr = JsonProvider.provider()
                            .createPointer(path.substring(0, path.indexOf("[${index}]")));
                    final JsonPointer subptr = JsonProvider.provider()
                            .createPointer(path.substring(path.indexOf("]") + 1));
                    final JsonArrayBuilder builder = JsonProvider.provider().createArrayBuilder();
                    ptr.getValue(payload)
                            .asJsonArray()
                            .stream()
                            .forEach(j -> {
                                JsonValue value = subptr.getValue(j.asJsonObject());
                                builder.add(value);
                            });
                    return ofNullable(builder.build()).map(this::mapValue).orElse(null);
                }
                return null;
            }
            return ofNullable(pointer.getValue(payload)).map(this::mapValue).orElse(null);
        }

        private Object mapValue(final JsonValue value) {
            switch (value.getValueType()) {
                case ARRAY:
                    return value.asJsonArray().stream().map(this::mapValue).collect(toList());
                case STRING:
                    return JsonString.class.cast(value).getString();
                case TRUE:
                    return true;
                case FALSE:
                    return false;
                case NUMBER:
                    return JsonNumber.class.cast(value).doubleValue();
                case NULL:
                    return null;
                case OBJECT:
                default:
                    return value;
            }
        }
    }

    @RequiredArgsConstructor(access = PRIVATE)
    public static class ConditionGroup {

        private final Collection<Condition> conditions;

        private final Function<Stream<Boolean>, Boolean> aggregator;

        public boolean isVisible(final JsonObject payload) {
            return conditions
                    .stream()
                    .allMatch(group -> aggregator.apply(conditions.stream().map(c -> c.evaluateCondition(payload))));
        }
    }

}
