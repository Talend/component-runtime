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
package org.talend.sdk.component.tcketl.app.connection;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.Objects;
import java.util.Optional;

import javafx.fxml.FXMLLoader;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.stage.Window;

import org.talend.sdk.component.tcketl.config.ApplicationConfig;

/** The modal dialog in which the user tells where the component server is. */
public final class ConnectionDialog {

    private static final String STYLESHEET = "/org/talend/sdk/component/tcketl/app/tcketl.css";

    static final ButtonType APPLY = new ButtonType("Save and connect", ButtonBar.ButtonData.OK_DONE);

    private ConnectionDialog() {
        // static helper only
    }

    /** @return the new settings, empty when the dialog was dismissed */
    public static Optional<ApplicationConfig> show(final Window owner, final ConnectionViewModel model) {
        final Dialog<ApplicationConfig> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.setTitle("Connection settings");
        dialog.setDialogPane(create(model));
        dialog.setResultConverter(button -> button == APPLY ? model.result().orElse(null) : null);
        try {
            return dialog.showAndWait();
        } finally {
            model.close();
        }
    }

    /** The content of the dialog, with its buttons, ready to be put in a {@link Dialog}. */
    static DialogPane create(final ConnectionViewModel model) {
        final DialogPane pane = load(model);
        pane.getStylesheets()
                .add(Objects.requireNonNull(ConnectionDialog.class.getResource(STYLESHEET), STYLESHEET)
                        .toExternalForm());
        pane.getButtonTypes().setAll(APPLY, ButtonType.CANCEL);
        pane.lookupButton(APPLY).disableProperty().bind(model.valid().not());
        return pane;
    }

    private static DialogPane load(final ConnectionViewModel model) {
        final URL fxml = Objects.requireNonNull(ConnectionDialog.class.getResource("ConnectionDialog.fxml"),
                "ConnectionDialog.fxml");
        final FXMLLoader loader = new FXMLLoader(fxml);
        loader.setControllerFactory(type -> new ConnectionDialogController(model));
        try {
            return loader.load();
        } catch (final IOException e) {
            throw new UncheckedIOException("Cannot load the connection dialog", e);
        }
    }
}
