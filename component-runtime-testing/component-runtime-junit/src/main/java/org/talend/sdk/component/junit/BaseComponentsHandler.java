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
package org.talend.sdk.component.junit;

import static java.lang.Math.abs;
import static java.util.Collections.emptyMap;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.apache.ziplock.JarLocation.jarLocation;
import static org.junit.Assert.fail;
import static org.talend.sdk.component.junit.SimpleFactory.configurationByExample;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.json.JsonObject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbConfig;

import org.apache.xbean.finder.filter.Filter;
import org.talend.sdk.component.junit.lang.StreamDecorator;
import org.talend.sdk.component.runtime.base.Lifecycle;
import org.talend.sdk.component.runtime.input.Input;
import org.talend.sdk.component.runtime.input.Mapper;
import org.talend.sdk.component.runtime.manager.ComponentFamilyMeta;
import org.talend.sdk.component.runtime.manager.ComponentManager;
import org.talend.sdk.component.runtime.manager.ContainerComponentRegistry;
import org.talend.sdk.component.runtime.manager.chain.Job;
import org.talend.sdk.component.runtime.manager.json.PreComputedJsonpProvider;
import org.talend.sdk.component.runtime.output.Processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BaseComponentsHandler implements ComponentsHandler {

    static final ThreadLocal<State> STATE = new ThreadLocal<>();

    private final ThreadLocal<PreState> initState = ThreadLocal.withInitial(PreState::new);

    protected String packageName;

    public EmbeddedComponentManager start() {
        final EmbeddedComponentManager embeddedComponentManager = new EmbeddedComponentManager(packageName) {

            @Override
            public void close() {
                try {
                    final State state = STATE.get();
                    if (state.jsonb != null) {
                        try {
                            state.jsonb.close();
                        } catch (final Exception e) {
                            // no-op: not important
                        }
                    }
                    STATE.remove();
                    initState.remove();
                } finally {
                    super.close();
                }
            }
        };

        STATE.set(new State(embeddedComponentManager, new ArrayList<>(), initState.get().emitter));
        return embeddedComponentManager;
    }

    @Override
    public Outputs collect(final Processor processor, final ControllableInputFactory inputs) {
        return collect(processor, inputs, 10);
    }

    /**
     * Collects all outputs of a processor.
     *
     * @param processor the processor to run while there are inputs.
     * @param inputs the input factory, when an input will return null it will stop the
     * processing.
     * @param bundleSize the bundle size to use.
     * @return a map where the key is the output name and the value a stream of the
     * output values.
     */
    @Override
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

    @Override
    public <T> Stream<T> collect(final Class<T> recordType, final Mapper mapper, final int maxRecords) {
        return collect(recordType, mapper, maxRecords, Runtime.getRuntime().availableProcessors());
    }

    /**
     * Collects data emitted from this mapper. If the split creates more than one
     * mapper, it will create as much threads as mappers otherwise it will use the
     * caller thread.
     *
     * IMPORTANT: don't forget to consume all the stream to ensure the underlying
     * { @see org.talend.sdk.component.runtime.input.Input} is closed.
     *
     * @param recordType the record type to use to type the returned type.
     * @param mapper the mapper to go through.
     * @param maxRecords maximum number of records, allows to stop the source when
     * infinite.
     * @param concurrency requested (1 can be used instead if &lt;= 0) concurrency for the reader execution.
     * @param <T> the returned type of the records of the mapper.
     * @return all the records emitted by the mapper.
     */
    @Override
    public <T> Stream<T> collect(final Class<T> recordType, final Mapper mapper, final int maxRecords,
            final int concurrency) {
        mapper.start();

        final State state = STATE.get();
        final long assess = mapper.assess();
        final int proc = Math.max(1, concurrency);
        final List<Mapper> mappers = mapper.split(Math.max(assess / proc, 1));
        switch (mappers.size()) {
        case 0:
            return Stream.empty();
        case 1:
            return StreamDecorator.decorate(
                    asStream(asIterator(mappers.iterator().next().create(), new AtomicInteger(maxRecords))),
                    collect -> {
                        try {
                            collect.run();
                        } finally {
                            mapper.stop();
                        }
                    });
        default: // N producers-1 consumer pattern
            final AtomicInteger threadCounter = new AtomicInteger(0);
            final ExecutorService es = Executors.newFixedThreadPool(mappers.size(), r -> new Thread(r) {

                {
                    setName(BaseComponentsHandler.this.getClass().getSimpleName() + "-pool-" + abs(mapper.hashCode())
                            + "-" + threadCounter.incrementAndGet());
                }
            });
            final AtomicInteger recordCounter = new AtomicInteger(maxRecords);
            final Semaphore permissions = new Semaphore(0);
            final Queue<T> records = new ConcurrentLinkedQueue<>();
            final CountDownLatch latch = new CountDownLatch(mappers.size());
            final List<? extends Future<?>> tasks = mappers
                    .stream()
                    .map(Mapper::create)
                    .map(input -> (Iterator<T>) asIterator(input, recordCounter))
                    .map(it -> es.submit(() -> {
                        try {
                            while (it.hasNext()) {
                                final T next = it.next();
                                records.add(next);
                                permissions.release();
                            }
                        } finally {
                            latch.countDown();
                        }
                    }))
                    .collect(toList());
            es.shutdown();

            final int timeout = Integer.getInteger("talend.component.junit.timeout", 5);
            new Thread() {

                {
                    setName(BaseComponentsHandler.class.getSimpleName() + "-monitor_" + abs(mapper.hashCode()));
                }

                @Override
                public void run() {
                    try {
                        latch.await(timeout, MINUTES);
                    } catch (final InterruptedException e) {
                        Thread.interrupted();
                    } finally {
                        permissions.release();
                    }
                }
            }.start();
            return StreamDecorator.decorate(asStream(new Iterator<T>() {

                @Override
                public boolean hasNext() {
                    try {
                        permissions.acquire();
                    } catch (final InterruptedException e) {
                        Thread.interrupted();
                        fail(e.getMessage());
                    }
                    return !records.isEmpty();
                }

                @Override
                public T next() {
                    T poll = records.poll();
                    if (poll != null) {
                        return mapRecord(state, recordType, poll);
                    }
                    return null;
                }
            }), task -> {
                try {
                    task.run();
                } finally {
                    tasks.forEach(f -> {
                        try {
                            f.get(5, SECONDS);
                        } catch (final InterruptedException e) {
                            Thread.interrupted();
                        } catch (final ExecutionException | TimeoutException e) {
                            // no-op
                        } finally {
                            if (!f.isDone() && !f.isCancelled()) {
                                f.cancel(true);
                            }
                        }
                    });
                }
            });
        }
    }

    private <T> Stream<T> asStream(final Iterator<T> iterator) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.IMMUTABLE), false);
    }

    private <T> Iterator<T> asIterator(final Input input, final AtomicInteger counter) {
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
                    input.stop();
                }
                if (hasNext) {
                    counter.decrementAndGet();
                }
                return hasNext;
            }

            @Override
            public T next() {
                return (T) next;
            }
        };
    }

    @Override
    public <T> List<T> collectAsList(final Class<T> recordType, final Mapper mapper) {
        return collectAsList(recordType, mapper, 1000);
    }

    @Override
    public <T> List<T> collectAsList(final Class<T> recordType, final Mapper mapper, final int maxRecords) {
        return collect(recordType, mapper, maxRecords).collect(toList());
    }

    @Override
    public Mapper createMapper(final Class<?> componentType, final Object configuration) {
        return create(Mapper.class, componentType, configuration);
    }

    @Override
    public Processor createProcessor(final Class<?> componentType, final Object configuration) {
        return create(Processor.class, componentType, configuration);
    }

    private <C, T, A> A create(final Class<A> api, final Class<T> componentType, final C configuration) {
        final ComponentFamilyMeta.BaseMeta<? extends Lifecycle> meta = findMeta(componentType);
        return api.cast(meta
                .getInstantiator()
                .apply(configuration == null || meta.getParameterMetas().isEmpty() ? emptyMap()
                        : configurationByExample(configuration, meta
                                .getParameterMetas()
                                .stream()
                                .filter(p -> p.getName().equals(p.getPath()))
                                .findFirst()
                                .map(p -> p.getName() + '.')
                                .orElseThrow(() -> new IllegalArgumentException("Didn't find any option and therefore "
                                        + "can't convert the configuration instance to a configuration")))));
    }

    private <T> ComponentFamilyMeta.BaseMeta<? extends Lifecycle> findMeta(final Class<T> componentType) {
        return asManager()
                .find(c -> c.get(ContainerComponentRegistry.class).getComponents().values().stream())
                .flatMap(f -> Stream.concat(f.getProcessors().values().stream(),
                        f.getPartitionMappers().values().stream()))
                .filter(m -> m.getType().getName().equals(componentType.getName()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No component " + componentType));
    }

    @Override
    public <T> List<T> collect(final Class<T> recordType, final String family, final String component,
            final int version, final Map<String, String> configuration) {
        Job
                .components()
                .component("in",
                        family + "://" + component + "?__version=" + version
                                + configuration
                                        .entrySet()
                                        .stream()
                                        .map(entry -> entry.getKey() + "=" + entry.getValue())
                                        .collect(joining("&", "&", "")))
                .component("collector", "test://collector")
                .connections()
                .from("in")
                .to("collector")
                .build()
                .run();

        return getCollectedData(recordType);
    }

    @Override
    public <T> void process(final Iterable<T> inputs, final String family, final String component, final int version,
            final Map<String, String> configuration) {
        setInputData(inputs);

        Job
                .components()
                .component("emitter", "test://emitter")
                .component("out",
                        family + "://" + component + "?__version=" + version
                                + configuration
                                        .entrySet()
                                        .stream()
                                        .map(entry -> entry.getKey() + "=" + entry.getValue())
                                        .collect(joining("&", "&", "")))
                .connections()
                .from("emitter")
                .to("out")
                .build()
                .run();

    }

    @Override
    public ComponentManager asManager() {
        return STATE.get().manager;
    }

    public <T> T findService(final String plugin, final Class<T> serviceClass) {
        return serviceClass.cast(asManager()
                .findPlugin(plugin)
                .orElseThrow(() -> new IllegalArgumentException("cant find plugin '" + plugin + "'"))
                .get(ComponentManager.AllServices.class)
                .getServices()
                .get(serviceClass));
    }

    public <T> T findService(final Class<T> serviceClass) {
        return findService(
                Optional.of(getTestPlugins()).filter(c -> !c.isEmpty()).map(c -> c.iterator().next()).orElseThrow(
                        () -> new IllegalStateException("No component plugin found")),
                serviceClass);
    }

    public Set<String> getTestPlugins() {
        return EmbeddedComponentManager.class.cast(asManager()).testPlugins;
    }

    @Override
    public <T> void setInputData(final Iterable<T> data) {
        initState.get().emitter = data.iterator();
    }

    @Override
    public <T> List<T> getCollectedData(final Class<T> recordType) {
        final State state = STATE.get();
        return state.collector
                .stream()
                .filter(r -> recordType.isInstance(r) || JsonObject.class.isInstance(r))
                .map(r -> mapRecord(state, recordType, r))
                .collect(toList());
    }

    private <T> T mapRecord(final State state, final Class<T> recordType, final Object r) {
        if (recordType.isInstance(r)) {
            return recordType.cast(r);
        }
        if (JsonObject.class.isInstance(r)) {
            final Jsonb jsonb = state.jsonb();
            return jsonb.fromJson(jsonb.toJson(r), recordType);
        }
        throw new IllegalArgumentException("Unsupported record: " + r);
    }

    static class PreState {

        Iterator<?> emitter;
    }

    @RequiredArgsConstructor
    static class State {

        final ComponentManager manager;

        final Collection<Object> collector;

        final Iterator<?> emitter;

        volatile Jsonb jsonb;

        synchronized Jsonb jsonb() {
            if (jsonb == null) {
                jsonb = manager
                        .getJsonbProvider()
                        .create()
                        .withProvider(new PreComputedJsonpProvider("test", manager.getJsonpProvider(),
                                manager.getJsonpParserFactory(), manager.getJsonpWriterFactory(),
                                manager.getJsonpBuilderFactory(), manager.getJsonpGeneratorFactory(),
                                manager.getJsonpReaderFactory())) // reuses the same memory buffering
                        .withConfig(new JsonbConfig().setProperty("johnzon.cdi.activated", false))
                        .build();
            }
            return jsonb;
        }
    }

    public static class EmbeddedComponentManager extends ComponentManager {

        private final ComponentManager oldInstance;

        private final Set<String> testPlugins;

        private EmbeddedComponentManager(final String componentPackage) {
            super(findM2(), "TALEND-INF/dependencies.txt", "org.talend.sdk.component:type=component,value=%s");
            testPlugins = addJarContaining(Thread.currentThread().getContextClassLoader(),
                    componentPackage.replace('.', '/'));
            container
                    .builder("component-runtime-junit.jar", jarLocation(SimpleCollector.class).getAbsolutePath())
                    .create();
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
        protected boolean isContainerClass(final Filter filter, final String name) {
            // embedded mode (no plugin structure) so just run with all classes in parent classloader
            return true;
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
