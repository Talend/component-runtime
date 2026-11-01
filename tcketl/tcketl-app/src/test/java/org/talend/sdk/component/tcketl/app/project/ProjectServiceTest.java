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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.talend.sdk.component.tcketl.app.async.AsyncRunner;
import org.talend.sdk.component.tcketl.app.testing.Connectors;
import org.talend.sdk.component.tcketl.app.testing.TestAsync;
import org.talend.sdk.component.tcketl.domain.connector.ConnectorRole;
import org.talend.sdk.component.tcketl.domain.connector.ConnectorSummary;
import org.talend.sdk.component.tcketl.domain.connector.Port;
import org.talend.sdk.component.tcketl.domain.pipeline.Link;
import org.talend.sdk.component.tcketl.domain.pipeline.NodeId;
import org.talend.sdk.component.tcketl.domain.pipeline.Pipeline;
import org.talend.sdk.component.tcketl.domain.pipeline.PipelineEditor;
import org.talend.sdk.component.tcketl.domain.pipeline.Position;
import org.talend.sdk.component.tcketl.persistence.FileSystemPipelineRepository;

class ProjectServiceTest {

    @TempDir
    Path dir;

    private final PipelineEditor editor = new PipelineEditor();

    private final FileSystemPipelineRepository repository = new FileSystemPipelineRepository();

    private final List<String> failures = new ArrayList<>();

    private final List<String> done = new ArrayList<>();

    private final ProjectService project = new ProjectService(editor, repository, AsyncRunner.direct());

    @Test
    void anyChangeMakesThePipelineDirtyEvenWhenItIsUndoneByHand() {
        assertFalse(project.dirty().get());
        assertEquals("Untitled - tcketl", project.title().get());

        final NodeId node = addNode();
        editor.removeNode(node);

        assertTrue(project.dirty().get());
        assertEquals("Untitled * - tcketl", project.title().get());
    }

    @Test
    void savingBindsTheFileCleansTheFlagAndNamesAnUntitledPipelineAfterTheFile() throws IOException {
        addNode();
        final Path file = dir.resolve("demo.tcketl.json");

        project.saveAs(file, () -> done.add("saved"), failures::add);

        assertEquals(List.of("saved"), done);
        assertEquals(List.of(), failures);
        assertEquals(file, project.file().get());
        assertFalse(project.dirty().get());
        assertEquals("demo - tcketl", project.title().get());
        assertEquals(editor.pipeline(), repository.load(file));
    }

    @Test
    void aPipelineThatHasAChosenNameKeepsIt() {
        editor.rename("Orders");

        project.saveAs(dir.resolve("other.tcketl.json"), () -> done.add("saved"), failures::add);

        assertEquals("Orders", project.name().get());
    }

    @Test
    void whatIsSavedIsWhatThePipelineWasWhenSavingBegan() throws IOException {
        final TestAsync async = new TestAsync();
        final ProjectService slow = new ProjectService(editor, repository, async.runner());
        final NodeId node = addNode();
        final Position before = editor.node(node).orElseThrow().position();
        final Path file = dir.resolve("slow.tcketl.json");

        slow.saveAs(file, () -> done.add("saved"), failures::add);
        assertTrue(slow.busy().get());
        editor.moveNode(node, new Position(400, 300));
        async.drain();

        assertEquals(List.of("saved"), done);
        assertFalse(slow.busy().get());
        assertTrue(slow.dirty().get(), "the move was made after the snapshot");
        assertEquals(before, repository.load(file).nodes().get(0).position());
    }

    @Test
    void aSavedPipelineIsRestoredIdenticallyAndIsClean() {
        final NodeId first = addNode();
        final NodeId second = addNode();
        editor.setConfiguration(first, Map.of("config.url", "http://example", "config.retries", "3"));
        editor.connect(Link.of(first, Port.DEFAULT_FLOW, second, Port.DEFAULT_FLOW));
        final Path file = dir.resolve("again.tcketl.json");
        project.saveAs(file, () -> {
        }, failures::add);
        final Pipeline expected = editor.pipeline();

        final PipelineEditor other = new PipelineEditor();
        final ProjectService reopened = new ProjectService(other, repository, AsyncRunner.direct());
        reopened.open(file, () -> done.add("opened"), failures::add);

        assertEquals(List.of("opened"), done);
        assertEquals(expected, other.pipeline());
        assertEquals(file, reopened.file().get());
        assertFalse(reopened.dirty().get());
    }

    @Test
    void aFileThatCannotBeReadLeavesThePipelineUntouched() throws IOException {
        addNode();
        final Pipeline before = editor.pipeline();
        final Path broken = dir.resolve("broken.tcketl.json");
        Files.writeString(broken, "{ this is not json", StandardCharsets.UTF_8);

        project.open(broken, () -> done.add("opened"), failures::add);

        assertEquals(List.of(), done);
        assertEquals(1, failures.size());
        assertTrue(failures.get(0).startsWith("Cannot open broken.tcketl.json: "), failures.get(0));
        assertEquals(before, editor.pipeline());
        assertNull(project.file().get());
        assertFalse(project.busy().get());
    }

    @Test
    void aMissingFileIsSaidToBeMissing() {
        project.open(dir.resolve("gone.tcketl.json"), () -> done.add("opened"), failures::add);

        assertEquals(List.of("Cannot open gone.tcketl.json: the file does not exist"), failures);
    }

    @Test
    void aFailedSaveKeepsThePipelineDirtyAndWithoutFile() throws IOException {
        addNode();
        final Path blocker = Files.writeString(dir.resolve("plain.txt"), "x");

        project.saveAs(blocker.resolve("under.tcketl.json"), () -> done.add("saved"), failures::add);

        assertEquals(List.of(), done);
        assertEquals(1, failures.size());
        assertTrue(failures.get(0).startsWith("Cannot save under.tcketl.json: "), failures.get(0));
        assertTrue(project.dirty().get());
        assertNull(project.file().get());
        assertFalse(project.busy().get());
    }

    @Test
    void startingAgainEmptiesThePipelineAndForgetsTheFile() {
        addNode();
        project.saveAs(dir.resolve("old.tcketl.json"), () -> {
        }, failures::add);
        addNode();

        project.newPipeline();

        assertEquals(Pipeline.empty(), editor.pipeline());
        assertNull(project.file().get());
        assertFalse(project.dirty().get());
        assertEquals("Untitled - tcketl", project.title().get());
    }

    @Test
    void onlyOneFileOperationRunsAtATime() {
        final TestAsync async = new TestAsync();
        final ProjectService slow = new ProjectService(editor, repository, async.runner());
        addNode();

        slow.saveAs(dir.resolve("a.tcketl.json"), () -> done.add("saved"), failures::add);
        slow.open(dir.resolve("b.tcketl.json"), () -> done.add("opened"), failures::add);
        async.drain();

        assertEquals(List.of("Another file operation is still running"), failures);
        assertEquals(List.of("saved"), done);
    }

    @Test
    void savingWithoutAFileNeedsTheFileToBeChosenFirst() {
        assertThrows(IllegalStateException.class, () -> project.save(() -> {
        }, failures::add));
    }

    @ParameterizedTest
    @CsvSource({ "demo.tcketl.json,demo", "Demo.TCKETL.JSON,Demo", "plain.json,plain", "noextension,noextension",
            "a.b.json,a.b", ".hidden,.hidden" })
    void aFilenameProposesTheNameOfThePipeline(final String fileName, final String expected) {
        assertEquals(expected, ProjectService.baseName(Path.of("folder", fileName)));
    }

    private NodeId addNode() {
        final int index = editor.pipeline().nodes().size();
        final ConnectorSummary connector =
                Connectors.summary("family", "Connector " + index, ConnectorRole.PROCESSOR);
        return editor
                .addNode(Connectors.definition(connector, List.of(Port.DEFAULT_FLOW), List.of(Port.DEFAULT_FLOW)),
                        new Position(40.0 + index * 200, 40));
    }
}
