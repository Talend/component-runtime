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
package org.talend.sdk.component.studio;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;
import org.talend.sdk.component.server.front.model.ComponentId;
import org.talend.sdk.component.server.front.model.ComponentIndex;


/**
 * Unit-tests for {@link ComponentModel}
 */
public class ComponentModelTest {
    
    @Test
    public void testGetOriginalFamilyName() {
        String expectedFamilyName = "Local/XML|File/XML";
        
        ComponentId id = new ComponentId("id", "plugin", "XML", "XMLInput");
        ComponentIndex idx = new ComponentIndex(id, "XML Input", null, null, 1, Arrays.asList("Local", "File"), null);
        
        ComponentModel componentModel = new ComponentModel(idx);
        Assert.assertEquals(expectedFamilyName, componentModel.getOriginalFamilyName());
    }

}
