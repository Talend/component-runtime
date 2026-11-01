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
package org.talend.sdk.component.tcketl.domain.connector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class ConnectorSummaryTest {

    @Test
    void missingNamesFallBackToTheTechnicalOnes() {
        final ConnectorSummary summary = new ConnectorSummary(TestConnectors.ref("Reader"), null, "", null, List.of(),
                ConnectorRole.INPUT, false, null);

        assertEquals("family", summary.familyDisplayName());
        assertEquals("Reader", summary.displayName());
        assertEquals("", summary.familyId());
        assertEquals("", summary.documentation());
        assertEquals("id-Reader", summary.id().value());
        assertEquals("family", summary.family());
        assertEquals("Reader", summary.name());
    }

    @Test
    void categoriesAreCopiedAndUnmodifiable() {
        final List<String> categories = new ArrayList<>(List.of("Cloud"));
        final ConnectorSummary summary = new ConnectorSummary(TestConnectors.ref("Reader"), "fid", "Family", "Reader",
                categories, ConnectorRole.INPUT, true, "doc");

        categories.add("Other");

        assertEquals(List.of("Cloud"), summary.categories());
        assertThrows(UnsupportedOperationException.class, () -> summary.categories().add("x"));
    }
}
