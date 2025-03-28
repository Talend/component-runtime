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
package org.talend.sdk.component.runtime.manager.extension;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.meta.ConditionalOutput;
import org.talend.sdk.component.api.processor.Processor;

class ConditionalOutputMetadataEnricherTest {

    @Test
    void oneAnnotation() {
        Map<String, String> metas = invokeMetaEnricher(OutputClass1.class);
        Assertions.assertFalse(metas.isEmpty());
        Assertions.assertTrue(metas.containsKey(ConditionalOutputMetadataEnricher.META_KEY_RETURN_VARIABLE));
        Assertions
                .assertEquals("output1",
                        metas.get(ConditionalOutputMetadataEnricher.META_KEY_RETURN_VARIABLE));
    }

    @Test
    void oneAnnotation2() {
        Map<String, String> metas = invokeMetaEnricher(OutputClass2.class);
        Assertions.assertFalse(metas.isEmpty());
        Assertions.assertTrue(metas.containsKey(ConditionalOutputMetadataEnricher.META_KEY_RETURN_VARIABLE));
        Assertions
                .assertEquals("output2",
                        metas.get(ConditionalOutputMetadataEnricher.META_KEY_RETURN_VARIABLE));
    }

    @Test
    void empty() {
        Map<String, String> metas = invokeMetaEnricher(EmptyOutputClass.class);
        Assertions.assertTrue(metas.isEmpty());
    }

    private Map<String, String> invokeMetaEnricher(Class<?> clazz) {
        Annotation[] annotations = clazz.getAnnotations();
        return new ConditionalOutputMetadataEnricher().onComponent(OutputClass1.class, annotations);
    }

    @ConditionalOutput("output1")
    @Processor(name = "test1")
    private static class OutputClass1 {

    }

    @ConditionalOutput("output2")
    @Processor(name = "test2")
    private static class OutputClass2 {

    }

    @Processor(name = "test2")
    private static class EmptyOutputClass {

    }
}
