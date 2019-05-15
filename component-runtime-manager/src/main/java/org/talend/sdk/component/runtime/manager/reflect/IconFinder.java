/**
 * Copyright (C) 2006-2019 Talend Inc. - www.talend.com
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

import static java.util.Optional.of;
import static java.util.Optional.ofNullable;

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
        return Stream.of(type.getAnnotations()).filter(it -> {
            try {
                final Class<? extends Annotation> clazz = it.annotationType();
                return clazz.getSimpleName().endsWith("Icon") && clazz.getMethod("value") != null;
            } catch (final NoSuchMethodException e) {
                return false;
            }
        }).map(it -> {
            try {
                return String.valueOf(it.annotationType().getMethod("value").invoke(it));
            } catch (final IllegalAccessException | NoSuchMethodException e) {
                throw new IllegalStateException(e);
            } catch (final InvocationTargetException e) {
                throw new IllegalStateException(e.getTargetException());
            }
        }).findFirst();
    }

    public Optional<String> findDirectIcon(final AnnotatedElement type) {
        return ofNullable(type.getAnnotation(Icon.class))
                .map(i -> i.value() == Icon.IconType.CUSTOM ? of(i.custom()).filter(s -> !s.isEmpty()).orElse("default")
                        : i.value().getKey());
    }
}
