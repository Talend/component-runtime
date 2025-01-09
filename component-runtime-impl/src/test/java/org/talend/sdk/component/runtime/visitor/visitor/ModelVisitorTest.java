/**
 * Copyright (C) 2006-2025 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.runtime.visitor.visitor;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.component.AfterVariables.AfterVariable;
import org.talend.sdk.component.api.component.AfterVariables.AfterVariableContainer;
import org.talend.sdk.component.api.input.Assessor;
import org.talend.sdk.component.api.input.Emitter;
import org.talend.sdk.component.api.input.PartitionMapper;
import org.talend.sdk.component.api.input.PartitionSize;
import org.talend.sdk.component.api.input.Producer;
import org.talend.sdk.component.api.input.Split;
import org.talend.sdk.component.api.processor.AfterGroup;
import org.talend.sdk.component.api.processor.ElementListener;
import org.talend.sdk.component.api.processor.Processor;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.standalone.DriverRunner;
import org.talend.sdk.component.api.standalone.RunAtDriver;
import org.talend.sdk.component.runtime.visitor.ModelListener;
import org.talend.sdk.component.runtime.visitor.ModelVisitor;
import org.talend.sdk.component.runtime.visitor.visitor.ModelVisitorTest.MapperAfterVariableNokWrongDeclaredType.Mapper;
import org.talend.sdk.component.runtime.visitor.visitor.ModelVisitorTest.Registrar.In;

class ModelVisitorTest {

    @Test
    void valid() {
        assertEquals(asList("@Emitter(org.talend.sdk.component.runtime.visitor.visitor.ModelVisitorTest$Registrar$In)",
                "@PartitionMapper(org.talend.sdk.component.runtime.visitor.visitor.ModelVisitorTest$Registrar$Mapper)",
                "@Processor(org.talend.sdk.component.runtime.visitor.visitor.ModelVisitorTest$Registrar$Out)",
                "@DriverRunner(org.talend.sdk.component.runtime.visitor.visitor.ModelVisitorTest$Registrar$Standalone)"),
                visit(Registrar.class));
    }

    @Test
    void validBulk() {
        assertEquals(singletonList(
                "@Processor(org.talend.sdk.component.runtime.visitor.visitor.ModelVisitorTest$ProcessorBulk$Out)"),
                visit(ProcessorBulk.class));
    }

    @Test
    void componentWithConflictingAnnotations() {
        assertThrows(IllegalArgumentException.class, () -> visit(InvalidComponent.class));
    }

    @Test
    void producerNoProduce() {
        assertThrows(IllegalArgumentException.class, () -> visit(EmitterNoProduces.class));
    }

    @Test
    void processorNoListener() {
        assertThrows(IllegalArgumentException.class, () -> visit(ProcessorNoListener.class));
    }

    @Test
    void mapperNoAssessor() {
        assertThrows(IllegalArgumentException.class, () -> visit(MapperNoAssessor.class));
    }

    @Test
    void mapperInfiniteNoAssessor() {
        visit(InfiniteMapperNoAssessor.class);
    }

    @Test
    void mapperInfiniteStoppable() {
        visit(InfiniteMapperStoppable.class);
    }

    @Test
    void mapperStoppable() {
        assertThrows(IllegalArgumentException.class, () -> visit(MapperStoppable.class));
    }

    @Test
    void mapperNoSplit() {
        assertThrows(IllegalArgumentException.class, () -> visit(MapperNoSplit.class));
    }

    @Test
    void mapperNoEmitter() {
        assertThrows(IllegalArgumentException.class, () -> visit(MapperNoEmitter.class));
    }

    @Test
    void mapperWithSplitParameter() {
        assertEquals(singletonList(
                "@PartitionMapper(org.talend.sdk.component.runtime.visitor.visitor.ModelVisitorTest$MapperSplitParameter$Mapper)"),
                visit(MapperSplitParameter.class));
    }

    @Test
    void mapperInvalidSplitParameter() {
        assertThrows(IllegalArgumentException.class, () -> visit(MapperInvalidSplitParameter.class));
    }

    @Test
    void mapperInvalidSplitReturnType() {
        assertThrows(IllegalArgumentException.class, () -> visit(MapperInvalidSplitReturnType.class));
    }

    @Test
    void mapperInvalidAssessorParameter() {
        assertThrows(IllegalArgumentException.class, () -> visit(MapperInvalidAssessorParams.class));
    }

    @Test
    void mapperInvalidAssessorReturnType() {
        assertThrows(IllegalArgumentException.class, () -> visit(MapperInvalidAssessorReturnType.class));
    }

    @Test
    void processorAfterVariableOk() {
        assertEquals(singletonList("@Processor(" + ProcessorAfterVariableOk.Out.class.getName() + ")"),
                visit(ProcessorAfterVariableOk.class));
    }

    @Test
    void processorAfterVariableNokWrongContainerReturnType() {
        assertThrows(IllegalArgumentException.class,
                () -> visit(ProcessorAfterVariableNokWrongContainerReturnType.class));
    }

    @Test
    void processorAfterVariableNokWrongContainerReturnParameterizedDimension() {
        assertThrows(IllegalArgumentException.class,
                () -> visit(ProcessorAfterVariableNokWrongContainerReturnParameterizedDimension.class));
    }

    @Test
    void processorAfterVariableNokWrongContainerReturnParameterizedType() {
        assertThrows(IllegalArgumentException.class,
                () -> visit(ProcessorAfterVariableNokWrongContainerReturnParameterizedType.class));
    }

    @Test
    void processorAfterVariableNokMoreThanOneContainer() {
        assertThrows(IllegalArgumentException.class, () -> visit(ProcessorAfterVariableNokMoreThanOneContainer.class));
    }

    @Test
    void processorAfterVariableNokWrongParamCount() {
        assertThrows(IllegalArgumentException.class, () -> visit(ProcessorAfterVariableNokWrongParamCount.class));

    }

    @Test
    void processorAfterVariableNokWrongDeclaredType() {
        assertThrows(IllegalArgumentException.class, () -> visit(ProcessorAfterVariableNokWrongDeclaredType.class));
    }

    @Test
    void emitterAfterVariableOk() {
        assertEquals(singletonList("@Emitter(" + EmitterAfterVariableOk.In.class.getName() + ")"),
                visit(EmitterAfterVariableOk.class));
    }

    @Test
    void emitterAfterVariableNokWrongReturnType() {
        assertThrows(IllegalArgumentException.class, () -> visit(EmitterAfterVariableNokWrongReturnType.class));
    }

    @Test
    void emitterAfterVariableNokWrongDeclaredType() {
        assertThrows(IllegalArgumentException.class, () -> visit(EmitterAfterVariableNokWrongDeclaredType.class));
    }

    @Test
    void emitterAfterVariableNokWrongParamCount() {
        assertThrows(IllegalArgumentException.class, () -> visit(EmitterAfterVariableNokWrongParamCount.class));
    }

    @Test
    void mapperAfterVariableOk() {
        assertEquals(singletonList("@PartitionMapper(" + MapperAfterVariableOk.Mapper.class.getName() + ")"),
                visit(MapperAfterVariableOk.class));
    }

    @Test
    void mapperAfterVariableNokWrongReturnType() {
        assertThrows(IllegalArgumentException.class, () -> visit(MapperAfterVariableNokWrongReturnType.class));
    }

    @Test
    void mapperAfterVariableNokWrongDeclaredType() {
        assertThrows(IllegalArgumentException.class, () -> visit(MapperAfterVariableNokWrongDeclaredType.class));
    }

    @Test
    void mapperAfterVariableNokWrongParamType() {
        assertThrows(IllegalArgumentException.class, () -> visit(MapperAfterVariableNokWrongParamType.class));
    }

    @Test
    void standaloneNoRunMethod() {
        assertThrows(IllegalArgumentException.class, () -> visit(StandaloneNoRunMethod.class));
    }

    @Test
    void standaloneRunMethodWithArguments() {
        assertThrows(IllegalArgumentException.class, () -> visit(StandaloneRunMethodWithParams.class));
    }

    @Test
    void standaloneTwoRunMethods() {
        assertThrows(IllegalArgumentException.class, () -> visit(StandaloneTwoRunMethods.class));
    }

    @Test
    void standaloneAfterVariableOk() {
        assertEquals(singletonList("@DriverRunner(" + StandaloneAfterVariableOk.Standalone.class.getName() + ")"),
                visit(StandaloneAfterVariableOk.class));
    }

    @Test
    void standaloneAfterVariableNokWrongReturnType() {
        assertThrows(IllegalArgumentException.class, () -> visit(StandaloneAfterVariableNokWrongReturnType.class));
    }

    @Test
    void standaloneAfterVariableNokWrongDeclaredType() {
        assertThrows(IllegalArgumentException.class, () -> visit(StandaloneAfterVariableNokWrongDeclaredType.class));
    }

    @Test
    void standaloneAfterVariableNokWrongParamType() {
        assertThrows(IllegalArgumentException.class, () -> visit(StandaloneAfterVariableNokWrongParamCount.class));
    }

    private List<String> visit(final Class<?> type) {
        final ModelVisitor visitor = new ModelVisitor();
        final List<String> tracker = new ArrayList<>();
        Stream
                .of(type.getClasses())
                .sorted(Comparator.comparing(Class::getName))
                .forEach(nested -> visitor.visit(nested, new ModelListener() {

                    @Override
                    public void onPartitionMapper(final Class<?> type, final PartitionMapper partitionMapper) {
                        tracker.add("@PartitionMapper(" + type.getName() + ")");
                    }

                    @Override
                    public void onEmitter(final Class<?> type, final Emitter emitter) {
                        tracker.add("@Emitter(" + type.getName() + ")");
                    }

                    @Override
                    public void onProcessor(final Class<?> type, final Processor processor) {
                        tracker.add("@Processor(" + type.getName() + ")");
                    }

                    @Override
                    public void onDriverRunner(final Class<?> type, final DriverRunner runner) {
                        tracker.add("@DriverRunner(" + type.getName() + ")");
                    }
                }, true));
        return tracker;
    }

    public static class ValidIn {

        @Producer
        public ValidIn produces() {
            return this;
        }
    }

    public static class Registrar {

        @PartitionMapper(family = "comp", name = "Mapper")
        public static class Mapper {

            @Assessor
            public long get() {
                return 1;
            }

            @Split
            public Collection<Mapper> ins() {
                return emptyList();
            }

            @Emitter
            public In emit() {
                return null;
            }
        }

        @Emitter(family = "comp", name = "Input")
        public static class In {

            @Producer
            public In produces() {
                return this;
            }
        }

        @Processor(family = "comp", name = "Output")
        public static class Out {

            @ElementListener
            public void onNext(final In in) {
                // no-op
            }
        }

        @DriverRunner(family = "comp", name = "Standalone")
        public static class Standalone {

            @RunAtDriver
            public void run() {
                // no-op
            }
        }
    }

    public static class InfiniteMapperNoAssessor {

        @PartitionMapper(family = "comp", name = "Mapper", infinite = true)
        public static class Mapper {

            @Split
            public Collection<Mapper> ins() {
                return emptyList();
            }

            @Emitter
            public ValidIn emit() {
                return null;
            }
        }
    }

    public static class InfiniteMapperStoppable {

        @PartitionMapper(family = "comp", name = "Mapper", infinite = true, stoppable = true)
        public static class Mapper {

            @Split
            public Collection<Mapper> ins() {
                return emptyList();
            }

            @Emitter
            public ValidIn emit() {
                return null;
            }
        }
    }

    public static class MapperStoppable {

        @PartitionMapper(family = "comp", name = "Mapper", infinite = false, stoppable = true)
        public static class Mapper {

            @Assessor
            public long get() {
                return 1;
            }

            @Split
            public Collection<Mapper> ins() {
                return emptyList();
            }

            @Emitter
            public ValidIn emit() {
                return null;
            }
        }
    }

    public static class MapperNoAssessor {

        @PartitionMapper(family = "comp", name = "Mapper")
        public static class Mapper {

            @Split
            public Collection<Mapper> ins() {
                return emptyList();
            }

            @Emitter
            public ValidIn emit() {
                return null;
            }
        }
    }

    public static class InvalidComponent {

        @Emitter(family = "comp", name = "TwoIsWrong")
        @PartitionMapper(family = "comp", name = "Same")
        public static class Invalid extends ValidIn {
        }
    }

    public static class MapperNoSplit {

        @PartitionMapper(family = "comp", name = "Mapper")
        public static class Mapper {

            @Assessor
            public long get() {
                return 1;
            }

            @Emitter
            public ValidIn emit() {
                return null;
            }
        }
    }

    public static class MapperSplitParameter {

        @PartitionMapper(family = "comp", name = "Mapper")
        public static class Mapper {

            @Assessor
            public long get() {
                return 1;
            }

            @Split
            public Collection<Mapper> ins(@PartitionSize final int count) {
                return emptyList();
            }

            @Emitter
            public ValidIn emit() {
                return null;
            }
        }
    }

    public static class MapperInvalidSplitParameter {

        @PartitionMapper(family = "comp", name = "Mapper")
        public static class Mapper {

            @Assessor
            public long get() {
                return 1;
            }

            @Split
            public Collection<Mapper> ins(final int count) {
                return emptyList();
            }

            @Emitter
            public ValidIn emit() {
                return null;
            }
        }
    }

    public static class MapperInvalidSplitReturnType {

        @PartitionMapper(family = "comp", name = "Mapper")
        public static class Mapper {

            @Assessor
            public long get() {
                return 1;
            }

            @Split
            public Mapper ins(final int count) {
                return this;
            }

            @Emitter
            public ValidIn emit() {
                return null;
            }
        }
    }

    public static class MapperInvalidAssessorParams {

        @PartitionMapper(family = "comp", name = "Mapper")
        public static class Mapper {

            @Assessor
            public long get(final int whatCouldItBe) {
                return 1;
            }

            @Split
            public Collection<Mapper> ins() {
                return emptyList();
            }

            @Emitter
            public ValidIn emit() {
                return null;
            }
        }
    }

    public static class MapperInvalidAssessorReturnType {

        @PartitionMapper(family = "comp", name = "Mapper")
        public static class Mapper {

            @Assessor
            public Mapper get(final int whatCouldItBe) {
                return this;
            }

            @Split
            public Collection<Mapper> ins() {
                return emptyList();
            }

            @Emitter
            public ValidIn emit() {
                return null;
            }
        }
    }

    public static class MapperNoEmitter {

        @PartitionMapper(family = "comp", name = "Mapper")
        public static class Mapper {

            @Assessor
            public long get() {
                return 1;
            }

            @Split
            public Collection<Mapper> ins() {
                return emptyList();
            }
        }
    }

    public static class ProcessorNoListener {

        @Processor(family = "comp", name = "Output")
        public static class Out {
        }
    }

    public static class ProcessorBulk {

        @Processor(family = "comp", name = "Bulk")
        public static class Out {

            @AfterGroup
            public void commit(final Collection<Record> records) {
                // no-op
            }
        }
    }

    public static class EmitterNoProduces {

        @Emitter(family = "comp", name = "Input")
        public static class In {
        }
    }

    public static class ProcessorAfterVariableOk {

        @AfterVariable(value = "NAME", type = Integer.class)
        @Processor(family = "comp", name = "Output")
        public static class Out {

            @AfterVariableContainer
            public Map<String, Object> commit() {
                return Collections.emptyMap();
            }

            @AfterGroup
            public void commit(final Collection<Record> records) {
                // no-op
            }
        }
    }

    public static class ProcessorAfterVariableNokWrongContainerReturnType {

        @AfterVariable(value = "NAME", type = Integer.class)
        @Processor(family = "comp", name = "Output")
        public static class Out {

            @AfterVariableContainer
            public String commit() {
                return "It should be Map<String, Object>";
            }

            @AfterGroup
            public void commit(final Collection<Record> records) {
                // no-op
            }
        }
    }

    public static class ProcessorAfterVariableNokWrongContainerReturnParameterizedDimension {

        @AfterVariable(value = "NAME", type = Integer.class)
        @Processor(family = "comp", name = "Output")
        public static class Out {

            @AfterVariableContainer
            public Collection<String> commit() {
                return Collections.emptyList();
            }

            @AfterGroup
            public void commit(final Collection<Record> records) {
                // no-op
            }
        }
    }

    public static class ProcessorAfterVariableNokWrongContainerReturnParameterizedType {

        @Processor(family = "comp", name = "Output")
        public static class Out {

            @AfterVariableContainer
            public Map<String, String> commit() {
                return Collections.emptyMap();
            }

            @AfterGroup
            public void commit(final Collection<Record> records) {
                // no-op
            }
        }
    }

    public static class ProcessorAfterVariableNokMoreThanOneContainer {

        @Processor(family = "comp", name = "Output")
        public static class Out {

            @AfterVariableContainer
            public Map<String, Object> container1() {
                return Collections.emptyMap();
            }

            @AfterVariableContainer
            public Map<String, Object> container2() {
                return Collections.emptyMap();
            }

            @AfterGroup
            public void commit(final Collection<Record> records) {
                // no-op
            }
        }
    }

    public static class ProcessorAfterVariableNokWrongParamCount {

        @Processor(family = "comp", name = "Output")
        public static class Out {

            @AfterVariableContainer
            public Map<String, Object> commit(String param) {
                return Collections.emptyMap();
            }

            @AfterGroup
            public void commit(final Collection<Record> records) {
                // no-op
            }
        }
    }

    public static class ProcessorAfterVariableNokWrongDeclaredType {

        @AfterVariable(value = "Name", type = Mapper.class)
        @Processor(family = "comp", name = "Output")
        public static class Out {

            @AfterVariableContainer
            public Map<String, Object> container() {
                return Collections.emptyMap();
            }

            @AfterGroup
            public void commit(final Collection<Record> records) {
                // no-op
            }
        }
    }

    public static class EmitterAfterVariableOk {

        @AfterVariable(value = "NAME", type = Integer.class)
        @Emitter(family = "comp", name = "Input")
        public static class In {

            @AfterVariableContainer
            public Map<String, Object> commit() {
                return Collections.emptyMap();
            }

            @Producer
            public Record emit() {
                return null;
            }
        }
    }

    public static class EmitterAfterVariableNokWrongReturnType {

        @Emitter(family = "comp", name = "Input")
        public static class In {

            @AfterVariableContainer
            public Map<String, String> commit() {
                return Collections.emptyMap();
            }

            @Producer
            public Record emit() {
                return null;
            }
        }
    }

    public static class EmitterAfterVariableNokWrongParamCount {

        @Emitter(family = "comp", name = "Input")
        public static class In {

            @AfterVariableContainer
            public Map<String, Object> commit(String param) {
                return Collections.emptyMap();
            }

            @Producer
            public Record emit() {
                return null;
            }
        }
    }

    public static class EmitterAfterVariableNokWrongDeclaredType {

        @AfterVariable(value = "Name", type = Mapper.class)
        @Emitter(family = "comp", name = "Input")
        public static class In {

            @AfterVariableContainer
            public Map<String, Object> container() {
                return Collections.emptyMap();
            }

            @Producer
            public Record emit() {
                return null;
            }
        }
    }

    public static class MapperAfterVariableOk {

        @AfterVariable(value = "NAME", type = Integer.class)
        @PartitionMapper(family = "comp", name = "Mapper")
        public static class Mapper {

            @Assessor
            public long get() {
                return 1;
            }

            @Split
            public Collection<Mapper> ins() {
                return emptyList();
            }

            @Emitter
            public In emit() {
                return null;
            }

            @AfterVariableContainer
            public Map<String, Object> oz() {
                return Collections.emptyMap();
            }
        }
    }

    public static class MapperAfterVariableNokWrongReturnType {

        @PartitionMapper(family = "comp", name = "Mapper")
        public static class Mapper {

            @Assessor
            public long get() {
                return 1;
            }

            @Split
            public Collection<Mapper> ins() {
                return emptyList();
            }

            @Emitter
            public In emit() {
                return null;
            }

            @AfterVariableContainer
            public Map<String, Class<?>> oz() {
                return Collections.emptyMap();
            }
        }
    }

    public static class MapperAfterVariableNokWrongParamType {

        @PartitionMapper(family = "comp", name = "Mapper")
        public static class Mapper {

            @Assessor
            public long get() {
                return 1;
            }

            @Split
            public Collection<Mapper> ins() {
                return emptyList();
            }

            @Emitter
            public In emit() {
                return null;
            }

            @AfterVariableContainer
            public Map<String, Object> oz(String param) {
                return Collections.emptyMap();
            }
        }
    }

    public static class MapperAfterVariableNokWrongDeclaredType {

        @AfterVariable(value = "Name", type = Mapper.class)
        @PartitionMapper(family = "comp", name = "Mapper")
        public static class Mapper {

            @Assessor
            public long get() {
                return 1;
            }

            @Split
            public Collection<Mapper> ins() {
                return emptyList();
            }

            @Emitter
            public In emit() {
                return null;
            }

            @AfterVariableContainer
            public Map<String, Object> container() {
                return Collections.emptyMap();
            }
        }
    }

    public static class StandaloneNoRunMethod {

        @DriverRunner
        public static class Standalone {
        }
    }

    public static class StandaloneRunMethodWithParams {

        @DriverRunner(family = "comp", name = "standalone")
        public static class Standalone {

            @RunAtDriver
            public void run(Object argument) {
            }
        }
    }

    public static class StandaloneTwoRunMethods {

        @DriverRunner(family = "comp", name = "standalone")
        public static class Standalone {

            @RunAtDriver
            public void run(Object argument) {
            }

            @RunAtDriver
            public void run1(Object argument) {
            }
        }
    }

    public static class StandaloneAfterVariableOk {

        @AfterVariable(value = "NAME", type = Integer.class)
        @DriverRunner(family = "comp", name = "standalone")
        public static class Standalone {

            @AfterVariableContainer
            public Map<String, Object> commit() {
                return Collections.emptyMap();
            }

            @RunAtDriver
            public Record emit() {
                return null;
            }
        }
    }

    public static class StandaloneAfterVariableNokWrongReturnType {

        @DriverRunner(family = "comp", name = "standalone")
        public static class Standalone {

            @AfterVariableContainer
            public Map<String, String> commit() {
                return Collections.emptyMap();
            }

            @RunAtDriver
            public Record emit() {
                return null;
            }
        }
    }

    public static class StandaloneAfterVariableNokWrongDeclaredType {

        @AfterVariable(value = "Name", type = Mapper.class)
        @DriverRunner(family = "comp", name = "standalone")
        public static class Standalone {

            @AfterVariableContainer
            public Map<String, Object> container() {
                return Collections.emptyMap();
            }

            @RunAtDriver
            public Record emit() {
                return null;
            }
        }
    }

    public static class StandaloneAfterVariableNokWrongParamCount {

        @DriverRunner(family = "comp", name = "standalone")
        public static class Standalone {

            @AfterVariableContainer
            public Map<String, Object> commit(String param) {
                return Collections.emptyMap();
            }

            @RunAtDriver
            public Record emit() {
                return null;
            }
        }
    }
}
