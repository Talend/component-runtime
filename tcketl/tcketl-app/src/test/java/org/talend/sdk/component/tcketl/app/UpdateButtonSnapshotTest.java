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
import java.util.Map;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.talend.sdk.component.tcketl.app.async.AsyncRunner;
import org.talend.sdk.component.tcketl.app.testing.Connectors;
import org.talend.sdk.component.tcketl.app.testing.FakeCatalog;
import org.talend.sdk.component.tcketl.app.testing.FakeConnection;
import org.talend.sdk.component.tcketl.app.testing.FxHarness;
import org.talend.sdk.component.tcketl.config.ApplicationConfig;
import org.talend.sdk.component.tcketl.config.ConfigLoadResult;
import org.talend.sdk.component.tcketl.config.FileConfigStore;
import org.talend.sdk.component.tcketl.domain.action.ActionOutcome;
import org.talend.sdk.component.tcketl.domain.connector.ConnectorDefinition;
import org.talend.sdk.component.tcketl.domain.connector.ConnectorRole;
import org.talend.sdk.component.tcketl.domain.connector.ConnectorSummary;
import org.talend.sdk.component.tcketl.domain.connector.Port;
import org.talend.sdk.component.tcketl.domain.pipeline.NodeId;
import org.talend.sdk.component.tcketl.domain.pipeline.Position;
import org.talend.sdk.component.tcketl.domain.property.ActionBinding;
import org.talend.sdk.component.tcketl.domain.property.ActionDefinition;
import org.talend.sdk.component.tcketl.domain.property.PropertyDefinition;
import org.talend.sdk.component.tcketl.domain.property.PropertyType;

/** The button of an update service, driven in the real window with a fake server. */
@Tag("ui")
@EnabledIfSystemProperty(named = FxHarness.SNAPSHOTS, matches = ".+")
class UpdateButtonSnapshotTest {

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
    void theAnswerOfTheUpdateFillsTheFieldsAndIsStored() throws Exception {
        final NodeId id = open(new ActionOutcome.Replacement(Map.of(".host", "db", ".port", "5432")));
        FxHarness.call(() -> {
            root.canvas().select(id);
            return null;
        });
        assertTrue(FxHarness.await(scene, PATIENCE, () -> !FxHarness.all(inspector(), Button.class).isEmpty()),
                "No form in the inspector");

        FxHarness.call(() -> {
            button().fire();
            return null;
        });

        final boolean filled = FxHarness.await(scene, PATIENCE, () -> "db".equals(field("Host").getText()));
        FxHarness.snapshot(scene, directory.resolve("inspector-update.png"));
        assertTrue(filled, "The field was not filled");
        assertEquals("5432", FxHarness.call(() -> field("Port").getText()));
        FxHarness.snapshot(scene, directory.resolve("inspector-update.png"));
        assertEquals("db", FxHarness.call(() -> root.editor().node(id).orElseThrow().configuration())
                .get("config.upd.host"));
    }

    @Test
    void anEmptyAnswerIsReportedAndChangesNothing() throws Exception {
        final NodeId id = open(new ActionOutcome.Replacement(Map.of()));
        FxHarness.call(() -> {
            root.canvas().select(id);
            return null;
        });
        assertTrue(FxHarness.await(scene, PATIENCE, () -> !FxHarness.all(inspector(), Button.class).isEmpty()));

        FxHarness.call(() -> {
            button().fire();
            return null;
        });

        assertTrue(FxHarness.await(scene, PATIENCE, () -> FxHarness.all(inspector(), Label.class)
                .stream()
                .anyMatch(label -> label.getStyleClass().contains("form-ko"))));
    }

    private Button button() {
        return FxHarness.all(inspector(), Button.class)
                .stream()
                .filter(button -> button.getStyleClass().contains("form-action"))
                .findFirst()
                .orElseThrow();
    }

    private TextField field(final String title) {
        final List<TextField> fields = FxHarness.all(inspector(), TextField.class);
        return fields.get("Host".equals(title) ? 0 : 1);
    }

    private Parent inspector() {
        return (StackPane) scene.lookup(".inspector-area");
    }

    private NodeId open(final ActionOutcome answer) throws Exception {
        final ConnectorDefinition definition = definition();
        final FakeCatalog catalog = new FakeCatalog().withConnectors(List.of(summary)).define(definition);
        root = new CompositionRoot(new FileConfigStore(directory.resolve("unused-config.json")),
                ConfigLoadResult.loaded(ApplicationConfig.defaults()), AsyncRunner.production(),
                settings -> FakeConnection.onPort(9001, catalog).withActions(request -> answer));
        scene = FxHarness.call(() -> {
            final Stage stage = new Stage();
            final Scene shown = MainWindow.createScene(root, stage);
            stage.setScene(shown);
            root.start();
            return shown;
        });
        return FxHarness.call(() -> root.editor().addNode(definition, new Position(100, 100)));
    }

    private ConnectorDefinition definition() {
        final List<PropertyDefinition> properties = List.of(
                PropertyDefinition.builder("config", PropertyType.OBJECT).displayName("Configuration").build(),
                PropertyDefinition.builder("config.upd", PropertyType.OBJECT)
                        .displayName("Connection")
                        .action(new ActionBinding("update", "refresh", List.of("."), Map.of()))
                        .build(),
                PropertyDefinition.builder("config.upd.host", PropertyType.STRING).displayName("Host").build(),
                PropertyDefinition.builder("config.upd.port", PropertyType.STRING).displayName("Port").build());
        final List<ActionDefinition> actions = List.of(new ActionDefinition("sampleForm", "update", "refresh",
                "Refresh",
                List.of(PropertyDefinition.builder("value", PropertyType.OBJECT).parameterIndex(0).build())));
        return new ConnectorDefinition(summary.ref(), summary.displayName(), List.of(), List.of(Port.DEFAULT_FLOW),
                false, false, "", properties, actions);
    }
}
