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
import java.net.URL;
import java.util.Objects;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import org.talend.sdk.component.tcketl.app.icon.IconService;

/** Builds the scene of the main window out of its FXML, its controller and its style sheet. */
final class MainWindow {

    private static final int WIDTH = 1280;

    private static final int HEIGHT = 800;

    private MainWindow() {
        // utility class
    }

    /**
     * Must be called once the JavaFX toolkit runs, on its thread.
     *
     * @param root what the window shows and acts on
     * @param stage the window the scene is made for, which the dialogs belong to
     */
    static Scene createScene(final CompositionRoot root, final Stage stage) throws IOException {
        final IconService icons = new IconService(root.async(), root.session());
        final FXMLLoader loader = new FXMLLoader(resource("MainView.fxml"));
        loader.setControllerFactory(type -> new MainController(root, icons, stage));
        final Scene scene = new Scene(loader.load(), WIDTH, HEIGHT);
        scene.getStylesheets().add(resource("tcketl.css").toExternalForm());
        return scene;
    }

    private static URL resource(final String name) {
        return Objects.requireNonNull(MainWindow.class.getResource(name), () -> "Missing resource " + name);
    }
}
