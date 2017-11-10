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
package org.talend.sdk.component.runtime.manager;

import java.util.Collection;
import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;
import org.talend.sdk.component.api.input.PartitionMapper;
import org.talend.sdk.component.runtime.manager.ComponentFamilyMeta.PartitionMapperMeta;

/**
 * Unit-test for {@link PartitionMapperMeta}
 */
public class PartitionMapperMetaTest {

    @Test
    public void testGetInputFlows() {
        ComponentFamilyMeta parent = new ComponentFamilyMeta("plugin", Collections.emptyList(), "default", "name");
        PartitionMapperMeta meta = new PartitionMapperMeta(parent, "name", "default", 1, TestPartitionMapper.class, null, null,
                null, true);
        Assert.assertTrue(meta.getInputFlows().isEmpty());
    }

    @Test
    public void testGetOutputFlows() {
        ComponentFamilyMeta parent = new ComponentFamilyMeta("plugin", Collections.emptyList(), "default", "name");
        PartitionMapperMeta meta = new PartitionMapperMeta(parent, "name", "default", 1, TestPartitionMapper.class, null, null,
                null, true);
        Collection<String> outputs = meta.getOutputFlows();
        Assert.assertEquals(1, outputs.size());
        Assert.assertTrue(outputs.contains("__default__"));
    }

    @PartitionMapper
    private static class TestPartitionMapper {

    }
}
