/**
 * Copyright (C) 2006-2024 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.junit.beam;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.io.Serializable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.PipelineResult;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.testing.PAssert;
import org.apache.beam.sdk.transforms.Create;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.values.PBegin;
import org.apache.beam.sdk.values.PCollection;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.input.Assessor;
import org.talend.sdk.component.api.input.Emitter;
import org.talend.sdk.component.api.input.PartitionMapper;
import org.talend.sdk.component.api.input.PartitionSize;
import org.talend.sdk.component.api.input.Producer;
import org.talend.sdk.component.api.input.Split;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.service.Service;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;
import org.talend.sdk.component.api.service.source.ProducerFinder;
import org.talend.sdk.component.container.Container;
import org.talend.sdk.component.junit.BaseComponentsHandler;
import org.talend.sdk.component.junit.environment.Environment;
import org.talend.sdk.component.junit.environment.builtin.ContextualEnvironment;
import org.talend.sdk.component.junit.environment.builtin.beam.SparkRunnerEnvironment;
import org.talend.sdk.component.junit5.Injected;
import org.talend.sdk.component.junit5.WithComponents;
import org.talend.sdk.component.junit5.environment.EnvironmentalTest;
import org.talend.sdk.component.runtime.base.Delegated;
import org.talend.sdk.component.runtime.beam.TalendIO;
import org.talend.sdk.component.runtime.beam.coder.registry.SchemaRegistryCoder;
import org.talend.sdk.component.runtime.input.Mapper;
import org.talend.sdk.component.runtime.manager.ComponentManager;
import org.talend.sdk.component.runtime.manager.ComponentManager.AllServices;
import org.talend.sdk.component.runtime.manager.chain.Job;

@Slf4j
@Environment(ContextualEnvironment.class)
@Environment(SparkRunnerEnvironment.class)
@WithComponents("org.talend.sdk.component.junit.beam.test")
public class ProducerFinderEnvironmentTest implements Serializable {

    @Injected
    private BaseComponentsHandler handler;

    private static ComponentManager manager;

    @Service
    private RecordBuilderFactory factory;

    /**
     * + * arrayblocking queue capacity
     * + * 7 fixed CAPACITY variable
     * + * recordCount value
     * + * 10 11sec 13sec
     * + * 100 16sec 13sec
     * + * 1000 57sec 16sec
     * + * 10000 ~ 7min55 52sec
     * + * 100000 1h34min 25min
     * +
     */
    private final Integer recordCount =
            Integer.valueOf(System.getProperty("ProducerFinderEnvironmentTest.recordCount", "1000"));

    @BeforeAll
    static void forceManagerInit() {
        // manager for non environmental tests
        if (manager == null) {
            manager = ComponentManager.instance();
            if (manager.find(Stream::of).count() == 0) {
                manager.addPlugin(new File("target/test-classes").getAbsolutePath());
            }
        }
    }

    @Test
    void finderWithTacokitFamily() {
        final Iterator<Record> recordIterator = getInterator(ComponentManager.instance(), "TckFamily", recordCount);
        List<Record> records =
                StreamSupport.stream(Spliterators.spliteratorUnknownSize(recordIterator, Spliterator.ORDERED), false)
                        .collect(toList());
        Assertions.assertEquals(recordCount, records.size());
    }

    @Test
    void interateWithBeamFamily() {
        final Iterator<Record> recordIterator = getInterator(ComponentManager.instance(), "BeamFamily", recordCount);
        List<Record> records =
                StreamSupport.stream(Spliterators.spliteratorUnknownSize(recordIterator, Spliterator.ORDERED), false)
                        .collect(toList());
        Assertions.assertEquals(recordCount, records.size());
    }

    @EnvironmentalTest
    void runPipelineBeam() {
        runPipelineBeam(recordCount, 10);
    }

    @Disabled // Execute manually
    @EnvironmentalTest
    void runPipelineBeamPerfs() {
        FromConfig[] confs = new FromConfig[]{
                FromConfig.of(100_000, 10),
                FromConfig.of(100_000, 50),
                FromConfig.of(200_000, 50)
        };

        List<PerfResult> results = new ArrayList<>();
        for(FromConfig conf : confs) {
            long start = System.currentTimeMillis();
            runPipelineBeam(conf.getNbRows(), conf.getNbCols());
            long end = System.currentTimeMillis();
            results.add(new PerfResult(conf, (end - start)));
        }

        displayPerfResult("runPipelineBeamPerfs", results);
    }

    void runPipelineBeam(int nbRows, int nbCols) {
        Map<String, String> config = new HashMap<>();
        config.put("config.nbRows", String.valueOf(nbRows));
        config.put("config.nbCols", String.valueOf(nbCols));
        Mapper mapper =
                manager.findMapper("BeamFamily", "from", 1, config).get();
        assertNotNull(mapper);
        final Object delegate = Delegated.class.cast(mapper).getDelegate();
        assertNotNull(delegate);
        runPipeline((PTransform<PBegin, PCollection<Record>>) delegate, nbRows, nbCols);
    }

    @EnvironmentalTest
    void runPipelineTacokt() {
        Map<String, String> config = new HashMap<>();
        config.put("config.nbRows", String.valueOf(recordCount));
        config.put("config.nbCols", "10");

        Mapper mapper = manager.findMapper("TckFamily", "from", 1, config).get();
        assertNotNull(mapper);
        runPipeline(TalendIO.read(mapper), recordCount, 10);
    }

    private void runPipeline(PTransform<PBegin, PCollection<Record>> transform, int nbRows, int nbCols) {
        final Pipeline pipeline = Pipeline.create(PipelineOptionsFactory.create());
        final PTransform<PBegin, PCollection<Record>> start = transform;
        final PCollection<Record> out = pipeline.apply(start);

        List<Record> records = IntStream.rangeClosed(1, nbRows)
                .mapToObj(row -> {
                    Record.Builder builder = factory.newRecordBuilder();
                    IntStream.rangeClosed(1, nbCols)
                            .forEach(col -> builder.withString("col_" + col, "value_" + row + "_" + col));
                    return builder.build();
                })
                .collect(toList());

        PAssert.that(out).containsInAnyOrder(records);
        Assert.assertEquals(PipelineResult.State.DONE, pipeline.run().waitUntilFinish());
    }

    @EnvironmentalTest
    void runJobWithTacokitFamily() {
        runJob(handler.asManager(), "TckFamily");
    }

    @EnvironmentalTest
    void runJobWithBeamFamily() {
        runJob(handler.asManager(), "BeamFamily");
    }

    @Disabled // Run manually
    @EnvironmentalTest
    void runJobWithBeamFamilyPerfs() {

        FromConfig[] confs = new FromConfig[]{
                FromConfig.of(100, 10),
                FromConfig.of(100_000, 1),
                FromConfig.of(1_000, 15),
                FromConfig.of(100_000, 10),
                FromConfig.of(100_000, 20),
                FromConfig.of(100_000, 50)
        };

        List<PerfResult> results = new ArrayList<>();
        for(FromConfig c : confs){
            long start = System.currentTimeMillis();
            runJob(handler.asManager(), "BeamFamily", c.getNbRows(), c.getNbCols());
            long end = System.currentTimeMillis();
            long totalMillis = end - start;
            results.add(new PerfResult(c, totalMillis));
        }

        displayPerfResult("runJobWithBeamFamilyPerfs", results);

    }

    private void displayPerfResult(String name, List<PerfResult> results){
        StringBuilder sb = new StringBuilder();
        sb.append(System.lineSeparator());
        sb.append("Performance test: " + name);
        sb.append(System.lineSeparator());
        for(PerfResult result : results){
            Duration duration = Duration.ofMillis(result.getDuration());
            long minutes = duration.toMinutes();
            Duration durationSubMinutes = duration.minusMinutes(minutes);
            long total = durationSubMinutes.toMillis();
            long seconds = total / 1000;
            long millis = total % 1000;
            sb.append(
                    String.format("\t- cols: %s, rows: %s, duration: %s:%s:%s (%s ms)",
                            result.getConf().getNbCols(), result.getConf().getNbRows(), minutes, seconds, millis,
                            result.getDuration())
            );
            sb.append(System.lineSeparator());
        }
        sb.append(System.lineSeparator());
        log.info(sb.toString());
    }

    private Iterator<Record> getInterator(final ComponentManager manager, final String family,
                                          final int nbRows) {
        return getInterator(manager, family, nbRows, 10);
    }

    private Iterator<Record> getInterator(final ComponentManager manager, final String family,
            final int nbRows, final int nbCols) {
        final Container container = manager.findPlugin("test-classes").get();
        ProducerFinder finder = (ProducerFinder) container.get(AllServices.class)
                .getServices()
                .get(ProducerFinder.class);
        assertNotNull(finder);

        Map<String, String> config = new HashMap<>();
        config.put("config.nbRows", String.valueOf(nbRows));
        config.put("config.nbCols", String.valueOf(nbCols));

        final Iterator<Record> recordIterator = finder.find(family, "from", 1,
                config);

        assertNotNull(recordIterator);
        return recordIterator;
    }

    private void runJob(final ComponentManager manager, final String family) {
        runJob(manager, family, recordCount, 10);
    }

    private void runJob(final ComponentManager manager, final String family, int nbRows, int nbCols) {
        final Iterator<Record> recordIterator = getInterator(manager, family, nbRows, nbCols);
        assertNotNull(recordIterator);
        Iterable<Record> iterable = toIterable(recordIterator);
        handler.resetState();
        handler.setInputData(iterable);
        Job
                .components()
                .component("emitter", "test://emitter")
                .component("output", "test://collector")
                .connections()
                .from("emitter")
                .to("output")
                .build()
                .run();
        List<Record> records = handler.getCollectedData(Record.class);
        assertEquals(nbRows, records.size());
    }

    static <T> Iterable<T> toIterable(Iterator<T> it) {
        return () -> {
            return it;
        };
    }

    @PartitionMapper(name = "from", family = "BeamFamily")
    public static class BeamFamilyFrom extends PTransform<PBegin, PCollection<Record>> {

        private final FromConfig config;

        @Service
        private final RecordBuilderFactory recordBuilderFactory;

        public BeamFamilyFrom(@Option("config") final FromConfig config, final RecordBuilderFactory factory) {
            this.config = config;
            recordBuilderFactory = factory;
        }

        @Override
        public PCollection<Record> expand(final PBegin input) {
            PCollection<Record> apply = input.apply(Create.of(IntStream.rangeClosed(1, config.getNbRows())
                            .mapToObj(row -> {
                                Record.Builder builder = recordBuilderFactory.newRecordBuilder();
                                IntStream.rangeClosed(1, config.getNbCols()).forEach(col ->
                                        builder.withString("col_" + col, "value_" + row + "_" + col));
                                return builder.build();
                            })
                            .collect(toList()))
                    .withCoder(SchemaRegistryCoder.of()));
            return apply;
        }

    }

    @PartitionMapper(name = "from", family = "TckFamily")
    public static class TckFamilyFrom implements Serializable {

        @Service
        private final RecordBuilderFactory recordBuilderFactory;

        private final FromConfig conf;

        private int counted = 1;

        public TckFamilyFrom(@Option("config") final FromConfig conf, final RecordBuilderFactory factory) {
            this.conf = conf;
            recordBuilderFactory = factory;
        }

        @Assessor
        public long estimateSize() {
            return 1;
        }

        @Split
        public List<TckFamilyFrom> split(@PartitionSize final long bundleSize) {
            return singletonList(this);
        }

        @Emitter
        public TckFamilyFrom create() {
            return this;
        }

        @Producer
        public Record next() {
            Record record = counted > this.conf.getNbRows() ? null : getRecord();
            this.counted++;
            return record;
        }

        private Record getRecord(){
            Record.Builder builder = recordBuilderFactory.newRecordBuilder();
            IntStream.rangeClosed(1, this.conf.getNbCols())
                    .forEach(col -> builder.withString("col_"+ col, "value_"+ this.counted + "_"+ col));
                    return builder.build();
        }
    }

    @AllArgsConstructor
    @Data
    private static class PerfResult{
        private FromConfig conf;
        private long duration;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    public static class FromConfig implements Serializable{

        @Option
        private int nbRows;

        @Option int nbCols;

        private static FromConfig of(int nbRows, int nbCols){
            return new FromConfig(nbRows, nbCols);
        }

    }

}
