/**
 * Copyright (C) 2006-2026 Talend Inc. - www.talend.com
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

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.talend.sdk.component.runtime.reflect.Parameters.isGroupBuffer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;
import javax.json.bind.config.BinaryDataStrategy;
import javax.json.spi.JsonProvider;

import org.talend.sdk.component.api.processor.AfterGroup;
import org.talend.sdk.component.api.processor.BeforeGroup;
import org.talend.sdk.component.api.processor.ElementListener;
import org.talend.sdk.component.api.processor.Input;
import org.talend.sdk.component.api.processor.LastGroup;
import org.talend.sdk.component.api.processor.Output;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;
import org.talend.sdk.component.runtime.base.Delegated;
import org.talend.sdk.component.runtime.base.LifecycleImpl;
import org.talend.sdk.component.runtime.record.RecordBuilderFactoryImpl;
import org.talend.sdk.component.runtime.record.RecordConverters;
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

    private transient JsonBuilderFactory jsonBuilderFactory;

    private transient RecordBuilderFactory recordBuilderFactory;

    private transient JsonProvider jsonProvider;

    private transient boolean forwardReturn;

    private transient RecordConverters converter;

    private transient Class<?> expectedRecordType;

    private transient Collection<Object> records;

    private Map<String, String> internalConfiguration;

    private RecordConverters.MappingMetaRegistry mappings;

    public ProcessorImpl(final String rootName, final String name, final String plugin,
            final Map<String, String> internalConfiguration, final Serializable delegate) {
        super(delegate, rootName, name, plugin);
        this.internalConfiguration = internalConfiguration;
    }

    protected ProcessorImpl() {
        // no-op
    }

    public Map<String, String> getInternalConfiguration() {
        return ofNullable(internalConfiguration).orElseGet(Collections::emptyMap);
    }

    @Override
    public void beforeGroup() {
        if (beforeGroup == null) {
            beforeGroup = findMethods(BeforeGroup.class).collect(toList());
            afterGroup = findMethods(AfterGroup.class).collect(toList());
            process = findMethods(ElementListener.class).findFirst().orElse(null);

            // IMPORTANT: ensure you call only once the create(....), see studio integration (mojo)
            parameterBuilderProcess = process == null ? emptyList()
                    : Stream.of(process.getParameters()).map(this::buildProcessParamBuilder).collect(toList());
            parameterBuilderAfterGroup = afterGroup
                    .stream()
                    .map(after -> new AbstractMap.SimpleEntry<>(after, Stream.of(after.getParameters())
                            .map(param -> {
                                if (isGroupBuffer(param.getParameterizedType())) {
                                    expectedRecordType = Class.class
                                            .cast(ParameterizedType.class
                                                    .cast(param.getParameterizedType())
                                                    .getActualTypeArguments()[0]);
                                    return (Function<OutputFactory, Object>) o -> records;
                                }
                                return toOutputParamBuilder(param);
                            })
                            .collect(toList())))
                    .collect(toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));
            forwardReturn = process != null && process.getReturnType() != void.class;

            converter = new RecordConverters();

            mappings = new RecordConverters.MappingMetaRegistry();
        }

        beforeGroup.forEach(this::doInvoke);
        if (process == null) { // collect records for @AfterGroup param
            records = new ArrayList<>();
        }
    }

    private BiFunction<InputFactory, OutputFactory, Object> buildProcessParamBuilder(final Parameter parameter) {
        if (parameter.isAnnotationPresent(Output.class)) {
            return (inputs, outputs) -> {
                final String name = parameter.getAnnotation(Output.class).value();
                return outputs.create(name);
            };
        }

        final Class<?> parameterType = parameter.getType();
        final String inputName =
                ofNullable(parameter.getAnnotation(Input.class)).map(Input::value).orElse(Branches.DEFAULT_BRANCH);
        return (inputs, outputs) -> doConvertInput(parameterType, inputs.read(inputName));
    }

    private Function<OutputFactory, Object> toOutputParamBuilder(final Parameter parameter) {
        return outputs -> {
            if (parameter.isAnnotationPresent(LastGroup.class)) {
                return false;
            }
            final String name = parameter.getAnnotation(Output.class).value();
            return outputs.create(name);
        };
    }

    private Object doConvertInput(final Class<?> parameterType, final Object data) {
        if (data == null || parameterType.isInstance(data)
                || parameterType.isPrimitive() /* mainly for tests, no > manager */) {
            return data;
        }
        return converter
                .toType(mappings, data, parameterType, this::jsonBuilderFactory, this::jsonProvider, this::jsonb,
                        this::recordBuilderFactory);
    }

    private Jsonb jsonb() {
        if (jsonb != null) {
            return jsonb;
        }
        synchronized (this) {
            if (jsonb == null) {
                jsonb = ContainerFinder.Instance.get().find(plugin()).findService(Jsonb.class);
            }
            if (jsonb == null) { // for tests mainly
                jsonb = JsonbBuilder.create(new JsonbConfig().withBinaryDataStrategy(BinaryDataStrategy.BASE_64));
            }
        }
        return jsonb;
    }

    private RecordBuilderFactory recordBuilderFactory() {
        if (recordBuilderFactory != null) {
            return recordBuilderFactory;
        }
        synchronized (this) {
            if (recordBuilderFactory == null) {
                recordBuilderFactory =
                        ContainerFinder.Instance.get().find(plugin()).findService(RecordBuilderFactory.class);
            }
            if (recordBuilderFactory == null) {
                recordBuilderFactory = new RecordBuilderFactoryImpl("$volatile");
            }
        }

        return recordBuilderFactory;
    }

    private JsonBuilderFactory jsonBuilderFactory() {
        if (jsonBuilderFactory != null) {
            return jsonBuilderFactory;
        }
        synchronized (this) {
            if (jsonBuilderFactory == null) {
                jsonBuilderFactory =
                        ContainerFinder.Instance.get().find(plugin()).findService(JsonBuilderFactory.class);
            }
            if (jsonBuilderFactory == null) {
                jsonBuilderFactory = Json.createBuilderFactory(emptyMap());
            }
        }
        return jsonBuilderFactory;
    }

    private JsonProvider jsonProvider() {
        if (jsonProvider != null) {
            return jsonProvider;
        }
        synchronized (this) {
            if (jsonProvider == null) {
                jsonProvider = ContainerFinder.Instance.get().find(plugin()).findService(JsonProvider.class);
            }
        }
        return jsonProvider;
    }

    @Override
    public void afterGroup(final OutputFactory output) {
        afterGroup.forEach(after -> {
            Object[] params = parameterBuilderAfterGroup.get(after)
                    .stream()
                    .map(b -> b.apply(output))
                    .toArray(Object[]::new);
            doInvoke(after, params);
        });
        if (records != null) {
            records = null;
        }
    }

    @Override
    public boolean isLastGroupUsed() {
        AtomicReference<Boolean> hasLastGroup = new AtomicReference<>(false);
        Optional.ofNullable(afterGroup)
                .orElse(new ArrayList<>())
                .forEach(after -> {
                    for (Parameter param : after.getParameters()) {
                        if (param.isAnnotationPresent(LastGroup.class)) {
                            hasLastGroup.set(true);
                        }
                    }
                });
        return hasLastGroup.get();
    }

    @Override
    public void afterGroup(final OutputFactory output, final boolean last) {
        afterGroup.forEach(after -> {
            Object[] params = Stream.concat(
                    parameterBuilderAfterGroup.get(after)
                            .stream()
                            .map(b -> b.apply(output))
                            .filter(b -> !b.equals(false)),
                    Stream.of(last)).toArray(Object[]::new);
            doInvoke(after, params);
        });
        if (records != null) {
            records = null;
        }
    }

    @Override
    public void onNext(final InputFactory inputFactory, final OutputFactory outputFactory) {
        if (process == null) {
            // todo: handle @Input there too? less likely it becomes useful
            records.add(doConvertInput(expectedRecordType, inputFactory.read(Branches.DEFAULT_BRANCH)));
        } else {
            final Object[] args = parameterBuilderProcess
                    .stream()
                    .map(b -> b.apply(inputFactory, outputFactory))
                    .toArray(Object[]::new);
            final Object out = doInvoke(process, args);
            if (forwardReturn) {
                outputFactory.create(Branches.DEFAULT_BRANCH).emit(out);
            }
        }
    }

    @Override
    public Object getDelegate() {
        return delegate;
    }

    Object writeReplace() throws ObjectStreamException {
        return new SerializationReplacer(plugin(), rootName(), name(), internalConfiguration, serializeDelegate());
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

        private final Map<String, String> internalConfiguration;

        private final byte[] value;

        Object readResolve() throws ObjectStreamException {
            try {
                return new ProcessorImpl(component, name, plugin, internalConfiguration, loadDelegate(value, plugin));
            } catch (final IOException | ClassNotFoundException e) {
                final InvalidObjectException invalidObjectException = new InvalidObjectException(e.getMessage());
                invalidObjectException.initCause(e);
                throw invalidObjectException;
            }
        }
    }
}
