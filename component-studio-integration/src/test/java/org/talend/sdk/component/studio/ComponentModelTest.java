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

    @Test
    public void testGetLongName() {
        String expectedName = "XML Input";

        ComponentId id = new ComponentId("id", "plugin", "XML", "XMLInput");
        ComponentIndex idx = new ComponentIndex(id, "XML Input", null, null, 1, Arrays.asList("Local", "File"), null);

        ComponentModel componentModel = new ComponentModel(idx);
        Assert.assertEquals(expectedName, componentModel.getLongName());
    }

}
