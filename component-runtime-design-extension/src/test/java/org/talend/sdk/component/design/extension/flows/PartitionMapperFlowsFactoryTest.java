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

import java.util.Collection;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Unit-test for {@link PartitionMapperFlowsFactory}
 */
class PartitionMapperFlowsFactoryTest {

    @Test
    void testGetInputFlows() {
        PartitionMapperFlowsFactory factory = new PartitionMapperFlowsFactory();
        Assertions.assertTrue(factory.getInputFlows().isEmpty());
    }

    @Test
    void testGetOutputFlows() {
        PartitionMapperFlowsFactory factory = new PartitionMapperFlowsFactory();
        Collection<String> outputs = factory.getOutputFlows();
        Assertions.assertEquals(1, outputs.size());
        Assertions.assertTrue(outputs.contains("__default__"));
    }

}
