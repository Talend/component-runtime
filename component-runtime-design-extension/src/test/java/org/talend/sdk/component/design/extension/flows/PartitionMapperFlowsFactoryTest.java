/**
 * Copyright (C) 2006-2017 Talend Inc. - www.talend.com
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

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit-test for {@link PartitionMapperFlowsFactory}
 */
public class PartitionMapperFlowsFactoryTest {

    @Test
    public void testGetInputFlows() {
        PartitionMapperFlowsFactory factory = new PartitionMapperFlowsFactory();
        Assert.assertTrue(factory.getInputFlows().isEmpty());
    }

    @Test
    public void testGetOutputFlows() {
        PartitionMapperFlowsFactory factory = new PartitionMapperFlowsFactory();
        Collection<String> outputs = factory.getOutputFlows();
        Assert.assertEquals(1, outputs.size());
        Assert.assertTrue(outputs.contains("__default__"));
    }

}
