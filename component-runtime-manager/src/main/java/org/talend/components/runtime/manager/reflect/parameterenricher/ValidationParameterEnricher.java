// ============================================================================
//
// Copyright (C) 2006-2017 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.components.runtime.manager.reflect.parameterenricher;

import static java.util.Collections.singletonMap;
import static java.util.Optional.ofNullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;

import org.talend.component.api.configuration.constraint.meta.Validation;
import org.talend.component.api.configuration.constraint.meta.Validations;
import org.talend.components.spi.parameter.ParameterExtensionEnricher;

public class ValidationParameterEnricher implements ParameterExtensionEnricher {

    public static final String META_PREFIX = "tcomp::validation::";

    @Override
    public Map<String, String> onParameterAnnotation(final String parameterName, final Type parameterType,
            final Annotation annotation) {
        final Class<?> asClass = toClass(parameterType);
        return ofNullable(annotation.annotationType().getAnnotation(Validations.class)).map(v -> Stream.of(v.value()))
                .orElseGet(() -> ofNullable(annotation.annotationType().getAnnotation(Validation.class)).map(Stream::of)
                        .orElseGet(Stream::empty))
                .filter(v -> Stream.of(v.expectedTypes()).anyMatch(t -> asClass != null && t.isAssignableFrom(asClass))).findFirst()
                .map(v -> singletonMap(META_PREFIX + v.name(), getValueString(annotation))).orElseGet(Collections::emptyMap);
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
