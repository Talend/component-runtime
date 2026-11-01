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
package org.talend.sdk.component.tcketl.app.testing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.talend.sdk.component.tcketl.domain.connector.ConnectorDefinition;
import org.talend.sdk.component.tcketl.domain.connector.ConnectorId;
import org.talend.sdk.component.tcketl.domain.connector.ConnectorSummary;
import org.talend.sdk.component.tcketl.domain.server.ComponentCatalog;
import org.talend.sdk.component.tcketl.domain.server.ServerStatus;
import org.talend.sdk.component.tcketl.domain.server.ServerUnavailableException;

/** A component server that answers with what the test gave it, and remembers what it was asked. */
public final class FakeCatalog implements ComponentCatalog {

    private final Map<ConnectorId, ConnectorDefinition> definitions = new LinkedHashMap<>();

    private final Set<ConnectorId> failingIds = new HashSet<>();

    private final List<List<ConnectorId>> batches = new ArrayList<>();

    private ServerStatus status = new ServerStatus("1.0.0", "abc123", "stamp");

    private List<ConnectorSummary> connectors = List.of();

    private RuntimeException failure;

    private int statusCalls;

    private int listCalls;

    public FakeCatalog withStatus(final ServerStatus newStatus) {
        this.status = newStatus;
        return this;
    }

    public FakeCatalog withConnectors(final List<ConnectorSummary> newConnectors) {
        this.connectors = List.copyOf(newConnectors);
        return this;
    }

    public FakeCatalog define(final ConnectorDefinition definition) {
        definitions.put(definition.id(), definition);
        return this;
    }

    /** Every call fails with an unreachable server, until {@link #working()}. */
    public FakeCatalog unreachable(final String description) {
        this.failure = new ServerUnavailableException(description, null);
        return this;
    }

    public FakeCatalog working() {
        this.failure = null;
        return this;
    }

    /** Loading definitions fails for each batch that contains one of the ids. */
    public FakeCatalog failBatchesWith(final ConnectorId... ids) {
        failingIds.addAll(List.of(ids));
        return this;
    }

    /** The batches of ids the definitions were asked for, in order. */
    public List<List<ConnectorId>> batches() {
        return batches;
    }

    public int statusCalls() {
        return statusCalls;
    }

    public int listCalls() {
        return listCalls;
    }

    @Override
    public ServerStatus status() {
        statusCalls++;
        failIfUnreachable();
        return status;
    }

    @Override
    public List<ConnectorSummary> connectors() {
        listCalls++;
        failIfUnreachable();
        return connectors;
    }

    @Override
    public Map<ConnectorId, ConnectorDefinition> connectors(final Collection<ConnectorId> ids) {
        batches.add(List.copyOf(ids));
        failIfUnreachable();
        if (ids.stream().anyMatch(failingIds::contains)) {
            throw new ServerUnavailableException("The server did not answer", null);
        }
        final Map<ConnectorId, ConnectorDefinition> found = new LinkedHashMap<>();
        for (final ConnectorId id : ids) {
            if (definitions.containsKey(id)) {
                found.put(id, definitions.get(id));
            }
        }
        return found;
    }

    private void failIfUnreachable() {
        if (failure != null) {
            throw failure;
        }
    }
}
