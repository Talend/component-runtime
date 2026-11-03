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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.talend.sdk.component.tcketl.app.async.FxAsync;
import org.talend.sdk.component.tcketl.app.testing.Connectors;
import org.talend.sdk.component.tcketl.app.testing.FakeCatalog;
import org.talend.sdk.component.tcketl.app.testing.FakeConnection;
import org.talend.sdk.component.tcketl.app.testing.FxHarness;
import org.talend.sdk.component.tcketl.config.ApplicationConfig;
import org.talend.sdk.component.tcketl.config.ConfigLoadResult;
import org.talend.sdk.component.tcketl.config.FileConfigStore;
import org.talend.sdk.component.tcketl.domain.connector.ConnectorDefinition;
import org.talend.sdk.component.tcketl.domain.connector.ConnectorRole;
import org.talend.sdk.component.tcketl.domain.connector.ConnectorSummary;
import org.talend.sdk.component.tcketl.domain.connector.Port;
import org.talend.sdk.component.tcketl.domain.pipeline.NodeId;
import org.talend.sdk.component.tcketl.domain.pipeline.Position;
import org.talend.sdk.component.tcketl.domain.property.ConditionClause;
import org.talend.sdk.component.tcketl.domain.property.Constraints;
import org.talend.sdk.component.tcketl.domain.property.EvaluationStrategy;
import org.talend.sdk.component.tcketl.domain.property.GridLayout;
import org.talend.sdk.component.tcketl.domain.property.LayoutHints;
import org.talend.sdk.component.tcketl.domain.property.PropertyDefinition;
import org.talend.sdk.component.tcketl.domain.property.PropertyType;
import org.talend.sdk.component.tcketl.domain.property.VisibilityCondition;

/**
 * Renders the inspector in the real main window off screen and drives its controls. Like the other UI tests it only
 * runs with {@code -Dtcketl.ui.snapshots=<directory>}.
 */
@Tag("ui")
@EnabledIfSystemProperty(named = FxHarness.SNAPSHOTS, matches = ".+")
class InspectorSnapshotTest {

    private static final Duration PATIENCE = Duration.ofSeconds(20);

    private final Path directory = FxHarness.snapshotDirectory();

    private final ConnectorSummary summary =
            Connectors.summary("sampleForm", "Sample form", "Input", List.of("Forms"), ConnectorRole.INPUT);

    private CompositionRoot root;

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
    void theFormOfTheSelectedNodeHasOneTabPerLayoutAndStoresEdits() throws Exception {
        final NodeId id = open();
        final long revision = FxHarness.call(() -> root.editor().revision());

        select(id);
        final TabPane tabs = awaitTabs();
        assertEquals(List.of("Main", "Advanced"),
                FxHarness.call(() -> tabs.getTabs().stream().map(Tab::getText).toList()));
        assertEquals(revision, FxHarness.call(() -> root.editor().revision()), "Opening a form edited the pipeline");
        assertEquals(Map.of(), configuration(id));
        FxHarness.snapshot(scene, directory.resolve("inspector-main.png"));

        FxHarness.call(() -> {
            textField(tabs, "Name").setText("orders");
            return null;
        });
        assertEquals("orders", configuration(id).get("config.name"));

        FxHarness.call(() -> {
            tabs.getSelectionModel().select(1);
            return null;
        });
        assertTrue(FxHarness.await(scene, PATIENCE, () -> !FxHarness.all(tabs, ComboBox.class).isEmpty()));
        FxHarness.snapshot(scene, directory.resolve("inspector-advanced.png"));
        FxHarness.call(() -> {
            @SuppressWarnings("unchecked")
            final ComboBox<String> mode = (ComboBox<String>) FxHarness.all(tabs, ComboBox.class).get(0);
            mode.setValue("FAST");
            return null;
        });
        assertEquals("FAST", configuration(id).get("config.mode"));

        FxHarness.call(() -> {
            root.canvas().clearSelection();
            root.canvas().select(id);
            return null;
        });
        final TabPane again = awaitTabs();
        assertEquals("orders", FxHarness.call(() -> textField(again, "Name").getText()));
    }

    @Test
    void aWrongValueShowsItsMessage() throws Exception {
        final NodeId id = open();
        select(id);
        final TabPane tabs = awaitTabs();
        assertTrue(FxHarness.call(() -> errors(tabs).isEmpty()), "A message is shown for a form just opened");

        FxHarness.call(() -> {
            textField(tabs, "Count").setText("abc");
            return null;
        });
        assertTrue(FxHarness.await(scene, PATIENCE, () -> !errors(tabs).isEmpty()),
                "The message of the wrong value is not shown");
        FxHarness.snapshot(scene, directory.resolve("inspector-required.png"));
    }

    @Test
    void aConditionShowsAndHidesAFieldWhenItsTargetChanges() throws Exception {
        final NodeId id = open();
        select(id);
        final TabPane tabs = awaitTabs();
        FxHarness.call(() -> {
            tabs.getSelectionModel().select(1);
            return null;
        });
        assertTrue(FxHarness.await(scene, PATIENCE, () -> !labelsTitled(tabs, "Tags").isEmpty()));
        assertFalse(FxHarness.call(() -> shown(labelsTitled(tabs, "Tags").get(0))), "Tags shown while disabled");

        FxHarness.call(() -> {
            tabs.getSelectionModel().select(0);
            final CheckBox enabled = (CheckBox) FxHarness.all(tabs, CheckBox.class).get(0);
            enabled.setSelected(true);
            tabs.getSelectionModel().select(1);
            return null;
        });
        assertTrue(FxHarness.await(scene, PATIENCE, () -> shown(labelsTitled(tabs, "Tags").get(0))),
                "Tags hidden while enabled");
        FxHarness.snapshot(scene, directory.resolve("inspector-conditional.png"));
    }

    private static List<Label> labelsTitled(final Parent parent, final String title) {
        return FxHarness.all(parent, Label.class).stream().filter(label -> title.equals(label.getText())).toList();
    }

    private static boolean shown(final javafx.scene.Node node) {
        for (javafx.scene.Node current = node; current != null; current = current.getParent()) {
            if (!current.isVisible()) {
                return false;
            }
        }
        return true;
    }

    private List<Label> errors(final Parent parent) {
        return FxHarness.all(parent, Label.class)
                .stream()
                .filter(label -> label.getStyleClass().contains("form-error") && label.isVisible())
                .toList();
    }

    private Map<String, String> configuration(final NodeId id) throws Exception {
        return FxHarness.call(() -> root.editor().node(id).orElseThrow().configuration());
    }

    private NodeId open() throws Exception {
        final ConnectorDefinition definition = definition();
        final FakeCatalog catalog = new FakeCatalog().withConnectors(List.of(summary)).define(definition);
        root = new CompositionRoot(new FileConfigStore(directory.resolve("unused-config.json")),
                ConfigLoadResult.loaded(ApplicationConfig.defaults()), FxAsync.production(),
                settings -> FakeConnection.onPort(9001, catalog));
        scene = FxHarness.call(() -> {
            final Stage stage = new Stage();
            final Scene shown = MainWindow.createScene(root, stage);
            stage.setScene(shown);
            root.start();
            return shown;
        });
        return FxHarness.call(() -> root.editor().addNode(definition, new Position(100, 100)));
    }

    private void select(final NodeId id) throws Exception {
        FxHarness.call(() -> {
            root.canvas().select(id);
            return null;
        });
    }

    private TabPane awaitTabs() throws Exception {
        assertTrue(FxHarness.await(scene, PATIENCE, () -> !FxHarness.all(inspector(), TabPane.class).isEmpty()),
                "No form in the inspector");
        return FxHarness.call(() -> FxHarness.all(inspector(), TabPane.class).get(0));
    }

    private StackPane inspector() {
        return (StackPane) scene.lookup(".inspector-area");
    }

    private static TextField textField(final Parent parent, final String title) {
        for (final Label label : FxHarness.all(parent, Label.class)) {
            if (title.equals(label.getText()) && label.getLabelFor() instanceof TextField field) {
                return field;
            }
        }
        throw new AssertionError("No text field titled " + title);
    }

    private ConnectorDefinition definition() {
        final LayoutHints layout = new LayoutHints(
                List.of(new GridLayout("Main", List.of(List.of("name", "count"), List.of("enabled"))),
                        new GridLayout("Advanced", List.of(List.of("mode"), List.of("tags")))),
                List.of());
        final List<PropertyDefinition> properties = List.of(
                PropertyDefinition.builder("config", PropertyType.OBJECT)
                        .displayName("Configuration")
                        .layout(layout)
                        .build(),
                PropertyDefinition.builder("config.name", PropertyType.STRING)
                        .displayName("Name")
                        .constraints(Constraints.NONE.withRequired(true))
                        .build(),
                PropertyDefinition.builder("config.count", PropertyType.NUMBER).displayName("Count").build(),
                PropertyDefinition.builder("config.enabled", PropertyType.BOOLEAN).displayName("Enabled").build(),
                PropertyDefinition.builder("config.mode", PropertyType.ENUM)
                        .displayName("Mode")
                        .constraints(Constraints.NONE.withEnumValues(List.of("SLOW", "FAST")))
                        .build(),
                PropertyDefinition.builder("config.tags", PropertyType.ARRAY)
                        .displayName("Tags")
                        .visibility(new VisibilityCondition(VisibilityCondition.Operator.AND,
                                List.of(new ConditionClause("enabled", EvaluationStrategy.DEFAULT, Map.of(),
                                        List.of("true"), false))))
                        .build(),
                PropertyDefinition.builder("config.tags[]", PropertyType.STRING).displayName("Tag").build());
        return new ConnectorDefinition(summary.ref(), summary.displayName(), List.of(), List.of(Port.DEFAULT_FLOW),
                false, false, "", properties, List.of());
    }
}
