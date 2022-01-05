/**
 * Copyright (C) 2006-2022 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.runtime.manager;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Method;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.processor.ElementListener;
import org.talend.sdk.component.api.processor.Input;
import org.talend.sdk.component.api.processor.Output;
import org.talend.sdk.component.api.processor.OutputEmitter;
import org.talend.sdk.component.api.processor.Processor;
import org.talend.sdk.component.runtime.manager.ComponentFamilyMeta.ProcessorMeta;

import lombok.Data;

/**
 * Unit-tests for {@link ProcessorMeta}
 */
class ProcessorMetaTest {

    @Test
    void testGetListener() {
        final ComponentFamilyMeta parent = new ComponentFamilyMeta("plugin", emptyList(), "default", "name", "");
        final ProcessorMeta meta = new ProcessorMeta(parent, "name", "default", 1, TestProcessor.class, null, null,
                null, true, Collections.emptyMap());
        final Method listener = meta.getListener();
        assertEquals("map", listener.getName());
        assertEquals(4, listener.getParameterCount());
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
