/**
 * Copyright (C) 2006-2017 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.runtime.output;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import org.talend.sdk.component.api.processor.AfterGroup;
import org.talend.sdk.component.api.processor.BeforeGroup;
import org.talend.sdk.component.api.processor.ElementListener;
import org.talend.sdk.component.api.processor.Input;
import org.talend.sdk.component.api.processor.Output;
import org.talend.sdk.component.api.processor.data.ObjectMap;
import org.talend.sdk.component.runtime.base.Delegated;
import org.talend.sdk.component.runtime.base.LifecycleImpl;
import org.talend.sdk.component.runtime.output.data.AccessorCache;
import org.talend.sdk.component.runtime.output.data.ObjectMapImpl;
import org.talend.sdk.component.runtime.serialization.ContainerFinder;
import org.talend.sdk.component.runtime.serialization.EnhancedObjectInputStream;

import lombok.AllArgsConstructor;

public class ProcessorImpl extends LifecycleImpl implements Processor, Delegated {

    private transient List<Method> beforeGroup;

    private transient List<Method> afterGroup;

    private transient Method process;

    private transient List<BiFunction<InputFactory, OutputFactory, Object>> parameterBuilder;

    private boolean forwardReturn;

    public ProcessorImpl(final String rootName, final String name, final String plugin, final Serializable delegate) {
        super(delegate, rootName, name, plugin);
    }

    protected ProcessorImpl() {
        // no-op
    }

    @Override
    public void beforeGroup() {
        if (process == null) {
            beforeGroup = findMethods(BeforeGroup.class).collect(toList());
            afterGroup = findMethods(AfterGroup.class).collect(toList());
            process = findMethods(ElementListener.class).findFirst().get();

            // IMPORTANT: ensure you call only once the create(....), see studio integration (mojo)
            parameterBuilder = Stream.of(process.getParameters()).map(parameter -> {
                if (parameter.isAnnotationPresent(Output.class)) {
                    return (BiFunction<InputFactory, OutputFactory, Object>) (inputs, outputs) -> outputs
                            .create(parameter.getAnnotation(Output.class).value());
                }

                final Class<?> parameterType = parameter.getType();
                final boolean isObjectMap = ObjectMap.class.isAssignableFrom(parameterType);
                final AccessorCache cache = ofNullable(
                        ContainerFinder.Instance.get().find(plugin()).findService(AccessorCache.class))
                                .orElseGet(() -> new AccessorCache(plugin()));
                final String inputName = ofNullable(parameter.getAnnotation(Input.class)).map(Input::value)
                        .orElse(Branches.DEFAULT_BRANCH);
                return (BiFunction<InputFactory, OutputFactory, Object>) (inputs, outputs) -> doConvertInput(parameterType,
                        isObjectMap, cache, inputs.read(inputName));
            }).collect(toList());
            forwardReturn = process.getReturnType() != void.class;
        }

        beforeGroup.forEach(this::doInvoke);
    }

    private Object doConvertInput(final Class<?> parameterType, final boolean isObjectMap, final AccessorCache cache,
            final Object data) {
        if (isObjectMap) {
            if (ObjectMap.class.isInstance(data)) {
                return data;
            }
            return new ObjectMapImpl(plugin(), data, cache);
        }
        if (parameterType.isInstance(data) || parameterType.isPrimitive()/* this case can need some more work */) {
            return data;
        }
        // here we need to subclass parameter.getType to support an objectmap as input
        return newSubClassInstance(parameterType, new ObjectMapImpl(plugin(), data, cache));
    }

    @Override
    public void afterGroup(final OutputFactory output) {
        afterGroup.forEach(this::doInvoke);
    }

    @Override
    public void onNext(final InputFactory inputFactory, final OutputFactory outputFactory) {
        final Object out = doInvoke(process,
                parameterBuilder.stream().map(b -> b.apply(inputFactory, outputFactory)).toArray(Object[]::new));
        if (forwardReturn) {
            outputFactory.create(Branches.DEFAULT_BRANCH).emit(out);
        }
    }

    @Override
    public Object getDelegate() {
        return delegate;
    }

    protected Object newSubClassInstance(final Class<?> type, final ObjectMap data) {
        throw new UnsupportedOperationException("Subclass " + getClass().getName() + " to support " + type + " as input or "
                + "use an ObjectMap in " + plugin() + "#" + rootName() + "#" + name());
    }

    Object writeReplace() throws ObjectStreamException {
        return new SerializationReplacer(plugin(), rootName(), name(), serializeDelegate());
    }

    protected static Serializable loadDelegate(final byte[] value, final String plugin)
            throws IOException, ClassNotFoundException {
        try (final ObjectInputStream ois = new EnhancedObjectInputStream(new ByteArrayInputStream(value),
                ContainerFinder.Instance.get().find(plugin).classloader())) {
            return Serializable.class.cast(ois.readObject());
        }
    }

    @AllArgsConstructor
    private static class SerializationReplacer implements Serializable {

        private final String plugin;

        private final String component;

        private final String name;

        private final byte[] value;

        Object readResolve() throws ObjectStreamException {
            try {
                return new ProcessorImpl(component, name, plugin, loadDelegate(value, plugin));
            } catch (final IOException | ClassNotFoundException e) {
                throw new InvalidObjectException(e.getMessage());
            }
        }
    }
}
