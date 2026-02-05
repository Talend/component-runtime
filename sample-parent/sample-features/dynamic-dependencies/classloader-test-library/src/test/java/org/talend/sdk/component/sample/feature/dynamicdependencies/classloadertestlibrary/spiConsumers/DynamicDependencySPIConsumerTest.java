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
package org.talend.sdk.component.sample.feature.dynamicdependencies.classloadertestlibrary.spiConsumers;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class DynamicDependencySPIConsumerTest {

    @Test
    void testGetValue_withNoSPIAvailable() {
        DynamicDependencySPIConsumer<String> consumer = new DynamicDependencySPIConsumer<>();
        String value = consumer.getValue();
        
        Assertions.assertNotNull(value);
        Assertions.assertEquals("[ERROR] StringsProviderSPIAsDynamicDependency not loaded!", value);
    }

    @Test
    void testTransform_withNoSPIAvailable() {
        DynamicDependencySPIConsumer<String> consumer = new DynamicDependencySPIConsumer<>();
        String transformed = consumer.transform(s -> s.toUpperCase());
        
        Assertions.assertNotNull(transformed);
        Assertions.assertTrue(transformed.startsWith("[ERROR]"));
    }

    @Test
    void testTransform_withIntegerReturn_noSPIAvailable() {
        DynamicDependencySPIConsumer<Integer> consumer = new DynamicDependencySPIConsumer<>();
        Integer length = consumer.transform(String::length);
        
        Assertions.assertNotNull(length);
        Assertions.assertTrue(length > 0);
    }

    @Test
    void testGetSPIImpl_isEmpty() {
        DynamicDependencySPIConsumer<String> consumer = new DynamicDependencySPIConsumer<>();
        
        Assertions.assertFalse(consumer.getSPIImpl().isPresent());
    }
}
