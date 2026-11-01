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
package org.talend.sdk.component.tcketl.app.palette;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.talend.sdk.component.tcketl.app.testing.Connectors.summary;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.talend.sdk.component.tcketl.domain.connector.ConnectorRole;
import org.talend.sdk.component.tcketl.domain.connector.ConnectorSummary;

class PaletteTest {

    private static final ConnectorRole INPUT = ConnectorRole.INPUT;

    private static final List<ConnectorSummary> CONNECTORS = List
            .of(summary("azure", "Azure Storage", "Blob Input", List.of("Cloud/Azure"), INPUT),
                    summary("azure", "Azure Storage", "Queue Output", List.of("Cloud/Azure"), INPUT),
                    summary("files", "Local files", "CSV Reader", List.of("File/Input"), INPUT),
                    summary("db", "Databases", "SQL Reader", List.of("Database"), INPUT));

    @Test
    void groupsByFamilyAndSortsEverythingIgnoringTheCase() {
        final Palette palette = Palette
                .of(List
                        .of(summary("z", "zeta", "cherry", List.of(), INPUT),
                                summary("b", "Beta", "banana", List.of(), INPUT),
                                summary("a", "alpha", "Banana", List.of(), INPUT),
                                summary("b", "Beta", "Apple", List.of(), INPUT),
                                summary("b", "Beta", "cherry", List.of(), INPUT)));

        assertEquals(List.of("alpha", "Beta", "zeta"),
                palette.families().stream().map(PaletteFamily::displayName).toList());
        assertEquals(List.of("Apple", "banana", "cherry"),
                palette.families().get(1).connectors().stream().map(ConnectorSummary::displayName).toList());
        assertEquals(5, palette.size());
    }

    @ParameterizedTest(name = "\"{0}\" finds {1}")
    @MethodSource("searches")
    void keepsTheConnectorsThatMatchEveryWordOfTheQuery(final String query, final List<String> expected) {
        assertEquals(expected, names(Palette.of(CONNECTORS).filter(query)));
    }

    static Stream<Arguments> searches() {
        return Stream
                .of(Arguments.of("blob", List.of("Blob Input")),
                        Arguments.of("QUEUE", List.of("Queue Output")),
                        Arguments.of("storage", List.of("Blob Input", "Queue Output")),
                        Arguments.of("database", List.of("SQL Reader")),
                        Arguments.of("storage blob", List.of("Blob Input")),
                        Arguments.of("  blob   input ", List.of("Blob Input")),
                        Arguments.of("blob reader", List.of()),
                        Arguments.of("nothing", List.of()));
    }

    @Test
    void aBlankQueryKeepsEverything() {
        final Palette palette = Palette.of(CONNECTORS);
        assertSame(palette, palette.filter("   "));
    }

    private static List<String> names(final Palette palette) {
        return palette
                .families()
                .stream()
                .flatMap(family -> family.connectors().stream())
                .map(ConnectorSummary::displayName)
                .collect(Collectors.toList());
    }
}
