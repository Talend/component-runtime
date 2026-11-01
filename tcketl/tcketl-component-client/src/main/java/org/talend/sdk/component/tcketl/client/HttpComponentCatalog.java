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

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.talend.sdk.component.tcketl.domain.Preconditions;
import org.talend.sdk.component.tcketl.domain.connector.ConnectorDefinition;
import org.talend.sdk.component.tcketl.domain.connector.ConnectorId;
import org.talend.sdk.component.tcketl.domain.connector.ConnectorSummary;
import org.talend.sdk.component.tcketl.domain.server.ComponentCatalog;
import org.talend.sdk.component.tcketl.domain.server.ConnectorDocumentation;
import org.talend.sdk.component.tcketl.domain.server.DocumentationSource;
import org.talend.sdk.component.tcketl.domain.server.IconData;
import org.talend.sdk.component.tcketl.domain.server.IconSource;
import org.talend.sdk.component.tcketl.domain.server.IconTheme;
import org.talend.sdk.component.tcketl.domain.server.ServerProtocolException;
import org.talend.sdk.component.tcketl.domain.server.ServerStatus;

/**
 * The component server seen through HTTP: environment, component index and details, icons and documentation. Nothing
 * is cached here, see {@link CachingComponentCatalog}. The language is fixed for the lifetime of the instance.
 */
final class HttpComponentCatalog implements ComponentCatalog, IconSource, DocumentationSource {

    /** The server resolves every identifier of a details call, a few per call keeps the URL and the answer small. */
    static final int DETAILS_BATCH_SIZE = 10;

    private static final System.Logger LOGGER = System.getLogger(HttpComponentCatalog.class.getName());

    private final HttpTransport transport;

    private final String language;

    HttpComponentCatalog(final HttpTransport transport, final String language) {
        this.transport = Preconditions.requireNonNull(transport, "transport");
        this.language = Preconditions.requireNonBlank(language, "language");
    }

    @Override
    public ServerStatus status() {
        final Wire.Environment environment = Wire
                .read(transport.getOrThrow(HttpTransport.JSON, new HttpTransport.Query(), "environment"),
                        Wire.Environment.class);
        final String hash = environment.connectors() == null ? "" : environment.connectors().pluginsHash();
        return new ServerStatus(environment.version(), environment.commit(),
                Preconditions.nullToEmpty(environment.lastUpdated()) + '|' + Preconditions.nullToEmpty(hash));
    }

    @Override
    public List<ConnectorSummary> connectors() {
        final HttpTransport.Query query =
                new HttpTransport.Query().add("language", language).add("includeIconContent", "false");
        final Wire.ComponentIndexes indexes = Wire
                .read(transport.getOrThrow(HttpTransport.JSON, query, "component", "index"),
                        Wire.ComponentIndexes.class);
        final List<ConnectorSummary> summaries = new ArrayList<>();
        if (indexes.components() != null) {
            for (final Wire.ComponentIndex index : indexes.components()) {
                final Optional<ConnectorSummary> summary = ConnectorMapper.summary(index);
                if (summary.isPresent()) {
                    summaries.add(summary.get());
                } else {
                    LOGGER.log(System.Logger.Level.WARNING, "Ignoring a component of the index that has no identity");
                }
            }
        }
        return List.copyOf(summaries);
    }

    @Override
    public Map<ConnectorId, ConnectorDefinition> connectors(final Collection<ConnectorId> ids) {
        final List<ConnectorId> requested = List.copyOf(new LinkedHashSet<>(ids));
        final Map<ConnectorId, ConnectorDefinition> definitions = new LinkedHashMap<>();
        for (int from = 0; from < requested.size(); from += DETAILS_BATCH_SIZE) {
            loadBatch(requested.subList(from, Math.min(from + DETAILS_BATCH_SIZE, requested.size())), definitions);
        }
        return definitions;
    }

    @Override
    public Optional<IconData> componentIcon(final ConnectorId id, final IconTheme theme) {
        return icon(theme, "component", "icon", id.value());
    }

    @Override
    public Optional<IconData> familyIcon(final String familyId, final IconTheme theme) {
        return icon(theme, "component", "icon", "family", familyId);
    }

    @Override
    public Optional<ConnectorDocumentation> documentation(final ConnectorId id) {
        final HttpTransport.Response response = transport
                .get(HttpTransport.JSON, new HttpTransport.Query().add("language", language), "documentation",
                        "component", id.value());
        if (response.status() == HttpURLConnection.HTTP_NOT_FOUND) {
            return Optional.empty();
        }
        if (!response.isSuccess()) {
            throw response.toException();
        }
        final Wire.Documentation documentation = Wire.read(response, Wire.Documentation.class);
        return Optional.of(new ConnectorDocumentation(documentation.type(), documentation.source()));
    }

    /**
     * Asks for the details of the batch. The server refuses the whole call when one identifier is unknown, with an
     * error payload per rejected identifier: those are dropped and the call is repeated for the others.
     */
    private void loadBatch(final List<ConnectorId> batch, final Map<ConnectorId, ConnectorDefinition> into) {
        List<ConnectorId> pending = batch;
        while (!pending.isEmpty()) {
            final HttpTransport.Query query = new HttpTransport.Query().add("language", language);
            pending.forEach(id -> query.add("identifiers", id.value()));
            final HttpTransport.Response response = transport.get(HttpTransport.JSON, query, "component", "details");
            if (response.isSuccess()) {
                collect(response, pending, into);
                return;
            }
            final Set<String> rejected = response.status() == HttpURLConnection.HTTP_BAD_REQUEST
                    ? Wire.rejectedIds(response.body())
                    : Set.of();
            final List<ConnectorId> remaining = new ArrayList<>(pending);
            remaining.removeIf(id -> rejected.contains(id.value()));
            if (remaining.size() == pending.size()) {
                throw response.toException();
            }
            pending = remaining;
        }
    }

    private void collect(final HttpTransport.Response response, final List<ConnectorId> asked,
            final Map<ConnectorId, ConnectorDefinition> into) {
        final Wire.ComponentDetails details = Wire.read(response, Wire.ComponentDetails.class);
        if (details.details() == null) {
            return;
        }
        final Set<ConnectorId> wanted = new LinkedHashSet<>(asked);
        for (final Wire.ComponentDetail detail : details.details()) {
            try {
                final ConnectorDefinition definition = ConnectorMapper.definition(detail);
                if (wanted.contains(definition.id())) {
                    into.put(definition.id(), definition);
                }
            } catch (final IllegalArgumentException e) {
                throw new ServerProtocolException(response.status(), "Malformed component details: " + e.getMessage(),
                        e);
            }
        }
    }

    private Optional<IconData> icon(final IconTheme theme, final String... segments) {
        final HttpTransport.Query query = new HttpTransport.Query().add("theme", theme.name().toLowerCase(Locale.ROOT));
        final HttpTransport.Response response = transport.get(HttpTransport.BINARY, query, segments);
        if (response.status() == HttpURLConnection.HTTP_NOT_FOUND) {
            return Optional.empty();
        }
        if (!response.isSuccess()) {
            throw response.toException();
        }
        return Optional.of(new IconData(response.body(), response.mediaType()));
    }
}
