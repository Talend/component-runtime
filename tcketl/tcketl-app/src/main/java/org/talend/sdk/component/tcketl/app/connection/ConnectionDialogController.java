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

import java.util.ArrayList;
import java.util.List;

import javafx.beans.InvalidationListener;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

/** Binds the controls of {@code ConnectionDialog.fxml} to a {@link ConnectionViewModel}. */
public final class ConnectionDialogController {

    private static final String SUCCESS_STYLE = "success";

    private static final String FAILURE_STYLE = "failure";

    private final ConnectionViewModel model;

    @FXML
    private ComboBox<String> scheme;

    @FXML
    private TextField host;

    @FXML
    private TextField port;

    @FXML
    private TextField basePath;

    @FXML
    private ComboBox<String> language;

    @FXML
    private Label url;

    @FXML
    private Button testButton;

    @FXML
    private Label testResult;

    @FXML
    private Label validation;

    ConnectionDialogController(final ConnectionViewModel model) {
        this.model = model;
    }

    @FXML
    private void initialize() {
        scheme.getItems().setAll(ConnectionViewModel.SCHEMES);
        scheme.valueProperty().bindBidirectional(model.scheme());
        host.textProperty().bindBidirectional(model.host());
        port.textProperty().bindBidirectional(model.port());
        basePath.textProperty().bindBidirectional(model.basePath());
        language.getItems().setAll(languages());
        language.valueProperty().bindBidirectional(model.language());
        url.textProperty().bind(model.url());
        validation.textProperty().bind(model.validationMessage());
        testButton.disableProperty().bind(model.valid().not().or(model.testing()));
        testResult.textProperty().bind(model.testMessage());
        final InvalidationListener restyle = observed -> showTestOutcome();
        model.testMessage().addListener(restyle);
        model.testSucceeded().addListener(restyle);
        model.testing().addListener(restyle);
        showTestOutcome();
    }

    @FXML
    private void test() {
        model.test();
    }

    private List<String> languages() {
        final List<String> languages = new ArrayList<>(ConnectionViewModel.LANGUAGES);
        final String configured = model.language().get();
        if (!languages.contains(configured)) {
            languages.add(0, configured);
        }
        return languages;
    }

    private void showTestOutcome() {
        testResult.getStyleClass().removeAll(SUCCESS_STYLE, FAILURE_STYLE);
        if (!model.testMessage().get().isEmpty() && !model.testing().get()) {
            testResult.getStyleClass().add(model.testSucceeded().get() ? SUCCESS_STYLE : FAILURE_STYLE);
        }
    }
}
