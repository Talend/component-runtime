/**
 *  Copyright (C) 2006-2017 Talend Inc. - www.talend.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.talend.sdk.component.runtime.beam.impl;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.apache.beam.sdk.coders.CannotProvideCoderException;
import org.apache.beam.sdk.coders.Coder;
import org.apache.beam.sdk.coders.CoderRegistry;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.values.PBegin;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.WindowingStrategy;
import org.talend.sdk.component.runtime.base.Delegated;
import org.talend.sdk.component.runtime.base.Lifecycle;
import org.talend.sdk.component.runtime.base.Serializer;
import org.talend.sdk.component.runtime.output.Branches;
import org.talend.sdk.component.runtime.output.InputFactory;
import org.talend.sdk.component.runtime.output.OutputFactory;
import org.talend.sdk.component.runtime.output.Processor;
import org.talend.sdk.component.runtime.serialization.ContainerFinder;
import org.talend.sdk.component.runtime.serialization.EnhancedObjectInputStream;

import lombok.AllArgsConstructor;

// light reflection based impl of ByteBuddyDoFnRunner of beam
// see org.apache.beam.sdk.transforms.reflect.DoFnSignatures.parseSignature() for the logic
//
// note: this is output oriented so states, timers, new SDF API, ... are not supported
public class BeamProcessorChainImpl implements Processor, Serializable, Delegated {

    private final Object original;

    private final String family;

    private final String name;

    private final String plugin;

    private final List<Processor> processors;

    private final List<Processor> preProcessors;

    private final Processor lastProcessor;

    private int startedCount;

    private int beforeChunkCount;

    public BeamProcessorChainImpl(final PTransform<PCollection<?>, ?> transform, final CoderRegistry coderRegistry,
        final String plugin, final String family, final String name) {
        this(singletonList(transform), coderRegistry, plugin, family, name);
    }

    public BeamProcessorChainImpl(final List<PTransform<PCollection<?>, ?>> transforms,
        final CoderRegistry coderRegistry, final String plugin, final String family, final String name) {
        this(transforms.get(transforms.size() - 1),
            transforms.stream().flatMap(t -> toProcessors(t, coderRegistry, plugin, family, name)).collect(toList()),
            plugin, family, name);
    }

    protected BeamProcessorChainImpl(final Object original, final List<Processor> processors, final String plugin,
        final String family, final String name) {
        this.original = original;
        this.plugin = plugin;
        this.family = family;
        this.name = name;
        this.processors = processors;
        this.preProcessors = processors.size() <= 1 ? emptyList() : processors.subList(0, processors.size() - 1);
        this.lastProcessor = processors.get(processors.size() - 1);
    }

    Object writeReplace() throws ObjectStreamException {
        return new SerializationReplacer(plugin(), rootName(), name(), Serializer.toBytes(processors));
    }

    @Override
    public void beforeGroup() {
        beforeChunkCount = 0;
        for (final Processor p : processors) {
            p.beforeGroup();
            beforeChunkCount++;
        }
    }

    @Override
    public void afterGroup(final OutputFactory output) {
        if (beforeChunkCount == 0) {
            return;
        }
        final List<Processor> toExecute = new ArrayList<>(processors.subList(0, beforeChunkCount));
        Collections.reverse(toExecute);
        toExecute.forEach(p -> p.afterGroup(output));
        beforeChunkCount = 0;
    }

    @Override
    public void onNext(final InputFactory input, final OutputFactory output) {
        Collection<InputFactory> finalInput = singletonList(input);
        if (!preProcessors.isEmpty()) {
            for (final Processor p : preProcessors) {
                final StoringOuputFactory tmpOutput = new StoringOuputFactory();
                finalInput.forEach(in -> p.onNext(in, tmpOutput));
                if (tmpOutput.getValues() == null) { // chain is stopped
                    return;
                }
                finalInput = tmpOutput.getValues().stream().map(val -> (InputFactory) name -> {
                    if (!Branches.DEFAULT_BRANCH.equals(name)) {
                        throw new IllegalArgumentException("Only default branch is supported at the moment");
                    }
                    return val;
                }).collect(toList());
            }
        }
        finalInput.forEach(in -> lastProcessor.onNext(in, output));
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
        startedCount = 0;
        for (final Processor p : processors) {
            p.start();
            startedCount++;
        }
    }

    @Override
    public void stop() {
        if (startedCount == 0) {
            return;
        }
        final List<Processor> toExecute = new ArrayList<>(processors.subList(0, startedCount));
        Collections.reverse(toExecute);
        toExecute.forEach(Lifecycle::stop);
        startedCount = 0;
    }

    @Override
    public Object getDelegate() {
        // todo: revisit?
        return original;
    }

    private static Stream<Processor> toProcessors(final PTransform<PCollection<?>, ?> transform,
        final CoderRegistry coderRegistry, final String plugin, final String family, final String name) {
        return extractDoFn(transform, coderRegistry).stream()
            .map(fn -> new BeamProcessorImpl(fn, fn, plugin, family, name));
    }

    private static Collection<DoFn<?, ?>> extractDoFn(final PTransform<PCollection<?>, ?> transform,
        final CoderRegistry coderRegistry) {
        final CapturingPipeline capturingPipeline = new CapturingPipeline(PipelineOptionsFactory.create());
        if (coderRegistry != null) {
            capturingPipeline.setCoderRegistry(coderRegistry);
        }
        capturingPipeline.apply(new PTransform<PBegin, PCollection<Object>>() {

            @Override
            public PCollection<Object> expand(final PBegin input) {
                return PCollection.createPrimitiveOutputInternal(capturingPipeline, WindowingStrategy.globalDefault(),
                    PCollection.IsBounded.BOUNDED, TypingCoder.INSTANCE);
            }

            @Override
            protected Coder<?> getDefaultOutputCoder() throws CannotProvideCoderException {
                return TypingCoder.INSTANCE;
            }
        }).apply(transform);

        final CapturingPipeline.SinkExtractor sinkExtractor = new CapturingPipeline.SinkExtractor();
        capturingPipeline.traverseTopologically(sinkExtractor);

        return sinkExtractor.getOutputs();
    }

    @AllArgsConstructor
    private static class SerializationReplacer implements Serializable {

        private final String plugin;

        private final String component;

        private final String name;

        private final byte[] value;

        Object readResolve() throws ObjectStreamException {
            try {
                final List<Processor> processors = List.class.cast(loadDelegate());
                return new BeamProcessorChainImpl(processors, processors, plugin, component, name);
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

    private static class TypingCoder extends Coder<Object> {

        private static final Coder<Object> INSTANCE = new TypingCoder();

        @Override
        public void encode(final Object o, final OutputStream outputStream) throws IOException {
            // no-op
        }

        @Override
        public Object decode(final InputStream inputStream) throws IOException {
            return null;
        }

        @Override
        public List<? extends Coder<?>> getCoderArguments() {
            return emptyList();
        }

        @Override
        public void verifyDeterministic() throws NonDeterministicException {
            // no-op
        }
    }
}
