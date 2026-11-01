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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.talend.sdk.component.tcketl.app.async.AsyncRunner;
import org.talend.sdk.component.tcketl.app.testing.Connectors;
import org.talend.sdk.component.tcketl.domain.connector.ConnectorRole;
import org.talend.sdk.component.tcketl.domain.connector.Port;
import org.talend.sdk.component.tcketl.domain.pipeline.Pipeline;
import org.talend.sdk.component.tcketl.domain.pipeline.PipelineEditor;
import org.talend.sdk.component.tcketl.domain.pipeline.Position;
import org.talend.sdk.component.tcketl.persistence.FileSystemPipelineRepository;

class ProjectActionsTest {

    @TempDir
    Path dir;

    private final PipelineEditor editor = new PipelineEditor();

    private final FileSystemPipelineRepository repository = new FileSystemPipelineRepository();

    private final FakeDialogs dialogs = new FakeDialogs();

    private final ProjectService project = new ProjectService(editor, repository, AsyncRunner.direct());

    private final ProjectActions actions = new ProjectActions(project, dialogs);

    private final List<String> proceeded = new ArrayList<>();

    @Test
    void aCleanPipelineIsReplacedWithoutAskingAnything() {
        actions.confirmThen(() -> proceeded.add("go"));

        assertEquals(List.of("go"), proceeded);
        assertEquals(List.of(), dialogs.asked);
    }

    @Test
    void cancellingKeepsThePipelineAndDoesNotProceed() {
        edit();
        dialogs.choice = ProjectDialogs.Choice.CANCEL;

        actions.newPipeline();

        assertEquals(List.of("Untitled"), dialogs.asked);
        assertEquals(1, editor.pipeline().nodes().size());
        assertTrue(project.dirty().get());
    }

    @Test
    void discardingLosesTheChangesAndProceeds() {
        edit();
        dialogs.choice = ProjectDialogs.Choice.DISCARD;

        actions.newPipeline();

        assertEquals(Pipeline.empty(), editor.pipeline());
        assertFalse(project.dirty().get());
    }

    @Test
    void savingFirstAsksForAFileThenProceedsOnceItIsWritten() {
        edit();
        dialogs.choice = ProjectDialogs.Choice.SAVE;
        dialogs.toSave = Optional.of(dir.resolve("kept"));
        final Path written = dir.resolve("kept.tcketl.json");

        actions.confirmThen(() -> proceeded.add(Files.exists(written) ? "after-write" : "too-early"));

        assertEquals(List.of("after-write"), proceeded);
        assertEquals(List.of("Untitled.tcketl.json"), dialogs.suggestions);
        assertFalse(project.dirty().get());
    }

    @Test
    void aSaveThatFailsStopsTheOperationAndTellsWhy() throws IOException {
        edit();
        dialogs.choice = ProjectDialogs.Choice.SAVE;
        dialogs.toSave = Optional.of(Files.writeString(dir.resolve("plain.txt"), "x").resolve("under"));

        actions.newPipeline();

        assertEquals(1, dialogs.errors.size());
        assertTrue(dialogs.errors.get(0).startsWith("Cannot save under.tcketl.json: "), dialogs.errors.get(0));
        assertEquals(1, editor.pipeline().nodes().size());
        assertTrue(project.dirty().get());
    }

    @Test
    void giveUpTheFileChooserAndNothingHappens() {
        edit();
        dialogs.choice = ProjectDialogs.Choice.SAVE;

        actions.confirmThen(() -> proceeded.add("go"));
        actions.save();
        actions.saveAs();
        actions.open();

        assertEquals(List.of(), proceeded);
        assertEquals(List.of(), dialogs.errors);
        assertTrue(project.dirty().get());
        assertEquals(1, editor.pipeline().nodes().size());
    }

    @Test
    void savingAgainWritesToTheSameFileWithoutAsking() throws IOException {
        edit();
        dialogs.toSave = Optional.of(dir.resolve("twice.tcketl.json"));
        actions.save();
        edit();
        dialogs.toSave = Optional.empty();

        actions.save();

        assertEquals(1, dialogs.suggestions.size());
        assertFalse(project.dirty().get());
        assertEquals(editor.pipeline(), repository.load(dir.resolve("twice.tcketl.json")));
    }

    @Test
    void openingAFileFirstAsksAboutTheUnsavedChanges() {
        final Path file = dir.resolve("other.tcketl.json");
        edit();
        dialogs.toSave = Optional.of(file);
        actions.saveAs();
        edit();
        dialogs.toOpen = Optional.of(file);
        dialogs.choice = ProjectDialogs.Choice.CANCEL;

        actions.open();
        assertEquals(2, editor.pipeline().nodes().size());

        dialogs.choice = ProjectDialogs.Choice.DISCARD;
        actions.open();
        assertEquals(1, editor.pipeline().nodes().size());
        assertFalse(project.dirty().get());
    }

    @Test
    void aFileThatCannotBeOpenedIsReportedAndTheCurrentPipelineStays() {
        edit();
        dialogs.choice = ProjectDialogs.Choice.DISCARD;
        dialogs.toOpen = Optional.of(dir.resolve("missing.tcketl.json"));

        actions.open();

        assertEquals(List.of("Cannot open missing.tcketl.json: the file does not exist"), dialogs.errors);
        assertEquals(1, editor.pipeline().nodes().size());
    }

    @ParameterizedTest
    @CsvSource({ "demo,demo.tcketl.json", "demo.json,demo.tcketl.json", "demo.tcketl.json,demo.tcketl.json",
            "Demo.TCKETL.JSON,Demo.TCKETL.JSON", "demo.txt,demo.txt.tcketl.json" })
    void aPipelineFileAlwaysEndsWithTheFormatExtension(final String chosen, final String expected) {
        assertEquals(dir.resolve(expected), ProjectActions.withExtension(dir.resolve(chosen)));
    }

    private void edit() {
        final int index = editor.pipeline().nodes().size();
        editor
                .addNode(Connectors
                        .definition(Connectors.summary("family", "Connector " + index, ConnectorRole.PROCESSOR),
                                List.of(Port.DEFAULT_FLOW), List.of(Port.DEFAULT_FLOW)),
                        new Position(40.0 + index * 200, 40));
    }

    private static final class FakeDialogs implements ProjectDialogs {

        private final List<String> asked = new ArrayList<>();

        private final List<String> suggestions = new ArrayList<>();

        private final List<String> errors = new ArrayList<>();

        private Choice choice = Choice.CANCEL;

        private Optional<Path> toOpen = Optional.empty();

        private Optional<Path> toSave = Optional.empty();

        @Override
        public Choice confirmDiscard(final String name) {
            asked.add(name);
            return choice;
        }

        @Override
        public Optional<Path> chooseFileToOpen() {
            return toOpen;
        }

        @Override
        public Optional<Path> chooseFileToSave(final String suggestedName) {
            suggestions.add(suggestedName);
            return toSave;
        }

        @Override
        public void showError(final String message) {
            errors.add(message);
        }
    }
}
