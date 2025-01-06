/**
 * Copyright (C) 2006-2025 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.runtime.manager.reflect;

import static java.util.Comparator.comparing;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static org.talend.sdk.component.api.component.Icon.IconType.CUSTOM;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.InvocationTargetException;
import java.util.Optional;
import java.util.stream.Stream;

import org.talend.sdk.component.api.component.Icon;

public class IconFinder {

    public String findIcon(final AnnotatedElement type) {
        return findDirectIcon(type).orElseGet(() -> findIndirectIcon(type).orElse("default"));
    }

    public Optional<String> findIndirectIcon(final AnnotatedElement type) {
        // first try an annotation decorated with @Icon
        return ofNullable(findMetaIconAnnotation(type)
                .map(this::getMetaIcon)
                .orElseGet(() -> findImplicitIcon(type).orElse(null)));
    }

    private Optional<Annotation> findMetaIconAnnotation(final AnnotatedElement type) {
        return Stream
                .of(type.getAnnotations())
                .filter(this::isMetaIcon)
                .min(comparing(it -> it.annotationType().getName()));
    }

    private boolean isMetaIcon(final Annotation it) {
        return it.annotationType().isAnnotationPresent(Icon.class) && hasMethod(it.annotationType(), "value");
    }

    private Optional<String> findImplicitIcon(final AnnotatedElement type) {
        return findImplicitIconAnnotation(type).map(it -> String.valueOf(invoke(it, it.annotationType(), "value")));
    }

    private Optional<Annotation> findImplicitIconAnnotation(final AnnotatedElement type) {
        return Stream
                .of(type.getAnnotations())
                .filter(it -> it.annotationType().getSimpleName().endsWith("Icon")
                        && hasMethod(it.annotationType(), "value"))
                .findFirst();
    }

    private String getMetaIcon(final Annotation it) {
        // extract type and value (by convention), type can be custom to use value
        if (hasMethod(it.annotationType(), "type")) {
            final Object enumValue = invoke(it, it.annotationType(), "type");
            if (!"custom".equalsIgnoreCase(String.valueOf(enumValue))) {
                return getEnumKey(enumValue).orElseGet(() -> String.valueOf(invoke(it, it.annotationType(), "value")));
            }
        }
        final Object value = invoke(it, it.annotationType(), "value");
        if (value.getClass().isEnum()) {
            return getEnumKey(value).orElseGet(() -> String.valueOf(value));
        }
        return String.valueOf(value);
    }

    private Optional<String> getEnumKey(final Object enumValue) {
        return Stream
                .of("getKey", "getValue", "name")
                .filter(getter -> hasMethod(enumValue.getClass(), getter))
                .map(name -> String.valueOf(invoke(enumValue, enumValue.getClass(), name)))
                .findFirst();
    }

    private boolean hasMethod(final Class<?> clazz, final String method) {
        try {
            return clazz.getMethod(method) != null;
        } catch (final NoSuchMethodException e) {
            return false;
        }
    }

    private Object invoke(final Object instance, final Class<?> type, final String method) {
        try {
            return type.getMethod(method).invoke(instance);
        } catch (final IllegalAccessException | NoSuchMethodException e) {
            throw new IllegalStateException(e);
        } catch (final InvocationTargetException e) {
            throw new IllegalStateException(e.getTargetException());
        }
    }

    public Optional<String> findDirectIcon(final AnnotatedElement type) {
        return ofNullable(type.getAnnotation(Icon.class))
                .map(i -> i.value() == Icon.IconType.CUSTOM ? of(i.custom()).filter(s -> !s.isEmpty()).orElse("default")
                        : i.value().getKey());
    }

    public boolean isCustom(final Annotation icon) {
        if (Icon.class == icon.annotationType()) {
            return Icon.class.cast(icon).value() == CUSTOM;
        }
        if (hasMethod(icon.annotationType(), "type")) {
            return "custom".equalsIgnoreCase(String.valueOf(invoke(icon, icon.annotationType(), "type")));
        }
        return false;
    }

    public Annotation extractIcon(final AnnotatedElement annotatedElement) {
        return ofNullable(annotatedElement.getAnnotation(Icon.class))
                .map(Annotation.class::cast)
                .orElseGet(() -> findMetaIconAnnotation(annotatedElement)
                        .orElseGet(() -> findImplicitIconAnnotation(annotatedElement).orElse(null)));
    }
}
