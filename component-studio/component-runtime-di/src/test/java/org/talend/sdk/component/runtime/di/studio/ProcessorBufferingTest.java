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
import org.talend.sdk.component.api.processor.MultiOutputIterator;
import org.talend.sdk.component.api.processor.Output;
import org.talend.sdk.component.api.processor.Processor;
import org.talend.sdk.component.api.processor.TaggedOutput;
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

    /**
     * Tests that a processor using a split {@link MultiOutputIterator} in {@code @AfterGroup}
     * correctly routes records between MAIN and REJECT via {@code hasDataFor} without buffering.
     *
     * <p>
     * Uses a single shared tagged iterator that routes even-indexed records to MAIN and
     * odd-indexed records to REJECT. The drain loop uses {@code hasDataFor} to consume
     * only the connection that has a record ready — which exercises the fix in
     * {@code hasDataFor} that skips unregistered pending records instead of stalling.
     */
    @ParameterizedTest
    @EnumSource(OutputMode.class)
    void afterGroupSplitIteratorShouldRouteRecordsWithHasDataFor(OutputMode outputMode) {
        final ComponentManager manager = ComponentManager.instance();
        final Map<Class<?>, Object> servicesMapper = getServicesMapper(manager);
        final Jsonb jsonb = (Jsonb) servicesMapper.get(Jsonb.class);
        builderFactory = (RecordBuilderFactory) servicesMapper.get(RecordBuilderFactory.class);

        final org.talend.sdk.component.runtime.output.Processor processor = manager
                .findProcessor("ProcessorBufferingTest", "splitAfterGroupEmitter", 1, new HashMap<>())
                .orElseThrow(() -> new IllegalStateException("splitAfterGroupEmitter processor not found"));
        JobStateAware.init(processor, new HashMap<>());

        // chunkSize=5: after 5 inputs, @AfterGroup fires and emits 100K records via split iterator
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

        gc();
        final long memoryBefore = usedMemoryMB();

        for (int i = 0; i < 5; i++) {
            final Record inputRecord = builderFactory.newRecordBuilder()
                    .withString("id", "input-" + i)
                    .withString("name", "record-" + i)
                    .build();
            final row1Struct inputRow = (row1Struct) registry.find(row1Struct.class).newInstance(inputRecord);
            inputsHandler.setInputValue("FLOW", inputRow);

            chunkProcessor.onElement(inputFactory, outputFactory);

            if (outputMode != OutputMode.NO_OUTPUT) {
                // Drain using hasDataFor — exercises the fix that skips unregistered pending records
                while (outputsHandler.hasMoreData()) {
                    if (outputsHandler.hasDataFor("MAIN")) {
                        outputsHandler.getValue("MAIN");
                        mainCount++;
                    }
                    if (outputMode == OutputMode.TWO_OUTPUT && outputsHandler.hasDataFor("REJECT")) {
                        outputsHandler.getValue("REJECT");
                        rejectCount++;
                    }
                }
            }
        }

        chunkProcessor.flush(outputFactory);

        // Drain after flush — @AfterGroup with lastGroup=true fires inside flush(),
        // so the split iterator is only set there and must be drained here.
        if (outputMode != OutputMode.NO_OUTPUT) {
            while (outputsHandler.hasMoreData()) {
                if (outputsHandler.hasDataFor("MAIN")) {
                    outputsHandler.getValue("MAIN");
                    mainCount++;
                }
                if (outputMode == OutputMode.TWO_OUTPUT && outputsHandler.hasDataFor("REJECT")) {
                    outputsHandler.getValue("REJECT");
                    rejectCount++;
                }
            }
        }

        gc();

        final long memoryAfter = usedMemoryMB();
        final long memoryDelta = memoryAfter - memoryBefore;

        System.out.println("=== ProcessorBufferingTest: @AfterGroup split iterator ===");
        System.out.println("Output mode:     " + outputMode);
        System.out.println("MAIN drained:    " + mainCount);
        System.out.println("REJECT drained:  " + rejectCount);
        System.out.println("Memory delta:    " + memoryDelta + " MB");

        final int half = RECORD_COUNT / 2;
        switch (outputMode) {
            case TWO_OUTPUT:
                org.junit.jupiter.api.Assertions.assertEquals(half, mainCount,
                        "MAIN should receive half the records (even indices)");
                org.junit.jupiter.api.Assertions.assertEquals(half, rejectCount,
                        "REJECT should receive half the records (odd indices)");
                break;
            case ONE_OUTPUT:
                org.junit.jupiter.api.Assertions.assertEquals(half, mainCount,
                        "MAIN should receive half the records");
                org.junit.jupiter.api.Assertions.assertEquals(0, rejectCount,
                        "REJECT not registered, should receive nothing");
                break;
            case NO_OUTPUT:
                org.junit.jupiter.api.Assertions.assertEquals(0, mainCount + rejectCount,
                        "No connections registered, no records should be received");
                break;
        }
        org.junit.jupiter.api.Assertions.assertTrue(memoryDelta <= MAX_MEMORY_DELTA_MB,
                "Memory delta should be <= " + MAX_MEMORY_DELTA_MB + " MB, was " + memoryDelta + " MB");

        chunkProcessor.stop();
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

    /**
     * Tests that a processor using {@link MultiOutputIterator} correctly splits records
     * between MAIN and REJECT without buffering and without requiring lockstep iteration.
     *
     * <p>
     * The split processor routes even-indexed records to MAIN and odd-indexed to REJECT
     * from a SINGLE shared source iterator. Without tagged-output support this would
     * require buffering all records first.
     */
    @ParameterizedTest
    @EnumSource(OutputMode.class)
    void multiOutputIteratorShouldSplitRecordsWithoutBuffering(OutputMode outputMode) {
        final ComponentManager manager = ComponentManager.instance();
        final Map<Class<?>, Object> servicesMapper = getServicesMapper(manager);
        final Jsonb jsonb = (Jsonb) servicesMapper.get(Jsonb.class);
        builderFactory = (RecordBuilderFactory) servicesMapper.get(RecordBuilderFactory.class);

        final org.talend.sdk.component.runtime.output.Processor processor = manager
                .findProcessor("ProcessorBufferingTest", "splitEmitter", 1, new HashMap<>())
                .orElseThrow(() -> new IllegalStateException("splitEmitter processor not found"));
        JobStateAware.init(processor, new HashMap<>());

        final AutoChunkProcessor chunkProcessor = new AutoChunkProcessor(RECORD_COUNT + 1, processor);
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

        final Record inputRecord = builderFactory.newRecordBuilder()
                .withString("id", "input-1")
                .withString("name", "trigger")
                .build();
        final RecordConverters.MappingMetaRegistry registry = new RecordConverters.MappingMetaRegistry();
        final row1Struct inputRow = (row1Struct) registry.find(row1Struct.class).newInstance(inputRecord);
        inputsHandler.setInputValue("FLOW", inputRow);

        gc();
        final long memoryBefore = usedMemoryMB();

        chunkProcessor.onElement(inputFactory, outputFactory);

        // Drain using per-connection hasDataFor checks — correct for split streaming
        int mainCount = 0;
        int rejectCount = 0;
        if (outputMode != OutputMode.NO_OUTPUT) {
            while (outputsHandler.hasMoreData()) {
                if (outputsHandler.hasDataFor("MAIN")) {
                    outputsHandler.getValue("MAIN");
                    mainCount++;
                }
                if (outputMode == OutputMode.TWO_OUTPUT) {
                    if (outputsHandler.hasDataFor("REJECT")) {
                        outputsHandler.getValue("REJECT");
                        rejectCount++;
                    }
                }
            }
        }

        chunkProcessor.flush(outputFactory);
        gc();
        final long memoryDelta = usedMemoryMB() - memoryBefore;

        System.out.println("=== ProcessorBufferingTest: MultiOutputIterator split ===");
        System.out.println("Records emitted: " + RECORD_COUNT);
        System.out.println("MAIN drained:    " + mainCount);
        System.out.println("REJECT drained:  " + rejectCount);
        System.out.println("Memory delta:    " + memoryDelta + " MB");

        final int expected = RECORD_COUNT / 2;
        switch (outputMode) {
            case TWO_OUTPUT:
                // Both connections registered: each receives exactly half
                org.junit.jupiter.api.Assertions.assertEquals(expected, mainCount,
                        "MAIN should receive half the records");
                org.junit.jupiter.api.Assertions.assertEquals(expected, rejectCount,
                        "REJECT should receive the other half");
                break;
            case ONE_OUTPUT:
                // Only MAIN registered: MAIN gets its half, REJECT records are skipped
                org.junit.jupiter.api.Assertions.assertEquals(expected, mainCount,
                        "MAIN should receive half the records");
                org.junit.jupiter.api.Assertions.assertEquals(0, rejectCount,
                        "REJECT not registered, should receive nothing");
                break;
            case NO_OUTPUT:
                // No connections registered: nothing drained
                org.junit.jupiter.api.Assertions.assertEquals(0, mainCount + rejectCount,
                        "No connections registered, no records should be received");
                break;
        }
        org.junit.jupiter.api.Assertions.assertTrue(memoryDelta <= MAX_MEMORY_DELTA_MB,
                "Memory delta should be <= " + MAX_MEMORY_DELTA_MB + " MB, was " + memoryDelta + " MB");

        chunkProcessor.stop();
    }

    /**
     * Tests that a processor using {@link MultiOutputIterator#setIterator(String, Iterator)}
     * (independent mode) correctly streams independent lazy sources per output connection,
     * equivalent to using two separate output parameters but from one
     * fixed method parameter.
     */
    @ParameterizedTest
    @EnumSource(OutputMode.class)
    void multiOutputIteratorIndependentModeShouldStreamPerConnection(OutputMode outputMode) {
        final ComponentManager manager = ComponentManager.instance();
        final Map<Class<?>, Object> servicesMapper = getServicesMapper(manager);
        final Jsonb jsonb = (Jsonb) servicesMapper.get(Jsonb.class);
        builderFactory = (RecordBuilderFactory) servicesMapper.get(RecordBuilderFactory.class);

        final org.talend.sdk.component.runtime.output.Processor processor = manager
                .findProcessor("ProcessorBufferingTest", "independentEmitter", 1, new HashMap<>())
                .orElseThrow(() -> new IllegalStateException("independentEmitter processor not found"));
        JobStateAware.init(processor, new HashMap<>());

        final AutoChunkProcessor chunkProcessor = new AutoChunkProcessor(RECORD_COUNT + 1, processor);
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

        final Record inputRecord = builderFactory.newRecordBuilder()
                .withString("id", "input-1")
                .withString("name", "trigger")
                .build();
        final RecordConverters.MappingMetaRegistry registry = new RecordConverters.MappingMetaRegistry();
        final row1Struct inputRow = (row1Struct) registry.find(row1Struct.class).newInstance(inputRecord);
        inputsHandler.setInputValue("FLOW", inputRow);

        gc();
        final long memoryBefore = usedMemoryMB();

        chunkProcessor.onElement(inputFactory, outputFactory);

        // Independent mode: drain each connection with hasDataFor
        int mainCount = 0;
        int rejectCount = 0;
        if (outputMode != OutputMode.NO_OUTPUT) {
            while (outputsHandler.hasMoreData()) {
                if (outputsHandler.hasDataFor("MAIN")) {
                    outputsHandler.getValue("MAIN");
                    mainCount++;
                }
                if (outputMode == OutputMode.TWO_OUTPUT && outputsHandler.hasDataFor("REJECT")) {
                    outputsHandler.getValue("REJECT");
                    rejectCount++;
                }
            }
        }

        chunkProcessor.flush(outputFactory);
        gc();
        final long memoryDelta = usedMemoryMB() - memoryBefore;

        System.out.println("=== ProcessorBufferingTest: MultiOutputIterator independent mode ===");
        System.out.println("Output mode:     " + outputMode);
        System.out.println("MAIN drained:    " + mainCount);
        System.out.println("REJECT drained:  " + rejectCount);
        System.out.println("Memory delta:    " + memoryDelta + " MB");

        switch (outputMode) {
            case TWO_OUTPUT:
                org.junit.jupiter.api.Assertions.assertEquals(RECORD_COUNT, mainCount,
                        "MAIN should receive all records");
                org.junit.jupiter.api.Assertions.assertEquals(RECORD_COUNT / 2, rejectCount,
                        "REJECT should receive half the records");
                break;
            case ONE_OUTPUT:
                org.junit.jupiter.api.Assertions.assertEquals(RECORD_COUNT, mainCount,
                        "MAIN should receive all records");
                org.junit.jupiter.api.Assertions.assertEquals(0, rejectCount,
                        "REJECT not registered, should receive nothing");
                break;
            case NO_OUTPUT:
                org.junit.jupiter.api.Assertions.assertEquals(0, mainCount + rejectCount,
                        "No connections registered, no records should be received");
                break;
        }
        org.junit.jupiter.api.Assertions.assertTrue(memoryDelta <= MAX_MEMORY_DELTA_MB,
                "Memory delta should be <= " + MAX_MEMORY_DELTA_MB + " MB, was " + memoryDelta + " MB");

        chunkProcessor.stop();
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
                @Output final MultiOutputIterator<Record> output) {
            output.setIterator("FLOW", new java.util.Iterator<>() {

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
        public void afterGroup(@Output final MultiOutputIterator<Record> out,
                @LastGroup final boolean lastGroup) {
            if (!lastGroup) {
                return;
            }

            // MAIN gets records where index % 2 != 0
            out.setIterator("MAIN", new java.util.Iterator<>() {

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
            out.setIterator("REJECT", new java.util.Iterator<>() {

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
        }
    }

    /**
     * Processor that uses a single shared split {@link MultiOutputIterator} in {@code @AfterGroup}
     * to route even-indexed records to MAIN and odd-indexed records to REJECT via {@link TaggedOutput}.
     * This tests the {@code hasDataFor} fix: when only MAIN is registered, odd-indexed pending
     * records must be skipped rather than stalling the drain loop.
     */
    @Processor(name = "splitAfterGroupEmitter", family = "ProcessorBufferingTest")
    public static class SplitAfterGroupEmitterProcessor implements Serializable {

        @BeforeGroup
        public void beforeGroup() {
        }

        @ElementListener
        public void onElement(@Input final Record input) {
        }

        @AfterGroup
        public void afterGroup(@Output final MultiOutputIterator<Record> out,
                @LastGroup final boolean lastGroup) {
            if (!lastGroup) {
                return;
            }
            out.setIterator(new java.util.Iterator<>() {

                private int index = 0;

                @Override
                public boolean hasNext() {
                    return index < RECORD_COUNT;
                }

                @Override
                public TaggedOutput<Record> next() {
                    final String outputName = (index % 2 == 0) ? "MAIN" : "REJECT";
                    final Record rec = builderFactory.newRecordBuilder()
                            .withString("id", outputName.toLowerCase() + "-" + index)
                            .withString("name", "split-aftergroup-record-" + index)
                            .withString("data", "split-aftergroup-data-" + index)
                            .build();
                    index++;
                    return TaggedOutput.of(outputName, rec);
                }
            });
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

    /**
     * Processor that uses a single {@link MultiOutputIterator} to split 100K records
     * between MAIN (even indices) and REJECT (odd indices) from a single lazy source.
     * This is the true-split scenario: one source, two outputs, no buffering.
     */
    @Processor(name = "splitEmitter", family = "ProcessorBufferingTest")
    public static class SplitEmitterProcessor implements Serializable {

        @ElementListener
        public void onElement(@Input final Record input,
                @Output final MultiOutputIterator<Record> splitter) {
            splitter.setIterator(new java.util.Iterator<>() {

                private int index = 0;

                @Override
                public boolean hasNext() {
                    return index < RECORD_COUNT;
                }

                @Override
                public TaggedOutput<Record> next() {
                    final String outputName = (index % 2 == 0) ? "MAIN" : "REJECT";
                    final Record rec = builderFactory.newRecordBuilder()
                            .withString("id", outputName.toLowerCase() + "-" + index)
                            .withString("name", "split-record-" + index)
                            .withString("data", "split-data-" + index)
                            .build();
                    index++;
                    return TaggedOutput.of(outputName, rec);
                }
            });
        }
    }

    /**
     * Processor that uses {@link MultiOutputIterator#setIterator(String, Iterator)}
     * (independent mode) to assign separate lazy sources to MAIN and REJECT.
     * MAIN gets RECORD_COUNT records, REJECT gets RECORD_COUNT/2 records.
     */
    @Processor(name = "independentEmitter", family = "ProcessorBufferingTest")
    public static class IndependentEmitterProcessor implements Serializable {

        @ElementListener
        public void onElement(@Input final Record input,
                @Output final MultiOutputIterator<Record> out) {
            out.setIterator("MAIN", new java.util.Iterator<>() {

                private int index = 0;

                @Override
                public boolean hasNext() {
                    return index < RECORD_COUNT;
                }

                @Override
                public Record next() {
                    return builderFactory.newRecordBuilder()
                            .withString("id", "main-" + index)
                            .withString("name", "main-record-" + index++)
                            .build();
                }
            });
            out.setIterator("REJECT", new java.util.Iterator<>() {

                private int index = 0;

                @Override
                public boolean hasNext() {
                    return index < RECORD_COUNT / 2;
                }

                @Override
                public Record next() {
                    return builderFactory.newRecordBuilder()
                            .withString("id", "reject-" + index)
                            .withString("name", "reject-record-" + index++)
                            .build();
                }
            });
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
