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
package org.talend.sdk.component.tcketl.app.inspector;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import org.talend.sdk.component.tcketl.app.async.AsyncRunner;
import org.talend.sdk.component.tcketl.app.canvas.CanvasViewModel;
import org.talend.sdk.component.tcketl.app.canvas.Selection;
import org.talend.sdk.component.tcketl.app.session.ServerConnection;
import org.talend.sdk.component.tcketl.app.session.ServerSession;
import org.talend.sdk.component.tcketl.domain.Preconditions;
import org.talend.sdk.component.tcketl.domain.action.ActionOutcome;
import org.talend.sdk.component.tcketl.domain.action.ActionRequest;
import org.talend.sdk.component.tcketl.domain.connector.ConnectorDefinition;
import org.talend.sdk.component.tcketl.domain.pipeline.Node;
import org.talend.sdk.component.tcketl.domain.pipeline.NodeId;
import org.talend.sdk.component.tcketl.domain.pipeline.PipelineEditor;
import org.talend.sdk.component.tcketl.domain.pipeline.PipelineEvent;
import org.talend.sdk.component.tcketl.domain.pipeline.Resolution;
import org.talend.sdk.component.tcketl.domain.property.ActionDefinition;
import org.talend.sdk.component.tcketl.domain.server.ConnectorDocumentation;
import org.talend.sdk.component.tcketl.domain.server.ServerException;
import org.talend.sdk.component.tcketl.forms.ActionContext;
import org.talend.sdk.component.tcketl.forms.FormView;
import org.talend.sdk.component.tcketl.forms.FormViewModel;

/**
 * The panel next to the canvas: the form of the selected node. Each edit is stored in the pipeline as it is made. The
 * panel follows the selection and the pipeline; it is rebuilt when the node is changed by something else than the form
 * itself, and it never changes the pipeline by merely showing a form.
 */
public final class InspectorView extends BorderPane {

    private final PipelineEditor editor;

    private final CanvasViewModel canvas;

    private final ServerSession session;

    private final AsyncRunner async;

    private final Label title = new Label();

    private FormView form;

    private NodeId shown;

    private Map<String, String> lastPushed = Map.of();

    public InspectorView(final CanvasViewModel canvas) {
        this(canvas, null, null);
    }

    /** With a session and a runner, the forms can call the services of the connectors on the server. */
    public InspectorView(final CanvasViewModel canvas, final ServerSession session, final AsyncRunner async) {
        this.canvas = Preconditions.requireNonNull(canvas, "canvas");
        this.session = session;
        this.async = async;
        this.editor = canvas.editor();
        getStyleClass().add("inspector");
        title.getStyleClass().add("inspector-title");
        canvas.selection().addListener((value, before, after) -> rebuild());
        editor.addListener(this::onEvent);
        rebuild();
    }

    private void onEvent(final PipelineEvent event) {
        if (event instanceof PipelineEvent.NodeConfigured configured) {
            if (configured.node().equals(shown) && !configured.configuration().equals(lastPushed)) {
                rebuild();
            }
        } else if (event instanceof PipelineEvent.NodeResolutionChanged changed) {
            if (changed.node().equals(shown)) {
                rebuild();
            }
        } else if (event instanceof PipelineEvent.NodeRelabeled relabeled) {
            if (relabeled.node().equals(shown)) {
                title.setText(relabeled.label());
            }
        } else if (event instanceof PipelineEvent.Reset || event instanceof PipelineEvent.NodeRemoved) {
            rebuild();
        }
    }

    private void rebuild() {
        if (form != null) {
            form.dispose();
            form = null;
        }
        shown = null;
        final Selection selection = canvas.selection().get();
        if (selection instanceof Selection.OfNode selected) {
            final Optional<Node> node = editor.node(selected.id());
            if (node.isPresent()) {
                showNode(node.get());
                return;
            }
        }
        setTop(null);
        setCenter(message(selection instanceof Selection.OfLink ? "A link has no configuration."
                : "Select a node to configure it."));
    }

    private void showNode(final Node node) {
        shown = node.id();
        title.setText(node.label());
        final Button docs = new Button("Documentation");
        docs.setDisable(session == null || async == null);
        docs.setOnAction(event -> showDocumentation(node, docs));
        final HBox header = new HBox(8, title, docs);
        header.setAlignment(Pos.CENTER_LEFT);
        setTop(header);
        final Resolution resolution = editor.resolution(node.id());
        if (resolution instanceof Resolution.Resolved resolved) {
            showForm(node, resolved.definition());
        } else if (resolution instanceof Resolution.Unresolved unresolved) {
            setCenter(message("The connector could not be loaded"
                    + (unresolved.reason().isEmpty() ? "." : ": " + unresolved.reason())
                    + "\nIts configuration is kept as it is."));
        } else {
            setCenter(message("Loading the connector\u2026"));
        }
    }

    /** The asciidoc source of the connector, loaded off the UI thread and shown as text in a window. */
    private void showDocumentation(final Node node, final Button button) {
        final ServerConnection connection = session.connection().get();
        if (connection == null) {
            return;
        }
        button.setDisable(true);
        async.<Optional<ConnectorDocumentation>> submit(() -> connection.documentation()
                .documentation(
                        node.connector().id()),
                documentation -> {
                    button.setDisable(false);
                    final TextArea text = new TextArea(documentation.map(ConnectorDocumentation::text)
                            .orElse("The server has no documentation for this connector."));
                    text.setEditable(false);
                    text.setWrapText(true);
                    final Stage stage = new Stage();
                    stage.setTitle("Documentation - " + node.label());
                    stage.setScene(new Scene(text, 640, 480));
                    stage.show();
                }, failure -> {
                    button.setDisable(false);
                    final TextArea text = new TextArea("Cannot load the documentation: " + describe(failure));
                    text.setEditable(false);
                    final Stage stage = new Stage();
                    stage.setScene(new Scene(text, 480, 120));
                    stage.show();
                });
    }

    private void showForm(final Node node, final ConnectorDefinition definition) {
        final FormViewModel model = FormViewModel.of(definition.properties(), node.configuration());
        lastPushed = node.configuration();
        model.valuesProperty().addListener((value, before, after) -> {
            lastPushed = after.toConfiguration();
            editor.setConfiguration(node.id(), lastPushed);
        });
        form = FormView.of(model, actionsOf(definition));
        setCenter(form.node());
    }

    /** The connection is read when a service is called, so a reconnection is followed without rebuilding. */
    private ActionContext actionsOf(final ConnectorDefinition definition) {
        if (session == null || async == null) {
            return ActionContext.NONE;
        }
        return new ActionContext() {

            @Override
            public String family() {
                return definition.ref().family();
            }

            @Override
            public List<ActionDefinition> actions() {
                return definition.actions();
            }

            @Override
            public void execute(final ActionRequest request, final Consumer<ActionOutcome> onOutcome) {
                final ServerConnection connection = session.connection().get();
                if (connection == null) {
                    onOutcome.accept(new ActionOutcome.Failed("Not connected to a component server"));
                    return;
                }
                async.submit(() -> connection.actions().execute(request), onOutcome,
                        failure -> onOutcome.accept(new ActionOutcome.Failed(describe(failure))));
            }
        };
    }

    private static String describe(final Throwable failure) {
        final String message = failure instanceof ServerException server && !server.getMessage().isBlank()
                ? server.getMessage()
                : failure.getMessage();
        return message == null || message.isBlank() ? failure.getClass().getSimpleName() : message;
    }

    private static VBox message(final String text) {
        final Label label = new Label(text);
        label.setWrapText(true);
        label.getStyleClass().add("inspector-message");
        return new VBox(label);
    }
}
