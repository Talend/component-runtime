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

import static java.util.Collections.singletonMap;
import static java.util.Optional.ofNullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;

import org.talend.sdk.component.api.configuration.constraint.meta.Validation;
import org.talend.sdk.component.api.configuration.constraint.meta.Validations;

public class ValidationParameterEnricher extends BaseParameterEnricher {

    public static final String META_PREFIX = "tcomp::validation::";

    @Override
    public Map<String, String> onParameterAnnotation(final String parameterName, final Type parameterType,
            final Annotation annotation) {
        final Class<?> asClass = toClass(parameterType);
        return ofNullable(annotation.annotationType().getAnnotation(Validations.class))
                .map(v -> Stream.of(v.value()))
                .orElseGet(() -> ofNullable(annotation.annotationType().getAnnotation(Validation.class))
                        .map(Stream::of)
                        .orElseGet(Stream::empty))
                .filter(v -> Stream.of(v.expectedTypes()).anyMatch(t -> asClass != null && t.isAssignableFrom(asClass)))
                .findFirst()
                .map(v -> singletonMap(META_PREFIX + v.name(), getValueString(annotation)))
                .orElseGet(Collections::emptyMap);
    }

    private Class<?> toClass(final Type parameterType) {
        return ParameterizedType.class.isInstance(parameterType)
                ? toClass(ParameterizedType.class.cast(parameterType).getRawType())
                : (Class.class.isInstance(parameterType) ? Class.class.cast(parameterType) : null);
    }

    private String getValueString(final Annotation annotation) {
        try {
            return String.valueOf(annotation.annotationType().getMethod("value").invoke(annotation));
        } catch (final IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            return "true"; // no value method means it is a boolean marker
        }
    }
}
