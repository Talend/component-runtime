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
package org.talend.sdk.component.runtime.beam.impl;

import static java.util.Collections.singletonList;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.List;
import java.util.function.Supplier;

import org.apache.beam.sdk.io.BoundedSource;
import org.apache.beam.sdk.io.Read;
import org.apache.beam.sdk.io.Source;
import org.apache.beam.sdk.io.UnboundedSource;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.values.PBegin;
import org.talend.sdk.component.runtime.base.Delegated;
import org.talend.sdk.component.runtime.base.Serializer;
import org.talend.sdk.component.runtime.input.Input;
import org.talend.sdk.component.runtime.input.Mapper;
import org.talend.sdk.component.runtime.output.Processor;
import org.talend.sdk.component.runtime.serialization.ContainerFinder;
import org.talend.sdk.component.runtime.serialization.EnhancedObjectInputStream;

import lombok.AllArgsConstructor;
import lombok.Data;

public class BeamMapperImpl implements Mapper, Serializable, Delegated {

    private final PipelineOptions options = PipelineOptionsFactory.create();

    private final Object original;

    private final FlowDefinition flow;

    private final String family;

    private final String name;

    private final String plugin;

    private final ClassLoader loader;

    public BeamMapperImpl(final PTransform<PBegin, ?> begin, final String plugin, final String family,
            final String name) {
        this(begin, createFlowDefinition(begin, plugin, family, name), plugin, family, name);
    }

    protected BeamMapperImpl(final Object original, final FlowDefinition flow, final String plugin, final String family,
            final String name) {
        this.original = original;
        this.flow = flow;
        this.plugin = plugin;
        this.family = family;
        this.name = name;
        this.loader = ContainerFinder.Instance.get().find(plugin()).classloader();
    }

    @Override
    public long assess() {
        return execute(() -> {
            try {
                return BoundedSource.class.isInstance(flow.source)
                        ? BoundedSource.class.cast(flow.source).getEstimatedSizeBytes(options)
                        : 1;
            } catch (final Exception e) {
                throw new IllegalStateException(e);
            }
        });
    }

    @Override
    public List<Mapper> split(final long desiredSize) {
        return singletonList(this);
    }

    @Override
    public Input create() {
        return execute(() -> {
            try {
                final boolean isBounded = BoundedSource.class.isInstance(flow.source);
                final Source.Reader<?> reader = isBounded ? BoundedSource.class.cast(flow.source).createReader(options)
                        : UnboundedSource.class.cast(flow.source).createReader(options, null);
                return new BeamInput(reader, flow.processor, plugin, family, name, loader, isBounded ? 0 : 30);
            } catch (final IOException e) {
                throw new IllegalArgumentException(e);
            }
        });
    }

    @Override
    public boolean isStream() {
        return UnboundedSource.class.isInstance(flow.source);
    }

    @Override
    public String plugin() {
        return plugin;
    }

    @Override
    public String rootName() {
        return family;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public void start() {
        // no-op
    }

    @Override
    public void stop() {
        // no-op
    }

    @Override
    public Object getDelegate() {
        return original;
    }

    private <T> T execute(final Supplier<T> task) {
        final Thread thread = Thread.currentThread();
        final ClassLoader tccl = thread.getContextClassLoader();
        thread.setContextClassLoader(this.loader);
        try {
            return task.get();
        } finally {
            thread.setContextClassLoader(tccl);
        }
    }

    Object writeReplace() throws ObjectStreamException {
        return new SerializationReplacer(plugin(), rootName(), name(), Serializer.toBytes(flow));
    }

    private static FlowDefinition createFlowDefinition(final PTransform<PBegin, ?> begin, final String plugin,
            final String family, final String name) {
        final CapturingPipeline capturingPipeline = new CapturingPipeline(PipelineOptionsFactory.create());
        final CapturingPipeline.SourceExtractor sourceExtractor = new CapturingPipeline.SourceExtractor();
        capturingPipeline.apply(begin);
        capturingPipeline.traverseTopologically(sourceExtractor);
        PTransform<? super PBegin, ?> transform = sourceExtractor.getTransform();
        if (transform == null) {
            capturingPipeline.apply(begin);
            transform = capturingPipeline.getRoot();
        }

        if (Read.Bounded.class.isInstance(transform)) {
            final Processor processor = sourceExtractor.getTransforms().isEmpty() ? null
                    : new BeamProcessorChainImpl(sourceExtractor.getTransforms(), capturingPipeline.getCoderRegistry(),
                            plugin, family, name);
            return new FlowDefinition(Read.Bounded.class.cast(transform).getSource(), processor);
        }
        if (Read.Unbounded.class.isInstance(transform)) {
            final Processor processor = sourceExtractor.getTransforms().isEmpty() ? null
                    : new BeamProcessorChainImpl(sourceExtractor.getTransforms(), capturingPipeline.getCoderRegistry(),
                            plugin, family, name);
            return new FlowDefinition(Read.Unbounded.class.cast(transform).getSource(), processor);
        }

        throw new InvalidMapperPipelineException("This implementation only supports Bounded sources");
    }

    public static class InvalidMapperPipelineException extends RuntimeException {

        public InvalidMapperPipelineException(final String message) {
            super(message);
        }
    }

    @Data
    private static class FlowDefinition implements Serializable {

        private final Source<?> source;

        private final Processor processor;
    }

    @AllArgsConstructor
    private static class SerializationReplacer implements Serializable {

        private final String plugin;

        private final String component;

        private final String name;

        private final byte[] value;

        Object readResolve() throws ObjectStreamException {
            try {
                final FlowDefinition flow = FlowDefinition.class.cast(loadDelegate());
                return new BeamMapperImpl(flow, flow, plugin, component, name);
            } catch (final IOException | ClassNotFoundException e) {
                throw new InvalidObjectException(e.getMessage());
            }
        }

        private Serializable loadDelegate() throws IOException, ClassNotFoundException {
            try (final ObjectInputStream ois = new EnhancedObjectInputStream(new ByteArrayInputStream(value),
                    ContainerFinder.Instance.get().find(plugin).classloader())) {
                return Serializable.class.cast(ois.readObject());
            }
        }
    }
}
