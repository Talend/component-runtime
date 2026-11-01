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
package org.talend.sdk.component.tcketl.app.project;

import java.io.File;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import org.talend.sdk.component.tcketl.persistence.PipelineRepository;

/** The project dialogs as the file choosers of the platform and alerts over the main window. */
public final class FxProjectDialogs implements ProjectDialogs {

    private static final String STYLESHEET = "/org/talend/sdk/component/tcketl/app/tcketl.css";

    private final Window owner;

    private File lastDirectory;

    public FxProjectDialogs(final Window owner) {
        this.owner = owner;
    }

    @Override
    public Choice confirmDiscard(final String name) {
        final ButtonType save = new ButtonType("Save", ButtonBar.ButtonData.YES);
        final ButtonType discard = new ButtonType("Don't save", ButtonBar.ButtonData.NO);
        final Alert alert = alert(Alert.AlertType.CONFIRMATION, "Unsaved changes",
                "Save the changes to \"" + name + "\"?", "The changes are lost if you do not save them.");
        alert.getButtonTypes().setAll(save, discard, ButtonType.CANCEL);
        final Optional<ButtonType> answer = alert.showAndWait();
        if (answer.filter(save::equals).isPresent()) {
            return Choice.SAVE;
        }
        return answer.filter(discard::equals).isPresent() ? Choice.DISCARD : Choice.CANCEL;
    }

    @Override
    public Optional<Path> chooseFileToOpen() {
        return Optional.ofNullable(chooser("Open a pipeline").showOpenDialog(owner)).map(this::remember);
    }

    @Override
    public Optional<Path> chooseFileToSave(final String suggestedName) {
        final FileChooser chooser = chooser("Save the pipeline");
        chooser.setInitialFileName(suggestedName);
        return Optional.ofNullable(chooser.showSaveDialog(owner)).map(this::remember);
    }

    @Override
    public void showError(final String message) {
        final Alert alert = alert(Alert.AlertType.ERROR, "tcketl", null, message);
        alert.getButtonTypes().setAll(ButtonType.OK);
        alert.showAndWait();
    }

    private Alert alert(final Alert.AlertType type, final String title, final String header, final String content) {
        final Alert alert = new Alert(type, content);
        alert.initOwner(owner);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setResizable(true);
        alert
                .getDialogPane()
                .getStylesheets()
                .add(Objects.requireNonNull(FxProjectDialogs.class.getResource(STYLESHEET), STYLESHEET)
                        .toExternalForm());
        return alert;
    }

    private FileChooser chooser(final String title) {
        final String extension = PipelineRepository.FILE_EXTENSION;
        final FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        chooser
                .getExtensionFilters()
                .addAll(new FileChooser.ExtensionFilter("tcketl pipelines (*" + extension + ")", "*" + extension),
                        new FileChooser.ExtensionFilter("JSON files (*.json)", "*.json"),
                        new FileChooser.ExtensionFilter("All files", "*.*"));
        if (lastDirectory != null && lastDirectory.isDirectory()) {
            chooser.setInitialDirectory(lastDirectory);
        }
        return chooser;
    }

    private Path remember(final File chosen) {
        final File folder = chosen.getParentFile();
        if (folder != null) {
            lastDirectory = folder;
        }
        return chosen.toPath();
    }
}
