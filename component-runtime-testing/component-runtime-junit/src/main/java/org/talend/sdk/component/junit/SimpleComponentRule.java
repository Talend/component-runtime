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
package org.talend.sdk.component.junit;

import static java.util.Collections.emptyMap;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.stream.Collectors.toList;
import static org.apache.ziplock.JarLocation.jarLocation;
import static org.talend.sdk.component.junit.SimpleFactory.configurationByExample;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.talend.sdk.component.runtime.input.Input;
import org.talend.sdk.component.runtime.input.Mapper;
import org.talend.sdk.component.runtime.manager.ComponentManager;
import org.talend.sdk.component.runtime.manager.ContainerComponentRegistry;
import org.talend.sdk.component.runtime.manager.chain.CountingSuccessListener;
import org.talend.sdk.component.runtime.manager.chain.ExecutionChainBuilder;
import org.talend.sdk.component.runtime.manager.chain.ToleratingErrorHandler;
import org.talend.sdk.component.runtime.output.Processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class SimpleComponentRule implements TestRule {

    static final ThreadLocal<State> STATE = new ThreadLocal<>();

    private final String packageName;

    private final ThreadLocal<PreState> initState = ThreadLocal.withInitial(PreState::new);

    @Override
    public Statement apply(final Statement base, final Description description) {
        return new Statement() {

            @Override
            public void evaluate() throws Throwable {
                try (final EmbeddedComponentManager manager = new EmbeddedComponentManager(packageName)) {
                    STATE.set(new State(manager, new ArrayList<>(), initState.get().emitter));
                    base.evaluate();
                } finally {
                    STATE.remove();
                    initState.remove();
                }
            }
        };
    }

    public Outputs collect(final Processor processor, final ControllableInputFactory inputs) {
        return collect(processor, inputs, 10);
    }

    /**
     * Collects all outputs of a processor.
     *
     * @param processor
     * the processor to run while there are inputs.
     * @param inputs
     * the input factory, when an input will return null it will stop the
     * processing.
     * @param bundleSize
     * the bundle size to use.
     * @return a map where the key is the output name and the value a stream of the
     * output values.
     */
    public Outputs collect(final Processor processor, final ControllableInputFactory inputs, final int bundleSize) {
        final AutoChunkProcessor autoChunkProcessor = new AutoChunkProcessor(bundleSize, processor);
        autoChunkProcessor.start();
        final Outputs outputs = new Outputs();
        try {
            while (inputs.hasMoreData()) {
                autoChunkProcessor.onElement(inputs, name -> value -> {
                    final List aggregator = outputs.data.computeIfAbsent(name, n -> new ArrayList<>());
                    aggregator.add(value);
                });
            }
        } finally {
            autoChunkProcessor.stop();
        }
        return outputs;
    }

    /**
     * Collects data emitted from this mapper. If the split creates more than one
     * mapper, it will create as much threads as mappers otherwise it will use the
     * caller thread.
     *
     * IMPORTANT: don't forget to consume all the stream to ensure the underlying
     * {@see org.talend.sdk.component.runtime.input.Input} is closed.
     *
     * @param recordType
     * the record type to use to type the returned type.
     * @param mapper
     * the mapper to go through.
     * @param maxRecords
     * maximum number of records, allows to stop the source when
     * infinite.
     * @param <T>
     * the returned type of the records of the mapper.
     * @return all the records emitted by the mapper.
     */
    public <T> Stream<T> collect(final Class<T> recordType, final Mapper mapper, final int maxRecords) {
        mapper.start();

        final long assess = mapper.assess();
        final int proc = Math.max(1, Runtime.getRuntime().availableProcessors());
        final List<Mapper> mappers = mapper.split(Math.max(assess / proc, 1));
        switch (mappers.size()) {
        case 0:
            return Stream.empty();
        case 1:
            return asStream(
                    asIterator(mappers.iterator().next().create(), mapper::stop, new AtomicInteger(maxRecords)));
        default:
            final ExecutorService es = Executors.newFixedThreadPool(mappers.size());
            final Runnable cleaner = new Runnable() {

                private int remaining = mappers.size();

                @Override
                public void run() {
                    synchronized (this) {
                        remaining--;
                        if (remaining > 0) {
                            return;
                        }
                    }

                    if (es.isShutdown()) {
                        return;
                    }

                    es.shutdown();

                    mapper.stop();

                    try {
                        final int timeout = Integer.getInteger("talend.component.junit.timeout", 5);
                        if (!es.awaitTermination(timeout, MINUTES)) {
                            throw new IllegalStateException("Mapper collection lasted for more than " + timeout
                                    + "mn, you can customize "
                                    + "this value setting the system property -Dtalend.component.junit.timeout=x, x in minutes.");
                        }
                    } catch (final InterruptedException e) {
                        Thread.interrupted();
                        throw new IllegalStateException(e);
                    }
                }
            };
            final AtomicInteger recordCounter = new AtomicInteger(maxRecords);
            return mappers
                    .stream()
                    .map(m -> asIterator(m.create(), cleaner, recordCounter))
                    .map(this::asStream)
                    .collect(Stream::empty, Stream::concat, Stream::concat);
        }
    }

    private <T> Stream<T> asStream(final Iterator<T> iterator) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.IMMUTABLE), false);
    }

    private <T> Iterator<T> asIterator(final Input input, final Runnable onLast, final AtomicInteger counter) {
        input.start();
        return new Iterator<T>() {

            private boolean closed;

            private Object next;

            @Override
            public boolean hasNext() {
                final int remaining = counter.get();
                if (remaining <= 0) {
                    return false;
                }

                final boolean hasNext = (next = input.next()) != null;
                if (!hasNext && !closed) {
                    closed = true;
                    try {
                        input.stop();
                    } finally {
                        try {
                            onLast.run();
                        } catch (final RuntimeException re) {
                            log.warn(re.getMessage(), re);
                        }
                    }
                }
                counter.decrementAndGet();
                return hasNext;
            }

            @Override
            public T next() {
                return (T) next;
            }
        };
    }

    public <T> List<T> collectAsList(final Class<T> recordType, final Mapper mapper) {
        return collectAsList(recordType, mapper, 1000);
    }

    public <T> List<T> collectAsList(final Class<T> recordType, final Mapper mapper, final int maxRecords) {
        return collect(recordType, mapper, maxRecords).collect(toList());
    }

    public Mapper createMapper(final Class<?> componentType, final Object configuration) {
        return create(Mapper.class, componentType, configuration);
    }

    public Processor createProcessor(final Class<?> componentType, final Object configuration) {
        return create(Processor.class, componentType, configuration);
    }

    private <C, T, A> A create(final Class<A> api, final Class<T> componentType, final C configuration) {
        return api.cast(asManager()
                .find(c -> c.get(ContainerComponentRegistry.class).getComponents().values().stream())
                .flatMap(f -> Stream.concat(f.getProcessors().values().stream(),
                        f.getPartitionMappers().values().stream()))
                .filter(m -> m.getType().getName().equals(componentType.getName()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No component " + componentType))
                .getInstantiator()
                .apply(configurationByExample(configuration)));
    }

    public <T> List<T> collect(final Class<T> recordType, final String family, final String component,
            final int version, final Map<String, String> configuration) {
        ExecutionChainBuilder
                .start()
                .withConfiguration("test", true)
                .fromInput(family, component, version, configuration)
                .toProcessor("test", "collector", 1, emptyMap())
                .create(asManager(), file -> {
                    throw new IllegalArgumentException();
                }, new CountingSuccessListener(), new ToleratingErrorHandler(0))
                .get()
                .execute();
        return getCollectedData(recordType);
    }

    public <T> void process(final Iterable<T> inputs, final String family, final String component, final int version,
            final Map<String, String> configuration) {
        setInputData(inputs);
        ExecutionChainBuilder
                .start()
                .withConfiguration("test", true)
                .fromInput("test", "emitter", 1, emptyMap())
                .toProcessor(family, component, version, configuration)
                .create(asManager(), file -> {
                    throw new IllegalArgumentException();
                }, new CountingSuccessListener(), new ToleratingErrorHandler(0))
                .get()
                .execute();
    }

    public ComponentManager asManager() {
        return STATE.get().manager;
    }

    public <T> void setInputData(final Iterable<T> data) {
        initState.get().emitter = data.iterator();
    }

    public <T> List<T> getCollectedData(final Class<T> recordType) {
        return STATE.get().collector.stream().filter(recordType::isInstance).map(recordType::cast).collect(toList());
    }

    static class PreState {

        Iterator<?> emitter;
    }

    @RequiredArgsConstructor
    static class State {

        final ComponentManager manager;

        final Collection<Object> collector;

        final Iterator<?> emitter;
    }

    private static class EmbeddedComponentManager extends ComponentManager {

        private final ComponentManager oldInstance;

        private EmbeddedComponentManager(final String componentPackage) {
            super(findM2(), "TALEND-INF/dependencies.txt", "org.talend.sdk.component:type=component,value=%s");
            addJarContaining(Thread.currentThread().getContextClassLoader(), componentPackage.replace('.', '/'));
            container.create("component-runtime-junit.jar", jarLocation(SimpleCollector.class).getAbsolutePath());
            oldInstance = CONTEXTUAL_INSTANCE.get();
            CONTEXTUAL_INSTANCE.set(this);
        }

        @Override
        public void close() {
            try {
                super.close();
            } finally {
                CONTEXTUAL_INSTANCE.compareAndSet(this, oldInstance);
            }
        }

        @Override
        protected boolean isContainerClass(final String name) {
            /*
             * return super.isContainerClass(value) || value.startsWith(componentPackage) ||
             * value.startsWith("org.junit.") ||
             * value.startsWith("org.talend.sdk.component.junit");
             */
            return true; // embedded mode (no plugin structure) so just run with all classes in parent
                         // classloader
        }
    }

    public static class Outputs {

        private final Map<String, List<?>> data = new HashMap<>();

        public int size() {
            return data.size();
        }

        public Set<String> keys() {
            return data.keySet();
        }

        public <T> List<T> get(final Class<T> type, final String name) {
            return (List<T>) data.get(name);
        }
    }
}
