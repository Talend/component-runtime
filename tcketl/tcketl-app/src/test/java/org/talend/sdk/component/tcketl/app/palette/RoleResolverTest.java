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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.tcketl.app.async.Cancellable;
import org.talend.sdk.component.tcketl.app.testing.Connectors;
import org.talend.sdk.component.tcketl.app.testing.FakeCatalog;
import org.talend.sdk.component.tcketl.app.testing.TestAsync;
import org.talend.sdk.component.tcketl.domain.connector.ConnectorId;
import org.talend.sdk.component.tcketl.domain.connector.ConnectorRole;
import org.talend.sdk.component.tcketl.domain.connector.ConnectorSummary;

class RoleResolverTest {

    private final TestAsync async = new TestAsync();

    private final FakeCatalog catalog = new FakeCatalog();

    private final RoleResolver resolver = new RoleResolver(async.runner());

    private final Map<ConnectorId, ConnectorRole> roles = new HashMap<>();

    @Test
    void loadsTheDefinitionsInBatchesOfTenTwoAtATime() {
        final List<ConnectorSummary> connectors = connectors(25);
        connectors.forEach(connector -> catalog.define(Connectors.asOutput(connector)));

        resolver.resolve(catalog, ids(connectors), roles::putAll);

        assertEquals(2, async.io().pending());
        while (async.io().pending() + async.ui().pending() > 0) {
            assertTrue(async.io().pending() + async.ui().pending() <= RoleResolver.CONCURRENCY);
            async.io().runNext();
            async.ui().runNext();
        }
        assertEquals(List.of(10, 10, 5), catalog.batches().stream().map(List::size).toList());
        assertEquals(25, roles.size());
        assertTrue(roles.values().stream().allMatch(role -> role == ConnectorRole.OUTPUT));
    }

    @Test
    void aFailedBatchOnlyEndsItsOwnLane() {
        final List<ConnectorSummary> connectors = connectors(25);
        connectors.forEach(connector -> catalog.define(Connectors.asInput(connector)));
        catalog.failBatchesWith(connectors.get(0).id());

        resolver.resolve(catalog, ids(connectors), roles::putAll);
        async.drain();

        assertEquals(15, roles.size());
        assertFalse(roles.containsKey(connectors.get(0).id()));
        assertTrue(roles.containsKey(connectors.get(24).id()));
    }

    @Test
    void cancellingStopsTheWorkThatIsLeft() {
        final List<ConnectorSummary> connectors = connectors(25);
        connectors.forEach(connector -> catalog.define(Connectors.asInput(connector)));

        final Cancellable run = resolver.resolve(catalog, ids(connectors), roles::putAll);
        async.io().runNext();
        run.cancel();
        async.drain();

        assertTrue(run.isCancelled());
        assertTrue(roles.isEmpty());
        assertEquals(1, catalog.batches().size());
    }

    private static List<ConnectorSummary> connectors(final int count) {
        return IntStream
                .range(0, count)
                .mapToObj(index -> Connectors.summary("family", "connector-" + index, ConnectorRole.PROCESSOR))
                .toList();
    }

    private static List<ConnectorId> ids(final List<ConnectorSummary> connectors) {
        return connectors.stream().map(ConnectorSummary::id).toList();
    }
}
