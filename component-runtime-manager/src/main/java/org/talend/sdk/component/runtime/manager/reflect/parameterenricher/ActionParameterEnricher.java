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
package org.talend.sdk.component.runtime.manager.reflect.parameterenricher;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.talend.sdk.component.api.configuration.action.meta.ActionRef;
import org.talend.sdk.component.api.service.ActionType;

public class ActionParameterEnricher extends BaseParameterEnricher {

    public static final String META_PREFIX = "tcomp::action::";

    private final ConcurrentMap<String, String> clientActionsMapping = new ConcurrentHashMap<>();

    @Override
    public Map<String, String> onParameterAnnotation(final String parameterName, final Type parameterType,
            final Annotation annotation) {
        final Class<? extends Annotation> annotationType = annotation.annotationType();
        final ActionRef ref = annotationType.getAnnotation(ActionRef.class);
        if (ref == null) {
            return emptyMap();
        }
        final Class<?> actionType = ref.value();
        if (actionType == Object.class) { // client action
            return singletonMap(
                    META_PREFIX
                            + clientActionsMapping.computeIfAbsent(annotationType.getSimpleName(), this::toSnakeCase),
                    getClientActionName(annotation));
        }
        final String type = actionType.getAnnotation(ActionType.class).value();
        return new HashMap<String, String>() {

            {
                put(META_PREFIX + type, getValueString(ref.ref(), annotation));
                ofNullable(getParametersString(annotation)).ifPresent(v -> put(META_PREFIX + type + "::parameters", v));
                Stream
                        .of(annotationType.getMethods())
                        .filter(it -> annotationType == it.getDeclaringClass()
                                && Stream.of("parameters", "value").noneMatch(v -> it.getName().equalsIgnoreCase(v)))
                        .forEach(m -> put(META_PREFIX + type + "::" + m.getName(), getString(m, annotation)));
            }
        };
    }

    private String toSnakeCase(final String camelCaseName) {
        final char[] chars = camelCaseName.substring(1).toCharArray();
        return Character.toLowerCase(camelCaseName.charAt(0))
                + IntStream.range(0, chars.length).mapToObj(i -> chars[i]).map(c -> {
                    if (Character.isUpperCase(c)) {
                        return "_" + Character.toLowerCase(c);
                    }
                    return Character.toString(c);
                }).collect(joining());
    }

    private String getClientActionName(final Annotation clientMeta) {
        final String value = getValueString("value", clientMeta);
        return "CUSTOM".equalsIgnoreCase(value) ? getValueString("name", clientMeta) : value;
    }

    private String getString(final Method method, final Annotation annotation) {
        try {
            return String.valueOf(method.invoke(annotation));
        } catch (final IllegalAccessException | InvocationTargetException e) {
            throw new IllegalArgumentException("No valid " + method + " for " + annotation);
        }
    }

    private String getValueString(final String method, final Annotation annotation) {
        try {
            return getString(annotation.annotationType().getMethod(method), annotation);
        } catch (final NoSuchMethodException e) {
            throw new IllegalArgumentException("No " + method + " for " + annotation);
        }
    }

    private String getParametersString(final Annotation annotation) {
        try {
            return String
                    .join(",", String[].class
                            .cast(annotation.annotationType().getMethod("parameters").invoke(annotation)));
        } catch (final IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            return null;
        }
    }
}
