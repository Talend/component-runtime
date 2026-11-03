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
import java.util.HashMap;
import java.util.Map;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;

import org.talend.sdk.component.tcketl.app.CompositionRoot;
import org.talend.sdk.component.tcketl.app.async.AsyncRunner;
import org.talend.sdk.component.tcketl.app.palette.Palette;
import org.talend.sdk.component.tcketl.app.palette.PaletteFamily;
import org.talend.sdk.component.tcketl.config.ApplicationConfig;
import org.talend.sdk.component.tcketl.domain.connector.ConnectorId;
import org.talend.sdk.component.tcketl.domain.connector.ConnectorSummary;
import org.talend.sdk.component.tcketl.domain.server.IconData;
import org.talend.sdk.component.tcketl.domain.server.IconTheme;
import org.talend.sdk.component.tcketl.domain.server.ServerEndpoint;
import org.talend.sdk.component.tcketl.web.canvas.CanvasComponent;
import org.talend.sdk.component.tcketl.web.canvas.CanvasProtocol;

/**
 * The designer page. One {@link CompositionRoot} lives per browser tab (UI): the pipeline and the connection are not
 * shared between tabs, and everything is closed when the tab goes away so no call outlives it.
 */
@Route("")
@PageTitle("TCK ETL designer")
public class DesignerView extends VerticalLayout {

    private static final long serialVersionUID = 1L;

    private final transient WebSettings settings;

    private final TextField host = new TextField("Host");

    private final IntegerField port = new IntegerField("Port");

    private final TextField basePath = new TextField("Base URL");

    private final Select<String> scheme = new Select<>();

    private final Span status = new Span();

    private final TextField search = new TextField();

    private final Div palette = new Div();

    private final transient Map<ConnectorId, IconData> icons = new HashMap<>();

    private final CanvasComponent canvasView = new CanvasComponent();

    private final Div problems = new Div();

    private final Div inspectorHolder = new Div();

    private final Div projectHolder = new Div();

    private transient InspectorPanel inspector;

    private final Span feedback = new Span();

    private transient CanvasProtocol protocol;

    private transient boolean scheduled;

    private transient CompositionRoot root;

    public DesignerView(final WebSettings settings) {
        this.settings = settings;
        setSizeFull();
        setPadding(false);
        setSpacing(false);
        add(header(), content());
    }

    private HorizontalLayout header() {
        scheme.setLabel("Scheme");
        scheme.setItems("http", "https");
        scheme.setWidth(100, PIXELS);
        host.setWidth(200, PIXELS);
        port.setWidth(110, PIXELS);
        port.setMin(1);
        port.setMax(65535);
        port.setStepButtonsVisible(false);
        basePath.setWidth(160, PIXELS);
        final Button connect = new Button("Connect", e -> connect());
        final HorizontalLayout bar =
                new HorizontalLayout(new H3("TCK ETL"), scheme, host, port, basePath, connect, status);
        bar.setWidthFull();
        bar.setAlignItems(FlexComponent.Alignment.BASELINE);
        bar.getStyle().set("padding", "0.5rem 1rem").set("border-bottom", "1px solid var(--lumo-contrast-20pct)");
        return bar;
    }

    private HorizontalLayout content() {
        search.setPlaceholder("Search connectors");
        search.setClearButtonVisible(true);
        search.setValueChangeMode(ValueChangeMode.LAZY);
        search.setWidthFull();
        search.addValueChangeListener(e -> root.palette().query().set(e.getValue()));
        palette.setWidthFull();
        final VerticalLayout side = new VerticalLayout(search, palette);
        side.setWidth(320, PIXELS);
        side.setHeightFull();
        palette.getStyle().set("overflow-y", "auto");
        final HorizontalLayout body = new HorizontalLayout(side, canvasPane(), inspectorResizer(), inspectorHolder);
        inspectorHolder.setWidth(420, PIXELS);
        inspectorHolder.setHeightFull();
        inspectorHolder.getStyle().set("flex-shrink", "0");
        body.getStyle().set("overflow", "hidden");
        body.setSizeFull();
        return body;
    }

    /** A vertical bar the user drags to resize the inspector; the width is kept in the browser's local storage. */
    private Div inspectorResizer() {
        final Div handle = new Div();
        handle.setWidth(6, PIXELS);
        handle.setHeightFull();
        handle.getStyle()
                .set("flex-shrink", "0")
                .set("cursor", "col-resize")
                .set("background", "var(--lumo-contrast-10pct)")
                .set("touch-action", "none");
        handle.getElement()
                .executeJs("const handle = $0, panel = $1, key = 'tcketl.inspector.width';"
                        + "const limit = w => Math.max(280, Math.min(w, window.innerWidth - 480));"
                        + "const saved = parseInt(window.localStorage.getItem(key), 10);"
                        + "if (saved) { panel.style.width = limit(saved) + 'px'; }"
                        + "handle.addEventListener('pointerdown', e => {"
                        + "  handle.setPointerCapture(e.pointerId); e.preventDefault();"
                        + "  const move = m => { panel.style.width = limit(window.innerWidth - m.clientX) + 'px'; };"
                        + "  const up = () => { handle.removeEventListener('pointermove', move);"
                        + "    handle.removeEventListener('pointerup', up);"
                        + "    window.localStorage.setItem(key, parseInt(panel.style.width, 10)); };"
                        + "  handle.addEventListener('pointermove', move); handle.addEventListener('pointerup', up);"
                        + "});", handle.getElement(), inspectorHolder.getElement());
        return handle;
    }

    private VerticalLayout canvasPane() {
        final Button zoomIn = new Button("+", e -> canvasView.zoomBy(1.25));
        final Button zoomOut = new Button("\u2212", e -> canvasView.zoomBy(0.8));
        final Button reset = new Button("Reset view", e -> canvasView.resetView());
        final Button delete = new Button("Delete selection", e -> root.canvas().deleteSelection());
        final HorizontalLayout tools = new HorizontalLayout(zoomOut, zoomIn, reset, delete, projectHolder, feedback);
        tools.setAlignItems(FlexComponent.Alignment.CENTER);
        tools.getStyle().set("flex-wrap", "wrap");
        canvasView.setSizeFull();
        canvasView.getStyle().set("flex", "1").set("border", "1px solid var(--lumo-contrast-20pct)");
        problems.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("max-height", "8rem")
                .set("overflow-y", "auto");
        final VerticalLayout pane = new VerticalLayout(tools, canvasView, problems);
        pane.setPadding(false);
        pane.getStyle().set("flex", "1").set("min-width", "0");
        pane.setSizeFull();
        return pane;
    }

    private void refreshCanvas() {
        if (scheduled || root == null) {
            return;
        }
        scheduled = true;
        getUI().ifPresent(ui -> ui.beforeClientResponse(canvasView, context -> {
            scheduled = false;
            if (root != null) {
                canvasView.setGraph(protocol.graph());
                renderProblems();
            }
        }));
    }

    private void renderProblems() {
        problems.removeAll();
        root.canvas().problems().forEach(p -> problems.add(new Div(new Span(p.severity() + ": " + p.message()))));
        feedback.setText(root.canvas().feedback().get() == null ? "" : root.canvas().feedback().get());
    }

    @Override
    protected void onAttach(final AttachEvent event) {
        final UI ui = event.getUI();
        final ApplicationConfig initial = settings.initialConfig();
        root = new CompositionRoot(new MemoryConfigStore(initial), new MemoryConfigStore(initial).load(),
                AsyncRunner.production(command -> ui.access(command::run)),
                org.talend.sdk.component.tcketl.app.session.ClientServerConnection::create);
        show(initial);
        protocol = new CanvasProtocol(root.canvas(), id -> icons.get(id));
        inspector = new InspectorPanel(root, node -> root.editor()
                .node(node)
                .map(found -> icons.get(found.connector().id()))
                .orElse(null));
        inspectorHolder.removeAll();
        inspectorHolder.add(inspector);
        canvasView.onEvent(e -> {
            protocol.handle(e.detail());
            refreshCanvas();
        });
        root.editor().addListener(e -> refreshCanvas());
        root.canvas().selection().addListener((o, before, after) -> refreshCanvas());
        root.canvas().problems().addListener((javafx.collections.ListChangeListener<Object>) c -> refreshCanvas());
        root.canvas().feedback().addListener((o, before, after) -> refreshCanvas());
        refreshCanvas();
        root.session().state().addListener((o, before, after) -> refreshStatus());
        root.session().problem().addListener((o, before, after) -> refreshStatus());
        root.session().serverStatus().addListener((o, before, after) -> refreshStatus());
        root.palette().visible().addListener((o, before, after) -> renderPalette(after));
        projectHolder.removeAll();
        projectHolder.add(new ProjectBar(root));
        refreshStatus();
        root.start();
    }

    @Override
    protected void onDetach(final DetachEvent event) {
        if (inspector != null) {
            inspector.dispose();
            inspector = null;
        }
        if (root != null) {
            root.close();
            root = null;
        }
    }

    private void show(final ApplicationConfig config) {
        final ServerEndpoint server = config.server();
        scheme.setValue(server.scheme());
        host.setValue(server.host());
        port.setValue(server.port());
        basePath.setValue(server.basePath());
    }

    private void connect() {
        try {
            if (!settings.isAllowed(host.getValue())) {
                Notification.show("This host is not allowed by the server configuration", 4000,
                        Notification.Position.MIDDLE);
                return;
            }
            final ServerEndpoint server = new ServerEndpoint(scheme.getValue(), host.getValue(),
                    port.getValue() == null ? 0 : port.getValue(), basePath.getValue());
            root.apply(root.config().get().withServer(server));
        } catch (final IllegalArgumentException e) {
            Notification.show(e.getMessage(), 4000, Notification.Position.MIDDLE);
        }
    }

    private void refreshStatus() {
        final String problem = root.session().problem().get();
        status.setText(problem == null || problem.isBlank() ? root.session().description().get() : problem);
    }

    private void renderPalette(final Palette visible) {
        palette.removeAll();
        for (final PaletteFamily family : visible.families()) {
            final VerticalLayout entries = new VerticalLayout();
            entries.setPadding(false);
            entries.setSpacing(false);
            for (final ConnectorSummary connector : family.connectors()) {
                entries.add(entry(connector));
            }
            final Details details =
                    new Details(family.displayName() + " (" + family.connectors().size() + ")", entries);
            details.setOpened(!search.isEmpty());
            palette.add(details);
        }
        if (visible.isEmpty()) {
            palette.add(new Span(root.palette().placeholderText().get()));
        }
    }

    private HorizontalLayout entry(final ConnectorSummary connector) {
        final Image icon = new Image();
        icon.setWidth(24, PIXELS);
        icon.setHeight(24, PIXELS);
        icon.setAlt("");
        final Span label = new Span(connector.displayName());
        label.getElement().setAttribute("title", connector.documentation());
        final HorizontalLayout row = new HorizontalLayout(icon, label);
        row.setAlignItems(FlexComponent.Alignment.CENTER);
        row.getElement()
                .executeJs("$0.draggable = true; $0.style.cursor = \"grab\";"
                        + "$0.addEventListener(\"dragstart\", e => { e.dataTransfer.setData(\"application/x-tcketl-connector\", $1);"
                        + " e.dataTransfer.effectAllowed = \"copy\"; });", row.getElement(),
                        connector.ref().id().value());
        row.getElement().addEventListener("dblclick", e -> root.canvas().addConnector(connector));
        loadIcon(connector, icon);
        return row;
    }

    private void loadIcon(final ConnectorSummary connector, final Image target) {
        final ConnectorId id = connector.ref().id();
        final IconData cached = icons.get(id);
        if (cached != null) {
            apply(cached, id, target);
            return;
        }
        final CompositionRoot current = root;
        current.async()
                .submit(
                        () -> current.session().connection().get().icons().componentIcon(id, IconTheme.LIGHT),
                        found -> found.ifPresent(data -> {
                            icons.put(id, data);
                            apply(data, id, target);
                            if (inspector != null) {
                                inspector.refreshHeader();
                            }
                            refreshCanvas();
                        }), failure -> {
                        });
    }

    private static void apply(final IconData data, final ConnectorId id, final Image target) {
        final StreamResource resource = new StreamResource("icon-" + Math.abs(id.value().hashCode()),
                () -> new ByteArrayInputStream(data.content()));
        resource.setContentType(data.mediaType());
        target.setSrc(resource);
    }
}
