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
package org.talend.sdk.component.tcketl.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.tcketl.domain.connector.ConnectorDefinition;
import org.talend.sdk.component.tcketl.domain.connector.ConnectorId;
import org.talend.sdk.component.tcketl.domain.connector.ConnectorRef;
import org.talend.sdk.component.tcketl.domain.connector.ConnectorRole;
import org.talend.sdk.component.tcketl.domain.connector.ConnectorSummary;
import org.talend.sdk.component.tcketl.domain.server.ComponentCatalog;
import org.talend.sdk.component.tcketl.domain.server.ServerStatus;

class CachingComponentCatalogTest {

    private static List<ConnectorId> ids(final String... values) {
        return Arrays.stream(values).map(ConnectorId::new).toList();
    }

    @Test
    void theStampIsTakenBeforeTheIndexIsLoadedAndTheIndexIsKept() {
        final FakeCatalog delegate = new FakeCatalog();
        final CachingComponentCatalog catalog = new CachingComponentCatalog(delegate);

        final List<ConnectorSummary> first = catalog.connectors();
        final List<ConnectorSummary> second = catalog.connectors();

        assertSame(first, second);
        assertEquals(List.of("status", "index"), delegate.calls);
    }

    @Test
    void onlyTheDefinitionsThatAreNotKnownYetAreLoadedAndTheRequestedOrderIsKept() {
        final FakeCatalog delegate = new FakeCatalog();
        final CachingComponentCatalog catalog = new CachingComponentCatalog(delegate);

        catalog.connectors(ids("a", "b"));
        final Map<ConnectorId, ConnectorDefinition> definitions = catalog.connectors(ids("c", "a", "unknown", "c"));

        assertEquals(ids("c", "a"), List.copyOf(definitions.keySet()));
        assertEquals(List.of("status", "details[a, b]", "details[c, unknown]"), delegate.calls);
    }

    @Test
    void anUnchangedStampKeepsTheCachesButAChangedOneDropsThem() {
        final FakeCatalog delegate = new FakeCatalog();
        final CachingComponentCatalog catalog = new CachingComponentCatalog(delegate);
        catalog.connectors();
        catalog.connectors(ids("a"));

        assertEquals("1", catalog.status().stamp());
        catalog.connectors();
        catalog.connectors(ids("a"));
        assertEquals(List.of("status", "index", "details[a]", "status"), delegate.calls);

        delegate.stamp = "2";
        assertEquals("2", catalog.status().stamp());
        catalog.connectors();
        catalog.connectors(ids("a"));
        assertEquals(List.of("status", "index", "details[a]", "status", "status", "index", "details[a]"),
                delegate.calls);
    }

    @Test
    void aStampChangeDuringTheLoadingOfTheIndexKeepsTheAnswerOutOfTheCache() {
        final FakeCatalog delegate = new FakeCatalog();
        final CachingComponentCatalog catalog = new CachingComponentCatalog(delegate);
        delegate.whileLoading = () -> {
            delegate.whileLoading = () -> {
            };
            delegate.stamp = "2";
            catalog.status();
        };

        assertEquals(1, catalog.connectors().size());
        assertEquals(1, catalog.connectors().size());

        assertEquals(List.of("status", "index", "status", "index"), delegate.calls);
    }

    @Test
    void aStampChangeDuringTheLoadingOfDefinitionsKeepsThemOutOfTheCache() {
        final FakeCatalog delegate = new FakeCatalog();
        final CachingComponentCatalog catalog = new CachingComponentCatalog(delegate);
        delegate.whileLoading = () -> {
            delegate.whileLoading = () -> {
            };
            delegate.stamp = "2";
            catalog.status();
        };

        assertEquals(ids("a"), List.copyOf(catalog.connectors(ids("a")).keySet()));
        assertEquals(ids("a"), List.copyOf(catalog.connectors(ids("a")).keySet()));

        assertEquals(List.of("status", "details[a]", "status", "details[a]"), delegate.calls);
    }

    @Test
    void invalidatingForgetsEverythingAndTakesANewBaseline() {
        final FakeCatalog delegate = new FakeCatalog();
        final CachingComponentCatalog catalog = new CachingComponentCatalog(delegate);
        catalog.connectors();
        catalog.connectors(ids("a"));

        catalog.invalidate();
        catalog.connectors();
        catalog.connectors(ids("a"));

        assertEquals(List.of("status", "index", "details[a]", "status", "index", "details[a]"), delegate.calls);
    }

    private static final class FakeCatalog implements ComponentCatalog {

        private static final Set<String> KNOWN = Set.of("a", "b", "c");

        private final List<String> calls = new ArrayList<>();

        private String stamp = "1";

        private Runnable whileLoading = () -> {
        };

        @Override
        public ServerStatus status() {
            calls.add("status");
            return new ServerStatus("1.0.0", "abcdef", stamp);
        }

        @Override
        public List<ConnectorSummary> connectors() {
            calls.add("index");
            whileLoading.run();
            return List.of(new ConnectorSummary(ref("a"), "familyId", "Family", "Name a", List.of(),
                    ConnectorRole.PROCESSOR, false, ""));
        }

        @Override
        public Map<ConnectorId, ConnectorDefinition> connectors(final Collection<ConnectorId> ids) {
            calls.add("details" + ids.stream().map(ConnectorId::value).toList());
            whileLoading.run();
            final Map<ConnectorId, ConnectorDefinition> found = new LinkedHashMap<>();
            for (final ConnectorId id : ids) {
                if (KNOWN.contains(id.value())) {
                    found.put(id, new ConnectorDefinition(ref(id.value()), "Name " + id.value(),
                            List.of("__default__"), List.of("__default__"), false, false, "", List.of(), List.of()));
                }
            }
            return found;
        }

        private static ConnectorRef ref(final String id) {
            return new ConnectorRef(new ConnectorId(id), "family", "name-" + id, "plugin", 1);
        }
    }
}
