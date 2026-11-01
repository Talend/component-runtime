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

import java.nio.file.Path;
import java.util.Locale;

import org.talend.sdk.component.tcketl.persistence.PipelineRepository;

/**
 * The commands of the File menu. Every command that would throw away unsaved changes first asks what to do with
 * them, and goes on only once they are saved or the user agreed to lose them.
 *
 * <p>
 * Everything MUST be used on the UI thread.
 */
public final class ProjectActions {

    private static final String JSON_EXTENSION = ".json";

    private static final String FORBIDDEN_IN_FILE_NAMES = "[\\\\/:*?\"<>|]";

    private final ProjectService project;

    private final ProjectDialogs dialogs;

    public ProjectActions(final ProjectService project, final ProjectDialogs dialogs) {
        this.project = project;
        this.dialogs = dialogs;
    }

    public void newPipeline() {
        confirmThen(project::newPipeline);
    }

    /** Asks for a file, then opens it. */
    public void open() {
        dialogs.chooseFileToOpen().ifPresent(this::open);
    }

    public void open(final Path file) {
        confirmThen(() -> project.open(file, () -> {
        }, dialogs::showError));
    }

    /** Saves to the file of the pipeline, or asks for one when it has none. */
    public void save() {
        saveThen(() -> {
        });
    }

    /** Asks for a file, then saves to it. */
    public void saveAs() {
        saveAsThen(() -> {
        });
    }

    /** Runs {@code proceed} now when nothing would be lost, else once the user has decided what happens to it. */
    public void confirmThen(final Runnable proceed) {
        if (!project.dirty().get()) {
            proceed.run();
            return;
        }
        final ProjectDialogs.Choice choice = dialogs.confirmDiscard(project.name().get());
        if (choice == ProjectDialogs.Choice.SAVE) {
            saveThen(proceed);
        } else if (choice == ProjectDialogs.Choice.DISCARD) {
            proceed.run();
        }
    }

    /** The file name of {@code file}, made a pipeline file name. */
    static Path withExtension(final Path file) {
        final String name = String.valueOf(file.getFileName());
        final String lower = name.toLowerCase(Locale.ROOT);
        if (lower.endsWith(PipelineRepository.FILE_EXTENSION)) {
            return file;
        }
        final String stem =
                lower.endsWith(JSON_EXTENSION) ? name.substring(0, name.length() - JSON_EXTENSION.length()) : name;
        return file.resolveSibling(stem + PipelineRepository.FILE_EXTENSION);
    }

    private void saveThen(final Runnable proceed) {
        if (project.file().get() != null) {
            project.save(proceed, dialogs::showError);
        } else {
            saveAsThen(proceed);
        }
    }

    private void saveAsThen(final Runnable proceed) {
        dialogs
                .chooseFileToSave(suggestedName())
                .map(ProjectActions::withExtension)
                .ifPresent(file -> project.saveAs(file, proceed, dialogs::showError));
    }

    private String suggestedName() {
        return project.name().get().replaceAll(FORBIDDEN_IN_FILE_NAMES, "_") + PipelineRepository.FILE_EXTENSION;
    }
}
