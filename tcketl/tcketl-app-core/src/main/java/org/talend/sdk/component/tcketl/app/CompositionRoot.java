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
package org.talend.sdk.component.tcketl.app;

import java.io.IOException;
import java.lang.System.Logger;
import java.util.List;
import java.util.function.Function;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;

import org.talend.sdk.component.tcketl.app.async.AsyncRunner;
import org.talend.sdk.component.tcketl.app.canvas.CanvasViewModel;
import org.talend.sdk.component.tcketl.app.connection.ConnectionTester;
import org.talend.sdk.component.tcketl.app.palette.PaletteViewModel;
import org.talend.sdk.component.tcketl.app.pipeline.NodeResolver;
import org.talend.sdk.component.tcketl.app.project.ProjectService;
import org.talend.sdk.component.tcketl.app.session.ClientServerConnection;
import org.talend.sdk.component.tcketl.app.session.ServerConnection;
import org.talend.sdk.component.tcketl.app.session.ServerSession;
import org.talend.sdk.component.tcketl.config.ApplicationConfig;
import org.talend.sdk.component.tcketl.config.ConfigLoadResult;
import org.talend.sdk.component.tcketl.config.ConfigLocator;
import org.talend.sdk.component.tcketl.config.ConfigStore;
import org.talend.sdk.component.tcketl.config.FileConfigStore;
import org.talend.sdk.component.tcketl.domain.pipeline.PipelineEditor;
import org.talend.sdk.component.tcketl.persistence.FileSystemPipelineRepository;
import org.talend.sdk.component.tcketl.persistence.PipelineRepository;

/**
 * Wires the application together. It holds what lives as long as the application does: the settings and where they
 * are stored, the background executors, the session with the component server, the pipeline being edited and the
 * view models. The views are built on top of it and never need to know how any of this is made, which is also what
 * lets the tests replace the server and the files.
 *
 * <p>
 * Everything MUST be used on the UI thread.
 */
public final class CompositionRoot implements AutoCloseable {

    private static final Logger LOGGER = System.getLogger(CompositionRoot.class.getName());

    private final ConfigStore store;

    private final AsyncRunner async;

    private final Function<ApplicationConfig, ServerConnection> connections;

    private final ReadOnlyObjectWrapper<ApplicationConfig> config;

    private final ReadOnlyStringWrapper notice;

    private final ServerSession session;

    private final PaletteViewModel palette;

    private final PipelineEditor editor;

    private final ProjectService project;

    private final NodeResolver resolver;

    private final CanvasViewModel canvas;

    /**
     * Pipelines are read from and written to the file system.
     *
     * @see #CompositionRoot(ConfigStore, ConfigLoadResult, AsyncRunner, Function, PipelineRepository)
     */
    public CompositionRoot(final ConfigStore store, final ConfigLoadResult loaded, final AsyncRunner async,
            final Function<ApplicationConfig, ServerConnection> connections) {
        this(store, loaded, async, connections, new FileSystemPipelineRepository());
    }

    /**
     * @param store where the settings are kept
     * @param loaded the settings read from the store, and what went wrong when doing so
     * @param async the executors the blocking work and its outcomes go through
     * @param connections what turns settings into a connection to the server they describe
     * @param repository where the pipelines are read from and written to
     */
    public CompositionRoot(final ConfigStore store, final ConfigLoadResult loaded, final AsyncRunner async,
            final Function<ApplicationConfig, ServerConnection> connections, final PipelineRepository repository) {
        this.store = store;
        this.async = async;
        this.connections = connections;
        this.config = new ReadOnlyObjectWrapper<>(loaded.config());
        this.notice = new ReadOnlyStringWrapper(loaded.warning().orElse(""));
        this.session = new ServerSession(async);
        this.palette = new PaletteViewModel(session, async);
        this.editor = new PipelineEditor();
        this.project = new ProjectService(editor, repository, async);
        this.resolver = new NodeResolver(editor, session, async);
        this.canvas = new CanvasViewModel(editor, session);
    }

    /**
     * The application as it runs for real.
     *
     * @param arguments the command line, which may tell where the settings are
     * @param async the executors the blocking work and its outcomes go through
     * @throws IllegalArgumentException when the command line is not valid
     */
    public static CompositionRoot create(final List<String> arguments, final AsyncRunner async) {
        final ConfigStore store = new FileConfigStore(ConfigLocator.locate(arguments));
        return new CompositionRoot(store, store.load(), async, ClientServerConnection::create);
    }

    /** Connects to the server of the settings. */
    public void start() {
        session.connect(connections.apply(config.get()));
    }

    /**
     * Makes the settings the current ones: they are saved, and the session switches to the server they describe. A
     * settings file that cannot be written does not prevent from using the settings until the application stops, it
     * is reported through {@link #notice()}.
     */
    public void apply(final ApplicationConfig newConfig) {
        config.set(newConfig);
        try {
            store.save(newConfig);
            notice.set("");
        } catch (final IOException e) {
            LOGGER.log(Logger.Level.WARNING, "Cannot save the settings to " + store.file() + ": " + e.getMessage());
            notice.set("The settings could not be saved to " + store.file() + ": " + e.getMessage());
        }
        session.connect(connections.apply(newConfig));
    }

    /** What turns settings into the answer of the server they describe, which the connection dialog tests with. */
    public ConnectionTester tester() {
        return settings -> connections.apply(settings).catalog().status();
    }

    public ReadOnlyObjectProperty<ApplicationConfig> config() {
        return config.getReadOnlyProperty();
    }

    /** Something the user should know about the settings, blank when there is nothing. */
    public ReadOnlyStringProperty notice() {
        return notice.getReadOnlyProperty();
    }

    public AsyncRunner async() {
        return async;
    }

    public ServerSession session() {
        return session;
    }

    public PaletteViewModel palette() {
        return palette;
    }

    /** The pipeline being edited. */
    public PipelineEditor editor() {
        return editor;
    }

    /** The file the pipeline comes from, and what to do with the changes that are not saved. */
    public ProjectService project() {
        return project;
    }

    public CanvasViewModel canvas() {
        return canvas;
    }

    @Override
    public void close() {
        canvas.close();
        resolver.close();
        palette.close();
        session.close();
        async.close();
    }
}
