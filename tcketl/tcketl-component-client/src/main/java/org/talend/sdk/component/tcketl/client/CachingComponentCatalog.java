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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.talend.sdk.component.tcketl.domain.Preconditions;
import org.talend.sdk.component.tcketl.domain.connector.ConnectorDefinition;
import org.talend.sdk.component.tcketl.domain.connector.ConnectorId;
import org.talend.sdk.component.tcketl.domain.connector.ConnectorSummary;
import org.talend.sdk.component.tcketl.domain.server.ComponentCatalog;
import org.talend.sdk.component.tcketl.domain.server.ServerStatus;

/**
 * Keeps what the server answered until it says it has changed. The server has no {@code ETag}: the only signal is the
 * {@link ServerStatus#stamp()}, which {@link #status()} compares on every call, so whoever polls the status keeps the
 * caches honest. There is no expiry. Network calls are made outside the lock; a {@code generation} counter tells
 * whether the caches were dropped while an answer was on its way, in which case that answer is returned but not kept.
 */
final class CachingComponentCatalog implements ComponentCatalog {

    private final ComponentCatalog delegate;

    private final Object lock = new Object();

    private final Map<ConnectorId, ConnectorDefinition> definitions = new HashMap<>();

    private long generation;

    private String stamp;

    private List<ConnectorSummary> summaries;

    CachingComponentCatalog(final ComponentCatalog delegate) {
        this.delegate = Preconditions.requireNonNull(delegate, "delegate");
    }

    /** Always asks the server, and drops the caches when the stamp is not the one seen before. */
    @Override
    public ServerStatus status() {
        final ServerStatus status = delegate.status();
        synchronized (lock) {
            if (stamp != null && !stamp.equals(status.stamp())) {
                clear();
            }
            stamp = status.stamp();
        }
        return status;
    }

    @Override
    public List<ConnectorSummary> connectors() {
        synchronized (lock) {
            if (summaries != null) {
                return summaries;
            }
        }
        observeStamp();
        final long loading = currentGeneration();
        final List<ConnectorSummary> loaded = List.copyOf(delegate.connectors());
        synchronized (lock) {
            if (loading == generation) {
                summaries = loaded;
            }
        }
        return loaded;
    }

    @Override
    public Map<ConnectorId, ConnectorDefinition> connectors(final Collection<ConnectorId> ids) {
        final List<ConnectorId> requested = List.copyOf(new LinkedHashSet<>(ids));
        observeStamp();
        final Map<ConnectorId, ConnectorDefinition> known = new HashMap<>();
        final List<ConnectorId> missing = new ArrayList<>();
        final long loading;
        synchronized (lock) {
            loading = generation;
            for (final ConnectorId id : requested) {
                final ConnectorDefinition cached = definitions.get(id);
                if (cached == null) {
                    missing.add(id);
                } else {
                    known.put(id, cached);
                }
            }
        }
        if (!missing.isEmpty()) {
            final Map<ConnectorId, ConnectorDefinition> loaded = delegate.connectors(missing);
            synchronized (lock) {
                if (loading == generation) {
                    definitions.putAll(loaded);
                }
            }
            known.putAll(loaded);
        }
        final Map<ConnectorId, ConnectorDefinition> ordered = new LinkedHashMap<>();
        for (final ConnectorId id : requested) {
            final ConnectorDefinition definition = known.get(id);
            if (definition != null) {
                ordered.put(id, definition);
            }
        }
        return ordered;
    }

    /** Forgets everything, e.g. when the user asks for a refresh. The next load takes a new baseline stamp. */
    void invalidate() {
        synchronized (lock) {
            clear();
            stamp = null;
        }
    }

    /**
     * The stamp is recorded before loading, so that a change happening after it is seen by the next {@link #status()}
     * even if it is not visible in the loaded data.
     */
    private void observeStamp() {
        final boolean known;
        synchronized (lock) {
            known = stamp != null;
        }
        if (!known) {
            status();
        }
    }

    private long currentGeneration() {
        synchronized (lock) {
            return generation;
        }
    }

    private void clear() {
        generation++;
        summaries = null;
        definitions.clear();
    }
}
