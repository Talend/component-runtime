/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.runtime.beam.spi;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toSet;

import java.io.Serializable;
import java.lang.instrument.ClassFileTransformer;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import org.talend.sdk.component.design.extension.flows.FlowsFactory;
import org.talend.sdk.component.runtime.base.Delegated;
import org.talend.sdk.component.runtime.beam.Versions;
import org.talend.sdk.component.runtime.beam.design.BeamFlowFactory;
import org.talend.sdk.component.runtime.beam.factory.service.AutoValueFluentApiFactory;
import org.talend.sdk.component.runtime.beam.factory.service.PluginCoderFactory;
import org.talend.sdk.component.runtime.beam.impl.BeamMapperImpl;
import org.talend.sdk.component.runtime.beam.impl.BeamProcessorChainImpl;
import org.talend.sdk.component.runtime.beam.transformer.BeamIOTransformer;
import org.talend.sdk.component.runtime.input.Mapper;
import org.talend.sdk.component.runtime.manager.ComponentFamilyMeta;
import org.talend.sdk.component.runtime.output.Processor;
import org.talend.sdk.component.runtime.serialization.ContainerFinder;
import org.talend.sdk.component.spi.component.ComponentExtension;

import lombok.RequiredArgsConstructor;

public class BeamComponentExtension implements ComponentExtension {

    @Override
    public boolean isActive() {
        try {
            return ofNullable(Thread.currentThread().getContextClassLoader())
                    .orElseGet(ClassLoader::getSystemClassLoader)
                    .loadClass("org.apache.beam.sdk.transforms.PTransform")
                    .getClassLoader() == getClass().getClassLoader();
        } catch (final NoClassDefFoundError | ClassNotFoundException e) {
            return false;
        }
    }

    @Override
    public <T> T unwrap(final Class<T> type, final Object... args) {
        if ("org.talend.sdk.component.design.extension.flows.FlowsFactory".equals(type.getName()) && args != null
                && args.length == 1 && ComponentFamilyMeta.BaseMeta.class.isInstance(args[0])) {
            if (ComponentFamilyMeta.ProcessorMeta.class.isInstance(args[0])) {
                try {
                    final FlowsFactory factory = FlowsFactory.get(ComponentFamilyMeta.BaseMeta.class.cast(args[0]));
                    factory.getOutputFlows();
                    return type.cast(factory);
                } catch (final Exception e) { // no @ElementListener, let's default for native transforms
                    return type.cast(BeamFlowFactory.OUTPUT); // default
                }
            }
        }
        if (type.isInstance(this)) {
            return type.cast(this);
        }
        return null;
    }

    @Override
    public Collection<ClassFileTransformer> getTransformers() {
        if (Boolean.getBoolean("talend.component.beam.transformers.skip")) {
            return emptySet();
        }
        final String classes = System.getProperty("talend.component.beam.transformers.io.enhanced");
        return singleton(classes == null ? new BeamIOTransformer()
                : new BeamIOTransformer(
                        Stream.of(classes.split(",")).map(String::trim).filter(it -> !it.isEmpty()).collect(toSet())));
    }

    @Override
    public void onComponent(final ComponentContext context) {
        if (org.apache.beam.sdk.transforms.PTransform.class.isAssignableFrom(context.getType())) {
            context.skipValidation();
        }
    }

    @Override
    public boolean supports(final Class<?> componentType) {
        return componentType == Mapper.class || componentType == Processor.class;
    }

    @Override
    public Map<Class<?>, Object> getExtensionServices(final String plugin) {
        return new HashMap<Class<?>, Object>() {

            {
                put(AutoValueFluentApiFactory.class, new AutoValueFluentApiFactory());
                put(PluginCoderFactory.class, new PluginCoderFactory(plugin));
            }
        };
    }

    @Override
    public <T> T convert(final ComponentInstance instance, final Class<T> component) {
        if (!supports(component)) {
            throw new IllegalArgumentException("Unsupported component API: " + component);
        }
        // lazy to not fail if unwrapped
        return (T) Proxy
                .newProxyInstance(Thread.currentThread().getContextClassLoader(),
                        new Class<?>[] { component, Serializable.class, Delegated.class },
                        new LazyComponentHandler(component, instance.plugin(), instance.family(), instance.name(),
                                Serializable.class.cast(instance.instance())));
    }

    @Override
    public Collection<String> getAdditionalDependencies() {
        return singletonList(Versions.GROUP + ":" + Versions.ARTIFACT + ":jar:" + Versions.VERSION);
    }

    @RequiredArgsConstructor
    private static class LazyComponentHandler implements InvocationHandler, Serializable {

        private final Class<?> expectedType;

        private final String plugin;

        private final String family;

        private final String name;

        private final Serializable instance;

        private final AtomicReference<Serializable> actualDelegate = new AtomicReference<>();

        @Override
        public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
            final ClassLoader loader = ContainerFinder.Instance.get().find(plugin).classloader();
            final Thread thread = Thread.currentThread();
            final ClassLoader oldLoader = thread.getContextClassLoader();
            thread.setContextClassLoader(loader);
            try {
                if (Object.class == method.getDeclaringClass()) {
                    return method.invoke(instance, args);
                }
                if (Delegated.class == method.getDeclaringClass()) {
                    return instance;
                }
                if ("plugin".equals(method.getName()) && method.getParameterCount() == 0) {
                    return plugin;
                }
                if ("name".equals(method.getName()) && method.getParameterCount() == 0) {
                    return name;
                }
                if ("rootName".equals(method.getName()) && method.getParameterCount() == 0) {
                    return family;
                }
                Serializable delegateInstance = actualDelegate.get();
                if (delegateInstance == null) {
                    synchronized (this) {
                        delegateInstance = actualDelegate.get();
                        if (delegateInstance == null) {
                            if (Mapper.class == expectedType) {
                                actualDelegate
                                        .set(new BeamMapperImpl(
                                                (org.apache.beam.sdk.transforms.PTransform<org.apache.beam.sdk.values.PBegin, ?>) instance,
                                                plugin, family, name));
                            }
                            if (Processor.class == expectedType) {
                                actualDelegate
                                        .set(new BeamProcessorChainImpl(
                                                (org.apache.beam.sdk.transforms.PTransform<org.apache.beam.sdk.values.PCollection<?>, org.apache.beam.sdk.values.PDone>) instance,
                                                null, plugin, family, name));
                            }
                        }
                        delegateInstance = actualDelegate.get();
                    }
                }
                return method.invoke(delegateInstance, args);
            } catch (final InvocationTargetException ite) {
                throw ite.getTargetException();
            } finally {
                thread.setContextClassLoader(oldLoader);
            }
        }
    }
}
