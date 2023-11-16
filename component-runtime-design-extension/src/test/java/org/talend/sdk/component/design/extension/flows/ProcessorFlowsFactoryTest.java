/**
 * Copyright (C) 2006-2023 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.design.extension.flows;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.processor.AfterGroup;
import org.talend.sdk.component.api.processor.ElementListener;
import org.talend.sdk.component.api.processor.Input;
import org.talend.sdk.component.api.processor.Output;
import org.talend.sdk.component.api.processor.OutputEmitter;
import org.talend.sdk.component.api.processor.Processor;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.runtime.manager.ComponentFamilyMeta.ProcessorMeta;

import lombok.Data;

/**
 * Unit-tests for {@link ProcessorMeta}
 */
class ProcessorFlowsFactoryTest {

    @Test
    void testGetInputFlows() {
        final ProcessorFlowsFactory factory = new ProcessorFlowsFactory(TestProcessor.class);
        final Collection<String> inputs = factory.getInputFlows();
        assertEquals(2, inputs.size());
        assertTrue(inputs.contains("__default__"));
        assertTrue(inputs.contains("REJECT"));
    }

    @Test
    void testGetOutputFlows() {
        final ProcessorFlowsFactory factory = new ProcessorFlowsFactory(TestProcessor.class);
        final Collection<String> outputs = factory.getOutputFlows();
        assertEquals(2, outputs.size());
        assertTrue(outputs.contains("__default__"));
        assertTrue(outputs.contains("OUTPUT"));
    }

    @Test
    void testGetMergeOutputFlows() {
        final ProcessorFlowsFactory factory = new ProcessorFlowsFactory(TestProcessorMergeOutputs.class);
        final Collection<String> outputs = factory.getOutputFlows();
        assertEquals(2, outputs.size());
        assertTrue(outputs.contains("__default__"));
        assertTrue(outputs.contains("OUTPUT"));
    }

    @Test
    void testProcessorBulk() {
        final ProcessorFlowsFactory factory = new ProcessorFlowsFactory(TestProcessorBulk.class);
        assertTrue(factory.getOutputFlows().isEmpty());
        assertEquals(singletonList("__default__"), factory.getInputFlows());
    }

    @Test
    void testProcessorBulkWithoutOutput() {
        final ProcessorFlowsFactory factory = new ProcessorFlowsFactory(TestProcessorBulkWithOutput.class);
        assertEquals(singletonList("__default__"), factory.getInputFlows());
        assertEquals(singletonList("OUTPUT"), factory.getOutputFlows());
    }

    @Processor
    private static class TestProcessor {

        @ElementListener
        public void map(@Input final InputData1 input1, @Input("REJECT") final InputData2 input2,
                @Output final OutputEmitter<OutputData1> output1,
                @Output("OUTPUT") final OutputEmitter<OutputData2> output2) {
            // no-op
        }

    }

    @Processor
    private static class TestProcessorMergeOutputs {

        @ElementListener
        public void map(@Input final InputData1 input1, @Output final OutputEmitter<OutputData1> output1) {
            // no-op
        }

        @AfterGroup
        public void afterGroup(@Output final OutputEmitter<OutputData1> output1,
                @Output("OUTPUT") final OutputEmitter<OutputData2> output2) {

        }

    }

    @Processor
    private static class TestProcessorBulk {

        @AfterGroup
        public void afterGroup(final Collection<Record> records) {

        }
    }

    @Processor
    private static class TestProcessorBulkWithOutput {

        @AfterGroup
        public void afterGroup(final Collection<Record> records,
                @Output("OUTPUT") final OutputEmitter<OutputData1> output) {

        }
    }

    @Data
    private static class InputData1 {

        private String value;
    }

    @Data
    private static class InputData2 {

        private int value;
    }

    @Data
    private static class OutputData1 {

        private String name;

        private int age;
    }

    @Data
    private static class OutputData2 {

        private String error;

        private int id;
    }
}
