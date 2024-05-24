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
package org.talend.sdk.component.runtime.manager.extension;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.component.ReturnVariables;
import org.talend.sdk.component.api.component.ReturnVariables.ReturnVariable;
import org.talend.sdk.component.api.processor.Processor;

class ReturnVariableMetadataEnricherTest {

    private static final String RETURN_VARIABLE_FIRST_NAME = "NAME1";

    private static final String RETURN_VARIABLE_SECOND_NAME = "NAME2";

    @Test
    void oneReturnVariableAnnotationWithAfter() {
        Map<String, String> metas = invokeMetaEnricher(InputClassWithOneReturnVariable.class);
        Assertions.assertFalse(metas.isEmpty());
        Assertions.assertTrue(metas.containsKey(ReturnVariableMetadataEnricher.META_KEY_RETURN_VARIABLE));
        Assertions
                .assertEquals("NAME1\\:java.lang.Integer\\:AFTER\\:",
                        metas.get(ReturnVariableMetadataEnricher.META_KEY_RETURN_VARIABLE));
    }

    @Test
    void twoReturnVariableAnnotationWithAfterAndFlow() {
        Map<String, String> metas = invokeMetaEnricher(InputClassWithTwoReturnVariable.class);
        Assertions.assertFalse(metas.isEmpty());
        Assertions.assertTrue(metas.containsKey(ReturnVariableMetadataEnricher.META_KEY_RETURN_VARIABLE));
        Assertions
                .assertEquals("NAME1\\:java.lang.Integer\\:AFTER\\:\\;NAME2\\:java.lang.String\\:FLOW\\:",
                        metas.get(ReturnVariableMetadataEnricher.META_KEY_RETURN_VARIABLE));
    }

    @Test
    void groupReturnVariablesAnnotationWithAfterAndFlow() {
        Map<String, String> metas = invokeMetaEnricher(InputClassWithReturnVariablesGroup.class);
        Assertions.assertFalse(metas.isEmpty());
        Assertions.assertTrue(metas.containsKey(ReturnVariableMetadataEnricher.META_KEY_RETURN_VARIABLE));
        Assertions
                .assertEquals("NAME1\\:java.lang.Integer\\:AFTER\\:\\;NAME2\\:java.lang.String\\:FLOW\\:",
                        metas.get(ReturnVariableMetadataEnricher.META_KEY_RETURN_VARIABLE));
    }

    @Test
    void illegalUseOfReturnVariableAnnotation() {
        Map<String, String> metas = invokeMetaEnricher(IllegalUseOfReturnVariableAnnotation.class);
        Assertions.assertTrue(metas.isEmpty());
    }

    @Test
    void emptyReturnVariables() {
        Map<String, String> metas = invokeMetaEnricher(EmptyReturnVariables.class);
        Assertions.assertTrue(metas.isEmpty());
    }

    private Map<String, String> invokeMetaEnricher(Class<?> clazz) {
        Annotation[] annotations = clazz.getAnnotations();
        return new ReturnVariableMetadataEnricher().onComponent(InputClassWithOneReturnVariable.class, annotations);
    }

    @ReturnVariable(value = RETURN_VARIABLE_FIRST_NAME, description = "", type = Integer.class,
            availability = ReturnVariable.AVAILABILITY.AFTER)
    @Processor
    private static class InputClassWithOneReturnVariable {

    }

    @ReturnVariable(value = RETURN_VARIABLE_FIRST_NAME, description = "", type = Integer.class,
            availability = ReturnVariable.AVAILABILITY.AFTER)
    @ReturnVariable(value = RETURN_VARIABLE_SECOND_NAME, description = "", type = String.class,
            availability = ReturnVariable.AVAILABILITY.FLOW)
    @Processor
    private static class InputClassWithTwoReturnVariable {

    }

    @ReturnVariables({
            @ReturnVariable(value = RETURN_VARIABLE_FIRST_NAME, description = "", type = Integer.class,
                    availability = ReturnVariable.AVAILABILITY.AFTER),
            @ReturnVariable(value = RETURN_VARIABLE_SECOND_NAME, description = "", type = String.class,
                    availability = ReturnVariable.AVAILABILITY.FLOW) })
    @Processor
    private static class InputClassWithReturnVariablesGroup {

    }

    @ReturnVariable(value = RETURN_VARIABLE_FIRST_NAME, description = "", type = Integer.class)
    private static class IllegalUseOfReturnVariableAnnotation {

    }

    @Processor
    private static class EmptyReturnVariables {
    }
}
