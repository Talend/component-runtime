// ============================================================================
//
// Copyright (C) 2006-2017 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
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
