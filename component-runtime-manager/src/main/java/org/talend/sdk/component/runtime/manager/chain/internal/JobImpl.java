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

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

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
    }

    @Slf4j
    @RequiredArgsConstructor
    private static class LinkBuilder implements Job.FromBuilder, Builder {

        private final List<Component> nodes;

        private final List<Edge> edges = new ArrayList<>();

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

        @Override
        public ExecutorBuilder build() {
            // remove orphan nodes & log them
            final List<Component> orphans = nodes
                    .stream()
                    .filter(n -> edges.stream().noneMatch(
                            l -> l.getFrom().getNode().equals(n) || l.getTo().getNode().equals(n)))
                    .collect(toList());
            orphans.forEach(o -> log.warn("component '" + o + "' is orphan in this graph. it will be ignored."));
            nodes.removeAll(orphans);

            // find startNodes - nodes that defined with a from link and no to
            final List<Component> startNodes = nodes
                    .stream()
                    .filter(node -> edges.stream().noneMatch(l -> l.getTo().getNode().equals(node)))
                    .collect(toList());
            if (startNodes.isEmpty()) {
                throw new IllegalStateException("There is no starting component in this graph.");
            }

            nodes.removeAll(startNodes);
            hasCyclicConnection(startNodes, edges, new HashSet<>(startNodes));
            return new ExecutorBuilder(startNodes, nodes, edges);
        }

        private boolean hasCyclicConnection(final Collection<Component> startNodes, final List<Edge> edges,
                final Set<Component> parsedNodes) {

            Set<Component> next = new HashSet<>();
            startNodes.forEach(startNode -> {
                edges
                        .stream()
                        .filter(e -> e.getFrom().getNode().getId().equals(startNode.getId()))
                        .map(Edge::getTo)
                        .forEach(to -> {
                            final Edge cyclic = edges
                                    .stream()
                                    .filter(e -> e.getFrom().getNode().getId().equals(to.getNode().getId()))
                                    .filter(e -> parsedNodes.contains(e.getTo().getNode()))
                                    .findFirst()
                                    .orElseGet(() -> {
                                        next.add(to.getNode());
                                        return null;
                                    });

                            if (cyclic != null) {
                                throw new IllegalStateException("The job have cyclic connection: " + cyclic);
                            }
                        });

            });

            parsedNodes.addAll(startNodes);
            return !next.isEmpty() && hasCyclicConnection(next, edges, parsedNodes);
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
    @RequiredArgsConstructor
    private static class ExecutorBuilder implements Job.ExecutorBuilder {

        private final List<Component> startNodes;

        private final List<Component> nodes;

        private final List<Edge> edges;

        private final ComponentManager manager = ComponentManager.instance();

        @Override
        public void run() {
            final Map<String, InputRunner> inputs = startNodes
                    .stream()
                    .map(n -> new AbstractMap.SimpleEntry<>(n.getId(), new InputRunner(manager
                            .findMapper(n.getNode().getFamily(), n.getNode().getComponent(), n.getNode().getVersion(),
                                    n.getNode().getConfiguration())
                            .orElseThrow(() -> new IllegalStateException("No mapper found for: " + n.getNode())))))
                    .collect(toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));

            final Map<String, Processor> processors = nodes
                    .stream()
                    .map(n -> new AbstractMap.SimpleEntry<>(n.getId(), manager
                            .findProcessor(n.getNode().getFamily(), n.getNode().getComponent(),
                                    n.getNode().getVersion(), n.getNode().getConfiguration())
                            .orElseThrow(() -> new IllegalStateException("No processor found for:" + n.getNode()))))
                    .collect(toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));

            try {
                processors.values().forEach(Lifecycle::start); // start processor
                do {
                    // prepare inputs data (inputs support only one output branch for now)
                    final Map<String, Map<String, Object>> inputsData = inputs
                            .entrySet()
                            .stream()
                            .map(in -> new AbstractMap.SimpleEntry<>(in.getKey(), new HashMap<String, Object>() {

                                {
                                    put("__default__", in.getValue().next());
                                }
                            }))
                            .collect(toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));

                    if (endOfFlow(inputsData)) {
                        break; // if no more inputs data stop reading
                    }

                    doRun(getNextComponents(inputs.keySet(), edges), inputsData, edges, processors);
                } while (true);

            } finally {
                processors.values().forEach(Lifecycle::stop);
                inputs.values().forEach(InputRunner::stop);
            }

        }

        private void doRun(final Set<Component> step, final Map<String, Map<String, Object>> inputsData,
                final List<Edge> edges, final Map<String, Processor> processors) {

            if (step.isEmpty() || inputsData.isEmpty()
                    || inputsData.values().stream().flatMap(in -> in.values().stream()).allMatch(Objects::isNull)) {

                return;
            }

            final Map<String, Map<String, Object>> nextInputData = new HashMap<>();
            step.forEach(component -> {

                final List<Edge> connections =
                        edges.stream().filter(edge -> edge.getTo().getNode().getId().equals(component.getId())).collect(
                                toList());

                final DataInputFactory inputFactory = new DataInputFactory();
                final Boolean[] hasData = { false };
                connections.forEach(connection -> {
                    final String fromId = connection.getFrom().getNode().getId();
                    final String fromBranch = connection.getFrom().getBranch();

                    final String toBranch = connection.getTo().getBranch();

                    final Object data = inputsData.get(fromId).get(fromBranch);
                    inputsData.get(fromId).remove(fromBranch);
                    if (data != null) {
                        inputFactory.withInput(toBranch, singletonList(data));
                        hasData[0] = true;
                    }

                });

                if (!hasData[0]) {
                    return;
                }

                final Processor processor = processors.get(component.getId());
                DataOutputFactory outputFactory = new DataOutputFactory();
                processor.beforeGroup();
                processor.onNext(inputFactory, outputFactory);
                processor.afterGroup(outputFactory);

                if (outputFactory.getOutputs().isEmpty()
                        || outputFactory.getOutputs().values().stream().allMatch(Objects::isNull)) {
                    return;
                }
                nextInputData.put(component.getId(), outputFactory.getOutputs());
            });

            inputsData
                    .entrySet()
                    .stream()
                    .filter(e -> e.getValue().isEmpty())
                    .map(Map.Entry::getKey)
                    .collect(toList())
                    .forEach(inputsData::remove);
            nextInputData.putAll(inputsData);// merge non used data with processor outputs
            final List<String> inputs = step.stream().map(Component::getId).collect(toList());
            inputs.addAll(inputsData.keySet());

            doRun(getNextComponents(inputs, edges), nextInputData, edges, processors);
        }

        private Set<Component> getNextComponents(final Collection<String> inputsIds, final List<Edge> edges) {
            return edges
                    .stream()
                    .filter(edge -> inputsIds.contains(edge.getFrom().getNode().getId()))
                    .map(Edge::getTo)
                    .filter(to -> edges
                            .stream()
                            .filter(edge -> to.getNode().getId().equals(edge.getTo().getNode().getId()))
                            .allMatch(edge -> inputsIds.contains(edge.getFrom().getNode().getId())))
                    .map(Connection::getNode)
                    .collect(toSet());
        }

        private boolean endOfFlow(final Map<String, Map<String, Object>> flowValues) {
            return flowValues.isEmpty() || flowValues.values().isEmpty()
                    || flowValues.values().stream().flatMap(e -> e.values().stream()).allMatch(Objects::isNull);
        }
    }

    public static class InputRunner {

        private final Mapper chainedMapper;

        private final Input input;

        public InputRunner(final Mapper mapper) {
            mapper.start();
            final long totalSize = mapper.assess();
            final Iterator<Mapper> iterator;
            try {
                iterator = mapper.split(totalSize).iterator();
            } finally {
                mapper.stop();
            }

            chainedMapper = new ChainedMapper(mapper, iterator);
            chainedMapper.start();
            input = chainedMapper.create();
            input.start();
        }

        public Object next() {
            return input.next();
        }

        public void stop() {
            chainedMapper.stop();
            input.stop();
        }
    }

    @Data
    private static class Component {

        private final String id;

        private final DSLParser.Step node;

    }

    @Data
    private static class Connection {

        private final Component node;

        private final String branch;
    }

    @Data
    private static class Edge {

        private final Connection from;

        private final Connection to;
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
