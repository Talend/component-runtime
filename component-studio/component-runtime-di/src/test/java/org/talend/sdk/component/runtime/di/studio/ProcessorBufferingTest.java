/**
 * Copyright (C) 2006-2026 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.runtime.di.studio;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import javax.json.bind.Jsonb;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.talend.sdk.component.api.processor.AfterGroup;
import org.talend.sdk.component.api.processor.BeforeGroup;
import org.talend.sdk.component.api.processor.ElementListener;
import org.talend.sdk.component.api.processor.Input;
import org.talend.sdk.component.api.processor.LastGroup;
import org.talend.sdk.component.api.processor.Output;
import org.talend.sdk.component.api.processor.OutputIterator;
import org.talend.sdk.component.api.processor.Processor;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;
import org.talend.sdk.component.runtime.di.AutoChunkProcessor;
import org.talend.sdk.component.runtime.di.InputsHandler;
import org.talend.sdk.component.runtime.di.JobStateAware;
import org.talend.sdk.component.runtime.di.OutputsHandler;
import org.talend.sdk.component.runtime.manager.ComponentManager;
import org.talend.sdk.component.runtime.output.InputFactory;
import org.talend.sdk.component.runtime.output.OutputFactory;
import org.talend.sdk.component.runtime.record.RecordConverters;

import lombok.Getter;
import lombok.ToString;

/**
 * Manual test to observe processor output buffering problem (QTDI-2709).
 *
 * Run this test to see memory consumption when a processor emits many records.
 * With the current push model, ALL records are buffered in memory before the
 * consumer can drain them.
 *
 * Expected behavior on current code: test FAILS (memory exceeds threshold).
 * After iterator pattern is implemented: test PASSES (memory stays low).
 */
class ProcessorBufferingTest {

    protected static RecordBuilderFactory builderFactory;

    // Large enough to create observable memory pressure
    static final int RECORD_COUNT = 100_000;

    // Memory threshold: if buffering all records uses more than this, the test fails.
    // 100K records with strings should use >10MB when fully buffered.
    // With streaming, memory should stay well under this.
    static final long MAX_MEMORY_DELTA_MB = 5;

    enum OutputMode {
        NO_OUTPUT,
        ONE_OUTPUT,
        TWO_OUTPUT
    }

    @BeforeAll
    static void forceManagerInit() {
        final ComponentManager manager = ComponentManager.instance();
        if (manager.find(Stream::of).count() == 0) {
            manager.addPlugin(new File("target/test-classes").getAbsolutePath());
        }
    }

    /**
     * Tests memory consumption when a processor emits 100K records in @ElementListener.
     *
     * FAILS on current code: all 100K records buffered → memory spike > threshold.
     * PASSES after iterator implementation: records stream lazily → memory stays low.
     */
    @ParameterizedTest
    @EnumSource(OutputMode.class)
    void elementListenerShouldNotBufferAllRecordsInMemory(OutputMode outputMode) {
        final ComponentManager manager = ComponentManager.instance();
        final Map<Class<?>, Object> servicesMapper = getServicesMapper(manager);
        final Jsonb jsonb = (Jsonb) servicesMapper.get(Jsonb.class);
        builderFactory = (RecordBuilderFactory) servicesMapper.get(RecordBuilderFactory.class);

        final org.talend.sdk.component.runtime.output.Processor processor = manager
                .findProcessor("ProcessorBufferingTest", "heavyEmitter", 1, new HashMap<>())
                .orElseThrow(() -> new IllegalStateException("processor not found"));
        JobStateAware.init(processor, new HashMap<>());

        final AutoChunkProcessor chunkProcessor = new AutoChunkProcessor(RECORD_COUNT + 1, processor);
        chunkProcessor.start();

        final InputsHandler inputsHandler = new InputsHandler(jsonb, servicesMapper);
        inputsHandler.addConnection("FLOW", row1Struct.class);

        final OutputsHandler outputsHandler = new OutputsHandler(jsonb, servicesMapper);
        if (outputMode != OutputMode.NO_OUTPUT) {
            outputsHandler.addConnection("FLOW", row1Struct.class);
        }

        final InputFactory inputFactory = inputsHandler.asInputFactory();
        final OutputFactory outputFactory = outputsHandler.asOutputFactory();

        // Prepare one input record
        final Record inputRecord = builderFactory.newRecordBuilder()
                .withString("id", "input-1")
                .withString("name", "trigger")
                .build();
        final RecordConverters.MappingMetaRegistry registry = new RecordConverters.MappingMetaRegistry();
        final row1Struct inputRow = (row1Struct) registry.find(row1Struct.class).newInstance(inputRecord);
        inputsHandler.setInputValue("FLOW", inputRow);

        // Force GC to get a clean baseline
        gc();

        final long memoryBefore = usedMemoryMB();

        // --- Producer emits all records in onElement() ---
        chunkProcessor.onElement(inputFactory, outputFactory);

        // Drain — same as Studio generated code (inside + outside loop, skipped for NO_OUTPUT)
        int drainedCount = 0;
        if (outputMode != OutputMode.NO_OUTPUT) {
            // sim inside of input loop
            while (outputsHandler.hasMoreData()) {
                outputsHandler.getValue("FLOW");
                drainedCount++;
            }
        }

        chunkProcessor.flush(outputFactory);

        // GC to reclaim input record objects from the loop
        gc();

        // Measure memory AFTER production, BEFORE drain — valid for all output modes
        final long memoryAfterProduction = usedMemoryMB();
        final long memoryDelta = memoryAfterProduction - memoryBefore;

        System.out.println("=== ProcessorBufferingTest: @ElementListener ===");
        System.out.println("Output mode:               " + outputMode);
        System.out.println("Records emitted: " + RECORD_COUNT);
        System.out.println("Memory before onElement(): " + memoryBefore + " MB");
        System.out.println("Memory after onElement():  " + memoryAfterProduction + " MB");
        System.out.println("Memory delta:              " + memoryDelta + " MB");
        System.out.println("Threshold:                 " + MAX_MEMORY_DELTA_MB + " MB");

        if (outputMode != OutputMode.NO_OUTPUT) {
            // sim outside of input loop
            while (outputsHandler.hasMoreData()) {
                outputsHandler.getValue("FLOW");
                drainedCount++;
            }
        }
        System.out.println("Records drained:           " + drainedCount);

        chunkProcessor.stop();

        // ASSERTION: memory delta should be under threshold
        // FAILS on current code (all records buffered → large delta)
        // PASSES after iterator implementation (lazy streaming → small delta)
        assertTrue(memoryDelta <= MAX_MEMORY_DELTA_MB,
                "Memory delta should be <= " + MAX_MEMORY_DELTA_MB + " MB (streaming), "
                        + "but was " + memoryDelta + " MB (all records buffered in memory)");
    }

    /**
     * Tests memory consumption when a processor emits 100K records in @AfterGroup.
     *
     * FAILS on current code: all records buffered in @AfterGroup.
     * PASSES after iterator implementation.
     */
    @ParameterizedTest
    @EnumSource(OutputMode.class)
    void afterGroupShouldNotBufferAllRecordsInMemory(OutputMode outputMode) {
        final ComponentManager manager = ComponentManager.instance();
        final Map<Class<?>, Object> servicesMapper = getServicesMapper(manager);
        final Jsonb jsonb = (Jsonb) servicesMapper.get(Jsonb.class);
        builderFactory = (RecordBuilderFactory) servicesMapper.get(RecordBuilderFactory.class);

        final org.talend.sdk.component.runtime.output.Processor processor = manager
                .findProcessor("ProcessorBufferingTest", "heavyAfterGroupEmitter", 1, new HashMap<>())
                .orElseThrow(() -> new IllegalStateException("processor not found"));
        JobStateAware.init(processor, new HashMap<>());

        // chunkSize=5: after 5 inputs, @AfterGroup fires and emits 100K records
        final AutoChunkProcessor chunkProcessor = new AutoChunkProcessor(5, processor);
        chunkProcessor.start();

        final InputsHandler inputsHandler = new InputsHandler(jsonb, servicesMapper);
        inputsHandler.addConnection("FLOW", row1Struct.class);

        final OutputsHandler outputsHandler = new OutputsHandler(jsonb, servicesMapper);
        if (outputMode != OutputMode.NO_OUTPUT) {
            outputsHandler.addConnection("MAIN", row1Struct.class);
            if (outputMode == OutputMode.TWO_OUTPUT) {
                outputsHandler.addConnection("REJECT", row1Struct.class);
            }
        }

        final InputFactory inputFactory = inputsHandler.asInputFactory();
        final OutputFactory outputFactory = outputsHandler.asOutputFactory();

        final RecordConverters.MappingMetaRegistry registry = new RecordConverters.MappingMetaRegistry();

        int mainCount = 0;
        int rejectCount = 0;

        // Force GC to get a clean baseline
        gc();
        final long memoryBefore = usedMemoryMB();

        for (int i = 0; i < RECORD_COUNT; i++) {
            final Record inputRecord = builderFactory.newRecordBuilder()
                    .withString("id", "input-" + i)
                    .withString("name", "record-" + i)
                    .build();
            final row1Struct inputRow = (row1Struct) registry.find(row1Struct.class).newInstance(inputRecord);
            inputsHandler.setInputValue("FLOW", inputRow);

            chunkProcessor.onElement(inputFactory, outputFactory);

            if (outputMode != OutputMode.NO_OUTPUT) {
                // Drain after each onElement — same as Studio generated code
                while (outputsHandler.hasMoreData()) {
                    if (outputsHandler.getValue("MAIN") != null) {
                        mainCount++;
                    }
                    if (outputMode == OutputMode.TWO_OUTPUT) {
                        if (outputsHandler.getValue("REJECT") != null) {
                            rejectCount++;
                        }
                    }
                }
            }
        }

        chunkProcessor.flush(outputFactory);

        // GC to reclaim input record objects from the loop
        gc();

        final long memoryAfterGroup = usedMemoryMB();
        final long memoryDelta = memoryAfterGroup - memoryBefore;

        System.out.println("=== ProcessorBufferingTest: @AfterGroup ===");
        System.out.println("Records emitted: " + RECORD_COUNT);
        System.out.println("Memory before:             " + memoryBefore + " MB");
        System.out.println("Memory after @AfterGroup:  " + memoryAfterGroup + " MB");
        System.out.println("Memory delta:              " + memoryDelta + " MB");

        if (outputMode != OutputMode.NO_OUTPUT) {
            while (outputsHandler.hasMoreData()) {
                if (outputsHandler.getValue("MAIN") != null) {
                    mainCount++;
                }
                if (outputMode == OutputMode.TWO_OUTPUT) {
                    if (outputsHandler.getValue("REJECT") != null) {
                        rejectCount++;
                    }
                }
            }

            // Verify all records were produced
            assertTrue(mainCount + rejectCount > 0,
                    "Should have produced records in @AfterGroup");
        }

        System.out.println("MAIN drained:              " + mainCount);
        System.out.println("REJECT drained:            " + rejectCount);

        chunkProcessor.stop();

        // ASSERTION: memory delta should be under threshold
        // FAILS on current code (all records buffered → large delta)
        // PASSES after iterator implementation (lazy streaming → small delta)
        assertTrue(memoryDelta <= MAX_MEMORY_DELTA_MB,
                "Memory delta should be <= " + MAX_MEMORY_DELTA_MB + " MB (streaming), "
                        + "but was " + memoryDelta + " MB (all records buffered in memory)");
    }

    // --- Helpers ---

    private static long usedMemoryMB() {
        final Runtime rt = Runtime.getRuntime();
        return (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
    }

    private Map<Class<?>, Object> getServicesMapper(ComponentManager manager) {
        return manager
                .findPlugin("test-classes")
                .get()
                .get(ComponentManager.AllServices.class)
                .getServices();
    }

    // --- Test components ---

    /**
     * Processor that provides a lazy iterator in @ElementListener.
     * Records are produced on-demand during the drain loop.
     */
    @Processor(name = "heavyEmitter", family = "ProcessorBufferingTest")
    public static class HeavyEmitterProcessor implements Serializable {

        @ElementListener
        public void onElement(@Input final Record input,
                @Output final OutputIterator<Record> output) {
            output.setIterator(new java.util.Iterator<>() {

                private int index = 0;

                @Override
                public boolean hasNext() {
                    return index < RECORD_COUNT;
                }

                @Override
                public Record next() {
                    final Record rec = builderFactory.newRecordBuilder()
                            .withString("id", "out-" + index)
                            .withString("name", "generated-record-with-some-payload-" + index)
                            .withString("data", "additional-field-to-increase-memory-footprint-" + index)
                            .build();
                    index++;
                    return rec;
                }
            });
        }
    }

    /**
     * Processor that accumulates inputs, then sets lazy iterators on MAIN and REJECT
     * in @AfterGroup. Simulates the N:M batch pattern.
     */
    @Processor(name = "heavyAfterGroupEmitter", family = "ProcessorBufferingTest")
    public static class HeavyAfterGroupEmitterProcessor implements Serializable {

        private int inputCount = 0;

        @BeforeGroup
        public void beforeGroup() {
            // no-op
        }

        @ElementListener
        public void onElement(@Input final Record input) {
            inputCount++;
        }

        @AfterGroup
        public void afterGroup(@Output("MAIN") final OutputIterator<Record> main,
                @Output("REJECT") final OutputIterator<Record> reject,
                @LastGroup final boolean lastGroup) {
            if (!lastGroup) {
                return;
            }

            // MAIN gets records where index % 2 != 0
            main.setIterator(new java.util.Iterator<>() {

                private int index = 0;

                @Override
                public boolean hasNext() {
                    while (index < RECORD_COUNT && index % 2 == 0) {
                        index++;
                    }
                    return index < RECORD_COUNT;
                }

                @Override
                public Record next() {
                    final Record rec = builderFactory.newRecordBuilder()
                            .withString("id", "result-" + index)
                            .withString("name", "processed-record-with-payload-" + index)
                            .withString("data", "bulk-result-data-field-" + index)
                            .build();
                    index++;
                    return rec;
                }
            });

            // REJECT gets records where index % 2 == 0
            reject.setIterator(new java.util.Iterator<>() {

                private int index = 0;

                @Override
                public boolean hasNext() {
                    while (index < RECORD_COUNT && index % 2 != 0) {
                        index++;
                    }
                    return index < RECORD_COUNT;
                }

                @Override
                public Record next() {
                    final Record rec = builderFactory.newRecordBuilder()
                            .withString("id", "reject-" + index)
                            .withString("name", "rejected-record-" + index)
                            .withString("data", "reject-data-" + index)
                            .build();
                    index++;
                    return rec;
                }
            });

            inputCount = 0;
        }
    }

    // --- Row struct ---

    @Getter
    @ToString
    public static class row1Struct implements routines.system.IPersistableRow {

        public String id;

        public String name;

        public String data;

        @Override
        public void writeData(final ObjectOutputStream objectOutputStream) {
            throw new UnsupportedOperationException("#writeData()");
        }

        @Override
        public void readData(final ObjectInputStream objectInputStream) {
            throw new UnsupportedOperationException("#readData()");
        }
    }

    private void gc() {
        // GC to reclaim input record objects from the loop
        System.gc();
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
