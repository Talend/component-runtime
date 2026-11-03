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
package org.talend.sdk.component.tcketl.web;

import static com.vaadin.flow.component.Unit.PIXELS;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.server.StreamResource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.talend.sdk.component.tcketl.app.CompositionRoot;
import org.talend.sdk.component.tcketl.app.canvas.Selection;
import org.talend.sdk.component.tcketl.app.session.ServerConnection;
import org.talend.sdk.component.tcketl.domain.action.ActionOutcome;
import org.talend.sdk.component.tcketl.domain.action.ActionRequest;
import org.talend.sdk.component.tcketl.domain.connector.ConnectorDefinition;
import org.talend.sdk.component.tcketl.domain.pipeline.Node;
import org.talend.sdk.component.tcketl.domain.pipeline.NodeId;
import org.talend.sdk.component.tcketl.domain.pipeline.PipelineEvent;
import org.talend.sdk.component.tcketl.domain.pipeline.Resolution;
import org.talend.sdk.component.tcketl.domain.property.ActionDefinition;
import org.talend.sdk.component.tcketl.domain.server.ConnectorDocumentation;
import org.talend.sdk.component.tcketl.domain.server.IconData;
import org.talend.sdk.component.tcketl.domain.server.ServerException;
import org.talend.sdk.component.tcketl.forms.ActionContext;
import org.talend.sdk.component.tcketl.forms.FormViewModel;
import org.talend.sdk.component.tcketl.forms.vaadin.VaadinFormView;

/**
 * The panel next to the canvas: the form of the selected node. Each edit is stored in the pipeline as it is made. The
 * panel follows the selection and the pipeline; it is rebuilt when the node is changed by something else than the form
 * itself, and it never changes the pipeline by merely showing a form.
 */
final class InspectorPanel extends Div {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LoggerFactory.getLogger(InspectorPanel.class);

    private static final int HEADER_ICON_SIZE = 32;

    private final transient CompositionRoot root;

    private final transient Function<NodeId, IconData> icons;

    private transient VaadinFormView form;

    private transient NodeId shown;

    private transient Map<String, String> lastPushed = Map.of();

    InspectorPanel(final CompositionRoot root, final Function<NodeId, IconData> icons) {
        this.root = root;
        this.icons = icons;
        addClassName("inspector");
        getStyle().set("overflow-y", "auto").set("height", "100%").set("padding", "0 0.5rem");
        root.canvas().selection().addListener((value, before, after) -> rebuild());
        root.editor().addListener(this::onEvent);
        rebuild();
    }

    /** Rebuilds the header, for instance when the icon of the shown connector has arrived. */
    void refreshHeader() {
        if (shown != null) {
            rebuild();
        }
    }

    void dispose() {
        if (form != null) {
            form.dispose();
            form = null;
        }
        shown = null;
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
        } else if (event instanceof PipelineEvent.Reset || event instanceof PipelineEvent.NodeRemoved) {
            rebuild();
        }
    }

    private void rebuild() {
        dispose();
        removeAll();
        final Selection selection = root.canvas().selection().get();
        if (selection instanceof Selection.OfNode selected) {
            final Optional<Node> node = root.editor().node(selected.id());
            if (node.isPresent()) {
                showNode(node.get());
                return;
            }
        }
        add(message(selection instanceof Selection.OfLink ? "A link has no configuration."
                : "Select a node to configure it."));
    }

    private void showNode(final Node node) {
        shown = node.id();
        add(header(node));
        final Resolution resolution = root.editor().resolution(node.id());
        if (resolution instanceof Resolution.Resolved resolved) {
            showForm(node, resolved.definition());
        } else if (resolution instanceof Resolution.Unresolved unresolved) {
            add(message("The connector could not be loaded"
                    + (unresolved.reason().isEmpty() ? "." : ": " + unresolved.reason())
                    + " Its configuration is kept as it is."));
        } else {
            add(message("Loading the connector\u2026"));
        }
    }

    private Component header(final Node node) {
        final HorizontalLayout header = new HorizontalLayout();
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        final IconData data = icons.apply(node.id());
        if (data != null) {
            final Image icon = new Image();
            icon.setWidth(HEADER_ICON_SIZE, PIXELS);
            icon.setHeight(HEADER_ICON_SIZE, PIXELS);
            icon.setAlt("");
            final StreamResource resource = new StreamResource("inspector-icon-" + Math.abs(node.id().hashCode()),
                    () -> new ByteArrayInputStream(data.content()));
            resource.setContentType(data.mediaType());
            icon.setSrc(resource);
            header.add(icon);
        }
        final Span title = new Span(node.label());
        title.getStyle().set("font-weight", "600").set("font-size", "var(--lumo-font-size-l)");
        final Button docs = new Button("Documentation", event -> showDocumentation(node, event.getSource()));
        docs.addThemeVariants(ButtonVariant.LUMO_SMALL);
        header.add(title, docs);
        return header;
    }

    /** The asciidoc source of the connector, loaded off the request thread and shown as text in a dialog. */
    private void showDocumentation(final Node node, final Button button) {
        final ServerConnection connection = root.session().connection().get();
        if (connection == null) {
            return;
        }
        button.setEnabled(false);
        root.async()
                .<Optional<ConnectorDocumentation>> submit(
                        () -> connection.documentation().documentation(node.connector().id()), documentation -> {
                            button.setEnabled(true);
                            dialog(node, documentation.map(ConnectorDocumentation::text)
                                    .orElse("The server has no documentation for this connector."));
                        }, failure -> {
                            button.setEnabled(true);
                            dialog(node, "Cannot load the documentation: " + describe(failure));
                        });
    }

    private static void dialog(final Node node, final String text) {
        final Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Documentation - " + node.label());
        final Pre content = new Pre(text);
        content.getStyle().set("white-space", "pre-wrap").set("max-width", "640px");
        dialog.add(content);
        dialog.getFooter().add(new Button("Close", event -> dialog.close()));
        dialog.open();
    }

    private void showForm(final Node node, final ConnectorDefinition definition) {
        try {
            final FormViewModel model = FormViewModel.of(definition.properties(), node.configuration());
            lastPushed = node.configuration();
            model.valuesProperty().addListener((value, before, after) -> {
                lastPushed = after.toConfiguration();
                root.editor().setConfiguration(node.id(), lastPushed);
            });
            form = VaadinFormView.of(model, actionsOf(definition));
            add(form.component());
        } catch (final RuntimeException failure) {
            LOGGER.error("Cannot render the form of {}", node.connector().id(), failure);
            form = null;
            add(message("The form cannot be rendered: " + describe(failure)));
        }
    }

    /** The connection is read when a service is called, so a reconnection is followed without rebuilding. */
    private ActionContext actionsOf(final ConnectorDefinition definition) {
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
                final ServerConnection connection = root.session().connection().get();
                if (connection == null) {
                    onOutcome.accept(new ActionOutcome.Failed("Not connected to a component server"));
                    return;
                }
                root.async()
                        .submit(() -> connection.actions().execute(request), onOutcome,
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

    private static Span message(final String text) {
        final Span span = new Span(text);
        span.getStyle().set("color", "var(--lumo-secondary-text-color)");
        return span;
    }
}
