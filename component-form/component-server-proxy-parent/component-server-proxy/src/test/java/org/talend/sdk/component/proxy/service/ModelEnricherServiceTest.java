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
package org.talend.sdk.component.proxy.service;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.enterprise.inject.spi.CDI;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.proxy.test.WithServer;
import org.talend.sdk.component.server.front.model.ActionReference;
import org.talend.sdk.component.server.front.model.ConfigTypeNode;
import org.talend.sdk.component.server.front.model.PropertyValidation;
import org.talend.sdk.component.server.front.model.SimplePropertyDefinition;

@WithServer
class ModelEnricherServiceTest {

    @Test
    void convertConfig() {
        final ModelEnricherService modelEnricherService = CDI.current().select(ModelEnricherService.class).get();
        final ConfigTypeNode configTypeNode = newConfig(newProperty());
        final ConfigTypeNode enriched = modelEnricherService.enrich(configTypeNode, "en");

        assertEquals(configTypeNode.getId(), enriched.getId());
        assertEquals(configTypeNode.getVersion(), enriched.getVersion());
        assertEquals(configTypeNode.getConfigurationType(), enriched.getConfigurationType());
        assertEquals(configTypeNode.getName(), enriched.getName());
        assertEquals(configTypeNode.getDisplayName(), enriched.getDisplayName());
        assertEquals(configTypeNode.getEdges(), enriched.getEdges());
        assertEquals(configTypeNode.getParentId(), enriched.getParentId());
        assertEquals(2, enriched.getProperties().size());

        final SimplePropertyDefinition def = enriched
                .getProperties()
                .stream()
                .filter(it -> it.getPath().equals("_datasetMetadata.name"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No property for _datasetMetadata.name"));
        assertEquals("name", def.getName());
        assertEquals("STRING", def.getType());
        assertTrue(def.getValidation().getRequired());
        assertEquals("The Display Name From RB", def.getDisplayName());
        assertEquals("The Placeholder From Resource Bundle", def.getPlaceholder());
    }

    private ConfigTypeNode newConfig(final SimplePropertyDefinition... props) {
        return new ConfigTypeNode("a", 3, "b", "test", "Test", "The Test Config", emptySet(), asList(props),
                singletonList(new ActionReference("test", "cb", "test", emptyList())));
    }

    private SimplePropertyDefinition newProperty() {
        return new SimplePropertyDefinition("foo.bar", "bar", "The Bar", "STRING", "set",
                new PropertyValidation(false, null, null, null, null, null, null, false, null, null), emptyMap(), null,
                null);
    }
}
