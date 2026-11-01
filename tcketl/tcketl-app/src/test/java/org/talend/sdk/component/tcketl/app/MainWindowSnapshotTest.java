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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;

import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import org.talend.sdk.component.tcketl.app.async.AsyncRunner;
import org.talend.sdk.component.tcketl.app.session.ServerSession.State;
import org.talend.sdk.component.tcketl.app.testing.Connectors;
import org.talend.sdk.component.tcketl.app.testing.FakeCatalog;
import org.talend.sdk.component.tcketl.app.testing.FakeConnection;
import org.talend.sdk.component.tcketl.app.testing.FxHarness;
import org.talend.sdk.component.tcketl.config.ApplicationConfig;
import org.talend.sdk.component.tcketl.config.ConfigLoadResult;
import org.talend.sdk.component.tcketl.config.FileConfigStore;
import org.talend.sdk.component.tcketl.domain.connector.ConnectorRole;
import org.talend.sdk.component.tcketl.domain.connector.ConnectorSummary;
import org.talend.sdk.component.tcketl.domain.pipeline.Resolution;
import org.talend.sdk.component.tcketl.persistence.FileSystemPipelineRepository;

/**
 * Renders the real main window off screen, to look at it and to check what it shows. It needs the JavaFX toolkit, so
 * it only runs when asked to, with {@code -Dtcketl.ui.snapshots=<directory>} where the pictures are written. Add
 * {@code -Dtcketl.ui.config=<settings file>} to also render the window against the server those settings point at.
 */
@Tag("ui")
@EnabledIfSystemProperty(named = FxHarness.SNAPSHOTS, matches = ".+")
class MainWindowSnapshotTest {

    static final String LIVE_CONFIG = "tcketl.ui.config";

    private static final Duration PATIENCE = Duration.ofSeconds(20);

    private static final String FAMILY = "Name: The family -en";

    private final Path directory = FxHarness.snapshotDirectory();

    private final ConnectorSummary simpleInput =
            Connectors.summary("the_family", FAMILY, "Simple Input", List.of("Test"), ConnectorRole.INPUT);

    // the index reports it as a processor, whereas its definition says it is a sink
    private final ConnectorSummary sink =
            Connectors.summary("the_family", FAMILY, "Name: The Output 1 -en", List.of("Test"),
                    ConnectorRole.PROCESSOR);

    private final ConnectorSummary formInput =
            Connectors.summary("sampleForm", "Sample form", "Input", List.of("Forms"), ConnectorRole.INPUT);

    private final ConnectorSummary fixedSchema = Connectors
            .summary("sampleFixedSchema", "Sample fixed schema", "Fixed Schema Input", List.of(), ConnectorRole.INPUT);

    @TempDir
    Path temp;

    private CompositionRoot root;

    private Stage stage;

    private Scene scene;

    @BeforeAll
    static void startToolkit() throws InterruptedException {
        FxHarness.start();
    }

    @AfterEach
    void close() throws Exception {
        if (root != null) {
            FxHarness.call(() -> {
                root.close();
                return null;
            });
        }
    }

    @Test
    void paletteOfAServerWithComponents() throws Exception {
        show(fakeRoot(componentsCatalog()));
        assertTrue(FxHarness.await(PATIENCE, () -> root.palette().isResolved(sink)));

        final int colors = FxHarness.snapshot(scene, directory.resolve("palette.png"));

        assertTrue(colors > 20, "The window is blank: " + colors + " color(s)");
        assertEquals("Connected to http://localhost:9001/api/v1, server 1.0.0", text("#statusLabel"));
        assertEquals(3, familiesShown());
        assertEquals(ConnectorRole.OUTPUT, FxHarness.call(() -> root.palette().roleOf(sink)));
        assertTrue(FxHarness.call(() -> !scene.lookup("#banner").isVisible()));
    }

    @Test
    void searchNarrowsThePalette() throws Exception {
        show(fakeRoot(componentsCatalog()));
        assertTrue(FxHarness.await(PATIENCE, () -> root.palette().isResolved(sink)));

        FxHarness.call(() -> {
            root.palette().query().set("fixed");
            return null;
        });
        FxHarness.snapshot(scene, directory.resolve("palette-search.png"));

        assertEquals(1, familiesShown());
    }

    @Test
    void unreachableServerShowsTheBanner() throws Exception {
        show(fakeRoot(new FakeCatalog().unreachable("Cannot reach http://localhost:9001/api/v1: Connection refused")));
        assertTrue(FxHarness.await(PATIENCE, () -> root.session().state().get() == State.FAILED));

        FxHarness.snapshot(scene, directory.resolve("banner.png"));

        assertTrue(FxHarness.call(() -> scene.lookup("#banner").isVisible()));
        assertTrue(text("#bannerText").contains("Connection refused"), text("#bannerText"));
        assertEquals("Not connected to http://localhost:9001/api/v1", text("#statusLabel"));
    }

    @Test
    void aPipelineIsStartedFromThePaletteAndSavedFromTheKeyboard() throws Exception {
        show(fakeRoot(componentsCatalog()));
        assertTrue(FxHarness.await(scene, PATIENCE, () -> root.palette().isResolved(simpleInput)
                && entryIsShown(simpleInput)));
        // a pipeline that never was saved asks where to save it, which a dialog answers: give it its file first
        final Path file = temp.resolve("orders.tcketl.json");
        FxHarness.call(() -> {
            root.project().saveAs(file, () -> {
            }, failure -> {
                throw new IllegalStateException(failure);
            });
            return null;
        });
        assertTrue(FxHarness.await(PATIENCE, () -> !root.project().busy().get()));
        assertEquals("orders - tcketl", title());
        assertTrue(FxHarness.call(() -> scene.lookup(".canvas-hint").isVisible()),
                "The canvas does not say what to do");

        FxHarness.doubleClick(scene, entry(simpleInput));
        assertTrue(FxHarness.await(scene, PATIENCE, () -> root.editor().pipeline().nodes().size() == 1
                && root.editor()
                        .resolution(root.editor().pipeline().nodes().get(0).id()) instanceof Resolution.Resolved));
        assertEquals("Simple Input", FxHarness.call(() -> root.editor().pipeline().nodes().get(0).label()));
        assertEquals("orders * - tcketl", title());
        assertEquals(1, FxHarness.call(() -> scene.getRoot().lookupAll(".node-card").size()));
        assertTrue(FxHarness.call(() -> !scene.lookup(".canvas-hint").isVisible()));
        final int colors = FxHarness.snapshot(scene, directory.resolve("shell-pipeline.png"));
        assertTrue(colors > 20, "The window is blank: " + colors + " color(s)");

        FxHarness.shortcut(scene, KeyCode.S);
        assertTrue(FxHarness.await(PATIENCE, () -> !root.project().dirty().get()));
        assertEquals("orders - tcketl", title());
        assertEquals(1, new FileSystemPipelineRepository().load(file).nodes().size());
    }

    @Test
    void theKeyboardReachesThePalette() throws Exception {
        final FakeCatalog catalog = componentsCatalog();
        show(fakeRoot(catalog));
        assertTrue(FxHarness.await(scene, PATIENCE, () -> root.palette().isResolved(sink)));

        FxHarness.shortcut(scene, KeyCode.F);
        assertTrue(FxHarness
                .call(() -> scene.getFocusOwner() instanceof TextField field
                        && "Search components".equals(field.getPromptText())),
                "The search field has no focus");

        final int before = catalog.listCalls();
        FxHarness.key(scene, KeyCode.F5);
        assertTrue(FxHarness.await(PATIENCE, () -> catalog.listCalls() > before), "The palette was not refreshed");
    }

    @Test
    @EnabledIfSystemProperty(named = LIVE_CONFIG, matches = ".+")
    void paletteOfTheServerOfTheSettings() throws Exception {
        show(CompositionRoot.create(List.of("--config", System.getProperty(LIVE_CONFIG))));
        assertTrue(FxHarness.await(PATIENCE, () -> root.session().state().get() == State.CONNECTED
                && !root.palette().visible().get().families().isEmpty()));
        // the window is never shown, so only laying the scene out creates the cells, which then ask for their icons
        FxHarness.await(scene, PATIENCE, () -> everyRoleIsKnown() && everyIconIsLoaded());

        final int colors = FxHarness.snapshot(scene, directory.resolve("palette-live.png"));

        assertTrue(colors > 20, "The window is blank: " + colors + " color(s)");
        assertTrue(FxHarness.call(() -> iconsShown().stream().distinct().count() > 1), "Every icon is a placeholder");
        assertTrue(text("#statusLabel").startsWith("Connected to "), text("#statusLabel"));
    }

    private String title() throws Exception {
        return FxHarness.call(stage::getTitle);
    }

    private boolean entryIsShown(final ConnectorSummary connector) {
        return cells(connector).findAny().isPresent();
    }

    // where the pointer has to be to reach the entry of the palette
    private Point2D entry(final ConnectorSummary connector) throws Exception {
        return FxHarness
                .call(() -> cells(connector)
                        .findFirst()
                        .map(cell -> cell
                                .localToScene(cell.getBoundsInLocal().getCenterX(),
                                        cell.getBoundsInLocal().getCenterY()))
                        .orElseThrow(() -> new IllegalStateException("The palette does not show " + connector)));
    }

    private Stream<Node> cells(final ConnectorSummary connector) {
        return scene
                .getRoot()
                .lookupAll(".tree-cell")
                .stream()
                .filter(cell -> cell instanceof TreeCell<?> shown && connector.equals(shown.getItem()));
    }

    private boolean everyRoleIsKnown() {
        return root
                .palette()
                .visible()
                .get()
                .families()
                .stream()
                .flatMap(family -> family.connectors().stream())
                .allMatch(root.palette()::isResolved);
    }

    private boolean everyIconIsLoaded() {
        final List<Image> icons = iconsShown();
        // the icons that are not there yet all show the same placeholder
        return !icons.isEmpty() && icons.stream().distinct().count() == icons.size();
    }

    private List<Image> iconsShown() {
        return scene
                .lookup(".tree-view")
                .lookupAll(".image-view")
                .stream()
                .map(view -> ((ImageView) view).getImage())
                .toList();
    }

    private FakeCatalog componentsCatalog() {
        return new FakeCatalog()
                .withConnectors(List.of(simpleInput, sink, formInput, fixedSchema))
                .define(Connectors.asInput(simpleInput))
                .define(Connectors.asOutput(sink))
                .define(Connectors.asInput(formInput))
                .define(Connectors.asInput(fixedSchema));
    }

    private CompositionRoot fakeRoot(final FakeCatalog catalog) {
        return new CompositionRoot(new FileConfigStore(directory.resolve("unused-config.json")),
                ConfigLoadResult.loaded(ApplicationConfig.defaults()), AsyncRunner.production(),
                settings -> FakeConnection.onPort(9001, catalog));
    }

    private void show(final CompositionRoot created) throws Exception {
        root = created;
        scene = FxHarness.call(() -> {
            stage = new Stage();
            final Scene shown = MainWindow.createScene(created, stage);
            stage.setScene(shown);
            created.start();
            return shown;
        });
    }

    private String text(final String selector) throws Exception {
        return FxHarness.call(() -> ((Label) scene.lookup(selector)).getText());
    }

    private int familiesShown() throws Exception {
        return FxHarness.call(() -> {
            final TreeView<?> tree = (TreeView<?>) scene.lookup(".tree-view");
            return tree.getRoot().getChildren().size();
        });
    }
}
