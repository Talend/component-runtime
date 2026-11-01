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
package org.talend.sdk.component.tcketl.app.session;

import java.lang.System.Logger;
import java.util.List;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.ReadOnlyLongProperty;
import javafx.beans.property.ReadOnlyLongWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;

import org.talend.sdk.component.tcketl.app.async.AsyncRunner;
import org.talend.sdk.component.tcketl.app.async.Cancellable;
import org.talend.sdk.component.tcketl.domain.connector.ConnectorSummary;
import org.talend.sdk.component.tcketl.domain.server.ServerStatus;

/**
 * The application's link with the component server it is configured for: it loads the list of connectors and
 * publishes how that went. A failed load keeps the list of the previous one, so that a server that goes away does not
 * empty the palette.
 *
 * <p>
 * Every method MUST be called on the UI thread, which is also the thread the properties are updated on.
 */
public final class ServerSession {

    private static final Logger LOGGER = System.getLogger(ServerSession.class.getName());

    private final AsyncRunner async;

    private final ReadOnlyObjectWrapper<State> state = new ReadOnlyObjectWrapper<>(State.DISCONNECTED);

    private final ReadOnlyObjectWrapper<ServerConnection> connection = new ReadOnlyObjectWrapper<>();

    private final ReadOnlyObjectWrapper<ServerStatus> serverStatus = new ReadOnlyObjectWrapper<>();

    private final ReadOnlyStringWrapper problem = new ReadOnlyStringWrapper("");

    private final ReadOnlyObjectWrapper<List<ConnectorSummary>> connectors =
            new ReadOnlyObjectWrapper<>(List.of());

    private final ReadOnlyLongWrapper revision = new ReadOnlyLongWrapper();

    private final StringBinding description;

    private Cancellable loading = Cancellable.NONE;

    public ServerSession(final AsyncRunner async) {
        this.async = async;
        this.description = Bindings.createStringBinding(this::describe, state, connection, serverStatus);
    }

    /** Switches to another server: the list is emptied and loaded again. */
    public void connect(final ServerConnection newConnection) {
        loading.cancel();
        connection.set(newConnection);
        serverStatus.set(null);
        connectors.set(List.of());
        revision.set(revision.get() + 1);
        load();
    }

    /** Asks the same server again, for example after a failure. */
    public void reload() {
        load();
    }

    /** Asks the same server again after dropping what was kept from it. */
    public void refresh() {
        final ServerConnection current = connection.get();
        if (current != null) {
            current.invalidate();
        }
        load();
    }

    public void close() {
        loading.cancel();
    }

    public ReadOnlyObjectProperty<State> state() {
        return state.getReadOnlyProperty();
    }

    /** The current connection, or {@code null} before {@link #connect}. */
    public ReadOnlyObjectProperty<ServerConnection> connection() {
        return connection.getReadOnlyProperty();
    }

    /** What the server said about itself when the list was loaded, or {@code null} when it is not connected. */
    public ReadOnlyObjectProperty<ServerStatus> serverStatus() {
        return serverStatus.getReadOnlyProperty();
    }

    /** Why the last load failed, blank unless the state is {@link State#FAILED}. */
    public ReadOnlyStringProperty problem() {
        return problem.getReadOnlyProperty();
    }

    public ReadOnlyObjectProperty<List<ConnectorSummary>> connectors() {
        return connectors.getReadOnlyProperty();
    }

    /** Changes each time {@link #connectors()} is replaced, even by an equal list. */
    public ReadOnlyLongProperty revision() {
        return revision.getReadOnlyProperty();
    }

    /** A sentence for the status bar. */
    public StringBinding description() {
        return description;
    }

    private void load() {
        loading.cancel();
        final ServerConnection current = connection.get();
        if (current == null) {
            return;
        }
        problem.set("");
        state.set(State.CONNECTING);
        loading = async.submit(() -> new Loaded(current.catalog().status(), current.catalog().connectors()),
                loaded -> {
                    serverStatus.set(loaded.status());
                    connectors.set(List.copyOf(loaded.connectors()));
                    revision.set(revision.get() + 1);
                    state.set(State.CONNECTED);
                }, failure -> {
                    log(current, failure);
                    problem.set(Problems.describe(failure));
                    serverStatus.set(null);
                    state.set(State.FAILED);
                });
    }

    private static void log(final ServerConnection connection, final Throwable failure) {
        final String message = "Cannot load the components of " + connection.endpoint();
        if (Problems.isExpected(failure)) {
            LOGGER.log(Logger.Level.WARNING, message + ": " + failure.getMessage());
        } else {
            LOGGER.log(Logger.Level.ERROR, message, failure);
        }
    }

    private String describe() {
        final ServerConnection current = connection.get();
        final State now = state.get();
        if (current == null || now == State.DISCONNECTED) {
            return "Not connected";
        }
        final String url = current.endpoint().toString();
        if (now == State.CONNECTING) {
            return "Connecting to " + url + "\u2026";
        }
        if (now == State.FAILED) {
            return "Not connected to " + url;
        }
        final ServerStatus status = serverStatus.get();
        final String version = status == null || status.version().isBlank() ? "" : ", server " + status.version();
        return "Connected to " + url + version;
    }

    public enum State {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        FAILED
    }

    private record Loaded(ServerStatus status, List<ConnectorSummary> connectors) {
    }
}
