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
import org.talend.sdk.component.api.component.AfterVariables;
import org.talend.sdk.component.api.component.AfterVariables.AfterVariable;
import org.talend.sdk.component.api.processor.Processor;

class AfterVariableMetadataEnricherTest {

    private static final String AFTER_VARIABLE_FIRST_NAME = "NAME1";

    private static final String AFTER_VARIABLE_SECOND_NAME = "NAME2";

    @Test
    void oneAfterVariableAnnotation() {
        Map<String, String> metas = invokeMetaEnricher(InputClassWithOneAfterVariable.class);
        Assertions.assertFalse(metas.isEmpty());
        Assertions.assertTrue(metas.containsKey(AfterVariableMetadataEnricher.META_KEY_AFTER_VARIABLE));
        Assertions
                .assertEquals("NAME1\\:java.lang.Integer\\:",
                        metas.get(AfterVariableMetadataEnricher.META_KEY_AFTER_VARIABLE));
    }

    @Test
    void twoAfterVariableAnnotation() {
        Map<String, String> metas = invokeMetaEnricher(InputClassWithTwoAfterVariable.class);
        Assertions.assertFalse(metas.isEmpty());
        Assertions.assertTrue(metas.containsKey(AfterVariableMetadataEnricher.META_KEY_AFTER_VARIABLE));
        Assertions
                .assertEquals("NAME1\\:java.lang.Integer\\:\\;NAME2\\:java.lang.String\\:",
                        metas.get(AfterVariableMetadataEnricher.META_KEY_AFTER_VARIABLE));
    }

    @Test
    void groupAfterVariablesAnnotation() {
        Map<String, String> metas = invokeMetaEnricher(InputClassWithAfterVariablesGroup.class);
        Assertions.assertFalse(metas.isEmpty());
        Assertions.assertTrue(metas.containsKey(AfterVariableMetadataEnricher.META_KEY_AFTER_VARIABLE));
        Assertions
                .assertEquals("NAME1\\:java.lang.Integer\\:\\;NAME2\\:java.lang.String\\:",
                        metas.get(AfterVariableMetadataEnricher.META_KEY_AFTER_VARIABLE));
    }

    @Test
    void illegalUseOfAfterVariableAnnotation() {
        Map<String, String> metas = invokeMetaEnricher(IllegalUseOfAfterVariableAnnotation.class);
        Assertions.assertTrue(metas.isEmpty());
    }

    @Test
    void emptyAfterVariables() {
        Map<String, String> metas = invokeMetaEnricher(EmptyAfterVariables.class);
        Assertions.assertTrue(metas.isEmpty());
    }

    private Map<String, String> invokeMetaEnricher(Class<?> clazz) {
        Annotation[] annotations = clazz.getAnnotations();
        return new AfterVariableMetadataEnricher().onComponent(InputClassWithOneAfterVariable.class, annotations);
    }

    @AfterVariable(value = AFTER_VARIABLE_FIRST_NAME, description = "", type = Integer.class)
    @Processor
    private static class InputClassWithOneAfterVariable {

    }

    @AfterVariable(value = AFTER_VARIABLE_FIRST_NAME, description = "", type = Integer.class)
    @AfterVariable(value = AFTER_VARIABLE_SECOND_NAME, description = "", type = String.class)
    @Processor
    private static class InputClassWithTwoAfterVariable {

    }

    @AfterVariables({ @AfterVariable(value = AFTER_VARIABLE_FIRST_NAME, description = "", type = Integer.class),
            @AfterVariable(value = AFTER_VARIABLE_SECOND_NAME, description = "", type = String.class) })
    @Processor
    private static class InputClassWithAfterVariablesGroup {

    }

    @AfterVariable(value = AFTER_VARIABLE_FIRST_NAME, description = "", type = Integer.class)
    private static class IllegalUseOfAfterVariableAnnotation {

    }

    @Processor
    private static class EmptyAfterVariables {
    }
}
