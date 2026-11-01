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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.talend.sdk.component.tcketl.app.async.AsyncRunner;
import org.talend.sdk.component.tcketl.app.async.Cancellable;
import org.talend.sdk.component.tcketl.app.session.ServerConnection;
import org.talend.sdk.component.tcketl.app.session.ServerSession;
import org.talend.sdk.component.tcketl.domain.connector.ConnectorId;
import org.talend.sdk.component.tcketl.domain.connector.ConnectorRole;
import org.talend.sdk.component.tcketl.domain.connector.ConnectorSummary;

/**
 * What the palette shows: the connectors of the session, filtered by the search text, and the role of each of them.
 * The role of a connector is the one the index suggests until its definition has been loaded in the background.
 *
 * <p>
 * Everything MUST be used on the UI thread.
 */
public final class PaletteViewModel {

    private final ServerSession session;

    private final RoleResolver resolver;

    private final StringProperty query = new SimpleStringProperty("");

    private final ReadOnlyObjectWrapper<Palette> all = new ReadOnlyObjectWrapper<>(Palette.EMPTY);

    private final ReadOnlyObjectWrapper<Palette> visible = new ReadOnlyObjectWrapper<>(Palette.EMPTY);

    private final ReadOnlyObjectWrapper<Map<ConnectorId, ConnectorRole>> roles =
            new ReadOnlyObjectWrapper<>(Map.of());

    private final StringBinding placeholderText;

    private final StringBinding countText;

    private Cancellable resolution = Cancellable.NONE;

    public PaletteViewModel(final ServerSession session, final AsyncRunner async) {
        this.session = session;
        this.resolver = new RoleResolver(async);
        this.placeholderText =
                Bindings.createStringBinding(this::placeholder, session.state(), all, visible, query);
        this.countText = Bindings.createStringBinding(this::count, all, visible);
        query.addListener((observed, before, after) -> visible.set(all.get().filter(after)));
        session.revision().addListener((observed, before, after) -> rebuild());
        rebuild();
    }

    /** The search text. */
    public StringProperty query() {
        return query;
    }

    /** The connectors that match the search text. */
    public ReadOnlyObjectProperty<Palette> visible() {
        return visible.getReadOnlyProperty();
    }

    /** The roles known so far, a new map each time one is learnt. */
    public ReadOnlyObjectProperty<Map<ConnectorId, ConnectorRole>> roles() {
        return roles.getReadOnlyProperty();
    }

    public ConnectorRole roleOf(final ConnectorSummary connector) {
        return roles.get().getOrDefault(connector.id(), connector.provisionalRole());
    }

    /** Whether the role comes from the definition of the connector rather than from the index. */
    public boolean isResolved(final ConnectorSummary connector) {
        return roles.get().containsKey(connector.id());
    }

    /** Why the list is empty, blank when there is something to show. */
    public StringBinding placeholderText() {
        return placeholderText;
    }

    public StringBinding countText() {
        return countText;
    }

    /** Asks the server for the connectors again. */
    public void refresh() {
        session.refresh();
    }

    public void close() {
        resolution.cancel();
    }

    private void rebuild() {
        resolution.cancel();
        resolution = Cancellable.NONE;
        final List<ConnectorSummary> connectors = session.connectors().get();
        final Palette palette = Palette.of(connectors);
        all.set(palette);
        visible.set(palette.filter(query.get()));
        forgetUnknownRoles(connectors);
        final ServerConnection connection = session.connection().get();
        if (connection != null && !connectors.isEmpty()) {
            resolution = resolver.resolve(connection.catalog(),
                    connectors.stream().map(ConnectorSummary::id).toList(), this::learn);
        }
    }

    private void forgetUnknownRoles(final List<ConnectorSummary> connectors) {
        final Map<ConnectorId, ConnectorRole> known = roles.get();
        if (known.isEmpty()) {
            return;
        }
        final Map<ConnectorId, ConnectorRole> kept = new HashMap<>(known);
        kept.keySet().retainAll(connectors.stream().map(ConnectorSummary::id).toList());
        roles.set(Map.copyOf(kept));
    }

    private void learn(final Map<ConnectorId, ConnectorRole> resolved) {
        final Map<ConnectorId, ConnectorRole> merged = new HashMap<>(roles.get());
        merged.putAll(resolved);
        roles.set(Map.copyOf(merged));
    }

    private String placeholder() {
        if (!visible.get().isEmpty()) {
            return "";
        }
        if (!all.get().isEmpty()) {
            return "No component matches \u201c" + query.get().strip() + "\u201d";
        }
        switch (session.state().get()) {
            case CONNECTING:
                return "Loading components\u2026";
            case CONNECTED:
                return "This server has no components";
            case FAILED:
                return "Components unavailable";
            default:
                return "Not connected";
        }
    }

    private String count() {
        final int total = all.get().size();
        final int shown = visible.get().size();
        if (total == 0) {
            return "";
        }
        if (shown == total) {
            return total == 1 ? "1 connector" : total + " connectors";
        }
        return shown + " of " + total;
    }
}
