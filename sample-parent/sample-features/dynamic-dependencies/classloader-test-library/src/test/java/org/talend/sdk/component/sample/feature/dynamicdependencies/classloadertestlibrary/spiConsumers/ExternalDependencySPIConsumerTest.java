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

class ExternalDependencySPIConsumerTest {

    @Test
    void testGetValue_withNoSPIAvailable() {
        ExternalDependencySPIConsumer<String> consumer = new ExternalDependencySPIConsumer<>();
        String value = consumer.getValue();
        
        Assertions.assertNotNull(value);
        Assertions.assertEquals("[ERROR] StringProviderFromExternalSPI not loaded!", value);
    }

    @Test
    void testTransform_withNoSPIAvailable() {
        ExternalDependencySPIConsumer<String> consumer = new ExternalDependencySPIConsumer<>();
        String transformed = consumer.transform(s -> s.toLowerCase());
        
        Assertions.assertNotNull(transformed);
        Assertions.assertTrue(transformed.contains("[error]"));
    }

    @Test
    void testTransform_withBooleanReturn_noSPIAvailable() {
        ExternalDependencySPIConsumer<Boolean> consumer = new ExternalDependencySPIConsumer<>();
        Boolean containsError = consumer.transform(s -> s.contains("[ERROR]"));
        
        Assertions.assertNotNull(containsError);
        Assertions.assertTrue(containsError);
    }

    @Test
    void testGetSPIImpl_isEmpty() {
        ExternalDependencySPIConsumer<String> consumer = new ExternalDependencySPIConsumer<>();
        
        Assertions.assertFalse(consumer.getSPIImpl().isPresent());
    }
}
