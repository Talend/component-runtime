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
package org.talend.sdk.component.runtime.beam.chain.impl;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.PipelineResult;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.transforms.join.CoGroupByKey;
import org.apache.beam.sdk.transforms.join.KeyedPCollectionTuple;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.PDone;
import org.apache.beam.sdk.values.TupleTag;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.runtime.beam.TalendFn;
import org.talend.sdk.component.runtime.beam.TalendIO;
import org.talend.sdk.component.runtime.beam.coder.registry.SchemaRegistryCoder;
import org.talend.sdk.component.runtime.beam.transform.AutoKVWrapper;
import org.talend.sdk.component.runtime.beam.transform.CoGroupByKeyResultMappingTransform;
import org.talend.sdk.component.runtime.beam.transform.RecordBranchFilter;
import org.talend.sdk.component.runtime.beam.transform.RecordBranchMapper;
import org.talend.sdk.component.runtime.beam.transform.RecordBranchUnwrapper;
import org.talend.sdk.component.runtime.beam.transform.RecordKVUnwrapper;
import org.talend.sdk.component.runtime.beam.transform.RecordNormalizer;
import org.talend.sdk.component.runtime.input.Mapper;
import org.talend.sdk.component.runtime.manager.chain.Job;
import org.talend.sdk.component.runtime.manager.chain.internal.JobImpl;
import org.talend.sdk.component.runtime.output.Processor;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class BeamExecutor implements Job.ExecutorBuilder {

    private final JobImpl.JobExecutor delegate;

    @Override
    public Job.ExecutorBuilder property(final String name, final Object value) {
        delegate.property(name, value);
        return this;
    }

    @Override
    public void run() {
        try {
            final Map<String, Mapper> mappers = delegate
                    .getLevels()
                    .values()
                    .stream()
                    .flatMap(Collection::stream)
                    .filter(Job.Component::isSource)
                    .collect(toMap(Job.Component::getId, e -> delegate
                            .getManager()
                            .findMapper(e.getNode().getFamily(), e.getNode().getComponent(), e.getNode().getVersion(),
                                    e.getNode().getConfiguration())
                            .orElseThrow(() -> new IllegalStateException("No mapper found for: " + e.getNode()))));

            final Map<String, Processor> processors = delegate
                    .getLevels()
                    .values()
                    .stream()
                    .flatMap(Collection::stream)
                    .filter(component -> !component.isSource())
                    .collect(toMap(Job.Component::getId, e -> delegate
                            .getManager()
                            .findProcessor(e.getNode().getFamily(), e.getNode().getComponent(),
                                    e.getNode().getVersion(), e.getNode().getConfiguration())
                            .orElseThrow(() -> new IllegalStateException("No processor found for:" + e.getNode()))));

            final Pipeline pipeline = Pipeline.create(createPipelineOptions());
            final Map<String, PCollection<Record>> pCollections = new HashMap<>();
            delegate.getLevels().values().stream().flatMap(Collection::stream).forEach(component -> {
                if (component.isSource()) {
                    final Mapper mapper = mappers.get(component.getId());
                    pCollections
                            .put(component.getId(),
                                    pipeline
                                            .apply(toName("TalendIO", component), TalendIO.read(mapper))
                                            .apply(toName("RecordNormalizer", component),
                                                    RecordNormalizer.of(mapper.plugin())));
                } else {
                    final Processor processor = processors.get(component.getId());
                    final List<Job.Edge> joins = getEdges(delegate.getEdges(), component, e -> e.getTo().getNode());
                    final Map<String, PCollection<KV<String, Record>>> inputs =
                            joins.stream().collect(toMap(e -> e.getTo().getBranch(), e -> {
                                final PCollection<Record> pc = pCollections.get(e.getFrom().getNode().getId());
                                final PCollection<Record> filteredInput = pc
                                        .apply(toName("RecordBranchFilter", component, e),
                                                RecordBranchFilter.of(processor.plugin(), e.getFrom().getBranch()));
                                final PCollection<Record> mappedInput;
                                if (e.getFrom().getBranch().equals(e.getTo().getBranch())) {
                                    mappedInput = filteredInput;
                                } else {
                                    mappedInput = filteredInput
                                            .apply(toName("RecordBranchMapper", component, e),
                                                    RecordBranchMapper
                                                            .of(processor.plugin(), e.getFrom().getBranch(),
                                                                    e.getTo().getBranch()));
                                }
                                return mappedInput
                                        .apply(toName("RecordBranchUnwrapper", component, e),
                                                RecordBranchUnwrapper.of(processor.plugin(), e.getTo().getBranch()))
                                        .apply(toName("AutoKVWrapper", component, e), AutoKVWrapper
                                                .of(processor.plugin(), delegate.getKeyProvider(component.getId()),
                                                        component.getId(), e.getFrom().getBranch()));
                            }));
                    final PCollection<Record> preparedInput;
                    if (inputs.size() == 1) {
                        final Map.Entry<String, PCollection<KV<String, Record>>> input =
                                inputs.entrySet().iterator().next();
                        preparedInput = input
                                .getValue()
                                .apply(toName("RecordKVUnwrapper", component), ParDo.of(new RecordKVUnwrapper()))
                                .setCoder(SchemaRegistryCoder.of())
                                .apply(toName("RecordNormalizer", component), RecordNormalizer.of(processor.plugin()));
                    } else {
                        KeyedPCollectionTuple<String> join = null;
                        for (final Map.Entry<String, PCollection<KV<String, Record>>> entry : inputs.entrySet()) {
                            final TupleTag<Record> branch = new TupleTag<>(entry.getKey());
                            join = join == null ? KeyedPCollectionTuple.of(branch, entry.getValue())
                                    : join.and(branch, entry.getValue());
                        }
                        preparedInput = join
                                .apply(toName("CoGroupByKey", component), CoGroupByKey.create())
                                .apply(toName("CoGroupByKeyResultMappingTransform", component),
                                        new CoGroupByKeyResultMappingTransform<>(processor.plugin(), true));
                    }

                    if (getEdges(delegate.getEdges(), component, e -> e.getFrom().getNode()).isEmpty()) {
                        final PTransform<PCollection<Record>, PDone> write = TalendIO.write(processor);
                        preparedInput.apply(toName("Output", component), write);
                    } else {
                        final PTransform<PCollection<Record>, PCollection<Record>> process = TalendFn.asFn(processor);
                        pCollections
                                .put(component.getId(), preparedInput.apply(toName("Processor", component), process));
                    }
                }
            });
            final PipelineResult result = pipeline.run();
            result.waitUntilFinish();// the wait until finish don't wait for the job to complete on the direct runner
            while (PipelineResult.State.RUNNING.equals(result.getState())) {
                try {
                    Thread.sleep(100L);
                } catch (final InterruptedException e) {
                    throw new IllegalStateException("the job was aborted", e);
                }
            }

        } finally {
            delegate
                    .getLevels()
                    .values()
                    .stream()
                    .flatMap(Collection::stream)
                    .map(Job.Component::getId)
                    .forEach(JobImpl.LocalSequenceHolder::clean);
        }
    }

    private String toName(final String transform, final Job.Component component, final Job.Edge e) {
        return String
                .format(transform + "/step=%s,from=%s(%s)-to=%s(%s)", component.getId(), e.getFrom().getNode().getId(),
                        e.getFrom().getBranch(), e.getTo().getNode().getId(), e.getTo().getBranch());
    }

    private String toName(final String transform, final Job.Component component) {
        return String.format(transform + "/%s", component.getId());
    }

    private List<Job.Edge> getEdges(final List<Job.Edge> edges, final Job.Component step,
            final Function<Job.Edge, Job.Component> componentMapper) {
        return edges.stream().filter(edge -> componentMapper.apply(edge).equals(step)).collect(toList());
    }

    private PipelineOptions createPipelineOptions() {
        return PipelineOptionsFactory
                .fromArgs(System
                        .getProperties()
                        .stringPropertyNames()
                        .stream()
                        .filter(p -> p.startsWith("talend.beam.job."))
                        .map(k -> "--" + k.substring("talend.beam.job.".length()) + "=" + System.getProperty(k))
                        .toArray(String[]::new))
                .create();
    }

}
