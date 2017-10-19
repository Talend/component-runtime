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
package org.talend.components.runtime.base;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.talend.components.runtime.serialization.ContainerFinder;

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
        invoke(PostConstruct.class);
    }

    @Override
    public void stop() {
        invoke(PreDestroy.class);
    }

    private void invoke(final Class<? extends Annotation> marker) {
        findMethods(marker).forEach(this::doInvoke);
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
            throw new IllegalStateException(e.getTargetException());
        } finally {
            thread.setContextClassLoader(oldLoader);
        }
    }

    // mainly done by instance to avoid to rely on a registry maybe not initialized after serialization
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
