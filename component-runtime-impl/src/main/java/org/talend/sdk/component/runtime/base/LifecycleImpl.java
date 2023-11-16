/**
 * Copyright (C) 2006-2023 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.runtime.base;

import static org.talend.sdk.component.runtime.base.lang.exception.InvocationExceptionWrapper.toRuntimeException;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.talend.sdk.component.runtime.serialization.ContainerFinder;

// base class to handle postconstruct/predestroy
public class LifecycleImpl extends Named implements Lifecycle {

    protected Object delegate;

    private transient ClassLoader loader;

    public LifecycleImpl(final Object delegate, final String rootName, final String name, final String plugin) {
        super(rootName, name, plugin);
        this.delegate = delegate;
    }

    protected LifecycleImpl() {
        // no-op
    }

    @Override
    public void start() {
        findMethods(PostConstruct.class).forEach(it -> doInvoke(it, evaluateParameters(PostConstruct.class, it)));
    }

    protected Object[] evaluateParameters(final Class<? extends Annotation> marker, final Method method) {
        return new Object[0];
    }

    @Override
    public void stop() {
        invoke(PreDestroy.class);
    }

    private void invoke(final Class<? extends Annotation> marker) {
        findMethods(marker).forEach(this::doInvoke);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final LifecycleImpl lifecycle = LifecycleImpl.class.cast(o);
        return Objects.equals(delegate, lifecycle.delegate) && Objects.equals(loader, lifecycle.loader);
    }

    @Override
    public int hashCode() {
        return Objects.hash(delegate, loader);
    }

    protected Object doInvoke(final Method m, final Object... args) {
        final Thread thread = Thread.currentThread();
        final ClassLoader oldLoader = thread.getContextClassLoader();
        thread.setContextClassLoader(findLoader());
        try {
            return m.invoke(delegate, args);
        } catch (final IllegalAccessException e) {
            throw new IllegalStateException(e);
        } catch (final InvocationTargetException e) {
            throw toRuntimeException(e);
        } finally {
            thread.setContextClassLoader(oldLoader);
        }
    }

    // mainly done by instance to avoid to rely on a registry maybe not initialized
    // after serialization
    protected Stream<Method> findMethods(final Class<? extends Annotation> marker) {
        final Thread thread = Thread.currentThread();
        final ClassLoader oldLoader = thread.getContextClassLoader();
        thread.setContextClassLoader(findLoader());
        try {
            return Stream.of(delegate.getClass().getMethods()).filter(m -> m.isAnnotationPresent(marker)).peek(m -> {
                if (!m.isAccessible()) {
                    m.setAccessible(true);
                }
            });
        } finally {
            thread.setContextClassLoader(oldLoader);
        }
    }

    protected byte[] serializeDelegate() {
        return Serializer.toBytes(delegate);
    }

    private ClassLoader findLoader() {
        if (loader == null) {
            try {
                loader = ContainerFinder.Instance.get().find(plugin()).classloader();
            } catch (final IllegalStateException ise) {
                // probably better to register a finder but if not don't fail and use TCCL
            }
            if (loader == null) {
                loader = Thread.currentThread().getContextClassLoader();
            }
        }
        return loader;
    }
}
