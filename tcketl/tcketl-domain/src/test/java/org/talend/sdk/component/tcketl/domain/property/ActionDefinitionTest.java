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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

class ActionDefinitionTest {

    @Test
    void parametersFollowTheDeclaredIndexAndSkipNestedProperties() {
        final ActionDefinition action = new ActionDefinition("fam", ActionTypes.UPDATE, "refresh", "Refresh",
                List
                        .of(parameter("second", 1), nested("first.child"), parameter("first", 0),
                                nested("second.child")));

        assertEquals(List.of("first", "second"),
                action.parameters().stream().map(PropertyDefinition::path).collect(Collectors.toList()));
        assertEquals(4, action.properties().size());
    }

    @Test
    void aBlankNameIsRefused() {
        assertThrows(IllegalArgumentException.class,
                () -> new ActionDefinition("fam", ActionTypes.UPDATE, " ", "", List.of()));
    }

    private static PropertyDefinition parameter(final String path, final int index) {
        return PropertyDefinition.builder(path, PropertyType.STRING).parameterIndex(index).build();
    }

    private static PropertyDefinition nested(final String path) {
        return PropertyDefinition.builder(path, PropertyType.STRING).build();
    }
}
