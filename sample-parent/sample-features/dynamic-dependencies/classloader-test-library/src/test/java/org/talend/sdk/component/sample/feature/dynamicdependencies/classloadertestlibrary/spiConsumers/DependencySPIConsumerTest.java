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

class DependencySPIConsumerTest {

    @Test
    void testGetValue_withSPILoaded() {
        DependencySPIConsumer<String> consumer = new DependencySPIConsumer<>();
        String value = consumer.getValue();
        
        Assertions.assertNotNull(value);
        Assertions.assertEquals("Mock value from dependency", value);
    }

    @Test
    void testTransform_withSPILoaded() {
        DependencySPIConsumer<String> consumer = new DependencySPIConsumer<>();
        String transformed = consumer.transform(s -> s.toUpperCase());
        
        Assertions.assertNotNull(transformed);
        Assertions.assertEquals("MOCK VALUE FROM DEPENDENCY", transformed);
    }

    @Test
    void testTransform_withPrefixFunction() {
        DependencySPIConsumer<String> consumer = new DependencySPIConsumer<>();
        String transformed = consumer.transform(s -> "Prefix: " + s);
        
        Assertions.assertNotNull(transformed);
        Assertions.assertTrue(transformed.startsWith("Prefix: "));
        Assertions.assertTrue(transformed.contains("Mock value"));
    }

    @Test
    void testTransform_withIntegerReturn() {
        DependencySPIConsumer<Integer> consumer = new DependencySPIConsumer<>();
        Integer length = consumer.transform(String::length);
        
        Assertions.assertNotNull(length);
        Assertions.assertTrue(length > 0);
    }

    @Test
    void testGetSPIImpl_isPresent() {
        DependencySPIConsumer<String> consumer = new DependencySPIConsumer<>();
        
        Assertions.assertTrue(consumer.getSPIImpl().isPresent());
    }
}
