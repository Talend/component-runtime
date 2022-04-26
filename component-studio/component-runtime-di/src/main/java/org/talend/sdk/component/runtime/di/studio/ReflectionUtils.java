/**
 * Copyright (C) 2006-2022 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.runtime.di.studio;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

import org.talend.sdk.component.runtime.base.Lifecycle;
import org.talend.sdk.component.runtime.base.lang.exception.InvocationExceptionWrapper;
import org.talend.sdk.component.runtime.serialization.ContainerFinder;

import lombok.SneakyThrows;

public class ReflectionUtils {

    static Stream<Method> findMethods(final Object delegate, final Class<? extends Annotation> marker,
            final ClassLoader loader) {
        return callInLoader(loader,
                () -> Stream.of(delegate.getClass().getMethods()).filter(m -> m.isAnnotationPresent(marker)).peek(m -> {
                    if (!m.isAccessible()) {
                        m.setAccessible(true);
                    }
                }));
    }

    static Stream<Field> findFields(final Object delegate, final Class<? extends Annotation> marker,
            final ClassLoader loader) {
        return callInLoader(loader,
                () -> Stream.of(delegate.getClass().getDeclaredFields())
                        .filter(m -> m.isAnnotationPresent(marker))
                        .peek(m -> {
                            if (!m.isAccessible()) {
                                m.setAccessible(true);
                            }
                        }));
    }

    static Stream<Field> findFields(final Object delegate, final Class<? extends Annotation> marker) {
        return findFields(delegate.getClass(), marker);
    }

    static Stream<Field> findFields(final Class delegateClass, final Class<? extends Annotation> marker) {
        return Stream.of(delegateClass.getDeclaredFields()).filter(m -> m.isAnnotationPresent(marker)).peek(m -> {
            if (!m.isAccessible()) {
                m.setAccessible(true);
            }
        });
    }

    static ClassLoader getClassLoader(final Lifecycle lifecycle) {
        return Optional
                .ofNullable(ContainerFinder.Instance.get().find(lifecycle.plugin()).classloader())
                .orElseGet(() -> Thread.currentThread().getContextClassLoader());
    }

    @SneakyThrows
    static <T> T callInLoader(final ClassLoader loader, final Callable<T> supplier) {
        final Thread thread = Thread.currentThread();
        final ClassLoader oldLoader = thread.getContextClassLoader();
        thread.setContextClassLoader(loader);
        try {
            return supplier.call();
        } catch (final IllegalAccessException e) {
            throw new IllegalStateException(e);
        } catch (final InvocationTargetException e) {
            throw InvocationExceptionWrapper.toRuntimeException(e);
        } finally {
            thread.setContextClassLoader(oldLoader);
        }
    }

}
