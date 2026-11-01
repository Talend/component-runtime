/**
 * Copyright (C) 2006-2026 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.tcketl.domain.property;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class PropertyDefinitionTest {

    @Test
    void nameDefaultsToTheLastPathSegment() {
        assertEquals("login", PropertyDefinition.builder("config.dataset.login", PropertyType.STRING).build().name());
        assertEquals("fields[]", PropertyDefinition.builder("config.fields[]", PropertyType.STRING).build().name());
        assertEquals("config", PropertyDefinition.builder("config", PropertyType.OBJECT).build().name());
    }

    @Test
    void displayNameFallsBackToTheName() {
        assertEquals("login", PropertyDefinition.builder("a.login", PropertyType.STRING).build().displayName());
        assertEquals("Login",
                PropertyDefinition.builder("a.login", PropertyType.STRING).displayName("Login").build().displayName());
    }

    @Test
    void proposalLabelsKeepTheServerOrder() {
        final Map<String, String> labels = new LinkedHashMap<>();
        labels.put("ZULU", "Zulu");
        labels.put("ALPHA", "Alpha");
        labels.put("MIKE", "Mike");

        final PropertyDefinition definition =
                PropertyDefinition.builder("mode", PropertyType.ENUM).proposalLabels(labels).build();

        assertEquals(List.of("ZULU", "ALPHA", "MIKE"), new ArrayList<>(definition.proposalLabels().keySet()));
    }

    @Test
    void metadataAndActionsAreCopiedAndUnmodifiable() {
        final Map<String, String> metadata = new LinkedHashMap<>(Map.of("a", "1"));
        final PropertyDefinition definition = PropertyDefinition
                .builder("p", PropertyType.STRING)
                .metadata(metadata)
                .action(new ActionBinding(ActionTypes.HEALTHCHECK, "check"))
                .build();

        metadata.put("b", "2");

        assertEquals(Map.of("a", "1"), definition.metadata());
        assertThrows(UnsupportedOperationException.class, () -> definition.metadata().put("c", "3"));
        assertThrows(UnsupportedOperationException.class, () -> definition.actions().clear());
    }

    @Test
    void actionIsLookedUpByType() {
        final ActionBinding update = new ActionBinding(ActionTypes.UPDATE, "refresh");
        final PropertyDefinition definition = PropertyDefinition
                .builder("p", PropertyType.OBJECT)
                .action(new ActionBinding(ActionTypes.HEALTHCHECK, "check"))
                .action(update)
                .build();

        assertEquals(update, definition.action(ActionTypes.UPDATE).orElseThrow());
        assertTrue(definition.action(ActionTypes.SUGGESTIONS).isEmpty());
    }

    @Test
    void anIndexBelowZeroMeansNotAParameter() {
        final PropertyDefinition.Builder builder = PropertyDefinition.builder("p", PropertyType.OBJECT);

        assertFalse(builder.build().isParameter());
        assertFalse(builder.parameterIndex(-7).build().isParameter());
        assertEquals(PropertyDefinition.NOT_A_PARAMETER, builder.build().parameterIndex());
        assertTrue(builder.parameterIndex(0).build().isParameter());
    }

    @Test
    void aBlankPathIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> PropertyDefinition.builder(" ", PropertyType.STRING));
    }
}
