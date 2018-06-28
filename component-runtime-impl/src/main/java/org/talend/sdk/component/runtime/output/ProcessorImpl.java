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
package org.talend.sdk.component.runtime.output;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.json.JsonObject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.talend.sdk.component.api.processor.AfterGroup;
import org.talend.sdk.component.api.processor.BeforeGroup;
import org.talend.sdk.component.api.processor.ElementListener;
import org.talend.sdk.component.api.processor.Input;
import org.talend.sdk.component.api.processor.Output;
import org.talend.sdk.component.runtime.base.Delegated;
import org.talend.sdk.component.runtime.base.LifecycleImpl;
import org.talend.sdk.component.runtime.serialization.ContainerFinder;
import org.talend.sdk.component.runtime.serialization.EnhancedObjectInputStream;

import lombok.AllArgsConstructor;

public class ProcessorImpl extends LifecycleImpl implements Processor, Delegated {

    private transient List<Method> beforeGroup;

    private transient List<Method> afterGroup;

    private transient Method process;

    private transient List<BiFunction<InputFactory, OutputFactory, Object>> parameterBuilderProcess;

    private transient Map<Method, List<Function<OutputFactory, Object>>> parameterBuilderAfterGroup;

    private transient Jsonb jsonb;

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

            // IMPORTANT: ensure you call only once the create(....), see studio integration
            // (mojo)
            parameterBuilderProcess =
                    Stream.of(process.getParameters()).map(this::buildProcessParamBuilder).collect(toList());
            parameterBuilderAfterGroup = afterGroup
                    .stream()
                    .map(after -> new AbstractMap.SimpleEntry<>(after,
                            Stream.of(after.getParameters()).map(this::toOutputParamBuilder).collect(toList())))
                    .collect(toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));
            forwardReturn = process.getReturnType() != void.class;
        }

        beforeGroup.forEach(this::doInvoke);
    }

    private BiFunction<InputFactory, OutputFactory, Object> buildProcessParamBuilder(final Parameter parameter) {
        if (parameter.isAnnotationPresent(Output.class)) {
            return (inputs, outputs) -> {
                final String name = parameter.getAnnotation(Output.class).value();
                return outputs.create(name);
            };
        }

        final Class<?> parameterType = parameter.getType();
        final boolean isGeneric = JsonObject.class.isAssignableFrom(parameterType);
        final String inputName =
                ofNullable(parameter.getAnnotation(Input.class)).map(Input::value).orElse(Branches.DEFAULT_BRANCH);
        return (inputs, outputs) -> doConvertInput(parameterType, isGeneric, inputs.read(inputName));
    }

    private Function<OutputFactory, Object> toOutputParamBuilder(final Parameter parameter) {
        return (outputs) -> {
            final String name = parameter.getAnnotation(Output.class).value();
            return outputs.create(name);
        };
    }

    private Object doConvertInput(final Class<?> parameterType, final boolean isGeneric, final Object data) {
        if (data == null) {
            return null;
        }
        if (isGeneric) {
            if (JsonObject.class.isInstance(data)) {
                return data;
            }
            final Jsonb jsonb = jsonb();
            return jsonb.fromJson(jsonb.toJson(data), JsonObject.class);
        }
        if (parameterType.isInstance(data) || parameterType.isPrimitive()/* this case can need some more work */) {
            return data;
        }
        // here we need to subclass parameter.getType to support an objectmap as input
        final Jsonb jsonb = jsonb();
        return jsonb.fromJson(
                JsonObject.class.isInstance(data) ? JsonObject.class.cast(data).toString() : jsonb.toJson(data),
                parameterType);
    }

    private Jsonb jsonb() {
        if (jsonb == null) {
            synchronized (this) {
                if (jsonb == null) {
                    jsonb = ContainerFinder.Instance.get().find(plugin()).findService(Jsonb.class);
                }
                if (jsonb == null) { // for tests mainly
                    jsonb = JsonbBuilder.create();
                }
            }
        }
        return jsonb;
    }

    @Override
    public void afterGroup(final OutputFactory output) {
        afterGroup.forEach(after -> doInvoke(after,
                parameterBuilderAfterGroup.get(after).stream().map(b -> b.apply(output)).toArray(Object[]::new)));
    }

    @Override
    public void onNext(final InputFactory inputFactory, final OutputFactory outputFactory) {
        final Object out = doInvoke(process,
                parameterBuilderProcess.stream().map(b -> b.apply(inputFactory, outputFactory)).toArray(Object[]::new));
        if (forwardReturn) {
            outputFactory.create(Branches.DEFAULT_BRANCH).emit(out);
        }
    }

    @Override
    public Object getDelegate() {
        return delegate;
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
