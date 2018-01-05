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
package org.talend.sdk.component.runtime.visitor.visitor;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.input.Assessor;
import org.talend.sdk.component.api.input.Emitter;
import org.talend.sdk.component.api.input.PartitionMapper;
import org.talend.sdk.component.api.input.PartitionSize;
import org.talend.sdk.component.api.input.Producer;
import org.talend.sdk.component.api.input.Split;
import org.talend.sdk.component.api.processor.ElementListener;
import org.talend.sdk.component.api.processor.Processor;
import org.talend.sdk.component.runtime.visitor.ModelListener;
import org.talend.sdk.component.runtime.visitor.ModelVisitor;

class ModelVisitorTest {

    @Test
    void valid() {
        assertEquals(asList("@Emitter(org.talend.sdk.component.runtime.visitor.visitor.ModelVisitorTest$Registrar$In)",
                "@PartitionMapper(org.talend.sdk.component.runtime.visitor.visitor.ModelVisitorTest$Registrar$Mapper)",
                "@Processor(org.talend.sdk.component.runtime.visitor.visitor.ModelVisitorTest$Registrar$Out)"),
                visit(Registrar.class));
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

    private List<String> visit(final Class<?> type) {
        final ModelVisitor visitor = new ModelVisitor();
        final List<String> tracker = new ArrayList<>();
        Stream.of(type.getClasses()).sorted(Comparator.comparing(Class::getName)).forEach(
                nested -> visitor.visit(nested, new ModelListener() {

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

    public static class EmitterNoProduces {

        @Emitter(family = "comp", name = "Input")
        public static class In {
        }
    }
}
