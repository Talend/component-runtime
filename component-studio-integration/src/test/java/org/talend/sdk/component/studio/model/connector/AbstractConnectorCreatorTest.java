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
package org.talend.sdk.component.studio.model.connector;

import org.junit.Assert;
import org.junit.Test;
import org.talend.core.model.process.EConnectionType;

/**
 * Unit-tests for {@link AbstractConnectorCreator}
 */
public class AbstractConnectorCreatorTest {

    private static final String DEFAULT = "__default__";

    @Test
    public void testGetTypeDefault() {
        EConnectionType expectedType = EConnectionType.FLOW_MAIN;
        EConnectionType actualType = AbstractConnectorCreator.getType(DEFAULT);
        Assert.assertEquals(expectedType, actualType);
    }

    @Test
    public void testGetTypeMain() {
        EConnectionType expectedType = EConnectionType.FLOW_MAIN;
        EConnectionType actualType = AbstractConnectorCreator.getType("Main");
        Assert.assertEquals(expectedType, actualType);
    }

    @Test
    public void testGetTypeReject() {
        EConnectionType expectedType = EConnectionType.REJECT;
        EConnectionType actualType = AbstractConnectorCreator.getType("reject");
        Assert.assertEquals(expectedType, actualType);
    }

    @Test
    public void testGetTypeRejectUpperCase() {
        EConnectionType expectedType = EConnectionType.REJECT;
        EConnectionType actualType = AbstractConnectorCreator.getType("REJECT");
        Assert.assertEquals(expectedType, actualType);
    }

    @Test
    public void testGetNameDefault() {
        String expectedName = "Main";
        String actualName = AbstractConnectorCreator.getName(DEFAULT);
        Assert.assertEquals(expectedName, actualName);
    }

    @Test
    public void testGetNameAny() {
        String expectedName = "Any";
        String actualName = AbstractConnectorCreator.getName("Any");
        Assert.assertEquals(expectedName, actualName);
    }
}
