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

import static org.talend.sdk.component.runtime.base.lang.exception.InvocationExceptionWrapper.toRuntimeException;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

import org.talend.sdk.component.api.component.AfterVariables.AfterVariableContainer;
import org.talend.sdk.component.runtime.base.Delegated;
import org.talend.sdk.component.runtime.base.Lifecycle;
import org.talend.sdk.component.runtime.serialization.ContainerFinder.Instance;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AfterVariableExtracter {

    /**
     * Extract the after variable map from the classes annotated with
     * {@link org.talend.sdk.component.api.input.Emitter},
     * {@link org.talend.sdk.component.api.input.PartitionMapper},
     * {@link org.talend.sdk.component.api.processor.Output}.
     * After variables can appear during the job process in the map.
     *
     * @return map with after variables.
     */
    public static Map<String, Object> extractAfterVariables(final Lifecycle lifecycle) {
        if (lifecycle instanceof Delegated) {
            final Object delegate = ((Delegated) lifecycle).getDelegate();

            ClassLoader classloader = Optional
                    .ofNullable(Instance.get().find(lifecycle.plugin()).classloader())
                    .orElseGet(() -> Thread.currentThread().getContextClassLoader());
            return findMethods(delegate, AfterVariableContainer.class, classloader)
                    .findFirst()
                    .map(m -> callInLoader(classloader, () -> (Map<String, Object>) m.invoke(delegate)))
                    .orElse(Collections.emptyMap());
        } else {
            log.warn("Not supported implementation of lifecycle");
        }

        return Collections.emptyMap();
    }

    private static Stream<Method> findMethods(final Object delegate, final Class<? extends Annotation> marker,
            final ClassLoader loader) {
        return callInLoader(loader,
                () -> Stream.of(delegate.getClass().getMethods()).filter(m -> m.isAnnotationPresent(marker)).peek(m -> {
                    if (!m.isAccessible()) {
                        m.setAccessible(true);
                    }
                }));
    }

    @SneakyThrows
    public static <T> T callInLoader(final ClassLoader loader, final Callable<T> supplier) {
        final Thread thread = Thread.currentThread();
        final ClassLoader oldLoader = thread.getContextClassLoader();
        thread.setContextClassLoader(loader);
        try {
            return supplier.call();
        } catch (final IllegalAccessException e) {
            throw new IllegalStateException(e);
        } catch (final InvocationTargetException e) {
            throw toRuntimeException(e);
        } finally {
            thread.setContextClassLoader(oldLoader);
        }
    }
}
