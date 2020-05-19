/**
 * Copyright (C) 2006-2019 Talend Inc. - www.talend.com
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
import static java.util.Collections.emptyIterator;
import static java.util.Collections.emptyMap;
import static java.util.Locale.ROOT;
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbConfig;
import javax.json.spi.JsonProvider;

import org.apache.xbean.finder.filter.Filter;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.service.injector.Injector;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;
import org.talend.sdk.component.junit.lang.StreamDecorator;
import org.talend.sdk.component.runtime.base.Lifecycle;
import org.talend.sdk.component.runtime.input.Input;
import org.talend.sdk.component.runtime.input.Mapper;
import org.talend.sdk.component.runtime.manager.ComponentFamilyMeta;
import org.talend.sdk.component.runtime.manager.ComponentManager;
import org.talend.sdk.component.runtime.manager.ContainerComponentRegistry;
import org.talend.sdk.component.runtime.manager.chain.AutoChunkProcessor;
import org.talend.sdk.component.runtime.manager.chain.Job;
import org.talend.sdk.component.runtime.manager.json.PreComputedJsonpProvider;
import org.talend.sdk.component.runtime.output.OutputFactory;
import org.talend.sdk.component.runtime.output.Processor;
import org.talend.sdk.component.runtime.record.RecordConverters;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BaseComponentsHandler implements ComponentsHandler {

    protected static final Local<State> STATE = loadStateHolder();

    private static Local<State> loadStateHolder() {
        switch (System.getProperty("talend.component.junit.handler.state", "thread").toLowerCase(ROOT)) {
        case "static":
            return new Local.StaticImpl<>();
        default:
            return new Local.ThreadLocalImpl<>();
        }
    }

    private final ThreadLocal<PreState> initState = ThreadLocal.withInitial(PreState::new);

    protected String packageName;

    protected Collection<String> isolatedPackages;

    public <T> T injectServices(final T instance) {
        if (instance == null) {
            return null;
        }
        final String plugin = getSinglePlugin();
        final Map<Class<?>, Object> services = asManager()
                .findPlugin(plugin)
                .orElseThrow(() -> new IllegalArgumentException("cant find plugin '" + plugin + "'"))
                .get(ComponentManager.AllServices.class)
                .getServices();
        Injector.class.cast(services.get(Injector.class)).inject(instance);
        return instance;
    }

    public BaseComponentsHandler withIsolatedPackage(final String packageName, final String... packages) {
        isolatedPackages =
                Stream.concat(Stream.of(packageName), Stream.of(packages)).filter(Objects::nonNull).collect(toList());
        if (isolatedPackages.isEmpty()) {
            isolatedPackages = null;
        }
        return this;
    }

    public EmbeddedComponentManager start() {
        final EmbeddedComponentManager embeddedComponentManager = new EmbeddedComponentManager(packageName) {

            @Override
            protected boolean isContainerClass(final Filter filter, final String name) {
                if (name == null) {
                    return super.isContainerClass(filter, null);
                }
                return (isolatedPackages == null || isolatedPackages.stream().noneMatch(name::startsWith))
                        && super.isContainerClass(filter, name);
            }

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

        STATE
                .set(new State(embeddedComponentManager, new ArrayList<>(), initState.get().emitter, null, null, null,
                        null));
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
        final OutputFactory outputFactory = name -> value -> {
            final List aggregator = outputs.data.computeIfAbsent(name, n -> new ArrayList<>());
            aggregator.add(value);
        };
        try {
            while (inputs.hasMoreData()) {
                autoChunkProcessor.onElement(inputs, outputFactory);
            }
            autoChunkProcessor.flush(outputFactory);
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
            return StreamDecorator
                    .decorate(asStream(asIterator(mappers.iterator().next().create(), new AtomicInteger(maxRecords))),
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
                        Thread.currentThread().interrupt();
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
                        Thread.currentThread().interrupt();
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
                            Thread.currentThread().interrupt();
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
        return api
                .cast(meta
                        .getInstantiator()
                        .apply(configuration == null || meta.getParameterMetas().get().isEmpty() ? emptyMap()
                                : configurationByExample(configuration, meta
                                        .getParameterMetas()
                                        .get()
                                        .stream()
                                        .filter(p -> p.getName().equals(p.getPath()))
                                        .findFirst()
                                        .map(p -> p.getName() + '.')
                                        .orElseThrow(() -> new IllegalArgumentException(
                                                "Didn't find any option and therefore "
                                                        + "can't convert the configuration instance to a configuration")))));
    }

    private <T> ComponentFamilyMeta.BaseMeta<? extends Lifecycle> findMeta(final Class<T> componentType) {
        return asManager()
                .find(c -> c.get(ContainerComponentRegistry.class).getComponents().values().stream())
                .flatMap(f -> Stream
                        .concat(f.getProcessors().values().stream(), f.getPartitionMappers().values().stream()))
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

    @Override
    public <T> T findService(final String plugin, final Class<T> serviceClass) {
        return serviceClass
                .cast(asManager()
                        .findPlugin(plugin)
                        .orElseThrow(() -> new IllegalArgumentException("cant find plugin '" + plugin + "'"))
                        .get(ComponentManager.AllServices.class)
                        .getServices()
                        .get(serviceClass));
    }

    @Override
    public <T> T findService(final Class<T> serviceClass) {
        return findService(getSinglePlugin(), serviceClass);
    }

    public Set<String> getTestPlugins() {
        return new HashSet<>(EmbeddedComponentManager.class.cast(asManager()).testPlugins);
    }

    @Override
    public <T> void setInputData(final Iterable<T> data) {
        final State state = STATE.get();
        if (state == null) {
            initState.get().emitter = data.iterator();
        } else {
            state.emitter = data.iterator();
        }
    }

    @Override
    public <T> List<T> getCollectedData(final Class<T> recordType) {
        final State state = STATE.get();
        return state.collector
                .stream()
                .filter(r -> recordType.isInstance(r) || JsonObject.class.isInstance(r) || Record.class.isInstance(r))
                .map(r -> mapRecord(state, recordType, r))
                .collect(toList());
    }

    public void resetState() {
        final State state = STATE.get();
        if (state == null) {
            STATE.remove();
        } else {
            state.collector.clear();
            state.emitter = emptyIterator();
        }
    }

    private String getSinglePlugin() {
        return Optional
                .of(EmbeddedComponentManager.class.cast(asManager()).testPlugins/* sorted */)
                .filter(c -> !c.isEmpty())
                .map(c -> c.iterator().next())
                .orElseThrow(() -> new IllegalStateException("No component plugin found"));
    }

    private <T> T mapRecord(final State state, final Class<T> recordType, final Object r) {
        if (recordType.isInstance(r)) {
            return recordType.cast(r);
        }
        if (Record.class == recordType) {
            return recordType
                    .cast(new RecordConverters()
                            .toRecord(state.registry, r, state::jsonb, state::recordBuilderFactory));
        }
        return recordType
                .cast(new RecordConverters()
                        .toType(state.registry, r, recordType, state::jsonBuilderFactory, state::jsonProvider,
                                state::jsonb, state::recordBuilderFactory));
    }

    static class PreState {

        Iterator<?> emitter;
    }

    @AllArgsConstructor
    protected static class State {

        final ComponentManager manager;

        final Collection<Object> collector;

        final RecordConverters.MappingMetaRegistry registry = new RecordConverters.MappingMetaRegistry();

        Iterator<?> emitter;

        volatile Jsonb jsonb;

        volatile JsonProvider jsonProvider;

        volatile JsonBuilderFactory jsonBuilderFactory;

        volatile RecordBuilderFactory recordBuilderFactory;

        synchronized Jsonb jsonb() {
            if (jsonb == null) {
                jsonb = manager
                        .getJsonbProvider()
                        .create()
                        .withProvider(new PreComputedJsonpProvider("test", manager.getJsonpProvider(),
                                manager.getJsonpParserFactory(), manager.getJsonpWriterFactory(),
                                manager.getJsonpBuilderFactory(), manager.getJsonpGeneratorFactory(),
                                manager.getJsonpReaderFactory())) // reuses the same memory buffers
                        .withConfig(new JsonbConfig().setProperty("johnzon.cdi.activated", false))
                        .build();
            }
            return jsonb;
        }

        synchronized JsonProvider jsonProvider() {
            if (jsonProvider == null) {
                jsonProvider = manager.getJsonpProvider();
            }
            return jsonProvider;
        }

        synchronized JsonBuilderFactory jsonBuilderFactory() {
            if (jsonBuilderFactory == null) {
                jsonBuilderFactory = manager.getJsonpBuilderFactory();
            }
            return jsonBuilderFactory;
        }

        synchronized RecordBuilderFactory recordBuilderFactory() {
            if (recordBuilderFactory == null) {
                recordBuilderFactory = manager.getRecordBuilderFactoryProvider().apply("test");
            }
            return recordBuilderFactory;
        }
    }

    public static class EmbeddedComponentManager extends ComponentManager {

        private final ComponentManager oldInstance;

        private final List<String> testPlugins;

        private EmbeddedComponentManager(final String componentPackage) {
            super(findM2(), "TALEND-INF/dependencies.txt", "org.talend.sdk.component:type=component,value=%s");
            testPlugins = addJarContaining(Thread.currentThread().getContextClassLoader(),
                    componentPackage.replace('.', '/'));
            container
                    .builder("component-runtime-junit.jar", jarLocation(SimpleCollector.class).getAbsolutePath())
                    .create();
            oldInstance = ComponentManager.contextualInstance().get();
            ComponentManager.contextualInstance().set(this);
        }

        @Override
        public void close() {
            try {
                super.close();
            } finally {
                ComponentManager.contextualInstance().compareAndSet(this, oldInstance);
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

    interface Local<T> {

        void set(T value);

        T get();

        void remove();

        class StaticImpl<T> implements Local<T> {

            private final AtomicReference<T> state = new AtomicReference<>();

            @Override
            public void set(final T value) {
                state.set(value);
            }

            @Override
            public T get() {
                return state.get();
            }

            @Override
            public void remove() {
                state.set(null);
            }
        }

        class ThreadLocalImpl<T> implements Local<T> {

            private final ThreadLocal<T> threadLocal = new ThreadLocal<>();

            @Override
            public void set(final T value) {
                threadLocal.set(value);
            }

            @Override
            public T get() {
                return threadLocal.get();
            }

            @Override
            public void remove() {
                threadLocal.remove();
            }
        }
    }
}
