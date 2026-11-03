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
package org.talend.sdk.component.tcketl.app.pipeline;

import java.lang.System.Logger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javafx.beans.value.ChangeListener;

import org.talend.sdk.component.tcketl.app.async.AsyncRunner;
import org.talend.sdk.component.tcketl.app.session.Problems;
import org.talend.sdk.component.tcketl.app.session.ServerConnection;
import org.talend.sdk.component.tcketl.app.session.ServerSession;
import org.talend.sdk.component.tcketl.domain.connector.ConnectorDefinition;
import org.talend.sdk.component.tcketl.domain.connector.ConnectorId;
import org.talend.sdk.component.tcketl.domain.pipeline.Node;
import org.talend.sdk.component.tcketl.domain.pipeline.NodeId;
import org.talend.sdk.component.tcketl.domain.pipeline.PipelineEditor;
import org.talend.sdk.component.tcketl.domain.pipeline.PipelineEvent;
import org.talend.sdk.component.tcketl.domain.pipeline.PipelineListener;
import org.talend.sdk.component.tcketl.domain.pipeline.Resolution;

/**
 * Gives the nodes of a pipeline their connector definition (ports, properties), which is not part of the saved
 * pipeline and has to be asked to the component server: for a node that was just dropped, for every node of a pipeline
 * that was just opened, and again when the server is another one or came back.
 *
 * <p>
 * A node whose connector the server does not know, or that could not be asked, is marked as unresolved with the
 * reason and is otherwise left as it is. A node that already has a definition keeps it when the server cannot be
 * reached, so a server that goes away does not take the ports of the canvas with it.
 *
 * <p>
 * Everything MUST be used on the UI thread, which is also the thread the editor is changed on.
 */
public final class NodeResolver implements AutoCloseable {

    static final String NO_SERVER = "no component server is connected";

    private static final Logger LOGGER = System.getLogger(NodeResolver.class.getName());

    private final PipelineEditor editor;

    private final ServerSession session;

    private final AsyncRunner async;

    private final PipelineListener editorListener = this::onEditorEvent;

    private final ChangeListener<ServerConnection> connectionListener = (observable, before, now) -> resolveAll();

    private final ChangeListener<ServerSession.State> stateListener = (observable, before, now) -> {
        if (now == ServerSession.State.CONNECTED) {
            resolveAll();
        }
    };

    private long generation;

    private final java.util.Set<NodeId> migrating = new java.util.HashSet<>();

    private boolean closed;

    public NodeResolver(final PipelineEditor editor, final ServerSession session, final AsyncRunner async) {
        this.editor = editor;
        this.session = session;
        this.async = async;
        editor.addListener(editorListener);
        session.connection().addListener(connectionListener);
        session.state().addListener(stateListener);
    }

    /** Asks the server for the definition of every node. */
    public void resolveAll() {
        resolve(editor.pipeline().nodes().stream().map(Node::id).toList());
    }

    /** Asks the server, in one go, for the definitions of the connectors of these nodes. */
    public void resolve(final Collection<NodeId> ids) {
        if (closed) {
            return;
        }
        final Map<ConnectorId, List<NodeId>> wanted = new LinkedHashMap<>();
        for (final NodeId id : ids) {
            editor
                    .node(id)
                    .ifPresent(node -> wanted.computeIfAbsent(node.connector().id(), key -> new ArrayList<>()).add(id));
        }
        if (wanted.isEmpty()) {
            return;
        }
        final ServerConnection connection = session.connection().get();
        if (connection == null) {
            wanted.values().forEach(nodes -> nodes.forEach(id -> markUnresolved(id, NO_SERVER)));
            return;
        }
        final long requested = generation;
        final List<ConnectorId> connectors = List.copyOf(wanted.keySet());
        async
                .<Map<ConnectorId, ConnectorDefinition>> submit(() -> connection.catalog().connectors(connectors),
                        definitions -> arrived(requested, connection, wanted, definitions),
                        failure -> failed(requested, connection, wanted, failure));
    }

    @Override
    public void close() {
        closed = true;
        editor.removeListener(editorListener);
        session.connection().removeListener(connectionListener);
        session.state().removeListener(stateListener);
    }

    private void onEditorEvent(final PipelineEvent event) {
        if (event instanceof PipelineEvent.Reset) {
            generation++;
            resolveAll();
        } else if (event instanceof PipelineEvent.NodeAdded added
                && editor.resolution(added.node().id()) instanceof Resolution.Pending) {
            resolve(List.of(added.node().id()));
        }
    }

    private void arrived(final long requested, final ServerConnection connection,
            final Map<ConnectorId, List<NodeId>> wanted, final Map<ConnectorId, ConnectorDefinition> definitions) {
        if (!stillWanted(requested, connection)) {
            return;
        }
        wanted.forEach((connector, nodes) -> {
            final ConnectorDefinition definition = definitions.get(connector);
            for (final NodeId id : nodes) {
                if (editor.node(id).isEmpty()) {
                    continue;
                }
                if (definition == null) {
                    editor.markUnresolved(id, "it is not deployed on " + connection.endpoint());
                } else {
                    editor.bind(id, definition);
                    migrateIfOlder(connection, id, definition.ref().version());
                }
            }
        });
    }

    /** A node saved with an older version of its connector gets its configuration migrated by the server. */
    private void migrateIfOlder(final ServerConnection connection, final NodeId id, final int current) {
        final Node node = editor.node(id).orElse(null);
        if (node == null || !migrating.add(id)) {
            return;
        }
        final int saved = node.connector().version();
        if (saved >= current) {
            migrating.remove(id);
            if (saved > current) {
                LOGGER
                        .log(Logger.Level.WARNING, "Node " + node.label() + " was saved with version " + saved
                                + " of its connector but the server has " + current + ": left untouched");
            }
            return;
        }
        final Map<String, String> configuration = node.configuration();
        async.<Map<String, String>> submit(
                () -> connection.migrator().migrate(node.connector().id(), saved, configuration), migrated -> {
                    migrating.remove(id);
                    final Node now = editor.node(id).orElse(null);
                    if (!closed && now != null && now.connector().version() == saved
                            && now.configuration().equals(configuration)) {
                        editor.migrated(id, current, migrated);
                    }
                }, failure -> {
                    migrating.remove(id);
                    LOGGER
                            .log(Logger.Level.WARNING, "Cannot migrate the configuration of " + node.label() + ": "
                                    + Problems.describe(failure));
                });
    }

    private void failed(final long requested, final ServerConnection connection,
            final Map<ConnectorId, List<NodeId>> wanted, final Throwable failure) {
        if (!stillWanted(requested, connection)) {
            return;
        }
        final String reason = Problems.describe(failure);
        LOGGER.log(Logger.Level.WARNING, "Cannot load the definition of " + wanted.keySet() + ": " + reason);
        wanted.values().forEach(nodes -> nodes.forEach(id -> markUnresolved(id, reason)));
    }

    /** An answer is of no use when the pipeline was replaced or the server switched since the question. */
    private boolean stillWanted(final long requested, final ServerConnection connection) {
        return !closed && requested == generation && session.connection().get() == connection;
    }

    private void markUnresolved(final NodeId id, final String reason) {
        if (editor.node(id).isPresent() && !(editor.resolution(id) instanceof Resolution.Resolved)) {
            editor.markUnresolved(id, reason);
        }
    }
}
