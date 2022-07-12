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
package org.talend.sdk.component.runtime.manager.extension;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.component.FlowVariables;
import org.talend.sdk.component.api.component.FlowVariables.FlowVariable;
import org.talend.sdk.component.api.processor.Processor;

class FlowVariableMetadataEnricherTest {

    private static final String FLOW_VARIABLE_FIRST_NAME = "NAME1";

    private static final String FLOW_VARIABLE_SECOND_NAME = "NAME2";

    @Test
    void oneFlowVariableAnnotation() {
        Map<String, String> metas = invokeMetaEnricher(InputClassWithOneFlowVariable.class);
        Assertions.assertFalse(metas.isEmpty());
        Assertions.assertTrue(metas.containsKey(FlowVariableMetadataEnricher.META_KEY_FLOW_VARIABLE));
        Assertions
                .assertEquals("NAME1\\:java.lang.Integer\\:",
                        metas.get(FlowVariableMetadataEnricher.META_KEY_FLOW_VARIABLE));
    }

    @Test
    void twoFlowVariableAnnotation() {
        Map<String, String> metas = invokeMetaEnricher(InputClassWithTwoFlowVariable.class);
        Assertions.assertFalse(metas.isEmpty());
        Assertions.assertTrue(metas.containsKey(FlowVariableMetadataEnricher.META_KEY_FLOW_VARIABLE));
        Assertions
                .assertEquals("NAME1\\:java.lang.Integer\\:\\;NAME2\\:java.lang.String\\:",
                        metas.get(FlowVariableMetadataEnricher.META_KEY_FLOW_VARIABLE));
    }

    @Test
    void groupFlowVariablesAnnotation() {
        Map<String, String> metas = invokeMetaEnricher(InputClassWithFlowVariablesGroup.class);
        Assertions.assertFalse(metas.isEmpty());
        Assertions.assertTrue(metas.containsKey(FlowVariableMetadataEnricher.META_KEY_FLOW_VARIABLE));
        Assertions
                .assertEquals("NAME1\\:java.lang.Integer\\:\\;NAME2\\:java.lang.String\\:",
                        metas.get(FlowVariableMetadataEnricher.META_KEY_FLOW_VARIABLE));
    }

    @Test
    void illegalUseOfFlowVariableAnnotation() {
        Map<String, String> metas = invokeMetaEnricher(IllegalUseOfFlowVariableAnnotation.class);
        Assertions.assertTrue(metas.isEmpty());
    }

    @Test
    void emptyFlowVariables() {
        Map<String, String> metas = invokeMetaEnricher(EmptyFlowVariables.class);
        Assertions.assertTrue(metas.isEmpty());
    }

    private Map<String, String> invokeMetaEnricher(Class<?> clazz) {
        Annotation[] annotations = clazz.getAnnotations();
        return new FlowVariableMetadataEnricher().onComponent(InputClassWithOneFlowVariable.class, annotations);
    }

    @FlowVariable(value = FLOW_VARIABLE_FIRST_NAME, description = "", type = Integer.class)
    @Processor
    private static class InputClassWithOneFlowVariable {

    }

    @FlowVariable(value = FLOW_VARIABLE_FIRST_NAME, description = "", type = Integer.class)
    @FlowVariable(value = FLOW_VARIABLE_SECOND_NAME, description = "", type = String.class)
    @Processor
    private static class InputClassWithTwoFlowVariable {

    }

    @FlowVariables({ @FlowVariable(value = FLOW_VARIABLE_FIRST_NAME, description = "", type = Integer.class),
            @FlowVariable(value = FLOW_VARIABLE_SECOND_NAME, description = "", type = String.class) })
    @Processor
    private static class InputClassWithFlowVariablesGroup {

    }

    @FlowVariable(value = FLOW_VARIABLE_FIRST_NAME, description = "", type = Integer.class)
    private static class IllegalUseOfFlowVariableAnnotation {

    }

    @Processor
    private static class EmptyFlowVariables {
    }
}
