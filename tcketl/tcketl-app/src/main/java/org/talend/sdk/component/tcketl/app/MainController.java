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

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;

import org.talend.sdk.component.tcketl.app.canvas.CanvasView;
import org.talend.sdk.component.tcketl.app.canvas.ProblemsView;
import org.talend.sdk.component.tcketl.app.connection.ConnectionDialog;
import org.talend.sdk.component.tcketl.app.connection.ConnectionViewModel;
import org.talend.sdk.component.tcketl.app.icon.IconService;
import org.talend.sdk.component.tcketl.app.inspector.InspectorView;
import org.talend.sdk.component.tcketl.app.palette.PaletteView;
import org.talend.sdk.component.tcketl.app.project.FxProjectDialogs;
import org.talend.sdk.component.tcketl.app.project.ProjectActions;
import org.talend.sdk.component.tcketl.app.session.EnvironmentWatcher;
import org.talend.sdk.component.tcketl.app.session.ServerSession;

/**
 * The main window: menus, the banner shown when the server cannot be reached, the panels and the status bar. The
 * title of the window is the one of the pipeline, and closing the window asks what to do with unsaved changes.
 */
public final class MainController {

    private final CompositionRoot root;

    private final IconService icons;

    private final Stage stage;

    private PaletteView palette;

    private ProjectActions project;

    @FXML
    private StackPane paletteHost;

    @FXML
    private BorderPane canvasHost;

    @FXML
    private StackPane inspectorHost;

    @FXML
    private HBox banner;

    @FXML
    private Label bannerText;

    @FXML
    private Label statusLabel;

    @FXML
    private Label noticeLabel;

    @FXML
    private Hyperlink changedLink;

    @FXML
    private MenuItem newItem;

    @FXML
    private MenuItem openItem;

    @FXML
    private MenuItem saveItem;

    @FXML
    private MenuItem saveAsItem;

    MainController(final CompositionRoot root, final IconService icons, final Stage stage) {
        this.root = root;
        this.icons = icons;
        this.stage = stage;
    }

    @FXML
    private void initialize() {
        final ServerSession session = root.session();
        palette = new PaletteView(root.palette(), icons);
        paletteHost.getChildren().add(palette);

        final CanvasView canvas = new CanvasView(root.canvas(), icons);
        canvasHost.setCenter(canvas);
        canvasHost.setBottom(new ProblemsView(root.canvas()));
        palette.setOnActivate(canvas::addConnectorInView);
        inspectorHost.getChildren().add(new InspectorView(root.canvas(), root.session(), root.async(), icons));

        project = new ProjectActions(root.project(), new FxProjectDialogs(stage));
        stage.titleProperty().bind(root.project().title());
        stage.setOnCloseRequest(event -> {
            event.consume();
            quit();
        });
        // a file is not opened or written twice at the same time
        for (final MenuItem item : new MenuItem[] { newItem, openItem, saveItem, saveAsItem }) {
            item.disableProperty().bind(root.project().busy());
        }

        banner.visibleProperty().bind(session.state().isEqualTo(ServerSession.State.FAILED));
        banner.managedProperty().bind(banner.visibleProperty());
        bannerText.textProperty().bind(session.problem());
        statusLabel.textProperty().bind(session.description());
        noticeLabel.textProperty().bind(root.notice());
        noticeLabel.visibleProperty().bind(root.notice().isNotEmpty());
        noticeLabel.managedProperty().bind(noticeLabel.visibleProperty());
        final EnvironmentWatcher watcher = new EnvironmentWatcher(session, root.async());
        changedLink.visibleProperty().bind(watcher.changed());
        changedLink.managedProperty().bind(changedLink.visibleProperty());
        final Timeline poller = new Timeline(new KeyFrame(Duration.seconds(30), event -> watcher.poll()));
        poller.setCycleCount(Animation.INDEFINITE);
        poller.play();
    }

    @FXML
    private void newPipeline() {
        project.newPipeline();
    }

    @FXML
    private void openPipeline() {
        project.open();
    }

    @FXML
    private void savePipeline() {
        project.save();
    }

    @FXML
    private void savePipelineAs() {
        project.saveAs();
    }

    @FXML
    private void openConnectionSettings() {
        final ConnectionViewModel model =
                new ConnectionViewModel(root.config().get(), root.tester(), root.async());
        ConnectionDialog.show(stage, model).ifPresent(root::apply);
    }

    @FXML
    private void retry() {
        root.session().reload();
    }

    @FXML
    private void refreshPalette() {
        root.palette().refresh();
    }

    @FXML
    private void searchPalette() {
        palette.focusSearch();
    }

    @FXML
    private void quit() {
        project.confirmThen(Platform::exit);
    }
}
