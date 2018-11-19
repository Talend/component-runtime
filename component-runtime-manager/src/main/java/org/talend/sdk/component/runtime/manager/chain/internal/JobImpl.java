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
package org.talend.sdk.component.runtime.manager.chain.internal;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;

import org.talend.sdk.component.api.processor.OutputEmitter;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;
import org.talend.sdk.component.runtime.base.Lifecycle;
import org.talend.sdk.component.runtime.input.Input;
import org.talend.sdk.component.runtime.input.Mapper;
import org.talend.sdk.component.runtime.jsonb.MultipleFormatDateAdapter;
import org.talend.sdk.component.runtime.manager.ComponentManager;
import org.talend.sdk.component.runtime.manager.chain.AutoChunkProcessor;
import org.talend.sdk.component.runtime.manager.chain.ChainedMapper;
import org.talend.sdk.component.runtime.manager.chain.GroupKeyProvider;
import org.talend.sdk.component.runtime.manager.chain.Job;
import org.talend.sdk.component.runtime.output.InputFactory;
import org.talend.sdk.component.runtime.output.OutputFactory;
import org.talend.sdk.component.runtime.output.Processor;
import org.talend.sdk.component.runtime.output.ProcessorImpl;
import org.talend.sdk.component.runtime.record.RecordBuilderFactoryImpl;
import org.talend.sdk.component.runtime.record.RecordConverters;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

public class JobImpl implements Job {

    public static class NodeBuilderImpl implements NodeBuilder {

        private final List<Component> nodes = new ArrayList<>();

        private final Map<String, Map<String, Object>> properties = new HashMap<>();

        @Override
        public NodeBuilder property(final String name, final Object value) {
            final Component lastComponent = nodes.get(nodes.size() - 1);
            properties.computeIfAbsent(lastComponent.getId(), s -> new HashMap<>());
            properties.get(lastComponent.getId()).put(name, value);
            return this;
        }

        @Override
        public NodeBuilder component(final String id, final String uri) {
            nodes.add(new Component(id, DSLParser.parse(uri)));
            return this;
        }

        @Override
        public LinkBuilder connections() {
            return new LinkBuilder(nodes, properties);
        }

    }

    @Slf4j
    @RequiredArgsConstructor
    public static class LinkBuilder implements Job.FromBuilder, Builder {

        private final List<Component> nodes;

        private final Map<String, Map<String, Object>> properties;

        private final List<Edge> edges = new ArrayList<>();

        private final Map<Integer, Set<Component>> levels = new TreeMap<>();

        @Override
        public ToBuilder from(final String id, final String branch) {
            final Component from = nodes
                    .stream()
                    .filter(node -> node.getId().equals(id))
                    .findFirst()
                    .orElseThrow(
                            () -> new IllegalStateException("No component with id '" + id + "' in created components"));

            edges
                    .stream()
                    .filter(edge -> edge.getFrom().getNode().getId().equals(id)
                            && edge.getFrom().getBranch().equals(branch))
                    .findFirst()
                    .ifPresent(edge -> {
                        throw new IllegalStateException(
                                "(" + id + "," + branch + ") node is already connected : " + edge);
                    });

            return new To(nodes, edges, new Connection(from, branch), this);
        }

        public void doBuild() {
            final List<Component> orphans = nodes
                    .stream()
                    .filter(n -> edges
                            .stream()
                            .noneMatch(l -> l.getFrom().getNode().equals(n) || l.getTo().getNode().equals(n)))
                    .collect(toList());
            orphans.forEach(o -> log.warn("component '" + o + "' is orphan in this graph. it will be ignored."));
            nodes.removeAll(orphans);

            // set up sources
            nodes
                    .stream()
                    .filter(node -> edges.stream().noneMatch(l -> l.getTo().getNode().equals(node)))
                    .forEach(component -> component.setSource(true));
            calculateGraphOrder(0, new HashSet<>(nodes), new ArrayList<>(edges), levels);
        }

        private void calculateGraphOrder(final int order, final Set<Component> nodes, final List<Edge> edges,
                final Map<Integer, Set<Component>> orderedGraph) {
            if (edges.isEmpty()) {
                orderedGraph.put(order, nodes); // last nodes
                return;
            }
            final Set<Component> startingNodes = nodes
                    .stream()
                    .filter(node -> edges.stream().noneMatch(l -> l.getTo().getNode().equals(node)))
                    .collect(toSet());
            if (order == 0 && startingNodes.isEmpty()) {
                throw new IllegalStateException("There is no starting component in this graph.");
            }
            final List<Edge> level = edges
                    .stream()
                    .filter(edge -> startingNodes.contains(edge.getFrom().getNode()))
                    .filter(edge -> edges
                            .stream()
                            .filter(others -> edge.getTo().getNode().equals(others.getTo().getNode()))
                            .map(others -> others.getFrom().getNode())
                            .allMatch(startingNodes::contains))
                    .collect(toList());
            if (level.isEmpty()) {
                throw new IllegalStateException("the job pipeline has cyclic connection");
            }
            final Set<Component> components = level.stream().map(edge -> edge.getFrom().getNode()).collect(toSet());
            orderedGraph.put(order, components);
            edges.removeAll(level);
            nodes.removeAll(components);
            calculateGraphOrder(order + 1, nodes, edges, orderedGraph);
        }

        @Override
        public JobExecutor build() {
            doBuild();
            return new JobExecutor(levels, edges, properties);
        }
    }

    @RequiredArgsConstructor
    private static class To implements ToBuilder {

        private final List<Component> nodes;

        private final List<Edge> edges;

        private final Connection from;

        private final Builder builder;

        @Override
        public Builder to(final String id, final String branch) {
            final Component to = nodes
                    .stream()
                    .filter(node -> node.getId().equals(id))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No component with id '" + id + "' in created nodes"));

            edges
                    .stream()
                    .filter(edge -> edge.getTo().getNode().getId().equals(id)
                            && edge.getTo().getBranch().equals(branch))
                    .findFirst()
                    .ifPresent(edge -> {
                        throw new IllegalStateException(
                                "(" + id + "," + branch + ") node is already connected : " + edge);
                    });
            edges.add(new Edge(from, new Connection(to, branch)));
            return builder;
        }
    }

    @Getter
    @Slf4j
    @RequiredArgsConstructor
    public static class JobExecutor implements Job.ExecutorBuilder {

        private final Map<Integer, Set<Component>> levels;

        private final List<Edge> edges;

        private final Map<String, Map<String, Object>> componentProperties;

        private final Map<String, Object> jobProperties = new HashMap<>();

        private final ComponentManager manager = ComponentManager.instance();

        @Override
        public ExecutorBuilder property(final String name, final Object value) {
            jobProperties.put(name, value);
            return this;
        }

        @Override
        public void run() {
            ExecutorBuilder runner = this;
            final Object o = jobProperties.get(ExecutorBuilder.class.getName());
            if (ExecutorBuilder.class.isInstance(o)) {
                runner = ExecutorBuilder.class.cast(o);
            } else if (Class.class.isInstance(o)) {
                runner = newRunner(Class.class.cast(o));
            } else if (String.class.isInstance(o)) {
                final String name = String.class.cast(o).trim();
                if (!"standalone".equalsIgnoreCase(name) && !"default".equalsIgnoreCase(name)
                        && !"local".equalsIgnoreCase(name)) {
                    if ("beam".equalsIgnoreCase(name)) {
                        try {
                            runner = newRunner(Thread.currentThread().getContextClassLoader(),
                                    "org.talend.sdk.component.runtime.beam.chain.impl.BeamExecutor");
                        } catch (final RuntimeException re) {
                            log
                                    .error("Can't instantiate beam job integration, "
                                            + "did you add org.talend.sdk.component:component-runtime-beam in your dependencies",
                                            re);
                        }
                    } else {
                        runner = newRunner(Thread.currentThread().getContextClassLoader(), name);
                    }
                }
            } else if (o != null) {
                throw new IllegalArgumentException(o + " is not an ExecutionBuilder");
            } else {
                final ClassLoader loader = Thread.currentThread().getContextClassLoader();
                try (final InputStream stream =
                        loader.getResourceAsStream("META-INF/services/" + ExecutorBuilder.class.getName())) {
                    if (stream != null) {
                        runner = new BufferedReader(new InputStreamReader(stream))
                                .lines()
                                .map(String::trim)
                                .filter(s -> !s.startsWith("#") && !s.isEmpty())
                                .findFirst()
                                .map(clazz -> newRunner(loader, clazz))
                                .orElse(this);
                    }
                } catch (final IOException e) {
                    log.debug(e.getMessage(), e);
                }
            }

            if (runner == this) {
                JobExecutor.class.cast(runner).localRun();
            } else {
                runner.run();
            }
        }

        private ExecutorBuilder newRunner(final ClassLoader loader, final String clazz) {
            try {
                final Class<? extends ExecutorBuilder> aClass =
                        (Class<? extends ExecutorBuilder>) loader.loadClass(clazz);
                return newRunner(aClass);
            } catch (final ClassNotFoundException e) {
                throw new IllegalArgumentException(e);
            }
        }

        private ExecutorBuilder newRunner(final Class<? extends ExecutorBuilder> runnerType) {
            try {
                try {
                    return runnerType.getConstructor(JobExecutor.class).newInstance(JobExecutor.this);
                } catch (final NoSuchMethodException e) {
                    return runnerType.getConstructor().newInstance();
                }
            } catch (final NoSuchMethodException | InstantiationException | IllegalAccessException e1) {
                throw new IllegalArgumentException(e1);
            } catch (InvocationTargetException e1) {
                throw new IllegalArgumentException(e1.getTargetException());
            }
        }

        private void localRun() {
            final Map<String, InputRunner> inputs =
                    levels.values().stream().flatMap(Collection::stream).filter(Component::isSource).map(n -> {
                        final Mapper mapper = manager
                                .findMapper(n.getNode().getFamily(), n.getNode().getComponent(),
                                        n.getNode().getVersion(), n.getNode().getConfiguration())
                                .orElseThrow(() -> new IllegalStateException("No mapper found for: " + n.getNode()));
                        return new AbstractMap.SimpleEntry<>(n.getId(), new InputRunner(mapper));
                    }).collect(toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));

            final Map<String, AutoChunkProcessor> processors = levels
                    .values()
                    .stream()
                    .flatMap(Collection::stream)
                    .filter(component -> !component.isSource())
                    .map(component -> {
                        final Processor processor = manager
                                .findProcessor(component.getNode().getFamily(), component.getNode().getComponent(),
                                        component.getNode().getVersion(), component.getNode().getConfiguration())
                                .orElseThrow(() -> new IllegalStateException(
                                        "No processor found for:" + component.getNode()));
                        final AtomicInteger maxBatchSize = new AtomicInteger(1);
                        if (ProcessorImpl.class.isInstance(processor)) {
                            ProcessorImpl.class
                                    .cast(processor)
                                    .getInternalConfiguration()
                                    .entrySet()
                                    .stream()
                                    .filter(it -> it.getKey().endsWith("$maxBatchSize") && it.getValue() != null
                                            && !it.getValue().trim().isEmpty())
                                    .findFirst()
                                    .ifPresent(val -> {
                                        try {
                                            maxBatchSize.set(Integer.parseInt(val.getValue().trim()));
                                        } catch (final NumberFormatException nfe) {
                                            throw new IllegalArgumentException("Invalid configuratoin: " + val);
                                        }
                                    });
                        }
                        return new AbstractMap.SimpleEntry<>(component.getId(),
                                new AutoChunkProcessor(maxBatchSize.get(), processor));
                    })
                    .collect(toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));

            try {
                final Map<String, AtomicBoolean> sourcesWithData = levels
                        .values()
                        .stream()
                        .flatMap(Collection::stream)
                        .filter(Component::isSource)
                        .map(component -> new AbstractMap.SimpleEntry<>(component.getId(), new AtomicBoolean(true)))
                        .collect(toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));
                processors.values().forEach(Lifecycle::start); // start processor
                final Map<String, Map<String, Map<String, Collection<Record>>>> flowData = new HashMap<>();
                final AtomicBoolean running = new AtomicBoolean(true);
                do {
                    levels.forEach((level, components) -> components.forEach((Component component) -> {
                        if (component.isSource()) {
                            final InputRunner source = inputs.get(component.getId());
                            final Record data = source.next();
                            if (data == null) {
                                sourcesWithData.get(component.getId()).set(false);
                                return;
                            }
                            final String key = getKeyProvider(component.getId())
                                    .apply(new GroupContextImpl(data, component.getId(), "__default__"));
                            flowData.computeIfAbsent(component.getId(), s -> new HashMap<>());
                            flowData.get(component.getId()).computeIfAbsent("__default__", s -> new TreeMap<>());
                            flowData
                                    .get(component.getId())
                                    .get("__default__")
                                    .computeIfAbsent(key, k -> new ArrayList<>())
                                    .add(data);
                        } else {
                            final List<Edge> connections =
                                    getConnections(getEdges(), component, e -> e.getTo().getNode());
                            final DataInputFactory dataInputFactory = new DataInputFactory();
                            if (connections.size() == 1) {
                                final Edge edge = connections.get(0);
                                final String fromId = edge.getFrom().getNode().getId();
                                final String fromBranch = edge.getFrom().getBranch();
                                final String toBranch = edge.getTo().getBranch();

                                final Map<String, Map<String, Collection<Record>>> idData = flowData.get(fromId);
                                final Record data = idData == null ? null : pollFirst(idData.get(fromBranch));
                                if (data != null) {
                                    dataInputFactory.withInput(toBranch, singletonList(data));
                                }
                            } else { // need grouping
                                final Map<String, Map<String, Collection<Record>>> availableDataForStep =
                                        new HashMap<>();
                                connections.forEach(edge -> {
                                    final String fromId = edge.getFrom().getNode().getId();
                                    final String fromBranch = edge.getFrom().getBranch();
                                    final String toBranch = edge.getTo().getBranch();
                                    final Map<String, Collection<Record>> data =
                                            flowData.get(fromId) == null ? null : flowData.get(fromId).get(fromBranch);
                                    if (data != null && !data.isEmpty()) {
                                        availableDataForStep.put(toBranch, data);
                                    }
                                });

                                final Map<String, String> joined = joinWithFusionSort(availableDataForStep);
                                if (!joined.isEmpty() && connections.size() == joined.size()) {
                                    joined.forEach((k, v) -> {
                                        final Collection data = availableDataForStep.get(k).remove(v);
                                        dataInputFactory.withInput(k, data);
                                    });
                                }
                            }
                            if (dataInputFactory.inputs.isEmpty()) {
                                if (level.equals(levels.size() - 1)
                                        && sourcesWithData.entrySet().stream().noneMatch(e -> e.getValue().get())) {
                                    running.set(false);
                                }
                                return;
                            }
                            final AutoChunkProcessor processor = processors.get(component.getId());

                            final DataOutputFactory dataOutputFactory = new DataOutputFactory(getManager()
                                    .findPlugin(processor.plugin())
                                    .get()
                                    .get(ComponentManager.AllServices.class)
                                    .getServices());
                            processor.onElement(dataInputFactory, dataOutputFactory);
                            dataOutputFactory.getOutputs().forEach((branch, data) -> data.forEach(item -> {
                                final String key = getKeyProvider(component.getId())
                                        .apply(new GroupContextImpl(item, component.getId(), branch));
                                flowData.computeIfAbsent(component.getId(), s -> new HashMap<>());
                                flowData.get(component.getId()).computeIfAbsent(branch, s -> new TreeMap<>());
                                flowData
                                        .get(component.getId())
                                        .get(branch)
                                        .computeIfAbsent(key, k -> new ArrayList<>())
                                        .add(item);
                            }));
                        }
                    }));
                } while (running.get());
            } finally {
                processors.values().forEach(Lifecycle::stop);
                inputs.values().forEach(InputRunner::stop);
                levels
                        .values()
                        .stream()
                        .flatMap(Collection::stream)
                        .map(Component::getId)
                        .forEach(LocalSequenceHolder::clean);
            }
        }

        private Map<String, String>
                joinWithFusionSort(final Map<String, Map<String, Collection<Record>>> dataByBranch) {
            final Map<String, String> join = new HashMap<>();
            dataByBranch.forEach((branch1, records1) -> {
                dataByBranch.forEach((branch2, records2) -> {
                    if (!branch1.equals(branch2)) {
                        for (final String key1 : records1.keySet()) {
                            for (final String key2 : records2.keySet()) {
                                if (key1.equals(key2)) {
                                    join.putIfAbsent(branch1, key1);
                                    join.putIfAbsent(branch2, key2);
                                } else if (key1.compareTo(key2) < 0) {
                                    break;// see fusion sort
                                }
                            }
                        }
                    }
                });
            });
            return join;
        }

        private Record pollFirst(final Map<String, Collection<Record>> data) {
            if (data == null || data.isEmpty()) {
                return null;
            }
            while (!data.isEmpty()) {
                final String key = data.keySet().iterator().next();
                final Collection<Record> items = data.get(key);
                if (!items.isEmpty()) {
                    final Iterator<Record> iterator = items.iterator();
                    final Record item = iterator.next();
                    iterator.remove();
                    return item;
                } else {
                    data.remove(key);
                }
            }
            return null;
        }

        private List<Job.Edge> getConnections(final List<Job.Edge> edges, final Job.Component step,
                final Function<Edge, Component> direction) {
            return edges.stream().filter(edge -> direction.apply(edge).equals(step)).collect(toList());
        }

        public GroupKeyProvider getKeyProvider(final String componentId) {
            if (componentProperties.get(componentId) != null) {
                final Object o = componentProperties.get(componentId).get(GroupKeyProvider.class.getName());
                if (GroupKeyProvider.class.isInstance(o)) {
                    return new GroupKeyProviderImpl(GroupKeyProvider.class.cast(o));
                }
            }

            final Object o = jobProperties.get(GroupKeyProvider.class.getName());
            if (GroupKeyProvider.class.isInstance(o)) {
                return new GroupKeyProviderImpl(GroupKeyProvider.class.cast(o));
            }

            final ServiceLoader<GroupKeyProvider> services = ServiceLoader.load(GroupKeyProvider.class);
            if (services.iterator().hasNext()) {
                return services.iterator().next();
            }

            return LocalSequenceHolder.cleanAndGet(componentId);
        }
    }

    @Data
    private static class GroupContextImpl implements GroupKeyProvider.GroupContext {

        private final Record data;

        private final String componentId;

        private final String branchName;
    }

    public static class LocalSequenceHolder {

        private static final Map<String, AtomicLong> GENERATORS = new HashMap<>();

        public static GroupKeyProvider cleanAndGet(final String name) {
            GENERATORS.put(name, new AtomicLong(0));
            return c -> Long.toString(GENERATORS.get(name).incrementAndGet());
        }

        public static void clean(final String name) {
            GENERATORS.remove(name);
        }
    }

    @Slf4j
    private static class InputRunner {

        private final Mapper chainedMapper;

        private final Input input;

        private InputRunner(final Mapper mapper) {
            RuntimeException error = null;
            try {
                mapper.start();
                chainedMapper = new ChainedMapper(mapper, mapper.split(mapper.assess()).iterator());
                chainedMapper.start();
                input = chainedMapper.create();
                input.start();
            } catch (final RuntimeException re) {
                error = re;
                throw re;
            } finally {
                try {
                    mapper.stop();
                } catch (final RuntimeException re) {
                    if (error == null) {
                        throw re;
                    }
                    log.error(re.getMessage(), re);
                }
            }
        }

        public Record next() {
            final Object next = input.next();
            if (next == null) {
                return null;
            }
            return Record.class.cast(next);
        }

        public void stop() {
            RuntimeException error = null;
            try {
                if (input != null) {
                    input.stop();
                }
            } catch (final RuntimeException re) {
                error = re;
                throw re;
            } finally {
                try {
                    if (chainedMapper != null) {
                        chainedMapper.stop();
                    }
                } catch (final RuntimeException re) {
                    if (error == null) {
                        throw re;
                    }
                    log.error(re.getMessage(), re);
                }
            }
        }
    }

    @Data
    private static class DataOutputFactory implements OutputFactory {

        private final Map<Class<?>, Object> services;

        private final Map<String, Collection<Record>> outputs = new HashMap<>();

        @Override
        public OutputEmitter create(final String name) {
            return new OutputEmitterImpl(name);
        }

        @AllArgsConstructor
        private class OutputEmitterImpl implements OutputEmitter {

            private final String name;

            @Override
            public void emit(final Object value) {
                outputs
                        .computeIfAbsent(name, k -> new ArrayList<>())
                        .add(new RecordConverters()
                                .toRecord(value, () -> Jsonb.class.cast(services.get(Jsonb.class)),
                                        () -> RecordBuilderFactory.class
                                                .cast(services.get(RecordBuilderFactory.class))));
            }
        }
    }

    private static class DataInputFactory implements InputFactory {

        private final Map<String, Iterator<Object>> inputs = new HashMap<>();

        private volatile Jsonb jsonb;

        private volatile RecordBuilderFactory factory;

        private DataInputFactory withInput(final String branch, final Collection<Object> branchData) {
            inputs.put(branch, branchData.iterator());
            return this;
        }

        @Override
        public Object read(final String name) {
            final Iterator<?> iterator = inputs.get(name);
            if (iterator != null && iterator.hasNext()) {
                return map(iterator.next());
            }
            return null;
        }

        private Object map(final Object next) {
            if (next == null || Record.class.isInstance(next)) {
                return next;
            }

            final String str = jsonb.toJson(next);
            // primitives mainly, not that accurate in main code but for now not forbidden
            if (str.equals(next.toString())) {
                return next;
            }
            // pojo
            return new RecordConverters().toRecord(next, () -> {
                if (jsonb == null) {
                    synchronized (this) {
                        if (jsonb == null) {
                            jsonb = JsonbBuilder
                                    .create(new JsonbConfig()
                                            .withAdapters(new MultipleFormatDateAdapter())
                                            .setProperty("johnzon.cdi.activated", false));
                        }
                    }
                }
                return jsonb;
            }, () -> {
                if (factory == null) {
                    synchronized (this) {
                        if (factory == null) {
                            factory = new RecordBuilderFactoryImpl("test");
                        }
                    }
                }
                return factory;
            });
        }
    }

    @AllArgsConstructor
    protected static class GroupKeyProviderImpl implements GroupKeyProvider {

        private final GroupKeyProvider delegate;

        @Override
        public String apply(final GroupKeyProvider.GroupContext context) {
            return delegate.apply(context);
        }
    }
}
