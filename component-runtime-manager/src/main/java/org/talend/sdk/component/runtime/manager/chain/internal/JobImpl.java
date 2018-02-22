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
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.json.JsonObject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;

import org.talend.sdk.component.api.processor.OutputEmitter;
import org.talend.sdk.component.runtime.base.Lifecycle;
import org.talend.sdk.component.runtime.input.Input;
import org.talend.sdk.component.runtime.input.Mapper;
import org.talend.sdk.component.runtime.manager.ComponentManager;
import org.talend.sdk.component.runtime.manager.chain.ChainedMapper;
import org.talend.sdk.component.runtime.manager.chain.Job;
import org.talend.sdk.component.runtime.output.InputFactory;
import org.talend.sdk.component.runtime.output.OutputFactory;
import org.talend.sdk.component.runtime.output.Processor;

import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

public class JobImpl implements Job {

    public static class NodeBuilderImpl implements NodeBuilder {

        private List<Component> nodes = new ArrayList<>();

        @Override
        public NodeBuilder component(final String id, final String uri) {
            nodes.add(new Component(id, DSLParser.parse(uri)));
            return this;
        }

        @Override
        public LinkBuilder connections() {
            return new LinkBuilder(nodes);
        }

        public List<Component> getNodes() {
            return nodes;
        }
    }

    @Slf4j
    @Getter
    @RequiredArgsConstructor
    public static class LinkBuilder implements Job.FromBuilder, Builder {

        private final List<Component> nodes;

        private final List<Edge> edges = new ArrayList<>();

        private final Map<Integer, Set<Component>> levels = new TreeMap<>();

        @Override
        public ToBuilder from(final String id, final String branch) {
            final Component from = nodes.stream().filter(node -> node.getId().equals(id)).findFirst().orElseThrow(
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
                    .filter(n -> edges.stream().noneMatch(
                            l -> l.getFrom().getNode().equals(n) || l.getTo().getNode().equals(n)))
                    .collect(toList());
            orphans.forEach(o -> log.warn("component '" + o + "' is orphan in this graph. it will be ignored."));
            nodes.removeAll(orphans);

            // set up sources
            nodes.stream().filter(node -> edges.stream().noneMatch(l -> l.getTo().getNode().equals(node))).forEach(
                    component -> component.setSource(true));
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
            return new JobExecutor(levels, edges);
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
            final Component to = nodes.stream().filter(node -> node.getId().equals(id)).findFirst().orElseThrow(
                    () -> new IllegalStateException("No component with id '" + id + "' in created nodes"));

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

    @Slf4j
    @Getter
    @RequiredArgsConstructor
    public static class JobExecutor implements Job.ExecutorBuilder {

        private final Map<Integer, Set<Component>> levels;

        private final List<Edge> edges;

        protected final Map<String, Object> properties = new HashMap<>();

        private final ComponentManager manager = ComponentManager.instance();

        @Override
        public ExecutorBuilder property(final String name, final Object value) {
            properties.put(name, value);
            return this;
        }

        @Override
        public void run() {
            final Map<String, InputRunner> inputs = levels
                    .values()
                    .stream()
                    .flatMap(Collection::stream)
                    .filter(Component::isSource)
                    .map(n -> new AbstractMap.SimpleEntry<>(n.getId(), new InputRunner(manager
                            .findMapper(n.getNode().getFamily(), n.getNode().getComponent(), n.getNode().getVersion(),
                                    n.getNode().getConfiguration())
                            .orElseThrow(() -> new IllegalStateException("No mapper found for: " + n.getNode())))))
                    .collect(toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));

            final Map<String, Processor> processors = levels
                    .values()
                    .stream()
                    .flatMap(Collection::stream)
                    .filter(component -> !component.isSource())
                    .map(component -> new AbstractMap.SimpleEntry<>(component.getId(), manager
                            .findProcessor(component.getNode().getFamily(), component.getNode().getComponent(),
                                    component.getNode().getVersion(), component.getNode().getConfiguration())
                            .orElseThrow(
                                    () -> new IllegalStateException("No processor found for:" + component.getNode()))))
                    .collect(toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));

            try {
                processors.values().forEach(Lifecycle::start); // start processor
                do {
                    Map<String, Map<String, Object>> flowData = new HashMap<>();
                    levels.values().stream().flatMap(Collection::stream).forEach(component -> {
                        if (component.isSource()) {
                            final InputRunner source = inputs.get(component.getId());
                            final Object data = source.next();
                            if (data == null) {
                                return;
                            }
                            edges
                                    .stream()
                                    .filter(edge -> edge.getFrom().getNode().equals(component))
                                    .map(Edge::getTo)
                                    .forEach(connection -> {
                                        flowData.put(component.getId(), singletonMap("__default__", data));
                                    });
                        } else if (!flowData.isEmpty()) {
                            final DataInputFactory dataInputFactory = new DataInputFactory();
                            final Set<Edge> connections =
                                    edges.stream().filter(edge -> edge.getTo().getNode().equals(component)).collect(
                                            toSet());
                            connections.forEach(edge -> {
                                final String fromId = edge.getFrom().getNode().getId();
                                final String fromBranch = edge.getFrom().getBranch();
                                final String toBranch = edge.getTo().getBranch();
                                final Object data =
                                        flowData.get(fromId) == null ? null : flowData.get(fromId).get(fromBranch);
                                if (data != null) {
                                    dataInputFactory.withInput(toBranch, singletonList(data));
                                }
                            });
                            if (dataInputFactory.inputs.isEmpty()) {
                                return;
                            }
                            final Processor processor = processors.get(component.getId());
                            DataOutputFactory dataOutputFactory = new DataOutputFactory();
                            processor.beforeGroup();
                            processor.onNext(dataInputFactory, dataOutputFactory);
                            processor.afterGroup(dataOutputFactory);
                            connections.forEach(edge -> {
                                final String toId = edge.getTo().getNode().getId();
                                flowData.put(toId, dataOutputFactory.getOutputs());
                            });
                        }
                    });

                    if (flowData.isEmpty()) {
                        break;
                    }
                } while (true);

            } finally {
                processors.values().forEach(Lifecycle::stop);
                inputs.values().forEach(InputRunner::stop);
            }
        }

        @Slf4j
        public static class InputRunner {

            private final Mapper chainedMapper;

            private final Input input;

            public InputRunner(final Mapper mapper) {
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

            public Object next() {
                return input.next();
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

            private final Map<String, Object> outputs = new HashMap<>();

            @Override
            public OutputEmitter create(final String name) {
                return value -> outputs.put(name, value);
            }
        }

        private static class DataInputFactory implements InputFactory {

            private final Map<String, Iterator<Object>> inputs = new HashMap<>();

            private volatile Jsonb jsonb;

            public DataInputFactory withInput(final String branch, final Collection<Object> branchData) {
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
                if (next == null || JsonObject.class.isInstance(next)) {
                    return next;
                }
                if (jsonb == null) {
                    synchronized (this) {
                        if (jsonb == null) {
                            jsonb = JsonbBuilder.create(new JsonbConfig().setProperty("johnzon.cdi.activated", false));
                        }
                    }
                }
                final String str = jsonb.toJson(next);
                // primitives mainly, not that accurate in main code but for now not forbidden
                if (str.equals(next.toString())) {
                    return next;
                }
                // pojo
                return jsonb.fromJson(str, JsonObject.class);
            }
        }
    }

}
