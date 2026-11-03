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
package org.talend.sdk.component.tcketl.app.canvas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.DataFormat;
import javafx.scene.input.KeyCode;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.shape.CubicCurve;
import javafx.stage.Stage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import org.talend.sdk.component.tcketl.app.CompositionRoot;
import org.talend.sdk.component.tcketl.app.async.FxAsync;
import org.talend.sdk.component.tcketl.app.icon.IconService;
import org.talend.sdk.component.tcketl.app.palette.PaletteTransfer;
import org.talend.sdk.component.tcketl.app.session.ServerSession;
import org.talend.sdk.component.tcketl.app.testing.Connectors;
import org.talend.sdk.component.tcketl.app.testing.FakeCatalog;
import org.talend.sdk.component.tcketl.app.testing.FakeConnection;
import org.talend.sdk.component.tcketl.app.testing.FxHarness;
import org.talend.sdk.component.tcketl.config.ApplicationConfig;
import org.talend.sdk.component.tcketl.config.ConfigLoadResult;
import org.talend.sdk.component.tcketl.config.FileConfigStore;
import org.talend.sdk.component.tcketl.domain.connector.ConnectorRole;
import org.talend.sdk.component.tcketl.domain.connector.ConnectorSummary;
import org.talend.sdk.component.tcketl.domain.connector.Port;
import org.talend.sdk.component.tcketl.domain.pipeline.Endpoint;
import org.talend.sdk.component.tcketl.domain.pipeline.Link;
import org.talend.sdk.component.tcketl.domain.pipeline.LinkRejection;
import org.talend.sdk.component.tcketl.domain.pipeline.NodeId;
import org.talend.sdk.component.tcketl.domain.pipeline.Pipeline;
import org.talend.sdk.component.tcketl.domain.pipeline.PipelineProblem;
import org.talend.sdk.component.tcketl.domain.pipeline.Position;
import org.talend.sdk.component.tcketl.domain.pipeline.Resolution;
import org.talend.sdk.component.tcketl.persistence.FileSystemPipelineRepository;
import org.talend.sdk.component.tcketl.persistence.PipelineRepository;

/**
 * Draws the canvas off screen with the real stylesheet and drives it through the input of the real scene, which
 * picks what is under the pointer, counts the clicks and routes the keys as it does for a window, to look at what it
 * shows and to check what the gestures do. The toolkit is needed, so it only runs when asked to, with
 * {@code -Dtcketl.ui.snapshots=<directory>} where the pictures are written. Add
 * {@code -Dtcketl.ui.config=<settings file>} to also draw a pipeline with the connectors of the server those settings
 * point at.
 */
@Tag("ui")
@EnabledIfSystemProperty(named = FxHarness.SNAPSHOTS, matches = ".+")
class CanvasSnapshotTest {

    private static final String LIVE_CONFIG = "tcketl.ui.config";

    private static final String STYLESHEET = "/org/talend/sdk/component/tcketl/app/tcketl.css";

    private static final Duration PATIENCE = Duration.ofSeconds(20);

    private static final int WIDTH = 1000;

    private static final int HEIGHT = 600;

    private static final double TOLERANCE = 0.01;

    private static final String MAIN = Port.DEFAULT_FLOW;

    private static final String FAMILY = "Name: The family -en";

    @TempDir
    Path temp;

    private final Path directory = FxHarness.snapshotDirectory();

    private final ConnectorSummary input =
            Connectors.summary("the_family", FAMILY, "Simple Input", List.of("Test"), ConnectorRole.INPUT);

    private final ConnectorSummary otherInput =
            Connectors.summary("sampleForm", "Sample form", "Input", List.of("Forms"), ConnectorRole.INPUT);

    private final ConnectorSummary processor = Connectors
            .summary("sampleFixedSchema", "Sample fixed schema", "Fixed Schema Processor", List.of(),
                    ConnectorRole.PROCESSOR);

    private final ConnectorSummary sink = Connectors
            .summary("the_family", FAMILY, "Name: The Output 1 -en", List.of("Test"), ConnectorRole.PROCESSOR);

    private final ConnectorSummary absent =
            Connectors.summary("gone", "Gone", "Not deployed", List.of(), ConnectorRole.PROCESSOR);

    private CompositionRoot root;

    private Scene scene;

    private CanvasView canvas;

    private NodeId read;

    private NodeId transform;

    private NodeId write;

    private NodeId rejects;

    @BeforeAll
    static void startToolkit() throws InterruptedException {
        FxHarness.start();
    }

    @AfterEach
    void close() throws Exception {
        if (root != null) {
            FxHarness.call(() -> {
                canvas.close();
                root.close();
                return null;
            });
        }
    }

    @Test
    void anEmptyCanvasInvitesToAddConnectors() throws Exception {
        show(catalog());

        snapshot("canvas-empty");

        assertTrue(FxHarness.call(() -> scene.lookup(".canvas-hint").isVisible()));
        assertEquals(0, FxHarness.call(() -> FxHarness.all(scene.getRoot(), NodeView.class).size()));
    }

    @Test
    void aPipelineIsDrawnWithTheMouse() throws Exception {
        show(catalog());
        addNodes();

        assertEquals(List.of(), flows(read, Port.Direction.INPUT));
        assertEquals(List.of(MAIN), flows(read, Port.Direction.OUTPUT));
        assertEquals(List.of(MAIN), flows(transform, Port.Direction.INPUT));
        assertEquals(List.of(MAIN, "second", "REJECT"), flows(transform, Port.Direction.OUTPUT));
        assertEquals(List.of(MAIN), flows(write, Port.Direction.INPUT));
        assertEquals(List.of(), flows(write, Port.Direction.OUTPUT));

        // while the link is drawn, the inputs show whether they accept it
        final PortView source = output(read, MAIN);
        press(source);
        drag(source, center(input(transform, MAIN)));
        assertEquals(new Endpoint(read, MAIN), linkSource());
        assertEquals(PortView.Mode.HOT, input(transform, MAIN).mode());
        assertEquals(PortView.Mode.COMPATIBLE, input(write, MAIN).mode());
        snapshot("canvas-link-being-drawn");
        release(source, center(input(transform, MAIN)));
        layout();
        assertNull(linkSource());
        assertEquals(PortView.Mode.IDLE, input(write, MAIN).mode());

        connectWithTheMouse(output(transform, "second"), input(write, MAIN));
        connectWithTheMouse(output(transform, "REJECT"), input(rejects, MAIN));

        final List<Link> expected = List
                .of(link(read, MAIN, transform), link(transform, "second", write), link(transform, "REJECT", rejects));
        assertEquals(expected, links());
        expected.forEach(this::assertJoined);
        snapshot("canvas-pipeline");
    }

    @Test
    void aLinkThatBreaksARuleIsRefused() throws Exception {
        show(catalog());
        addNodes();
        final NodeId other = add(otherInput, 40, 280);
        awaitResolved(other);
        FxHarness.call(() -> {
            root.editor().connect(link(read, MAIN, transform));
            return null;
        });
        layout();

        // an input takes one link only
        final PortView source = output(other, MAIN);
        press(source);
        drag(source, center(input(transform, MAIN)));
        assertEquals(PortView.Mode.REFUSED, input(transform, MAIN).mode());
        assertEquals(PortView.Mode.COMPATIBLE, input(write, MAIN).mode());
        assertEquals(LinkRejection.INPUT_PORT_TAKEN.message(), feedback());
        snapshot("canvas-link-refused");
        release(source, center(input(transform, MAIN)));
        layout();
        assertEquals(List.of(link(read, MAIN, transform)), links());
        assertEquals(LinkRejection.INPUT_PORT_TAKEN.message(), feedback());
        assertEquals(PortView.Mode.IDLE, input(transform, MAIN).mode());

        // nor does a node link to itself
        final PortView own = output(transform, MAIN);
        press(own);
        drag(own, center(input(transform, MAIN)));
        assertEquals(LinkRejection.SELF_LINK.message(), feedback());
        release(own, center(input(transform, MAIN)));
        assertEquals(1, links().size());

        // letting go anywhere else gives up, quietly, as the escape key does
        press(source);
        release(source, new Point2D(900, 500));
        assertNull(linkSource());
        assertEquals("", feedback());
        press(source);
        FxHarness.key(scene, KeyCode.ESCAPE);
        assertNull(linkSource());
        assertEquals(PortView.Mode.IDLE, input(write, MAIN).mode());
    }

    @Test
    void movingANodeKeepsItsLinksAndDoesNotPanTheCanvas() throws Exception {
        show(catalog());
        addNodes();
        // far enough to make the canvas wider than the window, so that it can pan
        final NodeId far = add(sink, 1500, 800);
        awaitResolved(far);
        FxHarness.call(() -> {
            root.editor().connect(link(read, MAIN, transform));
            root.editor().connect(link(transform, "second", write));
            return null;
        });
        layout();

        final Node title = FxHarness.call(() -> nodeView(transform).lookup(".node-title"));
        final Point2D grab = FxHarness.call(() -> centerOf(title));
        final Point2D drop = grab.add(97, 43);
        FxHarness.press(scene, grab);
        FxHarness.dragTo(scene, drop);
        FxHarness.release(scene, drop);
        layout();

        assertEquals(new Position(420, 100),
                FxHarness.call(() -> root.editor().node(transform).orElseThrow().position()));
        assertEquals(new Position(420, 100), FxHarness.call(() -> {
            final NodeView view = nodeView(transform);
            return new Position(view.getLayoutX(), view.getLayoutY());
        }));
        assertTrue(FxHarness.call(() -> root.canvas().selection().get().selects(transform)));
        assertTrue(FxHarness.call(() -> nodeView(transform).getStyleClass().contains("selected")));
        assertEquals(0, scrolled().getX(), TOLERANCE);
        assertEquals(0, scrolled().getY(), TOLERANCE);
        links().forEach(this::assertJoined);
        snapshot("canvas-node-moved");

        // the empty canvas, on the other hand, pans, and what was selected is not anymore
        final Point2D empty = new Point2D(900, 520);
        assertSame(FxHarness.call(this::surface), FxHarness.under(scene, empty));
        final Point2D panned = empty.subtract(new Point2D(150, 90));
        FxHarness.press(scene, empty);
        FxHarness.dragTo(scene, panned);
        FxHarness.release(scene, panned);
        layout();
        assertEquals(Selection.NONE, FxHarness.call(() -> root.canvas().selection().get()));
        assertTrue(scrolled().getX() > 0 && scrolled().getY() > 0, "The canvas did not pan: " + scrolled());
        links().forEach(this::assertJoined);
    }

    @Test
    void zoomingKeepsTheLinksOnTheirPorts() throws Exception {
        show(catalog());
        addNodes();
        FxHarness.call(() -> {
            root.editor().connect(link(read, MAIN, transform));
            root.editor().connect(link(transform, "second", write));
            return null;
        });
        layout();

        final Button zoomIn = FxHarness
                .call(() -> FxHarness
                        .all(scene.getRoot(), Button.class)
                        .stream()
                        .filter(button -> button.getStyleClass().contains("zoom-button")
                                && "+".equals(button.getText()))
                        .findFirst()
                        .orElseThrow());
        final Point2D button = FxHarness.call(() -> centerOf(zoomIn));
        FxHarness.click(scene, button);
        FxHarness.click(scene, button);
        layout();

        assertEquals(1.56, FxHarness.call(() -> root.canvas().zoom().get()), TOLERANCE);
        links().forEach(this::assertJoined);
        snapshot("canvas-zoomed");
    }

    @Test
    void deleteRemovesTheSelectedLinkThenTheSelectedNode() throws Exception {
        show(catalog());
        addNodes();
        final Link first = link(read, MAIN, transform);
        final Link second = link(transform, "second", write);
        FxHarness.call(() -> {
            root.editor().connect(first);
            root.editor().connect(second);
            return null;
        });
        layout();

        final CubicCurve line = FxHarness.call(() -> curve(first));
        FxHarness.click(scene, FxHarness.call(() -> middleOf(line)));
        layout();
        assertTrue(FxHarness.call(() -> root.canvas().selection().get().selects(first)));
        snapshot("canvas-link-selected");
        FxHarness.key(scene, KeyCode.DELETE);
        layout();
        assertEquals(List.of(second), links());
        assertEquals(1, FxHarness.call(() -> FxHarness.all(scene.getRoot(), LinkView.class).size()));
        assertEquals(4, FxHarness.call(() -> FxHarness.all(scene.getRoot(), NodeView.class).size()));

        final Node title = FxHarness.call(() -> nodeView(transform).lookup(".node-title"));
        FxHarness.click(scene, FxHarness.call(() -> centerOf(title)));
        FxHarness.key(scene, KeyCode.DELETE);
        layout();
        assertEquals(List.of(), links());
        assertEquals(0, FxHarness.call(() -> FxHarness.all(scene.getRoot(), LinkView.class).size()));
        assertEquals(3, FxHarness.call(() -> FxHarness.all(scene.getRoot(), NodeView.class).size()));
        assertTrue(FxHarness.call(() -> root.editor().node(transform).isEmpty()));
    }

    @Test
    void aNodeIsRenamedInPlace() throws Exception {
        show(catalog());
        final NodeId id = add(input, 40, 80);
        awaitResolved(id);

        final Node header = FxHarness.call(() -> nodeView(id).lookup(".node-header"));
        FxHarness.doubleClick(scene, FxHarness.call(() -> centerOf(header)));
        layout();
        final TextField field = FxHarness.call(() -> (TextField) nodeView(id).lookup(".node-rename"));
        snapshot("canvas-renaming");
        FxHarness.call(() -> {
            field.setText("  Read orders ");
            return null;
        });
        FxHarness.key(scene, KeyCode.ENTER);
        layout();

        assertEquals("Read orders", FxHarness.call(() -> root.editor().node(id).orElseThrow().label()));
        assertEquals("Read orders", FxHarness.call(() -> ((Label) nodeView(id).lookup(".node-title")).getText()));
        assertNull(FxHarness.call(() -> nodeView(id).lookup(".node-rename")));
        assertSame(FxHarness.call(this::surface), FxHarness.call(() -> scene.getFocusOwner()));
    }

    @Test
    void aConnectorDroppedFromThePaletteBecomesANodeUnderThePointer() throws Exception {
        show(catalog());
        final Point2D at = new Point2D(300, 200);

        // only what a palette entry carries is welcome
        final FxHarness.Drag entry = FxHarness.drag(scene, PaletteTransfer.CONNECTOR, input.id().value());
        assertEquals(TransferMode.COPY, entry.over(at));
        assertEquals(TransferMode.COPY, entry.over(at.add(5, 5)));
        entry.leave(at);
        final FxHarness.Drag text = FxHarness.drag(scene, DataFormat.PLAIN_TEXT, "some text");
        assertNull(text.over(at));
        assertFalse(text.drop(at));
        assertEquals(0, FxHarness.call(() -> root.editor().pipeline().nodes().size()));

        assertTrue(FxHarness.drag(scene, PaletteTransfer.CONNECTOR, input.id().value()).drop(at));
        final NodeId dropped = FxHarness.call(() -> root.editor().pipeline().nodes().get(0).id());
        awaitResolved(dropped);
        layout();
        assertEquals(new Position(240, 180),
                FxHarness.call(() -> root.editor().node(dropped).orElseThrow().position()));
        assertWithin(NodeView.class, FxHarness.under(scene, at));
        assertTrue(FxHarness.call(() -> root.canvas().selection().get().selects(dropped)));
        assertTrue(FxHarness
                .call(() -> scene.lookup(".canvas-hint") != null && !scene.lookup(".canvas-hint").isVisible()));

        // an entry the server does not list gives nothing, and a drop on a node reaches the canvas behind it
        assertFalse(FxHarness.drag(scene, PaletteTransfer.CONNECTOR, "nobody/Home").drop(new Point2D(600, 400)));
        final Node title = FxHarness.call(() -> nodeView(dropped).lookup(".node-title"));
        assertTrue(FxHarness
                .drag(scene, PaletteTransfer.CONNECTOR, sink.id().value())
                .drop(FxHarness.call(() -> centerOf(title))));
        assertEquals(2, FxHarness.call(() -> root.editor().pipeline().nodes().size()));
        layout();
        snapshot("canvas-dropped");
    }

    @Test
    void thePointerReachesTheNodesThePortsAndTheLinks() throws Exception {
        show(catalog());
        final Node surface = FxHarness.call(this::surface);
        // the hint covers the middle of the canvas, and must let the pointer through
        assertSame(surface, under(new Point2D(500, 300)));
        assertSame(surface, under(new Point2D(1, 1)));

        addNodes();
        final Link first = link(read, MAIN, transform);
        FxHarness.call(() -> {
            root.editor().connect(first);
            root.editor().connect(link(transform, "second", write));
            return null;
        });
        layout();

        assertSame(handle(output(read, MAIN)), under(center(output(read, MAIN))));
        assertSame(handle(input(transform, MAIN)), under(center(input(transform, MAIN))));
        assertSame(handle(output(transform, "REJECT")), under(center(output(transform, "REJECT"))));
        final Node title = FxHarness.call(() -> nodeView(transform).lookup(".node-title"));
        assertWithin(NodeView.class, under(FxHarness.call(() -> centerOf(title))));
        assertWithin(LinkView.class, under(FxHarness.call(() -> middleOf(curve(first)))));
        assertSame(surface, under(new Point2D(900, 500)));

        // the shadow of a card belongs to the canvas, not to the card
        final Bounds card =
                FxHarness.call(() -> nodeView(transform).localToScene(nodeView(transform).getLayoutBounds()));
        assertSame(surface, under(new Point2D(card.getCenterX(), card.getMaxY() + 6)));
        assertSame(surface, under(new Point2D(card.getCenterX(), card.getMinY() - 6)));

        final Button zoomIn = FxHarness
                .call(() -> FxHarness
                        .all(scene.getRoot(), Button.class)
                        .stream()
                        .filter(button -> "+".equals(button.getText()))
                        .findFirst()
                        .orElseThrow());
        assertWithin(Button.class, under(FxHarness.call(() -> centerOf(zoomIn))));
        // and the zoom controls take no more room than they need
        final Bounds controls = FxHarness.call(() -> {
            final Node zoom = scene.lookup(".zoom-controls");
            return zoom.localToScene(zoom.getLayoutBounds());
        });
        assertSame(surface, under(new Point2D(controls.getMinX() - 10, controls.getCenterY())));
    }

    @Test
    void anUnavailableConnectorAndTheProblemsOfThePipeline() throws Exception {
        show(catalog());
        final NodeId lost = add(absent, 60, 80);
        final NodeId orphan = add(sink, 360, 80);
        assertTrue(FxHarness
                .await(scene, PATIENCE,
                        () -> root.editor().resolution(lost) instanceof Resolution.Unresolved
                                && root.editor().resolution(orphan) instanceof Resolution.Resolved));

        assertTrue(FxHarness.call(() -> nodeView(lost).getStyleClass().contains("unresolved")));
        assertEquals("Unavailable", FxHarness.call(() -> ((Label) nodeView(lost).lookup(".node-status")).getText()));
        assertEquals(List.of(), flows(lost, Port.Direction.INPUT));

        final ProblemsView problems = FxHarness.call(() -> FxHarness.all(scene.getRoot(), ProblemsView.class).get(0));
        assertTrue(FxHarness.call(problems::isVisible));
        assertFalse(FxHarness.call(problems::isExpanded));
        assertEquals("Problems (2)", FxHarness.call(problems::getText));
        FxHarness.call(() -> {
            problems.setExpanded(true);
            return null;
        });
        layout();
        snapshot("canvas-problems");

        // a click on a finding selects the node it is about
        final Node cell = FxHarness.call(() -> problems.lookupAll(".list-cell").stream().filter(candidate -> {
            final Object item = ((ListCell<?>) candidate).getItem();
            return item instanceof PipelineProblem problem && orphan.equals(problem.node());
        }).findFirst().orElseThrow());
        FxHarness.click(scene, FxHarness.call(() -> centerOf(cell)));
        layout();
        assertTrue(FxHarness.call(() -> root.canvas().selection().get().selects(orphan)));
        snapshot("canvas-problem-selected");
    }

    @Test
    void aSavedPipelineComesBackAsItWas() throws Exception {
        show(catalog());
        addNodes();
        FxHarness.call(() -> {
            root.editor().connect(link(read, MAIN, transform));
            root.editor().connect(link(transform, "second", write));
            root.editor().connect(link(transform, "REJECT", rejects));
            return null;
        });
        final Path file = temp.resolve("demo" + PipelineRepository.FILE_EXTENSION);
        final AtomicReference<String> failure = new AtomicReference<>();

        FxHarness.call(() -> {
            root.project().saveAs(file, () -> {
            }, failure::set);
            return null;
        });
        assertTrue(FxHarness.await(PATIENCE, () -> !root.project().busy().get()), "The save did not end");
        assertNull(failure.get());
        final Pipeline saved = FxHarness.call(() -> root.editor().pipeline());

        FxHarness.call(() -> {
            root.project().newPipeline();
            return null;
        });
        layout();
        assertEquals(0, FxHarness.call(() -> FxHarness.all(scene.getRoot(), NodeView.class).size()));
        assertTrue(FxHarness.call(() -> scene.lookup(".canvas-hint").isVisible()));

        FxHarness.call(() -> {
            root.project().open(file, () -> {
            }, failure::set);
            return null;
        });
        assertTrue(FxHarness
                .await(scene, PATIENCE,
                        () -> saved.equals(root.editor().pipeline()) && saved
                                .nodes()
                                .stream()
                                .allMatch(node -> root.editor().resolution(node.id()) instanceof Resolution.Resolved)),
                "The pipeline did not come back");
        assertNull(failure.get());

        assertEquals(4, FxHarness.call(() -> FxHarness.all(scene.getRoot(), NodeView.class).size()));
        assertEquals(3, FxHarness.call(() -> FxHarness.all(scene.getRoot(), LinkView.class).size()));
        saved.links().forEach(this::assertJoined);
        snapshot("canvas-reopened");
    }

    @Test
    void aConnectorIsAddedWhereTheUserLooks() throws Exception {
        show(catalog());
        final NodeId far = add(sink, 1500, 800);
        awaitResolved(far);
        final ScrollPane scroll = FxHarness.call(() -> FxHarness.all(scene.getRoot(), ScrollPane.class).get(0));
        FxHarness.call(() -> {
            scroll.setHvalue(1);
            scroll.setVvalue(1);
            return null;
        });
        layout();

        FxHarness.call(() -> {
            canvas.addConnectorInView(input);
            return null;
        });
        layout();

        final NodeId added = FxHarness
                .call(() -> root.editor().pipeline().nodes().get(1).id());
        final Point2D corner = FxHarness.call(() -> nodeView(added).localToScene(0, 0));
        final Bounds visible = FxHarness.call(() -> scroll.localToScene(scroll.getLayoutBounds()));
        assertTrue(visible.contains(corner), "The node is out of sight: " + corner + " not in " + visible);
    }

    @Test
    @EnabledIfSystemProperty(named = LIVE_CONFIG, matches = ".+")
    void aPipelineIsDrawnWithTheConnectorsOfTheLiveServer() throws Exception {
        show(CompositionRoot.create(List.of("--config", System.getProperty(LIVE_CONFIG)), FxAsync.production()));
        assertTrue(FxHarness.await(PATIENCE, () -> !root.session().connectors().get().isEmpty()));
        read = drop("Fixed Schema Input", 40, 80);
        transform = drop("Fixed Schema Processor", 320, 60);
        write = drop("Name: The Output 1 -en", 640, 60);
        awaitResolved(read, transform, write);

        connectWithTheMouse(output(read, MAIN), input(transform, MAIN));
        connectWithTheMouse(output(transform, "second"), input(write, MAIN));

        assertEquals(List.of(link(read, MAIN, transform), link(transform, "second", write)), links());
        links().forEach(this::assertJoined);
        // the nodes all show the same placeholder until their own icon arrives
        assertTrue(
                FxHarness.await(scene, PATIENCE,
                        () -> iconsShown(read, transform, write).stream().distinct().count() == 3),
                "The nodes did not get their icons");
        snapshot("canvas-live");
    }

    // ---- setting up

    private FakeCatalog catalog() {
        return new FakeCatalog()
                .withConnectors(List.of(input, otherInput, processor, sink, absent))
                .define(Connectors.asInput(input))
                .define(Connectors.asInput(otherInput))
                .define(Connectors.definition(processor, List.of(MAIN), List.of(MAIN, "second", "REJECT")))
                .define(Connectors.asOutput(sink));
    }

    private void show(final FakeCatalog catalog) throws Exception {
        show(new CompositionRoot(new FileConfigStore(temp.resolve("config.json")),
                ConfigLoadResult.loaded(ApplicationConfig.defaults()), FxAsync.production(),
                settings -> FakeConnection.onPort(9001, catalog), new FileSystemPipelineRepository()));
    }

    private void show(final CompositionRoot created) throws Exception {
        root = created;
        scene = FxHarness.call(() -> {
            canvas = new CanvasView(created.canvas(), new IconService(created.async(), created.session()));
            final BorderPane area = new BorderPane(canvas);
            area.setBottom(new ProblemsView(created.canvas()));
            final Scene shown = new Scene(area, WIDTH, HEIGHT);
            shown.getStylesheets().add(CanvasSnapshotTest.class.getResource(STYLESHEET).toExternalForm());
            new Stage().setScene(shown);
            created.start();
            return shown;
        });
        assertTrue(FxHarness.await(PATIENCE, () -> created.session().state().get() == ServerSession.State.CONNECTED),
                "Not connected");
        layout();
    }

    // an input, a processor with three outputs and two sinks, without any link
    private void addNodes() throws Exception {
        read = add(input, 40, 80);
        transform = add(processor, 320, 60);
        write = add(sink, 640, 60);
        rejects = add(sink, 640, 260);
        awaitResolved(read, transform, write, rejects);
    }

    private NodeId add(final ConnectorSummary connector, final double x, final double y) throws Exception {
        return FxHarness.call(() -> root.canvas().addConnector(connector, new Position(x, y)));
    }

    private NodeId drop(final String displayName, final double x, final double y) throws Exception {
        return FxHarness.call(() -> {
            final ConnectorSummary connector = root
                    .session()
                    .connectors()
                    .get()
                    .stream()
                    .filter(candidate -> candidate.displayName().equals(displayName))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("The server has no connector " + displayName));
            return root.canvas().dropConnector(connector.id(), new Position(x, y)).orElseThrow();
        });
    }

    private void awaitResolved(final NodeId... ids) throws Exception {
        assertTrue(
                FxHarness
                        .await(scene, PATIENCE,
                                () -> Arrays
                                        .stream(ids)
                                        .allMatch(id -> root.editor().resolution(id) instanceof Resolution.Resolved)),
                "The definitions were not loaded");
    }

    private void layout() throws Exception {
        FxHarness.call(() -> {
            scene.getRoot().applyCss();
            scene.getRoot().layout();
            return null;
        });
    }

    private void snapshot(final String name) throws Exception {
        final int colors = FxHarness.snapshot(scene, directory.resolve(name + ".png"));
        assertTrue(colors > 20, name + " is blank: " + colors + " color(s)");
    }

    // ---- reading what is drawn, on the toolkit thread

    // what the pointer reaches at a point of the scene, as the toolkit picks it
    private Node under(final Point2D scenePoint) throws Exception {
        return FxHarness.under(scene, scenePoint);
    }

    private static void assertWithin(final Class<? extends Node> type, final Node picked) {
        Node node = picked;
        while (node != null && !type.isInstance(node)) {
            node = node.getParent();
        }
        assertTrue(node != null, "The pointer reached " + picked + ", which is not in a " + type.getSimpleName());
    }

    private Pane surface() {
        return (Pane) scene.lookup(".canvas-surface");
    }

    private NodeView nodeView(final NodeId id) {
        return FxHarness
                .all(scene.getRoot(), NodeView.class)
                .stream()
                .filter(view -> view.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No view for " + id));
    }

    private List<Image> iconsShown(final NodeId... ids) {
        return Arrays.stream(ids).map(id -> FxHarness.all(nodeView(id), ImageView.class).get(0).getImage()).toList();
    }

    private CubicCurve curve(final Link link) {
        return FxHarness
                .all(scene.getRoot(), LinkView.class)
                .stream()
                .filter(view -> view.link().equals(link))
                .map(view -> (CubicCurve) view.lookup(".link"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No view for " + link));
    }

    private static Point2D centerOf(final Node node) {
        return node
                .localToScene(node.getBoundsInLocal().getCenterX(), node.getBoundsInLocal().getCenterY());
    }

    private static Point2D middleOf(final CubicCurve curve) {
        // the point of a cubic curve halfway along its parameter
        final double x =
                (curve.getStartX() + 3 * curve.getControlX1() + 3 * curve.getControlX2() + curve.getEndX()) / 8;
        final double y =
                (curve.getStartY() + 3 * curve.getControlY1() + 3 * curve.getControlY2() + curve.getEndY()) / 8;
        return curve.localToScene(x, y);
    }

    private List<String> flows(final NodeId id, final Port.Direction direction) throws Exception {
        return FxHarness
                .call(() -> FxHarness
                        .all(nodeView(id), PortView.class)
                        .stream()
                        .filter(port -> port.port().direction() == direction)
                        .map(port -> port.port().flow())
                        .toList());
    }

    private PortView output(final NodeId id, final String flow) throws Exception {
        return FxHarness.call(() -> nodeView(id).port(Port.Direction.OUTPUT, flow).orElseThrow());
    }

    private PortView input(final NodeId id, final String flow) throws Exception {
        return FxHarness.call(() -> nodeView(id).port(Port.Direction.INPUT, flow).orElseThrow());
    }

    private Point2D center(final PortView port) throws Exception {
        return FxHarness.call(() -> port.anchorIn(scene.getRoot()));
    }

    private Point2D scrolled() throws Exception {
        return FxHarness.call(() -> {
            final ScrollPane scroll = FxHarness.all(scene.getRoot(), ScrollPane.class).get(0);
            return new Point2D(scroll.getHvalue(), scroll.getVvalue());
        });
    }

    private List<Link> links() throws Exception {
        return FxHarness.call(() -> root.editor().pipeline().links());
    }

    private Endpoint linkSource() throws Exception {
        return FxHarness.call(() -> root.canvas().linkSource().get());
    }

    private String feedback() throws Exception {
        return FxHarness.call(() -> root.canvas().feedback().get());
    }

    private static Link link(final NodeId from, final String flow, final NodeId to) {
        return new Link(new Endpoint(from, flow), new Endpoint(to, MAIN));
    }

    // the curve of a link starts and ends on the ports it joins, in the coordinates of the canvas
    private void assertJoined(final Link link) {
        try {
            final PortView from = output(link.from().node(), link.from().flow());
            final PortView to = input(link.to().node(), link.to().flow());
            final double[] actual = FxHarness.call(() -> {
                final CubicCurve curve = curve(link);
                final Pane space = surface();
                final Point2D start = from.anchorIn(space);
                final Point2D end = to.anchorIn(space);
                return new double[] { curve.getStartX() - start.getX(), curve.getStartY() - start.getY(),
                        curve.getEndX() - end.getX(), curve.getEndY() - end.getY() };
            });
            assertEquals("[0.0, 0.0, 0.0, 0.0]",
                    Arrays.toString(Arrays.stream(actual).map(CanvasSnapshotTest::round).toArray()),
                    "The link " + link + " is off its ports");
        } catch (final RuntimeException e) {
            throw e;
        } catch (final Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static double round(final double value) {
        return Math.abs(value) < 0.5 ? 0.0 : value;
    }

    // ---- the gestures of the user

    private void connectWithTheMouse(final PortView source, final PortView target) throws Exception {
        press(source);
        drag(source, center(target));
        release(source, center(target));
        layout();
    }

    private void press(final PortView source) throws Exception {
        FxHarness.press(scene, center(source));
    }

    // the pointer goes through the middle of the way, as it does when the user does not jump
    private void drag(final PortView source, final Point2D to) throws Exception {
        FxHarness.dragTo(scene, center(source).midpoint(to));
        FxHarness.dragTo(scene, to);
    }

    private void release(final PortView source, final Point2D at) throws Exception {
        FxHarness.release(scene, at);
    }

    private Node handle(final PortView port) throws Exception {
        return FxHarness.call(() -> port.lookup(".handle"));
    }
}
