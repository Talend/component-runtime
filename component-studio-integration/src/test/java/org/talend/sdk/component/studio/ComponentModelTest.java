/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.talend.core.model.general.ModuleNeeded;
import org.talend.core.model.process.INodeConnector;
import org.talend.core.model.process.INodeReturn;
import org.talend.core.model.temp.ECodePart;
import org.talend.sdk.component.server.front.model.ComponentDetail;
import org.talend.sdk.component.server.front.model.ComponentId;
import org.talend.sdk.component.server.front.model.ComponentIndex;

/**
 * Unit-tests for {@link ComponentModel}
 */
class ComponentModelTest {

    @Test
    void getModuleNeeded() {
        final ComponentId id = new ComponentId("id", "family", "plugin", "group:plugin:1", "XML", "XMLInput");
        final ComponentIndex idx =
                new ComponentIndex(id, "XML Input", null, null, null, 1, Arrays.asList("Local", "File"), null);
        final ComponentDetail detail =
                new ComponentDetail(id, "XML Input", null, "Processor", 1, null, null, null, null, null);
        final ComponentModel componentModel = new ComponentModel(idx, detail);
        final List<ModuleNeeded> modulesNeeded = componentModel.getModulesNeeded();
        assertEquals(18, modulesNeeded.size());
        // just assert a few
        assertTrue(modulesNeeded.stream().anyMatch(m -> m.getModuleName().startsWith("component-runtime-manager")));
        assertTrue(modulesNeeded.stream().anyMatch(m -> m.getModuleName().startsWith("component-runtime-impl")));
        assertTrue(modulesNeeded.stream().anyMatch(m -> m.getModuleName().startsWith("component-runtime-di")));
        assertTrue(modulesNeeded.stream().anyMatch(m -> m.getModuleName().startsWith("xbean-reflect")));
        assertTrue(modulesNeeded.stream().anyMatch(m -> "plugin-1.jar".equals(m.getModuleName())));
    }

    @Test
    void testGetOriginalFamilyName() {
        String expectedFamilyName = "Local/XML|File/XML";

        ComponentId id = new ComponentId("id", "family", "plugin", "plugin", "XML", "XMLInput");
        ComponentIndex idx =
                new ComponentIndex(id, "XML Input", null, null, null, 1, Arrays.asList("Local", "File"), null);
        ComponentDetail detail =
                new ComponentDetail(id, "XML Input", null, "Processor", 1, null, null, null, null, null);
        ComponentModel componentModel = new ComponentModel(idx, detail);

        assertEquals(expectedFamilyName, componentModel.getOriginalFamilyName());
    }

    @Test
    void testGetLongName() {
        String expectedName = "XML Input";

        ComponentId id = new ComponentId("id", "family", "plugin", "plugin", "XML", "XMLInput");
        ComponentIndex idx =
                new ComponentIndex(id, "XML Input", null, null, null, 1, Arrays.asList("Local", "File"), null);
        ComponentDetail detail =
                new ComponentDetail(id, "XML Input", null, "Processor", 1, null, null, null, null, null);
        ComponentModel componentModel = new ComponentModel(idx, detail);

        assertEquals(expectedName, componentModel.getLongName());
    }

    @Disabled("Cannot be tested as CorePlugin is null")
    @Test
    void testCreateConnectors() {

        ComponentId id = new ComponentId("id", "family", "plugin", "plugin", "XML", "XMLInput");
        ComponentIndex idx =
                new ComponentIndex(id, "XML Input", null, null, null, 1, Arrays.asList("Local", "File"), null);
        ComponentDetail detail =
                new ComponentDetail(id, "XML Input", null, "Processor", 1, null, null, null, null, null);
        ComponentModel componentModel = new ComponentModel(idx, detail);

        List<? extends INodeConnector> connectors = componentModel.createConnectors(null);
        assertEquals(22, connectors.size());
    }

    @Test
    void testCreateReturns() {

        ComponentId id = new ComponentId("id", "family", "plugin", "plugin", "XML", "XMLInput");
        ComponentIndex idx =
                new ComponentIndex(id, "XML Input", null, null, null, 1, Arrays.asList("Local", "File"), null);
        ComponentDetail detail =
                new ComponentDetail(id, "XML Input", null, "Processor", 1, null, null, null, null, null);
        ComponentModel componentModel = new ComponentModel(idx, detail);

        List<? extends INodeReturn> returnVariables = componentModel.createReturns(null);
        assertEquals(2, returnVariables.size());
        INodeReturn errorMessage = returnVariables.get(0);
        assertEquals("Error Message", errorMessage.getDisplayName());
        assertEquals("!!!NodeReturn.Availability.AFTER!!!", errorMessage.getAvailability());
        assertEquals("String", errorMessage.getType());
        INodeReturn numberLines = returnVariables.get(1);
        assertEquals("Number of line", numberLines.getDisplayName());
        assertEquals("!!!NodeReturn.Availability.AFTER!!!", numberLines.getAvailability());
        assertEquals("int | Integer", numberLines.getType());
    }

    @Test
    void testGetAvailableProcessorCodeParts() {

        ComponentId id = new ComponentId("id", "family", "plugin", "plugin", "XML", "XMLInput");
        ComponentIndex idx =
                new ComponentIndex(id, "XML Input", null, null, null, 1, Arrays.asList("Local", "File"), null);
        ComponentDetail detail =
                new ComponentDetail(id, "XML Input", null, "Processor", 1, null, null, null, null, null);
        ComponentModel componentModel = new ComponentModel(idx, detail);

        List<ECodePart> codeParts = componentModel.getAvailableCodeParts();
        assertEquals(3, codeParts.size());
        assertTrue(codeParts.contains(ECodePart.BEGIN));
        assertTrue(codeParts.contains(ECodePart.MAIN));
        assertTrue(codeParts.contains(ECodePart.FINALLY));
    }

    @Test
    void testGetAvailableInputCodeParts() {

        ComponentId id = new ComponentId("id", "family", "plugin", "plugin", "XML", "XMLInput");
        ComponentIndex idx =
                new ComponentIndex(id, "XML Input", null, null, null, 1, Arrays.asList("Local", "File"), null);
        ComponentDetail detail = new ComponentDetail(id, "XML Input", null, "Input", 1, null, null, null, null, null);
        ComponentModel componentModel = new ComponentModel(idx, detail);

        List<ECodePart> codeParts = componentModel.getAvailableCodeParts();
        assertEquals(3, codeParts.size());
        assertTrue(codeParts.contains(ECodePart.BEGIN));
        assertTrue(codeParts.contains(ECodePart.END));
        assertTrue(codeParts.contains(ECodePart.FINALLY));
    }
}
