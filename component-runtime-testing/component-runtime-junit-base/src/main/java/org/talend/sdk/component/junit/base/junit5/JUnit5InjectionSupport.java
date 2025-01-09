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
package org.talend.sdk.component.junit.base.junit5;

import java.lang.annotation.Annotation;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;

/**
 * Helper which ensure the same code is used for the field injections and parameter injections.
 */
public interface JUnit5InjectionSupport extends ParameterResolver, TestInstancePostProcessor {

    Class<? extends Annotation> injectionMarker();

    default boolean supports(final Class<?> type) {
        return type.isInstance(this);
    }

    default Object findInstance(final ExtensionContext extensionContext, final Class<?> type, final Annotation marker) {
        return findInstance(extensionContext, type);
    }

    default Object findInstance(final ExtensionContext extensionContext, final Class<?> type) {
        return this;
    }

    @Override
    default boolean supportsParameter(final ParameterContext parameterContext, final ExtensionContext extensionContext)
            throws ParameterResolutionException {
        return supports(parameterContext.getParameter().getType());
    }

    @Override
    default Object resolveParameter(final ParameterContext parameterContext, final ExtensionContext extensionContext)
            throws ParameterResolutionException {
        return findInstance(extensionContext, parameterContext.getParameter().getType());
    }

    @Override
    default void postProcessTestInstance(final Object testInstance, final ExtensionContext context) {
        Class<?> testClass = context.getRequiredTestClass();
        while (testClass != Object.class) {
            Stream
                    .of(testClass.getDeclaredFields())
                    .filter(c -> c.isAnnotationPresent(injectionMarker()))
                    .forEach(f -> {
                        if (!supports(f.getType())) {
                            throw new IllegalArgumentException("@" + injectionMarker() + " not supported on " + f);
                        }
                        if (!f.isAccessible()) {
                            f.setAccessible(true);
                        }
                        try {
                            f.set(testInstance, findInstance(context, f.getType(), f.getAnnotation(injectionMarker())));
                        } catch (final IllegalAccessException e) {
                            throw new IllegalStateException(e);
                        }
                    });
            testClass = testClass.getSuperclass();
        }
    }
}
